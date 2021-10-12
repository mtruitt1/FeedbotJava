package org.domt.feedbot;

import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OnReactionRemoved implements ReactionRemoveListener {
    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
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
            ColorRoleSelection.ColorRoleSelectionChange(user, emoji, false);
            return;
        }
        if (message.getContent().contains("ZOMBIES")) {
            CarlFightStuff.ZombieReaction(user, emoji, message, false);
        }
    }
}
