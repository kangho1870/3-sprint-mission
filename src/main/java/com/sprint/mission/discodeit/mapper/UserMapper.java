package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.registry.JwtRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {BinaryContentMapper.class})
public abstract class UserMapper {

    @Autowired
    protected BinaryContentMapper binaryContentMapper;

    @Autowired
    private JwtRegistry jwtRegistry;

    @Mapping(target = "online", expression = "java(isOnline(user))")
    public abstract UserDto toDto(User user);

    protected boolean isOnline(User user) {
        return jwtRegistry.hasActiveJwtInformationByUserId(user.getId());
    }
}
