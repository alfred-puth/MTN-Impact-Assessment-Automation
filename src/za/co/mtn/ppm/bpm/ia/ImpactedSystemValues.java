package za.co.mtn.ppm.bpm.ia;

/**
 * Class for storing the Impact Assessment Request Type, Impacted Systems table component values
 */
public class ImpactedSystemValues {
    private final String octaneWorkspace;
    private final String isDomain;
    private final String impactedSystem;
    private final String involvement;
    private final String estimateHours;

    /**
     * Constructor method for the class storing values from the Impact Assessment Request Type, Impacted Systems table component.
     *
     * @param octaneWorkspace Octane Workspace value
     * @param isDomain        IS Domain value
     * @param impactedSystem  Impacted System value
     * @param involvement     Involvement value
     * @param estimateHours   Estimated Hours value
     */
    protected ImpactedSystemValues(String octaneWorkspace, String isDomain, String impactedSystem, String involvement, String estimateHours) {
        this.octaneWorkspace = octaneWorkspace;
        this.isDomain = isDomain;
        this.impactedSystem = impactedSystem;
        this.involvement = involvement;
        this.estimateHours = estimateHours;
    }

    /**
     * Get method for Octane Workspace
     *
     * @return Octane Workspace string value
     */
    protected String getOctaneWorkspace() {
        return octaneWorkspace;
    }

    /**
     * Get method for IS Domain
     *
     * @return IS Domain string value
     */
    protected String getIsDomain() {
        return isDomain;
    }

    /**
     * Get method for Impacted Systems
     *
     * @return Impacted System string value
     */
    protected String getImpactedSystem() {
        return impactedSystem;
    }

    /**
     * Get method for Involvement
     *
     * @return Involvement string value
     */
    protected String getInvolvement() {
        return involvement;
    }

    /**
     * Get method for Estimated Hours
     *
     * @return Estimated Hours string value
     */
    protected String getEstimateHours() {
        return estimateHours;
    }
}
