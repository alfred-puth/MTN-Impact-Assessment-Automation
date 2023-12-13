package za.co.mtn.ppm.bpm.ia;


import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
     * Method to set the SQL string to be used for extracting the IT Project Data that's linked to the IS PMO Impact Assessment
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setItProjectInformationSql(String prjId) {
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
     * Method to set the SQL string to be used for extracting the IT Project Release Data that's linked to the IS PMO Impact Assessment
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setItProjectReleaseInformationSql(String prjId) {
        // Create the sql string
        String sql = "SELECT nvl(krd.visible_parameter11, 'null') AS ispmo_incl_retail_build, nvl(krd.visible_parameter12, 'null') AS ispmo_incl_charg_sys, nvl(krd.visible_parameter13, 'null') AS ispmo_incl_wholsal_rel, nvl(krd.visible_parameter14, 'null') AS ispmo_incl_siya_rel,  nvl(krd.visible_parameter15, 'null') AS ispmo_incl_ilula_rel, nvl(krd.visible_parameter20, 'null') AS ispmo_incl_siebel_rel";
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp").concat(" INNER JOIN kcrt_request_details krd ON kfpp.request_id = krd.request_id AND krd.batch_number = 1");
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
        sql = sql.concat(" FROM kcrt_fg_pfm_project kfpp").concat(" INNER JOIN kcrt_request_details krd ON kfpp.request_id = krd.request_id AND krd.batch_number = 1").concat(" INNER JOIN pm_projects pp ON krd.parameter3 = pp.project_id").concat(" INNER JOIN pm_project_types ppt ON pp.project_id = ppt.project_id");
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
        String sql = "SELECT kr.request_id AS request_id, decode(krt.reference_code, 'IS_PMO_TESTING_FEATURE', krhd.visible_parameter1, krhd.visible_parameter2) AS is_domain";
        sql = sql.concat(" FROM pm_projects pp")
                .concat(" INNER JOIN kcrt_fg_master_proj_ref kfpr ON pp.project_id = kfpr.ref_master_project_id")
                .concat(" INNER JOIN kcrt_request_types krt ON kfpr.request_type_id = krt.request_type_id AND krt.reference_code IN ( 'IS_PMO_FEATURE', 'IS_PMO_TESTING_FEATURE' )")
                .concat(" INNER JOIN kcrt_requests kr ON kfpr.request_id = kr.request_id")
                .concat(" INNER JOIN kcrt_req_header_details krhd ON kr.request_id = krhd.request_id");
        sql = sql.concat(" WHERE kr.status_code IN ( 'NEW', 'IN_PROGRESS' )")
                .concat(" AND pp.project_id = ").concat(prjId);
        return sql;
    }

    /**
     * Method to set the SQL string to be used for extracting the IT Project Milestones data
     *
     * @param prjId IT Project ID
     * @return SQL String with the created SQL statement
     */
    private String setItProjectMilestonesSql(String prjId) {
        // Create the sql String
        String sql = "SELECT wti.name, wts.sched_finish_date, wta.act_finish_date, ks.state_name";
        sql = sql.concat(" FROM pm_work_plans pwp")
                .concat(" INNER JOIN wp_tasks wt ON pwp.work_plan_id = wt.work_plan_id")
                .concat(" INNER JOIN wp_task_info wti ON wt.task_info_id = wti.task_info_id AND wti.task_type_code = 'M'")
                .concat(" INNER JOIN wp_task_schedule wts ON wt.task_schedule_id = wts.task_schedule_id")
                .concat(" INNER JOIN wp_task_actuals wta ON wt.task_actuals_id = wta.actuals_id")
                .concat(" INNER JOIN wp_milestones wm ON wt.milestone_id = wm.milestone_id AND wm.major = 'Y'")
                .concat(" INNER JOIN kdrv_states ks ON wti.status = ks.state_id");
        sql = sql.concat(" WHERE pwp.entity_type = 'WORK_PLAN'").concat(" AND pwp.project_id = ").concat(prjId);
        sql = sql.concat(" ORDER BY wt.sequence_number ASC");
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
        if (response.code() != 200) {
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
        if (!jsonResults.isEmpty()) {
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
        if (response.code() != 200) {
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
        if (!jsonResults.isEmpty()) {
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
     * @param ppmBaseUrl  PPM Base URL for identifying the PPM environment
     * @param username    PPM User for access to the PPM entities.
     * @param password    PPM User password
     * @param restUrl     REST API URL for the method
     * @param itProjectId IS PMO Impact Assessment IT Project ID
     * @return SQL Data as HasMap
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected HashMap<String, String> getItProjectData(String ppmBaseUrl, String username, String password,
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
        String jsonPayload = "{ \"querySql\": \"" + setItProjectInformationSql(itProjectId) + "\"}";
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
        if (response.code() != 200) {
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

        // Close the Response body
        response.close();
        // Return data as HashMap<String, String>
        return result;
    }

    /**
     * Method that use the sqlRunner to get the IS PMO Impact Assessment linked Project Release Information for the creation of the IS PMO Feature RT
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
    protected HashMap<String, String> getItProjectReleaseData(String ppmBaseUrl, String username, String password,
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
        String jsonPayload = "{ \"querySql\": \"" + setItProjectReleaseInformationSql(itProjectId) + "\"}";
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
        if (response.code() != 200) {
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
        // ISPMO_INCL_RETAIL_BUILD
        if (!jsonValue.getString(0).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(0), jsonValue.getString(0));
        }
        // ISPMO_INCL_CHARG_SYS
        if (!jsonValue.getString(1).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(1), jsonValue.getString(1));
        }
        // ISPMO_INCL_WHOLSAL_REL
        if (!jsonValue.getString(2).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(2), jsonValue.getString(2));
        }
        // ISPMO_INCL_SIYA_REL
        if (!jsonValue.getString(3).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(3), jsonValue.getString(3));
        }
        // ISPMO_INCL_ILULA_REL
        if (!jsonValue.getString(4).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(4), jsonValue.getString(4));
        }
        // ISPMO_INCL_SIEBEL_REL
        if (!jsonValue.getString(5).equalsIgnoreCase("null")) {
            result.put(jsonColumnHeaders.getString(5), jsonValue.getString(5));
        }
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
        if (response.code() != 200) {
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
        if (response.code() != 200) {
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
        if (!jsonResults.isEmpty()) {
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
        if (response.code() != 200) {
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
        if (!jsonResults.isEmpty()) {
            // Get the "values" token Array from the "results" token Array
            JSONArray jsonValues = new JSONArray(jsonResults.toString());
            for (int i = 0; i < jsonValues.length(); i++) {
                // Convert to an JSONObject to get the data Array
                JSONTokener tokenerVal = new JSONTokener(jsonValues.get(i).toString());
                JSONObject jsonVal = new JSONObject(tokenerVal);
                // Set the JSONArray with the "results" token Array List
                JSONArray jsonValue = new JSONArray(jsonVal.getString("values"));
                result.add(new FeatureValues(jsonValue.getString(0), jsonValue.getString(1)));
            }
        }

        response.close();
        // Return data as HashMap<String, String>
        return result;
    }

    /**
     * Method to get the IT Project Milestone information
     *
     * @param ppmBaseUrl  PPM Base URL for identifying the PPM environment
     * @param username    PPM User for access to the PPM entities.
     * @param password    PPM User password
     * @param restUrl     REST API URL for the method
     * @param iaProjectId IT Project ID
     * @return ArrayList Object with IT Project Milestone data
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected ArrayList<ProjectMilestoneValues> getItProjectMilestoneData(String ppmBaseUrl, String username, String password,
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
        String jsonPayload = "{ \"querySql\": \"" + setItProjectMilestonesSql(iaProjectId) + "\"}";
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
        if (response.code() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }
        // Check Response Body
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("JSON SQL Return output: " + json);
        // Set the JSONArray with the "results" token Array List
        JSONArray jsonResults = new JSONArray(json.getString("results"));
        ArrayList<ProjectMilestoneValues> result = new ArrayList<>();
        if (!jsonResults.isEmpty()) {
            // Get the "values" token Array from the "results" token Array
            JSONArray jsonValues = new JSONArray(jsonResults.toString());
            for (int i = 0; i < jsonValues.length(); i++) {
                // Convert to an JSONObject to get the data Array
                JSONTokener tokenerVal = new JSONTokener(jsonValues.get(i).toString());
                JSONObject jsonVal = new JSONObject(tokenerVal);
                // Set the JSONArray with the "results" token Array List
                JSONArray jsonValue = new JSONArray(jsonVal.getString("values"));
                result.add(new ProjectMilestoneValues(jsonValue.get(0).toString(), jsonValue.get(1).toString(), jsonValue.get(2).toString(), jsonValue.get(3).toString()));
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
            if (result.size() > 0) {
                // Sort list alphabetically to create features
                Collections.sort(iaDomians);
                result = iaDomians;
            }
        }
        return result;
    }

    /**
     * Method to generate the JSON Payload for the creation of the IS PMO Feature Request
     *
     * @param iaRequestId   IS PMO Impact Assessment Request ID
     * @param iaProjectName IS PMO Impact Assessment linked IT Project
     * @param iaIsDomain    Domain identifier for the IS PMO Feature Request Creation
     * @param itProjectData IS PMO Impact Assessment linked IT Project Data
     * @param itReleaseData IS PMO Impact Assessment linked IT Project Release Data
     * @param epmoPrjData   IS PMO Impact Assessment linked IT Project's EPMO Project Data
     * @return JSON Payload
     */
    private JSONObject setJsonObjectCreateRequestType(String iaRequestId, String iaProjectName, String iaIsDomain,
                                                      HashMap<String, String> itProjectData, HashMap<String, String> itReleaseData, HashMap<String, String> epmoPrjData) {
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
        // Start with the IT Project fields to create the IS PMO Feature from the
        // HashMap Array: itProjectData
        // ISPMO_PRJ_NUM
        JSONObject tokenIspmoPrjNumObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PRJ_NUM")) {
            tokenIspmoPrjNumObj.put("token", "REQD.ISPMO_PRJ_NUM");
            // Set the stringValue Array
            JSONArray stringValueIspmoPrjNumArray = new JSONArray();
            stringValueIspmoPrjNumArray.put(itProjectData.get("ISPMO_PRJ_NUM"));
            // Add Array to the JSONObject
            tokenIspmoPrjNumObj.put("stringValue", stringValueIspmoPrjNumArray);
        }
        // ISPMO_PRJ_URL
        JSONObject tokenIspmoPrjUrlObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PRJ_URL")) {
            tokenIspmoPrjUrlObj.put("token", "REQD.ISPMO_PRJ_URL");
            // Set the stringValue Array
            JSONArray stringValueIspmoPrjUrlArray = new JSONArray();
            stringValueIspmoPrjUrlArray.put(itProjectData.get("ISPMO_PRJ_URL"));
            // Add Array to the JSONObject
            tokenIspmoPrjUrlObj.put("stringValue", stringValueIspmoPrjUrlArray);
        }
        // ISPMO_PRJ_PHASE
        JSONObject tokenIspmoPrjPhaseObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PRJ_PHASE")) {
            tokenIspmoPrjPhaseObj.put("token", "REQD.ISPMO_PRJ_PHASE");
            // Set the stringValue Array
            JSONArray stringValueIspmoPrjPhaseArray = new JSONArray();
            stringValueIspmoPrjPhaseArray.put(itProjectData.get("ISPMO_PRJ_PHASE"));
            // Add Array to the JSONObject
            tokenIspmoPrjPhaseObj.put("stringValue", stringValueIspmoPrjPhaseArray);
        }
        // ISPMO_PRJ_STATUS
        JSONObject tokenIspmoPrjStatusObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PRJ_STATUS")) {
            tokenIspmoPrjStatusObj.put("token", "REQD.ISPMO_PRJ_STATUS");
            // Set the stringValue Array
            JSONArray stringValueIspmoPrjStatusArray = new JSONArray();
            stringValueIspmoPrjStatusArray.put(itProjectData.get("ISPMO_PRJ_STATUS"));
            // Add Array to the JSONObject
            tokenIspmoPrjStatusObj.put("stringValue", stringValueIspmoPrjStatusArray);
        }
        // EPMO_PROJECT_NUM
        JSONObject tokenEpmoProjectNumObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("EPMO_PROJECT_NUM")) {
            tokenEpmoProjectNumObj.put("token", "REQD.EPMO_PROJECT_NUM");
            // Set the stringValue Array
            JSONArray stringValueEpmoProjectNumArray = new JSONArray();
            stringValueEpmoProjectNumArray.put(itProjectData.get("EPMO_PROJECT_NUM"));
            // Add Array to the JSONObject
            tokenEpmoProjectNumObj.put("stringValue", stringValueEpmoProjectNumArray);
        }
        // ISPMO_PM
        JSONObject tokenIspmoPmObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PM")) {
            tokenIspmoPmObj.put("token", "REQD.ISPMO_PM");
            // Set the stringValue Array
            JSONArray stringValueIspmoPmArray = new JSONArray();
            stringValueIspmoPmArray.put(itProjectData.get("ISPMO_PM"));
            // Add Array to the JSONObject
            tokenIspmoPmObj.put("stringValue", stringValueIspmoPmArray);
        }
        // ISPMO_PRJ_RAG
        JSONObject tokenIspmoPrjRagObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PRJ_RAG")) {
            tokenIspmoPrjRagObj.put("token", "REQD.ISPMO_PRJ_RAG");
            // Set the stringValue Array
            JSONArray stringValueIspmoPrjRagArray = new JSONArray();
            stringValueIspmoPrjRagArray.put(itProjectData.get("ISPMO_PRJ_RAG"));
            // Add Array to the JSONObject
            tokenIspmoPrjRagObj.put("stringValue", stringValueIspmoPrjRagArray);
        }
        // ISPM_EPMO_BUSINESS_UNIT
        JSONObject tokenBusinessUnitObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPM_EPMO_BUSINESS_UNIT")) {
            tokenBusinessUnitObj.put("token", "REQD.ISPM_EPMO_BUSINESS_UNIT");
            // Set the stringValue Array
            JSONArray stringValueBusinessUnitArray = new JSONArray();
            stringValueBusinessUnitArray.put(itProjectData.get("ISPM_EPMO_BUSINESS_UNIT"));
            // Add Array to the JSONObject
            tokenBusinessUnitObj.put("stringValue", stringValueBusinessUnitArray);
        }
        // ISPMO_EPMO_SUB_AREA
        JSONObject tokenSubAreaObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_EPMO_SUB_AREA")) {
            tokenSubAreaObj.put("token", "REQD.ISPMO_EPMO_SUB_AREA");
            // Set the stringValue Array
            JSONArray stringValueSubAreaArray = new JSONArray();
            stringValueSubAreaArray.put(itProjectData.get("ISPMO_EPMO_SUB_AREA"));
            // Add Array to the JSONObject
            tokenSubAreaObj.put("stringValue", stringValueSubAreaArray);
        }
        // ISPMO_EPMO_BU_PRIORITY
        JSONObject tokenBuPriorityObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_EPMO_BU_PRIORITY")) {
            tokenBuPriorityObj.put("token", "REQD.ISPMO_EPMO_BU_PRIORITY");
            // Set the stringValue Array
            JSONArray stringValueBuPriorityArray = new JSONArray();
            stringValueBuPriorityArray.put(itProjectData.get("ISPMO_EPMO_BU_PRIORITY"));
            // Add Array to the JSONObject
            tokenBuPriorityObj.put("stringValue", stringValueBuPriorityArray);
        }
        // ISPMO_EPMO_ORG_PRIORITY
        JSONObject tokenOrgPriorityObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_EPMO_ORG_PRIORITY")) {
            tokenOrgPriorityObj.put("token", "REQD.ISPMO_EPMO_ORG_PRIORITY");
            // Set the stringValue Array
            JSONArray stringValueOrgPriorityArray = new JSONArray();
            stringValueOrgPriorityArray.put(itProjectData.get("ISPMO_EPMO_ORG_PRIORITY"));
            // Add Array to the JSONObject
            tokenOrgPriorityObj.put("stringValue", stringValueOrgPriorityArray);
        }
        // ISPMO_PROJECT_TYPE
        JSONObject tokenIspmoProjectTypeObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PROJECT_TYPE")) {
            tokenIspmoProjectTypeObj.put("token", "REQD.ISPMO_PROJECT_TYPE");
            // Set the stringValue Array
            JSONArray stringValueIspmoProjectTypeArray = new JSONArray();
            stringValueIspmoProjectTypeArray.put(itProjectData.get("ISPMO_PROJECT_TYPE"));
            // Add Array to the JSONObject
            tokenIspmoProjectTypeObj.put("stringValue", stringValueIspmoProjectTypeArray);
        }
        // ISPMO_PRJ_SHORT_DESC
        JSONObject tokenIspmoPrjShortDescriptionObj = new JSONObject();
        // Check if container key exists
        if (itProjectData.containsKey("ISPMO_PRJ_SHORT_DESC")) {
            tokenIspmoPrjShortDescriptionObj.put("token", "REQD.ISPMO_PRJ_SHORT_DESC");
            // Set the stringValue Array
            JSONArray stringValueIspmoPrjShortDescriptionArray = new JSONArray();
            stringValueIspmoPrjShortDescriptionArray.put(itProjectData.get("ISPMO_PRJ_SHORT_DESC"));
            // Add Array to the JSONObject
            tokenIspmoPrjShortDescriptionObj.put("stringValue", stringValueIspmoPrjShortDescriptionArray);
        }
        // End the IT Project fields to create the IS PMO Feature from the HashMap Array: itProjectData
        // Start with the IT Project Release fields to create the IS PMO Feature from the
        // HashMap Array: itReleaseData
        // ISPMO_INCL_RETAIL_BUILD
        JSONObject tokenIspmoRetailBuildObj = new JSONObject();
        // Check if container key exists
        if (itReleaseData.containsKey("ISPMO_INCL_RETAIL_BUILD")) {
            tokenIspmoRetailBuildObj.put("token", "REQD.ISPMO_INCL_RETAIL_BUILD");
            // Set the stringValue Array
            JSONArray stringValueIspmoRetailBuildArray = new JSONArray();
            stringValueIspmoRetailBuildArray.put(itReleaseData.get("ISPMO_INCL_RETAIL_BUILD"));
            // Add Array to the JSONObject
            tokenIspmoRetailBuildObj.put("stringValue", stringValueIspmoRetailBuildArray);
        }
        // ISPMO_INCL_CHARG_SYS
        JSONObject tokenIspmoChargingSystemsObj = new JSONObject();
        // Check if container key exists
        if (itReleaseData.containsKey("ISPMO_INCL_CHARG_SYS")) {
            tokenIspmoChargingSystemsObj.put("token", "REQD.ISPMO_INCL_CHARG_SYS");
            // Set the stringValue Array
            JSONArray stringValueIspmoChargingSystemsArray = new JSONArray();
            stringValueIspmoChargingSystemsArray.put(itReleaseData.get("ISPMO_INCL_CHARG_SYS"));
            // Add Array to the JSONObject
            tokenIspmoChargingSystemsObj.put("stringValue", stringValueIspmoChargingSystemsArray);
        }
        // ISPMO_INCL_WHOLSAL_REL
        JSONObject tokenIspmoWholesaleObj = new JSONObject();
        // Check if container key exists
        if (itReleaseData.containsKey("ISPMO_INCL_WHOLSAL_REL")) {
            tokenIspmoWholesaleObj.put("token", "REQD.ISPMO_INCL_WHOLSAL_REL");
            // Set the stringValue Array
            JSONArray stringValueIspmoWholesaleArray = new JSONArray();
            stringValueIspmoWholesaleArray.put(itReleaseData.get("ISPMO_INCL_WHOLSAL_REL"));
            // Add Array to the JSONObject
            tokenIspmoWholesaleObj.put("stringValue", stringValueIspmoWholesaleArray);
        }
        // ISPMO_INCL_SIYA_REL
        JSONObject tokenIspmoSiyakhulaObj = new JSONObject();
        // Check if container key exists
        if (itReleaseData.containsKey("ISPMO_INCL_SIYA_REL")) {
            tokenIspmoSiyakhulaObj.put("token", "REQD.ISPMO_INCL_SIYA_REL");
            // Set the stringValue Array
            JSONArray stringValueIspmoSiyakhulaArray = new JSONArray();
            stringValueIspmoSiyakhulaArray.put(itReleaseData.get("ISPMO_INCL_SIYA_REL"));
            // Add Array to the JSONObject
            tokenIspmoSiyakhulaObj.put("stringValue", stringValueIspmoSiyakhulaArray);
        }
        // ISPMO_INCL_ILULA_REL
        JSONObject tokenIspmoIlulaObj = new JSONObject();
        // Check if container key exists
        if (itReleaseData.containsKey("ISPMO_INCL_ILULA_REL")) {
            tokenIspmoIlulaObj.put("token", "REQD.ISPMO_INCL_ILULA_REL");
            // Set the stringValue Array
            JSONArray stringValueIspmoIlulaArray = new JSONArray();
            stringValueIspmoIlulaArray.put(itReleaseData.get("ISPMO_INCL_ILULA_REL"));
            // Add Array to the JSONObject
            tokenIspmoIlulaObj.put("stringValue", stringValueIspmoIlulaArray);
        }
        // ISPMO_INCL_SIEBEL_REL
        JSONObject tokenIspmoSiebelObj = new JSONObject();
        // Check if container key exists
        if (itReleaseData.containsKey("ISPMO_INCL_SIEBEL_REL")) {
            tokenIspmoSiebelObj.put("token", "REQD.ISPMO_INCL_SIEBEL_REL");
            // Set the stringValue Array
            JSONArray stringValueIspmoSiebelArray = new JSONArray();
            stringValueIspmoSiebelArray.put(itReleaseData.get("ISPMO_INCL_SIEBEL_REL"));
            // Add Array to the JSONObject
            tokenIspmoSiebelObj.put("stringValue", stringValueIspmoSiebelArray);
        }
        // End the IT Project Release fields to create the IS PMO Feature from the HashMap Array: itReleaseData
        // Start with the EPMO Project fields to create the IS PMO Feature from the HashMap Array: epmoPrjData
        // ISPMO_EPMO_PRJ_URL
        JSONObject tokenEpmoProjectUrlObj = new JSONObject();
        // Check if container key exists
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_PRJ_URL")) {
            tokenEpmoProjectUrlObj.put("token", "REQD.ISPMO_EPMO_PRJ_URL");
            // Set the stringValue Array
            JSONArray stringValueEpmoProjectUrlArray = new JSONArray();
            stringValueEpmoProjectUrlArray.put(epmoPrjData.get("ISPMO_EPMO_PRJ_URL"));
            // Add Array to the JSONObject
            tokenEpmoProjectUrlObj.put("stringValue", stringValueEpmoProjectUrlArray);
        }
        // ISPMO_EPMO_PRJ_NUM
        JSONObject tokenEpmoProjectNumberObj = new JSONObject();
        // Check if container key exists
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_PRJ_NUM")) {
            tokenEpmoProjectNumberObj.put("token", "REQD.ISPMO_EPMO_PRJ_NUM");
            // Set the stringValue Array
            JSONArray stringValueEpmoProjectNumberArray = new JSONArray();
            stringValueEpmoProjectNumberArray.put(epmoPrjData.get("ISPMO_EPMO_PRJ_NUM"));
            // Add Array to the JSONObject
            tokenEpmoProjectNumberObj.put("stringValue", stringValueEpmoProjectNumberArray);
        }
        // ISPMO_EPMO_PM
        JSONObject tokenEpmoProjectManagerObj = new JSONObject();
        // Check if container key exists
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_PM")) {
            tokenEpmoProjectManagerObj.put("token", "REQD.ISPMO_EPMO_PM");
            // Set the stringValue Array
            JSONArray stringValueEpmoProjectManagerArray = new JSONArray();
            stringValueEpmoProjectManagerArray.put(epmoPrjData.get("ISPMO_EPMO_PM"));
            // Add Array to the JSONObject
            tokenEpmoProjectManagerObj.put("stringValue", stringValueEpmoProjectManagerArray);
        }
        // ISPMO_EPMO_TYPE
        JSONObject tokenEpmoProjectTypeObj = new JSONObject();
        // Check if container key exists
        if (!epmoPrjData.isEmpty() && epmoPrjData.containsKey("ISPMO_EPMO_TYPE")) {
            tokenEpmoProjectTypeObj.put("token", "REQD.ISPMO_EPMO_TYPE");
            // Set the stringValue Array
            JSONArray stringValueEpmoProjectTypeArray = new JSONArray();
            stringValueEpmoProjectTypeArray.put(epmoPrjData.get("ISPMO_EPMO_TYPE"));
            // Add Array to the JSONObject
            tokenEpmoProjectTypeObj.put("stringValue", stringValueEpmoProjectTypeArray);
        }
        // End the EPMO Project fields to create the IS PMO Feature from the HashMap Array: epmoPrjData
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.put(tokensLastUpdateDateObj);
        fieldArray.put(tokenEntityLastUpdateDateObj);
        fieldArray.put(tokenItProjectNameObj);
        fieldArray.put(tokenDescriptionObj);
        fieldArray.put(tokenImpactAssessmentNumObj);
        fieldArray.put(tokenIsDomainObj);
        if (!tokenIspmoPrjNumObj.isEmpty()) fieldArray.put(tokenIspmoPrjNumObj);
        if (!tokenIspmoPrjUrlObj.isEmpty()) fieldArray.put(tokenIspmoPrjUrlObj);
        if (!tokenIspmoPrjPhaseObj.isEmpty()) fieldArray.put(tokenIspmoPrjPhaseObj);
        if (!tokenEpmoProjectNumObj.isEmpty()) fieldArray.put(tokenEpmoProjectNumObj);
        if (!tokenIspmoPmObj.isEmpty()) fieldArray.put(tokenIspmoPmObj);
        if (!tokenIspmoPrjRagObj.isEmpty()) fieldArray.put(tokenIspmoPrjRagObj);
        if (!tokenBusinessUnitObj.isEmpty()) fieldArray.put(tokenBusinessUnitObj);
        if (!tokenSubAreaObj.isEmpty()) fieldArray.put(tokenSubAreaObj);
        if (!tokenBuPriorityObj.isEmpty()) fieldArray.put(tokenBuPriorityObj);
        if (!tokenOrgPriorityObj.isEmpty()) fieldArray.put(tokenOrgPriorityObj);
        if (!tokenIspmoProjectTypeObj.isEmpty()) fieldArray.put(tokenIspmoProjectTypeObj);
        if (!tokenIspmoPrjShortDescriptionObj.isEmpty()) fieldArray.put(tokenIspmoPrjShortDescriptionObj);
        if (!tokenIspmoRetailBuildObj.isEmpty()) fieldArray.put(tokenIspmoRetailBuildObj);
        if (!tokenIspmoChargingSystemsObj.isEmpty()) fieldArray.put(tokenIspmoChargingSystemsObj);
        if (!tokenIspmoWholesaleObj.isEmpty()) fieldArray.put(tokenIspmoWholesaleObj);
        if (!tokenIspmoSiyakhulaObj.isEmpty()) fieldArray.put(tokenIspmoSiyakhulaObj);
        if (!tokenIspmoIlulaObj.isEmpty()) fieldArray.put(tokenIspmoIlulaObj);
        if (!tokenIspmoSiebelObj.isEmpty()) fieldArray.put(tokenIspmoSiebelObj);
        if (!tokenEpmoProjectUrlObj.isEmpty()) fieldArray.put(tokenEpmoProjectUrlObj);
        if (!tokenEpmoProjectNumberObj.isEmpty()) fieldArray.put(tokenEpmoProjectNumberObj);
        if (!tokenEpmoProjectManagerObj.isEmpty()) fieldArray.put(tokenEpmoProjectManagerObj);
        if (!tokenEpmoProjectTypeObj.isEmpty()) fieldArray.put(tokenEpmoProjectTypeObj);
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
     * Method that create the IS PMO Feature per IS Domain
     *
     * @param ppmBaseUrl    PPM Base URL for identifying the PPM environment
     * @param username      PPM User for access to the PPM entities.
     * @param password      PPM User password
     * @param restUrl       REST API URL for the method
     * @param iaRequestId   IS PMO Impact Assessment Request ID
     * @param iaProjectId   IS PMO Impact Assessment linked IT Project ID
     * @param iaProjectName IS PMO Impact Assessment linked IT Project
     * @param itProjectData IS PMO Impact Assessment linked IT Project Data
     * @param itReleaseData IS PMO Impact Assessment linked IT Project Release Data
     * @param epmoPrjData   IS PMO Impact Assessment linked IT Project's EPMO Project Data
     * @return New IS PMO Feature Request ID String
     * @throws IOException   IO Exceptions are thrown up to the main class method
     * @throws JSONException JSON Exceptions are thrown up to the main class method
     */
    protected String createIspmoFeatureRequest(String ppmBaseUrl, String username, String password, String restUrl, String iaRequestId, String iaProjectId, String iaProjectName, String iaDomain, HashMap<String, String> itProjectData, HashMap<String, String> itReleaseData, HashMap<String, String> epmoPrjData) throws IOException, JSONException {

        // REST API URL
        String requestUrl = ppmBaseUrl + restUrl;
        log("POST Request Creating RT URL: " + requestUrl);
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
        String jsonPayload = setJsonObjectCreateRequestType(iaRequestId, iaProjectName, iaDomain, itProjectData, itReleaseData, epmoPrjData).toString();
        log("Create IS PMO Feature Pay Load: " + jsonPayload);
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
        if (response.code() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }

        // JSONTokener - Set all the JSON keys as a token from the Json Return string.
        assert response.body() != null : "The POST Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("Successful POST response output Updating RT: " + json);
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
        if (response.code() == 200) {
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
     * @param itProjectMilestoneObjArray IT Project Major Milestone data
     * @return Json Object with the payload
     */
    private JSONObject setJsonObjectUpdateFeatureRequestTypeImpactedSystemFields(ArrayList<ImpactedSystemValues> impactedSystemsObjArray, ArrayList<ImpactedSystemValues> allImpactedSystemsObjArray, ArrayList<ProjectMilestoneValues> itProjectMilestoneObjArray) throws ParseException {
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
        // RT Token: ISPMO_MILESTONES
        JSONObject tokenProjectMilestoneObj = new JSONObject();
        tokenProjectMilestoneObj.put("token", "REQD.ISPMO_MILESTONES");
        // Set the stringValue Array for the Involvement
        JSONArray stringValueProjectMilestoneArray = new JSONArray();
        String projectMilestoneString = setProjectMilestoneHtml(itProjectMilestoneObjArray);
        stringValueProjectMilestoneArray.put(projectMilestoneString);
        tokenProjectMilestoneObj.put("stringValue", stringValueProjectMilestoneArray);
        // Set the Field Array for the IS PMO Feature Request dynamically depending on the data
        JSONArray fieldArray = new JSONArray();
        fieldArray.put(tokensLastUpdateDateObj);
        fieldArray.put(tokenEntityLastUpdateDateObj);
        fieldArray.put(tokenImpactedSystemsObj);
        fieldArray.put(tokenInvolvementObj);
        fieldArray.put(tokenAllInvolvementObj);
        fieldArray.put(tokenProjectMilestoneObj);
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
    protected void updateFeatureRequestTypeImpactedSystemFields(String ppmBaseUrl, String username, String password, String restUrl, String featureReqId, ArrayList<ImpactedSystemValues> isValuesObjArray, ArrayList<ImpactedSystemValues> isAllValuesObjArray, ArrayList<ProjectMilestoneValues> prjMilestones) throws IOException, JSONException, ParseException {

        // REST API URL
        String requestUrl = ppmBaseUrl + restUrl + "/" + featureReqId;
        log("PUT Request Update RT URL: " + requestUrl);
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
        String jsonPayload = setJsonObjectUpdateFeatureRequestTypeImpactedSystemFields(isValuesObjArray, isAllValuesObjArray, prjMilestones).toString();
        log("Create IS PMO Feature Pay Load: " + jsonPayload);
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
        if (response.code() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.code());
        }

        // JSONTokener - Set all the JSON keys as a token from the Json Return string.
        assert response.body() != null : "The PUT Return Body is Empty";
        // Set the JSONObject from the Response Body
        JSONObject json = new JSONObject(response.body().string());
        log("Successful PUT response output Updating RT: " + json);
        // Disconnect the connection
        response.close();
    }

    /**
     * Method to set the Impacted Systems String for the REST API PUT Request
     *
     * @param impactedSystemObj ArrayList with ImpactedSystemsValue Object data
     * @return Impacted Systems semicolon separated String
     */
    private String setImpactedSystemString(ArrayList<ImpactedSystemValues> impactedSystemObj) {
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
        String result = null;
        // Build the string with HTML tags for a table
        if (!domainInvolvementObj.isEmpty()) {

            result = "<table style=\"border: 1px solid black; border-collapse: collapse; width: 98%;\">";
            result = result.concat("<tr>");
            result = result.concat("<td style=\"font-weight: bold; width: 39%;\">Systems Impacted</td>");
            result = result.concat("<td style=\"font-weight: bold; width: 39%;\">Involvement</td>");
            result = result.concat("<td style=\"font-weight: bold;\">Effort Estimation (Hours)</td>");
            result = result.concat("</tr>");
            // Iterate through the Domain Involvement Object
            for (ImpactedSystemValues impactedSystemValues : domainInvolvementObj) {
                result = result.concat("<tr>");
                // Systems Impacted
                result = result.concat("<td>").concat(impactedSystemValues.getImpactedSystem()).concat("</td>");
                // Involvement
                result = result.concat("<td>").concat(impactedSystemValues.getInvolvement()).concat("</td>");
                // Effort Estimation (Hours)
                result = result.concat("<td>").concat(impactedSystemValues.getEstimateHours()).concat("</td>");
                result = result.concat("</tr>");
                // <<-- Testing multiple rows when field is not edible-->>
            }
            result = result.concat("</table>");
        }
        return result;
    }

    /**
     * Method to set the HTML for the IT Project Milestones
     *
     * @param projectMilestoneObj IT Project Major Milestone value array list
     * @return HTML string
     */
    private String setProjectMilestoneHtml(ArrayList<ProjectMilestoneValues> projectMilestoneObj) throws ParseException {
        String result = null;
        // Format the date for display in HTML table
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        // Build the string with HTML tags for a table
        if (!projectMilestoneObj.isEmpty()) {

            result = "<table style=\"border: 1px solid black; border-collapse: collapse; width: 98%;\">";
            result = result.concat("<tr>");
            result = result.concat("<td style=\"font-weight: bold; width: 40%;\">Milestone</td>");
            result = result.concat("<td style=\"font-weight: bold; width: 20%;\">Scheduled Finish</td>");
            result = result.concat("<td style=\"font-weight: bold; width: 20%;\">Actual Finish</td>");
            result = result.concat("<td style=\"font-weight: bold;\">Status</td>");
            result = result.concat("</tr>");
            // Iterate through the Domain Involvement Object
            for (ProjectMilestoneValues projectMilestoneValues : projectMilestoneObj) {
                result = result.concat("<tr>");
                // Milestone
                result = result.concat("<td>").concat(projectMilestoneValues.getMilestoneTaskName()).concat("</td>");
                // Scheduled Finish
                String milestoneScheduledFinishDate = projectMilestoneValues.getMilestoneScheduledFinishDate();
                if (milestoneScheduledFinishDate.equalsIgnoreCase("null")) {
                    result = result.concat("<td>").concat("-").concat("</td>");
                } else {
                    result = result.concat("<td>").concat(formatter.parse(milestoneScheduledFinishDate).toString()).concat("</td>");
                }
                // Actual Finish
                String milestoneActualFinishDate = projectMilestoneValues.getMilestoneActualFinishDate();
                if (milestoneActualFinishDate.equalsIgnoreCase("null")) {
                    result = result.concat("<td>").concat("-").concat("</td>");
                } else {
                    result = result.concat("<td>").concat(formatter.parse(milestoneActualFinishDate).toString()).concat("</td>");
                }
                // Status
                result = result.concat("<td>").concat(projectMilestoneValues.getMilestoneTaskStatus()).concat("</td>");
                result = result.concat("</tr>");
                // <<-- Testing multiple rows when field is not edible-->>
            }
            result = result.concat("</table>");
        }
        return result;
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
