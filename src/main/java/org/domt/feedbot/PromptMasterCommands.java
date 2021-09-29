package org.domt.feedbot;

import org.javacord.api.interaction.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PromptMasterCommands {
    public static SlashCommandBuilder GetMasterBuilder() {
        return SlashCommand.with("getmaster", "Grants you prompt master if you meet the reqs.");
    }

    public static SlashCommandBuilder GiveRoleBuilder() {
        return SlashCommand.with("giverole", "Gives the user the chosen role. Prompt master only.", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User to give a role to.", true),
                SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "role", "The role to give", true, AllPromptRolesChoices())
        )).setDefaultPermission(false);
    }

    public static List<SlashCommandOptionChoice> AllPromptRolesChoices() {
        List<SlashCommandOptionChoice> choices = new ArrayList<>();
        for (String role : Main.botInfo.prompt_roles) {
            choices.add(SlashCommandOptionChoice.create(role, role));
        }
        return choices;
    }

    public static SlashCommandBuilder PingPromptsBuilder() {
        return SlashCommand.with("pingprompts", "Allows you to ping prompt roles for a prompt.").setDefaultPermission(false);
    }
}