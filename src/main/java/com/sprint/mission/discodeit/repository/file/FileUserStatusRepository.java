package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.entity.dto.userStatus.UserStatusCreateRequestDto;
import com.sprint.mission.discodeit.entity.dto.userStatus.UserStatusUpdateRequestDto;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.Instant;
import java.util.*;

@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileUserStatusRepository implements UserStatusRepository {
    private final String FILE_PATH;

    public FileUserStatusRepository(@Value("${discodeit.repository.file-directory}") String filePath) {
        FILE_PATH = filePath + "/userStatus.ser";
    }

    public Map<UUID, UserStatus> loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return new HashMap<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<UUID, UserStatus>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void saveToFile(Map<UUID, UserStatus> userStatus) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(userStatus);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public UserStatus createUserStatus(UserStatusCreateRequestDto userStatusCreateRequestDto) {
        Map<UUID, UserStatus> userStatuses = loadFromFile();
        UserStatus status = new UserStatus(userStatusCreateRequestDto.getUserId(), userStatusCreateRequestDto.getNowTime());

        if (userStatuses.containsKey(status.getId())) {
            throw new IllegalArgumentException("데이터가 이미 존재합니다.");
        }
        userStatuses.put(status.getId(), status);
        saveToFile(userStatuses);
        return status;
    }

    @Override
    public UserStatus findStatusById(UUID statusId) {
        Map<UUID, UserStatus> userStatuses = loadFromFile();
        if (!userStatuses.containsKey(statusId)) {
            throw new NoSuchElementException("존재하지 않는 데이터입니다.");
        }
        return userStatuses.get(statusId);
    }

    @Override
    public List<UserStatus> findAllStatus() {
        return loadFromFile().values().stream().toList();
    }

    @Override
    public boolean updateUserStatus(UserStatusUpdateRequestDto userStatusUpdateRequestDto) {
        Map<UUID, UserStatus> userStatuses = loadFromFile();
        UserStatus userStatus = userStatuses.get(userStatusUpdateRequestDto.getUserStatusId());

        if (userStatus == null) {
            throw new NoSuchElementException("존재하지 않는 데이터입니다.");
        }

        userStatus.setLastActivityAt(userStatusUpdateRequestDto.getNowTime());
        saveToFile(userStatuses);
        return true;
    }

    @Override
    public UserStatus updateByUserId(UUID userId) {
        Map<UUID, UserStatus> userStatuses = loadFromFile();
        for (UserStatus userStatus : userStatuses.values()) {
            if (userStatus.getUserId().equals(userId)) {
                userStatus.setLastActivityAt(Instant.now());
                saveToFile(userStatuses);
                return userStatus;
            }
        }
        throw new NoSuchElementException("존재하지 않는 데이터입니다.");
    }

    @Override
    public boolean deleteUserStatus(UUID statusId) {
        Map<UUID, UserStatus> userStatuses = loadFromFile();
        if (userStatuses.containsKey(statusId)) {
            userStatuses.remove(statusId);
            saveToFile(userStatuses);
            return true;
        }
        return false;
    }
}
