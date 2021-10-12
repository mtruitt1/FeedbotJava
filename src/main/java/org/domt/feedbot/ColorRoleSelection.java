package org.domt.feedbot;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ColorRoleSelection {
    public static HashMap<Emoji, Role> colorRoleEmojiTable = null;
    public static boolean settingUp = false;

    public static void SetColorRoleEmojiTable() {
        settingUp = true;
        colorRoleEmojiTable = new HashMap<>();
        TextChannel roleSelect = (TextChannel)Main.activeServer.getChannelById(Main.botInfo.roleSelectChannelID).get();
        Message[] roleSelectMessages = new Message[Main.botInfo.roleSelectMessages.length];
        for (int i = 0; i < roleSelectMessages.length; i++) {
            try {
                roleSelectMessages[i] = roleSelect.getMessageById(Main.botInfo.roleSelectMessages[i]).get();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        for (Message message : roleSelectMessages) {
            message.removeAllReactions();
            String[] lines = message.getContent().split("\\R");
            for (String line : lines) {
                if (line.length() == 0) {
                    continue;
                }
                Emoji emoji = null;
                if (line.startsWith("<")) {
                    long emojiID = Long.parseLong(line.split(">:")[0].split(":")[2]);
                    emoji = Main.api.getCustomEmojiById(emojiID).get();
                    message.addReaction(emoji);
                } else {
                    String unicode = line.split(":")[0];
                    message.addReaction(unicode);
                    for (Reaction reaction : message.getReactions()) {
                        Emoji reactionEmoji = reaction.getEmoji();
                        if (reactionEmoji.equalsEmoji(unicode)) {
                            emoji = reactionEmoji;
                            break;
                        }
                    }
                }
                long roleID = Long.parseLong(line.split("&")[1].split(">")[0]);
                Role role = Main.activeServer.getRoleById(roleID).get();
                colorRoleEmojiTable.put(emoji, role);
                //System.out.println("Added pair " + emoji + "/" + role + " to the table!");
            }
        }
        settingUp = false;
    }

    public static void ColorRoleSelectionChange(User us, Emoji emoji, boolean added) {
        User user = Main.activeServer.requestMember(us).join();
        if (colorRoleEmojiTable == null) {
            SetColorRoleEmojiTable();
        }
        if (added) {
            Reaction removeable = null;
            List<Reaction> reactions = new ArrayList<>();
            for (long id : Main.botInfo.roleSelectMessages) {
                Message message = Main.activeServer.getTextChannelById(Main.botInfo.roleSelectChannelID).get().getMessageById(id).join();
                if (message != null) {
                    reactions.addAll(message.getReactions());
                }
            }
            for (Reaction reaction : reactions) {
                if (!reaction.getEmoji().equalsEmoji(emoji)) {
                    //System.out.println(reaction.getEmoji());
                    Role toRemove = Main.activeServer.getRolesByName("color" + colorRoleEmojiTable.get(reaction.getEmoji()).getName()).get(0);
                    user.removeRole(toRemove);
                    reaction.removeUser(user);
                } else {
                    removeable = reaction;
                }
            }
            if (colorRoleEmojiTable.get(emoji).hasUser(user)) {
                Role toAdd = Main.activeServer.getRolesByName("color" + colorRoleEmojiTable.get(emoji).getName()).get(0);
                //System.out.println("Adding " + toAdd);
                user.addRole(toAdd);
            } else {
                removeable.removeUser(user);
            }
        } else {
            Role toRemove = Main.activeServer.getRolesByName("color" + colorRoleEmojiTable.get(emoji).getName()).get(0);
            //System.out.println("Removing " + toRemove);
            user.removeRole(toRemove);
        }
    }
}
