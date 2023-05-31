/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Running TestApp: 
// gradle runApp 

package application.java;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

import by.bcrypto.bee2j.constants.JceNameConstants;
import by.bcrypto.bee2j.provider.Bee2SecurityProvider;
import by.bcrypto.bee2j.provider.BignPublicKey;
import org.apache.commons.math3.geometry.partitioning.BSPTree;
import org.hyperledger.fabric.gateway.*;


public class App {

	private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");

	private static PrivateKey privateKey;
	private static PublicKey publicKey;
	private static String certificate;

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

	public static void main(String[] args) throws Exception {
		// enrolls the admin and registers the user
		try {
			EnrollAdmin.main(null);
			RegisterUser.main(null);
		} catch (Exception e) {
			System.err.println(e);
		}

		createKeyPair();

		// connect to the network and invoke the smart contract
		try (Gateway gateway = connect()) {

			// get the network and contract
			Network network = gateway.getNetwork(CHANNEL_NAME);
			Contract contract = network.getContract(CHAINCODE_NAME);

			byte[] result;

			System.out.println("Submit Transaction: InitLedger creates the initial set of assets on the ledger.");
			contract.submitTransaction("InitLedger");

			createInitialTransactions(contract);

			System.out.println("\n");
			result = contract.evaluateTransaction("GetAllAssets");
			System.out.println("Evaluate Transaction: GetAllAssets, result: " + new String(result));

			System.out.println("\n");
			System.out.println("Submit Transaction: CreateAsset asset13");
			// CreateAsset creates an asset with ID asset13, color yellow, owner Tom, size 5 and appraisedValue of 1300
			final String asset13Owner = "Tom";
			String signature = sign(asset13Owner);
			contract.submitTransaction("CreateAsset", "asset13", "yellow", "5", asset13Owner, "1300", signature, certificate);

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
			final String asset1Owner = "Tomoko";
			contract.submitTransaction("UpdateAsset", "asset1", "blue", "5", asset1Owner, "350", sign(asset1Owner), certificate);

			System.out.println("\n");
			System.out.println("Evaluate Transaction: ReadAsset asset1");
			result = contract.evaluateTransaction("ReadAsset", "asset1");
			System.out.println("result: " + new String(result));

			try {
				System.out.println("\n");
				System.out.println("Submit Transaction: UpdateAsset asset70");
				// Non existing asset asset70 should throw Error
				final String asset70Owner = "Tomoko";
				contract.submitTransaction("UpdateAsset", "asset70", "blue", "5", asset70Owner, "300", sign(asset70Owner), certificate);
			} catch (Exception e) {
				System.err.println("Expected an error on UpdateAsset of non-existing Asset: " + e);
			}

			System.out.println("\n");
			System.out.println("Submit Transaction: TransferAsset asset1 from owner Tomoko > owner Tom");
			// TransferAsset transfers an asset with given ID to new owner Tom
			final String newAsset1Owner = "Tom";
			contract.submitTransaction("TransferAsset", "asset1", newAsset1Owner, sign(newAsset1Owner), certificate);

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

	private static void createKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
		Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
		Security.addProvider(bee2j);

		KeyPairGenerator bignKeyPairGenerator = KeyPairGenerator.getInstance("Bign","Bee2");
		KeyPair bignKeyPair =  bignKeyPairGenerator.generateKeyPair();
		privateKey = bignKeyPair.getPrivate();
		publicKey = bignKeyPair.getPublic();
		certificate = Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	private static String sign(String owner) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		Signature bignSignature = Signature.getInstance(JceNameConstants.BignWithBelt, JceNameConstants.ProviderName);
		bignSignature.initSign(privateKey);

		bignSignature.update(owner.getBytes());
		byte[] sig = bignSignature.sign();

		return Base64.getEncoder().encodeToString(sig);
	}

	private static void createInitialTransactions(Contract ctx) throws NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException, ContractException, InterruptedException, TimeoutException {
		createAsset(ctx, "asset1", "blue", 5, "Tomoko", 300);
		createAsset(ctx, "asset2", "red", 5, "Brad", 400);
		createAsset(ctx, "asset3", "green", 10, "Jin Soo", 500);
		createAsset(ctx, "asset4", "yellow", 10, "Max", 600);
		createAsset(ctx, "asset5", "black", 15, "Adrian", 700);
		createAsset(ctx, "asset6", "white", 15, "Michel", 700);
	}

	private static void createAsset(final Contract contract, final String assetID, final String color, final int size,
									final String owner, final int appraisedValue) throws NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException, ContractException, InterruptedException, TimeoutException {
		contract.submitTransaction("CreateAsset", assetID, color, Integer.toString(size), owner, Integer.toString(appraisedValue), sign(owner), certificate);
	}
}
