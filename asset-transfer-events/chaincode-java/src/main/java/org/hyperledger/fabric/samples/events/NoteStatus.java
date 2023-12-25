/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import org.json.JSONObject;

@DataType()
public final class NoteStatus {

    @Property()
    private final String noteID;

    @Property()
    private String shipper;

    @Property()
    private String reciever;

    @Property()
    private String status;

    public NoteStatus(final String ID, final String shipper, final String reciever, final String status) {
        noteID = ID;
        this.shipper = shipper;
        this.reciever = reciever;
        this.status = status;
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

     // Serialize asset without private properties
    public byte[] serialize() {
        return serialize(null).getBytes(UTF_8);
    }

    public String serialize(final String privateProps) {
        Map<String, Object> tMap = new HashMap();
        tMap.put("ID", noteID);
        tMap.put("Shipper",  shipper);
        tMap.put("Reciever",  reciever);
        tMap.put("Status",  status);
        if (privateProps != null && privateProps.length() > 0) {
            tMap.put("asset_properties", new JSONObject(privateProps));
        }
        return new JSONObject(tMap).toString();
    }

    public static NoteStatus deserialize(final byte[] assetJSON) {
        return deserialize(new String(assetJSON, UTF_8));
    }

    public static NoteStatus deserialize(final String assetJSON) {

        JSONObject json = new JSONObject(assetJSON);
        Map<String, Object> tMap = json.toMap();
        final String id = (String) tMap.get("ID");

        final String shipper = (String) tMap.get("Shipper");
        final String reciever = (String) tMap.get("Reciever");
        final String status = (String) tMap.get("Status");
        NoteStatus result = new NoteStatus(id, shipper, reciever, status);
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

        NoteStatus other = (NoteStatus) obj;

        return Objects.deepEquals(
                new String[]{getID(), getShipper(), getReciever(), getStatus()},
                new String[]{other.getID(), other.getShipper(), other.getReciever(), other.getStatus()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID(), getShipper(), getReciever(), getStatus());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + " [ID=" + noteID + ", shipper=" + shipper + ", reciever=" + reciever
                + ", status=" + status + "]";
    }
}
