package za.co.mtn.ppm.bpm.ia;

/**
 * Class for storing the IS PMO Feature and IS PMO Testing Feature values
 */
public class FeatureValues {
    private final String featureRequestId;
    private final String featureIsDomain;
    private final String octaneFeatureUrl;

    /**
     * Constructor method for the class storing the IS PMO Feature and IS PMO Testing Feature values
     *
     * @param featureRequestId IS PMO Feature or IS PMO Testing Feature Request ID
     * @param featureIsDomain  IS PMO Feature or IS PMO Testing Feature IS Domain
     */
    protected FeatureValues(String featureRequestId, String featureIsDomain, String octaneFeatureUrl) {
        this.featureRequestId = featureRequestId;
        this.featureIsDomain = featureIsDomain;
        this.octaneFeatureUrl = octaneFeatureUrl;
    }

    /**
     * Get method for IS PMO Feature or IS PMO Testing Feature Request ID
     *
     * @return IS PMO Feature or IS PMO Testing Feature Request ID string value
     */
    protected String getFeatureRequestId() {
        return featureRequestId;
    }

    /**
     * Get method for IS PMO Feature or IS PMO Testing Feature IS Domain
     *
     * @return IS PMO Feature or IS PMO Testing Feature Request IS Domain string value
     */
    protected String getFeatureIsDomain() {
        return featureIsDomain;
    }

    /**
     * Get method for IS PMO Feature or IS PMO Testing Feature Octane URL
     *
     * @return IS PMO Feature or IS PMO Testing Feature Octane URL string value
     */
    public String getOctaneFeatureUrl() {
        return octaneFeatureUrl;
    }
}
