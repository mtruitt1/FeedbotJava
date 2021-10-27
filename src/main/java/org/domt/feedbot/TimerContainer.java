package org.domt.feedbot;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TimerContainer {
    public static ServerTextChannel lfg;
    public static void StartLFGPurger(int checkFreqSecs, long maxAgeSecs) {
        Timer lfgPurge = new Timer();
        lfgPurge.schedule(new TimerTask() {
            @Override
            public void run() {
                RunLFGPurge(maxAgeSecs);
            }
        }, 0, checkFreqSecs*1000);

        Timer brewThreads = new Timer();
        brewThreads.schedule(new TimerTask() {
            @Override
            public void run() {
                CheckHomebrewThreads();
            }
        }, 0, 60*60*1000);
    }

    public static void RunLFGPurge(long maxAgeSecs) {
        System.out.println("Starting LFG purge");
        if (lfg == null) {
            lfg = Main.api.getChannelById(Main.botInfo.lfgChannelID).orElse(null).asServerTextChannel().orElse(null);
        }
        if (lfg == null) {
            return;
        }
        if (Main.botInfo.immuneMessages == null) {
            Main.botInfo.immuneMessages = new ArrayList<>();
            for (long id : Main.botInfo.purgeImmunity) {
                Main.botInfo.immuneMessages.add(Main.api.getMessageById(id, lfg).join());
            }
        }
        Instant maxAge = Instant.now().minusSeconds(maxAgeSecs);
        for (Message message : lfg.getMessages(500).join()) {
            if (!Main.botInfo.immuneMessages.contains(message)) {
                System.out.println("Checking age of message ID " + message.getId());
                Instant messageCreation = message.getCreationTimestamp();
                if (messageCreation.isBefore(maxAge)) {
                    User sendContent = message.getAuthor().asUser().orElse(null);
                    if (sendContent != null) {
                        try {
                            sendContent.sendMessage("Hello! Your post in the Discord of Many Things " + lfg.getMentionTag() + " channel has been deleted since it is older than the maximum lfg post age. Your message content is copied below in the event you wish to post again:");
                            sendContent.sendMessage(message.getContent());
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                    message.delete();
                }
            }
        }
    }

    public static void CheckHomebrewThreads() {

    }
}
