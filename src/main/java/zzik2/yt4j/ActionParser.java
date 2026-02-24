package zzik2.yt4j;

import zzik2.yt4j.model.*;
import zzik2.yt4j.model.*;
import zzik2.yt4j.util.JsonPath;

import java.util.ArrayList;
import java.util.List;

final class ActionParser {

    ActionParser() {}

    List<ChatMessage> parseActions(JsonPath actions) {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < actions.arraySize(); i++) {
            JsonPath action = actions.index(i);

            JsonPath addChatItem = action.get("addChatItemAction");
            if (addChatItem.isAbsent()) {
                JsonPath replay = action.get("replayChatItemAction");
                if (replay.isPresent()) {
                    messages.addAll(parseActions(replay.get("actions")));
                }
                continue;
            }

            JsonPath item = addChatItem.get("item");
            if (item.isAbsent()) continue;

            ChatMessage msg = parseChatItem(item);
            if (msg != null && msg.id() != null) messages.add(msg);
        }
        return messages;
    }

    List<DeleteEvent> parseDeleteActions(JsonPath actions) {
        List<DeleteEvent> deletes = new ArrayList<>();
        for (int i = 0; i < actions.arraySize(); i++) {
            JsonPath action = actions.index(i);
            JsonPath deleted = action.get("markChatItemAsDeletedAction");
            if (deleted.isAbsent()) continue;

            String targetId = deleted.get("targetItemId").asString().orElse(null);
            String message = parseSimpleText(deleted.get("deletedStateMessage"));
            deletes.add(new DeleteEvent(targetId, message));
        }
        return deletes;
    }

    ChatMessage parseChatItem(JsonPath item) {
        JsonPath textRenderer = item.get("liveChatTextMessageRenderer");
        JsonPath paidRenderer = item.get("liveChatPaidMessageRenderer");
        JsonPath stickerRenderer = item.get("liveChatPaidStickerRenderer");
        JsonPath memberRenderer = item.get("liveChatMembershipItemRenderer");

        JsonPath primary = firstPresent(textRenderer, paidRenderer, stickerRenderer, memberRenderer);
        if (primary == null)
            return null;

        ChatMessage.Builder builder = ChatMessage.builder();

        builder.id(primary.get("id").asString().orElse(null));
        builder.authorName(primary.get("authorName", "simpleText").asString().orElse(null));
        builder.authorChannelId(primary.get("authorExternalChannelId").asString().orElse(null));

        primary.get("timestampUsec").asString().ifPresent(ts -> builder.timestampUsec(Long.parseLong(ts)));

        List<MessageSegment> segments = new ArrayList<>();
        String text = parseMessage(primary.get("message"), segments);
        builder.text(text);
        builder.segments(segments);

        builder.authorIconUrl(pickThumbnailUrl(primary.get("authorPhoto", "thumbnails")));

        parseAuthorBadges(primary.get("authorBadges"), builder);

        if (paidRenderer.isPresent()) {
            builder.type(MessageType.SUPER_CHAT);
            builder.purchaseAmount(paidRenderer.get("purchaseAmountText", "simpleText").asString().orElse(null));
            builder.bodyBackgroundColor(paidRenderer.get("bodyBackgroundColor").asInt().orElse(0));
            builder.headerBackgroundColor(paidRenderer.get("headerBackgroundColor").asInt().orElse(0));
            builder.bodyTextColor(paidRenderer.get("bodyTextColor").asInt().orElse(0));
            builder.headerTextColor(paidRenderer.get("headerTextColor").asInt().orElse(0));
            builder.authorNameTextColor(paidRenderer.get("authorNameTextColor").asInt().orElse(0));
        }

        if (stickerRenderer.isPresent()) {
            builder.type(MessageType.SUPER_STICKER);
            builder.purchaseAmount(stickerRenderer.get("purchaseAmountText", "simpleText").asString().orElse(null));
            builder.stickerIconUrl(pickThumbnailUrl(stickerRenderer.get("sticker", "thumbnails")));
        }

        if (memberRenderer.isPresent()) {
            builder.type(MessageType.MEMBERSHIP);
            List<MessageSegment> memberSegments = new ArrayList<>();
            String memberText = parseMessage(memberRenderer.get("headerSubtext"), memberSegments);
            builder.text(memberText);
            builder.segments(memberSegments);
        }

        return builder.build();
    }

    private String parseMessage(JsonPath message, List<MessageSegment> segments) {
        JsonPath runs = message.get("runs");
        if (runs.isAbsent()) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runs.arraySize(); i++) {
            JsonPath run = runs.index(i);

            run.get("text").asString().ifPresent(t -> {
                sb.append(t);
                segments.add(new MessageSegment.Text(t));
            });

            JsonPath emojiObj = run.get("emoji");
            if (emojiObj.isPresent()) {
                Emoji emoji = parseEmoji(emojiObj);
                segments.add(new MessageSegment.EmojiSegment(emoji));
                if (!emoji.shortcuts().isEmpty()) {
                    sb.append(' ').append(emoji.shortcuts().get(0)).append(' ');
                }
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private Emoji parseEmoji(JsonPath emojiObj) {
        Emoji.Builder builder = Emoji.builder();
        builder.emojiId(emojiObj.get("emojiId").asString().orElse(null));
        builder.custom(emojiObj.get("isCustomEmoji").asBoolean().orElse(false));
        builder.iconUrl(pickThumbnailUrl(emojiObj.get("image", "thumbnails")));

        List<String> shortcuts = new ArrayList<>();
        JsonPath shortcutsArr = emojiObj.get("shortcuts");
        for (int i = 0; i < shortcutsArr.arraySize(); i++) {
            shortcutsArr.index(i).asString().ifPresent(shortcuts::add);
        }
        builder.shortcuts(shortcuts);

        List<String> searchTerms = new ArrayList<>();
        JsonPath termsArr = emojiObj.get("searchTerms");
        for (int i = 0; i < termsArr.arraySize(); i++) {
            termsArr.index(i).asString().ifPresent(searchTerms::add);
        }
        builder.searchTerms(searchTerms);

        return builder.build();
    }

    private void parseAuthorBadges(JsonPath badges, ChatMessage.Builder builder) {
        if (badges.isAbsent()) return;

        for (int i = 0; i < badges.arraySize(); i++) {
            JsonPath badge = badges.index(i).get("liveChatAuthorBadgeRenderer");
            if (badge.isAbsent()) continue;

            badge.get("icon", "iconType").asString().ifPresent(type -> {
                switch (type) {
                    case "OWNER":
                        builder.badge(AuthorBadge.OWNER);
                        break;
                    case "MODERATOR":
                        builder.badge(AuthorBadge.MODERATOR);
                        break;
                    case "VERIFIED":
                        builder.badge(AuthorBadge.VERIFIED);
                        break;
                }
            });

            if (badge.get("customThumbnail").isPresent()) {
                builder.badge(AuthorBadge.MEMBER);
                builder.memberBadgeIconUrl(pickThumbnailUrl(badge.get("customThumbnail", "thumbnails")));
            }
        }
    }

    String pickThumbnailUrl(JsonPath thumbnails) {
        if (thumbnails.isAbsent()) return null;
        String url = null;
        long maxWidth = 0;
        for (int i = 0; i < thumbnails.arraySize(); i++) {
            JsonPath thumb = thumbnails.index(i);
            String u = thumb.get("url").asString().orElse(null);
            long w = thumb.get("width").asLong().orElse(0L);
            if (u != null && w >= maxWidth) {
                maxWidth = w;
                url = u;
            }
        }
        return url;
    }

    private String parseSimpleText(JsonPath node) {
        return node.get("simpleText").asString()
                .orElseGet(() -> {
                    JsonPath runs = node.get("runs");
                    if (runs.isAbsent()) return null;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < runs.arraySize(); i++) {
                        runs.index(i).get("text").asString().ifPresent(sb::append);
                    }
                    return sb.length() == 0 ? null : sb.toString();
                });
    }

    private static JsonPath firstPresent(JsonPath... paths) {
        for (JsonPath p : paths) {
            if (p.isPresent()) return p;
        }
        return null;
    }
}
