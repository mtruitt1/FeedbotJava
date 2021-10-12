package org.domt.feedbot;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.*;

import java.util.Arrays;

public class ModCommands {
    public static SlashCommandBuilder EchoBuilder() {
        return SlashCommand.with("echo", "Echos the message to the selected channel.", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "Where to send the message", true),
                SlashCommandOption.create(SlashCommandOptionType.STRING, "message", "What to send", true)
        )).setDefaultPermission(false);
    }

    public static void Echo(SlashCommandInteraction slashCommandInteraction) {
        ServerTextChannel sendTo = (ServerTextChannel) slashCommandInteraction.getOptionChannelValueByName("channel").get();
        sendTo.sendMessage(slashCommandInteraction.getOptionStringValueByName("message").get());
        slashCommandInteraction.createImmediateResponder()
                .setContent("Sent message to " + sendTo.getMentionTag() + "!")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
    }

    public static SlashCommandBuilder RoleSelectBuilder() {
        return SlashCommand.with("colorroles", "Reconfigures the color role select message(s)").setDefaultPermission(false);
    }

    public static void RoleSelect(SlashCommandInteraction slashCommandInteraction) {
        ColorRoleSelection.SetColorRoleEmojiTable();
        slashCommandInteraction.createImmediateResponder()
                .setContent("Updated color role selection message(s)!")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
    }

    public static SlashCommandBuilder ToggleCarlFightBuilder() {
        return SlashCommand.with("togglefight", "Enables/disables event commands.").setDefaultPermission(false);
    }

    public static void ToggleFight(SlashCommandInteraction slashCommandInteraction) {
        slashCommandInteraction.createImmediateResponder()
                .setContent(CarlFightStuff.fightActive ? "Toggling fight off!" : "Toggling fight on!")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
        CarlFightStuff.fightActive = !CarlFightStuff.fightActive;
        if (CarlFightStuff.fightActive) {
            CarlFightStuff.SetupFight();
        } else {
            CarlFightStuff.EndFight();
        }
        Main.sandbox.sendMessage("Carl health: " + CarlFightStuff.carlStats.get("HP") + "\nDamage received (Total): " + CarlFightStuff.totalDamage);
        if (CarlFightStuff.fightActive) {
            Main.sandbox.sendMessage(slashCommandInteraction.getUser().getMentionTag() + ": Make sure to check the fight channel is correct, and if not set it using the dropdown!");
        }
    }
}
