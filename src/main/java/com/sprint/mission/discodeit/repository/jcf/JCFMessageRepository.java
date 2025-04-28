package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.dto.message.MessageCreateRequestDto;
import com.sprint.mission.discodeit.entity.dto.message.MessageDeleteRequestDto;
import com.sprint.mission.discodeit.entity.dto.message.MessageUpdateRequestDto;
import com.sprint.mission.discodeit.repository.MessageRepository;

import java.util.*;

public class JCFMessageRepository implements MessageRepository {

    private final Map<UUID, List<Message>> messages;

    public JCFMessageRepository() {
        this.messages = new HashMap<>();
    }

    public Map<UUID, List<Message>> getMessages() {
        return messages;
    }

    @Override
    public Message createMessage(MessageCreateRequestDto messageCreateRequestDto) {
        if (messages.containsKey(messageCreateRequestDto.getChannelId())) {
            List<Message> channelMessages = messages.get(messageCreateRequestDto.getChannelId());
            Message message = new Message(messageCreateRequestDto.getUserId(), messageCreateRequestDto.getMessageContent());
            messages.put(messageCreateRequestDto.getChannelId(), channelMessages);
            return message;
        } else {
            throw new NoSuchElementException("존재하지 않는 채널입니다.");
        }
    }

    @Override
    public List<Message> getChannelMessages(UUID channelId) {
        return messages.get(channelId) == null ? List.of() : messages.get(channelId);
    }

    @Override
    public boolean updateMessage(MessageUpdateRequestDto messageUpdateRequestDto) {
        List<Message> messages = this.messages.get(messageUpdateRequestDto.getChannelId());
        if (messages == null) {
            return false;
        }

        for (Message message : messages) {
            if (message.getId().equals(messageUpdateRequestDto.getMessageId())) {
                if (message.getSender().equals(messageUpdateRequestDto.getUserId())) {
                    message.setContent(messageUpdateRequestDto.getMessageContent());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void deleteMessage(MessageDeleteRequestDto messageDeleteRequestDto) {
        List<Message> messages = this.messages.get(messageDeleteRequestDto.getChannelId());

        for (Message message : messages) {
            if (message.getId().equals(messageDeleteRequestDto.getMessageId())) {
                if (message.getSender().equals(messageDeleteRequestDto.getUserId())) {
                    messages.remove(message);
                    System.out.println("메세지가 삭제 되었습니다.");
                    return;
                }
            }
        }
    }
}
