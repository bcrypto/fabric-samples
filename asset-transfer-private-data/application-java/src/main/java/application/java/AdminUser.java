package application.java;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

import java.security.PrivateKey;
import java.util.Set;

public class AdminUser implements User {

    private final RegistrationInfo registrationInfo;
    private final X509Identity adminIdentity;

    public AdminUser(RegistrationInfo registrationInfo, X509Identity adminIdentity){
        this.registrationInfo = registrationInfo;
        this.adminIdentity = adminIdentity;
    }
    @Override
    public String getName() {
        return registrationInfo.getAdminName();
    }

    @Override
    public Set<String> getRoles() {
        return null;
    }

    @Override
    public String getAccount() {
        return null;
    }

    @Override
    public String getAffiliation() {
        return registrationInfo.getAffiliation();
    }

    @Override
    public Enrollment getEnrollment() {
        return new Enrollment() {

            @Override
            public PrivateKey getKey() {
                return adminIdentity.getPrivateKey();
            }

            @Override
            public String getCert() {
                return Identities.toPemString(adminIdentity.getCertificate());
            }
        };
    }

    @Override
    public String getMspId() {
        return registrationInfo.getMspId();
    }

}
