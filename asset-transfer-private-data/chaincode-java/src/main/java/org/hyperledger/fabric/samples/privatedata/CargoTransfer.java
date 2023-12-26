package org.hyperledger.fabric.samples.privatedata;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
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
    private final static String WaybillIdCollectionName= "WAYBILL_ID_COLLECTION";
    private final static String ShipperCarrierCollectionName = "SHIPPER_CARRIER_COLLECTION";
    private final static String CarrierReceiverCollectionName = "CARRIER_RECEIVER_COLLECTION";
    private final static String ToSignByShipperId = "#toSignByShipper";
    private final static String ToSignByCarrierId = "#toSignByCarrier";
    private final static String ToSignByReceiverId = "#toSignByReceiver";

    private final Genson genson = new Genson();

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String ReserveWaybill(final Context ctx)
    {
        ChaincodeStub stub = ctx.getStub();
        ClientIdentity user = getUser(stub);
        verifyClientOrgMatchesPeerOrg(ctx);
        verifyUserRole(user, Role.SHIPPER);
        String ccc = getCCC(user);
        String gln = getGLN(user);
        String id = reserveWaybillId(user, stub);

        return ccc + "-" + gln + "-" + id;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String InitTransfer(final Context ctx) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = ctx.getStub().getTransient();
        if (!transientMap.containsKey(TransferPropertiesKey)) {
            String errorMessage = String.format("InitTransfer call must specify transfer_properties in Transient map input");
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }
        ClientIdentity user;
        try {
            user = new ClientIdentity(stub);
        } catch (Exception e) {
            throw new ChaincodeException("Can't verify user");
        }

        verifyUserRole(user, Role.SHIPPER);

        String transientWaybillJSON = new String(transientMap.get(TransferPropertiesKey), UTF_8);
        final Waybill waybill;
        try {
            waybill = genson.deserialize(transientWaybillJSON, Waybill.class);
        } catch (Exception err) {
            String errorMessage = String.format("TransientMap deserialized error: %s ", err);
            System.err.println(errorMessage);
            System.err.println(transientWaybillJSON);
            throw new ChaincodeException(transientWaybillJSON, Errors.INCOMPLETE_INPUT.toString());
        }

        //input validations
        String errorMessage = null;
        if (waybill.getCarrierGLN() == null) {
            errorMessage = String.format("Empty input in Transient map: carrier");
        }
        if (waybill.getId().equals("")) {
            errorMessage = String.format("Empty input in Transient map: id");
        }
        if (waybill.getReceiverGLN() == null) {
            errorMessage = String.format("Empty input in Transient map: receiver");
        }
        if (waybill.getShipperGLN() == null) {
            errorMessage = String.format("Empty input in Transient map: shipper");
        }

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        boolean isSignatureValid = verifyShipperSignature(waybill.getXmlData());
        if (!isSignatureValid) {
            errorMessage = String.format("Signature is invalid");
            throw new ChaincodeException(errorMessage, Errors.INVALID_SIGNATURE.toString());
        }

        // Verify that the client is submitting request to peer in their organization
        // This is to ensure that a client from another org doesn't attempt to read or
        // write private data from this peer.
        verifyClientOrgMatchesPeerOrg(ctx);

        // Make submitting client the owner
        //waybill.setShipperSignature(shipperSignature);
        String serializedWaybill = genson.serialize(waybill);
        System.out.printf("InitTransfer Put: collection %s, ID %s\n", ShipperCarrierCollectionName, waybill.getId());
        System.out.printf("Put: collection %s, ID %s\n", ShipperCarrierCollectionName, serializedWaybill);
        stub.putPrivateData(ShipperCarrierCollectionName, waybill.getId(), serializedWaybill);

        return serializedWaybill;

    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String AgreeByCarrier(final Context ctx) throws Exception {
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

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        boolean isSignatureValid = verifyCarrierSignature(waybill.getXmlData());
        if (!isSignatureValid) {
            errorMessage = String.format("Signature is invalid");
            throw new ChaincodeException(errorMessage, Errors.INVALID_SIGNATURE.toString());
        }

        verifyClientOrgMatchesPeerOrg(ctx);

        // Save waybill to org collection
        var serializedWaybill = genson.serialize(waybill);
        System.out.printf("Put AssetPrivateDetails: collection %s, ID %s\n", CarrierReceiverCollectionName, waybill.getId());
        stub.putPrivateData(CarrierReceiverCollectionName, waybill.getId(), serializedWaybill);

        return serializedWaybill;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String AgreeByReceiver(final Context ctx) throws Exception {
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

        if (errorMessage != null) {
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, Errors.INCOMPLETE_INPUT.toString());
        }

        boolean isSignatureValid = verifyReceiverSignature(waybill.getXmlData());
        if (!isSignatureValid) {
            errorMessage = String.format("Signature is invalid");
            throw new ChaincodeException(errorMessage, Errors.INVALID_SIGNATURE.toString());
        }

        //verifyClientOrgMatchesPeerOrg(ctx);

        // Save waybill to org collection
        System.out.printf("Put AssetPrivateDetails: collection %s, ID %s\n", CarrierReceiverCollectionName, waybill.getId());
        // todo: export
        var serializedWaybill = genson.serialize(waybill);
        stub.delPrivateData(CarrierReceiverCollectionName, waybill.getId());
        stub.delPrivateData(ShipperCarrierCollectionName, waybill.getId());

        return serializedWaybill;
    }

    private static boolean verifyShipperSignature(String xmlSignature) throws Exception {
        return verifySignature(xmlSignature, ToSignByShipperId);
    }

    private static boolean verifyCarrierSignature(String xmlSignature) throws Exception {
        return verifySignature(xmlSignature, ToSignByCarrierId);
    }

    private static boolean verifyReceiverSignature(String xmlSignature) throws Exception {
        return verifySignature(xmlSignature, ToSignByReceiverId);
    }

    private static boolean verifySignature(String xmlSignature, String id) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlSignature.getBytes()));
        Input.markIdAttribute(doc);
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Cannot find Signature element");
        }
        var node = getNodeWithReference(nl, id);

        DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector(), node);

        XMLSignature signature = fac.unmarshalXMLSignature(valContext);

        return signature.validate(valContext);
    }

    private static Node getNodeWithReference(NodeList nodeList, String id) throws Exception {
        for (var i = 0; i < nodeList.getLength(); i++) {
            var node = nodeList.item(i);
            var reference = (Element) ((Element) node).getElementsByTagName("Reference").item(0);

            if (reference.getAttribute("URI").equals(id)) {
                return node;
            }
        }
        throw new Exception("Can't find signature with id= " + id);
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
        ChaincodeStub stub = ctx.getStub();
        ClientIdentity user = getUser(stub);
        var role = getRole(user);
        if (role.equals(ShipperRole)) {
            return ShipperCarrierCollectionName;
        } else if (role.equals(CarrierRole)) {
            return ShipperCarrierCollectionName;
        } else if (role.equals(ReceiverRole)) {
            return CarrierReceiverCollectionName;
        } else {
            String errorMessage = String.format("Client with role %s is not authorized to read or write private data", role);
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

    private String getRole(ClientIdentity user) {
        return user.getAttributeValue("ROLE");
    }

    private String getGLN(ClientIdentity user){
        return user.getAttributeValue("GLN");
    }

    private String reserveWaybillId(ClientIdentity user, ChaincodeStub stub) {
        var id = 0;
        var bytes = stub.getPrivateData(WaybillIdCollectionName, getGLN(user));
        if (bytes != null && bytes.length != 0) {
            id = Integer.parseInt(new String(bytes, UTF_8));
        }

        id++;
        var stringId = Integer.toString(id);
        stub.putPrivateData(WaybillIdCollectionName, getGLN(user), stringId.getBytes(UTF_8));

        return stringId;
    }
}
