package za.co.mtn.ppm.bpm.ia;

import org.json.JSONException;
import za.co.mtn.ppm.bpm.ismpo.project.IspmoItProjectProcessor;
import za.co.mtn.ppm.bpm.ismpo.project.ProjectMilestoneValues;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that create IS PMO Feature Requests from the Impacted System Domains in the IS PMO Impact Assessment.
 */
public class CreateIsDomainFeatures {
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
     *             PROJECT_NAME: args[5] (IT Project Name linked to the IS PMO Impact Assessment Request)
     *             IT_PROJECT_REQUEST_TYPE: args[6] (IT Project Request Type Name)");
     */
    public static void main(String[] args) {
        // Verify that all Command Line Arguments has been submitted
//		log("Arguments length: " + args.length);
        if (args.length < 7) {
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
        log("PROJECT_ID: " + args[4]);
        // IT Project Name
        log("PROJECT_NAME: " + args[5]);
        // IT Project Request Type
        log("IT_PROJECT_REQUEST_TYPE: " + args[6]);
        log("**** End of Class Command Line Arguments****");

        final String ppmBaseUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final String requestId = args[3];
        final String projectId = args[4];
        final String projectName = args[5];
        final String projectRequestType = args[6];
        // Create new instances of ImpactAssessmentProcessor objects to be used in this class
        ImpactAssessmentProcessor iaProcessor = new ImpactAssessmentProcessor();
        try {
            log("<<-- Impacted System Domains in Impact Assessment -->>");
            log("<<- Get Impacted System Domains REST SQL Query ->>");
            ArrayList<String> iaDomainArray = iaProcessor.getImpactedSystemDomainsData(ppmBaseUrl, username, password,
                    SQL_REST_URL, requestId);
            // Check if Impact Assessment's Impacted Systems has any Domains capture
            // creating IS PMO Feature
            if (iaDomainArray.isEmpty()) {
                log("No Impacted Systems captured in the IS PMO Impact Assessment request."
                        .concat(" No (New) IS PMO Features required while there are no Impacted Systems captured."));
            } else {
                log("Impacted System Domain(s): " + iaDomainArray);
                log("<<-- Set PPM Feature Domains Array -->>");
                ArrayList<String> featureDomainArray = iaProcessor.getFeatureDomainsData(ppmBaseUrl, username, password,
                        SQL_REST_URL, requestId);
                log("Feature Domain Array List: " + featureDomainArray.toString());
                ArrayList<String> domainCreationList = iaProcessor.getFeatureCreatDomianList(iaDomainArray, featureDomainArray);
                log("<<-- IS PPM Feature Domains to be Created -->>");
                // Check if Domain Ctreation List is empty
                if (domainCreationList.isEmpty()) {
                    log("No New Domains therefor no IS PMO Features to be created");
                } else {
                    log("Domains list: " + domainCreationList);
                    log("<<- Get IT Project Data REST SQL Query ->>");
                    HashMap<String, String> itProjectInformation = iaProcessor.getItProjectData(ppmBaseUrl, username, password, SQL_REST_URL, projectId, projectRequestType);
                    // Get the EPMO Project Info depending on the IT Project Rerquest Type
                    HashMap<String, String> epmoProjectInformation = new HashMap<>();
                    if (projectRequestType.equalsIgnoreCase("IS PMO IT-EPMO Project")) {
                        log("<<- Get EPMO Project Data REST SQL Query->>");
                        epmoProjectInformation = iaProcessor.getEpmoProjectData(ppmBaseUrl, username, password,
                                SQL_REST_URL, projectId);
                    }
                    log("<<- Get IT Project Milestone Data REST SQL Query->>");
                    // Create new instances of IspmoProjectMilestoneProcessor class
                    IspmoItProjectProcessor prjMil = new IspmoItProjectProcessor();
                    ArrayList<ProjectMilestoneValues> projectMilestoneArraylist = prjMil.getItProjectMilestoneData(ppmBaseUrl, username, password, SQL_REST_URL, itProjectInformation.get("ISPMO_PRJ_NUM"));
                    log("<<-- Create IS PMO Feature(s)  -->>");
                    Set<String> stringSet = new HashSet<>();
                    for (String domainList : domainCreationList) {
                        log("Domain List " + domainCreationList.indexOf(domainList) + ": " + domainList);
                        String newRequestId = iaProcessor.createIspmoFeatureRequest(ppmBaseUrl, username, password, REQ_REST_URL, requestId, projectName, domainList, projectRequestType, itProjectInformation, epmoProjectInformation, projectMilestoneArraylist);
                        log("Created IS PMO Feature Number:" + newRequestId);
                        // Add the Request IDs to the String Set
                        stringSet.add(newRequestId);
                    }
                    // Add the References is New IS PMO Features were created
                    if (stringSet.isEmpty()) {
                        log("No IS PMO Feature(s) Created:");
                    } else {
                        log("<<-- Create IS PMO Impact Assessment references to IS PMO Feature(s)  -->>");
                        // Assigning the String Set to a comma separated String
                        String referenceRequestIds = String.join(",", stringSet);
                        // Run methed to add references
                        iaProcessor.setRequestReference(ppmBaseUrl, username, password, REQ_REST_URL, requestId, referenceRequestIds, "CHILD");
                    }
                }

            }

        } catch (IOException | JSONException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to write out the Command Line Arguments for this class
     */
    private static void printCommandLineArguments() {
        log("Command Line Arguments Layout: sc_ia_create_is_features <ENV_BASE_URL> <REST_USERNAME> <REST_USER_PASSWORD> <IA_REQUEST_ID> <PROJECT_ID> <PROJECT_NAME> <IT_PROJECT_REQUEST_TYPE>");
        log("ENV_BASE_URL: args[0] (PPM Base URL)");
        log("REST_USERNAME: args[1] (PPM System User - ppmsysuser)");
        log("REST_USER_PASSWORD: args[2] (PPM System User Password)");
        log("IA_REQUEST_ID: args[3] (IS PMO Impact Assessment No)");
        log("PROJECT_ID: args[4] (IT Project ID linked to the IS PMO Impact Assessment Request)");
        log("PROJECT_NAME: args[5] (IT Project Name linked to the IS PMO Impact Assessment Request)");
        log("IT_PROJECT_REQUEST_TYPE: args[6] (IT Project Request Type Name)");
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