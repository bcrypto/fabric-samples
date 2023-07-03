/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.*;
import java.util.*;

import by.bcrypto.bee2j.constants.JceNameConstants;
import by.bcrypto.bee2j.provider.Bee2SecurityProvider;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

@Contract(
        name = "basic",
        info = @Info(
                title = "Asset Transfer",
                description = "The hyperlegendary asset transfer",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Adrian Transfer",
                        url = "https://hyperledger.example.com")))
@Default
public final class AssetTransfer implements ContractInterface {

    private final Genson genson = new Genson();

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        INVALID_SIGNATURE,
    }

    private static final String FnpCertificatePath = Paths.get("out", "fnp", "cert.der").toString();
    private static final String NpCertificatePath = Paths.get("out", "np", "cert.der").toString();
    private static final String Ca0CertificatePath = Paths.get("out", "ca0", "cert.der").toString();
    private static final String Ca0CrlPath = Paths.get("out", "ca0", "crl1.der").toString();
    private static final String Ca1CertificatePath = Paths.get("out", "ca1", "cert.der").toString();
    private static final String Ca1CrlPath = Paths.get("out", "ca1", "crl1.der").toString();

    private static ArrayList<X509Certificate> certificateStorage;
    private static X509Certificate ca0Certificate;
    private static X509Certificate ca1Certificate;

    /**
     * Creates some initial assets on the ledger.
     *
     * @param ctx the transaction context
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
        Security.addProvider(bee2j);

        // Do nothing because it's not valid operation is such scenario
       /* CreateAsset(ctx, "asset1", "blue", 5, "Tomoko", 300);
        CreateAsset(ctx, "asset2", "red", 5, "Brad", 400);
        CreateAsset(ctx, "asset3", "green", 10, "Jin Soo", 500);
        CreateAsset(ctx, "asset4", "yellow", 10, "Max", 600);
        CreateAsset(ctx, "asset5", "black", 15, "Adrian", 700);
        CreateAsset(ctx, "asset6", "white", 15, "Michel", 700);*/

    }

    /**
     * Creates a new asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the new asset
     * @param color the color of the new asset
     * @param size the size for the new asset
     * @param owner the owner of the new asset
     * @param appraisedValue the appraisedValue of the new asset
     * @return the created asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset CreateAsset(final Context ctx, final String assetID, final String color, final int size,
        final String owner, final int appraisedValue, final String signature, final String certificate) {
        ChaincodeStub stub = ctx.getStub();

        if (AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s already exists", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        boolean valid = false;
        try {
            valid = verifySignature(owner, signature, certificate);
        } catch (Exception e) {
            throw new ChaincodeException(AssetTransferErrors.INVALID_SIGNATURE.toString(), e);
        }

        if (!valid) {
            String errorMessage = String.format("Signature %s is wrong", signature);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_SIGNATURE.toString());
        }

        Asset asset = new Asset(assetID, color, size, owner, appraisedValue, signature);
        // Use Genson to convert the Asset into string, sort it alphabetically and serialize it into a json string
        String sortedJson = genson.serialize(asset);
        stub.putStringState(assetID, sortedJson);

        return asset;
    }

    /**
     * Retrieves an asset with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset
     * @return the asset found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Asset ReadAsset(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetID);

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset asset = genson.deserialize(assetJSON, Asset.class);
        return asset;
    }

    /**
     * Updates the properties of an asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being updated
     * @param color the color of the asset being updated
     * @param size the size of the asset being updated
     * @param owner the owner of the asset being updated
     * @param appraisedValue the appraisedValue of the asset being updated
     * @return the transferred asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset UpdateAsset(final Context ctx, final String assetID, final String color, final int size,
        final String owner, final int appraisedValue, final String signature, final String certificate) {
        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        boolean valid = false;
        try {
            valid = verifySignature(owner, signature, certificate);
        } catch (Exception e) {
            throw new ChaincodeException(AssetTransferErrors.INVALID_SIGNATURE.toString(), e);
        }

        if (!valid) {
            String errorMessage = String.format("Signature %s is wrong", signature);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_SIGNATURE.toString());
        }

        Asset newAsset = new Asset(assetID, color, size, owner, appraisedValue, signature);
        // Use Genson to convert the Asset into string, sort it alphabetically and serialize it into a json string
        String sortedJson = genson.serialize(newAsset);
        stub.putStringState(assetID, sortedJson);
        return newAsset;
    }

    /**
     * Deletes asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being deleted
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteAsset(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        stub.delState(assetID);
    }

    /**
     * Checks the existence of the asset on the ledger
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset
     * @return boolean indicating the existence of the asset
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean AssetExists(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetID);

        return (assetJSON != null && !assetJSON.isEmpty());
    }

    /**
     * Changes the owner of a asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being transferred
     * @param newOwner the new owner
     * @return the old owner
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String TransferAsset(final Context ctx, final String assetID, final String newOwner,
                                final String signature, final String certificate) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetID);

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset asset = genson.deserialize(assetJSON, Asset.class);
        boolean valid = false;

        try {
            valid = verifySignature(newOwner, signature, certificate);
        } catch (Exception e) {
            throw new ChaincodeException(AssetTransferErrors.INVALID_SIGNATURE.toString(), e);
        }

        if (!valid)
        {
            String errorMessage = String.format("Signature %s is wrong", signature);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_SIGNATURE.toString());
        }

        Asset newAsset = new Asset(asset.getAssetID(), asset.getColor(), asset.getSize(), newOwner, asset.getAppraisedValue(), signature);
        // Use a Genson to conver the Asset into string, sort it alphabetically and serialize it into a json string
        String sortedJson = genson.serialize(newAsset);
        stub.putStringState(assetID, sortedJson);

        return asset.getOwner();
    }

    /**
     * Retrieves all assets from the ledger.
     *
     * @param ctx the transaction context
     * @return array of assets found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllAssets(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<Asset> queryResults = new ArrayList<Asset>();

        // To retrieve all assets from the ledger use getStateByRange with empty startKey & endKey.
        // Giving empty startKey & endKey is interpreted as all the keys from beginning to end.
        // As another example, if you use startKey = 'asset0', endKey = 'asset9' ,
        // then getStateByRange will retrieve asset with keys between asset0 (inclusive) and asset9 (exclusive) in lexical order.
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

        for (KeyValue result: results) {
            Asset asset = genson.deserialize(result.getStringValue(), Asset.class);
            System.out.println(asset);
            queryResults.add(asset);
        }

        final String response = genson.serialize(queryResults);

        return response;
    }

    private boolean verifySignature(String owner, String signature, String serialNumber) throws InvalidKeyException, CertificateException, IOException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, CertPathValidatorException, InvalidAlgorithmParameterException, CertPathBuilderException, CRLException {
            Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
            Security.addProvider(bee2j);
            X509Certificate certificate = getCertificate(serialNumber);
            PublicKey publicKey = certificate.getPublicKey();
            Signature bignSignature = Signature.getInstance(JceNameConstants.BignWithBelt, JceNameConstants.ProviderName);
            bignSignature.initVerify(publicKey);
            bignSignature.update(owner.getBytes());

            return bignSignature.verify(Base64.getDecoder().decode(signature)) && verifyCertificate(certificate);
    }

    private X509Certificate getCertificate(String certificateSerialNumber) throws CertificateException, IOException, NoSuchProviderException {

        if (certificateStorage == null){
            certificateStorage = new ArrayList<>();
            certificateStorage.add(createCertificate(FnpCertificatePath));
            certificateStorage.add(createCertificate(NpCertificatePath));
            ca0Certificate = createCertificate(Ca0CertificatePath);
            ca1Certificate = createCertificate(Ca1CertificatePath);
        }

        return certificateStorage.stream().filter(x -> x.getSerialNumber().toString().equals(certificateSerialNumber)).findFirst().get();
    }

    private X509Certificate createCertificate(String path) throws CertificateException, NoSuchProviderException, IOException {
        byte[] certificate = this.getClass().getClassLoader().getResourceAsStream(path).readAllBytes();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "Bee2");
        ByteArrayInputStream is = new ByteArrayInputStream(certificate);

        return (X509Certificate) certificateFactory.generateCertificate(is);
    }

    private boolean verifyCertificate(X509Certificate certificate) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, CertPathBuilderException, CertPathValidatorException, CertificateException, CRLException, IOException {
        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        Set<TrustAnchor> trustAnchorSet = new HashSet<>();
        trustAnchorSet.add(new TrustAnchor(ca0Certificate, null));
        Set<X509Certificate> intermediateCerts = new HashSet<>();
        intermediateCerts.add(ca1Certificate);
        Set<X509CRL> crls = new HashSet<>();
        crls.add(createCrl(Ca0CrlPath));
        crls.add(createCrl(Ca1CrlPath));

        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(certificate);

        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchorSet, selector);
        // uncomment to skip revocation
        //pkixParams.setRevocationEnabled(false);
        CertStore intermediateCertStore = CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(intermediateCerts));
        pkixParams.addCertStore(intermediateCertStore);
        CertStoreParameters crlsCertStore = new CollectionCertStoreParameters(crls);
        pkixParams.addCertStore(CertStore.getInstance("Collection", crlsCertStore));


        PKIXRevocationChecker rc = (PKIXRevocationChecker)cpb.getRevocationChecker();
        rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS));
        CertPath certPath = cpb.build(pkixParams).getCertPath();
        validator.validate(certPath, pkixParams);

        return true;
    }

    private X509CRL createCrl(String path) throws IOException, CertificateException, CRLException {
        InputStream inStream = new ByteArrayInputStream(this.getClass().getClassLoader().getResourceAsStream(path).readAllBytes());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        return (X509CRL) cf.generateCRL(inStream);
    }
}
