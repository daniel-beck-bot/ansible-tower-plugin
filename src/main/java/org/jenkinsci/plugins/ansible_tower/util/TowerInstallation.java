package org.jenkinsci.plugins.ansible_tower.util;

/*
    This class represents a Tower installation
 */

import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import org.kohsuke.stapler.verb.POST;

import java.util.List;

public class TowerInstallation extends AbstractDescribableImpl<TowerInstallation> {
    private static final long getSerialVersionUID = 1L;

    private final String towerDisplayName;
    private final String towerURL;
    private final String towerCredentialsId;
    private final boolean towerTrustCert;
    private final boolean enableDebugging;

    @DataBoundConstructor
    public TowerInstallation(String towerDisplayName, String towerURL, String towerCredentialsId, boolean towerTrustCert, boolean enableDebugging) {
        this.towerDisplayName = towerDisplayName;
        this.towerCredentialsId = towerCredentialsId;
        this.towerURL = towerURL;
        this.towerTrustCert = towerTrustCert;
        this.enableDebugging = enableDebugging;
    }

    public String getTowerDisplayName() { return this.towerDisplayName; }
    public String getTowerURL() { return this.towerURL; }
    public String getTowerCredentialsId() { return this.towerCredentialsId; }
    public boolean getTowerTrustCert() { return this.towerTrustCert; }
    public boolean getEnableDebugging() { return this.enableDebugging; }

    public TowerConnector getTowerConnector() {
        return TowerInstallation.getTowerConnectorStatic(this.towerURL, this.towerCredentialsId, this.towerTrustCert, this.enableDebugging);
    }

    public static TowerConnector getTowerConnectorStatic(String towerURL, String towerCredentialsId, boolean trustCert, boolean enableDebugging) {
        String username = null;
        String password = null;
        String oauth_token = null;
        if(StringUtils.isNotBlank(towerCredentialsId)) {
            List<StandardUsernamePasswordCredentials> credsList = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class);
            for(StandardUsernamePasswordCredentials creds : credsList) {
                if(creds.getId().equals(towerCredentialsId)) {
                    username = creds.getUsername();
                    password = creds.getPassword().getPlainText();
                }
            }
            List<StringCredentials> secretList = CredentialsProvider.lookupCredentials(StringCredentials.class);
            for(StringCredentials secret : secretList) {
                if(secret.getId().equals(towerCredentialsId)) {
                    oauth_token = secret.getSecret().getPlainText();
                }
            }
        }
        TowerConnector testConnector = new TowerConnector(towerURL, username, password, oauth_token, trustCert, enableDebugging);
        return testConnector;
    }

    @Extension
    public static class TowerInstallationDescriptor extends Descriptor<TowerInstallation> {

        // This requires a POST method to protect from CSFR
        @POST
        public FormValidation doTestTowerConnection(
                @QueryParameter("towerURL") final String towerURL,
                @QueryParameter("towerCredentialsId") final String towerCredentialsId,
                @QueryParameter("towerTrustCert") final boolean towerTrustCert,
                @QueryParameter("enableDebugging") final boolean enableDebugging
        ) {
            // Also, validate that we are an Administrator
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            TowerLogger.writeMessage("Starting to test connection with ("+ towerURL +") and ("+ towerCredentialsId +") and ("+ towerTrustCert +") with debugging ("+ enableDebugging +")");
            TowerConnector testConnector = TowerInstallation.getTowerConnectorStatic(towerURL, towerCredentialsId, towerTrustCert, enableDebugging);
            try {
                testConnector.testConnection();
                return FormValidation.ok("Success");
            } catch(Exception e) {
                return FormValidation.error(e.getMessage());
            }
        }

        // This requires a POST method to protect from CSFR
        @POST
        public ListBoxModel doFillTowerCredentialsIdItems(@AncestorInPath Project project) {
            // Also, validate that we are an Administrator
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            return new StandardListBoxModel().withEmptySelection().withMatching(
                    instanceOf(UsernamePasswordCredentials.class),
                    CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, project)
            ).withMatching(
                    instanceOf(StringCredentials.class),
                    CredentialsProvider.lookupCredentials(StringCredentials.class, project)
            );
        }

        @Override
        public String getDisplayName() {
            return "Tower Installation";
        }
    }
}


