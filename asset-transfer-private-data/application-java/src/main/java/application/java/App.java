/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Running TestApp: 
// gradle runApp 

package application.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import by.bcrypto.bee2j.constants.JceNameConstants;
import by.bcrypto.bee2j.provider.Bee2SecurityProvider;
import by.bcrypto.bee2j.provider.BignPrivateKeySpec;
import com.owlike.genson.Genson;
import org.hyperledger.fabric.gateway.*;

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
	private static final Genson genson = new Genson();

	private static PrivateKey fnpPrivateKey;
	private static PrivateKey npPrivateKey;
	private static X509Certificate fnpCertificate;
	private static X509Certificate npCertificate;

	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
	}

	// helper function for getting connected to the gateway
	public static Gateway connect(String userName) throws Exception{
		// Load a file system based wallet for managing identities.
		Path walletPath = Paths.get("wallet");
		Wallet wallet = Wallets.newFileSystemWallet(walletPath);
		// load a CCP
		Path networkConfigPath = Paths.get("..", "..", "test-network", "organizations", "peerOrganizations", "org1.example.com", "connection-org1.json");

		Gateway.Builder builder = Gateway.createBuilder();
		builder.identity(wallet, userName).networkConfig(networkConfigPath).discovery(true);
		return builder.connect();
	}

	public static void main(String[] args) {
		String userName = "ccchipper";//args[0];
		String jsonFile = "shipper.json";
		System.out.println("Start application as " + jsonFile);
		Organization shipper = null;
		try {
			byte[] shipperBytes = App.class.getClassLoader().getResourceAsStream(jsonFile).readAllBytes();
			shipper = genson.deserialize(shipperBytes, Organization.class);
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}

		// enrolls the admin and registers the user
		try {
			EnrollAdmin.main(null);
			RegisterUser.main(shipper, userName);
		} catch (Exception e) {
			System.err.println(e);
		}

		// connect to the network and invoke the smart contract
		try (Gateway gateway = connect(userName)) {

			// get the network and contract
			Network network = gateway.getNetwork(CHANNEL_NAME);
			Contract contract = network.getContract(CHAINCODE_NAME);

			byte[] result;

			System.out.println("Evaluate Transaction: Reserve waybill");
			result = contract.evaluateTransaction("ReserveWaybill");
			var id = new String(result);
			System.out.println("Reserved waybill with id " + id);
		}
		catch(Exception e){
			System.err.println(e);
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
}
