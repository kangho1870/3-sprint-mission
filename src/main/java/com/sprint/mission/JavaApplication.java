package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelService;
import com.sprint.mission.discodeit.service.jcf.JCFMessageService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JavaApplication {
    public static void main(String[] args) {

        ChannelService channelService = new JCFChannelService();
        MessageService messageService = new JCFMessageService();
        UserService userService = new JCFUserService(channelService);

        User user = new User("kangho", "1234");
        User user2 = new User("test", "1234");

        // 유저 등록
        userService.createUser(user);
        userService.createUser(user2);

        // 유저 단건 조회
        log("유저 단건 조회", () -> userService.getUser(user.getId()));

        // 전체 유저 조회
        userService.getAllUsers();
        logAll("전체 유저 조회", userService.getAllUsers(), System.out::println);

        // 유저 비밀번호 수정
        userService.modifyPassword(user.getId(), "1234", "0000");

        // 수정된 데이터 조회
        log("유저 데이터 수정 후", () -> userService.getUser(user.getId()));

        // 유저 삭제
//        userService.deleteUser(user.getId());
//        userService.getAllUsers();

        // 새로운 채널 생성
        System.out.println("채널 생성");
        Channel channel = channelService.createChannel("codeit", "코드잇 커뮤니티", user);

        // 유저의 채팅방 참여
        userService.joinChannel(channel, user2);

        // 전체 채널 조회
        logAll("채널 목록", channelService.getAllChannels(), System.out::println);

        // 채널 조회
        log("채널 단건 조회", () -> channelService.getChannel(channel.getId()));

        // 채널명 수정
        System.out.println("--------채널명 수정-------------");
        // 채널의 주인이 아닌경우
        log("채널명 수정", () -> channelService.modifyChannelName(channel.getId(), user2, "수정한 채널명"));


        // 채널의 주인인 경우
        log("채널명 수정", () -> channelService.modifyChannelName(channel.getId(), user, "수정한 채널명"));
        System.out.println("---------------------------------");

        // 채널 설명 수정
        System.out.println("----------채널 설명 수정---------");
        // 채널의 주인이 아닌경우
        log("채널 설명 수정", () -> channelService.modifyChannelDescription(channel.getId(), user2, "수정한 채널 설명"));


        // 채널의 주인인 경우
        log("채널 설명 수정", () -> channelService.modifyChannelDescription(channel.getId(), user, "수정한 채널 설명"));
        System.out.println("---------------------------------");

        System.out.println("----채널 수정 이후 채널 목록-----");
        logAll("채널 목록", channelService.getAllChannels(), System.out::println);
        System.out.println("---------------------------------");

        System.out.println("-----------채널 유저-------------");
        logAll("채널 유저", channel.getMembers(), e ->
                        System.out.println(e.getUserName())
                );
        System.out.println("---------------------------------");


        // 채널 강퇴
        // 채널의 주인이 아닐경우
        log("유저 강퇴", () -> channelService.kickOutChannel(channel.getId(), user, user2));

        // 채널의 주인일 경우
//        log("유저 강퇴", () -> channelService.kickOutChannel(channel.getId(), user2, user));

        System.out.println("--------강퇴 이후 채널 유저------");
        logAll("채널 유저", channelService.getChannel(channel.getId()).getMembers(), System.out::println);
        System.out.println("---------------------------------");


        // 메세지 보내기
        Message message = new Message(user, "안녕하세요");
        Message message2 = new Message(user2, "hi~");

        System.out.println("---------메세지 전송-------------");
        log("메세지 전송", () -> userService.sendMessage(message, channel), message.getContent());
        log("메세지 전송", () -> userService.sendMessage(message2, channel), message2.getContent());
        System.out.println("---------------------------------");

        // 채널의 메세지 확인
        System.out.println("--------채널의 메세지 확인-------");
        logAll("채널의 메세지 확인", messageService.getChannelMessages(channel), e ->
                        System.out.println(e.getSender().getUserName() + " : " + e.getContent())
                );
        System.out.println("---------------------------------");

        // 채널의 메세지 삭제
        System.out.println("--------메세지 삭제--------------");
        // 본인 외 메세지 삭제일 경우
//        log("메세지 삭제", () -> messageService.deleteMessage(channel, message2, user));

        // 본인이 작성한 메세지일 경우
        log("메세지 삭제", () -> messageService.deleteMessage(channel, message, user));
        System.out.println("---------------------------------");


        System.out.println("--------삭제 후 확인-------------");
        logAll("메세지 삭제 후", messageService.getChannelMessages(channel), e ->
                        System.out.println(e.getContent())
                );
        System.out.println("---------------------------------");

        // 채널 삭제
        log("채널 삭제", () -> channelService.deleteChannel(channel.getId(), user));

        log("채널 삭제 후", () -> channelService.getChannel(channel.getId()));

        logAll("채널", channelService.getAllChannels(), System.out::println);
        // 유저 삭제
        log("유저 삭제", () -> userService.deleteUser(user.getId()));

        log("유저 조회", () -> userService.getUser(user.getId()));
        logAll("채널", channelService.getAllChannels(), System.out::println);


    }

    private static <T> void log(String action, Supplier<T> supplier) {
        System.out.println(action + " : " + supplier.get());
    }
    private static <T> void log(String action, BooleanSupplier supplier) {
        System.out.println(action + " : " + (supplier.getAsBoolean() ? (action + " 성공") :(action + " 실패")));
    }
    private static <T> void log(String action, BooleanSupplier supplier, String content) {
        System.out.println(action + " : " + (supplier.getAsBoolean() ? (action + " 성공 : " + content) :(action + " 실패 ")));
    }

    private static <T> void logAll(String action, Collection<T> list, Consumer<T> consumer) {
        System.out.println(action + " : ");
        list.forEach(consumer);
    }
}
