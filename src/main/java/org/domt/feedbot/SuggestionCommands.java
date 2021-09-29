package org.domt.feedbot;

import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.Arrays;

public class SuggestionCommands {
    public static SlashCommandBuilder SuggestBuilder() {
        return SlashCommand.with("suggest", "Suggest something for the server!", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "anonymous", "Send suggestion anonymously?.", true),
                SlashCommandOption.create(SlashCommandOptionType.STRING, "suggestion", "Your suggestion", true)
        ));
    }
}
