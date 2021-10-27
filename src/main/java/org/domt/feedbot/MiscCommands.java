package org.domt.feedbot;

import org.javacord.api.entity.permission.Role;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiscCommands {
    public static List<String> pronounOptions = Arrays.asList("he/him", "she/her", "they/them", "xe/xim");

    public static SlashCommandBuilder PronounBuilder() {
        return SlashCommand.with("pronouns", "Add, remove, or clear your pronoun roles", Arrays.asList(
                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Add a pronoun to your roles.", PronounChoices()),
                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "remove", "Remove a pronoun from your roles.", PronounChoices()),
                SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "clear", "Remove all of your pronoun roles.")
        ));
    }

    public static List<SlashCommandOption> PronounChoices() {
        List<SlashCommandOptionChoice> choices = new ArrayList<>();
        for (String pronoun : pronounOptions) {
            choices.add(SlashCommandOptionChoice.create(pronoun, pronoun));
        }
        return Arrays.asList(SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "pronoun", "The pronoun role to add/remove.", true, choices));
    }

    public static void ChangeProunouns(SlashCommandInteraction slashCommandInteraction) {
        if (slashCommandInteraction.getOptions().get(0).getName().equalsIgnoreCase("clear")) {
            List<Role> toRemove = new ArrayList<>();
            for (Role role : slashCommandInteraction.getUser().getRoles(Main.activeServer)) {
                if (pronounOptions.contains(role.getName())) {
                    toRemove.add(role);
                }
            }
            for (Role role : toRemove) {
                slashCommandInteraction.getUser().removeRole(role);
            }
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Removed all your pronoun roles!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        } else {
            boolean add = slashCommandInteraction.getFirstOption().get().getName().equalsIgnoreCase("add");
            String pronoun = slashCommandInteraction.getFirstOption().get().getOptionStringValueByName("pronoun").get();
            Role toChange = Main.activeServer.getRolesByName(pronoun).get(0);
            if (add) {
                slashCommandInteraction.getUser().addRole(toChange);
            } else {
                slashCommandInteraction.getUser().removeRole(toChange);
            }
            slashCommandInteraction.createImmediateResponder()
                    .setContent((add ? "Added" : "Removed") + " the selected pronoun role!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        }
    }
}
