package org.domt.feedbot;

import org.javacord.api.interaction.*;

import java.util.Arrays;
import java.util.List;

public class MiscCommands {
    public static SlashCommandBuilder PronounBuilder() {
        return SlashCommand.with("pronoun", "Add, remove, or clear your pronoun roles", Arrays.asList(
                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Add a pronoun to your roles.", PronounChoices()),
                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "remove", "Remove a pronoun from your roles.", PronounChoices()),
                SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "clear", "Remove all of your pronoun roles.")
        ));
    }

    public static List<SlashCommandOption> PronounChoices() {
        return Arrays.asList(
                SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "pronoun", "The pronoun role to add/remove.", true, Arrays.asList(
                                SlashCommandOptionChoice.create("he/him", "he/him"),
                                SlashCommandOptionChoice.create("she/her", "she/her"),
                                SlashCommandOptionChoice.create("they/them", "they/them"),
                                SlashCommandOptionChoice.create("xe/xim", "xe/xim")
                        )
                ));
    }
}
