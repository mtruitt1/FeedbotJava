package org.domt.feedbot;

import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

public class SlashCommandListener implements SlashCommandCreateListener {
    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
        String commandName = slashCommandInteraction.getCommandName();
        System.out.println("Received command: " + commandName);
        switch (commandName) {
            case "pronouns":
                MiscCommands.ChangeProunouns(slashCommandInteraction);
                break;
            case "getmaster":
                PromptMasterCommands.GetMaster(slashCommandInteraction);
                break;
            case "suggest":
                SuggestionCommands.Suggestion(slashCommandInteraction);
                break;
            case "pingprompts":
                PromptMasterCommands.PingPrompts(slashCommandInteraction);
                break;
            case "giverole":
                PromptMasterCommands.GiveRole(slashCommandInteraction);
                break;
            case "echo":
                ModCommands.Echo(slashCommandInteraction);
                break;
            case "colorroles":
                ModCommands.RoleSelect(slashCommandInteraction);
                break;
            case "togglefight":
                ModCommands.ToggleFight(slashCommandInteraction);
                break;
            case "chooseclass":
                CarlFightStuff.ChooseClass(slashCommandInteraction);
                break;
            case "submitguess":
                CarlFightStuff.GuessSubmission(slashCommandInteraction);
                break;
            case "checkbuffs":
                CarlFightStuff.CheckBuffs(slashCommandInteraction);
                break;
            case "attack":
                CarlFightStuff.Attack(slashCommandInteraction);
                break;
            case "classpower":
                CarlFightStuff.ClassFeature(slashCommandInteraction);
                break;
            case "carlstats":
                CarlFightStuff.SetStat(slashCommandInteraction);
                break;
            case "zombiewave":
                CarlFightStuff.ZombieWave(slashCommandInteraction);
                break;
            case "setanswer":
                CarlFightStuff.SetAnswer(slashCommandInteraction);
                break;
        }
    }
}
