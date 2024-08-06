/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Running TestApp: 
// gradle runApp 

package application.java;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import by.bcrypto.bee2j.constants.JceNameConstants;
import by.bcrypto.bee2j.provider.Bee2SecurityProvider;
import by.bcrypto.bee2j.provider.BignPrivateKeySpec;
import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.partitioning.BSPTree;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.Peer;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

public class App {

	private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "private");
	private static final String FnpPrivateKeyPath = Paths.get("out", "fnp", "privkey.der").toString();
	private static final String FnpCertificatePath = Paths.get("out", "fnp", "cert.der").toString();
	private static final String FnpPassword = "fnpfnpfnp";
	private static final String NpPrivateKeyPath = Paths.get("out", "np", "privkey.der").toString();
	private static final String NpCertificatePath = Paths.get("out", "np", "cert.der").toString();
	private static final String NpPassword = "npnpnp";
	private final static String TransferPropertiesKey = "transfer_properties";
	private static final Genson genson = new Genson();

	private static PrivateKey fnpPrivateKey;
	private static PrivateKey npPrivateKey;
	private static X509Certificate fnpCertificate;
	private static X509Certificate npCertificate;

	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
	}

	// helper function for getting connected to the gateway
	public static Gateway connect(String userName, RegistrationInfo registrationInfo) throws Exception{
		// Load a file system based wallet for managing identities.
		Path walletPath = Paths.get("wallet");
		Wallet wallet = Wallets.newFileSystemWallet(walletPath);
		// load a CCP
		Path networkConfigPath = Paths.get("..", "..", "test-network", "organizations", "peerOrganizations", registrationInfo.getDomain(), "connection-"+ registrationInfo.getOrgName() +".json");

		Gateway.Builder builder = Gateway.createBuilder();
		builder.identity(wallet, userName).networkConfig(networkConfigPath).discovery(true);
		return builder.connect();
	}

	public static void main(String[] args) throws Exception {
		try {
			var shipperOrganization = parseOrganization("shipper.json");
			var carrierOrganization = parseOrganization("carrier.json");
			var receiverOrganization = parseOrganization("receiver.json");
			var shipperRegistrationInfo = new RegistrationInfo("https://localhost:7054", shipperOrganization.getName() + "Admin", "Org1MSP", "org1.example.com", "org1.department1", "org1");
			var carrierRegistrationInfo = new RegistrationInfo("https://localhost:11054", carrierOrganization.getName() + "Admin", "OperatorMSP", "operator.by", "operator.department1", "operator");
			var receiverRegistrationInfo = new RegistrationInfo("https://localhost:8054", receiverOrganization.getName() + "Admin", "Org2MSP", "org2.example.com", "org2.department1", "org2");

			// Delete wallet if it exists from prior runs
			FileUtils.deleteDirectory(new File("wallet"));
			// shipper
			EnrollAdmin.enroll(shipperRegistrationInfo);
			RegisterUser.register(shipperRegistrationInfo, shipperOrganization);

			// carrier
			EnrollAdmin.enroll(carrierRegistrationInfo);
			RegisterUser.register(carrierRegistrationInfo, carrierOrganization);

			// receiver
			EnrollAdmin.enroll(receiverRegistrationInfo);
			RegisterUser.register(receiverRegistrationInfo, receiverOrganization);

			var shipperContract = getContract(shipperOrganization.getName(), shipperRegistrationInfo);
			var gateway = connect(shipperOrganization.getName(), shipperRegistrationInfo);
			var channel = gateway.getNetwork(CHANNEL_NAME).getChannel();
			var peers = channel.getPeers();
			var peer = peers.stream().filter(x -> x.getName().equals("peer0.org1.example.com")).findFirst().get();
			var carrierContract = getContract(carrierOrganization.getName(), carrierRegistrationInfo);
			var receiverContract = getContract(receiverOrganization.getName(), receiverRegistrationInfo);

			// Register waybill
			var waybillId1 = registerWaybill(shipperContract, peer);
			var waybillId2 = registerWaybill(shipperContract, peer);

			// Init transfer
			initTransfer(waybillId2, shipperOrganization.getGLN(), carrierOrganization.getGLN(), receiverOrganization.getGLN(), shipperContract);

			// Get transfer by carrier
			var waybills = getWaybillsByRange(carrierContract);

		}
		catch (Exception e)
		{
			System.out.println(e);
			System.exit(1);
		}

	}

	private static PrivateKey createPrivateKey(String path, String password) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException {
		byte[] privateKeyContainer = App.class.getClassLoader().getResourceAsStream(path).readAllBytes();
		BignPrivateKeySpec bignPrivateKeySpec = new BignPrivateKeySpec(privateKeyContainer, password);
		KeyFactory bignKeyFactory = KeyFactory.getInstance("Bign", "Bee2");

		return bignKeyFactory.generatePrivate(bignPrivateKeySpec);
	}

	private static String sign(Signer signer) throws Exception {
		PrivateKey privateKey = getPrivateKey(signer);
		X509Certificate certificate = getCertificate(signer);

		Signature bignSignature = Signature.getInstance(JceNameConstants.BignWithBelt, JceNameConstants.ProviderName);
		bignSignature.initSign(privateKey);

		String owner = getOwner(certificate);
		bignSignature.update(owner.getBytes());
		byte[] sig = bignSignature.sign();

		return Base64.getEncoder().encodeToString(sig);
	}

	private static void createInitialTransactions(Contract ctx, Signer signer) throws Exception {
		createAsset(ctx, "asset1", "blue", 5, signer, 300);
		createAsset(ctx, "asset2", "red", 5, signer, 400);
		createAsset(ctx, "asset3", "green", 10, signer, 500);
		createAsset(ctx, "asset4", "yellow", 10, signer, 600);
		createAsset(ctx, "asset5", "black", 15, signer, 700);
		createAsset(ctx, "asset6", "white", 15, signer, 700);
	}

	private static void createAsset(final Contract contract, final String assetID, final String color, final int size, Signer signer, final int appraisedValue) throws Exception {
		contract.submitTransaction("CreateAsset", assetID, color, Integer.toString(size), getOwner(getCertificate(signer)), Integer.toString(appraisedValue), sign(signer), getCertificateSerialNumber(signer));
	}

	private static void initializeKeyMaterials() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, CertificateException {
		Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
		Security.addProvider(bee2j);

		fnpCertificate = createCertificate(FnpCertificatePath);
		npCertificate  = createCertificate(NpCertificatePath);

		fnpPrivateKey = createPrivateKey(FnpPrivateKeyPath, FnpPassword);
		npPrivateKey = createPrivateKey(NpPrivateKeyPath, NpPassword);
	}

	private static X509Certificate createCertificate(String path) throws CertificateException, NoSuchProviderException, IOException {
		byte[] certificate = App.class.getClassLoader().getResourceAsStream(path).readAllBytes();
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "Bee2");
		ByteArrayInputStream is = new ByteArrayInputStream(certificate);

		return (X509Certificate) certificateFactory.generateCertificate(is);
	}

	private static String getOwner(X509Certificate certificate) throws InvalidNameException {
		String dn = certificate.getSubjectX500Principal().getName();
		return new LdapName(dn).getRdns().stream().filter(x -> x.getType().equalsIgnoreCase("CN")).findFirst().get().getValue().toString();
	}

	private static PrivateKey getPrivateKey(Signer signer) throws Exception {
		switch (signer)
		{
			case Fnp:
				return fnpPrivateKey;
			case Np:
				return npPrivateKey;
			default:
				throw new Exception("Invalid signer");
		}
	}

	private static X509Certificate getCertificate(Signer signer) throws Exception {
		switch (signer)
		{
			case Fnp:
				return fnpCertificate;
			case Np:
				return npCertificate;
			default:
				throw new Exception("Invalid signer");
		}
	}

	private static String getCertificateSerialNumber(Signer signer) throws Exception {
		switch (signer)
		{
			case Fnp:
				return fnpCertificate.getSerialNumber().toString();
			case Np:
				return npCertificate.getSerialNumber().toString();
			default:
				throw new Exception("Invalid signer");
		}
	}

	private static String registerWaybill(Contract shipperContract, Peer peer) throws Exception {
			byte[] result;
			System.out.println("Submit Transaction: Reserve waybill");
			result = shipperContract.submitTransaction("ReserveWaybill");
		    //var transaction = shipperContract.createTransaction("ReserveWaybill");
			//transaction.setEndorsingPeers(Arrays.asList(peer));
			//result = transaction.submit();
			var id = new String(result);
			System.out.println("Reserved waybill with id " + id);
			return id;
	}

	private static Contract getContract(String userName, RegistrationInfo registrationInfo) throws Exception {
		Gateway gateway = connect(userName, registrationInfo);
		// get the network and contract
		Network network = gateway.getNetwork(CHANNEL_NAME);
		Contract contract = network.getContract(CHAINCODE_NAME);

		return contract;
	}

	private static Organization parseOrganization(String jsonFileName) throws IOException {
		byte[] bytes = App.class.getClassLoader().getResourceAsStream(jsonFileName).readAllBytes();
		return genson.deserialize(bytes, Organization.class);
	}

	private static void initTransfer(String id, String shipper, String carrier, String receiver, Contract shipperContract) throws IOException, ContractException, InterruptedException, TimeoutException {
		var xmlData = new String(App.class.getClassLoader().getResourceAsStream("waybill.xml").readAllBytes(), StandardCharsets.UTF_8);
		signByShipper(xmlData);
		var waybill = new Waybill(id, shipper, carrier, receiver, xmlData);
		//waybill = genson.deserialize("{\"carrierGLN\":\"carrierGLN\",\"id\":\"shipperCCC-shipperGLN-10\",\"receiverGLN\":\"receiverGLN\",\"shipperGLN\":\"shipperGLN\",\"xmlData\":\"<waybill>\\r\\n    <to>Receiver</to>\\r\\n    <from>Shipper</from>\\r\\n    <id>1</id>\\r\\n</waybill>\"}; {\"carrierGLN\":\"carrierGLN\",\"id\":\"shipperCCC-shipperGLN-10\",\"receiverGLN\":\"receiverGLN\",\"shipperGLN\":\"shipperGLN\",\"xmlData\":\"<waybill>\\r\\n    <to>Receiver</to>\\r\\n    <from>Shipper</from>\\r\\n    <id>1</id>\\r\\n</waybill>\"}", Waybill.class);
		var waybillJson = genson.serialize(waybill);
		var waybillBytes = waybillJson.getBytes(StandardCharsets.UTF_8);
		System.out.println("Create Transaction: InitTransfer");
		var transaction = shipperContract.createTransaction("InitTransfer");
		var transientMap = new HashMap<String, byte[]>();
		transientMap.put(TransferPropertiesKey, waybillBytes);
		transaction.setTransient(transientMap);
		System.out.println("Submit Transaction: InitTransfer");
		transaction.submit();
		System.out.println("InitTransfer: successful");
	}

	private static List<Waybill> getWaybillsByRange(Contract contract) throws ContractException {
		byte[] result;
		System.out.println("Evaluate Transaction: GetWaybillsByRange");
		result = contract.evaluateTransaction("GetWaybillsByRange", "", "");
		var waybills = genson.deserialize(result, new GenericType<List<Waybill>>(){});
		for (var waybill: waybills) {
			System.out.println("Found waybill with id =" + waybill.getId());
		}

		return waybills;
	}

	private static void agreeByCarrier(String id, String shipper, String carrier, String receiver, Contract shipperContract) throws IOException, ContractException, InterruptedException, TimeoutException {
		var xmlData = new String(App.class.getClassLoader().getResourceAsStream("waybill.xml").readAllBytes(), StandardCharsets.UTF_8);
		signByShipper(xmlData);
		var waybill = new Waybill(id, shipper, carrier, receiver, xmlData);
		//waybill = genson.deserialize("{\"carrierGLN\":\"carrierGLN\",\"id\":\"shipperCCC-shipperGLN-10\",\"receiverGLN\":\"receiverGLN\",\"shipperGLN\":\"shipperGLN\",\"xmlData\":\"<waybill>\\r\\n    <to>Receiver</to>\\r\\n    <from>Shipper</from>\\r\\n    <id>1</id>\\r\\n</waybill>\"}; {\"carrierGLN\":\"carrierGLN\",\"id\":\"shipperCCC-shipperGLN-10\",\"receiverGLN\":\"receiverGLN\",\"shipperGLN\":\"shipperGLN\",\"xmlData\":\"<waybill>\\r\\n    <to>Receiver</to>\\r\\n    <from>Shipper</from>\\r\\n    <id>1</id>\\r\\n</waybill>\"}", Waybill.class);
		var waybillJson = genson.serialize(waybill);
		var waybillBytes = waybillJson.getBytes(StandardCharsets.UTF_8);
		System.out.println("Create Transaction: InitTransfer");
		var transaction = shipperContract.createTransaction("InitTransfer");
		var transientMap = new HashMap<String, byte[]>();
		transientMap.put(TransferPropertiesKey, waybillBytes);
		transaction.setTransient(transientMap);
		System.out.println("Submit Transaction: InitTransfer");
		transaction.submit();
		System.out.println("InitTransfer: successful");
	}

	private static String signByShipper(String xmlData) {
		// todo: sign xml

		return xmlData;
	}
}
