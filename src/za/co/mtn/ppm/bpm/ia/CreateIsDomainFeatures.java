package za.co.mtn.ppm.bpm.ia;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateIsDomainFeatures {
    // Variable to be used updating IS PMO Impact Assessment Request Type
    static ArrayList<String> isDomainCreationList = new ArrayList<>();
    // Variable to set the REST API URL
    @SuppressWarnings("unused")
    private static final String REQ_REST_URL = "rest2/dm/requests";
    private static final String SQL_REST_URL = "rest2/sqlRunner/runSqlQuery";

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        // Verify that all Command Line Arguments has been submitted
//		log("Arguments length: " + args.length);
        if (args.length < 6) {
            log("The Class Command Line Arguments is incorrect!");
            printCommandLineArguments();
            System.exit(1);
        }
        // Assign parameters to variables for usage in methods
        log("**** Class Command Line Arguments****");
        // Base URL for PPM - Token: ENV_BASE_URL
        log("ENV BASE URL: " + args[0]);
        // REST API User
        log("PPM User: " + args[1]);
        // IS PMO Impact Assessment Request ID
        log("IA_REQUEST_ID: " + args[3]);
        // IT Project ID
        log("PROJECT_ID: " + args[4]);
        // IT Project Name
        log("PROJECT_NAME: " + args[5]);
        log("**** End of Class Command Line Arguments****");

        final String ppmBaseUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final String requestId = args[3];
        final String projectId = args[4];
        final String projectName = args[5];

        ImpactAssessmentProcessor iap = new ImpactAssessmentProcessor();
        try {
            log("<<-- Impacted System Domains in Impact Assessment -->>");
            log("<<- Get Impacted System Domains REST SQL ->>");
            ArrayList<String> iaDomainArray = iap.getImpactedSystemDomainsData(ppmBaseUrl, username, password,
                    SQL_REST_URL, iap.setImpactedSytemsDomainListSql(requestId));
            // Check if Impact Assessment's Impacted Systems has any Domains captured for
            // creating IS PMO Feature
            if (iaDomainArray.isEmpty()) {
                log("No Impacted Systems captured in the IS PMO Impact Assessment request."
                        .concat(" No IS PMO Features required while no Impacted Systems available."));
            } else {
                log("Impacted System Domain(s): " + iaDomainArray);
                log("<<-- Set PPM Feature Domains Array -->>");
                ArrayList<String> featureDomainArray = iap.getFeatureDomainsData(ppmBaseUrl, username, password,
                        SQL_REST_URL, iap.setFeatureDomainListSql(requestId));
                log("Feature Domain Array List: " + featureDomainArray.toString());
                ArrayList<String> domianCreationList = iap.getFeatureCreatDomianList(iaDomainArray, featureDomainArray);
                log("<<-- IS PPM Feature Domains to be Created -->>");
                log("Domians list: " + domianCreationList.toString());
                log("<<-- IT Project Data -->>");
                HashMap<String, String> itProjectInformation = iap.getItProjectData(ppmBaseUrl, username, password,
                        SQL_REST_URL, iap.setItProjectInformationSql(projectId));
                log("<<-- IT Project Release Data -->>");
                HashMap<String, String> itProjectReleaseInformation = iap.getItProjectReleaseData(ppmBaseUrl, username, password,
                        SQL_REST_URL, iap.setItProjectReleaseInformationSql(projectId));
                log("<<-- Testing JSON Payload -->>");
                JSONObject featureRequest = iap.setJsonObjectRequestType(requestId,projectId,projectName,"Billing CRM and OMS",itProjectInformation, itProjectReleaseInformation);
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printCommandLineArguments() {
        log("Command Line Arguments Layout: sc_update_impact_assessment <ENV_BASE_URL> <IA_REQUEST_ID> <IMPACTED_SYSTEM_DOMAINS> <PPM_FEATURE_DOMAINS> <COMPARE_INDICATOR>");
        log("ENV_BASE_URL: args[0] (PPM Base URL)");
        log("IA_REQUEST_ID: args[1] (IS PMO Impact Assessment No)");
        log("IMPACTED_SYSTEM_DOMAINS: args[2] (IS PMO Impact Assessment Request Impacted System Domains)");
        log("PPM_FEATURE_DOMAINS: args[3] (PPM Feature Domains that exit for the IS PMO Impact Assessment)");
        log("COMPARE_INDICATOR: args[4] (Compare Impacted Domains with Features Indicator)");
    }

    /**
     * Method to write out to the console or log file
     *
     * @param str
     */
    private static void log(final String str) {
        System.out.println(str);
    }
}