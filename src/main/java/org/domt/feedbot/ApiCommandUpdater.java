package org.domt.feedbot;

import org.javacord.api.entity.permission.Role;
import org.javacord.api.interaction.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ApiCommandUpdater {
    public static HashMap<String, SlashCommandBuilder> everyonePermanent = new HashMap<>();
    public static HashMap<String, SlashCommandBuilder> promptMasterPermanent = new HashMap<>();
    public static HashMap<String, SlashCommandBuilder> moderatorPermanent = new HashMap<>();
    public static HashMap<String, SlashCommandBuilder> allPermanent = new HashMap<>();
    public static HashMap<String, SlashCommandBuilder> fightCommands = new HashMap<>();
    public static HashMap<String, SlashCommandBuilder> everyoneCommands = new HashMap<>();
    public static HashMap<String, SlashCommandBuilder> fighterCommands = new HashMap<>();
    public static HashMap<String, SlashCommandBuilder> showrunnerCommands = new HashMap<>();
    static {
        everyonePermanent.put("pronouns", MiscCommands.PronounBuilder());
        everyonePermanent.put("getmaster", PromptMasterCommands.GetMasterBuilder());
        everyonePermanent.put("suggest", SuggestionCommands.SuggestBuilder());

        promptMasterPermanent.put("giverole", PromptMasterCommands.GiveRoleBuilder());
        promptMasterPermanent.put("pingprompts", PromptMasterCommands.PingPromptsBuilder());

        moderatorPermanent.put("echo", ModCommands.EchoBuilder());
        moderatorPermanent.put("colorroles", ModCommands.RoleSelectBuilder());
        moderatorPermanent.put("togglefight", ModCommands.ToggleCarlFightBuilder());

        allPermanent.putAll(everyonePermanent);
        allPermanent.putAll(promptMasterPermanent);
        allPermanent.putAll(moderatorPermanent);

        everyoneCommands.put("chooseclass", CarlFightStuff.ChooseClassBuilder());
        everyoneCommands.put("submitguess", CarlFightStuff.SubmitGuessBuilder());

        fighterCommands.put("checkbuffs", CarlFightStuff.CheckBuffsBuilder());
        fighterCommands.put("attack", CarlFightStuff.AttackBuilder());
        fighterCommands.put("classpower", CarlFightStuff.ClassFeatureBuilder());

        showrunnerCommands.put("carlstats", CarlFightStuff.CarlStatsBuilder());
        showrunnerCommands.put("zombiewave", CarlFightStuff.ZombieWaveBuilder());
        showrunnerCommands.put("setanswer", CarlFightStuff.SetAnswerBuilder());

        fightCommands.putAll(everyoneCommands);
        fightCommands.putAll(fighterCommands);
        fightCommands.putAll(showrunnerCommands);
    }

    public static void UpdateAllSlashCommands(boolean addFight) {
        List<SlashCommandBuilder> builders = new ArrayList<>(allPermanent.values());
        if (addFight) {
            builders.addAll(fightCommands.values());
        }
        Main.api.bulkOverwriteServerSlashCommands(Main.activeServer, builders);
        List<ServerSlashCommandPermissionsBuilder> permUpdates = new ArrayList<>();
        for (SlashCommand command : Main.api.getServerSlashCommands(Main.activeServer).join()) {
            if (fightCommands.containsKey(command.getName())) {
                if (!everyoneCommands.containsKey(command.getName())) {
                    List<SlashCommandPermissions> allowedRoles = new ArrayList<>();
                    if (fighterCommands.containsKey(command.getName())) {
                        for (Role level : CarlFightStuff.levelRoles) {
                            allowedRoles.add(SlashCommandPermissions.create(level.getId(), SlashCommandPermissionType.ROLE, true));
                        }
                    } else {
                        allowedRoles.add(SlashCommandPermissions.create(Main.botInfo.staffRoleID, SlashCommandPermissionType.ROLE, true));
                    }
                    permUpdates.add(new ServerSlashCommandPermissionsBuilder(command.getId(), allowedRoles));
                }
            } else {
                if (!everyonePermanent.containsKey(command.getName())) {
                    long roleID = promptMasterPermanent.containsKey(command.getName()) ? Main.botInfo.promptMasterRoleID : Main.botInfo.staffRoleID;
                    permUpdates.add(new ServerSlashCommandPermissionsBuilder(command.getId(), Arrays.asList(
                            SlashCommandPermissions.create(roleID, SlashCommandPermissionType.ROLE, true))));
                }
            }
        }
        Main.api.batchUpdateSlashCommandPermissions(Main.activeServer, permUpdates);
    }
}
