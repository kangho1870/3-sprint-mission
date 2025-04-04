package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.MessageService;

import java.util.List;

public class JCFMessageService implements MessageService {

    @Override
    public boolean sendMessage(Message message, Channel channel) {
        if (channel.getMembers().contains(message.getSender())) {
            channel.getMessages().add(message);

            channel.getMessages().forEach(m ->
                System.out.println(m.getSender().getUserName() + " : " + m.getContent())
            );
            return true;
        }else {
            System.out.println("참여하지 않은 채널입니다.");
            return false;
        }
    }

    @Override
    public List<Message> getChannelMessages(Channel channel) {
        List<Message> messages = channel.getMessages();
        return messages;
    }

    @Override
    public boolean deleteMessage(Channel channel, Message message, User user) {
        boolean result = false;
        List<Message> messages = channel.getMessages();
        for (Message m : messages) {
            if (m.getSender().getId().equals(user.getId())) {
                channel.getMessages().remove(message);
                result = true;
                System.out.println("(" + m.getContent() + ") 메세지가 삭제되었습니다.");
                break;
            }else {
                System.out.println("본인이 작성한 메세지만 삭제할 수 있습니다.");
            }
        }
        return result;
    }
}
