package zzik2.yt4j.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ChatMessage {
    private final String id;
    private final MessageType type;
    private final String authorName;
    private final String authorChannelId;
    private final String authorIconUrl;
    private final Set<AuthorBadge> badges;
    private final String memberBadgeIconUrl;
    private final String text;
    private final List<MessageSegment> segments;
    private final long timestampUsec;
    private final String purchaseAmount;
    private final String stickerIconUrl;
    private final int bodyBackgroundColor;
    private final int headerBackgroundColor;
    private final int bodyTextColor;
    private final int headerTextColor;
    private final int authorNameTextColor;

    private ChatMessage(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.authorName = builder.authorName;
        this.authorChannelId = builder.authorChannelId;
        this.authorIconUrl = builder.authorIconUrl;
        this.badges = builder.badges.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(builder.badges);
        this.memberBadgeIconUrl = builder.memberBadgeIconUrl;
        this.text = builder.text;
        this.segments = builder.segments == null ? Collections.emptyList() : Collections.unmodifiableList(builder.segments);
        this.timestampUsec = builder.timestampUsec;
        this.purchaseAmount = builder.purchaseAmount;
        this.stickerIconUrl = builder.stickerIconUrl;
        this.bodyBackgroundColor = builder.bodyBackgroundColor;
        this.headerBackgroundColor = builder.headerBackgroundColor;
        this.bodyTextColor = builder.bodyTextColor;
        this.headerTextColor = builder.headerTextColor;
        this.authorNameTextColor = builder.authorNameTextColor;
    }

    public String id() {
        return id;
    }

    public MessageType type() {
        return type;
    }

    public String authorName() {
        return authorName;
    }

    public String authorChannelId() {
        return authorChannelId;
    }

    public String authorIconUrl() {
        return authorIconUrl;
    }

    public Set<AuthorBadge> badges() {
        return badges;
    }

    public String memberBadgeIconUrl() {
        return memberBadgeIconUrl;
    }

    public String text() {
        return text;
    }

    public List<MessageSegment> segments() {
        return segments;
    }

    public long timestampUsec() {
        return timestampUsec;
    }

    public String purchaseAmount() {
        return purchaseAmount;
    }

    public String stickerIconUrl() {
        return stickerIconUrl;
    }

    public int bodyBackgroundColor() {
        return bodyBackgroundColor;
    }

    public int headerBackgroundColor() {
        return headerBackgroundColor;
    }

    public int bodyTextColor() {
        return bodyTextColor;
    }

    public int headerTextColor() {
        return headerTextColor;
    }

    public int authorNameTextColor() {
        return authorNameTextColor;
    }

    public boolean hasBadge(AuthorBadge badge) {
        return badges.contains(badge);
    }

    public boolean isSuperChat() {
        return type == MessageType.SUPER_CHAT || type == MessageType.SUPER_STICKER;
    }

    public boolean isMembership() {
        return type == MessageType.MEMBERSHIP;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ChatMessage{");
        sb.append("type=").append(type);
        sb.append(", author='").append(authorName).append('\'');
        if (text != null)
            sb.append(", text='").append(text).append('\'');
        if (purchaseAmount != null)
            sb.append(", amount='").append(purchaseAmount).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private MessageType type = MessageType.MESSAGE;
        private String authorName;
        private String authorChannelId;
        private String authorIconUrl;
        private final EnumSet<AuthorBadge> badges = EnumSet.noneOf(AuthorBadge.class);
        private String memberBadgeIconUrl;
        private String text;
        private List<MessageSegment> segments;
        private long timestampUsec;
        private String purchaseAmount;
        private String stickerIconUrl;
        private int bodyBackgroundColor;
        private int headerBackgroundColor;
        private int bodyTextColor;
        private int headerTextColor;
        private int authorNameTextColor;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder authorName(String authorName) {
            this.authorName = authorName;
            return this;
        }

        public Builder authorChannelId(String authorChannelId) {
            this.authorChannelId = authorChannelId;
            return this;
        }

        public Builder authorIconUrl(String authorIconUrl) {
            this.authorIconUrl = authorIconUrl;
            return this;
        }

        public Builder badge(AuthorBadge badge) {
            this.badges.add(badge);
            return this;
        }

        public Builder memberBadgeIconUrl(String memberBadgeIconUrl) {
            this.memberBadgeIconUrl = memberBadgeIconUrl;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder segments(List<MessageSegment> segments) {
            this.segments = segments;
            return this;
        }

        public Builder timestampUsec(long timestampUsec) {
            this.timestampUsec = timestampUsec;
            return this;
        }

        public Builder purchaseAmount(String purchaseAmount) {
            this.purchaseAmount = purchaseAmount;
            return this;
        }

        public Builder stickerIconUrl(String stickerIconUrl) {
            this.stickerIconUrl = stickerIconUrl;
            return this;
        }

        public Builder bodyBackgroundColor(int color) {
            this.bodyBackgroundColor = color;
            return this;
        }

        public Builder headerBackgroundColor(int color) {
            this.headerBackgroundColor = color;
            return this;
        }

        public Builder bodyTextColor(int color) {
            this.bodyTextColor = color;
            return this;
        }

        public Builder headerTextColor(int color) {
            this.headerTextColor = color;
            return this;
        }

        public Builder authorNameTextColor(int color) {
            this.authorNameTextColor = color;
            return this;
        }

        public ChatMessage build() {
            return new ChatMessage(this);
        }
    }
}
