# yt4j

YouTube 라이브 스트리밍의 채팅, 후원(Super Chat), 멤버십 이벤트를 실시간으로 구독하는 Java 라이브러리입니다.

## 요구 사항

- Java 11+

## 의존성

- Gson 2.10.1

## 빠른 시작

### Gradle
```groovy
dependencies {
    implementation 'kr.zzik2:yt4j:0.0.2'
}
```

### Maven
```xml
<dependency>
    <groupId>kr.zzik2</groupId>
    <artifactId>yt4j</artifactId>
    <version>0.0.2</version>
</dependency>
```

### 자동 폴링 (블로킹)

```java
import kr.zzik2.yt4j.YT4J;

YT4J client = YT4J.builder("VIDEO_ID_OR_URL")
    .topChatOnly(true)
    .onChat(msg -> System.out.println(msg.authorName() + ": " + msg.text()))
    .onSuperChat(msg -> System.out.println("후원 " + msg.purchaseAmount() + " from " + msg.authorName()))
    .onMembership(msg -> System.out.println("새 멤버: " + msg.authorName()))
    .onDelete(event -> System.out.println("삭제됨: " + event.targetId()))
    .build();

client.connect(); // 블로킹 연결

// ... 작업 수행

client.disconnect(); // 종료
```

### 비동기 연결

```java
client.connectAsync()
    .thenRun(() -> System.out.println("연결됨!"))
    .exceptionally(e -> { e.printStackTrace(); return null; });

// 비동기 종료
client.disconnectAsync();
```

### 방송 상태 확인

`connect()` 호출 전에 라이브 방송 여부를 확인할 수 있습니다.

```java
YT4J client = YT4J.builder("username")
    .inputType(YT4J.InputType.USER)
    .onChat(msg -> System.out.println(msg.text()))
    .build();

// 동기
BroadcastInfo info = client.broadcastInfo();
if (info.isLiveNow()) {
    client.connect();
}

// 비동기
client.broadcastInfoAsync().thenAccept(i -> {
    if (i.isLiveNow()) client.connectAsync();
});
```

### 수동 폴링

```java
YT4J client = YT4J.builder("VIDEO_ID_OR_URL")
    .onChat(msg -> System.out.println(msg.text()))
    .build();

// 한 번만 가져오기
client.fetch();
```

### EventHandler 인터페이스

```java
YT4J client = YT4J.builder("VIDEO_ID_OR_URL")
    .handler(new EventHandler() {
        @Override
        public void onChat(ChatMessage msg) {
            System.out.println(msg.authorName() + ": " + msg.text());
        }

        @Override
        public void onSuperChat(ChatMessage msg) {
            System.out.println("후원: " + msg.purchaseAmount());
        }
    })
    .build();

client.connect();
```

## 빌더 옵션

| 메서드 | 설명 | 기본값 |
|--------|------|--------|
| `topChatOnly(boolean)` | 인기 채팅만 수신 | `true` |
| `locale(Locale)` | 시스템 메시지 언어, 통화 표시 | `Locale.US` |
| `inputType(InputType)` | `VIDEO`, `CHANNEL`, `USER` | `VIDEO` |
| `userAgent(String)` | User-Agent 문자열 | Chrome UA |
| `handler(EventHandler)` | 이벤트 핸들러 (다수 등록 가능) | — |
| `onChat(Consumer)` | 채팅 콜백 | — |
| `onSuperChat(Consumer)` | 후원 콜백 | — |
| `onMembership(Consumer)` | 멤버십 콜백 | — |
| `onDelete(Consumer)` | 삭제 콜백 | — |

## 채널/사용자 핸들로 연결

```java
// 채널 ID로
YT4J client = YT4J.builder("UCxxxxxx")
    .inputType(YT4J.InputType.CHANNEL)
    .onChat(msg -> { /* ... */ })
    .build();

// 사용자 핸들로
YT4J client = YT4J.builder("username")
    .inputType(YT4J.InputType.USER)
    .onChat(msg -> { /* ... */ })
    .build();
```

** 주의 사항 **
사용자 핸들로 연결할 시 진행 중인 라이브가 여러 개일 경우, 가장 상단에 표시되는 라이브에 연결됩니다.

## ChatMessage 필드

| 메서드 | 설명 |
|--------|------|
| `id()` | 메시지 고유 ID |
| `type()` | `MESSAGE`, `SUPER_CHAT`, `SUPER_STICKER`, `MEMBERSHIP` |
| `authorName()` | 작성자 이름 |
| `authorChannelId()` | 작성자 채널 ID |
| `text()` | 메시지 텍스트 |
| `segments()` | 텍스트/이모지 세그먼트 리스트 |
| `badges()` | 작성자 뱃지 (`OWNER`, `MODERATOR`, `MEMBER`, `VERIFIED`) |
| `purchaseAmount()` | 후원 금액 (예: `₩5,000`) |
| `timestampUsec()` | 타임스탬프 (마이크로초) |


## 빌드/테스트

```bash
./gradlew build
./gradlew test
```

## 라이선스

MIT License
