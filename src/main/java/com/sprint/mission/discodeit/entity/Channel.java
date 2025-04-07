package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.common.Period;

import java.util.*;

public class Channel extends Period {

    private User channelAdmin;
    private String name;
    private String description;
    private Set<User> members = new HashSet<User>();
    private List<Message> messages = new ArrayList<>();


    public UUID getId() {
        return super.getId();
    }

    public Long getCreatedAt() {
        return super.getCreatedAt();
    }

    public Long getUpdatedAt() {
        return super.getUpdatedAt();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public Set<User> getMembers() {
        return members;
    }

    public User getChannelAdmin() {
        return channelAdmin;
    }

    public void setChannelAdmin(User channelAdmin) {
        this.channelAdmin = channelAdmin;
        update();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        update();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        update();
    }

    public Channel(User user, String name, String description) {
        super();
        this.channelAdmin = user;
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id=" + super.getId() +
                ", channelAdmin=" + channelAdmin +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", members=" + members +
                ", createdAt=" + super.getCreatedAt() +
                ", updatedAt=" + super.getUpdatedAt() +
                '}';
    }
}
