package com.github.str3ets.playerMessages;

import java.util.UUID;

public class ReportRecord {
    public final UUID id;
    public final long createdAt;

    public final UUID reporterUuid;
    public final String reporterName;

    public final UUID targetUuid;
    public final String targetName;

    public final String reason;
    public final String details;

    public final boolean handled;
    public final String handledBy;
    public final long handledAt;
    public final String resolution;

    public ReportRecord(UUID id, long createdAt,
                        UUID reporterUuid, String reporterName,
                        UUID targetUuid, String targetName,
                        String reason, String details,
                        boolean handled, String handledBy, long handledAt, String resolution) {
        this.id = id;
        this.createdAt = createdAt;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reason = reason;
        this.details = details;
        this.handled = handled;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.resolution = resolution;
    }
}
