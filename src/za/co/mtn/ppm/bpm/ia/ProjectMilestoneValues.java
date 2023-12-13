package za.co.mtn.ppm.bpm.ia;

/**
 * Class for storing the IT Project Major Milestone values
 */
public class ProjectMilestoneValues {
    private final String milestoneTaskName;
    private final String milestoneScheduledFinishDate;
    private final String milestoneActualFinishDate;
    private final String milestoneTaskStatus;

    /**
     * Constructor method for the class storing values from IT Project Major Milestone(s).
     *
     * @param milestoneTaskName            Milestone Task Name
     * @param milestoneScheduledFinishDate Scheduled Finish Date for the milestone
     * @param milestoneActualFinishDate    Actual Finish Date for the milestone
     * @param milestoneTaskStatus          Milestone task Status
     */
    protected ProjectMilestoneValues(String milestoneTaskName, String milestoneScheduledFinishDate, String milestoneActualFinishDate, String milestoneTaskStatus) {
        this.milestoneTaskName = milestoneTaskName;
        this.milestoneScheduledFinishDate = milestoneScheduledFinishDate;
        this.milestoneActualFinishDate = milestoneActualFinishDate;
        this.milestoneTaskStatus = milestoneTaskStatus;
    }

    /**
     * Get method for Major Milestone Task Name
     *
     * @return Milestone Task Name string value
     */
    protected String getMilestoneTaskName() {
        return milestoneTaskName;
    }

    /**
     * Get method for Major Milestone Scheduled Finish Date
     *
     * @return Milestone Scheduled Finish Date string value
     */
    protected String getMilestoneScheduledFinishDate() {
        return milestoneScheduledFinishDate;
    }

    /**
     * Get method for Major Milestone Actual Finish Date
     *
     * @return Milestone Actual Finish Date string value
     */
    protected String getMilestoneActualFinishDate() {
        return milestoneActualFinishDate;
    }

    /**
     * Get method for Major Milestone Task Status
     *
     * @return Milestone Task Status string value
     */
    protected String getMilestoneTaskStatus() {
        return milestoneTaskStatus;
    }
}
