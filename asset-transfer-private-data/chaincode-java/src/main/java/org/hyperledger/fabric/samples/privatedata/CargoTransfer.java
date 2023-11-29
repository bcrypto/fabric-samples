package org.hyperledger.fabric.samples.privatedata;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Contract(
        name = "private",
        info = @Info(
                title = "Cargo Transfer",
                description = "The hyperlegendary cargo transfer",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html")))
@Default
public final class CargoTransfer implements ContractInterface {

    private final static String RoleAttributeName = "ROLE";
    private final static String ShipperRole = "SHIPPER";
    private final static String CarrierRole = "CARRIER";
    private final static String ReceiverRole = "RECEIVER";
    private final static String TransferPropertiesKey = "transfer_properties";
    private final static String ShipperCarrierCollectionName = "SHIPPER_CARRIER_COLLECTION";
    private final static String CarrierReceiverCollectionName = "CARRIER_RECEIVER_COLLECTION";

    private final Genson genson = new Genson();
    private int id = 0;

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReserveWaybill(final Context ctx)
    {
        ChaincodeStub stub = ctx.getStub();
        ClientIdentity user = getUser(stub);
        verifyUserRole(user, Role.SHIPPER);
        String ccc = getCCC(user);
        String gln = getGLN(user);
        String id = reserveWaybillId(user);

        return ccc + "-" + gln + "-" + id;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Waybill InitTransfer(final Context ctx, final String shipperSignature) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = ctx.getStub().getTransient();
        if (!transientMap.containsKey(TransferPropertiesKey)) {
            String errorMessage = String.format("InitTransfer call must specify transfer_properties in Transient map input");
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }
       /* ClientIdentity user;
        try {
            user = new ClientIdentity(stub);
        } catch (Exception e) {
            throw new ChaincodeException("Can't verify user");
        }

        var role = user.getAttributeValue(RoleAttributeName);
        if (role != ShipperRole) {
            throw new ChaincodeException("For this action Role must be " + ShipperRole + "but it was " + role);
        }*/

        String transientWaybillJSON = new String(transientMap.get(TransferPropertiesKey), UTF_8);
        final Waybill waybill;
        try {
            waybill = genson.deserialize(transientWaybillJSON, Waybill.class);
        } catch (Exception err) {
            String errorMessage = String.format("TransientMap deserialized error: %s ", err);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        //input validations
        String errorMessage = null;
        if (waybill.getCarrier() == null) {
            errorMessage = String.format("Empty input in Transient map: carrier");
        }
        if (waybill.getId().equals("")) {
            errorMessage = String.format("Empty input in Transient map: id");
        }
        if (waybill.getReceiver() == null) {
            errorMessage = String.format("Empty input in Transient map: receiver");
        }
        if (waybill.getShipper() == null) {
            errorMessage = String.format("Empty input in Transient map: shipper");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        boolean isSignatureValid = verifySignature(transientWaybillJSON, shipperSignature);
        if (!isSignatureValid) {
            errorMessage = String.format("Signature is invalid");
            throw new ChaincodeException(errorMessage, Errors.INVALID_SIGNATURE.toString());
        }

        // Verify that the client is submitting request to peer in their organization
        // This is to ensure that a client from another org doesn't attempt to read or
        // write private data from this peer.
        verifyClientOrgMatchesPeerOrg(ctx);

        // Make submitting client the owner
        waybill.setShipperSignature(shipperSignature);
        String serializedWaybill = genson.serialize(waybill);
        System.out.printf("InitTransfer Put: collection %s, ID %s\n", ShipperCarrierCollectionName, waybill.getId());
        System.out.printf("Put: collection %s, ID %s\n", ShipperCarrierCollectionName, serializedWaybill);
        stub.putPrivateData(ShipperCarrierCollectionName, waybill.getId(), serializedWaybill);

        return waybill;

    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Waybill AgreeByCarrier(final Context ctx, final String carrierSignature) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = ctx.getStub().getTransient();
        if (!transientMap.containsKey(TransferPropertiesKey)) {
            String errorMessage = String.format("AgreeByCarrier call must specify transfer_properties in Transient map input");
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        String transientWaybillJSON = new String(transientMap.get(TransferPropertiesKey), UTF_8);
        final Waybill waybill;
        try {
            waybill = genson.deserialize(transientWaybillJSON, Waybill.class);
        } catch (Exception err) {
            String errorMessage = String.format("TransientMap deserialized error: %s ", err);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        //input validations
        String errorMessage = null;
        if (waybill.getShipperSignature().equals("")) {
            errorMessage = String.format("Empty input in Transient map: shipperSignature");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        boolean isSignatureValid = verifySignature(transientWaybillJSON, carrierSignature);
        if (!isSignatureValid) {
            errorMessage = String.format("Signature is invalid");
            throw new ChaincodeException(errorMessage, Errors.INVALID_SIGNATURE.toString());
        }
        waybill.setCarrierSignature(carrierSignature);

        verifyClientOrgMatchesPeerOrg(ctx);

        // Save waybill to org collection
        System.out.printf("Put AssetPrivateDetails: collection %s, ID %s\n", CarrierReceiverCollectionName, waybill.getId());
        stub.putPrivateData(CarrierReceiverCollectionName, waybill.getId(), genson.serialize(waybill));

        return waybill;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Waybill AgreeByReceiver(final Context ctx, final String receiverSignature) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = ctx.getStub().getTransient();
        if (!transientMap.containsKey(TransferPropertiesKey)) {
            String errorMessage = String.format("AgreeByReceiver call must specify transfer_properties in Transient map input");
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        String transientWaybillJSON = new String(transientMap.get(TransferPropertiesKey), UTF_8);
        final Waybill waybill;
        try {
            waybill = genson.deserialize(transientWaybillJSON, Waybill.class);
        } catch (Exception err) {
            String errorMessage = String.format("TransientMap deserialized error: %s ", err);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        //input validations
        String errorMessage = null;
        if (waybill.getCarrierSignature().equals("")) {
            errorMessage = String.format("Empty input in Transient map: shipperSignature");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        boolean isSignatureValid = verifySignature(transientWaybillJSON, receiverSignature);
        if (!isSignatureValid) {
            errorMessage = String.format("Signature is invalid");
            throw new ChaincodeException(errorMessage, Errors.INVALID_SIGNATURE.toString());
        }
        waybill.setReceiverSignature(receiverSignature);

        verifyClientOrgMatchesPeerOrg(ctx);

        // Save waybill to org collection
        System.out.printf("Put AssetPrivateDetails: collection %s, ID %s\n", CarrierReceiverCollectionName, waybill.getId());
        // todo: export
        stub.delPrivateData(CarrierReceiverCollectionName, waybill.getId());
        stub.delPrivateData(ShipperCarrierCollectionName, waybill.getId());

        return waybill;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Waybill[] GetWaybillsByRange(final Context ctx, final String startKey, final String endKey) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        System.out.printf("GetWaybillByRange: start %s, end %s\n", startKey, endKey);

        List<Waybill> queryResults = new ArrayList<>();
        // retrieve waybill with keys between startKey(inclusive) and endKey(exclusive) in lexical order.
        try (QueryResultsIterator<KeyValue> results = stub.getPrivateDataByRange(getCollectionName(ctx), startKey, endKey)) {
            for (KeyValue result : results) {
                if (result.getStringValue() == null || result.getStringValue().length() == 0) {
                    System.err.printf("Invalid Waybill json: %s\n", result.getStringValue());
                    continue;
                }
                Waybill waybill = genson.deserialize(result.getStringValue(), Waybill.class);
                queryResults.add(waybill);
                System.out.println("QueryResult: " + waybill.toString());
            }
        }
        return queryResults.toArray(new Waybill[0]);
    }


    private static boolean verifySignature(String waybill, String signature) {
        return true;
    }

    private void verifyClientOrgMatchesPeerOrg(final Context ctx) {
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String peerMSPID = ctx.getStub().getMspId();

        if (!peerMSPID.equals(clientMSPID)) {
            String errorMessage = String.format("Client from org %s is not authorized to read or write private data from an org %s peer", clientMSPID, peerMSPID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INVALID_ACCESS.toString());
        }
    }

    private String getCollectionName(final Context ctx) {
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        if (clientMSPID.equals(ShipperRole)) {
            return ShipperCarrierCollectionName;
        } else if (clientMSPID.equals(CarrierRole)) {
            return ShipperCarrierCollectionName;
        } else if (clientMSPID.equals(ReceiverRole)) {
            return CarrierReceiverCollectionName;
        } else {
            String errorMessage = String.format("Client from org %s is not authorized to read or write private data", clientMSPID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INVALID_ACCESS.toString());
        }
    }

    private void verifyUserRole(ClientIdentity user, Role expectedRole) {
        var actualRole = user.getAttributeValue(RoleAttributeName);
        if (!actualRole.equals(expectedRole.toString())) {
            throw new ChaincodeException("For this action Role must be " + expectedRole + " but it was " + actualRole);
        }
    }

    private ClientIdentity getUser(ChaincodeStub stub){
        ClientIdentity user;
        try {
            user = new ClientIdentity(stub);
        } catch (Exception e) {
            throw new ChaincodeException("Can't verify user");
        }

        return user;
    }

    private String getCCC(ClientIdentity user){
        return user.getAttributeValue("CCC");
    }

    private String getGLN(ClientIdentity user){
        return user.getAttributeValue("GLN");
    }

    private String reserveWaybillId(ClientIdentity user){
        return "1";
    }

}
