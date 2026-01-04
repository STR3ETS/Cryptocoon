package com.github.str3ets.playerMessages;

import java.util.UUID;

public class ReportEntry {
    public final UUID id;
    public final long createdAt;
    public final UUID reporterUuid;
    public final String reporterName;
    public final UUID targetUuid;
    public final String targetName;
    public final String reason;
    public final String details;

    public ReportEntry(UUID id, long createdAt, UUID reporterUuid, String reporterName,
                       UUID targetUuid, String targetName, String reason, String details) {
        this.id = id;
        this.createdAt = createdAt;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reason = reason;
        this.details = details;
    }
}
