package za.co.mtn.ppm.bpm.ia;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Class that updates IS PMO Feature and IS PMO Testing Feature Requests from the Impacted System table and IT Project Milestones.
 */
public class UpdateFeatures {
    // Variable to set the REST API URL
    private static final String REQ_REST_URL = "rest2/dm/requests";
    private static final String SQL_REST_URL = "rest2/sqlRunner/runSqlQuery";

    /**
     * Main method for the class
     *
     * @param args The following list of Arguments are required:
     *             ENV_BASE_URL: args[0] (PPM Base URL)
     *             REST_USERNAME: args[1] (PPM System User - ppmsysuser)
     *             REST_USER_PASSWORD: args[2] (PPM System User Password)
     *             IA_REQUEST_ID: args[3] (IS PMO Impact Assessment No)
     *             PROJECT_ID: args[4] (IT Project ID linked to the IS PMO Impact Assessment Request)
     */
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
            // Assign Impacted Systems table data to the ArrayList Object with the Impacted Systems table data
            ArrayList<ImpactedSystemValues> impactedSystemsData = iaProcessor.getIaImpactedSystemsTableData(ppmBaseUrl, username, password, SQL_REST_URL, requestId);
            // Check if Impacted Systems table is empty
            if (impactedSystemsData.isEmpty()) {
                log("No Impacted Systems captured in the IS PMO Impact Assessment table component.");
            } else {
                log("<<- Get PPM Features REST SQL Query ->>");
                // Assign PPM Features data to the ArrayList Object with the linked PPM Features
                ArrayList<FeatureValues> featuresLinkedToIaData = iaProcessor.getFeaturesLinkedToIaData(ppmBaseUrl, username, password, SQL_REST_URL, projectId);
                log("<<- Get IT Project Major Milestone REST SQL Query ->>");
                ArrayList<ProjectMilestoneValues> itProjectMajorMilestoneData = iaProcessor.getItProjectMilestoneData(ppmBaseUrl, username, password, SQL_REST_URL, projectId);
                log("<<-- Start Update PUT Request (IS PMO Feature(s) or IS PMO Testing Feature) -->>");
                // Check if the ArrayList Object with the linked PPM Features is empty
                if (!featuresLinkedToIaData.isEmpty()) {
                    // Iterate through the IS PMO Feature/IS PMO Testing Feature requests
                    for (FeatureValues featuresLinkedToIa : featuresLinkedToIaData) {
                        // Assign the PPM Feature, IS Domain to string variable
                        String featureDomain = featuresLinkedToIa.getFeature_is_Domain();
                        log("Feature Number: " + featuresLinkedToIa.getFeature_request_id() + " and IS Domain: " + featureDomain);
                        // Check Feature Domain equal to "Test Automation"
                        if (featureDomain.equalsIgnoreCase("Test Automation")) {
                            // Update the IS PMO Testing Feature Request Type
                            iaProcessor.updateFeatureRequestTypeImpactedSystemFields(ppmBaseUrl, username, password, REQ_REST_URL, featuresLinkedToIa.getFeature_request_id(), impactedSystemsData, impactedSystemsData, itProjectMajorMilestoneData);
                        } else {
                            // IS Domains no equal to "Test Automation"
                            // Variable to store the Impacted System Values for the domain
                            ArrayList<ImpactedSystemValues> domainImpactedSystemValues = new ArrayList<>();
                            // Iterate through the Impacted Systems for a domain
                            for (ImpactedSystemValues impactedSystems : impactedSystemsData) {
                                // Compare the Domain values and add to the variable
                                if (impactedSystems.getIsDomain().equalsIgnoreCase(featureDomain)) {
                                    domainImpactedSystemValues.add(new ImpactedSystemValues(impactedSystems.getOctaneWorkspace(), impactedSystems.getIsDomain(), impactedSystems.getImpactedSystem(), impactedSystems.getInvolvement(), impactedSystems.getEstimateHours()));
                                }
                            }
                            // Update the IS PMO Feature Request Type
                            iaProcessor.updateFeatureRequestTypeImpactedSystemFields(ppmBaseUrl, username, password, REQ_REST_URL, featuresLinkedToIa.getFeature_request_id(), domainImpactedSystemValues, impactedSystemsData, itProjectMajorMilestoneData);
                        }
                    }
                }
                log("<<-- End Update PUT Request (IS PMO Feature(s) or IS PMO Testing Feature) -->>");

            }
        } catch (IOException | ParseException e) {
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