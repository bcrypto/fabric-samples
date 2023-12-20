/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class App {
	private static final String channelName = "mychannel";
	private static final String chaincodeName = "events";

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

    private String loadXMLNode(Document doc, String xpath) {
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
			var firstBlockNumber = createAsset();
			File file = new File(classLoader.getResource("desadv.xml").getFile());
            Document doc = loadXML(file);
            String expression = "/DESADV/SG10";
            String str = getStringFromDocument(doc);
            String goods = loadXMLNode(doc, expression);
			updateAsset(goods);
			addAdvice(str);
			deleteAsset();

			// Replay events from the block containing the first transaction
			replayChaincodeEvents(firstBlockNumber);
		}
	}

	private CloseableIterator<ChaincodeEvent> startChaincodeEventListening() {
		System.out.println("\n*** Start chaincode event listening");

		var eventIter = network.getChaincodeEvents(chaincodeName);

		CompletableFuture.runAsync(() -> {
			eventIter.forEachRemaining(event -> {
				var payload = prettyJson(event.getPayload());
				System.out.println("\n<-- Chaincode event received: " + event.getEventName() + " - " + payload);
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

		System.out.println("\n*** CreateAsset committed successfully");

		return status.getBlockNumber();
	}

	private void updateAsset(String asset) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: UpdateItems for " + assetId);

		contract.submitTransaction("UpdateItems", assetId, asset);

		System.out.println("\n*** UpdateItems committed successfully");
	}

	private void addAdvice(String advice) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: AddAdvice for " + assetId);

		contract.submitTransaction("AddAdvice", assetId, advice);

		System.out.println("\n*** AddAdvice committed successfully");
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
