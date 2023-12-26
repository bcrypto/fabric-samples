package org.hyperledger.fabric.samples.privatedata;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

public class Input {

    private static final String IdAttributeKey = "id";

    public static void markIdAttribute(Node node) {
        var attrs = node.getAttributes();
        if (attrs != null && attrs.getLength() != 0)
        {
            var attr = (Attr) attrs.item(0);
            if (attr != null && attr.getName().equals(IdAttributeKey))
            {
                attr.getOwnerElement().setIdAttribute(IdAttributeKey, true);
            }
        }

        var nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                markIdAttribute(currentNode);
            }
        }
    }
}
