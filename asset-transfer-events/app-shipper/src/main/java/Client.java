import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Properties;

import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.protos.peer.ChannelQueryResponse;
import org.hyperledger.fabric.protos.peer.ChannelInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Client {

    private static final String chaincodeName = "events";
	private static final String PRIVATE_MSG_KEY = "edifact_message";
    private static final String PRIVATE_XMLDSIG_KEY = "message_signature";

    private Network network;
	private Contract contract;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Gateway gateway;
    private String channelName = "mychannel";
    private Properties prop;

    public Client(final Gateway gateway, final Properties prop) {
		this.gateway = gateway;
        network = gateway.getNetwork(channelName);
		contract = network.getContract(chaincodeName);
	}

    public Properties getProperties() {
        return prop;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public Network getNetwork() {
        return network;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannel(String channel) {
        channelName = channel;
        network = gateway.getNetwork(channelName);
		contract = network.getContract(chaincodeName);
    }

    public List<ChannelInfo> getChannelList() {
        Contract contract = network.getContract("cscc");
        try {
            byte[] result = contract.evaluateTransaction("GetChannels");
            ChannelQueryResponse res = ChannelQueryResponse.parseFrom(result);
            return res.getChannelsList();
        } catch (GatewayException e) {
            e.printStackTrace();
        } catch (InvalidProtocolBufferException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
	private CloseableIterator<ChaincodeEvent> startChaincodeEventListening() {
		//System.out.println("\n*** Start chaincode event listening");

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

	public long createNote(String assetId) throws EndorseException, SubmitException, CommitStatusException {
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

	private void addAdvice(String assetId, String advice) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: AddAdvice for " + assetId);

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

	private void addSignedAdvice(String assetId, String advice, String signature) throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println("\n--> Submit transaction: AddSignedAdvice for " + assetId);

		var commit = contract.newProposal("AddSignedAdvice")
				.addArguments(assetId)
				.putTransient(PRIVATE_MSG_KEY, advice)
				.putTransient(PRIVATE_XMLDSIG_KEY, signature)
				.build()
				.endorse()
				.submitAsync();

		var status = commit.getStatus();
		if (!status.isSuccessful()) {
			throw new RuntimeException("failed to commit transaction with status code " + status.getCode());
		}

		System.out.println("\n*** AddSignedAdvice committed successfully");
	}

	private String getAsset(String assetId) throws EndorseException, SubmitException, CommitStatusException, CommitException {
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

	private void deleteAsset(String assetId) throws EndorseException, SubmitException, CommitStatusException, CommitException {
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
