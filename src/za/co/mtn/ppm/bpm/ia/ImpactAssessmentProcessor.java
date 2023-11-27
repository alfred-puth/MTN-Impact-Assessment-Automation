package za.co.mtn.ppm.bpm.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ImpactAssessmentProcessor {
    // Constant variables
    private static final String PRJ_URL = "project/ViewProject.do?projectId=";

    /**
     * Method to set the SQL string to be used for extracting the Domain list from
     * the Impacted Systems Table Component on the IS PMO Impact Assessment RT
     *
     * @param reqId IS PMO Impact Assessment Request ID
     * @return
     */
    protected String setImpactedSytemsDomainListSql(String reqId) {
        // Create the sql string
        String sql = "SELECT DISTINCT kl.visible_user_data1";
        sql = sql.concat(" FROM kcrt_table_entries kte").concat(
                        " INNER JOIN knta_parameter_set_fields kpsf ON kte.parameter_set_field_id = kpsf.parameter_set_field_id AND kpsf.parameter_token LIKE 'SYS_IMPACTED'")
                .concat(" INNER JOIN knta_lookups kl ON kte.parameter2 = kl.lookup_code AND kl.lookup_type LIKE 'MTN - IS Impacted Systems List'")
                .concat(" INNER JOIN knta_lookups kldom ON kl.user_data1 = kldom.lookup_code AND kldom.lookup_type LIKE 'MTN - IS Domains List' AND kldom.user_data1 IS NOT NULL");
        sql = sql.concat(" WHERE kte.request_id = ").concat(reqId);
        // Return the string
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the existing IS PMO
     * Features that are linked to the IS PMO Impact Assessment RT
     *
     * @param reqId IS PMO Impact Assessment Request ID
     * @return
     */
    protected String setFeatureDomainListSql(String reqId) {
        // Create the sql string
        String sql = "SELECT DISTINCT kl.meaning";
        sql = sql.concat(" FROM knta_references_v krv")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON krv.target_id = krhd.request_id")
                .concat(" INNER JOIN knta_lookups kl ON krhd.parameter2 = kl.meaning AND kl.lookup_type LIKE 'MTN - IS Domains List'");
        sql = sql.concat(" WHERE krv.reference_detail LIKE 'IS PMO Feature'").concat(" AND krv.source_entity_id = 20")
                .concat(" AND krv.source_id = ").concat(reqId);
        // Return the string
        return sql;
    }

    protected String setItProjectInformationSql(String prjId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.prj_project_id AS ispmo_prj_url, nvl(kfpp.prj_phase_meaning, 'null') AS ispmo_prj_phase, nvl(ks.status_name, 'null') AS ispmo_prj_status, CASE WHEN krt.reference_code = 'IS_PMO_IT_KTLO_PROJECT' THEN 'null' ELSE nvl(krd.visible_parameter3, 'null') END AS epmo_project_num, nvl(replace(kfpp.prj_project_manager_username, '#@#', '; '), 'null') AS ispmo_pm, nvl(initcap(ppr.overall_health_indicator), 'null') AS ispmo_prj_rag, nvl(kfpp.prj_business_unit_meaning, 'null') AS ispm_epmo_business_unit, nvl(krhd.visible_parameter1, 'null') AS ispmo_epmo_sub_area, CASE WHEN krt.reference_code = 'IS_PMO_IT_KTLO_PROJECT' THEN 'null' ELSE nvl(krhd.visible_parameter2, 'null') END AS ispmo_epmo_bu_priority, CASE WHEN krt.reference_code = 'IS_PMO_IT_KTLO_PROJECT' THEN 'null' ELSE nvl(krhd.visible_parameter3, 'null') END AS ispmo_epmo_org_priority, nvl(krt.request_type_name, 'null') AS ispmo_project_type, nvl(kr.description, 'null') AS ispmo_prj_short_desc";
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpp.request_type_id = krt.request_type_id")
                .concat(" INNER JOIN kcrt_requests kr ON kfpp.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_statuses ks ON kr.status_id = ks.status_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON kr.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON kr.request_id = pp.pfm_request_id")
                .concat(" INNER JOIN pm_project_rollup ppr ON pp.rollup_id = ppr.rollup_id")
                .concat(" LEFT OUTER JOIN kdrv_projects_v kp ON kfpp.prj_project_id = kp.pm_project_id AND parent_project_id = - 1");
        sql = sql.concat(" WHERE kfpp.prj_project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method that use the sqlRunner to get the Impacted System Domains from IS PMO
     * Impact Assessment RT.
     *
     * Use POST REST "rest2/sqlRunner/runSqlQuery" to return the data
     *
     * @param ppmBaseUrl - PPM Base URL for identifying the PPM environment
     * @param username   - PPM User for access to the PPM entities.
     * @param password   - PPM User password
     * @param restUrl    - REST API URL for the method
     * @param sqlString  - IS PMO Impact Assessment request ID
     * @return
     * @throws IOException
     * @throws JSONException
     */
    protected ArrayList<String> getImpactedSystemDomainsData(String ppmBaseUrl, String username, String password,
                                                             String restUrl, String sqlString) throws IOException, JSONException {
        // REST API URL
        URL sqlUrl = new URL(ppmBaseUrl + restUrl);
        log("POST Request Run SQL Query URL: " + sqlUrl.toString());
        // Encode the Username and Password. Using Admin user to ensure
        String encoding = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
        // Set the connection and all the parameters
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) sqlUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "basic " + encoding);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("Ephemeral", "true");
        connection.setDoOutput(true);
        // Use output stream to set the payload of the POST Request
        OutputStream restOutput = connection.getOutputStream();
        String jsonPayload = "{ \"querySql\": \"" + sqlString + "\"}";
        // Execute the Pay load, flush the output stream and close the stream
        restOutput.write(jsonPayload.getBytes());
        restOutput.flush();
        restOutput.close();
        // Get the Response from server for the GET REST Request done.
        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
        }
        // JSON Rerturn
        BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
        // Get the runSqlQuery Data.
        // JSONTokener - Set all the JSON keys as a token from the Json Return string.
        JSONTokener tokener = new JSONTokener(br);
        // Set the JSONObject from the JSONTokener
        JSONObject json = new JSONObject(tokener);
        log("JSON SQL Return output: " + json.toString());
        // Set the JSONArray with the "results" token Array List
        JSONArray jsonResults = new JSONArray(json.getString("results"));
        ArrayList<String> result = new ArrayList<>();
        if (jsonResults.length() != 0) {
            // Get the "values" token Array from the "results" token Array
            JSONArray jsonValues = new JSONArray(jsonResults.toString());
            for (int i = 0; i < jsonValues.length(); i++) {
                // Convert to an JSONObject to get the data Array
                JSONTokener tokenerVal = new JSONTokener(jsonValues.get(i).toString());
                JSONObject jsonVal = new JSONObject(tokenerVal);
                // Set the JSONArray with the "results" token Array List
                JSONArray jsonValue = new JSONArray(jsonVal.getString("values"));
                result.add(jsonValue.getString(0));
            }
        }
        // Disconnect the connection
        connection.disconnect();
        // Return data as JSONArray
        return result;
    }

    /**
     * Method that use the sqlRunner to get the existing IS PMO Feature RT(s) linked
     * to the IS PMO Impact Assessment
     *
     * Use POST REST "rest2/sqlRunner/runSqlQuery" to return the data
     *
     * @param ppmBaseUrl - PPM Base URL for identifying the PPM environment
     * @param username   - PPM User for access to the PPM entities.
     * @param password   - PPM User password
     * @param restUrl    - REST API URL for the method
     * @param sqlString  - IS PMO Impact Assessment request ID
     * @return
     * @throws IOException
     * @throws JSONException
     */
    protected ArrayList<String> getFeatureDomainsData(String ppmBaseUrl, String username, String password,
                                                      String restUrl, String sqlString) throws IOException, JSONException {
        // REST API URL
        URL sqlUrl = new URL(ppmBaseUrl + restUrl);
        log("POST Request Run SQL Query URL: " + sqlUrl.toString());
        // Encode the Username and Password. Using Admin user to ensure
        String encoding = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
        // Set the connection and all the parameters
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) sqlUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "basic " + encoding);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("Ephemeral", "true");
        connection.setDoOutput(true);
        // Use output stream to set the payload of the POST Request
        OutputStream restOutput = connection.getOutputStream();
        String jsonPayload = "{ \"querySql\": \"" + sqlString + "\"}";
        // Execute the Pay load, flush the output stream and close the stream
        restOutput.write(jsonPayload.getBytes());
        restOutput.flush();
        restOutput.close();
        // Get the Response from server for the GET REST Request done.
        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
        }
        // JSON Rerturn
        BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
        // Get the runSqlQuery Data.
        // JSONTokener - Set all the JSON keys as a token from the Json Return string.
        JSONTokener tokener = new JSONTokener(br);
        // Set the JSONObject from the JSONTokener
        JSONObject json = new JSONObject(tokener);
        log("JSON SQL Return output: " + json.toString());
        // Set the JSONArray with the "results" token Array List
        JSONArray jsonResults = new JSONArray(json.getString("results"));
        ArrayList<String> result = new ArrayList<>();
        if (jsonResults.length() != 0) {
            // Get the "values" token Array from the "results" token Array
            JSONArray jsonValues = new JSONArray(jsonResults.toString());
            for (int i = 0; i < jsonValues.length(); i++) {
                // Convert to an JSONObject to get the data Array
                JSONTokener tokenerVal = new JSONTokener(jsonValues.get(i).toString());
                JSONObject jsonVal = new JSONObject(tokenerVal);
                // Set the JSONArray with the "results" token Array List
                JSONArray jsonValue = new JSONArray(jsonVal.getString("values"));
                result.add(jsonValue.getString(0));
            }
        }
        // Disconnect the connection
        connection.disconnect();
        // Return data as JSONArray
        return result;
    }

    /**
     * Method that use the sqlRunner to get the IS PMO Impact Assessment linked
     * Project Information for the creation of the IS PMO Feature RT
     *
     * Use POST REST "rest2/sqlRunner/runSqlQuery" to return the data
     *
     * @param ppmBaseUrl - PPM Base URL for identifying the PPM environment
     * @param username   - PPM User for access to the PPM entities.
     * @param password   - PPM User password
     * @param restUrl    - REST API URL for the method
     * @param sqlString  - IS PMO Impact Assessment request ID
     * @return
     * @throws IOException
     * @throws JSONException
     */
    protected HashMap<String, String> getItProjectData(String ppmBaseUrl, String username, String password,
                                                       String restUrl, String sqlString) throws IOException, JSONException {
        // REST API URL
        URL sqlUrl = new URL(ppmBaseUrl + restUrl);
        log("POST Request Run SQL Query URL: " + sqlUrl.toString());
        // Encode the Username and Password. Using Admin user to ensure
        String encoding = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
        // Set the connection and all the parameters
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) sqlUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "basic " + encoding);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("Ephemeral", "true");
        connection.setDoOutput(true);
        // Use output stream to set the payload of the POST Request
        OutputStream restOutput = connection.getOutputStream();
        String jsonPayload = "{ \"querySql\": \"" + sqlString + "\"}";
        // Execute the Pay load, flush the output stream and close the stream
        restOutput.write(jsonPayload.getBytes());
        restOutput.flush();
        restOutput.close();
        // Get the Response from server for the GET REST Request done.
        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
        }
        // JSON Rerturn
        BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
        // Get the runSqlQuery Data.
        // JSONTokener - Set all the JSON keys as a token from the Json Return string.
        JSONTokener tokener = new JSONTokener(br);
        // Set the JSONObject from the JSONTokener
        JSONObject json = new JSONObject(tokener);
        log("JSON SQL Return output: " + json.toString());
        // Set the JSONArray with the "" token Array List
        JSONArray jsonColumnHeaders = new JSONArray(json.getString("columnHeaders"));
        // Set the JSONArray with the "results" token Array List
        JSONArray jsonResults = new JSONArray(json.getString("results"));
        // Set the "values" token Array from the "results" token Array
        JSONArray jsonValues = new JSONArray(jsonResults.toString());
        // Convert to an JSONObject to get the jsonValues Array
        JSONTokener tokenerVal = new JSONTokener(jsonValues.get(0).toString());
        JSONObject jsonVal = new JSONObject(tokenerVal);
        // Set the JSONArray with the "results" token Array List
        JSONArray jsonValue = new JSONArray(jsonVal.getString("values"));
        // Declare HashMap<String, String> result for the return result
        HashMap<String, String> result = new HashMap<>();
        // Add the jsonColumnHeaders as Keys and jsonValue as Values to the HashMap
        // Array
        // ISPMO_PRJ_NUM
        result.put(jsonColumnHeaders.getString(0), jsonValue.getString(0));
        // ISPMO_PRJ_URL
        result.put(jsonColumnHeaders.getString(1), ppmBaseUrl + PRJ_URL + jsonValue.getString(1));
        // ISPMO_PRJ_PHASE
        if (!jsonValue.getString(2).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(2), jsonValue.getString(2));
        }
        // ISPMO_PRJ_STATUS
        if (!jsonValue.getString(3).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(3), jsonValue.getString(3));
        }
        // EPMO_PROJECT_NUM
        if (!jsonValue.getString(4).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(4), jsonValue.getString(4));
        }
        // ISPMO_PM
        if (!jsonValue.getString(5).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(5), jsonValue.getString(5));
        }
        // ISPMO_PRJ_RAG
        if (!jsonValue.getString(6).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(6), jsonValue.getString(6));
        }
        // ISPM_EPMO_BUSINESS_UNIT
        if (!jsonValue.getString(7).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(7), jsonValue.getString(7));
        }
        // ISPMO_EPMO_SUB_AREA
        if (!jsonValue.getString(8).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(8), jsonValue.getString(8));
        }
        // ISPMO_EPMO_BU_PRIORITY
        if (!jsonValue.getString(9).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(9), jsonValue.getString(9));
        }
        // ISPMO_EPMO_ORG_PRIORITY
        if (!jsonValue.getString(10).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(10), jsonValue.get(10).toString());
        }
        // ISPMO_PROJECT_TYPE
        result.put(jsonColumnHeaders.getString(11), jsonValue.getString(11));
        // ISPMO_PRJ_SHORT_DESC
        if (!jsonValue.getString(12).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(12), jsonValue.getString(12));
        }

        // Disconnect the connection
        connection.disconnect();
        // Return Data as HashMap
        return result;
    }

    /**
     * Method to get the list of Domains that does not have a IS PMO Feature RT
     * created for the linked IS PMO Impact Assessment RT
     *
     * @param iaDomians      {@code ArrayList<String>}with Impact Assessment Domains
     * @param featureDomians {@code ArrayList<String>} with IS PMO Feature Domains
     * @return
     */
    protected ArrayList<String> getFeatureCreatDomianList(ArrayList<String> iaDomians,
                                                          ArrayList<String> featureDomians) {
        // Set return Array String List
        ArrayList<String> result = iaDomians;
        // Check if there are any IS Domains in the Impacted Systems table
        if (!result.isEmpty()) {
            result.removeAll(featureDomians);
            // Check if there are any Impacted Systems without PPM Features
            if (result.size() > 0) {
                // Sort list alphabetically to create features
                Collections.sort(iaDomians);
                result = iaDomians;
            }
        }
        return result;
    }

    protected JSONObject setJsonObjectRequestType(String iaRequestId, String iaProjectId, String iaProjectName, String iaIsDomain,
                                                  HashMap<String, String> itProjectData) {
        // Get the current date and time in "yyyy-MM-dd'T'HH:mm:ss" format" No need to
        // include include the micro seconds and timezone
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // Date of processing
        Date date = new Date();
        // Start with the required fields to create the IS POMO Feature
        // RT Token: LAST_UPDATE_DATE
        JSONObject tokensLastUpdateDateObj = new JSONObject();
        tokensLastUpdateDateObj.put("token", "REQ.LAST_UPDATE_DATE");
        tokensLastUpdateDateObj.put("dateValue", formatter.format(date).toString());
        // RT Token: ENTITY_LAST_UPDATE_DATE
        JSONObject tokenEntityLastUpdateDateObj = new JSONObject();
        tokenEntityLastUpdateDateObj.put("token", "REQ.ENTITY_LAST_UPDATE_DATE");
        tokenEntityLastUpdateDateObj.put("dateValue", formatter.format(date).toString());
        // RT Token: KNTA_MASTER_PROJ_REF
        JSONObject tokenItProjectNameObj = new JSONObject();
        tokenItProjectNameObj.put("token", "REQ.KNTA_MASTER_PROJ_REF");
        // Set the stringValue Array for the Impact Assessment Number
        JSONArray stringValueItProjectNameArray = new JSONArray();
        stringValueItProjectNameArray.put(iaProjectName);
        tokenItProjectNameObj.put("stringValue", stringValueItProjectNameArray);
        // RT Token: DESCRIPTION
        JSONObject tokenDescriptionObj = new JSONObject();
        tokenDescriptionObj.put("token", "REQ.DESCRIPTION");
        // Set the stringValue Array for the Description
        JSONArray stringValueDescriptionArray = new JSONArray();
        // Check if IT-KTLO or Other IT Projects for the IS PMO Feature Description
        if (itProjectData.containsKey("EPMO_PROJECT_NUM")) {
            // '(IS ' || kfpp.request_id || ') ' || kfpp.project_name || ' (EPMO ' ||
            // krd.visible_parameter3 || ') '
            String featureOtherDescription = "(IS ".concat(itProjectData.get("ISPMO_PRJ_NUM")).concat(") ")
                    .concat(iaProjectName).concat(" (EPMO ").concat(itProjectData.get("EPMO_PROJECT_NUM")).concat(")");
            stringValueDescriptionArray.put(featureOtherDescription);
        } else {
            // '(IS ' || kfpp.request_id || ') ' || kfpp.project_name
            String featureKtloDescription = "(IS ".concat(itProjectData.get("ISPMO_PRJ_NUM")).concat(") ")
                    .concat(iaProjectName);
            stringValueDescriptionArray.put(featureKtloDescription);
        }
        // Add the stringValue Array to the tokenDescriptionObj JSONObject
        tokenDescriptionObj.put("stringValue", stringValueDescriptionArray);
        // IMPACT_ASSESSMENT_NUM
        JSONObject tokenImpactAssessmentNumObj = new JSONObject();
        tokenImpactAssessmentNumObj.put("token", "REQD.IMPACT_ASSESSMENT_NUM");
        // Set the stringValue Array for the Impact Assessment Number
        JSONArray stringValueImpactAssessmentNumArray = new JSONArray();
        stringValueImpactAssessmentNumArray.put(iaRequestId);
        tokenImpactAssessmentNumObj.put("stringValue", stringValueImpactAssessmentNumArray);
        // IS_DOMAIN
        JSONObject tokenIsDomainObj = new JSONObject();
        tokenIsDomainObj.put("token", "REQ.IS_DOMAIN");
        // Set the stringValue Array for the Impact Assessment Number
        JSONArray stringValueIsDomainArray = new JSONArray();
        stringValueIsDomainArray.put(iaIsDomain);
        tokenIsDomainObj.put("stringValue", stringValueIsDomainArray);
        // End the required fields to create the IS POMO Feature
        // Start with the IT Project fields to create the IS POMO Feature from the
        // HashMap Array: itProjectData
        // ISPMO_PRJ_NUM
        JSONObject tokenIspmoPrjNumObj = new JSONObject();
        // Set the stringValue Array for the IT Project Number
        JSONArray stringValueIspmoPrjNumArray = new JSONArray();
        if (itProjectData.containsKey("ISPMO_PRJ_NUM")) {
            tokenIspmoPrjNumObj.put("token", "REQD.ISPMO_PRJ_NUM");
            stringValueIspmoPrjNumArray.put(itProjectData.get("ISPMO_PRJ_NUM"));
            tokenIspmoPrjNumObj.put("stringValue", stringValueIspmoPrjNumArray);
        }
        // ISPMO_PRJ_URL
        JSONObject tokenIspmoPrjUrlObj = new JSONObject();
        // Set the stringValue Array for the IT Project Number
        JSONArray stringValueIspmoPrjUrlArray = new JSONArray();
        if (itProjectData.containsKey("ISPMO_PRJ_URL")) {
            tokenIspmoPrjUrlObj.put("token", "REQD.ISPMO_PRJ_NUM");
            stringValueIspmoPrjUrlArray.put(itProjectData.get("ISPMO_PRJ_NUM"));
            tokenIspmoPrjUrlObj.put("stringValue", stringValueIspmoPrjUrlArray);
        }
        // Set the Field Array for the IS PMO Feature Request
        JSONArray fieldArray = new JSONArray();
        fieldArray.put(tokensLastUpdateDateObj);
        fieldArray.put(tokenEntityLastUpdateDateObj);
        fieldArray.put(tokenItProjectNameObj);
        fieldArray.put(tokenDescriptionObj);
        fieldArray.put(tokenDescriptionObj);
        fieldArray.put(tokenImpactAssessmentNumObj);
        fieldArray.put(tokenIsDomainObj);
        // Set the Field Object
        JSONObject fieldObj = new JSONObject();
        fieldObj.put("field", fieldArray);
        // Full JSON Object - Top Level
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("fields", fieldObj);
        jsonObj.put("sourceType", "INTERFACE_RI");
        jsonObj.put("requestType", "IS PMO Feature");
        log("JSON Payload Generated: " + jsonObj.toString());
//		if(tokenIspmoPrjNumObj.isEmpty()) {
//			log("II Project Number Object is Empty");
//		} else {
//			log("II Project Number Object is Not Empty");
//		}
        return null;
    }

    /**
     * Method to write out to the consul
     *
     * @param str String to write out
     */
    private static void log(final String str) {
        System.out.println(str);
    }
}
