package org.domt.feedbot;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.Hashtable;

public class OnMessageReceived implements MessageCreateListener {
    public static Hashtable<ServerTextChannel, StickyMessage> stickies;

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        ServerTextChannel channel = event.getChannel().asServerTextChannel().orElse(null);
        if (channel == null) {
            return;
        }
        if (CarlFightStuff.fightActive && channel == CarlFightStuff.fightChannel && !event.getMessage().getContent().contains("CARL HEALTH")) {
            CarlFightStuff.updateNeeded = true;
            return;
        }
        if (event.getMessageAuthor().isYourself()) {
            return;
        }
        if (stickies.containsKey(channel)) {
            stickies.get(channel).message.delete();
            stickies.get(channel).message = channel.sendMessage(stickies.get(channel).content).join();
        }
    }

    public static class StickyMessage {
        public String content;
        public Message message;

        public StickyMessage(String c, Message m) {
            content = c;
            message = m;
        }
    }
}
