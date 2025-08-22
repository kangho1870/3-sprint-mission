package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.message.MessageNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BasicMessageService implements MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final MessageMapper messageMapper;
    private final PageResponseMapper pageResponseMapper;
    private final BinaryContentStorage binaryContentStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public MessageDto create(MessageCreateRequest messageCreateRequest,
                             List<BinaryContentCreateRequest> binaryContentCreateRequests) {
        Channel channel = channelRepository.findById(messageCreateRequest.channelId()).orElseThrow(() -> new ChannelNotFoundException(messageCreateRequest.channelId()));

        User author = userRepository.findById(messageCreateRequest.authorId()).orElseThrow(() -> new UserNotFoundException(messageCreateRequest.authorId()));

        Message message = new Message(
                messageCreateRequest.content(),
                channel,
                author
        );

        for (BinaryContentCreateRequest req : binaryContentCreateRequests) {
            BinaryContent binaryContent = new BinaryContent(
                    req.fileName(),
                    (long) req.bytes().length,
                    req.contentType()
            );

            message.addAttachment(binaryContent);
            BinaryContent savedBinaryContent = binaryContentRepository.save(binaryContent);

            BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(savedBinaryContent.getId(), req.bytes());
            eventPublisher.publishEvent(event);
        }

        MessageCreatedEvent messageCreatedEvent = new MessageCreatedEvent(message.getId(), message.getChannel().getId(), message.getAuthor().getId(), message.getContent());
        eventPublisher.publishEvent(messageCreatedEvent);
        return messageMapper.toDto(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    @Override
    public MessageDto find(UUID messageId) {
        return messageMapper.toDto(messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId))
        );
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<?> findAllByChannelId(UUID channelId, Instant cursor, Pageable pageable) {
        channelRepository.findById(channelId).orElseThrow(() -> new ChannelNotFoundException(channelId));

        pageable = PageRequest.of(0, pageable.getPageSize(), Sort.by("createdAt").descending());

        if (cursor != null) {
            return pageResponseMapper.fromSlice(messageRepository.findByChannelIdAndCreatedAtLessThanOrderByCreatedAtDesc(channelId, cursor, pageable)
                    .map(messageMapper::toDto));
        } else {
            return pageResponseMapper.fromSlice(messageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
                    .map(messageMapper::toDto));
        }
    }

    @Transactional
    @Override
    public MessageDto update(UUID messageId, MessageUpdateRequest request) {
        String newContent = request.newContent();
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new MessageNotFoundException(messageId));
        message.update(newContent);
        return messageMapper.toDto(messageRepository.save(message));
    }

    @Transactional
    @Override
    public void delete(UUID messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new MessageNotFoundException(messageId));

        message.getAttachments()
                .forEach(binaryContent ->
                        binaryContentRepository.deleteById(binaryContent.getId())
                );

        messageRepository.deleteById(messageId);
    }
}
