package za.co.mtn.ppm.bpm.ia;

/**
 * Class for storing the IS PMO Feature and IS PMO Testing Feature values
 */
public class FeatureValues {
    private final String feature_request_id;
    private final String feature_is_Domain;

    /**
     * Constructor method for the class storing the IS PMO Feature and IS PMO Testing Feature values
     *
     * @param featureRequestId IS PMO Feature or IS PMO Testing Feature Request ID
     * @param featureIsDomain  IS PMO Feature or IS PMO Testing Feature IS Domain
     */
    protected FeatureValues(String featureRequestId, String featureIsDomain) {
        feature_request_id = featureRequestId;
        feature_is_Domain = featureIsDomain;
    }

    /**
     * Get method for IS PMO Feature or IS PMO Testing Feature Request ID
     *
     * @return IS PMO Feature or IS PMO Testing Feature Request ID string value
     */
    protected String getFeature_request_id() {
        return feature_request_id;
    }

    /**
     * et method for IS PMO Feature or IS PMO Testing Feature IS Domain
     *
     * @return IS PMO Feature or IS PMO Testing Feature Request IS Domain string value
     */
    protected String getFeature_is_Domain() {
        return feature_is_Domain;
    }
}
