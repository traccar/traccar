package org.socratec.model;

import java.util.List;

public class LogbookEntriesResult {
    private int totalRequested;
    private int successful;
    private int failed;
    private List<LogbookEntryResult> results;

    public LogbookEntriesResult() {
    }

    public LogbookEntriesResult(int totalRequested, int successful, int failed, List<LogbookEntryResult> results) {
        this.totalRequested = totalRequested;
        this.successful = successful;
        this.failed = failed;
        this.results = results;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getSuccessful() {
        return successful;
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<LogbookEntryResult> getResults() {
        return results;
    }

    public void setResults(List<LogbookEntryResult> results) {
        this.results = results;
    }
}
