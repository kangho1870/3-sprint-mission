package com.sprint.mission.discodeit.repository.file;

import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFileRepository<K, V> {


    private final String filePath;

    protected AbstractFileRepository(@Value("${discodeit.repository.file-directory}")String basePath, String path) {
        this.filePath = basePath + path;
    }

    public Map<K, V> loadFromFile() {
        File file = new File(filePath);
        if (!file.exists()) return new HashMap<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<K, V>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void saveToFile(Map<K, V> data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(data);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패 : ", e);
        }
    }
}

