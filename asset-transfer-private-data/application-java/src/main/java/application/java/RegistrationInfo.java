package application.java;

public class RegistrationInfo {
    private final String caClientUrl;
    private final String adminName;
    private final String mspId;
    private final String domain;
    private final String affiliation;
    private final String orgName;

    public RegistrationInfo(String caClientUrl, String adminName, String mspId, String domain, String affiliation, String orgName) {
        this.caClientUrl = caClientUrl;
        this.adminName = adminName;
        this.mspId = mspId;
        this.domain = domain;
        this.affiliation = affiliation;
        this.orgName = orgName;
    }

    public String getCaClientUrl() {
        return caClientUrl;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getMspId() {
        return mspId;
    }

    public String getDomain() {
        return domain;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getOrgName() {
        return orgName;
    }
}
