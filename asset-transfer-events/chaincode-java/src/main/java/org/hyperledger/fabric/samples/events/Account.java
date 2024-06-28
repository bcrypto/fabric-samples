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
public final class Account {

    @Property()
    private final String accountName;

    @Property()
    private final int ttnCount;

    @Property()
    private final int tnCount;

    public Account(final String accountName, final int ttnCount, final int tnCount) {
        this.accountName = accountName;
        this.ttnCount = ttnCount;
        this.tnCount = tnCount;
    }

    /**
     * @return String return the accountName
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * @return int return the ttnCount
     */
    public int getTTNCount() {
        return ttnCount;
    }

    /**
     * @return int return the tnCount
     */
    public int getTNCount() {
        return tnCount;
    }

    public String serialize() {
        Map<String, Object> tMap = new HashMap<String, Object>();
        tMap.put("accountName", accountName);
        tMap.put("ttnCount",  ttnCount);
        tMap.put("tnCount",  tnCount);
        return new JSONObject(tMap).toString();
    }

    public static Account deserialize(final byte[] assetJSON) {
        return deserialize(new String(assetJSON, UTF_8));
    }

    public static Account deserialize(final String assetJSON) {

        JSONObject json = new JSONObject(assetJSON);
        Map<String, Object> tMap = json.toMap();
        final String accountName = (String) tMap.get("accountName");

        final int ttnCount = (int) tMap.get("ttnCount");
        final int tnCount = (int) tMap.get("tnCount");
        Account result = new Account(accountName, ttnCount, tnCount);
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

        Account other = (Account) obj;

        return getAccountName().equals(other.getAccountName())
                && (getTNCount() == other.getTNCount())
                && (getTTNCount() == other.getTTNCount());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountName(), getTNCount(), getTTNCount());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
            + " [accountName=" + accountName + ", ttnCount=" + ttnCount
            + ", tnCount=" + tnCount + "]";
    }
}
