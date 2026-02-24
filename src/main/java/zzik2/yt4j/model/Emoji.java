package zzik2.yt4j.model;

import java.util.Collections;
import java.util.List;

public final class Emoji {
    private final String emojiId;
    private final List<String> shortcuts;
    private final List<String> searchTerms;
    private final String iconUrl;
    private final boolean custom;

    private Emoji(Builder builder) {
        this.emojiId = builder.emojiId;
        this.shortcuts = builder.shortcuts == null ? Collections.emptyList() : Collections.unmodifiableList(builder.shortcuts);
        this.searchTerms = builder.searchTerms == null ? Collections.emptyList() : Collections.unmodifiableList(builder.searchTerms);
        this.iconUrl = builder.iconUrl;
        this.custom = builder.custom;
    }

    public String emojiId() {
        return emojiId;
    }

    public List<String> shortcuts() {
        return shortcuts;
    }

    public List<String> searchTerms() {
        return searchTerms;
    }

    public String iconUrl() {
        return iconUrl;
    }

    public boolean isCustom() {
        return custom;
    }

    @Override
    public String toString() {
        return "Emoji{id='" + emojiId + "', custom=" + custom + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String emojiId;
        private List<String> shortcuts;
        private List<String> searchTerms;
        private String iconUrl;
        private boolean custom;

        private Builder() {
        }

        public Builder emojiId(String emojiId) {
            this.emojiId = emojiId;
            return this;
        }

        public Builder shortcuts(List<String> shortcuts) {
            this.shortcuts = shortcuts;
            return this;
        }

        public Builder searchTerms(List<String> searchTerms) {
            this.searchTerms = searchTerms;
            return this;
        }

        public Builder iconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }

        public Builder custom(boolean custom) {
            this.custom = custom;
            return this;
        }

        public Emoji build() {
            return new Emoji(this);
        }
    }
}
