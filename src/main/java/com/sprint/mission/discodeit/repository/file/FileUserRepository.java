package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.dto.user.UserCreateDto;
import com.sprint.mission.discodeit.entity.dto.user.UserUpdateRequestDto;
import com.sprint.mission.discodeit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.*;

@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileUserRepository implements UserRepository {

    private final String FILE_PATH;

    public FileUserRepository(@Value("${discodeit.repository.file-directory}") String filePath) {
        FILE_PATH = filePath + "/user.ser";
    }

    public Map<UUID, User> loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return new HashMap<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<UUID, User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void saveToFile(Map<UUID, User> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public User createUser(UserCreateDto userCreateDto) {
        Map<UUID, User> users = loadFromFile();
        User user = new User(userCreateDto);
        users.put(user.getId(), user);
        saveToFile(users);
        return user;
    }

    @Override
    public Optional<User> getUser(UUID id) {
        Map<UUID, User> users = loadFromFile();
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> getAllUsers() {
        Map<UUID, User> users = loadFromFile();
        return users.values().stream().toList();
    }

    @Override
    public boolean modifyUser(UserUpdateRequestDto userUpdateRequestDto) {
        Map<UUID, User> users = loadFromFile();

        if (!users.containsKey(userUpdateRequestDto.getUserId())) {
            return false;
        }
        User user = users.get(userUpdateRequestDto.getUserId());

        if (user.getPassword().equals(userUpdateRequestDto.getOldPassword())) {
            user.setPassword(userUpdateRequestDto.getNewPassword());
            saveToFile(users);
            return true;
        }else {
            System.out.println("비밀번호가 일치하지 않습니다.");
            return false;
        }
    }

    @Override
    public boolean deleteUser(UUID id) {
        Map<UUID, User> users = loadFromFile();
        if (!users.containsKey(id)) {
            throw new NoSuchElementException("존재하지 않는 유저 입니다.");
        } else {
            users.remove(users.get(id).getId());
            saveToFile(users);
            return true;
        }
    }
}
