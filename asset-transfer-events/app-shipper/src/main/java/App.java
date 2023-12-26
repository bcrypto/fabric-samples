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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import java.io.StringWriter;
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

	private final Network network;
	private final Contract contract;
	private final String assetId = "asset" + Instant.now().toEpochMilli();
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

    private String nodeToString(final Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
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

    private String loadXMLNode(final Document doc, final String xpath) {
        Node node = null;
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/DESADV/SG10";
            node = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e5) {
            e5.printStackTrace();
        }
        return nodeToString(node);
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
			// Prepare resources
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("desadv.xml").getFile());
            Document doc = loadXML(file);
            String expression = "/DESADV/SG10";
            String str = getStringFromDocument(doc);
            String goods = loadXMLNode(doc, expression);
			
			System.out.println("Press Enter to create Note");
			Scanner in = new Scanner(System.in);
			String name = in.nextLine();
 
			var firstBlockNumber = createAsset();

			System.out.println("Press Enter to load DESADV message");
			name = in.nextLine();

 			updateAsset(goods);
			addAdvice(str);

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

	private long createAsset() throws EndorseException, SubmitException, CommitStatusException {
		System.out.println("\n--> Submit transaction: CreateNote, " + assetId + " from 10 to 100");

		var commit = contract.newProposal("CreateNote")
				.addArguments(assetId, "10", "100")
				.build()
				.endorse()
				.submitAsync();

		var status = commit.getStatus();
		if (!status.isSuccessful()) {
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}

		System.out.println("\n*** CreateNote committed successfully");

		return status.getBlockNumber();
	}

	private void updateAsset(String asset) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: UpdateItems for " + assetId);

		//contract.submitTransaction("UpdateItems", assetId, asset);
		//var transaction = contract.createTransaction("UpdateItems");
		//var transientMap = new HashMap<String, byte[]>();
		//transientMap.put(PRIVATE_PROPS_KEY, asset.getBytes(UTF_8));
		//transaction.setTransient(transientMap);
		//transaction.submit();
		var commit = contract.newProposal("UpdateItems")
				.addArguments(assetId, "1")
				.putTransient(PRIVATE_PROPS_KEY, asset)
				.build()
				.endorse()
				.submitAsync();

		var status = commit.getStatus();
		if (!status.isSuccessful()) {
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}

		System.out.println("\n*** UpdateItems committed successfully");
	}

	private void addAdvice(String advice) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: AddAdvice for " + assetId);

		//contract.submitTransaction("AddAdvice", assetId, advice);
		//var transaction = contract.createTransaction("AddAdvice");
		//var transientMap = new HashMap<String, byte[]>();
		//transientMap.put(PRIVATE_PROPS_KEY, advice.getBytes(UTF_8));
		//transaction.setTransient(transientMap);
		//transaction.submit();
		var commit = contract.newProposal("AddAdvice")
				.addArguments(assetId, "2")
				.putTransient(PRIVATE_PROPS_KEY, advice)
				.build()
				.endorse()
				.submitAsync();

		var status = commit.getStatus();
		if (!status.isSuccessful()) {
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}

		System.out.println("\n*** AddAdvice committed successfully");
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

	private String getItems() throws EndorseException, SubmitException, CommitStatusException, CommitException {
		byte[] result = null;
		System.out.println("\n--> Evaluate transaction: ReadItems, " + assetId);
		try {
			result = contract.evaluateTransaction("ReadItems", assetId);
			System.out.println("\n*** ReadItems evaluated successfully");
		} catch (GatewayException e1) {
			System.out.println("\n*** ReadItems wasn't evaluated");
			e1.printStackTrace();
		}
		return new String(result, UTF_8);
	}

	private void deleteAsset() throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: DeleteNote, " + assetId);

		contract.submitTransaction("DeleteNote", assetId);

		System.out.println("\n*** DeleteNote committed successfully");
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

				if (event.getEventName().equals("DeleteNote")) {
					// Reached the last submitted transaction so break to close the iterator and stop listening for events
					break;
				}
			}
		}
	}
}
