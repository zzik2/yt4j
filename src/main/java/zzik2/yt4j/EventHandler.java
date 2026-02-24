package zzik2.yt4j;

import zzik2.yt4j.model.ChatMessage;
import zzik2.yt4j.model.DeleteEvent;

/**
 * YouTube 라이브 채팅 이벤트를 수신하는 핸들러 인터페이스.
 *
 * <p>
 * 필요한 메서드만 오버라이드하여 사용합니다.
 * </p>
 *
 * <pre>{@code
 * YT4J client = YT4J.builder("VIDEO_ID")
 *         .onChat(msg -> System.out.println(msg.text()))
 *         .build();
 * }</pre>
 */
public interface EventHandler {

    /**
     * 일반 채팅 메시지 수신 시 호출됩니다.
     *
     * @param message 채팅 메시지
     */
    default void onChat(ChatMessage message) {}

    /**
     * Super Chat 또는 Super Sticker 수신 시 호출됩니다.
     *
     * @param message 후원 메시지 (purchaseAmount 포함)
     */
    default void onSuperChat(ChatMessage message) {}

    /**
     * 새 멤버십 가입 이벤트 수신 시 호출됩니다.
     *
     * @param message 멤버십 메시지
     */
    default void onMembership(ChatMessage message) {}

    /**
     * 메시지 삭제 이벤트 수신 시 호출됩니다.
     *
     * @param event 삭제 이벤트
     */
    default void onDelete(DeleteEvent event) {}
}
