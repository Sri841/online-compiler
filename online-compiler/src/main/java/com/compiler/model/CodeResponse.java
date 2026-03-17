package com.compiler.model;

public class CodeResponse {
    private String output;
    private String error;
    private String status;
    private long   executionTimeMs;
    private Long   submissionId;

    public CodeResponse() {}
    public CodeResponse(String output, String error, String status, long ms, Long id) {
        this.output = output; this.error = error; this.status = status;
        this.executionTimeMs = ms; this.submissionId = id;
    }
    public String getOutput()                { return output; }
    public void   setOutput(String v)        { this.output = v; }
    public String getError()                 { return error; }
    public void   setError(String v)         { this.error = v; }
    public String getStatus()                { return status; }
    public void   setStatus(String v)        { this.status = v; }
    public long   getExecutionTimeMs()       { return executionTimeMs; }
    public void   setExecutionTimeMs(long v) { this.executionTimeMs = v; }
    public Long   getSubmissionId()          { return submissionId; }
    public void   setSubmissionId(Long v)    { this.submissionId = v; }
}
