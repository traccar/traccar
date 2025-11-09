package org.socratec.model;

public class LogbookEntryResult {
    private int index;
    private String status;
    private LogbookEntry entry;
    private String error;
    private LogbookEntry originalEntry;

    public LogbookEntryResult() {
    }

    public LogbookEntryResult(int index, String status) {
        this.index = index;
        this.status = status;
    }

    public static LogbookEntryResult success(int index, LogbookEntry entry) {
        LogbookEntryResult result = new LogbookEntryResult(index, "success");
        result.setEntry(entry);
        return result;
    }

    public static LogbookEntryResult error(int index, String error, LogbookEntry originalEntry) {
        LogbookEntryResult result = new LogbookEntryResult(index, "error");
        result.setError(error);
        result.setOriginalEntry(originalEntry);
        return result;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LogbookEntry getEntry() {
        return entry;
    }

    public void setEntry(LogbookEntry entry) {
        this.entry = entry;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LogbookEntry getOriginalEntry() {
        return originalEntry;
    }

    public void setOriginalEntry(LogbookEntry originalEntry) {
        this.originalEntry = originalEntry;
    }
}
