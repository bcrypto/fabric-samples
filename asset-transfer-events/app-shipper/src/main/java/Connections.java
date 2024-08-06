/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

public final class Connections {

    private Connections() {
        // Private constructor to prevent instantiation
    }

    public static ManagedChannel newGrpcConnection(Properties prop) throws IOException {
        Path tlsCertPath = Paths.get(prop.getProperty("tls.cert.path"));
        String overrideAuth = prop.getProperty("gateway.auth");
        var credentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();
        return Grpc.newChannelBuilder(prop.getProperty("gateway.peer"), credentials)
                .overrideAuthority(overrideAuth)
                .build();
    }

    public static Identity newIdentity(Properties prop) throws IOException, CertificateException {
        Path certPath = Paths.get(prop.getProperty("cert.path"));
        var certReader = Files.newBufferedReader(certPath);
        var certificate = Identities.readX509Certificate(certReader);
        
        return new X509Identity(prop.getProperty("gateway.msp"), certificate);
    }

    public static Signer newSigner(Properties prop) throws IOException, InvalidKeyException {
        var path = Paths.get(prop.getProperty("key.path"));
        var keyReader = Files.newBufferedReader(path);
        var privateKey = Identities.readPrivateKey(keyReader);

        return Signers.newPrivateKeySigner(privateKey);
    }
}
