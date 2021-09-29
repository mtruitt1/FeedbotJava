package org.domt.feedbot;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.Hashtable;

public class OnMessageReceived implements MessageCreateListener {
    public static Hashtable<Long, String> stickies;

    @Override
    public void onMessageCreate(MessageCreateEvent event) {

    }
}
