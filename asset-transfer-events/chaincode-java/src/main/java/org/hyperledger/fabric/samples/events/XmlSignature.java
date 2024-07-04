package org.hyperledger.fabric.samples.events;

import org.w3c.dom.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;

@DataType()
public final class XmlSignature {

    @Property()
    private final String text;

    @Property()
    private final String reference;

    public XmlSignature(final String signature) throws IOException {
        this.text = signature;
        Document doc = XmlUtils.loadXML(signature);
        this.reference = XmlUtils.getDsigReference(doc);
    }

    private XmlSignature(final String signature, final String reference) {
        this.text = signature;
        this.reference = reference;
    }

    public String getText() {
        return text;
    }

    public String getReference() {
        return reference;
    }

    public JSONObject toJson() {
        Map<String, Object> tMap = new HashMap<String, Object>();
        tMap.put("Text", this.text);
        tMap.put("Reference",  this.reference);
        return new JSONObject(tMap);
    }

    public String serialize() {
        return toJson().toString();
    }

    public static XmlSignature fromJson(final JSONObject json) {
        Map<String, Object> tMap = json.toMap();
        final String text = (String) tMap.get("Text");

        final String reference = (String) tMap.get("Reference");
        return new XmlSignature(text, reference);
    }

    public static XmlSignature deserialize(final String string) {
        JSONObject json = new JSONObject(string);
        return fromJson(json);
    }
}
