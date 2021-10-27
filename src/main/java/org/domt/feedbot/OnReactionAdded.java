package org.domt.feedbot;

import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OnReactionAdded implements ReactionAddListener {
    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        User user = Main.activeServer.requestMember(event.requestUser().join()).join();
        Message message = event.requestMessage().join();
        Emoji emoji = event.getEmoji();
        if (user == null || user.isYourself()) {
            return;
        }
        System.out.println(user + ": " + event.getEmoji());
        List<Long> roleSelectMessages = new ArrayList<>();
        for (long id : Main.botInfo.roleSelectMessages) {
            roleSelectMessages.add(Long.valueOf(id));
        }
        if (event.getChannel().getId() == Main.botInfo.roleSelectChannelID && roleSelectMessages.contains(event.getMessage().get().getId())) {
            if (ColorRoleSelection.settingUp) {
                return;
            }
            ColorRoleSelection.ColorRoleSelectionChange(user, emoji, true);
            return;
        }
        try {
            if (message.getContent().contains("ZOMBIES")) {
                CarlFightStuff.ZombieReaction(user, emoji, message, true);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
