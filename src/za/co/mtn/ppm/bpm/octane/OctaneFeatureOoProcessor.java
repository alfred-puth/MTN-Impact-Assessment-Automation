package za.co.mtn.ppm.bpm.octane;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * Class for using OpenText OO application to Update Octane Features
 */
public class OctaneFeatureOoProcessor {
    // Class Constant for OO REST URL
    private static final String OO_URL = "oo/rest/v2/executions";
    // Class Variables set by the constructor
    private final String ooBaseUrl;
    private final String ooAuthKey;
    private final String octFeatureUrl;
    private final String featureImpactedSystemList;

    /**
     * Constructor to set OO POST variables for the Updating of Impacted Systems
     *
     * @param ooBaseUrl                 OO Base URL
     * @param ooAuthKey                 OO Authentication Key
     * @param octFeatureUrl             PPM Feature Request ID
     * @param featureImpactedSystemList PPM Feature Impacted System list string
     */
    public OctaneFeatureOoProcessor(String ooBaseUrl, String ooAuthKey, String octFeatureUrl, String featureImpactedSystemList) {
        this.ooBaseUrl = ooBaseUrl;
        this.ooAuthKey = ooAuthKey;
        this.octFeatureUrl = octFeatureUrl;
        this.featureImpactedSystemList = featureImpactedSystemList;
    }

    /**
     * Method to update the Octane Feature Impacted Systems with OpenText OO Application
     * OO REST Request - POST with Json Payload
     *
     * @throws IOException Java IO Exceptions
     */
    public void updateOctaneFeatureImpactedSystems() throws IOException {
        // REST API URL
        String ooUrl = getOoBaseUrl() + OO_URL;
        log("<<- Update OO Impacted Systems with OO ->>");
        log("POST Request OO URL: " + ooUrl);
        // OO Authentication Key for Basic Authentication
        final String authHeader = "Basic " + getOoAuthKey();
        // Set the POST Request and all the parameters
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS).build();
        MediaType mediaType = MediaType.parse("application/json");
        // Set the Json Payload for the POST Request
        String jsonPayload = setJsonObjectOORequest(featureImpactedSystemList, getOctFeatureUrl()).toString();
        log("Create OO Pay Load: " + jsonPayload);
        // POST Request Body
        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        // POST Request
        Request request = new Request.Builder()
                .url(ooUrl).addHeader("Authorization", authHeader)
                .addHeader("accept", "application/json")
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
        log("Successful POST OO Response code: " + response.code() + " and OO Response Message: " + response.body().string());
        // Disconnect the connection
        response.close();
    }

    /**
     * Set JSON Object with the Jsdon Payload for the OO Post Request
     *
     * @param impactedApplications String list of Impacted Systems
     * @param featureUrl           String with Octane Feature URL
     * @return JSON Object with the Payload
     */
    private JSONObject setJsonObjectOORequest(String impactedApplications, String featureUrl) {
        JSONObject jsonInputs = new JSONObject();
        jsonInputs.put("Impacted_Applications", impactedApplications);
        jsonInputs.put("Feature_URL", featureUrl);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("flowUuid", "538549a6-6ddb-42fd-bf39-c46fd5040eab");
        jsonObj.put("runName", "PPM_Octane_Move_Feature");
        jsonObj.put("logLevel", "STANDARD");
        jsonObj.put("inputs", jsonInputs);
        return jsonObj;
    }

    /**
     * Getter for the Octane Base URL
     *
     * @return Octane Base URL string
     */
    private String getOoBaseUrl() {
        return ooBaseUrl;
    }

    /**
     * Getter for the OO Authentication Key
     *
     * @return OO Authentication Key string
     */
    private String getOoAuthKey() {
        return ooAuthKey;
    }

    private String getOctFeatureUrl() {
        return octFeatureUrl;
    }

    private String getFeatureImpactedSystemList() {
        return featureImpactedSystemList;
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
