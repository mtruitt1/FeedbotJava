package org.domt.feedbot;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class SessionInfo {
    public long activeServerID;
    public long staffRoleID;
    public long sandboxID;
    public long suggestionLogID;
    public long lfgChannelID;
    public long[] purgeImmunity;
    public List<Message> immuneMessages;
    public long promptMasterRoleID;
    public long promptMasterChannelID;
    public long promptChannelID;
    public long roleSelectChannelID;
    public long[] roleSelectMessages;
    public StickyMessage[] stickyMessages;
    public String[] prompt_roles = {
            "Achiever",
            "Anthropologist",
            "Arcanist",
            "Architect",
            "Artificer",
            "Chronicler",
            "Connoisseur",
            "Gourmet",
            "Pride",
            "Skald",
            "Survivor",
            "Tinker",
            "Virtuoso",
            "Unhallowed",
            "Zoologist"
    };

    public Hashtable<ServerTextChannel, OnMessageReceived.StickyMessage> HashStickies() {
        Hashtable<ServerTextChannel, OnMessageReceived.StickyMessage> stickies = new Hashtable<>();
        for (StickyMessage sticky : stickyMessages) {
            ServerTextChannel channel = (ServerTextChannel)Main.api.getChannelById(sticky.channelID).orElse(null);
            if (channel == null || stickies.containsKey(channel)) {
                continue;
            }
            MessageSet messages = channel.getMessages(50).join();
            List<Message> myMessages = new ArrayList<>();
            for (Message m : messages.descendingSet()) {
                if (m.getAuthor().isYourself()) {
                    myMessages.add(m);
                }
            }
            myMessages.forEach(message -> {message.delete();});
            stickies.put(channel, new OnMessageReceived.StickyMessage(sticky.message, channel.sendMessage(sticky.message).join()));
        }
        return stickies;
    }

    public class StickyMessage {
        public long channelID;
        public String message;
    }
}
