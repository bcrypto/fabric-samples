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
import org.hyperledger.fabric.gateway.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

public class App {

	private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");
	private static final String FnpPrivateKeyPath = Paths.get("out", "fnp", "privkey.der").toString();
	private static final String FnpCertificatePath = Paths.get("out", "fnp", "cert.der").toString();
	private static final String FnpPassword = "fnpfnpfnp";
	private static final String NpPrivateKeyPath = Paths.get("out", "np", "privkey.der").toString();
	private static final String NpCertificatePath = Paths.get("out", "np", "cert.der").toString();
	private static final String NpPassword = "npnpnp";

	private static PrivateKey fnpPrivateKey;
	private static PrivateKey npPrivateKey;
	private static X509Certificate fnpCertificate;
	private static X509Certificate npCertificate;

	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
	}

	// helper function for getting connected to the gateway
	public static Gateway connect() throws Exception{
		// Load a file system based wallet for managing identities.
		Path walletPath = Paths.get("wallet");
		Wallet wallet = Wallets.newFileSystemWallet(walletPath);
		// load a CCP
		Path networkConfigPath = Paths.get("..", "..", "test-network", "organizations", "peerOrganizations", "org1.example.com", "connection-org1.yaml");

		Gateway.Builder builder = Gateway.createBuilder();
		builder.identity(wallet, "javaAppUser").networkConfig(networkConfigPath).discovery(true);
		return builder.connect();
	}

	public static void main(String[] args) {
		// enrolls the admin and registers the user
		try {
			EnrollAdmin.main(null);
			RegisterUser.main(null);
			initializeKeyMaterials();
		} catch (Exception e) {
			System.err.println(e);
		}

		// connect to the network and invoke the smart contract
		try (Gateway gateway = connect()) {

			// get the network and contract
			Network network = gateway.getNetwork(CHANNEL_NAME);
			Contract contract = network.getContract(CHAINCODE_NAME);

			byte[] result;

			System.out.println("Submit Transaction: InitLedger creates the initial set of assets on the ledger.");
			contract.submitTransaction("InitLedger");

			createInitialTransactions(contract, Signer.Fnp);

			System.out.println("\n");
			result = contract.evaluateTransaction("GetAllAssets");
			System.out.println("Evaluate Transaction: GetAllAssets, result: " + new String(result));

			System.out.println("\n");
			System.out.println("Submit Transaction: CreateAsset asset13");
			// CreateAsset creates an asset with ID asset13, color yellow, owner Fnp, size 5 and appraisedValue of 1300
			final Signer asset13Owner = Signer.Fnp;
			createAsset(contract, "asset13", "yellow", 5, asset13Owner, 1300);

			System.out.println("\n");
			System.out.println("Evaluate Transaction: ReadAsset asset13");
			// ReadAsset returns an asset with given assetID
			result = contract.evaluateTransaction("ReadAsset", "asset13");
			System.out.println("result: " + new String(result));

			System.out.println("\n");
			System.out.println("Evaluate Transaction: AssetExists asset1");
			// AssetExists returns "true" if an asset with given assetID exist
			result = contract.evaluateTransaction("AssetExists", "asset1");
			System.out.println("result: " + new String(result));

			System.out.println("\n");
			System.out.println("Submit Transaction: UpdateAsset asset1, new AppraisedValue : 350");
			// UpdateAsset updates an existing asset with new properties. Same args as CreateAsset
			final Signer asset1Owner = Signer.Fnp;
			contract.submitTransaction("UpdateAsset", "asset1", "blue", "5", getOwner(getCertificate(asset1Owner)), "350", sign(asset1Owner), getCertificateSerialNumber(asset1Owner));

			System.out.println("\n");
			System.out.println("Evaluate Transaction: ReadAsset asset1");
			result = contract.evaluateTransaction("ReadAsset", "asset1");
			System.out.println("result: " + new String(result));

			try {
				System.out.println("\n");
				System.out.println("Submit Transaction: UpdateAsset asset70");
				// Non existing asset asset70 should throw Error
				final Signer asset70Owner = Signer.Np;
				contract.submitTransaction("UpdateAsset", "asset70", "blue", "5", getOwner(getCertificate(asset70Owner)), "300", sign(asset70Owner), getCertificateSerialNumber(asset70Owner));
			} catch (Exception e) {
				System.err.println("Expected an error on UpdateAsset of non-existing Asset: " + e);
			}

			System.out.println("\n");
			System.out.println("Submit Transaction: TransferAsset asset1 from owner Fnp > owner Np");
			// TransferAsset transfers an asset with given ID to new owner Np
			final Signer newAsset1Owner = Signer.Np;
			contract.submitTransaction("TransferAsset", "asset1", getOwner(getCertificate(newAsset1Owner)), sign(newAsset1Owner), getCertificateSerialNumber(newAsset1Owner));

			System.out.println("\n");
			System.out.println("Evaluate Transaction: ReadAsset asset1");
			result = contract.evaluateTransaction("ReadAsset", "asset1");
			System.out.println("result: " + new String(result));
		}
		catch(Exception e){
			System.err.println(e);
			System.exit(1);
		}
	}

	private static PrivateKey createPrivateKey(String path, String password) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException {
		byte[] privateKeyContainer = App.class.getClassLoader().getResourceAsStream(path).readAllBytes();;
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
