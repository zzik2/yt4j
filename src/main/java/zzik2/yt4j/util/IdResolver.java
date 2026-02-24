package zzik2.yt4j.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IdResolver {
    private static final Pattern WATCH_V = Pattern.compile("[?&]v=([^&]+)");
    private static final Pattern EMBED = Pattern.compile("/embed/([^/?]+)");
    private static final Pattern SHORT_URL = Pattern.compile("youtu\\.be/([^/?]+)");
    private static final Pattern CHANNEL = Pattern.compile("/channel/([^/?]+)");
    private static final Pattern USER_HANDLE = Pattern.compile("/@([^/?]+)");

    private IdResolver() {}

    public static String resolveVideoId(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("입력이 비어있습니다");
        }
        if (isPlainId(input)) return input;

        Matcher m;

        m = WATCH_V.matcher(input);
        if (m.find()) return m.group(1);

        m = EMBED.matcher(input);
        if (m.find()) return m.group(1);

        m = SHORT_URL.matcher(input);
        if (m.find()) return m.group(1);

        throw new IllegalArgumentException("영상 ID를 추출할 수 없습니다: " + input);
    }

    public static String resolveChannelId(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("입력이 비어있습니다");
        }
        if (isPlainId(input)) return input;

        Matcher m = CHANNEL.matcher(input);
        if (m.find()) return m.group(1);

        throw new IllegalArgumentException("채널 ID를 추출할 수 없습니다: " + input);
    }

    public static String resolveUserHandle(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("입력이 비어있습니다");
        }
        if (isPlainId(input)) return input;

        Matcher m = USER_HANDLE.matcher(input);
        if (m.find()) return m.group(1);

        throw new IllegalArgumentException("사용자 핸들을 추출할 수 없습니다: " + input);
    }

    private static boolean isPlainId(String input) {
        return !input.contains("/") && !input.contains("?") && !input.contains("&") && !input.contains(".");
    }
}
