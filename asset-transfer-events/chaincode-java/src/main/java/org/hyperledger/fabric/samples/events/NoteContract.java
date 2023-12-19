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

import java.util.Map;

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
    static final String PRIVATE_PROPS_KEY = "asset_properties";

    /**
     * Retrieves the asset details with the specified ID
     *
     * @param ctx     the transaction context
     * @param assetID the ID of the asset
     * @return the asset found on the ledger. Returns error if asset is not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadItems(final Context ctx, final String assetID) {
        System.out.printf("ReadItems: ID %s\n", assetID);

        Note note = getState(ctx, assetID);
        //String privData = readPrivateData(ctx, assetID);
        return note.getAsset();
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
        System.out.printf("ReadAsset: ID %s\n", assetID);

        Note note = getState(ctx, assetID);
        //String privData = readPrivateData(ctx, assetID);
        return note.export();
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
    public Note CreateNote(final Context ctx, final String assetID, final String shipper, final String reciever) {
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

        savePrivateData(ctx, assetID);
        assetJSON = asset.serialize();
        System.out.printf("CreateNote Put: ID %s Data %s\n", assetID, new String(assetJSON));

        stub.putState(assetID, assetJSON);
        // add Event data to the transaction data. Event will be published after the block containing
        // this transaction is committed
        stub.setEvent("CreateNote", assetJSON);
        return asset;
    }


    /**
     * AddAdvice adds advice
     *   Save any private data, if provided in transient map
     *
     * @param ctx the transaction context
     * @param assetID asset to delete
     * @param advice new advice for the note
     * @return none
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void AddAdvice(final Context ctx, final String assetID, final String advice) {
        ChaincodeStub stub = ctx.getStub();
        String errorMessage = null;

        if (assetID == null || assetID.equals("")) {
            errorMessage = "Empty input: assetID";
        }
        if (advice == null || advice.equals("")) {
            errorMessage = "Empty input: advice";
        }
        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }
        System.out.printf("AddAdvice: verify asset %s exists\n", assetID);
        Note thisAsset = getState(ctx, assetID);
        // Add advice
        thisAsset.addAdvice(advice);

        System.out.printf(" Add Advice: ID %s\n", assetID);
        savePrivateData(ctx, assetID); // save private data if any
        byte[] assetJSON = thisAsset.serialize();

        stub.putState(assetID, assetJSON);
        stub.setEvent("AddAdvice", assetJSON); //publish Event
    }

    /**
     * Update existing asset on the ledger with provided parameters.
     * Saves the passed private data (asset properties) from transient map input.
     *
     * @param ctx            the transaction context
     *                       Transient map with asset_properties key with asset json as value
     * @param assetID
     * @param asset
     * @return the created asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Note UpdateItems(final Context ctx, final String assetID, final String asset) {
        ChaincodeStub stub = ctx.getStub();
        // input validations
        String errorMessage = null;
        if (assetID == null || assetID.equals("")) {
            errorMessage = String.format("Empty input: assetID");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }
        // reads from the Statedb. Check if asset already exists
        Note note = getState(ctx, assetID);

        if (asset != null) {
            note.setAsset(asset);
        }

        //savePrivateData(ctx, assetID);
        byte[] assetJSON = note.serialize();
        System.out.printf("UpdateItems Put: ID %s Data %s\n", assetID, new String(assetJSON));
        stub.putState(assetID, assetJSON);
        stub.setEvent("UpdateItems", assetJSON); //publish Event
        return note;
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
        Note asset = getState(ctx, assetID);

        System.out.printf(" Delete Note:  ID %s\n", assetID);
        // delete private details of asset
        removePrivateData(ctx, assetID);
        stub.delState(assetID);         // delete the key from Statedb
        stub.setEvent("DeleteNote", asset.serialize()); // publish Event
    }

    private Note getState(final Context ctx, final String assetID) {
        byte[] assetJSON = ctx.getStub().getState(assetID);
        if (assetJSON == null || assetJSON.length == 0) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        try {
            Note asset = Note.deserialize(assetJSON);
            return asset;
        } catch (Exception e) {
            throw new ChaincodeException("Deserialize error: " + e.getMessage(), AssetTransferErrors.DATA_ERROR.toString());
        }
    }

    private String readPrivateData(final Context ctx, final String assetKey) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);
        String privData = null;
        // only if ClientOrgMatchesPeerOrg
        if (peerMSPID.equals(clientMSPID)) {
            System.out.printf(" ReadPrivateData from collection %s, ID %s\n", implicitCollectionName, assetKey);
            byte[] propJSON = ctx.getStub().getPrivateData(implicitCollectionName, assetKey);

            if (propJSON != null && propJSON.length > 0) {
                privData = new String(propJSON, UTF_8);
            }
        }
        return privData;
    }

    private void savePrivateData(final Context ctx, final String assetKey) {
        String peerMSPID = ctx.getStub().getMspId();
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String implicitCollectionName = getCollectionName(ctx);

        if (peerMSPID.equals(clientMSPID)) {
            Map<String, byte[]> transientMap = ctx.getStub().getTransient();
            if (transientMap != null && transientMap.containsKey(PRIVATE_PROPS_KEY)) {
                byte[] transientAssetJSON = transientMap.get(PRIVATE_PROPS_KEY);

                System.out.printf("Asset's PrivateData Put in collection %s, ID %s\n", implicitCollectionName, assetKey);
                ctx.getStub().putPrivateData(implicitCollectionName, assetKey, transientAssetJSON);
            }
        }
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
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String collectionName = IMPLICIT_COLLECTION_NAME_PREFIX + clientMSPID;
        return collectionName;
    }

    private enum AssetTransferErrors {
        INCOMPLETE_INPUT,
        INVALID_ACCESS,
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        DATA_ERROR
    }

}
