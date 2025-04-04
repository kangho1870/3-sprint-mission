package com.sprint.mission.discodeit.entity;

import java.util.*;

public class Channel {
    private UUID id;
    private User channelAdmin;
    private String name;
    private String description;
    private Set<User> members = new HashSet<User>();
    private List<Message> messages = new ArrayList<>();

    private Long createdAt;
    private Long updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
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
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Channel(User user, String name, String description) {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.channelAdmin = user;
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id=" + id +
                ", channelAdmin=" + channelAdmin +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", members=" + members +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
