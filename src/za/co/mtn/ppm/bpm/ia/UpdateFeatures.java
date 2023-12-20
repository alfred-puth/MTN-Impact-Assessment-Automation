package za.co.mtn.ppm.bpm.ia;

import za.co.mtn.ppm.bpm.octane.OctaneFeatureOoProcessor;

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
     *             OO_BASE_URL: args[5] (OpenText OO application URL)
     *             OO_AUTH_KEY: args[6] (OpenText OO application Authentication Key)
     */
    public static void main(String[] args) {
        // Verify that all Command Line Arguments has been submitted
//		log("Arguments length: " + args.length);
        if (args.length < 5) {
            log("The Class Command Line Arguments is incorrect!");
            printCommandLineArguments();
            System.exit(1);
        }
        // Assign parameters to variables for usage in methods
        log("**** Class Command Line Arguments****");
        // Base URL for PPM
        log("ENV_BASE_URL: " + args[0]);
        // REST API Username
        log("REST_USERNAME: " + args[1]);
        // IS PMO Impact Assessment Request ID
        log("IA_REQUEST_ID: " + args[3]);
        // IT Project ID
        log("IT_PROJECT_ID: " + args[4]);
        // Base URL for OO
        log("OO_BASE_URL: " + args[5]);
        log("**** End of Class Command Line Arguments****");

        final String ppmBaseUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final String requestId = args[3];
        final String projectId = args[4];
        final String ooBaseUrl = args[5];
        final String ooAuthKey = args[6];
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
                log("<<-- Start Update PUT Request (IS PMO Feature(s) or IS PMO Testing Feature) -->>");
                // Check if the ArrayList Object with the linked PPM Features is empty
                if (!featuresLinkedToIaData.isEmpty()) {
                    // Iterate through the IS PMO Feature/IS PMO Testing Feature requests
                    for (FeatureValues featuresLinkedToIa : featuresLinkedToIaData) {
                        // Assign the PPM Feature, IS Domain to string variable
                        String featureDomain = featuresLinkedToIa.getFeatureIsDomain();
                        log("Feature Number: " + featuresLinkedToIa.getFeatureRequestId() + " and IS Domain: " + featureDomain);
                        log("<p style=\"float: left;\">");
                        // Check Feature Domain equal to "Test Automation"
                        if (featureDomain.equalsIgnoreCase("Test Automation")) {
                            // Update the IS PMO Testing Feature Request Type
                            iaProcessor.updateFeatureRequestTypeImpactedSystemFields(ppmBaseUrl, username, password, REQ_REST_URL, featuresLinkedToIa.getFeatureRequestId(), impactedSystemsData, impactedSystemsData);
                            // Update Octane Feature Impacted Systems through OpenText OO application if Octane Feature URL exists
                            String octFeatureUrl = featuresLinkedToIa.getOctaneFeatureUrl();
                            if (isNotBlankString(octFeatureUrl)) {
                                OctaneFeatureOoProcessor ooProcessor = new OctaneFeatureOoProcessor(ooBaseUrl, ooAuthKey, octFeatureUrl, featureDomain);
                                ooProcessor.updateOctaneFeatureImpactedSystems();
                            }
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
                            iaProcessor.updateFeatureRequestTypeImpactedSystemFields(ppmBaseUrl, username, password, REQ_REST_URL, featuresLinkedToIa.getFeatureRequestId(), domainImpactedSystemValues, impactedSystemsData);
                            // Update Octane Feature Impacted Systems through OpenText OO application if Octane Feature URL exists
                            String octFeatureUrl = featuresLinkedToIa.getOctaneFeatureUrl();
                            if (isNotBlankString(octFeatureUrl)) {
                                OctaneFeatureOoProcessor ooProcessor = new OctaneFeatureOoProcessor(ooBaseUrl, ooAuthKey, octFeatureUrl, featureDomain);
                                ooProcessor.updateOctaneFeatureImpactedSystems();
                            }

                        }
                        log("</p><br>");
                    }
                }
                log("<<-- End Update PUT Request (IS PMO Feature(s) or IS PMO Testing Feature) -->>");

            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to check if a String is Blank or Null
     *
     * @param string String for verification
     * @return Boolean (True or False)
     */
    private static boolean isNotBlankString(String string) {
        return string != null && !string.isEmpty() && !string.trim().isEmpty() && !string.equalsIgnoreCase("null");
    }

    /**
     * Method to write out the Command Line Arguments for this class
     */
    private static void printCommandLineArguments() {
        log("Command Line Arguments Layout: sc_ia_update_is_features <ENV_BASE_URL> <REST_USERNAME> <REST_USER_PASSWORD> <IA_REQUEST_ID> <IT_PROJECT_ID> <OO_BASE_URL> <OO_AUTH_KEY>");
        log("ENV_BASE_URL: args[0] (PPM Base URL)");
        log("REST_USERNAME: args[1] (PPM System User - ppmsysuser)");
        log("REST_USER_PASSWORD: args[2] (PPM System User Password)");
        log("IA_REQUEST_ID: args[3] (IS PMO Impact Assessment No)");
        log("IT_PROJECT_ID: args[4] (IS PMO Impact Assessment Linked IT Project Id)");
        log("OO_BASE_URL: args[5] (OO Environment Base URL)");
        log("OO_AUTH_KEY: args[6] (OO Authentication Key)");
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
