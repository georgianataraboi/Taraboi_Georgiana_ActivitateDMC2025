package com.example.myapplication;

public class SyncOperation {
    public int id;
    public String tableName;
    public String recordId;
    public String operation;
    public String data;
    public long timestamp;

    public SyncOperation() {
        // Empty constructor for database operations
    }

    public SyncOperation(String tableName, String recordId, String operation, String data) {
        this.tableName = tableName;
        this.recordId = recordId;
        this.operation = operation;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "SyncOperation{" +
                "id=" + id +
                ", tableName='" + tableName + '\'' +
                ", recordId='" + recordId + '\'' +
                ", operation='" + operation + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}