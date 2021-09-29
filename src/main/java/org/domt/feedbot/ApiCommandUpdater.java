package org.domt.feedbot;

import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiCommandUpdater {
    public static void RemoveSlashCommands(List<String> toRemove) throws ExecutionException, InterruptedException {
        Server server = Main.api.getServerById(Main.botInfo.activeServerID).orElse(null);
        if (server == null) {
            return;
        }
        List<SlashCommand> commands = Main.api.getServerSlashCommands(server).join();
        for (SlashCommand command : commands) {
            if (toRemove == null || toRemove.contains(command.getName())) {
                System.out.println("Removing command " + command.getName() + " from " + server.getName());
                command.deleteForServer(server).get();
            }
        }
    }

    public static void AddAllNormalSlashCommands() throws ExecutionException, InterruptedException {
        Server server = Main.api.getServerById(Main.botInfo.activeServerID).orElse(null);
        if (server == null) {
            return;
        }
        List<SlashCommandBuilder> everyoneCommands = new ArrayList<>();
        everyoneCommands.add(MiscCommands.PronounBuilder());
        everyoneCommands.add(PromptMasterCommands.GetMasterBuilder());

        List<SlashCommandBuilder> promptMasterCommands = new ArrayList<>();
        promptMasterCommands.add(PromptMasterCommands.GiveRoleBuilder());
        promptMasterCommands.add(PromptMasterCommands.PingPromptsBuilder());

        List<SlashCommandBuilder> moderatorCommands = new ArrayList<>();
        moderatorCommands.add(ModCommands.EchoBuilder());
        moderatorCommands.add(ModCommands.ToggleCarlFightBuilder());

        List<SlashCommandBuilder> allCommands = new ArrayList<>();
        allCommands.addAll(everyoneCommands);
        allCommands.addAll(promptMasterCommands);
        allCommands.addAll(moderatorCommands);

        for (SlashCommandBuilder builder : allCommands) {
            SlashCommand command = builder.createForServer(server).join();
            if (!everyoneCommands.contains(builder)) {
                long roleID = promptMasterCommands.contains(builder) ? Main.botInfo.promptMasterRoleID : Main.botInfo.staffRoleID;
                new SlashCommandPermissionsUpdater(server).setPermissions(Arrays.asList(SlashCommandPermissions.create(roleID, SlashCommandPermissionType.ROLE, true)))
                        .update(command.getId()).join();
            }
        }
    }
}
