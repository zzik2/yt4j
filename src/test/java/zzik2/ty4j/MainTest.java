package zzik2.ty4j;

import zzik2.yt4j.YT4J;

import java.io.IOException;
import java.util.Locale;

public class MainTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        YT4J client = YT4J.builder("MrRagoona88")
                .inputType(YT4J.InputType.USER)
                .topChatOnly(true)
                .locale(Locale.KOREAN)
                .onChat(msg -> System.out.println(msg.authorName() + ": " + msg.text()))
                .onSuperChat(msg -> System.out.println("후원 " + msg.purchaseAmount() + " from " + msg.authorName()))
                .onMembership(msg -> System.out.println("새 멤버: " + msg.authorName()))
                .onDelete(event -> System.out.println("삭제됨: " + event.targetId()))
                .build();

        boolean online = client.broadcastInfo().isLiveNow();

        if (online) {
            System.out.println("온라인 입니다!");
            client.connect();
        } else {
            System.out.println("온라인이 아닙니다!");
        }
        Thread.sleep(Integer.MAX_VALUE);
    }
}
