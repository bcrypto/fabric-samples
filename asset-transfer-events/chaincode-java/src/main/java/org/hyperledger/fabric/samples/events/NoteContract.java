/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.events;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import javax.xml.crypto.dsig.XMLSignatureException;

/**
 * Main Chaincode class.
 *
 * @see org.hyperledger.fabric.shim.Chaincode
 * <p>
 * Each chaincode transaction function must take, Context as first parameter.
 * Unless specified otherwise via annotation (@Contract or @Transaction), the contract name
 * is the class name (without package)
 * and the transaction name is the method name.
 */
@Contract(
        name = "note-events-java",
        info = @Info(
                title = "Note Events Contract",
                description = "The hyperlegendary note processing events sample",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Fabric Development Team",
                        url = "https://hyperledger.example.com")))
@Default
public final class NoteContract implements ContractInterface {

    static final String IMPLICIT_COLLECTION_NAME_PREFIX = "_implicit_org_";
    static final String COLLECTION_NAME = "shipper_reciever_collection";
    static final String PRIVATE_MSG_KEY = "edifact_message";
    static final String PRIVATE_XMLDSIG_KEY = "message_signature";
    final XmlDsig dsig;

    public NoteContract() {
        this.dsig = new XmlDsig();
    }
    /**
     * Retrieves the asset details with the specified ID
     *
     * @param ctx     the transaction context
     * @param assetID the ID of the asset
     * @return the asset found on the ledger. Returns error if asset is not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ExportNote(final Context ctx, final String assetID) {
        System.out.printf("ExportNote: ID %s\n", assetID);

        //NoteStatus note = getState(ctx, assetID);
        Note privData = readPrivateData(ctx, assetID);
        return privData.export();
    }

    /**
     * Creates a new asset on the ledger. Saves the passed private data (asset properties) from transient map input.
     *
     * @param ctx            the transaction context
     *                       Transient map with asset_properties key with asset json as value
     * @param assetID
     * @param shipper
     * @param reciever
     * @return the created note
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public NoteStatus CreateNote(final Context ctx, final String assetID, final String shipper, final String reciever) {
        ChaincodeStub stub = ctx.getStub();
        // input validations
        String errorMessage = null;
        if (assetID == null || assetID.equals("")) {
            errorMessage = String.format("Empty input: assetID");
        }
        if (shipper == null || shipper.equals("")) {
            errorMessage = String.format("Empty input: shipper");
        }
        if (reciever == null || reciever.equals("")) {
            errorMessage = String.format("Empty input: reciever");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }
        // Check if asset already exists
        byte[] assetJSON = ctx.getStub().getState(assetID);
        if (assetJSON != null && assetJSON.length > 0) {
            errorMessage = String.format("Note %s already exists", assetID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        Note asset = new Note(assetID, shipper, reciever);
        NoteStatus status = new NoteStatus(assetID, shipper, reciever, "empty");
        savePrivateData(ctx, assetID, asset);
        assetJSON = status.serialize();
        System.out.printf("CreateNote Put: ID %s Data %s\n", assetID, new String(assetJSON));

        stub.putState(assetID, assetJSON);

        if (isOperator(ctx)) {
            UpdateAccount(ctx, shipper, false);
        }
        // add Event data to the transaction data. Event will be published after the block containing
        // this transaction is committed
        stub.setEvent("CreateNote", assetJSON);
        return status;
    }


    /**
     * AddAdvice adds advice
     *   Save any private data, if provided in transient map
     *
     * @param ctx the transaction context
     * @param assetID asset to append data
     * @return none
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void AddAdvice(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String errorMessage = null;

        if (assetID == null || assetID.equals("")) {
            errorMessage = "Empty input: assetID";
        }
        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }
        System.out.printf("AddAdvice: verify asset %s exists\n", assetID);
        NoteStatus status = getState(ctx, assetID);
        //status.setStatus(message);
        // Add advice
        Note privData = readPrivateData(ctx, assetID);
        String asset = acceptPrivateData(ctx, PRIVATE_MSG_KEY);
        if (asset != null) {
            try {
                String id = XmlUtils.getMessageId(asset);
                privData.addMessage(id, asset);
                savePrivateData(ctx, assetID, privData);
            } catch (IOException e) {
                throw new ChaincodeException(errorMessage, AssetTransferErrors.DATA_ERROR.toString());
            }
        }

        System.out.printf(" Add Advice: ID %s\n", assetID);
        //savePrivateData(ctx, assetID); // save private data if any
        byte[] assetJSON = status.serialize();

        stub.putState(assetID, assetJSON);
        stub.setEvent("AddAdvice", assetJSON); //publish Event
    }


    /**
     * AddSignedAdvice adds advice and signature
     *   Save any private data, if provided in transient map
     *
     * @param ctx the transaction context
     * @param assetID asset to append data
     * @return none
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void AddSignedAdvice(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String errorMessage = null;

        if (assetID == null || assetID.equals("")) {
            errorMessage = "Empty input: assetID";
        }
        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }
        System.out.printf("AddSignedAdvice: verify asset %s exists\n", assetID);
        NoteStatus status = getState(ctx, assetID);
        //status.setStatus(message);
        // Add advice
        Note privData = readPrivateData(ctx, assetID);
        String asset = acceptPrivateData(ctx, PRIVATE_MSG_KEY);
        String signature = acceptPrivateData(ctx, PRIVATE_XMLDSIG_KEY);
        try {
            if (dsig.verify(asset, signature)) {
                String id = XmlUtils.getMessageId(asset);
                privData.addSignedMessage(id, asset, signature);
                savePrivateData(ctx, assetID, privData);
                System.out.printf(" Add Signed Advice: ID %s\n", assetID);
                byte[] assetJSON = status.serialize();
                stub.putState(assetID, assetJSON);
                stub.setEvent("AddSignedAdvice", assetJSON);
            }
        } catch (XMLSignatureException e) {
            System.out.printf("AddSignedAdvice: XML signature error\n");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.printf("AddSignedAdvice: XML reference error\n");
            e.printStackTrace();
        }
    }


    /**
     * AddSignature adds signature
     *   Save any private data, if provided in transient map
     *
     * @param ctx the transaction context
     * @param assetID asset to append data
     * @return none
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void AddSignature(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String errorMessage = null;

        if (assetID == null || assetID.equals("")) {
            errorMessage = "Empty input: assetID";
        }
        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }
        System.out.printf("AddSignature: verify asset %s exists\n", assetID);
        NoteStatus status = getState(ctx, assetID);
        //status.setStatus(message);
        // Add advice
        Note privData = readPrivateData(ctx, assetID);
        String signature = acceptPrivateData(ctx, PRIVATE_XMLDSIG_KEY);
        try {
            XmlSignature sign = new XmlSignature(signature);
            String asset = privData.getMessages().get(sign.getReference());
            if (dsig.verify(asset, signature)) {
                privData.addSignature(sign);
                savePrivateData(ctx, assetID, privData);
                System.out.printf(" Add Signature: ID %s\n", assetID);
                byte[] assetJSON = status.serialize();
                stub.putState(assetID, assetJSON);
                stub.setEvent("AddSignature", assetJSON);
            }
        } catch (XMLSignatureException e) {
            System.out.printf("AddSignature: XML signature error\n");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.printf("AddSignature: XML reference error\n");
            e.printStackTrace();
        }
    }

    /**
     * Deletes a asset & related details from the ledger.
     *
     * @param ctx the transaction context
     * @param assetID asset to delete
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteNote(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        System.out.printf("DeleteNote: verify asset %s exists\n", assetID);
        NoteStatus asset = getState(ctx, assetID);

        System.out.printf(" Delete Note:  ID %s\n", assetID);
        // delete private details of asset
        removePrivateData(ctx, assetID);
        stub.delState(assetID);         // delete the key from Statedb
        stub.setEvent("DeleteNote", asset.serialize()); // publish Event
    }

    private NoteStatus getState(final Context ctx, final String assetID) {
        byte[] assetJSON = ctx.getStub().getState(assetID);
        if (assetJSON == null || assetJSON.length == 0) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        try {
            NoteStatus asset = NoteStatus.deserialize(assetJSON);
            return asset;
        } catch (Exception e) {
            throw new ChaincodeException("Deserialize error: " + e.getMessage(), AssetTransferErrors.DATA_ERROR.toString());
        }
    }

    private Note readPrivateData(final Context ctx, final String assetKey) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);
        Note privData = null;
        // only if ClientOrgMatchesPeerOrg
        if (peerMSPID.equals(clientMSPID)) {
            System.out.printf(" ReadPrivateData from collection %s, ID %s\n", implicitCollectionName, assetKey);
            byte[] propJSON = ctx.getStub().getPrivateData(implicitCollectionName, assetKey);

            if (propJSON != null && propJSON.length > 0) {
                privData = Note.deserialize(propJSON);
            }
        }
        return privData;
    }

    private void savePrivateData(final Context ctx, final String assetKey, final Note note) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);

        if (peerMSPID.equals(clientMSPID)) {
            System.out.printf("Asset's PrivateData Put in collection %s, ID %s\n", implicitCollectionName, assetKey);
            ctx.getStub().putPrivateData(implicitCollectionName, assetKey, note.serialize());
        }
    }

    private String acceptPrivateData(final Context ctx, final String field) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        byte[] transientAsset = null;

        if (peerMSPID.equals(clientMSPID)) {
            Map<String, byte[]> transientMap = ctx.getStub().getTransient();
            if (transientMap != null && transientMap.containsKey(field)) {
                transientAsset = transientMap.get(field);
            }
        }
        return new String(transientAsset, UTF_8);
    }

    private void removePrivateData(final Context ctx, final String assetKey) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);

        if (peerMSPID.equals(clientMSPID)) {
            System.out.printf("PrivateData Delete from collection %s, ID %s\n", implicitCollectionName, assetKey);
            ctx.getStub().delPrivateData(implicitCollectionName, assetKey);
        }
    }

    // Return the implicit collection name, to use for private property persistance
    private String getCollectionName(final Context ctx) {
        // Get the MSP ID of submitting client identity
        //String clientMSPID = ctx.getClientIdentity().getMSPID();
        //String collectionName = IMPLICIT_COLLECTION_NAME_PREFIX + clientMSPID;
        return COLLECTION_NAME;
    }

    private boolean isOperator(final Context ctx) {
        String peerMSPID = ctx.getStub().getMspId();
        System.out.printf("peerMSPID: %s \n", peerMSPID);
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        System.out.printf("clientMSPID: %s \n", clientMSPID);
        return false;
    }

    private void UpdateAccount(final Context ctx, final String shipper, final boolean ttn) {
        Account account = null;
        String collection = "accounts";
        System.out.printf(" UpdateAccount from collection %s, ID %s\n", collection, shipper);
        byte[] propJSON = ctx.getStub().getPrivateData(collection, shipper);

        if (propJSON != null && propJSON.length > 0) {
            account = Account.deserialize(propJSON);
        } else {
            account = new Account(shipper, 0, 0);
        }
        int t = ttn ? 1 : 0;
        Account newValue = new Account(
            account.getAccountName(),
            account.getTTNCount() + t,
            account.getTNCount() + 1 - t);
        ctx.getStub().putPrivateData(collection, shipper, newValue.serialize());
    }

    /**
     * Retrieves all assets from the ledger.
     *
     * @param ctx the transaction context
     * @return array of assets found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetNoteList(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<String> queryResults = new ArrayList<String>();

        // To retrieve all assets from the ledger use getStateByRange with empty startKey & endKey.
        // Giving empty startKey & endKey is interpreted as all the keys from beginning to end.
        // As another example, if you use startKey = 'asset0', endKey = 'asset9' ,
        // then getStateByRange will retrieve asset with keys between asset0 (inclusive) and asset9 (exclusive) in lexical order.
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");
        for (KeyValue result: results) {
            queryResults.add(result.getKey());
        }
        final String response = String.join(" ", queryResults);
        return response;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetNotes(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");
        Map<String, Object> tMap = new HashMap<String, Object>();
        for (KeyValue result: results) {
            tMap.put(result.getKey(), result.getStringValue());
        }
        final String response = new JSONObject(tMap).toString();
        return response;
    }

    private enum AssetTransferErrors {
        INCOMPLETE_INPUT,
        INVALID_ACCESS,
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        DATA_ERROR
    }

}
