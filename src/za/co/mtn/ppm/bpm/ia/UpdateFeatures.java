package za.co.mtn.ppm.bpm.ia;

import java.io.IOException;
import java.util.ArrayList;

public class UpdateFeatures {
    // Variable to set the REST API URL
    private static final String REQ_REST_URL = "rest2/dm/requests";
    private static final String SQL_REST_URL = "rest2/sqlRunner/runSqlQuery";

    public static void main(String[] args) {
        // Verify that all Command Line Arguments has been submitted
//		log("Arguments length: " + args.length);
        if (args.length < 3) {
            log("The Class Command Line Arguments is incorrect!");
            printCommandLineArguments();
            System.exit(1);
        }
        // Assign parameters to variables for usage in methods
        log("**** Class Command Line Arguments****");
        // Base URL for PPM - Token: ENV_BASE_URL
        log("ENV_BASE_URL: " + args[0]);
        // REST API Username
        log("REST_USERNAME: " + args[1]);
        // IS PMO Impact Assessment Request ID
        log("IA_REQUEST_ID: " + args[3]);
        // IT Project ID
        log("IT_PROJECT_ID: " + args[4]);
        log("**** End of Class Command Line Arguments****");

        final String ppmBaseUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final String requestId = args[3];
        final String projectId = args[4];
        // Create new instances of ImpactAssessmentProcessor objects to be used in this class
        ImpactAssessmentProcessor iaProcessor = new ImpactAssessmentProcessor();
        try {
            log("<<-- Update PPM Feature Fields from IS PMO Impact Assessment -->>");
            log("<<- Get Impacted System Systems REST SQL Query ->>");
            ArrayList<ImpactedSystemValues> impactedSystemsData = iaProcessor.getIaImpactedSystemsDetailData(ppmBaseUrl, username, password, SQL_REST_URL, requestId);
            if (impactedSystemsData.isEmpty()) {
                log("No Impacted Systems captured in the IS PMO Impact Assessment table component.");
            } else {
                log("<<- Get PPM Features REST SQL Query ->>");
                ArrayList<FeatureValues> featuresLinkedToIaData = iaProcessor.getFeaturesLinkedToIaData(ppmBaseUrl, username, password, SQL_REST_URL, projectId);
                log("<<-- Generate the JSON for the PUT Request -->>");
                if (!featuresLinkedToIaData.isEmpty()) {
                    for (FeatureValues featuresLinkedToIa : featuresLinkedToIaData) {
                        String featureDomain = featuresLinkedToIa.getFeature_is_Domain();
                        log("Feature Number: " + featuresLinkedToIa.getFeature_request_id() + " and IS Domain: " + featureDomain);
                        // Variable to store the Impacted System Values for the domain
                        ArrayList<ImpactedSystemValues> domainImpactedSystemValues = new ArrayList<>();
                        // Iterate through the Impacted Systems for a domain
                        for (ImpactedSystemValues impactedSystems : impactedSystemsData) {
                            if (impactedSystems.getIsDomain().equalsIgnoreCase(featureDomain)) {
                                domainImpactedSystemValues.add(new ImpactedSystemValues(impactedSystems.getOctaneWorkspace(), impactedSystems.getIsDomain(), impactedSystems.getImpactedSystem(), impactedSystems.getInvolvement(), impactedSystems.getEstimateHours()));
                            }
                        }
                        // Update the PPM Feature Request Type
                        iaProcessor.updateFeatureRequestTypeImpactedSystemFields(ppmBaseUrl,username,password,REQ_REST_URL,featuresLinkedToIa.getFeature_request_id(),domainImpactedSystemValues);
                    }
                }


            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to write out the Command Line Arguments for this class
     */
    private static void printCommandLineArguments() {
        log("Command Line Arguments Layout: sc_ia_create_is_features <ENV_BASE_URL> <REST_USERNAME> <REST_USER_PASSWORD> <IA_REQUEST_ID> <IT_PROJECT_ID>");
        log("ENV_BASE_URL: args[0] (PPM Base URL)");
        log("REST_USERNAME: args[1] (PPM System User - ppmsysuser)");
        log("REST_USER_PASSWORD: args[2] (PPM System User Password)");
        log("IA_REQUEST_ID: args[3] (IS PMO Impact Assessment No)");
        log("IT_PROJECT_ID: args[3] (IS PMO Impact Assessment Linked IT Project Id)");
    }

    /**
     * Method to write out to the console or log file
     *
     * @param str String to print to console
     */
    private static void log(final String str) {
        System.out.println(str);
    }
}
