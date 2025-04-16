package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.file.FileChannelService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BasicMessageService implements MessageService {

    private final ChannelRepository channelRepository;

    public BasicMessageService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public List<Message> getChannelMessages(Channel channel) {
        return channelRepository.loadFromFile().get(channel.getId()).getMessages();
    }

    @Override
    public boolean deleteMessage(Channel channel, Message message, User user) {
        boolean result = false;

        Map<UUID, Channel> channels = channelRepository.loadFromFile();

        if (channels.containsKey(channel.getId())) {
            Channel ch = channels.get(channel.getId());
            for (Message m : ch.getMessages()) {
                if (m.getId().equals(message.getId())) {
                    ch.getMessages().remove(m);
                    channelRepository.saveToFile(channels);
                    result = true;
                    System.out.println("(" + m.getContent() + ") 메세지가 삭제되었습니다.");
                    break;
                } else {
                    System.out.println("본인이 작성한 메세지만 삭제할 수 있습니다.");
                }
            }
        }

        return result;
    }
}
