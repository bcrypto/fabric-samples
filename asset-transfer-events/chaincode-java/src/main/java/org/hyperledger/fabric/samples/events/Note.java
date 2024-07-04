/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONArray;
import org.json.JSONObject;

@DataType()
public final class Note {

    @Property()
    private final String noteID;

    @Property()
    private String shipper;

    @Property()
    private String reciever;

    @Property()
    private String status;

    @Property()
    private HashMap<String, String> messages;

    @Property()
    private List<XmlSignature> signatures;
    //private HashMap<String, String> signatures;

    public Note(final String ID, final String shipper, final String reciever) {
        noteID = ID;
        this.shipper = shipper;
        this.reciever = reciever;
        this.status = "0";
        messages = new HashMap<String, String>();
        signatures = new ArrayList<XmlSignature>();
    }

    /**
     * @return String return the noteID
     */
    public String getID() {
        return noteID;
    }

    /**
     * @return String return the shipper
     */
    public String getShipper() {
        return shipper;
    }

    /**
     * @param shipperName the shipper to set
     */
    public void setShipper(final String shipperName) {
        shipper = shipperName;
    }

    /**
     * @return String return the reciever
     */
    public String getReciever() {
        return reciever;
    }

    /**
     * @param recieverName the reciever to set
     */
    public void setReciever(final String recieverName) {
        this.reciever = recieverName;
    }

        /**
     * @param statusValue the status to set
     */
    public void setStatus(final String statusValue) {
        this.status = statusValue;
    }

    /**
     * @return String return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return HashMap<String, String> return the messages
     */
    public HashMap<String, String> getMessages() {
        return messages;
    }

    /**
     * @param msg the messages to set
     */
    public void setMessages(final Map<String, String> msg) {
        messages.clear();
        messages.putAll(msg);
    }


    /**
     * @return List<XmlSignature> return the signatures
     */
    public List<XmlSignature> getSignatures() {
        return signatures;
    }

    /**
     * @param signatures the signatures to set
     */
    public void setSignatures(final List<XmlSignature> sgn) {
        signatures.clear();
        signatures.addAll(sgn);
    }

    /**
     * @param id the message id
     * @param message the message to set
     */
    public void addMessage(final String id, final String message) {
        messages.put(id, message);
    }

    /**
     * @param id the signature id
     * @param signature the signature to set
     * @throws IOException
     */
    public void addSignature(final String signature) throws IOException {
        signatures.add(new XmlSignature(signature));
    }

    /**
     * @param id the signature id
     * @param signature the signature to set
     * @throws IOException
     */
    public void addSignature(final XmlSignature signature) throws IOException {
        signatures.add(signature);
    }

    /**
     * @param id the signature id
     * @param signature the signature to set
     * @throws IOException
     */
    public void addSignedMessage(final String id, final String message, final String signature) throws IOException {
        messages.put(id, message);
        addSignature(signature);
    }

     // Serialize asset without private properties
    public byte[] serialize() {
        return serialize(null).getBytes(UTF_8);
    }

    public String serialize(final String privateProps) {
        Map<String, Object> tMap = new HashMap<String, Object>();
        tMap.put("ID", noteID);
        tMap.put("Shipper",  shipper);
        tMap.put("Reciever",  reciever);
        tMap.put("Status",  status);
        tMap.put("Messages", new JSONObject(messages));
        tMap.put("Signatures", signatures.stream().map(x -> x.toJson()).toArray());
        if (privateProps != null && privateProps.length() > 0) {
            tMap.put("asset_properties", new JSONObject(privateProps));
        }
        return new JSONObject(tMap).toString();
    }

    public String export() {
        String result = "<DELNOTE>\n"
            + String.join("\n", messages.values()) + "\n"
            + signatures.stream().map(x -> x.getText()).reduce("\n", (x, y) -> String.join(x, y))
            + "</DELNOTE>";
        return result;
    }

    public static Note deserialize(final byte[] assetJSON) {
        return deserialize(new String(assetJSON, UTF_8));
    }

    public static Note deserialize(final String assetJSON) {

        JSONObject json = new JSONObject(assetJSON);
        Map<String, Object> tMap = json.toMap();
        final String id = (String) tMap.get("ID");

        final String shipper = (String) tMap.get("Shipper");
        final String reciever = (String) tMap.get("Reciever");
        final String status = (String) tMap.get("Status");
        Note result = new Note(id, shipper, reciever);
        result.setStatus(status);
        if (tMap.containsKey("Messages")) {
            JSONObject obj = json.getJSONObject("Messages");
            var list = new HashMap<String, String>(obj.length());
            obj.keySet().forEach(key -> {
                list.put(key, obj.getString(key));
            });
            result.setMessages(list);
        }
        if (tMap.containsKey("Signatures")) {
            JSONArray array = new JSONArray(json.getJSONArray("Signatures"));
            var list = new ArrayList<XmlSignature>(array.length());
            for (int i = 0; i < array.length(); i++) {
                list.add(XmlSignature.fromJson(array.getJSONObject(i)));
            }
            result.setSignatures(list);
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Note other = (Note) obj;

        return Objects.deepEquals(
            new String[]{getID(), getShipper(), getReciever()},
            new String[]{other.getID(), other.getShipper(), other.getReciever()})
            && Objects.deepEquals(getMessages(), other.getMessages())
            && Objects.deepEquals(getSignatures(), other.getSignatures());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID(), getShipper(), getReciever(), getMessages(), getSignatures());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + " [ID=" + noteID + ", shipper=" + shipper + ", reciever=" + reciever
                + ", status=" + status
                + ", messages=<...>, signatures=<...>]";
    }


}
