package za.co.mtn.ppm.bpm.ia;


import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import za.co.mtn.ppm.bpm.ismpo.project.IspmoItProjectProcessor;
import za.co.mtn.ppm.bpm.ismpo.project.ProjectMilestoneValues;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class that process the creation methods and updating methods of the IS PMO Feature Request from the IS PMO Impact Assessment Request
 */
public class ImpactAssessmentProcessor {
    // Constant variables for the class
    private static final String PRJ_URL = "project/ViewProject.do?projectId=";
    private static final int TEXT_AREA_HTML_MAX = 4000;

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
     * Method to write out to the consul
     *
     * @param str String to write out
     */
    private static void log(final String str) {
        System.out.println(str);
    }

    /**
     * Method to set the SQL string to be used for extracting the Domain list from the Impacted Systems Table Component on the IS PMO Impact Assessment Request
     *
     * @param reqId IS PMO Impact Assessment Request ID
     * @return SQL String with the created SQL statement
     */
    private String setImpactedSytemsDomainListSql(String reqId) {
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
     * Method to set the SQL string to be used for extracting the existing IS PMO Features Domains that are linked to the IS PMO Impact Assessment
     *
     * @param reqId IS PMO Impact Assessment Request ID
     * @return SQL String with the created SQL statement
     */
    private String setFeatureDomainListSql(String reqId) {
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

    /**
     * Method to set the SQL string to be used for extracting the IS PMO IT-EPMO Project Data that's linked to the IS PMO Impact Assessment
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setItEpmoProjectInformationIspmoFeatureSql(String prjId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.prj_project_id AS ispmo_prj_url, kfpp.prj_phase_meaning AS ispmo_prj_phase, ks.status_name AS ispmo_prj_status, krd.visible_parameter3 AS epmo_project_num, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_business_unit, krhd.visible_parameter1 AS ispmo_epmo_sub_area, krhd.visible_parameter2 AS ispmo_epmo_bu_priority, krhd.visible_parameter3 AS ispmo_epmo_org_priority, krt.request_type_name AS ispmo_project_type, kr.description AS ispmo_prj_short_desc, krd.visible_parameter11 AS ispmo_incl_retail_build, krd.visible_parameter12 AS ispmo_incl_charg_sys, krd.visible_parameter13 AS ispmo_incl_wholsal_rel, krd.visible_parameter14 AS ispmo_incl_siya_rel, krd.visible_parameter15 AS ispmo_incl_ilula_rel, krd.visible_parameter20 AS ispmo_incl_siebel_rel";
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpp.request_type_id = krt.request_type_id AND krt.reference_code = 'IS_PMO_IT_EPMO_PROJECT'")
                .concat(" INNER JOIN kcrt_requests kr ON kfpp.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_statuses ks ON kr.status_id = ks.status_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON kr.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON kr.request_id = pp.pfm_request_id")
                .concat(" INNER JOIN pm_project_rollup ppr ON pp.rollup_id = ppr.rollup_id");
        sql = sql.concat(" WHERE kfpp.prj_project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the IS PMO IT-Infrastructure Project Data that's linked to the IS PMO Impact Assessment
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setItInfrastructureProjectInformationTestingFeatureSql(String prjId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.prj_project_id AS ispmo_prj_url, kfpp.prj_phase_meaning AS ispmo_prj_phase, ks.status_name AS ispmo_prj_status, krd.visible_parameter3 AS epmo_project_num, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_business_unit, krhd.visible_parameter1 AS ispmo_epmo_sub_area, kr.description AS ispmo_prj_short_desc, krhd.visible_parameter25 AS ispmo_func_test_auto, krhd.visible_parameter38 AS ispmo_perf_test, krhd.visible_parameter39 AS ispmo_serv_virtual";
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpp.request_type_id = krt.request_type_id AND krt.reference_code = 'IS_PMO_IT_INFRASTRUCTURE_PROJECT'")
                .concat(" INNER JOIN kcrt_requests kr ON kfpp.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_statuses ks ON kr.status_id = ks.status_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON kr.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON kr.request_id = pp.pfm_request_id")
                .concat(" INNER JOIN pm_project_rollup ppr ON pp.rollup_id = ppr.rollup_id");
        sql = sql.concat(" WHERE kfpp.prj_project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the IS PMO IT-Reporting and Analytics Project Data that's linked to the IS PMO Impact Assessment
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setItReportingAnalyticsProjectInformationIspmoFeatureSql(String prjId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.prj_project_id AS ispmo_prj_url, kfpp.prj_phase_meaning AS ispmo_prj_phase, ks.status_name AS ispmo_prj_status, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_business_unit, krhd.visible_parameter1 AS ispmo_epmo_sub_area, krt.request_type_name AS ispmo_project_type, kr.description AS ispmo_prj_short_desc";
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpp.request_type_id = krt.request_type_id AND krt.reference_code = 'IS_PMO_IT_REPORTING_AND_ANALYTICS_PROJECT'")
                .concat(" INNER JOIN kcrt_requests kr ON kfpp.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_statuses ks ON kr.status_id = ks.status_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON kr.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON kr.request_id = pp.pfm_request_id")
                .concat(" INNER JOIN pm_project_rollup ppr ON pp.rollup_id = ppr.rollup_id");
        sql = sql.concat(" WHERE kfpp.prj_project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the IS PMO IT-KTLO Project Data that's linked to the IS PMO Impact Assessment
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setItKtloProjectInformationIspmoFeatureSql(String prjId) {
        // Create the sql string
        String sql = "SELECT kfpp.request_id AS ispmo_prj_num, kfpp.prj_project_id AS ispmo_prj_url, kfpp.prj_phase_meaning AS ispmo_prj_phase, ks.status_name AS ispmo_prj_status, replace(kfpp.prj_project_manager_username, '#@#', '; ') AS ispmo_pm, initcap(ppr.overall_health_indicator) AS ispmo_prj_rag, kfpp.prj_business_unit_meaning AS ispm_epmo_business_unit, krhd.visible_parameter1 AS ispmo_epmo_sub_area, krt.request_type_name AS ispmo_project_type, kr.description AS ispmo_prj_short_desc, krd.visible_parameter11 AS ispmo_incl_retail_build, krd.visible_parameter12 AS ispmo_incl_charg_sys, krd.visible_parameter13 AS ispmo_incl_wholsal_rel, krd.visible_parameter14 AS ispmo_incl_siya_rel, krd.visible_parameter15 AS ispmo_incl_ilula_rel, krd.visible_parameter20 AS ispmo_incl_siebel_rel";
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpp.request_type_id = krt.request_type_id AND krt.reference_code = 'IS_PMO_IT_KTLO_PROJECT'")
                .concat(" INNER JOIN kcrt_requests kr ON kfpp.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_statuses ks ON kr.status_id = ks.status_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_request_details krd ON kr.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON kr.request_id = pp.pfm_request_id")
                .concat(" INNER JOIN pm_project_rollup ppr ON pp.rollup_id = ppr.rollup_id");
        sql = sql.concat(" WHERE kfpp.prj_project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the EPMO Project Data that's linked to the IS PMO Impact Assessment
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setEpmoProjectInformationSql(String prjId) {
        // Create the sql string
        String sql = "SELECT pp.project_id AS ispmo_epmo_prj_url, pp.pfm_request_id AS ispmo_epmo_prj_num, nvl(pm_utils.get_project_manager_name_list(pp.project_id), 'null') AS ispmo_epmo_pm, nvl(ppt.project_type_name, 'null') AS ispmo_epmo_type";
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp")
                .concat(" INNER JOIN kcrt_request_details krd ON kfpp.request_id = krd.request_id AND krd.batch_number = 1")
                .concat(" INNER JOIN pm_projects pp ON krd.parameter3 = pp.project_id")
                .concat(" INNER JOIN pm_project_types ppt ON pp.project_id = ppt.project_id");
        sql = sql.concat(" WHERE kfpp.prj_project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the Impacted System Table data from the IS PMO Impact Assessment Request
     *
     * @param reqId IS PMO Impact Assessment Request ID
     * @return SQL String with the created SQL statement
     */
    private String setIaImpactedSystemsTableSql(String reqId) {
        // Create the sql string
        String sql = "SELECT nvl(klid.visible_user_data1, 'null') AS oct_workspace, nvl(klid.meaning, 'null') AS is_domain, klis.meaning AS impacted_systems, kte.visible_parameter3 involvement, kte.visible_parameter4 estimate_hrs";
        sql = sql.concat(" FROM kcrt_table_entries kte")
                .concat(" INNER JOIN knta_parameter_set_fields kpsf ON kte.parameter_set_field_id = kpsf.parameter_set_field_id AND kpsf.parameter_token LIKE 'SYS_IMPACTED'")
                .concat(" INNER JOIN knta_lookups klis ON kte.parameter2 = klis.lookup_code AND klis.lookup_type LIKE 'MTN - IS Impacted Systems List'")
                .concat(" LEFT OUTER JOIN knta_lookups klid ON klis.user_data1 = klid.lookup_code AND klid.lookup_type LIKE 'MTN - IS Domains List'");
        sql = sql.concat(" WHERE kte.request_id = ").concat(reqId);
        sql = sql.concat(" ORDER BY nvl(klid.meaning, 'null') ASC, klis.meaning ASC");
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the IS PMO Features/IS PMO Testing Feature Request data
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setFeaturesLinkedToIaSql(String prjId) {
        // Create the sql string
        String sql = "SELECT kr.request_id AS request_id, decode(krt.reference_code, 'IS_PMO_TESTING_FEATURE', krhd.visible_parameter1, krhd.visible_parameter2) AS is_domain, kfai.agile_entity_url";
        sql = sql.concat(" FROM pm_projects pp")
                .concat(" INNER JOIN kcrt_fg_master_proj_ref kfpr ON pp.project_id = kfpr.ref_master_project_id")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpr.request_type_id = krt.request_type_id AND krt.reference_code IN ( 'IS_PMO_FEATURE', 'IS_PMO_TESTING_FEATURE' )")
                .concat(" INNER JOIN kcrt_requests kr ON kfpr.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id")
                .concat(" INNER JOIN kcrt_fg_agile_info kfai ON kfpr.request_id = kfai.request_id");
        sql = sql.concat(" WHERE kr.status_code IN ( 'NEW', 'IN_PROGRESS' )")
                .concat(" AND pp.project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method that use the sqlRunner to get the Impacted System Domains from IS PMO Impact Assessment RT.
     * Use POST REST "rest2/sqlRunner/runSqlQuery" to return the data
     *
     * @param ppmBaseUrl  PPM Base URL for identifying the PPM environment
     * @param username    PPM User for access to the PPM entities.
     * @param password    PPM User password
     * @param restUrl     REST API URL for the method
     * @param iaRequestId IS PMO Impact Assessment request ID
     * @return String Array list with the Impacted Domains
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected ArrayList<String> getImpactedSystemDomainsData(String ppmBaseUrl, String username, String password,
                                                             String restUrl, String iaRequestId) throws IOException, JSONException {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = "{ \"querySql\": \"" + setImpactedSytemsDomainListSql(iaRequestId) + "\"}";
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }
        // Check Response Body
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("JSON SQL Return output: " + json);
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
        // Close the Response body
        response.close();
        // Return data as ArrayList<String>
        return result;
    }

    /**
     * Method that use the sqlRunner to get the existing IS PMO Feature RT(s) linked to the IS PMO Impact Assessment
     * Use POST REST "rest2/sqlRunner/runSqlQuery" to return the data
     *
     * @param ppmBaseUrl  PPM Base URL for identifying the PPM environment
     * @param username    PPM User for access to the PPM entities.
     * @param password    PPM User password
     * @param restUrl     REST API URL for the method
     * @param iaRequestId IS PMO Impact Assessment request ID
     * @return String Array list with the Impacted Domains
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected ArrayList<String> getFeatureDomainsData(String ppmBaseUrl, String username, String password,
                                                      String restUrl, String iaRequestId) throws IOException, JSONException {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = "{ \"querySql\": \"" + setFeatureDomainListSql(iaRequestId) + "\"}";
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }
        // Check Response Body
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("JSON SQL Return output: " + json);
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
        // Close the Response body
        response.close();
        // Return data as ArrayList<String>
        return result;
    }

    /**
     * Method that use the sqlRunner to get the IS PMO Impact Assessment linked Project Information for the creation of the IS PMO Feature RT
     * Use POST REST "rest2/sqlRunner/runSqlQuery" to return the data
     *
     * @param ppmBaseUrl    PPM Base URL for identifying the PPM environment
     * @param username      PPM User for access to the PPM entities.
     * @param password      PPM User password
     * @param restUrl       REST API URL for the method
     * @param itProjectId   IS PMO Impact Assessment IT Project ID
     * @param itRequestType IS PMO IT Project Request Type
     * @return SQL Data as HasMap
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected HashMap<String, String> getItProjectData(String ppmBaseUrl, String username, String password,
                                                       String restUrl, String itProjectId, String itRequestType) throws IOException, JSONException {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // Set variable for the Query SQL depending on the IT Project Request Type
        String querySql;
        switch (itRequestType) {
            case "IS PMO IT-EPMO Project":
                querySql = setItEpmoProjectInformationIspmoFeatureSql(itProjectId);
                break;
            case "IS PMO IT-KTLO Project":
                querySql = setItKtloProjectInformationIspmoFeatureSql(itProjectId);
                break;
            case "IS PMO IT-Reporting and Analytics Project":
                querySql = setItReportingAnalyticsProjectInformationIspmoFeatureSql(itProjectId);
                break;
            default:
                throw new IllegalArgumentException("Invalid request type name: " + itRequestType);
        }
        // JSON Payload
        String jsonPayload = "{ \"querySql\": \"" + querySql + "\"}";
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }
        // Check Response Body
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("JSON SQL Return output: " + json);
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
        for (int i = 0; i < jsonColumnHeaders.length(); i++) {
            if (isNotBlankString(jsonValue.get(i).toString())) {
                result.put(jsonColumnHeaders.getString(i), jsonValue.get(i).toString());
            }
        }
        // Add the jsonColumnHeaders as Keys and jsonValue as Values to the HashMap
        // Array
        // ISPMO_PRJ_NUM
//        result.put(jsonColumnHeaders.getString(0), jsonValue.getString(0));
        // ISPMO_PRJ_URL
//        result.put(jsonColumnHeaders.getString(1), ppmBaseUrl + PRJ_URL + jsonValue.getString(1));
        // ISPMO_PRJ_PHASE
//        if (!jsonValue.getString(2).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(2), jsonValue.getString(2));
//        }
        // ISPMO_PRJ_STATUS
//        if (!jsonValue.getString(3).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(3), jsonValue.getString(3));
//        }
        // EPMO_PROJECT_NUM
//        if (!jsonValue.getString(4).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(4), jsonValue.getString(4));
//        }
        // ISPMO_PM
//        if (!jsonValue.getString(5).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(5), jsonValue.getString(5));
//        }
        // ISPMO_PRJ_RAG
//        if (!jsonValue.getString(6).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(6), jsonValue.getString(6));
//        }
        // ISPM_EPMO_BUSINESS_UNIT
//        if (!jsonValue.getString(7).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(7), jsonValue.getString(7));
//        }
        // ISPMO_EPMO_SUB_AREA
//        if (!jsonValue.getString(8).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(8), jsonValue.getString(8));
//        }
        // ISPMO_EPMO_BU_PRIORITY
//        if (!jsonValue.getString(9).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(9), jsonValue.getString(9));
//        }
        // ISPMO_EPMO_ORG_PRIORITY
//        if (!jsonValue.getString(10).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(10), jsonValue.get(10).toString());
//        }
        // ISPMO_PROJECT_TYPE
//        result.put(jsonColumnHeaders.getString(11), jsonValue.getString(11));
        // ISPMO_PRJ_SHORT_DESC
//        if (!jsonValue.getString(12).equalsIgnoreCase("null")) {
//            result.put(jsonColumnHeaders.getString(12), jsonValue.getString(12));
//        }

        // Close the Response body
        response.close();
        // Return data as HashMap<String, String>
        return result;
    }

    /**
     * Method that use the sqlRunner to get the IS PMO Impact Assessment linked Project's EPMO Project Information for the creation of the IS PMO Feature RT
     * Use POST REST "rest2/sqlRunner/runSqlQuery" to return the data
     *
     * @param ppmBaseUrl  PPM Base URL for identifying the PPM environment
     * @param username    PPM User for access to the PPM entities.
     * @param password    PPM User password
     * @param restUrl     REST API URL for the method
     * @param itProjectId IS PMO Impact Assessment IT Project ID
     * @return SQL Data as HasMap
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected HashMap<String, String> getEpmoProjectData(String ppmBaseUrl, String username, String password,
                                                         String restUrl, String itProjectId) throws IOException, JSONException {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = "{ \"querySql\": \"" + setEpmoProjectInformationSql(itProjectId) + "\"}";
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }
        // Check Response Body
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("JSON SQL Return output: " + json);
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
        // ISPMO_EPMO_PRJ_URL
        result.put(jsonColumnHeaders.getString(0), ppmBaseUrl + PRJ_URL + jsonValue.getString(0));
        // ISPMO_EPMO_PRJ_NUM
        result.put(jsonColumnHeaders.getString(1), jsonValue.getString(1));
        // ISPMO_EPMO_PM
        result.put(jsonColumnHeaders.getString(2), jsonValue.getString(2));
        // ISPMO_EPMO_TYPE
        result.put(jsonColumnHeaders.getString(3), jsonValue.getString(3));
        // Close the Response body
        response.close();
        // Return data as HashMap<String, String>
        return result;
    }

    /**
     * Method to get the Impacted System Table data from the IS PMO Impact Assessment Request
     *
     * @param ppmBaseUrl  PPM Base URL for identifying the PPM environment
     * @param username    PPM User for access to the PPM entities.
     * @param password    PPM User password
     * @param restUrl     REST API URL for the method
     * @param iaRequestId IS PMO Impact Assessment Request
     * @return ArrayList Object with Impacted System Table data
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected ArrayList<ImpactedSystemValues> getIaImpactedSystemsTableData(String ppmBaseUrl, String username, String password,
                                                                            String restUrl, String iaRequestId) throws IOException, JSONException {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = "{ \"querySql\": \"" + setIaImpactedSystemsTableSql(iaRequestId) + "\"}";
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }
        // Check Response Body
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("JSON SQL Return output: " + json);
        // Set the JSONArray with the "results" token Array List
        JSONArray jsonResults = new JSONArray(json.getString("results"));
        ArrayList<ImpactedSystemValues> result = new ArrayList<>();
        if (jsonResults.length() != 0) {
            // Get the "values" token Array from the "results" token Array
            JSONArray jsonValues = new JSONArray(jsonResults.toString());
            for (int i = 0; i < jsonValues.length(); i++) {
                // Convert to an JSONObject to get the data Array
                JSONTokener tokenerVal = new JSONTokener(jsonValues.get(i).toString());
                JSONObject jsonVal = new JSONObject(tokenerVal);
                // Set the JSONArray with the "results" token Array List
                JSONArray jsonValue = new JSONArray(jsonVal.getString("values"));
                result.add(new ImpactedSystemValues(jsonValue.getString(0), jsonValue.getString(1), jsonValue.getString(2), jsonValue.getString(3), jsonValue.getString(4)));
            }
        }

        response.close();
        // Return data as HashMap<String, String>
        return result;
    }

    /**
     * Method to get the IS PMO Features and IS PMO Testing Features linked to the IT Project
     *
     * @param ppmBaseUrl  PPM Base URL for identifying the PPM environment
     * @param username    PPM User for access to the PPM entities.
     * @param password    PPM User password
     * @param restUrl     REST API URL for the method
     * @param iaProjectId IT Project ID
     * @return ArrayList Object with IS PMO Features and IS PMO Testing Features
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected ArrayList<FeatureValues> getFeaturesLinkedToIaData(String ppmBaseUrl, String username, String password,
                                                                 String restUrl, String iaProjectId) throws IOException, JSONException {
        // REST API URL
        String sqlUrl = ppmBaseUrl + restUrl;
        log("POST Request Run SQL Query URL: " + sqlUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = "{ \"querySql\": \"" + setFeaturesLinkedToIaSql(iaProjectId) + "\"}";
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(sqlUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }
        // Check Response Body
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("JSON SQL Return output: " + json);
        // Set the JSONArray with the "results" token Array List
        JSONArray jsonResults = new JSONArray(json.getString("results"));
        ArrayList<FeatureValues> result = new ArrayList<>();
        if (jsonResults.length() != 0) {
            // Get the "values" token Array from the "results" token Array
            JSONArray jsonValues = new JSONArray(jsonResults.toString());
            for (int i = 0; i < jsonValues.length(); i++) {
                // Convert to an JSONObject to get the data Array
                JSONTokener tokenerVal = new JSONTokener(jsonValues.get(i).toString());
                JSONObject jsonVal = new JSONObject(tokenerVal);
                // Set the JSONArray with the "results" token Array List
                JSONArray jsonValue = new JSONArray(jsonVal.getString("values"));
                result.add(new FeatureValues(jsonValue.getString(0), jsonValue.getString(1), jsonValue.get(2).toString()));
            }
        }

        response.close();
        // Return data as HashMap<String, String>
        return result;
    }

    /**
     * Method to get the list of Domains that does not have a IS PMO Feature RT created for the linked IS PMO Impact Assessment RT
     *
     * @param iaDomians      mpact Assessment Domains Array List
     * @param featureDomians IS PMO Feature Domains Array List
     * @return String Array List with the Domain(s) for creating IS PMO Features
     */
    protected ArrayList<String> getFeatureCreatDomianList(ArrayList<String> iaDomians,
                                                          ArrayList<String> featureDomians) {
        // Set return Array String List
        ArrayList<String> result = iaDomians;
        // Check if there are any IS Domains in the Impacted Systems table
        if (!result.isEmpty()) {
            result.removeAll(featureDomians);
            // Check if there are any Impacted Systems without PPM Features
            if (!result.isEmpty()) {
                // Sort list alphabetically to create features
                Collections.sort(iaDomians);
//                result = iaDomians;
            }
        }
        return result;
    }

    /**
     * Method to generate the JSON Payload for the creation of the IS PMO Feature Request
     * Only Use for IS PMO IT-EPMO Project Request Types
     *
     * @param baseUrl                PPM Base URL for identifying the PPM environment
     * @param iaRequestId            IS PMO Impact Assessment Request ID
     * @param iaProjectName          IS PMO Impact Assessment linked IT Project
     * @param iaIsDomain             Domain identifier for the IS PMO Feature Request Creation
     * @param itProjectData          IS PMO Impact Assessment linked IT Project Data
     * @param epmoPrjData            IS PMO Impact Assessment linked IT Project's EPMO Project Data
     * @param itProjectMilestoneData IS PMO Impact Assessment linked IT Project Milestone Data
     * @return JSON Payload
     */
    private JSONObject setJsonObjectItEpmoCreateRequestType(String baseUrl, String iaRequestId, String iaProjectName, String iaIsDomain,
                                                            HashMap<String, String> itProjectData, HashMap<String, String> epmoPrjData, ArrayList<ProjectMilestoneValues> itProjectMilestoneData) throws ParseException, JSONException {
        // Set the Token Prefix variables (RT Header or RT Details)
        final String headerFieldPrefix = "REQ.";
        final String detailsFieldPrefix = "REQD.";
        // Get the current date and time in "yyyy-MM-dd'T'HH:mm:ss" format" No need to
        // include include the micro seconds and timezone
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // Date of processing
        Date date = new Date();
        // Start with the required fields to create the IS POMO Feature
        // RT Token: LAST_UPDATE_DATE
        JSONObject tokensLastUpdateDateObj = new JSONObject();
        tokensLastUpdateDateObj.put("token", "REQ.LAST_UPDATE_DATE");
        tokensLastUpdateDateObj.put("dateValue", formatter.format(date));
        // RT Token: ENTITY_LAST_UPDATE_DATE
        JSONObject tokenEntityLastUpdateDateObj = new JSONObject();
        tokenEntityLastUpdateDateObj.put("token", "REQ.ENTITY_LAST_UPDATE_DATE");
        tokenEntityLastUpdateDateObj.put("dateValue", formatter.format(date));
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.put(tokensLastUpdateDateObj);
        fieldArray.put(tokenEntityLastUpdateDateObj);
        // RT Token: KNTA_MASTER_PROJ_REF
        fieldArray.put(setRequestFieldJsonObj(headerFieldPrefix + "KNTA_MASTER_PROJ_REF", iaProjectName));
        // IMPACT_ASSESSMENT_NUM
        fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "IMPACT_ASSESSMENT_NUM", iaRequestId));
        // IS_DOMAIN
        fieldArray.put(setRequestFieldJsonObj(headerFieldPrefix + "IS_DOMAIN", iaIsDomain));
        // Start to set the EPMO Project Fields section
        // ISPMO_EPMO_PRJ_URL
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_PRJ_URL")) {
            fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_EPMO_PRJ_URL", epmoPrjData.get("ISPMO_EPMO_PRJ_URL")));
        }
        // ISPMO_EPMO_PRJ_NUM
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_PRJ_NUM")) {
            fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_EPMO_PRJ_NUM", epmoPrjData.get("ISPMO_EPMO_PRJ_NUM")));
        }
        // ISPMO_EPMO_PM
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_PM")) {
            fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_EPMO_PM", epmoPrjData.get("ISPMO_EPMO_PM")));
        }
        // ISPMO_EPMO_TYPE
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_TYPE")) {
            fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_EPMO_TYPE", epmoPrjData.get("ISPMO_EPMO_TYPE")));
        }
        // End setting the EPMO Project Fields section
        // Set the HTML String for the IT Project Milestones
        IspmoItProjectProcessor projectProcessor = new IspmoItProjectProcessor();
        String projectMilestoneString = projectProcessor.setProjectMilestoneHtml(itProjectMilestoneData);
        if (isNotBlankString(projectMilestoneString)) {
            fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_MILESTONES", projectMilestoneString));
        }
        // Start to set the IT Project Fields section
        // Iterate through the IT Project Fields
        for (Map.Entry<String, String> featureFieldSet : itProjectData.entrySet()) {
            // Set the Feature Key and Value Variables
            final String featureKey = featureFieldSet.getKey();
            final String featureFieldValue = featureFieldSet.getValue();
            // PPM Feature Description Field update
            String featureDescription;
            if (featureKey.equalsIgnoreCase("ISPMO_PRJ_URL")) {
                // Set variable for the IT Project Name to be used for the Feature Description
                featureDescription = projectProcessor.setFeatureDescription(itProjectData.get("ISPMO_PRJ_NUM"), iaProjectName, itProjectData.get("EPMO_PROJECT_NUM"));
                // Add the IS PMO Feature Description Field
                fieldArray.put(projectProcessor.setRequestFieldJsonObj(headerFieldPrefix + "DESCRIPTION", featureDescription));
                // Add the IT Project URL Field
                fieldArray.put(projectProcessor.setRequestFieldJsonObj(detailsFieldPrefix + featureKey, baseUrl + PRJ_URL + featureFieldValue));
            } else {
                // All Other Feature Field
                // Check if the Feature Value is blank/null
                if (isNotBlankString(featureFieldValue)) {
                    fieldArray.put(projectProcessor.setRequestFieldJsonObj(detailsFieldPrefix + featureKey, featureFieldValue));
                }
            }
        }
        // End setting the IT Project Fields section
        // Set the Field Object
        JSONObject fieldObj = new JSONObject();
        fieldObj.put("field", fieldArray);
        // Full JSON Object - Top Level
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("fields", fieldObj);
        jsonObj.put("sourceType", "INTERFACE_RI");
        jsonObj.put("requestType", "IS PMO Feature");
        return jsonObj;
    }

    /**
     * Method to generate the JSON Payload for the creation of the IS PMO Feature Request
     * Only Use for IS PMO IT-KTLO Project and IS PMO IT-Reporting and Analytics Project Request Types
     *
     * @param baseUrl                PPM Base URL for identifying the PPM environment
     * @param iaRequestId            IS PMO Impact Assessment Request ID
     * @param iaProjectName          IS PMO Impact Assessment linked IT Project
     * @param iaIsDomain             Domain identifier for the IS PMO Feature Request Creation
     * @param itProjectData          IS PMO Impact Assessment linked IT Project Data
     * @param itProjectMilestoneData IS PMO Impact Assessment linked IT Project Milestone Data
     * @return JSON Payload
     */
    private JSONObject setJsonObjectNoneEpmoCreateRequestType(String baseUrl, String iaRequestId, String iaProjectName, String iaIsDomain,
                                                              HashMap<String, String> itProjectData, ArrayList<ProjectMilestoneValues> itProjectMilestoneData) throws ParseException, JSONException {
        // Set the Token Prefix variables (RT Header or RT Details)
        final String headerFieldPrefix = "REQ.";
        final String detailsFieldPrefix = "REQD.";
        // Get the current date and time in "yyyy-MM-dd'T'HH:mm:ss" format" No need to
        // include include the micro seconds and timezone
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // Date of processing
        Date date = new Date();
        // Start with the required fields to create the IS POMO Feature
        // RT Token: LAST_UPDATE_DATE
        JSONObject tokensLastUpdateDateObj = new JSONObject();
        tokensLastUpdateDateObj.put("token", "REQ.LAST_UPDATE_DATE");
        tokensLastUpdateDateObj.put("dateValue", formatter.format(date));
        // RT Token: ENTITY_LAST_UPDATE_DATE
        JSONObject tokenEntityLastUpdateDateObj = new JSONObject();
        tokenEntityLastUpdateDateObj.put("token", "REQ.ENTITY_LAST_UPDATE_DATE");
        tokenEntityLastUpdateDateObj.put("dateValue", formatter.format(date));
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.put(tokensLastUpdateDateObj);
        fieldArray.put(tokenEntityLastUpdateDateObj);
        // RT Token: KNTA_MASTER_PROJ_REF
        fieldArray.put(setRequestFieldJsonObj(headerFieldPrefix + "KNTA_MASTER_PROJ_REF", iaProjectName));
        // IMPACT_ASSESSMENT_NUM
        fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "IMPACT_ASSESSMENT_NUM", iaRequestId));
        // IS_DOMAIN
        fieldArray.put(setRequestFieldJsonObj(headerFieldPrefix + "IS_DOMAIN", iaIsDomain));
        // Set the HTML String for the IT Project Milestones
        IspmoItProjectProcessor projectProcessor = new IspmoItProjectProcessor();
        String projectMilestoneString = projectProcessor.setProjectMilestoneHtml(itProjectMilestoneData);
        if (isNotBlankString(projectMilestoneString)) {
            fieldArray.put(setRequestFieldJsonObj(detailsFieldPrefix + "ISPMO_MILESTONES", projectMilestoneString));
        }
        // Start to set the IT Project Fields section
        // Iterate through the IT Project Fields
        for (Map.Entry<String, String> featureFieldSet : itProjectData.entrySet()) {
            // Set the Feature Key and Value Variables
            final String featureKey = featureFieldSet.getKey();
            final String featureFieldValue = featureFieldSet.getValue();
            // PPM Feature Description Field update
            String featureDescription;
            if (featureKey.equalsIgnoreCase("ISPMO_PRJ_URL")) {
                // Set variable for the IT Project Name to be used for the Feature Description
                featureDescription = projectProcessor.setFeatureDescription(itProjectData.get("ISPMO_PRJ_NUM"), iaProjectName);
                // Add the IS PMO Feature Description Field
                fieldArray.put(projectProcessor.setRequestFieldJsonObj(headerFieldPrefix + "DESCRIPTION", featureDescription));
                // Add the IT Project URL Field
                fieldArray.put(projectProcessor.setRequestFieldJsonObj(detailsFieldPrefix + featureKey, baseUrl + PRJ_URL + featureFieldValue));
            } else {
                // All Other Feature Field
                // Check if the Feature Value is blank/null
                if (isNotBlankString(featureFieldValue)) {
                    fieldArray.put(projectProcessor.setRequestFieldJsonObj(detailsFieldPrefix + featureKey, featureFieldValue));
                }
            }
        }
        // End setting the IT Project Fields section
        // Set the Field Object
        JSONObject fieldObj = new JSONObject();
        fieldObj.put("field", fieldArray);
        // Full JSON Object - Top Level
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("fields", fieldObj);
        jsonObj.put("sourceType", "INTERFACE_RI");
        jsonObj.put("requestType", "IS PMO Feature");
        return jsonObj;
    }

    /**
     * Method to set the JSON Request Field Object for String Values
     *
     * @param strToken Token of the Request Field
     * @param strValue String Value Array
     * @return the JSONObject for the Request Field Object
     */
    private JSONObject setRequestFieldJsonObj(String strToken, String strValue) throws JSONException {
        // Declare the Request Field Object
        JSONObject requestFieldObj = new JSONObject();
        requestFieldObj.put("token", strToken);
        // Declare and Instantiate the strValue Array
        JSONArray stringValueArray = new JSONArray();
        stringValueArray.put(strValue);
        requestFieldObj.put("stringValue", stringValueArray);
        return requestFieldObj;
    }

    /**
     * Method that create the IS PMO Feature per IS Domain
     *
     * @param ppmBaseUrl    PPM Base URL for identifying the PPM environment
     * @param username      PPM User for access to the PPM entities.
     * @param password      PPM User password
     * @param restUrl       REST API URL for the method
     * @param iaRequestId   IS PMO Impact Assessment Request ID
     * @param iaProjectName IS PMO Impact Assessment linked IT Project
     * @param itProjectData IS PMO Impact Assessment linked IT Project Data
     * @param epmoPrjData   IS PMO Impact Assessment linked IT Project's EPMO Project Data
     * @return New IS PMO Feature Request ID String
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected String createIspmoFeatureRequest(String ppmBaseUrl, String username, String password, String restUrl, String iaRequestId, String iaProjectName, String iaDomain, String iaProjectRequestType, HashMap<String, String> itProjectData, HashMap<String, String> epmoPrjData, ArrayList<ProjectMilestoneValues> itProjectMilestonesArrayList) throws IOException, JSONException, ParseException {

        // REST API URL
        String requestUrl = ppmBaseUrl + restUrl;
        log("<p stryle=\"margin-left:1px\">");
        log("POST Request Creating RT URL: " + requestUrl);
        log("</p><br>");
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the POST RRequest and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload;
        switch (iaProjectRequestType) {
            case "IS PMO IT-EPMO Project":
                jsonPayload = setJsonObjectItEpmoCreateRequestType(ppmBaseUrl, iaRequestId, iaProjectName, iaDomain, itProjectData, epmoPrjData, itProjectMilestonesArrayList).toString();
                break;
            case "IS PMO IT-KTLO Project":
            case "IS PMO IT-Reporting and Analytics Project":
                jsonPayload = setJsonObjectNoneEpmoCreateRequestType(ppmBaseUrl, iaRequestId, iaProjectName, iaDomain, itProjectData, itProjectMilestonesArrayList).toString();
                break;
            default:
                throw new IllegalArgumentException("Invalid request type name: " + iaProjectRequestType);
        }
        log("<p stryle=\"margin-left:1px\">");
        log("Create IS PMO Feature Pay Load: " + jsonPayload);
        log("<hr></p><br>");
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(requestUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .post(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }

        // JSONTokener - Set all the JSON keys as a token from the Json Return string.
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("<p stryle=\"margin-left:1px\">");
        log("Successful POST response output Updating RT: " + json);
        log("<hr></p><br>");
        // Disconnect the connection
        response.close();
        // Return String with Request ID
        return json.getString("id");
    }

    /**
     * Method to set the reference between request types with relationship information
     *
     * @param ppmBaseUrl       PPM Base URL for identifying the PPM environment
     * @param username         PPM User for access to the PPM entities.
     * @param password         PPM User password
     * @param sourceRequestId  IS PMO Impact Assessment Request ID
     * @param targetRequestIds IS PMO Feature Request IDs
     * @param relationshipCode Relationship indications
     * @throws IOException IO Exceptions are thrown up to the main class method
     */
    protected void setRequestReference(String ppmBaseUrl, String username, String password, String restUrl, String sourceRequestId, String targetRequestIds, String relationshipCode) throws IOException {
        // Rest URL
        String putRequestUrl = ppmBaseUrl + restUrl + "/" + sourceRequestId + "/addReference/" + targetRequestIds + "/" + relationshipCode + "?refRelName=" + relationshipCode;
        log("PUT Request Reference RT URL: " + putRequestUrl);
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url(putRequestUrl)
                .addHeader("Ephemeral", "true")
                .addHeader("Authorization", authHeader)
                .put(body)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            log("Request Reference PUT Response: References Successfully Added");
        } else {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }

        response.close();
    }

    /**
     * Method to generate the JSON Payload for the updating of the IS PMO Feature or IS PMO Testing Feature Request
     *
     * @param impactedSystemsObjArray    Impacted System Table values for the Domain
     * @param allImpactedSystemsObjArray All Impacted System Table data
     * @return Json Object with the payload
     */
    private JSONObject setJsonObjectUpdateFeatureRequestTypeImpactedSystemFields(ArrayList<ImpactedSystemValues> impactedSystemsObjArray, ArrayList<ImpactedSystemValues> allImpactedSystemsObjArray) throws JSONException {
        // Get the current date and time in "yyyy-MM-dd'T'HH:mm:ss" format" No need to
        // include include the micro seconds and timezone
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // Date of processing
        Date date = new Date();
        // Start with the required fields to create the IS POMO Feature
        // RT Token: LAST_UPDATE_DATE
        JSONObject tokensLastUpdateDateObj = new JSONObject();
        tokensLastUpdateDateObj.put("token", "REQ.LAST_UPDATE_DATE");
        tokensLastUpdateDateObj.put("dateValue", formatter.format(date));
        // RT Token: ENTITY_LAST_UPDATE_DATE
        JSONObject tokenEntityLastUpdateDateObj = new JSONObject();
        tokenEntityLastUpdateDateObj.put("token", "REQ.ENTITY_LAST_UPDATE_DATE");
        tokenEntityLastUpdateDateObj.put("dateValue", formatter.format(date));
        // RT Token: ISPMO_IMPACTED_SYSTEMS
        JSONObject tokenImpactedSystemsObj = new JSONObject();
        tokenImpactedSystemsObj.put("token", "REQD.ISPMO_IMPACTED_SYSTEMS");
        // Set the stringValue Array for the Impacted Systems
        JSONArray stringValueImpactedSystemsArray = new JSONArray();
        String impactedSystemsString = setImpactedSystemString(impactedSystemsObjArray);
        stringValueImpactedSystemsArray.put(impactedSystemsString);
        tokenImpactedSystemsObj.put("stringValue", stringValueImpactedSystemsArray);
        // RT Token: ISPMO_INVOLVEMENTS
        JSONObject tokenInvolvementObj = new JSONObject();
        tokenInvolvementObj.put("token", "REQD.ISPMO_INVOLVEMENTS");
        // Set the stringValue Array for the Involvement
        JSONArray stringValueInvolvementArray = new JSONArray();
        String involvementString = setDomainInvolvementHtml(impactedSystemsObjArray);
        stringValueInvolvementArray.put(involvementString);
        tokenInvolvementObj.put("stringValue", stringValueInvolvementArray);
        // RT Token: IS_DOMAIN_INVOLVEMENTS
        JSONObject tokenAllInvolvementObj = new JSONObject();
        tokenAllInvolvementObj.put("token", "REQD.IS_DOMAIN_INVOLVEMENTS");
        // Set the stringValue Array for the Involvement
        JSONArray stringValueAllInvolvementArray = new JSONArray();
        String allInvolvementString = setDomainInvolvementHtml(allImpactedSystemsObjArray);
        stringValueAllInvolvementArray.put(allInvolvementString);
        tokenAllInvolvementObj.put("stringValue", stringValueAllInvolvementArray);
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.put(tokensLastUpdateDateObj);
        fieldArray.put(tokenEntityLastUpdateDateObj);
        fieldArray.put(tokenImpactedSystemsObj);
        fieldArray.put(tokenInvolvementObj);
        fieldArray.put(tokenAllInvolvementObj);
        // Set the Field Object
        JSONObject fieldObj = new JSONObject();
        fieldObj.put("field", fieldArray);
        // Full JSON Object - Top Level
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("fields", fieldObj);
        return jsonObj;
    }

    /**
     * Method to update the following fields in the IS PMO Feature and IS PMO Testing Feature Request Types:
     * - Impacted Systems
     * - Involvement per Impacted System
     * - Involvement Across All Delivery Areas (IS Domains)
     * - IT Project Milestones
     *
     * @param ppmBaseUrl          PPM Base URL for identifying the PPM environment
     * @param username            PPM User for access to the PPM entities.
     * @param password            PPM User password
     * @param restUrl             REST API URL for the method
     * @param featureReqId        IS PMO Feature or IS PMO Testing Feature Request Id
     * @param isValuesObjArray    Impacted System Table values for the Domain
     * @param isAllValuesObjArray All Impacted System Table data
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected void updateFeatureRequestTypeImpactedSystemFields(String ppmBaseUrl, String username, String password, String restUrl, String featureReqId, ArrayList<ImpactedSystemValues> isValuesObjArray, ArrayList<ImpactedSystemValues> isAllValuesObjArray) throws IOException, JSONException, ParseException {

        // REST API URL
        String requestUrl = ppmBaseUrl + restUrl + "/" + featureReqId;
        log("<p stryle=\"margin-left:1px\">");
        log("PUT Request Update RT URL: " + requestUrl);
        log("</p><br>");
        // Encode the Username and Password. Using Admin user to ensure
        final String auth = username + ":" + password;
        String encoding = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encoding;
        // Set the PUT Request with all the required parameters and timeouts
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // JSON Payload
        String jsonPayload = setJsonObjectUpdateFeatureRequestTypeImpactedSystemFields(isValuesObjArray, isAllValuesObjArray).toString();
        log("<p stryle=\"margin-left:1px\">");
        log("Create IS PMO Feature Pay Load: " + jsonPayload);
        log("<hr></p><br>");
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(requestUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
                .addHeader("Ephemeral", "true")
                .put(body)
                .build();
        Call call = client.newCall(request);
        // Execute the POST Request
        Response response = call.execute();
        // Get the Response from server for the GET REST Request done.
        if (!response.isSuccessful()) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }

        // JSONTokener - Set all the JSON keys as a token from the Json Return string.
        assert response.body() != null : "The PUT Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("<p stryle=\"margin-left:1px\">");
        log("Successful PUT response output Updating RT: " + json);
        log("<hr></p><br>");
        // Disconnect the connection
        response.close();
    }

    /**
     * Method to set the Impacted Systems String for the REST API PUT Request
     *
     * @param impactedSystemObj ArrayList with ImpactedSystemsValue Object data
     * @return Impacted Systems semicolon separated String
     */
    protected String setImpactedSystemString(ArrayList<ImpactedSystemValues> impactedSystemObj) {
        // Set the Impacted Systems array for the IS Domain
        Set<String> stringSet = new HashSet<>();
        if (!impactedSystemObj.isEmpty()) {
            for (ImpactedSystemValues impactedSystems : impactedSystemObj) {
                stringSet.add(impactedSystems.getImpactedSystem());
            }
        }
        // Get the return string from the array by joining.
        String result = null;
        if (!stringSet.isEmpty()) {
            result = String.join(";", stringSet);
        }
        return result;
    }

    /**
     * Method to set the HTML for the Involvement fields in Request Types: IS PMO Feature and IS PMO Testing Feature
     *
     * @param domainInvolvementObj Impacted Systems value array list
     * @return HTML String
     */
    private String setDomainInvolvementHtml(ArrayList<ImpactedSystemValues> domainInvolvementObj) {
        // Return String result
        String result = null;
        // Result String Length indicator
        int stringLength = 0;
        // Final Table string
        final String endHtmlTable = "</table>";
        // Build the string with HTML tags for a table
        if (!domainInvolvementObj.isEmpty()) {
            // Header Table string
            String headerHtmlTable;
            // Add header of the html table
            headerHtmlTable = "<table style=\"border: 1px solid black; border-collapse: collapse; width: 98%;\">";
            headerHtmlTable = headerHtmlTable.concat("<tr>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 39%;\">Systems Impacted</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold; width: 39%;\">Involvement</td>");
            headerHtmlTable = headerHtmlTable.concat("<td style=\"font-weight: bold;\">Effort Estimation (Hours)</td>");
            headerHtmlTable = headerHtmlTable.concat("</tr>");
            // Assign the Header Table string to the Return Result string
            result = headerHtmlTable;
            // Set the Result String Length indicator after HTML Header was created
            stringLength = stringLength + headerHtmlTable.length();
            // Iterate through the Domain Involvement Object
            for (ImpactedSystemValues impactedSystemValues : domainInvolvementObj) {
                // Inner Table string
                String innerHtmlTable;
                // Create the Row in the table body
                innerHtmlTable = "<tr>";
                // Systems Impacted
                innerHtmlTable = innerHtmlTable.concat("<td>").concat(impactedSystemValues.getImpactedSystem()).concat("</td>");
                // Involvement
                innerHtmlTable = innerHtmlTable.concat("<td>").concat(impactedSystemValues.getInvolvement()).concat("</td>");
                // Effort Estimation (Hours)
                innerHtmlTable = innerHtmlTable.concat("<td>").concat(impactedSystemValues.getEstimateHours()).concat("</td>");
                innerHtmlTable = innerHtmlTable.concat("</tr>");
                // Check if the Cumulative Result String Length indicator plus Inner Table string length is less than the HTML Text Area Maximum Character length
                if ((stringLength + innerHtmlTable.length() + endHtmlTable.length()) <= TEXT_AREA_HTML_MAX) {
                    // Assign the Inner Table string to the Return Result string
                    result = result.concat(innerHtmlTable);
                    // Set the Result String Length indicator after each iteration was created and less than HTML Text Area Maximum Character length
                    stringLength = stringLength + innerHtmlTable.length();
                } else {
                    // Break out of the loop if greater or equal to HTML Text Area Maximum Character length
                    break;
                }
            }

            // Assign the Final Table string to the Return Result string
            result = result.concat(endHtmlTable);
            // Set the Result String Length indicator after Final Table string was added
            stringLength = stringLength + endHtmlTable.length();
            log("Domain Involvement HTML Table String length: " + stringLength);
        }
        return result;
    }
}
