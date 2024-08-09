/*
 * Copyright BSU. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.crypto.dsig.XMLSignatureException;

import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.SubmitException;

import java.util.concurrent.TimeUnit;
import java.util.*;

public final class App {
 
	private static Properties prop;
	private static MainMenu menu;
	private static Client client;
	
	public static void main(final String[] args) throws Exception {
		String propFile = "app.properties";
		if (args.length > 0) {
			propFile = args[0];
			System.out.println("Config is loaded from " + propFile);
		} else {
			propFile = App.class.getClassLoader().getResource("app.properties").getFile();
		}
		prop = new Properties();
		try {
			prop.load(new FileInputStream(propFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("app.properties : " + propFile);
		prop.forEach((key, value) -> System.out.println("Key : " + key + ", Value : " + value));
	
		var grpcChannel = Connections.newGrpcConnection(prop);
		var builder = Gateway.newInstance()
				.identity(Connections.newIdentity(prop))
				.signer(Connections.newSigner(prop))
				.connection(grpcChannel)
				.evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

		try (var gateway = builder.connect()) {
			new App(gateway).run();
		} finally {
			grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	public App(final Gateway gateway) {
		client = new Client(gateway, prop);
		menu = new MainMenu(client);
	}

	public void run() throws IOException, XMLSignatureException, EndorseException, SubmitException, CommitStatusException, CommitException {
		
		//menu.show();
		menu.start();
/*		XmlSigner signer = new XmlSigner();
		signer.loadCertificate(prop.getProperty("bign.cert.path"));
		signer.loadPrivateKey(prop.getProperty("bign.key.path"), prop.getProperty("bign.key.pwd"));

		// Listen for events emitted by subsequent transactions, stopping when the try-with-resources block exits
		try (var eventSession = startChaincodeEventListening()) {
			// Prepare resources
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("desadv.xml").getFile());
            Document doc = XmlUtils.loadXML(file);
            String str = XmlUtils.getStringFromDocument(doc);
			String signature = signer.signDocument(doc, "#desadv1");
			
			System.out.println("Press Enter to create Note");
			Scanner in = new Scanner(System.in);
			String name = in.nextLine();
 
			var firstBlockNumber = createAsset();

			System.out.println("Press Enter to load DESADV message");
			name = in.nextLine();

			//addAdvice(str);
			//addAdvice(signature);
			addSignedAdvice(str, signature);

			System.out.println("Press Enter to export Note");
			name = in.nextLine();

			String delnote = getAsset();
			try{
				FileOutputStream outputStream = new FileOutputStream("./result.xml");
				outputStream.write(delnote.getBytes());
				outputStream.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			System.out.println("Press Enter to delete Note");
			name = in.nextLine();

			deleteAsset();

			System.out.println("Press Enter to see events history");
			name = in.nextLine();

			// Replay events from the block containing the first transaction
			replayChaincodeEvents(firstBlockNumber);
		} catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
			 */
	}
}
