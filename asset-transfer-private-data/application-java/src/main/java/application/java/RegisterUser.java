/*
SPDX-License-Identifier: Apache-2.0
*/

package application.java;

import java.nio.file.Paths;
import java.util.Properties;

import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

public class RegisterUser {

	public static void register(RegistrationInfo registrationInfo, Organization organization) throws Exception {
		var userName = organization.getName();
		// Create a CA client for interacting with the CA.
		Properties props = new Properties();
		props.put("pemFile",
			"../../test-network/organizations/peerOrganizations/"+ registrationInfo.getDomain() +"/ca/ca."+ registrationInfo.getDomain() +"-cert.pem");
		props.put("allowAllHostNames", "true");
		HFCAClient caClient = HFCAClient.createNewInstance(registrationInfo.getCaClientUrl(), props);
		CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
		caClient.setCryptoSuite(cryptoSuite);

		// Create a wallet for managing identities
		Wallet wallet = Wallets.newFileSystemWallet(Paths.get("wallet"));

		// Check to see if we've already enrolled the user.
		if (wallet.get(userName) != null) {
			System.out.println("An identity for the user \"" + userName + "\" already exists in the wallet");
			return;
		}

		X509Identity adminIdentity = (X509Identity)wallet.get(registrationInfo.getAdminName());
		if (adminIdentity == null) {
			System.out.println(registrationInfo.getAdminName() + " needs to be enrolled and added to the wallet first");
			return;
		}
		User admin = new AdminUser(registrationInfo, adminIdentity);

		// Register the user, enroll the user, and import the new identity into the wallet.
		RegistrationRequest registrationRequest = new RegistrationRequest(userName);
		registrationRequest.setAffiliation(registrationInfo.getAffiliation());
		registrationRequest.setEnrollmentID(userName);
		for (var entry : organization.getAttributes().entrySet()) {
			registrationRequest.addAttribute(new Attribute(entry.getKey(), entry.getValue(), true));
		}

		String enrollmentSecret = caClient.register(registrationRequest, admin);
		Enrollment enrollment = caClient.enroll(userName, enrollmentSecret);

		X509Identity user = Identities.newX509Identity(registrationInfo.getMspId(), enrollment);
		wallet.put(userName, user);
		System.out.println("Successfully enrolled user \"" + userName +"\" and imported it into the wallet");
	}

}
