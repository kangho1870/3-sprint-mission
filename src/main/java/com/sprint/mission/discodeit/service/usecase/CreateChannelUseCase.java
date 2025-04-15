package com.sprint.mission.discodeit.service.usecase;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.UserService;

public class CreateChannelUseCase {
    private UserService userService;
    private ChannelService channelService;

    public CreateChannelUseCase(UserService userService, ChannelService channelService) {
        this.userService = userService;
        this.channelService = channelService;
    }

    public Channel createChannel(String name, String description, User user) {
        Channel ch = new Channel(user, name, description);
        ch.addMember(user);
        return channelService.createChannel(ch, user);
    }
}
