package org.domt.feedbot;

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

    }

    public static SlashCommandBuilder ToggleCarlFightBuilder() {
        return SlashCommand.with("togglefight", "Enables/disables event commands.").setDefaultPermission(false);
    }

    public static void ToggleFight(SlashCommandInteraction slashCommandInteraction) {

    }
}
