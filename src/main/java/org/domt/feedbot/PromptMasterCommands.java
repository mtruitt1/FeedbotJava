package org.domt.feedbot;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PromptMasterCommands {
    public static SlashCommandBuilder GetMasterBuilder() {
        return SlashCommand.with("getmaster", "Grants you prompt master if you meet the reqs.");
    }

    public static void GetMaster(SlashCommandInteraction slashCommandInteraction) {
        User caller = slashCommandInteraction.getUser();
        List<String> roleNames = Arrays.asList(Main.botInfo.prompt_roles);
        int rolesReceived = 0;
        for (Role role : caller.getRoles(Main.activeServer)) {
            if (roleNames.contains(role.getName())) {
                rolesReceived++;
            }
        }
        if (rolesReceived > 2) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent("You are being given the Prompt Master role now!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            caller.addRole(Main.activeServer.getRoleById(Main.botInfo.promptMasterRoleID).get());
            TextChannel promptHall = (TextChannel)Main.activeServer.getChannelById(Main.botInfo.promptMasterChannelID).get();
            promptHall.sendMessage("Welcome " + caller.getMentionTag() + " to the prompt master hall! Please read the pinned messages for info about prompts.\n" +
                    "You can use `/pingprompts` to ping the prompt roles when you post a prompt, and `/giverole` to give someone a role after you post your prompt!");
        } else {
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Unfortunate you do not meet the requirements at this time! Please use this command again once you have gotten three prompt roles!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        }
    }

    public static SlashCommandBuilder GiveRoleBuilder() {
        return SlashCommand.with("giverole", "Gives the user the chosen role. Prompt master only.", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User to give a role to.", true),
                SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "role", "The role to give", true, AllPromptRolesChoices())
        )).setDefaultPermission(false);
    }

    public static void GiveRole(SlashCommandInteraction slashCommandInteraction) {
        Role toGive = Main.activeServer.getRolesByName(slashCommandInteraction.getOptionStringValueByName("role").get()).get(0);
        User giveTo = slashCommandInteraction.getOptionUserValueByName("user").get();
        if (giveTo == slashCommandInteraction.getUser()) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent("You can't give a role to yourself!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        if (giveTo.getRoles(Main.activeServer).contains(toGive)) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent(giveTo.getDisplayName(Main.activeServer) + " already has " + toGive.getName() + "!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        } else {
            giveTo.addRole(toGive);
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Gave " + toGive.getName() + " to " + giveTo.getDisplayName(Main.activeServer) + "!")
                    .respond();
        }
    }

    public static List<SlashCommandOptionChoice> AllPromptRolesChoices() {
        List<SlashCommandOptionChoice> choices = new ArrayList<>();
        for (String role : Main.botInfo.prompt_roles) {
            choices.add(SlashCommandOptionChoice.create(role, role));
        }
        return choices;
    }

    public static SlashCommandBuilder PingPromptsBuilder() {
        return SlashCommand.with("pingprompts", "Allows you to ping prompt roles for a prompt.", Arrays.asList(
                SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "roles", "Which roles to ping.", true, Arrays.asList(
                        SlashCommandOptionChoice.create("Neither", "Neither"),
                        SlashCommandOptionChoice.create("Brew", "Brew"),
                        SlashCommandOptionChoice.create("Lore", "Lore"),
                        SlashCommandOptionChoice.create("Both", "Both")
                ))
        )).setDefaultPermission(false);
    }

    public static void PingPrompts(SlashCommandInteraction slashCommandInteraction) {
        TextChannel prompts = (TextChannel)Main.activeServer.getChannelById(Main.botInfo.promptChannelID).orElse(null);
        if (prompts == null) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Couldn't find the prompts channel!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        Role allPrompts = Main.activeServer.getRolesByName("AllPrompts").get(0);
        Role brewPrompts = Main.activeServer.getRolesByName("BrewPrompts").get(0);
        Role lorePrompts = Main.activeServer.getRolesByName("LorePrompts").get(0);
        String toSend = "";
        switch (slashCommandInteraction.getOptionStringValueByName("roles").orElse("")) {
            case "Brew":
                toSend = allPrompts.getMentionTag() + " " + brewPrompts.getMentionTag();
                break;
            case "Lore":
                toSend = allPrompts.getMentionTag() + " " + lorePrompts.getMentionTag();
                break;
            case "Both":
                toSend = allPrompts.getMentionTag() + " " + brewPrompts.getMentionTag() + " " + lorePrompts.getMentionTag();
                break;
            default:
                toSend = allPrompts.getMentionTag();
                break;
        }
        prompts.sendMessage(toSend);
        slashCommandInteraction.createImmediateResponder()
                .setContent("Pinged the selected prompt roles!")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
    }
}