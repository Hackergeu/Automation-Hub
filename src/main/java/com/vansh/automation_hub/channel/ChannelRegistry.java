package com.vansh.automation_hub.channel;

import com.vansh.automation_hub.exception.ChannelSendException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Dispatcher: naam dekhke sahi channel deta hai
@Component
public class ChannelRegistry {

    private final Map<String, MessageChannel> channels;

    // Spring yahan MessageChannel ki SAARI implementations ek list mein de deta hai.
    // Kal ko SlackChannel jodo, toh ye file badalni nahi padegi.
    public ChannelRegistry(List<MessageChannel> allChannels) {
        this.channels = allChannels.stream()
                .collect(Collectors.toMap(MessageChannel::name, Function.identity()));
    }

    // Channel do. Naam galat ho toh non-retryable error
    public MessageChannel get(String name) {
        MessageChannel channel = channels.get(name);
        if (channel == null) {
            throw new ChannelSendException("Unknown channel: " + name, false);
        }
        return channel;
    }

    // API validation ke liye: channel exist karta hai AUR configured hai?
    public boolean isAvailable(String name) {
        MessageChannel channel = channels.get(name);
        return channel != null && channel.isAvailable();
    }
}