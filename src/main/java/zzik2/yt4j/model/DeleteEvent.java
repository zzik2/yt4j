package zzik2.yt4j.model;

public final class DeleteEvent {
    private final String targetId;
    private final String message;

    public DeleteEvent(String targetId, String message) {
        this.targetId = targetId;
        this.message = message;
    }

    public String targetId() {
        return targetId;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "DeleteEvent{targetId='" + targetId + "', message='" + message + "'}";
    }
}
