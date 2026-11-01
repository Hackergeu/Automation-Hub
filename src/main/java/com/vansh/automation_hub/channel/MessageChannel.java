package com.vansh.automation_hub.channel;

// Contract: har channel (Telegram, Discord, Email...) ko ye 2-3 cheezein aani chahiye
public interface MessageChannel {

    // Channel ka naam, jo Job.channel mein store hota hai (jaise "TELEGRAM")
    String name();

    // Ye channel is server par configured hai ya nahi (jaise webhook URL set hai?)
    // default = true, jise zarurat ho wo override kare
    default boolean isAvailable() {
        return true;
    }

    // Message bhejo. target optional hai (jaise email address). Fail hone par ChannelSendException phenko
    void send(String target, String message);
}