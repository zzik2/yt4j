package zzik2.yt4j.model;

public final class BroadcastInfo {
    private final boolean liveNow;
    private final String startTimestamp;
    private final String endTimestamp;

    public BroadcastInfo(boolean liveNow, String startTimestamp, String endTimestamp) {
        this.liveNow = liveNow;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    public boolean isLiveNow() {
        return liveNow;
    }

    public String startTimestamp() {
        return startTimestamp;
    }

    public String endTimestamp() {
        return endTimestamp;
    }

    @Override
    public String toString() {
        return "BroadcastInfo{liveNow=" + liveNow
                + ", start='" + startTimestamp + "'"
                + ", end='" + endTimestamp + "'}";
    }
}
