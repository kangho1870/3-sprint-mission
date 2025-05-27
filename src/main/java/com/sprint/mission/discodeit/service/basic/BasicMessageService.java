package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BasicMessageService implements MessageService {

  private final MessageRepository messageRepository;
  private final ChannelRepository channelRepository;
  private final UserRepository userRepository;
  private final BinaryContentRepository binaryContentRepository;
  private final MessageMapper messageMapper;

  @Transactional
  @Override
  public MessageDto create(MessageCreateRequest messageCreateRequest,
                           List<BinaryContentCreateRequest> binaryContentCreateRequests) {
    Channel channel = channelRepository.findById(messageCreateRequest.channelId())
            .orElseThrow(() -> new NoSuchElementException("Channel with id " + messageCreateRequest.channelId() + " does not exist"));

    User author = userRepository.findById(messageCreateRequest.authorId())
            .orElseThrow(() -> new NoSuchElementException("Author with id " + messageCreateRequest.authorId() + " does not exist"));

    Message message = new Message(
            messageCreateRequest.content(),
            channel,
            author
    );

    for (BinaryContentCreateRequest req : binaryContentCreateRequests) {
      BinaryContent binaryContent = new BinaryContent(
              req.fileName(),
              (long) req.bytes().length,
              req.contentType(),
              req.bytes()
      );

      message.addAttachment(binaryContent);
    }

    return messageMapper.toDto(messageRepository.save(message));
  }

  @Override
  public MessageDto find(UUID messageId) {
    return messageMapper.toDto(messageRepository.findById(messageId)
            .orElseThrow(
                    () -> new NoSuchElementException("Message with id " + messageId + " not found"))
    );
  }

  @Override
  public List<MessageDto> findAllByChannelId(UUID channelId) {
    return messageRepository.findAllByChannelId(channelId).stream().map(messageMapper::toDto).toList();
  }

  @Transactional
  @Override
  public MessageDto update(UUID messageId, MessageUpdateRequest request) {
    String newContent = request.newContent();
    Message message = messageRepository.findById(messageId)
        .orElseThrow(
            () -> new NoSuchElementException("Message with id " + messageId + " not found"));
    message.update(newContent);
    return messageMapper.toDto(messageRepository.save(message));
  }

  @Transactional
  @Override
  public void delete(UUID messageId) {
    Message message = messageRepository.findById(messageId)
        .orElseThrow(
            () -> new NoSuchElementException("Message with id " + messageId + " not found"));

    message.getAttachments()
            .forEach(binaryContent ->
                    binaryContentRepository.deleteById(binaryContent.getId())
            );

    messageRepository.deleteById(messageId);
  }
}
