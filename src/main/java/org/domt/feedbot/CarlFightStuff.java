package org.domt.feedbot;

import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;

import java.util.Arrays;
import java.util.List;

public class CarlFightStuff {
    public static List<String> fightCommands = Arrays.asList("chooseclass", "submitguess", "classpower", "carlhealth", "zombiewave");
    public static List<String> classRoleNames = Arrays.asList("Class Artificer", "Class Barbarian", "Class Bard", "Class Cleric", "Class Druid");
    public static long carlHP = 0;

    public static SlashCommandBuilder ChooseClassBuilder() {
        return SlashCommand.with("chooseclass", "Choose your class for the Carl fight!");
    }

    public static SlashCommandBuilder SubmitGuessBuilder() {
        return SlashCommand.with("submitguess", "Submit a riddle or puzzle answer.");
    }

    public static SlashCommandBuilder ClassFeatureBuilder() {
        return SlashCommand.with("classpower", "Use your class's special power!").setDefaultPermission(false);
    }

    public static SlashCommandBuilder CarlHealthBuilder() {
        return SlashCommand.with("carlhealth", "Heal, damage, or set Carl's health.").setDefaultPermission(false);
    }

    public static SlashCommandBuilder ZombieWave() {
        return SlashCommand.with("zombiewave", "Send a wave of zombies to a channel!").setDefaultPermission(false);
    }
}
