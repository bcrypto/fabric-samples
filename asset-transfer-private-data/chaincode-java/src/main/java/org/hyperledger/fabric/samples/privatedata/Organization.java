package org.hyperledger.fabric.samples.privatedata;

import org.hyperledger.fabric.contract.annotation.DataType;

@DataType()
public final class Organization {
    private final String Name;
    private final String Address;
    private final String Contact;

    public Organization(String name, String address, String contact) {
        Name = name;
        Address = null;
        Contact = contact;
    }

    public String getName(){
        return Name;
    }

    public String getAddress() {
        return Address;
    }

    public String getContact() {
        return Contact;
    }
}
