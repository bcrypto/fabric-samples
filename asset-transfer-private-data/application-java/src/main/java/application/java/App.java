/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Running TestApp: 
// gradle runApp 

package application.java;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.owlike.genson.Genson;
import org.apache.commons.io.FileUtils;
import org.hyperledger.fabric.gateway.*;

public class App {

    private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
    private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "private");
    private final static String TransferPropertiesKey = "transfer_properties";
    private final static String ToSignByShipperId = "#toSignByShipper";
    private final static String ToSignByCarrierId = "#toSignByCarrier";
    private final static String ToSignByReceiverId = "#toSignByReceiver";
    private static final Genson genson = new Genson();
    private static Wallet wallet;

    static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
    }

    // helper function for getting connected to the gateway
    public static Gateway connect(String userName, RegistrationInfo registrationInfo) throws Exception {
        // Load a file system based wallet for managing identities.
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallets.newFileSystemWallet(walletPath);
        // load a CCP
        Path networkConfigPath = Paths.get("..", "..", "test-network", "organizations", "peerOrganizations", registrationInfo.getDomain(), "connection-" + registrationInfo.getOrgName() + ".json");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, userName).networkConfig(networkConfigPath).discovery(true);
        return builder.connect();
    }

    public static void main(String[] args) {
        try {
            var shipperOrganization = parseOrganization("shipper.json");
            var carrierOrganization = parseOrganization("carrier.json");
            var receiverOrganization = parseOrganization("receiver.json");
            var shipperRegistrationInfo = new RegistrationInfo("https://localhost:7054", shipperOrganization.getName() + "Admin", "Org1MSP", "org1.example.com", "org1.department1", "org1");
            var carrierRegistrationInfo = new RegistrationInfo("https://localhost:11054", carrierOrganization.getName() + "Admin", "OperatorMSP", "operator.by", "operator.department1", "operator");
            var receiverRegistrationInfo = new RegistrationInfo("https://localhost:8054", receiverOrganization.getName() + "Admin", "Org2MSP", "org2.example.com", "org2.department1", "org2");

            wallet = Wallets.newFileSystemWallet(Paths.get("wallet"));
            // Delete wallet if it exists from prior runs
            FileUtils.deleteDirectory(new File("wallet"));
            // shipper
            EnrollAdmin.enroll(shipperRegistrationInfo);
            RegisterUser.register(shipperRegistrationInfo, shipperOrganization);

            // carrier
            EnrollAdmin.enroll(carrierRegistrationInfo);
            RegisterUser.register(carrierRegistrationInfo, carrierOrganization);

            // receiver
            EnrollAdmin.enroll(receiverRegistrationInfo);
            RegisterUser.register(receiverRegistrationInfo, receiverOrganization);

            var shipperContract = getContract(shipperOrganization.getName(), shipperRegistrationInfo);
            var carrierContract = getContract(carrierOrganization.getName(), carrierRegistrationInfo);
            var receiverContract = getContract(receiverOrganization.getName(), receiverRegistrationInfo);

            // Register waybill
            var waybillId1 = registerWaybill(shipperContract);
            var waybillId2 = registerWaybill(shipperContract);

            // Init transfer
            var waybill = initTransfer(waybillId2, shipperOrganization.getGLN(), carrierOrganization.getGLN(), receiverOrganization.getGLN(), shipperContract);

            // Sign by carrier
            waybill = agreeByCarrier(waybill, carrierContract);

            // Sign by receiver
            waybill = agreeByReceiver(waybill, receiverContract);

            var lines = Collections.singletonList(waybill.getXmlData());
            var file = Paths.get("result.xml");
            Files.write(file, lines, StandardCharsets.UTF_8);

            System.out.println("Sample completed successfully");
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private static String registerWaybill(Contract shipperContract) throws Exception {
        byte[] result;
        System.out.println("Submit Transaction: Reserve waybill");
        result = shipperContract.submitTransaction("ReserveWaybill");
        var id = new String(result);
        System.out.println("Reserved waybill with id " + id);
        return id;
    }

    private static Contract getContract(String userName, RegistrationInfo registrationInfo) throws Exception {
        var gateway = connect(userName, registrationInfo);
        var network = gateway.getNetwork(CHANNEL_NAME);

        return network.getContract(CHAINCODE_NAME);
    }

    private static Organization parseOrganization(String jsonFileName) throws IOException {
        var bytes = App.class.getClassLoader().getResourceAsStream(jsonFileName).readAllBytes();
        return genson.deserialize(bytes, Organization.class);
    }

    private static Waybill initTransfer(String id, String shipper, String carrier, String receiver, Contract shipperContract) throws Exception {
        var xmlData = new String(App.class.getClassLoader().getResourceAsStream("waybill.xml").readAllBytes(), StandardCharsets.UTF_8);
        xmlData = signByShipper(xmlData);
        var waybill = new Waybill(id, shipper, carrier, receiver, xmlData);
        var waybillJson = genson.serialize(waybill);
        var waybillBytes = waybillJson.getBytes(StandardCharsets.UTF_8);
        System.out.println("Create Transaction: InitTransfer");
        var transaction = shipperContract.createTransaction("InitTransfer");
        var transientMap = new HashMap<String, byte[]>();
        transientMap.put(TransferPropertiesKey, waybillBytes);
        transaction.setTransient(transientMap);
        System.out.println("Submit Transaction: InitTransfer");
        var result = transaction.submit();
        System.out.println("InitTransfer: successful");

        return genson.deserialize(result, Waybill.class);
    }

    private static Waybill agreeByReceiver(Waybill waybill1, Contract receiverContract) throws Exception {
        var xmlData = waybill1.getXmlData();
        xmlData = signByReceiver(xmlData);
        var waybill = new Waybill(waybill1.getId(), waybill1.getShipperGLN(), waybill1.getCarrierGLN(), waybill1.getReceiverGLN(), xmlData);
        var waybillJson = genson.serialize(waybill);
        var waybillBytes = waybillJson.getBytes(StandardCharsets.UTF_8);
        System.out.println("Create Transaction: AgreeByReceiver");
        var transaction = receiverContract.createTransaction("AgreeByReceiver");
        var transientMap = new HashMap<String, byte[]>();
        transientMap.put(TransferPropertiesKey, waybillBytes);
        transaction.setTransient(transientMap);
        System.out.println("Submit Transaction: AgreeByReceiver");
        var result = transaction.submit();
        System.out.println("AgreeByReceiver: successful");

        return genson.deserialize(result, Waybill.class);
    }

    private static Waybill agreeByCarrier(Waybill waybill1, Contract carrierContract) throws Exception {
        var xmlData = waybill1.getXmlData();
        xmlData = signByCarrier(xmlData);
        var waybill = new Waybill(waybill1.getId(), waybill1.getShipperGLN(), waybill1.getCarrierGLN(), waybill1.getReceiverGLN(), xmlData);
        var waybillJson = genson.serialize(waybill);
        var waybillBytes = waybillJson.getBytes(StandardCharsets.UTF_8);
        System.out.println("Create Transaction: AgreeByCarrier");
        var transaction = carrierContract.createTransaction("AgreeByCarrier");
        var transientMap = new HashMap<String, byte[]>();
        transientMap.put(TransferPropertiesKey, waybillBytes);
        transaction.setTransient(transientMap);
        System.out.println("Submit Transaction: AgreeByCarrier");
        var result = transaction.submit();
        System.out.println("AgreeByCarrier: successful");

        return genson.deserialize(result, Waybill.class);
    }

    private static String signByShipper(String xmlData) throws Exception {
        var shipperIdentity = (X509Identity) wallet.get("shipper");
        return sign(xmlData, shipperIdentity, ToSignByShipperId);
    }

    private static String signByCarrier(String xmlData) throws Exception {
        var carrierIdentity = (X509Identity) wallet.get("carrier");
        return sign(xmlData, carrierIdentity, ToSignByCarrierId);
    }

    private static String signByReceiver(String xmlData) throws Exception {
        var receiverIdentity = (X509Identity) wallet.get("receiver");
        return sign(xmlData, receiverIdentity, ToSignByReceiverId);
    }

    private static String sign(String xmlData, X509Identity identity, String id) {
        try (InputStream xmlStream = new ByteArrayInputStream(xmlData.getBytes())) {
            var input = new Input(xmlStream);
            var xmlSigner = new XmlSigner(input, identity.getPrivateKey(), identity.getCertificate());
            xmlSigner.generateXMLSignature(id);
            var byteOutputStream = new ByteArrayOutputStream();
            input.print(byteOutputStream);
            xmlData = byteOutputStream.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return xmlData;
    }
}