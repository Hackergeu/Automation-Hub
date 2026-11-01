package com.vansh.automation_hub.controller;

import com.vansh.automation_hub.channel.ChannelRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelRegistry channelRegistry;

    public ChannelController(ChannelRegistry channelRegistry) {
        this.channelRegistry = channelRegistry;
    }

    // GET /api/channels -> ["DISCORD", "TELEGRAM"]  (login zaroori, /api/** ke andar hai)
    @GetMapping
    public List<String> available() {
        return channelRegistry.availableChannelNames();
    }
}