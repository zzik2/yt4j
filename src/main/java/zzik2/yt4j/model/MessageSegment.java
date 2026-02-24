package zzik2.yt4j.model;

public interface MessageSegment {

    final class Text implements MessageSegment {
        private final String value;

        public Text(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    final class EmojiSegment implements MessageSegment {
        private final Emoji emoji;

        public EmojiSegment(Emoji emoji) {
            this.emoji = emoji;
        }

        public Emoji emoji() {
            return emoji;
        }

        @Override
        public String toString() {
            return emoji.shortcuts().isEmpty() ? emoji.emojiId() : emoji.shortcuts().get(0);
        }
    }
}
