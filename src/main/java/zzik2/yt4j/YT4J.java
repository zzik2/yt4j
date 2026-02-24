package zzik2.yt4j;

import zzik2.yt4j.http.HttpClient;
import zzik2.yt4j.model.*;
import zzik2.yt4j.model.BroadcastInfo;
import zzik2.yt4j.model.ChatMessage;
import zzik2.yt4j.model.DeleteEvent;
import zzik2.yt4j.util.IdResolver;
import zzik2.yt4j.util.JsonPath;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube 라이브 채팅 구독 클라이언트.
 *
 * <p>
 * 빌더 패턴으로 인스턴스를 생성하고, {@link #connect()}로 자동 폴링을 시작합니다.
 * </p>
 *
 * <pre>{@code
 * YT4J client = YT4J.builder("VIDEO_ID_OR_URL")
 *         .topChatOnly(true)
 *         .locale(Locale.KOREA)
 *         .onChat(msg -> System.out.println(msg.authorName() + ": " + msg.text()))
 *         .onSuperChat(msg -> System.out.println("후원: " + msg.purchaseAmount()))
 *         .build();
 *
 * client.connect();
 *
 * // ... 나중에
 * client.disconnect();
 * }</pre>
 */
public final class YT4J implements AutoCloseable {
    private static final String LIVE_CHAT_API = "https://www.youtube.com/youtubei/v1/live_chat/get_live_chat?key=";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final Pattern CONTINUATION_TOP = Pattern.compile("\"selected\":true,\"continuation\":\\{\"reloadContinuationData\":\\{\"continuation\":\"([^\"]*)\"");
    private static final Pattern CONTINUATION_ALL = Pattern.compile("\"selected\":false,\"continuation\":\\{\"reloadContinuationData\":\\{\"continuation\":\"([^\"]*)\"");
    private static final Pattern API_KEY = Pattern.compile("\"innertubeApiKey\":\"([^\"]*)\"");
    private static final Pattern IS_REPLAY = Pattern.compile("\"isReplay\":([^,]*)");
    private static final Pattern CHANNEL_ID = Pattern.compile("\"channelId\":\"([^\"]*)\",\"isOwnerViewing\"");
    private static final Pattern VIDEO_ID_FROM_PAGE = Pattern.compile("\"updatedMetadataEndpoint\":\\{\"videoId\":\"([^\"]*)");
    private static final Pattern INIT_DATA = Pattern.compile("window\\[\"ytInitialData\"] = (\\{.*?});\\s*</script>", Pattern.DOTALL);

    private final String inputId;
    private final InputType inputType;
    private final boolean topChatOnly;
    private final Locale locale;
    private final HttpClient http;
    private final ActionParser parser;
    private final List<EventHandler> handlers;
    private final Consumer<ChatMessage> onChat;
    private final Consumer<ChatMessage> onSuperChat;
    private final Consumer<ChatMessage> onMembership;
    private final Consumer<DeleteEvent> onDelete;

    private String videoId;
    private String channelId;
    private String continuation;
    private String apiKey;
    private String visitorData;
    private String clientVersion;
    private boolean replay;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread pollingThread;

    private YT4J(Builder builder) {
        this.inputId = builder.inputId;
        this.inputType = builder.inputType;
        this.topChatOnly = builder.topChatOnly;
        this.locale = builder.locale;
        this.http = new HttpClient(builder.userAgent);
        this.parser = new ActionParser();
        this.handlers = builder.handlers;
        this.onChat = builder.onChat;
        this.onSuperChat = builder.onSuperChat;
        this.onMembership = builder.onMembership;
        this.onDelete = builder.onDelete;
    }

    /**
     * 자동 폴링 스레드를 시작하여 라이브 채팅에 연결합니다.
     *
     * @throws IOException 초기 연결 실패 시
     */
    public void connect() throws IOException {
        initialize();
        if (running.compareAndSet(false, true)) {
            pollingThread = new Thread(this::pollingLoop, "yt4j-polling");
            pollingThread.setDaemon(true);
            pollingThread.start();
        }
    }

    /**
     * 자동 폴링을 중지하고 리소스를 정리합니다.
     */
    public void disconnect() {
        running.set(false);
        Thread t = pollingThread;
        if (t != null) {
            t.interrupt();
            pollingThread = null;
        }
    }

    /**
     * 수동으로 1회 채팅 데이터를 가져옵니다.
     * {@link #connect()} 호출 없이 사용할 수 있습니다.
     *
     * <p>
     * 첫 호출 시 자동으로 초기화를 수행합니다.
     * </p>
     *
     * @throws IOException 네트워크 오류 시
     */
    public void fetch() throws IOException {
        if (continuation == null) {
            initialize();
        }
        fetchOnce();
    }

    /**
     * 방송 정보를 조회합니다.
     *
     * <p>
     * {@link #connect()} 호출 전에도 사용할 수 있습니다.
     * 초기화되지 않은 경우 자동으로 영상 ID를 resolve합니다.
     * </p>
     *
     * @return 방송 정보
     * @throws IOException 네트워크 오류 시
     */
    public BroadcastInfo broadcastInfo() throws IOException {
        String vid = videoId != null ? videoId : resolveVideoIdOnly();
        if (vid == null) {
            return new BroadcastInfo(false, null, null);
        }
        String url = "https://www.youtube.com/watch?v=" + vid + "&hl=en&pbj=1";
        Map<String, String> headers = new HashMap<>();
        headers.put("x-youtube-client-name", "1");
        headers.put("x-youtube-client-version", resolveClientVersion());
        String response = http.get(url, headers);
        JsonPath json = JsonPath.parse(response);
        JsonPath details = findByKey(json, "liveBroadcastDetails");
        if (details.isAbsent()) {
            return new BroadcastInfo(false, null, null);
        }
        return new BroadcastInfo(
                details.get("isLiveNow").asBoolean().orElse(false),
                details.get("startTimestamp").asString().orElse(null),
                details.get("endTimestamp").asString().orElse(null)
        );
    }

    /**
     * 현재 연결된 영상 ID를 반환합니다.
     *
     * @return 영상 ID, 초기화 전이면 null
     */
    public String videoId() {
        return videoId;
    }

    /**
     * 현재 연결된 채널 ID를 반환합니다.
     *
     * @return 채널 ID, 초기화 전이면 null
     */
    public String channelId() {
        return channelId;
    }

    /**
     * 현재 방송이 다시보기인지 여부를 반환합니다.
     *
     * @return 다시보기이면 true
     */
    public boolean isReplay() {
        return replay;
    }

    /**
     * 자동 폴링이 실행 중인지 확인합니다.
     *
     * @return 폴링 중이면 true
     */
    public boolean isConnected() {
        return running.get();
    }

    @Override
    public void close() {
        disconnect();
    }

    // 내부 구현

    private String resolveVideoIdOnly() throws IOException {
        switch (inputType) {
            case VIDEO:
                return IdResolver.resolveVideoId(inputId);
            case CHANNEL: {
                String cid = IdResolver.resolveChannelId(inputId);
                String html = http.get("https://www.youtube.com/channel/" + cid + "/live", Collections.emptyMap());
                Matcher m = VIDEO_ID_FROM_PAGE.matcher(html);
                if (m.find()) return m.group(1);
                return null;
            }
            case USER: {
                String handle = IdResolver.resolveUserHandle(inputId);
                String html = http.get("https://www.youtube.com/@" + handle + "/live", Collections.emptyMap());
                Matcher m = VIDEO_ID_FROM_PAGE.matcher(html);
                if (m.find()) return m.group(1);
                return null;
            }
            default:
                throw new IllegalStateException("알 수 없는 입력 유형: " + inputType);
        }
    }

    private void initialize() throws IOException {
        String html;
        switch (inputType) {
            case VIDEO:
                videoId = IdResolver.resolveVideoId(inputId);
                html = http.get("https://www.youtube.com/watch?v=" + videoId, Collections.emptyMap());
                Matcher chMatcher = CHANNEL_ID.matcher(html);
                if (chMatcher.find()) channelId = chMatcher.group(1);
                break;
            case CHANNEL:
                channelId = IdResolver.resolveChannelId(inputId);
                html = http.get("https://www.youtube.com/channel/" + channelId + "/live", Collections.emptyMap());
                Matcher vidMatcher = VIDEO_ID_FROM_PAGE.matcher(html);
                if (vidMatcher.find()) {
                    videoId = vidMatcher.group(1);
                } else {
                    throw new IOException("채널이 현재 라이브 방송 중이 아닙니다: " + channelId);
                }
                break;
            case USER:
                String handle = IdResolver.resolveUserHandle(inputId);
                html = http.get("https://www.youtube.com/@" + handle + "/live", Collections.emptyMap());
                Matcher uvMatcher = VIDEO_ID_FROM_PAGE.matcher(html);
                if (uvMatcher.find()) {
                    videoId = uvMatcher.group(1);
                } else {
                    throw new IOException("사용자가 현재 라이브 방송 중이 아닙니다: " + handle);
                }
                break;
            default:
                throw new IllegalStateException("알 수 없는 입력 유형: " + inputType);
        }

        Matcher replayMatcher = IS_REPLAY.matcher(html);
        if (replayMatcher.find()) replay = Boolean.parseBoolean(replayMatcher.group(1));

        Matcher contTopMatcher = CONTINUATION_TOP.matcher(html);
        if (contTopMatcher.find()) continuation = contTopMatcher.group(1);

        if (!topChatOnly) {
            Matcher contAllMatcher = CONTINUATION_ALL.matcher(html);
            if (contAllMatcher.find()) continuation = contAllMatcher.group(1);
        }

        Matcher apiKeyMatcher = API_KEY.matcher(html);
        if (apiKeyMatcher.find()) apiKey = apiKeyMatcher.group(1);

        if (continuation == null) {
            throw new IOException("라이브 채팅을 찾을 수 없습니다. 영상이 라이브 방송 중인지 확인하세요: " + videoId);
        }

        html = http.get("https://www.youtube.com/live_chat?v=" + videoId, Collections.emptyMap());
        Matcher initMatcher = INIT_DATA.matcher(html);
        if (initMatcher.find()) {
            JsonPath initJson = JsonPath.parse(initMatcher.group(1));
            JsonPath continuations = initJson.get("contents", "liveChatRenderer", "continuations");
            for (int i = 0; i < continuations.arraySize(); i++) {
                JsonPath cont = continuations.index(i);
                cont.get("invalidationContinuationData", "continuation").asString().ifPresent(c -> this.continuation = c);
            }
        }
    }

    private void pollingLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                fetchOnce();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (running.get()) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        running.set(false);
    }

    private void fetchOnce() throws IOException {
        if (continuation == null) {
            throw new IOException("continuation 토큰이 없습니다. 연결을 다시 시도하세요.");
        }

        String payload = buildPayload();
        String response = http.post(LIVE_CHAT_API + apiKey, payload, Collections.emptyMap());
        JsonPath json = JsonPath.parse(response);

        json.get("responseContext", "visitorData").asString().ifPresent(vd -> {
            if (visitorData == null || visitorData.isEmpty()) visitorData = vd;
        });

        extractClientVersion(json);

        JsonPath liveChatCont = json.get("continuationContents", "liveChatContinuation");
        if (liveChatCont.isAbsent()) return;

        JsonPath actions = liveChatCont.get("actions");
        if (actions.isPresent()) {
            List<ChatMessage> messages = parser.parseActions(actions);
            List<DeleteEvent> deletes = parser.parseDeleteActions(actions);
            dispatch(messages, deletes);
        }

        updateContinuation(liveChatCont.get("continuations"));
    }

    private void dispatch(List<ChatMessage> messages, List<DeleteEvent> deletes) {
        for (ChatMessage msg : messages) {
            switch (msg.type()) {
                case MESSAGE:
                    handlers.forEach(h -> h.onChat(msg));
                    if (onChat != null) onChat.accept(msg);
                    break;
                case SUPER_CHAT:
                case SUPER_STICKER:
                    handlers.forEach(h -> h.onSuperChat(msg));
                    if (onSuperChat != null) onSuperChat.accept(msg);
                    break;
                case MEMBERSHIP:
                    handlers.forEach(h -> h.onMembership(msg));
                    if (onMembership != null) onMembership.accept(msg);
                    break;
            }
        }
        for (DeleteEvent del : deletes) {
            handlers.forEach(h -> h.onDelete(del));
            if (onDelete != null) onDelete.accept(del);
        }
    }

    private void updateContinuation(JsonPath continuations) {
        for (int i = 0; i < continuations.arraySize(); i++) {
            JsonPath cont = continuations.index(i);
            cont.get("invalidationContinuationData", "continuation").asString().ifPresent(c -> this.continuation = c);
            if (continuation == null) {
                cont.get("timedContinuationData", "continuation").asString().ifPresent(c -> this.continuation = c);
            }
            if (continuation == null) {
                cont.get("reloadContinuationData", "continuation").asString().ifPresent(c -> this.continuation = c);
            }
        }
    }

    private String buildPayload() {
        Map<String, Object> json = new LinkedHashMap<>();
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> client = new LinkedHashMap<>();

        if (visitorData != null) client.put("visitorData", visitorData);
        client.put("clientName", "WEB");
        client.put("clientVersion", resolveClientVersion());
        client.put("gl", locale.getCountry());
        client.put("hl", locale.getLanguage());

        context.put("client", client);
        json.put("context", context);
        json.put("continuation", continuation);

        return toJson(json);
    }

    private String resolveClientVersion() {
        if (clientVersion != null) return clientVersion;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return "2." + fmt.format(new Date(System.currentTimeMillis() - 86_400_000L));
    }

    private void extractClientVersion(JsonPath json) {
        JsonPath tracking = json.get("responseContext", "serviceTrackingParams");
        for (int i = 0; i < tracking.arraySize(); i++) {
            JsonPath service = tracking.index(i);
            if (!"CSI".equals(service.get("service").asString().orElse(""))) continue;
            JsonPath params = service.get("params");
            for (int j = 0; j < params.arraySize(); j++) {
                JsonPath param = params.index(j);
                if ("cver".equals(param.get("key").asString().orElse(""))) {
                    param.get("value").asString().ifPresent(v -> clientVersion = v);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {sb.append("\"").append(((String) val).replace("\"", "\\\"")).append("\"");
            } else if (val instanceof Map) {
                sb.append(toJson((Map<String, Object>) val));
            } else {
                sb.append(val);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private JsonPath findByKey(JsonPath node, String key) {
        if (node.isObject()) {
            JsonPath direct = node.get(key);
            if (direct.isPresent()) return direct;
            if (node.element() != null && node.element().isJsonObject()) {
                for (String k : node.element().getAsJsonObject().keySet()) {
                    JsonPath result = findByKey(node.get(k), key);
                    if (result.isPresent()) return result;
                }
            }
        }
        if (node.isArray()) {
            for (int i = 0; i < node.arraySize(); i++) {
                JsonPath result = findByKey(node.index(i), key);
                if (result.isPresent()) return result;
            }
        }
        return JsonPath.of(null);
    }

    // 빌더

    /**
     * 새 빌더를 생성합니다. 영상 ID 또는 URL을 전달합니다.
     *
     * @param videoIdOrUrl 영상 ID, URL, 채널 ID, 또는 사용자 핸들
     * @return 빌더
     */
    public static Builder builder(String videoIdOrUrl) {
        return new Builder(videoIdOrUrl);
    }

    public enum InputType {
        VIDEO, CHANNEL, USER
    }

    public static final class Builder {
        private final String inputId;
        private InputType inputType = InputType.VIDEO;
        private boolean topChatOnly = true;
        private Locale locale = Locale.US;
        private String userAgent = DEFAULT_USER_AGENT;
        private final List<EventHandler> handlers = new CopyOnWriteArrayList<>();
        private Consumer<ChatMessage> onChat;
        private Consumer<ChatMessage> onSuperChat;
        private Consumer<ChatMessage> onMembership;
        private Consumer<DeleteEvent> onDelete;

        private Builder(String inputId) {
            this.inputId = Objects.requireNonNull(inputId, "ID는 null일 수 없습니다");
        }

        /**
         * 입력 유형을 설정합니다. 기본값은 {@link InputType#VIDEO}입니다.
         *
         * @param type 입력 유형
         * @return this
         */
        public Builder inputType(InputType type) {
            this.inputType = Objects.requireNonNull(type);
            return this;
        }

        /**
         * 인기 채팅만 수신할지 설정합니다. 기본값은 true입니다.
         *
         * @param topOnly true이면 인기 채팅만
         * @return this
         */
        public Builder topChatOnly(boolean topOnly) {
            this.topChatOnly = topOnly;
            return this;
        }

        /**
         * 로케일을 설정합니다. YouTube 시스템 메시지 언어와 통화 표시에 영향을 줍니다.
         *
         * @param locale 로케일 (기본값: {@link Locale#US})
         * @return this
         */
        public Builder locale(Locale locale) {
            this.locale = Objects.requireNonNull(locale);
            return this;
        }

        /**
         * User-Agent 문자열을 설정합니다.
         *
         * @param userAgent User-Agent
         * @return this
         */
        public Builder userAgent(String userAgent) {
            this.userAgent = Objects.requireNonNull(userAgent);
            return this;
        }

        /**
         * {@link EventHandler}를 등록합니다. 여러 개 등록 가능합니다.
         *
         * @param handler 이벤트 핸들러
         * @return this
         */
        public Builder handler(EventHandler handler) {
            this.handlers.add(Objects.requireNonNull(handler));
            return this;
        }

        /**
         * 일반 채팅 메시지 콜백을 등록합니다.
         *
         * @param callback 콜백
         * @return this
         */
        public Builder onChat(Consumer<ChatMessage> callback) {
            this.onChat = callback;
            return this;
        }

        /**
         * Super Chat/Sticker 콜백을 등록합니다.
         *
         * @param callback 콜백
         * @return this
         */
        public Builder onSuperChat(Consumer<ChatMessage> callback) {
            this.onSuperChat = callback;
            return this;
        }

        /**
         * 멤버십 이벤트 콜백을 등록합니다.
         *
         * @param callback 콜백
         * @return this
         */
        public Builder onMembership(Consumer<ChatMessage> callback) {
            this.onMembership = callback;
            return this;
        }

        /**
         * 메시지 삭제 이벤트 콜백을 등록합니다.
         *
         * @param callback 콜백
         * @return this
         */
        public Builder onDelete(Consumer<DeleteEvent> callback) {
            this.onDelete = callback;
            return this;
        }

        /**
         * 클라이언트를 빌드합니다.
         *
         * @return YT4J 인스턴스
         */
        public YT4J build() {
            return new YT4J(this);
        }
    }
}
