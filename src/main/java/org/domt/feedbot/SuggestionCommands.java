package org.domt.feedbot;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;

import java.awt.*;
import java.util.Arrays;

public class SuggestionCommands {
    public static SlashCommandBuilder SuggestBuilder() {
        return SlashCommand.with("suggest", "Suggest something for the server!", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "showsender", "If false, suggestion is anonymous", true),
                SlashCommandOption.create(SlashCommandOptionType.STRING, "suggestion", "Your suggestion", true)
        ));
    }

    public static void Suggestion(SlashCommandInteraction slashCommandInteraction) {
        boolean isAnonymous = !slashCommandInteraction.getOptionBooleanValueByName("showsender").get();
        String suggestion = slashCommandInteraction.getOptionStringValueByName("suggestion").get();
        String title = "Suggestion received from " +
                (isAnonymous ?
                        "Anonymous" :
                        slashCommandInteraction.getUser().getDisplayName(Main.activeServer) + " (@" + slashCommandInteraction.getUser().getDiscriminatedName() + ")");
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(title)
                .setColor(isAnonymous ? Color.gray : Color.GREEN);
        builder.setDescription(isAnonymous ? "(Anonymous submission, no mention tag)" : slashCommandInteraction.getUser().getMentionTag());
        builder.addField("Suggestion:", suggestion);
        if (!isAnonymous) {
            builder.setThumbnail(slashCommandInteraction.getUser().getAvatar());
        }
        new MessageBuilder().setEmbed(builder)
                .send(Main.activeServer.getTextChannelById(Main.botInfo.suggestionLogID).get()).whenComplete((message, throwable) -> {
                    slashCommandInteraction.createImmediateResponder()
                            .setContent("Your suggestion has been submitted! If you don't get a message from a moderator regarding your suggestion soon, feel free to DM one of us about it!\n" +
                                    "Below is a preview of how we see your suggestion, and a button to un-send if you wish to do so.")
                            .addEmbed(builder)
                            .addComponents(ActionRow.of(
                                    Button.danger("unsend" + message.getId(), "Un-send")
                            ))
                            .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                            .respond();
                });
    }

    public static void UnsendMessage(ButtonInteraction buttonInteraction) {
        long messageID = Long.parseLong(buttonInteraction.getCustomId().split("d")[1]);
        ServerTextChannel suggestLog = Main.activeServer.getTextChannelById(Main.botInfo.suggestionLogID).get().asServerTextChannel().get();
        suggestLog.getMessageById(messageID).join().edit("**[This suggestion was deleted by its creator.]**").join().removeEmbed();
        buttonInteraction.createImmediateResponder()
                .setContent("Deleted your suggestion!")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
        buttonInteraction.createOriginalMessageUpdater().removeAllEmbeds().removeAllComponents().update();
    }
}
