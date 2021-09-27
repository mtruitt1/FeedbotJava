package org.domt.feedbot;

import org.javacord.api.interaction.*;

import java.util.Arrays;

public class PromptMasterCommands {
    public static SlashCommandBuilder GetMasterBuilder() {
        return SlashCommand.with("getmaster", "Grants you prompt master if you meet the reqs.");
    }

    public static SlashCommandBuilder GiveRoleBuilder() {
        return SlashCommand.with("giverole", "Gives the user the chosen role. Prompt master only.");
    }

    public static SlashCommandBuilder PingPromptsBuilder() {
        return SlashCommand.with("pingprompts", "Allows you to ping prompt roles for a prompt.");
    }
}