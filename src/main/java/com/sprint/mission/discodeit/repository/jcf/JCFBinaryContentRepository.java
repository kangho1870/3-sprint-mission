package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.dto.binaryContent.BinaryContentCreateRequestDto;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JCFBinaryContentRepository implements BinaryContentRepository {

    private final Map<UUID, List<BinaryContent>> binaryContents;

    public JCFBinaryContentRepository() {
        this.binaryContents = new HashMap<>();
    }

    @Override
    public BinaryContent createBinaryContent(BinaryContentCreateRequestDto binaryContentCreateRequestDto) {
        BinaryContent content = new BinaryContent(
                binaryContentCreateRequestDto.getOwnerId(),
                binaryContentCreateRequestDto.getContentType(),
                binaryContentCreateRequestDto.getData()
        );
        binaryContents
                .computeIfAbsent(binaryContentCreateRequestDto.getOwnerId(), k -> new ArrayList<>())
                .add(content);
        return content;
    }

    @Override
    public BinaryContent findBinaryContentById(UUID id) {
        return binaryContents.get(id).isEmpty() ? new BinaryContent(null, null, null) : binaryContents.get(id).stream().findFirst().orElse(null);
    }

    @Override
    public List<BinaryContent> findAllBinaryContentById(UUID id) {
        return binaryContents.get(id).isEmpty() ? List.of() : binaryContents.get(id);
    }

    @Override
    public boolean deleteBinaryContentById(UUID id) {
        for (Map.Entry<UUID, List<BinaryContent>> entry : binaryContents.entrySet()) {
            List<BinaryContent> contents = entry.getValue();
            Iterator<BinaryContent> iterator = contents.iterator();

            while (iterator.hasNext()) {
                BinaryContent content = iterator.next();
                if (content.getId().equals(id)) {
                    iterator.remove();

                    if (contents.isEmpty()) {
                        binaryContents.remove(entry.getKey());
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
