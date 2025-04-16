package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;

import java.util.List;

public class JCFMessageService implements MessageService {

    private final UserService userService;
    private final ChannelService channelService;


    public JCFMessageService(UserService userService, ChannelService channelService) {
        this.userService = userService;
        this.channelService = channelService;
    }

    @Override
    public List<Message> getChannelMessages(Channel channel) {
        return channelService.getChannel(channel.getId()).getMessages();
    }

    @Override
    public boolean deleteMessage(Channel channel, Message message, User user) {
        boolean result = false;
        List<Message> messages = channelService.getChannel(channel.getId()).getMessages();
        User findUser = userService.getUser(user.getId());
        for (Message m : messages) {
            if(m.getId().equals(message.getId())) {
                if (findUser.getId().equals(message.getSender().getId())) {
                    messages.remove(m);
                    result = true;
                    System.out.println("(" + m.getContent() + ") 메세지가 삭제되었습니다.");
                    break;
                }else {
                    System.out.println("본인이 작성한 메세지만 삭제할 수 있습니다.");
                }
            }
        }
        return result;
    }

}
