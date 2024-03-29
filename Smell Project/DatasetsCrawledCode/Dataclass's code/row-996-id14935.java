@InterfaceAudience.Private
@InterfaceStability.Unstable
public class JobStatusChangedEvent implements HistoryEvent {
 private JobStatusChanged datum = new JobStatusChanged();


 /**
   * Create an event to record the change in the Job Status
   * @param id Job ID
   * @param jobStatus The new job status
   */
 public JobStatusChangedEvent(JobID id, String jobStatus) {
 datum.jobid = new Utf8(id.toString());
 datum.jobStatus = new Utf8(jobStatus);
  }


 JobStatusChangedEvent() {}


 public Object getDatum() { return datum; }
 public void setDatum(Object datum) {
 this.datum = (JobStatusChanged)datum;
  }


 /** Get the Job Id */
 public JobID getJobId() { return JobID.forName(datum.jobid.toString()); }
 /** Get the event status */
 public String getStatus() { return datum.jobStatus.toString(); }
 /** Get the event type */
 public EventType getEventType() {
 return EventType.JOB_STATUS_CHANGED;
  }


}