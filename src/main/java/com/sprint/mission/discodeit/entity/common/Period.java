package com.sprint.mission.discodeit.entity.common;

import java.util.UUID;

public abstract class Period {
    private final UUID id;
    private final Long createdAt;
    private Long updatedAt;

    public Period() {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void update() {
        this.updatedAt = System.currentTimeMillis();
    }
}