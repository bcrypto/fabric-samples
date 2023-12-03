package org.hyperledger.fabric.samples.privatedata;

import org.hyperledger.fabric.contract.annotation.DataType;

@DataType()
public final class Waybill {
    private String id;
    private String shipperGLN;
    private String carrierGLN;
    private String receiverGLN;
    private String xmlData;

    public Waybill(){}
    public Waybill(String id, String shipperGLN, String carrierGLN, String receiverGLN, String xmlData) {
        this.id = id;
        this.shipperGLN = shipperGLN;
        this.carrierGLN = carrierGLN;
        this.receiverGLN = receiverGLN;
        this.xmlData = xmlData;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getShipperGLN() {
        return shipperGLN;
    }
    public void setShipperGLN(String shipperGLN) {
        this.shipperGLN = shipperGLN;
    }

    public String getCarrierGLN() {
        return carrierGLN;
    }
    public void setCarrierGLN(String carrierGLN) {
        this.carrierGLN = carrierGLN;
    }

    public String getReceiverGLN() {
        return receiverGLN;
    }
    public void setReceiverGLN(String receiverGLN) {
        this.receiverGLN = receiverGLN;
    }

    public String getXmlData() { return xmlData; }
    public void setXmlData(String xmlData) {
        this.xmlData = xmlData;
    }
}
