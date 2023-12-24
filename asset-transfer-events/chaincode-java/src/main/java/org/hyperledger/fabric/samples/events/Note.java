/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import org.json.JSONObject;
import org.json.JSONArray;

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
    private ArrayList<String> advices;

    public Note(final String ID, final String shipper, final String reciever) {
        noteID = ID;
        this.shipper = shipper;
        this.reciever = reciever;
        advices = new ArrayList();
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
     * @return String return the asset
     */
    public String getAsset() {
        return asset;
    }

    /**
     * @param asset the asset to set
     */
    public void setAsset(final String assetValue) {
        asset = assetValue;
    }

    /**
     * @return String[] return the advices
     */
    public ArrayList<String> getAdvices() {
        return advices;
    }

    /**
     * @param advices the advices to set
     */
    public void setAdvices(final ArrayList<String> advicesList) {
        advices.clear();
        advices.addAll(advicesList);
    }

    /**
     * @param advices the advices to set
     */
    public void addAdvice(final String advice) {
        advices.add(advice);
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
        tMap.put("Advices", advices.toArray());
        if (privateProps != null && privateProps.length() > 0) {
            tMap.put("asset_properties", new JSONObject(privateProps));
        }
        return new JSONObject(tMap).toString();
    }

    public String export() {
        String result = "<DELNOTE>\n"
            + String.join("\n", advices)
            + "\n<ITEMS>\n" + asset + "\n</ITEMS>\n"
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
        Note result = new Note(id, shipper, reciever);
        if (tMap.containsKey("Asset")) {
            result.setAsset((String) tMap.get("Asset"));
        }
        if (tMap.containsKey("Advices")) {
            JSONArray array = json.getJSONArray("Advices");
            var list = new ArrayList<String>(array.length());
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
            result.setAdvices(list);
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
        return Objects.hash(getID(), getShipper(), getReciever(), getAsset(), String.join("\n", getAdvices()));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + " [ID=" + noteID + ", shipper=" + shipper + ", reciever=" + reciever
                + ", asset=" + asset + ", advices=<...>]";
    }


}
