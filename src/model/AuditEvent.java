package model;

import java.time.LocalDateTime;

public class AuditEvent {
    private final long eventId;
    private final Integer actorId;
    private final String actorRole;
    private final String action;
    private final String entityType;
    private final String entityId;
    private final String subject;
    private final String outcome;
    private final String details;
    private final LocalDateTime createdAt;

    public AuditEvent(long eventId, Integer actorId, String actorRole, String action,
            String entityType, String entityId, String subject, String outcome, String details,
            LocalDateTime createdAt) {
        this.eventId = eventId;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.subject = subject;
        this.outcome = outcome;
        this.details = details;
        this.createdAt = createdAt;
    }

    public long getEventId() { return eventId; }
    public Integer getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getSubject() { return subject; }
    public String getOutcome() { return outcome; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
