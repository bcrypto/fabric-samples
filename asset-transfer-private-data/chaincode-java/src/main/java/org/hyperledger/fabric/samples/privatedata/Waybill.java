package org.hyperledger.fabric.samples.privatedata;

import org.hyperledger.fabric.contract.annotation.DataType;

@DataType()
public final class Waybill {
    private final String Id;
    private final Organization Shipper;
    private final Organization Carrier;
    private final Organization Receiver;

    private String ShipperSignature;
    private String CarrierSignature;
    private String ReceiverSignature;

    public Waybill(String id, Organization shipper, Organization carrier, Organization receiver) {
        Id = id;
        Shipper = shipper;
        Carrier = carrier;
        Receiver = receiver;
    }

    public String getId() {
        return Id;
    }

    public Organization getShipper() {
        return Shipper;
    }

    public Organization getCarrier() {
        return Carrier;
    }

    public Organization getReceiver() {
        return Receiver;
    }

    public String getShipperSignature() {
        return ShipperSignature;
    }

    public void setShipperSignature(String shipperSignature) {
        ShipperSignature = shipperSignature;
    }

    public String getCarrierSignature() {
        return CarrierSignature;
    }

    public void setCarrierSignature(String carrierSignature) {
        CarrierSignature = carrierSignature;
    }

    public String getReceiverSignature() {
        return ReceiverSignature;
    }

    public void setReceiverSignature(String receiverSignature) {
        ReceiverSignature = receiverSignature;
    }
}
