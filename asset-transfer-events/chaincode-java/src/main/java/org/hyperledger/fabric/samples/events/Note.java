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
public final class Note {

    @Property()
    private final String noteID;

    @Property()
    private String shipper;

    @Property()
    private String reciever;

    @Property()
    private String asset;

    @Property()
    private String[] advices;

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
     * @param shipper the shipper to set
     */
    public void setShipper(String shipper) {
        this.shipper = shipper;
    }

    /**
     * @return String return the reciever
     */
    public String getReciever() {
        return reciever;
    }

    /**
     * @param reciever the reciever to set
     */
    public void setReciever(String reciever) {
        this.reciever = reciever;
    }

    /**
     * @return String return the asset
     */
    public String getAsset() {
        return asset;
    }

    /**
     * @param asset the asset to set
     */
    public void setAsset(String asset) {
        this.asset = asset;
    }

    /**
     * @return String[] return the advices
     */
    public String[] getAdvices() {
        return advices;
    }

    /**
     * @param advices the advices to set
     */
    public void setAdvices(String[] advices) {
        this.advices = advices;
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
        tMap.put("Asset", asset);
        tMap.put("Advices", advices);
        if (privateProps != null && privateProps.length() > 0) {
            tMap.put("asset_properties", new JSONObject(privateProps));
        }
        return new JSONObject(tMap).toString();
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
        Note result = new Note(id, shipper, reciever);
        if (tMap.containsKey("Asset")) {
            result.setAsset((String) tMap.get("Asset"));
        }
        if (tMap.containsKey("Advices")) {
            result.setAdvices((String[]) tMap.get("Advices"));
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
                new String[]{getID(), getShipper(), getReciever(), getAsset()},
                new String[]{other.getID(), other.getShipper(), other.getReciever(), other.getAsset()})
                &&
                Objects.deepEquals(getAdvices(), other.getAdvices());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID(), getShipper(), getReciever(), getAsset(), getAdvices());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + " [ID=" + noteID + ", shipper=" + shipper + ", reciever=" + reciever  
                + ", asset=" + asset + ", advices=<...>]";
    }


}
