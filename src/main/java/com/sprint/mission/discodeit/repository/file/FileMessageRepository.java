package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.dto.message.MessageCreateRequestDto;
import com.sprint.mission.discodeit.entity.dto.message.MessageDeleteRequestDto;
import com.sprint.mission.discodeit.entity.dto.message.MessageUpdateRequestDto;
import com.sprint.mission.discodeit.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.*;

@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileMessageRepository implements MessageRepository {
    private final String FILE_PATH;

    public FileMessageRepository(@Value("${discodeit.repository.file-directory}") String filePath) {
        FILE_PATH = filePath + "/message.ser";
    }

    private Map<UUID, List<Message>> loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return new HashMap<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<UUID, List<Message>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private void saveToFile(Map<UUID, List<Message>> messages) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message createMessage(MessageCreateRequestDto messageCreateRequestDto) {
        Map<UUID, List<Message>> messages = loadFromFile();

        List<Message> channelMessages = messages.get(messageCreateRequestDto.getChannelId());
        if (channelMessages == null) {
            channelMessages = new ArrayList<>();
        }

        Message message = new Message(messageCreateRequestDto.getUserId(), messageCreateRequestDto.getMessageContent());
        channelMessages.add(message);
        messages.put(messageCreateRequestDto.getChannelId(), channelMessages);
        saveToFile(messages);
        return message;
    }

    @Override
    public List<Message> getChannelMessages(UUID channelId) {
        Map<UUID, List<Message>> messages = loadFromFile();
        return messages.get(channelId) == null ? List.of() : messages.get(channelId);
    }

    @Override
    public boolean updateMessage(MessageUpdateRequestDto messageUpdateRequestDto) {
        Map<UUID, List<Message>> messages = loadFromFile();
        if (messages.containsKey(messageUpdateRequestDto.getChannelId())) {
            List<Message> channelMessages = messages.get(messageUpdateRequestDto.getChannelId());
            for (Message message : channelMessages) {
                System.out.println(message.getId());
                if (message.getId().equals(messageUpdateRequestDto.getMessageId())) {
                    if (message.getSender().equals(messageUpdateRequestDto.getUserId())) {
                        message.setContent(messageUpdateRequestDto.getMessageContent());
                        saveToFile(messages);
                        return true;
                    }
                }
            }
        } else {
            System.out.println("존재하지 않는 채널입니다.");
        }
        return false;
    }

    @Override
    public void deleteMessage(MessageDeleteRequestDto messageDeleteRequestDto) {
        Map<UUID, List<Message>> messages = loadFromFile();
        List<Message> messageList = messages.get(messageDeleteRequestDto.getChannelId());
        System.out.println("Repository Message Id " + messageDeleteRequestDto.getMessageId());
        System.out.println();
        for (Message message : messageList) {
            if (message.getId().equals(messageDeleteRequestDto.getMessageId())) {
                if (message.getSender().equals(messageDeleteRequestDto.getUserId())) {
                    messageList.remove(message);
                    System.out.println("메세지가 삭제 되었습니다.");
                    saveToFile(messages);
                    return;
                }
            }
        }
    }
}
