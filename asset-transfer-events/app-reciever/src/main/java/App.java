/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import java.io.StringWriter;
import java.io.StringReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.client.GatewayException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.*;

public final class App {
	private static final String channelName = "mychannel";
	private static final String chaincodeName = "events";
	static final String PRIVATE_PROPS_KEY = "asset_properties";
	private static final String PRIVATE_MSG_KEY = "edifact_message";
    private static final String PRIVATE_XMLDSIG_KEY = "message_signature";

	private final Network network;
	private final Contract contract;
	private String assetId = "asset" + Instant.now().toEpochMilli();
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    //method to convert Document to String
    public String getStringFromDocument(final Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private Document loadXML(final File file) throws FileNotFoundException {
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            doc = builder.parse(new FileInputStream(file));
        } catch (ParserConfigurationException e2) {
            e2.printStackTrace();
        } catch (SAXException e3) {
            e3.printStackTrace();
        } catch (IOException e4) {
            e4.printStackTrace();
        }
        return doc;
    }

	public static void main(final String[] args) throws Exception {
		var grpcChannel = Connections.newGrpcConnection();
		var builder = Gateway.newInstance()
				.identity(Connections.newIdentity())
				.signer(Connections.newSigner())
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
		network = gateway.getNetwork(channelName);
		contract = network.getContract(chaincodeName);
	}

	public void run() throws EndorseException, SubmitException, CommitStatusException, CommitException {
		// Listen for events emitted by subsequent transactions, stopping when the try-with-resources block exits
		try (var eventSession = startChaincodeEventListening()) {
			//prepare resources
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("recadv.xml").getFile());
            File file1 = new File(classLoader.getResource("recadv1.xml").getFile());
            File file2 = new File(classLoader.getResource("recadv2.xml").getFile());
            Document doc = loadXML(file);
            String str = getStringFromDocument(doc);
			String str1 = getStringFromDocument(loadXML(file1));
			String str2 = getStringFromDocument(loadXML(file2));
			String items;
			String input;
			String prompt = "1. Exit\n2. Scan events\n3. Show channel history";
			Scanner in = new Scanner(System.in);
			System.out.println(prompt);
			int action = in.nextInt();
			while (action != 1) {
				switch(action) {
					case 2:
						assetId = scanEvents(eventSession);
						System.out.println("Target Note " + assetId);

						System.out.println("Select 1(No QVR), 2(QVR) variant");
						action = in.nextInt();
						System.out.println("Press Enter to send recadv1 message");
						input = in.nextLine();
						input = in.nextLine();
						if(action == 1) {
							addAdvice(str);
						} else {
							addAdvice(str1);
							System.out.println("Press Enter to send recadv2 message");
							input = in.nextLine();
							addAdvice(str2);
						}
						System.out.println("Press Enter to export Note");
						input = in.nextLine();
			
						String delnote = getAsset();
						try{
							FileOutputStream outputStream = new FileOutputStream("./result.xml");
							outputStream.write(delnote.getBytes());
							outputStream.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					case 3:
						// Replay events from the block containing the first transaction
						replayChaincodeEvents(0);
						break;
					default:
						break;
				}
				System.out.println(prompt);
				action = in.nextInt();
			}
		} catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
	}

	private String getAsset() throws EndorseException, SubmitException, CommitStatusException, CommitException {
		byte[] result = null;
		System.out.println("\n--> Evaluate transaction: ExportNote, " + assetId);
		try {
			result = contract.evaluateTransaction("ExportNote", assetId);
			System.out.println("\n*** ExportNote evaluated successfully");
		} catch (GatewayException e1) {
			System.out.println("\n*** ExportNote wasn't evaluated");
			e1.printStackTrace();
		}
		
		return new String(result, UTF_8);
	}

	private CloseableIterator<ChaincodeEvent> startChaincodeEventListening() {
		//System.out.println("\n*** Start chaincode event listening");

		var eventIter = network.getChaincodeEvents(chaincodeName);

		CompletableFuture.runAsync(() -> {
			eventIter.forEachRemaining(event -> {
				var payload = prettyJson(event.getPayload());
				//System.out.println("\n<-- Chaincode event received: " + event.getEventName() + " - " + payload);
			});
		});

		return eventIter;
	}

	private String prettyJson(final byte[] json) {
		return prettyJson(new String(json, StandardCharsets.UTF_8));
	}

	private String prettyJson(final String json) {
		var parsedJson = JsonParser.parseString(json);
		return gson.toJson(parsedJson);
	}

	private void addAdvice(String advice) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: AddAdvice for " + assetId);

		//contract.submitTransaction("AddAdvice", assetId, advice);
		var commit = contract.newProposal("AddAdvice")
				.addArguments(assetId)
				.putTransient(PRIVATE_MSG_KEY, advice)
				.build()
				.endorse()
				.submitAsync();

		var status = commit.getStatus();
		if (!status.isSuccessful()) {
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}

		System.out.println("\n*** AddAdvice committed successfully");
	}


	private void replayChaincodeEvents(final long startBlock) {
		System.out.println("\n*** Start chaincode event replay");

		var request = network.newChaincodeEventsRequest(chaincodeName)
				.startBlock(startBlock)
				.build();

		try (var eventIter = request.getEvents()) {
			while (eventIter.hasNext()) {
				var event = eventIter.next();
				var payload = prettyJson(event.getPayload());
				System.out.println("\n<-- Chaincode event replayed: " + event.getEventName() + " - " + payload);

				// if (event.getEventName().equals("DeleteNote")) {
				// 	// Reached the last submitted transaction so break to close the iterator and stop listening for events
				// 	break;
				// }
			}
		}
	}

	private String scanEvents(final CloseableIterator<ChaincodeEvent> eventIter) {
		System.out.println("\n*** Start chaincode event scanning");
		while (eventIter.hasNext()) {
			var event = eventIter.next();
			var payload = prettyJson(event.getPayload());
			System.out.println("\n<-- Chaincode event accepted: " + event.getEventName() + " - " + payload);
			var status = JsonParser.parseString(payload).getAsJsonObject();

			if (event.getEventName().equals("AddSignedAdvice")) {
				if(status.get("Status").getAsString() == "2") {
					System.out.println("Note is awaiting for response " + status.get("ID").getAsString());
				}
				// Reached the last submitted transaction so break to close the iterator and stop listening for events
				return status.get("ID").getAsString();
			}
		}
		return null;
	}
}
