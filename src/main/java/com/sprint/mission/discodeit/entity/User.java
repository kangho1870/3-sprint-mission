package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.common.Period;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class User extends Period {
    private String userName;
    private String password;
    private Set<Channel> channels = new HashSet<Channel>();

    public UUID getId() {
        return super.getId();
    }


    public Long getCreatedAt() {
        return super.getCreatedAt();
    }


    public Long getUpdatedAt() {
        return super.getUpdatedAt();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
        update();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        update();
    }

    public Set<Channel> getChannels() {
        return channels;
    }

    public User(String userName, String password) {
        super();
        this.userName = userName;
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + super.getId() +
                ", userName='" + userName + '\'' +
                ", createdAt=" + super.getCreatedAt() +
                ", updatedAt=" + super.getUpdatedAt() +
                '}';
    }
}
