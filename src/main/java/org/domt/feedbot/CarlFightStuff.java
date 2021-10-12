package org.domt.feedbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.*;

public class CarlFightStuff {
    public static String[] classRoleNames = {
            "Class Artificer",
            "Class Barbarian",
            "Class Bard",
            "Class Cleric",
            "Class Druid",
            "Class Fighter",
            "Class Monk",
            "Class Paladin",
            "Class Ranger",
            "Class Rogue",
            "Class Sorcerer",
            "Class Warlock",
            "Class Wizard"
    };
    public static List<Role> classRoles = new ArrayList<>();
    public static Role[] levelRoles = new Role[5];
    public static Role downedRole = null;
    public static ServerTextChannel fightChannel = null;
    public static Message carlHealthBar = null;
    public static ServerTextChannel answerChannel = null;
    public static String currentAnswer;
    public static int answersNeeded = 0;
    public static boolean fightActive = false;
    public static HashMap<String, Integer> carlStats = new HashMap<>();
    static {
        carlStats.put("HP", 10000);
        carlStats.put("maxHP", 10000);
        carlStats.put("AC", 20);
        carlStats.put("minAC", 14);
        carlStats.put("attackMod", 12);
        carlStats.put("minAttackMod", 4);
        carlStats.put("saveMod", 4);
        carlStats.put("hideDC", 15);
    }
    public static int totalDamage = 0;
    public static int carlHexRemaining = 0;
    public static int carlWepSabo = 0;
    public static int carlACSabo = 0;
    public static int skipCounterAttacks = 0;
    public static int[] bardInspires = new int[3];
    public static int wolfCount = 0;
    public static List<ServerTextChannel> blockedChannels = new ArrayList<>();
    public static CarlFightClassAttacks classAttacksInfo;
    public static HashMap<User, Instant> userClassFeatureTimes = new HashMap<>();
    public static Timer carlTimer;
    public static Timer barTimer;
    public static boolean updateNeeded = true;
    public static List<User> wildShapedUsers = new ArrayList<>();
    public static List<User> companionPack = new ArrayList<>();

    public static void SetupFight() {
        downedRole = Main.activeServer.getRolesByName("Downed").get(0);
        for (String name : classRoleNames) {
            classRoles.add(Main.activeServer.getRolesByName(name).get(0));
        }
        for (int i = 0; i < 5; i++) {
            levelRoles[i] = Main.activeServer.getRolesByName("Level " + (i + 1)).get(0);
        }

        ApiCommandUpdater.UpdateAllSlashCommands(true);

        Gson gson = new GsonBuilder().setLenient().create();
        String json = "";
        try {
            File f = new File("carl_fight.json");
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    json += line;
                }
            }
            System.out.println(json);
            classAttacksInfo = gson.fromJson(json, CarlFightClassAttacks.class);
            classAttacksInfo.ConvertToMap();
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return;
        }

        carlTimer = new Timer();
        carlTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                carlACSabo--;
                if (carlACSabo < 0) {
                    carlACSabo = 0;
                }
                carlWepSabo--;
                if (carlWepSabo < 0) {
                    carlWepSabo = 0;
                }
                carlHexRemaining--;
                if (carlHexRemaining < 0) {
                    carlHexRemaining = 0;
                }
                skipCounterAttacks--;
                if (skipCounterAttacks < 0) {
                    skipCounterAttacks = 0;
                }
            }
        }, 0, 60*1000);

        fightChannel = Main.activeServer.getTextChannelsByName("fight").get(0);

        List<SelectMenuOption> channelChoices = new ArrayList<>();
        for (ServerTextChannel channel : Main.activeServer.getTextChannels()) {
            channelChoices.add(SelectMenuOption.create("#" + channel.getName(), channel.getIdAsString()));
        }

        new MessageBuilder()
                .append("Carl fight is fully finished setting up!\nThe default fight channel is set to " + fightChannel.asServerTextChannel().get().getMentionTag() +
                        "\nUse the following dropdown to change the channel if that is incorrect:")
                        .addComponents(ActionRow.of(
                                SelectMenu.create("channelSelect", "Channel...", 1, 1, channelChoices)
                        ))
                .send(Main.sandbox);

        NewFightChannel();
        barTimer = new Timer();
        barTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (updateNeeded) {
                    PrintCarlBar();
                    updateNeeded = false;
                }
            }
        }, 0, 1000);

        fightActive = true;
    }

    public static void FightChannelSelection(SelectMenuInteraction selectMenuInteraction) {
        String channelID = selectMenuInteraction.getChosenOptions().get(0).getValue();
        ServerTextChannel newChannel = Main.activeServer.getChannelById(channelID).orElse(null).asServerTextChannel().orElse(null);
        if (newChannel != null) {
            fightChannel = Main.activeServer.getChannelById(channelID).orElse(null).asServerTextChannel().orElse(null);
            NewFightChannel();
            selectMenuInteraction.createImmediateResponder()
                    .setContent("Channel changed to "+ fightChannel.getMentionTag() + "! To change it, please see the original message.")
                    .respond();
        } else {
            selectMenuInteraction.createImmediateResponder()
                    .setContent("Could not change to the selected channel: " + selectMenuInteraction.getChosenOptions().get(0).getLabel())
                    .respond();
        }
    }

    public static void NewFightChannel() {
        MessageSet messages = fightChannel.getMessages(50).join();
        List<Message> myMessages = new ArrayList<>();
        for (Message m : messages.descendingSet()) {
            if (m.getAuthor().isYourself() && m.getContent().contains("CARL HEALTH")) {
                myMessages.add(m);
            }
        }
        myMessages.forEach(message -> {message.delete();});
        updateNeeded = true;
    }

    public static void EndFight() {
        ApiCommandUpdater.UpdateAllSlashCommands(false);
        carlHealthBar.delete();
        carlTimer.cancel();
        barTimer.cancel();
    }

    public static int UserLevel(User user) {
        //System.out.println(user);
        int highestLevel = 0;
        for (int i = 0; i < 5; i++) {
            //System.out.println("Level: " + levelRoles[i] + " " + levelRoles[i].getUsers().size());
            if (levelRoles[i].hasUser(user)) {
                highestLevel = i + 1;
            }
        }
        //System.out.println(user.getDiscriminatedName() + " is " + highestLevel);
        return highestLevel;
    }

    public static String UserClass(User user) {
        for (Role playerClass : classRoles) {
            if (user.getRoles(Main.activeServer).contains(playerClass)) {
                return playerClass.getName();
            }
        }
        return "None";
    }

    public static int GetCarlAC() {
        int carlFinalAC = carlStats.get("AC") - carlACSabo;
        if (carlFinalAC < carlStats.get("minAC")) {
            carlFinalAC = carlStats.get("minAC");
        }
        return carlFinalAC;
    }

    public static int GetCarlAttack() {
        int finalAttackMod = carlStats.get("attackMod") - carlWepSabo;
        if (finalAttackMod < carlStats.get("minAttackMod")) {
            finalAttackMod = carlStats.get("minAttackMod");
        }
        return finalAttackMod;
    }

    public static SlashCommandBuilder ChooseClassBuilder() {
        return SlashCommand.with("chooseclass", "Choose your class for the Carl fight!");
    }

    public static void ChooseClass(SlashCommandInteraction slashCommandInteraction) {
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() +
                        ": Choose your class for the current Carl fight! You can change your class at any time, but when you do, your level is set back to 1.\n" +
                        "Changing your class also immediately revives you!")
                .addComponents(ActionRow.of(
                        SelectMenu.create("classchoice", "Select a class...", 1, 1, Arrays.asList(
                                SelectMenuOption.create("Artificer (17 AC)", "Class Artificer"),
                                SelectMenuOption.create("Barbarian (17 AC)", "Class Barbarian"),
                                SelectMenuOption.create("Bard (15 AC)", "Class Bard"),
                                SelectMenuOption.create("Cleric (17 AC)", "Class Cleric"),
                                SelectMenuOption.create("Druid (15 AC)", "Class Druid"),
                                SelectMenuOption.create("Fighter (19 AC)", "Class Fighter"),
                                SelectMenuOption.create("Monk (17 AC)", "Class Monk"),
                                SelectMenuOption.create("Paladin (19 AC)", "Class Paladin"),
                                SelectMenuOption.create("Ranger (17 AC)", "Class Ranger"),
                                SelectMenuOption.create("Rogue (15 AC)", "Class Rogue"),
                                SelectMenuOption.create("Sorcerer (15 AC)", "Class Sorcerer"),
                                SelectMenuOption.create("Warlock (15 AC)", "Class Warlock"),
                                SelectMenuOption.create("Wizard (15 AC)", "Class Wizard")
                        ))
                ))
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
    }

    public static void ClassChosen(SelectMenuInteraction selectMenuInteraction) {
        String userClass = UserClass(selectMenuInteraction.getUser());
        int userLevel = UserLevel(selectMenuInteraction.getUser());
        SelectMenuOption selection = selectMenuInteraction.getChosenOptions().get(0);
        if (selection.getValue().equalsIgnoreCase(userClass)) {
            if (userLevel == 0) {
                selectMenuInteraction.getUser().addRole(levelRoles[0]);
            }
            selectMenuInteraction.createImmediateResponder()
                    .setContent(selectMenuInteraction.getUser().getMentionTag() + ": Your class remains the same!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        } else {
            if (userLevel > 0) {
                selectMenuInteraction.getUser().removeRole(levelRoles[userLevel - 1]);
            }
            Role addClass = null;
            for (Role classRole : classRoles) {
                if (classRole.getName().equalsIgnoreCase(selection.getValue())) {
                    addClass = classRole;
                }
                if (selectMenuInteraction.getUser().getRoles(Main.activeServer).contains(classRole) && !classRole.getName().equalsIgnoreCase(selection.getValue())) {
                    selectMenuInteraction.getUser().removeRole(classRole);
                }
            }
            selectMenuInteraction.getUser().addRole(addClass);
            selectMenuInteraction.getUser().addRole(levelRoles[0]);
            if (selectMenuInteraction.getUser().getRoles(Main.activeServer).contains(downedRole)) {
                selectMenuInteraction.getUser().removeRole(downedRole);
            }
            selectMenuInteraction.createImmediateResponder()
                    .setContent(selectMenuInteraction.getUser().getMentionTag() + ": You are now a level 1 " + selection.getLabel() + "!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        }
    }

    public static SlashCommandBuilder SubmitGuessBuilder() {
        return SlashCommand.with("submitguess", "Submit a riddle or puzzle answer.", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.STRING, "answer", "Your guess for the daily puzzle.")
        ));
    }

    public static void GuessSubmission(SlashCommandInteraction slashCommandInteraction) {
        ServerTextChannel sentIn = (ServerTextChannel)slashCommandInteraction.getChannel().orElse(null);
        if (sentIn == null) {
            return;
        }
        if (sentIn.getId() == answerChannel.getId()) {
            if (blockedChannels.contains(answerChannel)) {
                slashCommandInteraction.createImmediateResponder()
                        .setContent("You can't submit an answer to a channel with an active zombie wave in it! **Check the pins to find the message and help!**")
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
            } else {
                String answer = slashCommandInteraction.getOptionStringValueByName("answer").orElse(null);
                if (answer.equals(currentAnswer)) {
                    answersNeeded--;
                    slashCommandInteraction.createImmediateResponder()
                            .setContent("You've entered the correct answer! In the interest of keeping the event fun for others, please do not share this answer with those who haven't solved the puzzle!")
                            .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                            .respond();
                } else {
                    slashCommandInteraction.createImmediateResponder()
                            .setContent("Sorry, but that answer isn't correct.")
                            .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                            .respond();
                }
            }
        } else {
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Wrong channel for this answer!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        }
    }

    //returns -20 on critical, -1 on fumble, otherwise returns positive only
    public static int RollD20(int modifier) {
        int inspSize = 2;
        for (int i = 0; i < bardInspires.length; i++) {
            if (bardInspires[2 - i] > 0) {
                bardInspires[2 - i]--;
                inspSize += 3 - i;
                break;
            }
        }
        inspSize *= 2;
        int rawDice = 1 + Main.rand.nextInt(20);
        if (rawDice == 20) {
            return -20;
        }
        if (rawDice == -1) {
            return -1;
        }
        return rawDice + modifier + 1 + Main.rand.nextInt(inspSize);
    }

    public static int DamageCarl(int damage) {
        int finalDamage = damage;
        if (carlHexRemaining > 0) {
            carlHexRemaining--;
            finalDamage += Main.rand.nextInt(6) + 1;
        }
        int carlHP = carlStats.get("HP");
        carlHP -= finalDamage;
        carlStats.replace("HP", Math.max(carlHP, 0));
        totalDamage += finalDamage;
        return finalDamage;
    }

    public static void PrintCarlBar() {
        String carlHealth = "**CARL HEALTH:** ";
        double ratio = (double)carlStats.get("HP") / (double)carlStats.get("maxHP");
        int red = (int)Math.ceil(ratio * 50);
        System.out.println(ratio + " " + red);
        for (int i = 0; i < 50; i++) {
            if (i < red) {
                carlHealth += ":heart:";
            } else {
                carlHealth += ":white_small_square:";
            }
        }
        carlHealth += "\n*(This message is semi-sticky! It will only update once a second as needed.)*";
        if (carlHealthBar != null) {
            carlHealthBar.delete();
        }
        carlHealthBar = fightChannel.sendMessage(carlHealth).join();
    }

    public static boolean CarlCounter(User user) {
        int playerAC = 15;
        String userClass = UserClass(user);
        if (userClass.equalsIgnoreCase("Class Fighter") ||
                userClass.equalsIgnoreCase("ClassPaladin")) {
            playerAC = 19;
        } else if (userClass.equalsIgnoreCase("Class Artificer") ||
                userClass.equalsIgnoreCase("Class Barbarian") ||
                userClass.equalsIgnoreCase("Class Cleric") ||
                userClass.equalsIgnoreCase("Class Monk") ||
                userClass.equalsIgnoreCase("Class Ranger")) {
            playerAC = 17;
        }
        playerAC += (int)Math.floor(UserLevel(user) / 2.0);
        int roll = 1 + Main.rand.nextInt(20);
        if (roll == 20 || roll + GetCarlAttack() >= playerAC) {
            return true;
        }
        return false;
    }

    public static SlashCommandBuilder CheckBuffsBuilder() {
        return SlashCommand.with("checkbuffs", "Check on the current state of buffs").setDefaultPermission(false);
    }

    public static void CheckBuffs(SlashCommandInteraction slashCommandInteraction) {
        TextChannel sentIn = slashCommandInteraction.getChannel().orElse(null);
        if (sentIn == null) {
            return;
        }
        if (sentIn.getId() != fightChannel.getId()) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Wrong channel for this! Please head over to " + fightChannel)
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        int carlAttackMinus = Math.min(carlStats.get("attackMod") - carlStats.get("minAttackMod"), carlWepSabo);
        int carlACMinus = Math.min(carlStats.get("AC") - carlStats.get("minAC"), carlACSabo);
        slashCommandInteraction.createImmediateResponder()
                .setContent("**Current state of buffs:**\n" +
                        "> The next **" + carlHexRemaining + "** attacks will have hex damage.\n" +
                        "> Carl has a **-" + carlAttackMinus + "** to hit.\n" +
                        "> Carl has a **-" + carlACMinus + "** to his AC.\n" +
                        "> Carl can't counter-attack for **" + skipCounterAttacks + "** attacks.\n" +
                        "> There are **" + bardInspires[0] + "** d6 inspiration dice, **" + bardInspires[1] + "** d8 inspiration dice, and **" + bardInspires[2] + "** d10 inspiration dice.")
                .respond();
    }

    public static SlashCommandBuilder AttackBuilder() {
        return SlashCommand.with("attack", "Attack Carl!!").setDefaultPermission(false);
    }

    public static Message Attack(InteractionBase interaction) { return Attack(interaction, 0, 0, false, false); }

    public static Message Attack(InteractionBase interaction, int extraMod, int extraDam, boolean adv, boolean resist) {
        ServerTextChannel sentIn = (ServerTextChannel)interaction.getChannel().orElse(null);
        if (sentIn == null) {
            return null;
        }
        if (sentIn.getId() != fightChannel.getId()) {
            interaction.createImmediateResponder()
                    .setContent(interaction.getUser().getMentionTag() + ": Wrong channel for this! Please head over to " + fightChannel)
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return null;
        }
        if (interaction.getUser().getRoles(Main.activeServer).contains(downedRole)) {
            interaction.createImmediateResponder()
                    .setContent(interaction.getUser().getMentionTag() + ": You're downed! Ask someone to revive you so you can get back in the fight!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return null;
        }
        if (blockedChannels.contains(sentIn)) {
            interaction.createImmediateResponder()
                    .setContent("Carl is currently blocked by zombies! He can't be hit by normal attacks until the zombies are gone! Check pins to find the wave(s)!")
                    .respond();
            return null;
        }
        if (wildShapedUsers.contains(interaction.getUser())) {
            interaction.createImmediateResponder()
                    .setContent(interaction.getUser().getMentionTag() + ": You can only use wildshape attacks while in your wildshape!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return null;
        }
        int userLevel = UserLevel(interaction.getUser());
        String userClass = UserClass(interaction.getUser());
        String outputMessage = RollAttackAndDamage(interaction.getUser(), userClass, userLevel, 0, extraMod, extraDam, adv, resist);
        if (outputMessage.contains(" attempts to attack Carl but misses!")) {
            return interaction.createImmediateResponder()
                    .setContent(outputMessage)
                    .respond().join().update().join();
        } else {
            return interaction.createImmediateResponder()
                    .setContent(outputMessage)
                    .respond().join().update().join();
        }
    }

    public static String RollAttackAndDamage(User user, String userClass, int userLevel, int offset, int extraMod, int extraDamage, boolean adv, boolean resist) {
        boolean downed = false;
        boolean skipped = false;
        if (skipCounterAttacks == 0) {
            downed = CarlCounter(user);
        } else {
            skipped = true;
            skipCounterAttacks--;
            if (skipCounterAttacks < 0) {
                skipCounterAttacks = 0;
            }
        }
        int toHit = userLevel + (userLevel > 3 ? 4 : 3);
        int roll = RollD20(toHit);
        if (adv) {
            int newRoll = RollD20(toHit);
            if (newRoll > roll || newRoll == -20) {
                roll = newRoll;
            }
        }
        if ((roll > GetCarlAC() || roll == -20) && roll != 1) {
            CarlFightClassAttacks.ClassList.Level.Attack[] attackOptions = classAttacksInfo.classNameHashMap.get(userClass)[userLevel - 1].attacks;
            CarlFightClassAttacks.ClassList.Level.Attack attack = attackOptions[offset + Main.rand.nextInt(attackOptions.length - offset)];
            String[] diceRolls = attack.dice.trim().split("\\s+");
            int rollTotal = 0;
            for (String dice : diceRolls) {
                String[] numSize = dice.split("d");
                int num = Integer.parseInt(numSize[0]);
                int size = Integer.parseInt(numSize[1]);
                for (int d = 0; d < num; d++) {
                    rollTotal += Main.rand.nextInt(size) + 1;
                }
            }
            String isCritical = "";
            if (roll == -20) {
                rollTotal *= 2;
                isCritical = " (Critical damage!)";
            }
            rollTotal += attack.extra;
            if (attack.addMod) {
                rollTotal += userLevel > 3 ? 4 : 3;
            }
            String[] messageSplit = attack.message.split("#");
            if (downed && !resist) {
                user.addRole(downedRole).join();
            }
            return user.getMentionTag() + " " + messageSplit[0] + "**" + DamageCarl(rollTotal) + "**" + isCritical + messageSplit[1] + (skipped ? "" :
                    (downed ? "\nCarl counter-attacks and downs " + user.getMentionTag() + "!" : "\nCarl swings but misses with his counter-attack!"));
        } else {
            return user.getMentionTag() + " attempts to attack Carl but misses!" + (skipped ? "" :
                    (downed ? "\nCarl counter-attacks and downs " + user.getMentionTag() + "!" : "\nCarl swings but misses with his counter-attack!"));
        }
    }

    public static SlashCommandBuilder ClassFeatureBuilder() {
        return SlashCommand.with("classpower", "Use your class's special power!").setDefaultPermission(false);
    }

    public static void ClassFeature(SlashCommandInteraction slashCommandInteraction) {
        TextChannel sentIn = slashCommandInteraction.getChannel().orElse(null);
        String userClass = UserClass(slashCommandInteraction.getUser());
        if (sentIn == null) {
            return;
        }
        if (sentIn.getId() != fightChannel.getId()) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Wrong channel for this! Please head over to " + fightChannel.getMentionTag())
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        if (slashCommandInteraction.getUser().getRoles(Main.activeServer).contains(downedRole) && !userClass.equals("Class Ranger")) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + ": You're downed! Ask someone to revive you so you can get back in the fight!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        if (userClassFeatureTimes.containsKey(slashCommandInteraction.getUser())) {
            Instant lastUse = userClassFeatureTimes.get(slashCommandInteraction.getUser());
            Instant thisUse = slashCommandInteraction.getCreationTimestamp();
            if (thisUse.minusSeconds(60 * 0).isBefore(lastUse)) {
                slashCommandInteraction.createImmediateResponder()
                        .setContent(slashCommandInteraction.getUser().getMentionTag() + ": You can only use your class feature once every three minutes!")
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            } else {
                userClassFeatureTimes.replace(slashCommandInteraction.getUser(), slashCommandInteraction.getCreationTimestamp());
            }
        } else {
            userClassFeatureTimes.put(slashCommandInteraction.getUser(), slashCommandInteraction.getCreationTimestamp());
        }
        downedRole = downedRole.getLatestInstance().join();
        int userLevel = UserLevel(slashCommandInteraction.getUser());
        switch (userClass) {
            case "Class Artificer":
                Sabotage(userLevel, slashCommandInteraction);
                break;
            case "Class Barbarian":
                Rage(userLevel, slashCommandInteraction);
                break;
            case "Class Bard":
                AddInspiration(userLevel, slashCommandInteraction);
                break;
            case "Class Cleric":
                RevivePlayers(userLevel, slashCommandInteraction);
                break;
            case "Class Druid":
                WildShape(userLevel, slashCommandInteraction);
                break;
            case "Class Fighter":
                Maneuver(userLevel, slashCommandInteraction);
                break;
            case "Class Monk":
                KiPoints(userLevel, slashCommandInteraction);
                break;
            case "Class Paladin":
                Paladin(userLevel, slashCommandInteraction);
                break;
            case "Class Ranger":
                Companion(userLevel, slashCommandInteraction);
                break;
            case "Class Rogue":
                Sneak(userLevel, slashCommandInteraction);
                break;
            case "Class Sorcerer":
                Metamagic(userLevel, slashCommandInteraction);
                break;
            case "Class Warlock":
                AddHex(userLevel, slashCommandInteraction);
                break;
            case "Class Wizard":
                WizardSpell(userLevel, slashCommandInteraction);
                break;
        }
    }

    public static void Sabotage(int level, SlashCommandInteraction slashCommandInteraction) {
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Choose whether to sabotage Carl's weapons or armor:")
                .addComponents(ActionRow.of(
                        Button.danger("sabowep", "Weapons"),
                        Button.primary("saboarm", "Armor")
                ))
                .respond();
    }

    public static void SabotageButton(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        int amount = (int)Math.ceil(UserLevel(buttonInteraction.getUser()) / 2.0);
        if (buttonInteraction.getCustomId().equalsIgnoreCase("sabowep")) {
            carlWepSabo += amount;
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " sabotages Carl's weapons! His attacks are less likely to hit!")
                    .respond();
        } else {
            carlACSabo += 1;
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " sabotages Carl's armor! He's easier to hit now!")
                    .respond();
        }
        source.delete();
    }

    public static void Rage(int level, SlashCommandInteraction slashCommandInteraction) {
        int resistDeath = (int)Math.ceil(level / 2.0);
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": You enter a rage for 10 attacks! Use the reckless attack button and hit to continue your rage! You can resist death " + resistDeath + " times!")
                .addComponents(ActionRow.of(
                        Button.danger("braget10d" + resistDeath, "Reckless attack!")
                ))
                .respond();
    }

    public static void RecklessAttack(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        String[] vals = buttonInteraction.getCustomId().split("t")[1].split("d");
        int turns = Integer.parseInt(vals[0]);
        int resists = Integer.parseInt(vals[1]);
        Message hit = Attack(buttonInteraction, 0, UserLevel(buttonInteraction.getUser()), true, resists > 0);
        System.out.println(turns + " / " + resists);
        if (hit != null) {
            if (hit.getContent().contains("Carl counter")) {
                if (resists > 0) {
                    resists--;
                    buttonInteraction.getUser().removeRole(downedRole).join();
                    fightChannel.sendMessage(buttonInteraction.getUser().getMentionTag() + " resists death in their rage! (" + resists + " death resists remain!)").join();
                } else {
                    fightChannel.sendMessage(buttonInteraction.getUser().getMentionTag() + "'s rage ends as they fall to the ground!");
                    source.delete();
                    return;
                }
            }
            turns -= hit.getContent().contains("attempts to attack Carl") ? 2 : 1;
            if (turns > 0) {
                buttonInteraction.createFollowupMessageBuilder()
                        .setContent(buttonInteraction.getUser().getMentionTag() + ": Your rage continues for " + turns + " more turns!")
                        .addComponents(ActionRow.of(
                                Button.danger("braget" + turns + "d" + resists, "Reckless attack!")
                        ))
                        .send();
            } else {
                buttonInteraction.createFollowupMessageBuilder()
                        .setContent(buttonInteraction.getUser().getMentionTag() + ": Your rage ends and you return to attacking normally!")
                        .send();
            }
            source.createUpdater().removeAllComponents().applyChanges();
        }
    }

    public static void AddInspiration(int level, SlashCommandInteraction slashCommandInteraction) {
        if (level < 3) {
            bardInspires[0]++;
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + " has added a 1d6 inspiration to the pool!")
                    .respond();
        } else if (level < 5) {
            bardInspires[1]++;
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + " has added a 1d8 inspiration to the pool!")
                    .respond();
        } else {
            bardInspires[2]++;
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + " has added a 1d10 inspiration to the pool!")
                    .respond();
        }
    }

    public static void RevivePlayers(int level, SlashCommandInteraction slashCommandInteraction) {
        int totalToRevive = (int)Math.ceil(level / 2.0);
        Collection<User> downed = downedRole.getUsers();
        List<SelectMenuOption> options = new ArrayList<>();
        options.add(SelectMenuOption.create("None", "None"));
        for (User down : downed) {
            options.add(SelectMenuOption.create(down.getDisplayName(Main.activeServer), down.getIdAsString()));
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Select " + (totalToRevive == 1 ? "a user " : totalToRevive + " users ") +
                         "to revive!")
                .addComponents(ActionRow.of(
                        SelectMenu.create("reviveselection", "Revive selection...", 1, totalToRevive, options)
                ))
                .respond();
    }

    public static void PerformRevive(SelectMenuInteraction selectMenuInteraction) {
        Message source = selectMenuInteraction.getMessage();
        if (!source.getContent().contains(selectMenuInteraction.getUser().getMentionTag())) {
            selectMenuInteraction.createImmediateResponder()
                    .setContent(selectMenuInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        String revivify = "";
        List<SelectMenuOption> options = selectMenuInteraction.getChosenOptions();
        for (int i = 0; i < options.size(); i++) {
            try {
                SelectMenuOption selection = options.get(i);
                User user = Main.api.getUserById(selection.getValue()).get();
                user.removeRole(downedRole);
                if (!revivify.equals("")) {
                    if (selectMenuInteraction.getChosenOptions().size() == 2) {
                        revivify += " and ";
                    } else {
                        if (i < options.size() - 1) {
                            revivify += ", ";
                        } else {
                            revivify += ", and ";
                        }
                    }
                }
                revivify += user.getMentionTag();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        selectMenuInteraction.createImmediateResponder()
                .setContent(selectMenuInteraction.getUser().getMentionTag() + " channels divine magic and revives " + revivify + "!")
                .respond();
        source.delete();
    }

    public static void WildShape(int level, SlashCommandInteraction slashCommandInteraction) {
        int shapeTurns = 2 + (int)Math.ceil(level / 2.0);
        ActionRow shapes = null;
        if (level < 3) {
            shapes = ActionRow.of(Button.primary("nshapewolft" + shapeTurns, "Wolf"));
        } else if (level < 5) {
            shapes = ActionRow.of(
                    Button.primary("nshapewolft" + shapeTurns, "Wolf"),
                    Button.success("nshapecroct" + shapeTurns, "Crocodile")
            );
        } else {
            shapes = ActionRow.of(
                    Button.primary("nshapewolft" + shapeTurns, "Wolf"),
                    Button.success("nshapecrocodilet" + shapeTurns, "Crocodile"),
                    Button.danger("nshapeeaglet" + shapeTurns, "Eagle")
            );
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Choose your wildshape! You gain the following benefits for " + shapeTurns + " turns based on the shape you pick:" +
                        "\n- Wolves get a bonus to hit for every other wildshaped wolf, up to a max of **+15**, and deals 2d6 + mod on hit!" +
                        "\n- Crocodiles make one attack with advantage which deals 2d10 + mod and restrains Carl on hit!" +
                        "\n- Eagles make two attacks which both deal 2d6 + mod, but you can't be downed!" +
                        "\n*While wildshaped, you cannot attack normally!*")
                .addComponents(shapes)
                .respond();
    }

    public static void WildShapeTurn(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        String[] split = buttonInteraction.getCustomId().split("t");
        String shape = split[0];
        int turns = Integer.parseInt(split[1]);
        if (buttonInteraction.getCustomId().startsWith("nshape")) {
            if (!wildShapedUsers.contains(buttonInteraction.getUser())) {
                wildShapedUsers.add(buttonInteraction.getUser());
            }
            String message = buttonInteraction.getUser().getMentionTag() + " transforms into a ";
            switch (shape) {
                case "nshapewolf":
                    shape = "wolf";
                    wolfCount++;
                    break;
                case "nshapecrocodile":
                    shape = "crocodile";
                    break;
                case "nshapeeagle":
                    shape = "eagle";
                    break;
            }
            message += shape + " for " + turns + " turns!";
            buttonInteraction.createImmediateResponder()
                    .setContent(message)
                    .addComponents(ActionRow.of(
                            Button.danger("shape" + shape + "t" + turns, "Attack!"),
                            Button.primary("return", "End early")
                    ))
                    .respond();
            source.delete();
        } else {
            turns--;
            String message = buttonInteraction.getUser().getMentionTag();
            int userLevel = UserLevel(buttonInteraction.getUser());
            int toHit = userLevel + (userLevel > 3 ? 4 : 3);
            boolean downed = false;
            int startingSkips = skipCounterAttacks;
            if (!shape.contains("eagle")) {
                if (skipCounterAttacks == 0) {
                    downed = CarlCounter(buttonInteraction.getUser());
                } else {
                    skipCounterAttacks--;
                    if (skipCounterAttacks < 0) {
                        skipCounterAttacks = 0;
                    }
                }
            }
            System.out.println(shape);
            switch (shape) {
                case "shapewolf":
                    if (RollD20(toHit + Math.min(15, wolfCount)) > GetCarlAC()) {
                        int damage = 2 + Main.rand.nextInt(6) + Main.rand.nextInt(6) + (userLevel > 3 ? 4 : 3);
                        message += " bites Carl and deals **" + DamageCarl(damage) + "** damage!";
                    } else {
                        message += " gnashes at Carl but misses!";
                    }
                    if (startingSkips == 0) {
                        if (downed) {
                            message += "\nCarl bites back and downs " + buttonInteraction.getUser().getMentionTag() + ", ending their wildshape.";
                        } else {
                            message += "\nCarl misses with his counter-attack!";
                        }
                    }
                    if (downed || turns == 0) {
                        buttonInteraction.createImmediateResponder()
                                .setContent(message + (turns == 0 ? "\n" + buttonInteraction.getUser().getMentionTag() + ": Your wildshape ends!" : ""))
                                .respond();
                        wildShapedUsers.remove(buttonInteraction.getUser());
                    } else {
                        buttonInteraction.createImmediateResponder()
                                .setContent(message)
                                .addComponents(ActionRow.of(
                                        Button.danger(shape + "t" + turns, "Attack!"),
                                        Button.primary("return", "End early")
                                ))
                                .respond();
                    }
                    break;
                case "shapecrocodile":
                    if (RollD20(toHit) > GetCarlAC() || RollD20(toHit) > GetCarlAC()) {
                        int damage = 2 + Main.rand.nextInt(10) + Main.rand.nextInt(10) + (userLevel > 3 ? 4 : 3);
                        message += " chomps down on Carl and deals **" + DamageCarl(damage) + "** damage! Carl is temporarily restrained!";
                        skipCounterAttacks = startingSkips + 1;
                        downed = false;
                    } else {
                        message += " gnashes at Carl but misses!";
                        if (startingSkips == 0) {
                            if (downed) {
                                message += "\nCarl returns the blow and downs " + buttonInteraction.getUser().getMentionTag() + ", ending their wildshape.";
                            } else {
                                message += "\nCarl misses with his counter-attack!";
                            }
                        }
                    }
                    if (downed || turns == 0) {
                        buttonInteraction.createImmediateResponder()
                                .setContent(message + (turns == 0 ? "\n" + buttonInteraction.getUser().getMentionTag() + ": Your wildshape ends!" : ""))
                                .respond();
                        wildShapedUsers.remove(buttonInteraction.getUser());
                    } else {
                        buttonInteraction.createImmediateResponder()
                                .setContent(message)
                                .addComponents(ActionRow.of(
                                        Button.danger(shape + "t" + turns, "Attack!"),
                                        Button.primary("return", "End early")
                                ))
                                .respond();
                    }
                    break;
                case "shapeeagle":
                    if (RollD20(toHit) > GetCarlAC()) {
                        message += " divebombs Carl, pecking for **" + DamageCarl(2 + Main.rand.nextInt(6) + Main.rand.nextInt(6) + (userLevel > 3 ? 4 : 3)) + "** damage and";
                    } else {
                        message += " divebombs Carl, missing their beak attack and";
                    }
                    if (RollD20(toHit) > GetCarlAC()) {
                        message += " slashing with their talons for **" + DamageCarl(2 + Main.rand.nextInt(6) + Main.rand.nextInt(6) + (userLevel > 3 ? 4 : 3)) + "** damage!";
                    } else {
                        message += " failing to slice his armor with their talons!";
                    }
                    if (turns == 0) {
                        buttonInteraction.createImmediateResponder()
                                .setContent(message + (turns == 0 ? "\n" + buttonInteraction.getUser().getMentionTag() + ": Your wildshape ends!" : ""))
                                .respond();
                        wildShapedUsers.remove(buttonInteraction.getUser());
                    } else {
                        buttonInteraction.createImmediateResponder()
                                .setContent(message)
                                .addComponents(ActionRow.of(
                                        Button.danger(shape + "t" + turns, "Attack!"),
                                        Button.primary("return", "End early")
                                ))
                                .respond();
                    }
                    break;
            }
            source.createUpdater().removeAllComponents().applyChanges();
        }
    }

    public static void EndEarly(ButtonInteraction buttonInteraction) {
        wildShapedUsers.remove(buttonInteraction.getUser());
        buttonInteraction.createImmediateResponder()
                .setContent(buttonInteraction.getUser().getMentionTag() + " ends their wildshape early!")
                .respond();
        buttonInteraction.getMessage().createUpdater().removeAllComponents().applyChanges();
    }

    public static void Maneuver(int level, SlashCommandInteraction slashCommandInteraction) {
        ActionRow maneuvers = null;
        if (level < 3) {
            maneuvers = ActionRow.of(Button.danger("manuprecise", "Precision attack"));
        } else if (level < 5) {
            maneuvers = ActionRow.of(
                    Button.danger("manuprecise", "Precision attack"),
                    Button.success("manudisarm", "Disarming attack")
            );
        } else {
            maneuvers = ActionRow.of(
                    Button.danger("manuprecise", "Precision attack"),
                    Button.success("manudisarm", "Disarming attack"),
                    Button.primary("manuriposte", "Riposte")
            );
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Pick a maneuver to add to your attack!")
                .addComponents(maneuvers)
                .respond();
    }

    public static void DoManeuver(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        switch (buttonInteraction.getCustomId()) {
            case "manuprecise":
                Attack(buttonInteraction, Main.rand.nextInt(8) + 1, 0, false, false);
                break;
            case "manudisarm":
                Message disarm = Attack(buttonInteraction, 0, Main.rand.nextInt(8) + 1, false, false);
                if (disarm != null) {
                    skipCounterAttacks++;
                }
                break;
            case "manuriposte":
                Message riposte = Attack(buttonInteraction);
                if (riposte != null) {
                    if (riposte.getContent().contains("misses with his")) {
                        int skippable = skipCounterAttacks;
                        skipCounterAttacks = 1;
                        String riposteMessage = "Riposte! " + RollAttackAndDamage(buttonInteraction.getUser(), "Class Fighter", UserLevel(buttonInteraction.getUser()), 0, 0, Main.rand.nextInt(8) + 1, false, false);
                        fightChannel.sendMessage(riposteMessage);
                        skipCounterAttacks = skippable;
                    } else {
                        fightChannel.sendMessage(buttonInteraction.getUser().getMentionTag() + " fails to riposte!");
                    }
                }
                break;
        }
        source.delete();
    }

    public static void KiPoints(int level, SlashCommandInteraction slashCommandInteraction) {
        ActionRow kiOptions = null;
        if (level < 3) {
            kiOptions = ActionRow.of(Button.danger("monkflurry", "Flurry of blows"));
        } else if (level < 5) {
            kiOptions = ActionRow.of(
                    Button.danger("monkflurry", "Flurry of blows"),
                    Button.success("monkdodge", "Patient defense")
            );
        } else {
            kiOptions = ActionRow.of(
                    Button.danger("monkflurry", "Flurry of blows"),
                    Button.success("monkdodge", "Patient defense"),
                    Button.primary("monkstun", "Stunning blow")
            );
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": You channel your inner ki, how will you use it?")
                .addComponents(kiOptions)
                .respond();
    }

    public static void UseKiAbility(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        int level = UserLevel(buttonInteraction.getUser());
        String message = "";
        switch (buttonInteraction.getCustomId()) {
            case "monkflurry":
                int unarmedSize = 2 * (1 + (int)Math.ceil(level / 2.0));
                skipCounterAttacks++;
                message = RollAttackAndDamage(buttonInteraction.getUser(), "Class Monk", level, 0, 0, 0, false, false) +
                        "\nThis attack is followed by a flurry of blows!";
                message += RollD20(level + (level > 3 ? 4 : 3)) >= GetCarlAC() ? "\nThe first strike lands for **" + DamageCarl(1 + Main.rand.nextInt(unarmedSize) + (level > 3 ? 4 : 3)) + "** damage!" :
                        "\nThe first strike misses!";
                message += RollD20(level + (level > 3 ? 4 : 3)) >= GetCarlAC() ? "\nThe second strike lands for **" + DamageCarl(1 + Main.rand.nextInt(unarmedSize) + (level > 3 ? 4 : 3)) + "** damage!" :
                        "\nThe second strike misses!";
                boolean downed = CarlCounter(buttonInteraction.getUser());
                if (downed) {
                    buttonInteraction.getUser().addRole(downedRole);
                    message += "\nFollowing the flurry, Carl knocks out " + buttonInteraction.getUser().getMentionTag() + "!";
                }
                buttonInteraction.createImmediateResponder()
                        .setContent(message)
                        .respond();
                break;
            case "monkdodge":
                skipCounterAttacks++;
                message = RollAttackAndDamage(buttonInteraction.getUser(), "Class Monk", level, 0, 0, 0, false, false);
                buttonInteraction.createImmediateResponder()
                        .setContent(message + "\n" + buttonInteraction.getUser().getMentionTag() + " waits patiently to dodge Carl's next strike and does!")
                        .respond();
                break;
            case "monkstun":
                skipCounterAttacks += 6;
                message = RollAttackAndDamage(buttonInteraction.getUser(), "Class Monk", level, 0, 0, 0, false, false);
                buttonInteraction.createImmediateResponder()
                        .setContent(message + "\n" + buttonInteraction.getUser().getMentionTag() + " stuns Carl, preventing him from counter-attacking for the next five attacks!")
                        .respond();
                break;
        }
        source.delete();
    }

    public static void Paladin(int level, SlashCommandInteraction slashCommandInteraction) {
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Smite or lay on hands?")
                .addComponents(ActionRow.of(
                        Button.danger("smite", "Smite!"),
                        Button.success("layhands", "Lay on hands")
                ))
                .respond();
    }

    public static void LayOnHands(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        buttonInteraction.createImmediateResponder()
                        .setContent(buttonInteraction.getUser().getMentionTag() + " heals " + ReviveRandom().getMentionTag() + " with their pool of healing light!")
                                .respond();
        source.delete();
    }

    public static User ReviveRandom() {
        List<User> downed = new ArrayList<>(downedRole.getUsers());
        if (downed.size() > 0) {
            User heal = downed.get(Main.rand.nextInt(downed.size()));
            heal.removeRole(downedRole);
            return heal;
        }
        return Main.api.getYourself();
    }

    public static void Smite(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        int userLevel = UserLevel(buttonInteraction.getUser());
        int d8s = 4 + (int)Math.ceil(userLevel / 2.0);
        int d6s = -1 + (int)Math.ceil(userLevel / 2.0);
        int toHit = userLevel + (userLevel > 3 ? 4 : 3);
        if (RollD20(toHit) > GetCarlAC()) {
            int damage = 0;
            for (int i = 0; i < d8s; i++) {
                damage += Main.rand.nextInt(8) + 1;
                if (i < d6s) {
                    damage += Main.rand.nextInt(6) + 1;
                }
            }
            DamageCarl(damage);
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " delivers divine justice, dealing **" + damage + "** damage to Carl!")
                    .respond();
            CarlCounter(buttonInteraction.getUser());
        } else {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " swings with holy strength, but misses Carl by an inch!")
                    .respond();
        }
        source.delete();
    }

    public static void Companion(int level, SlashCommandInteraction slashCommandInteraction) {
        ActionRow companionOptions = null;
        if (level < 3) {
            companionOptions = ActionRow.of(Button.danger("comppack", "\"Join the pack!\""));
        } else if (level < 5) {
            companionOptions = ActionRow.of(
                    Button.danger("comppack", "\"Join the pack!\""),
                    Button.success("comphelp", "\"Give us some help!\"")
                    );
        } else {
            companionOptions = ActionRow.of(
                    Button.danger("comppack", "\"Join the pack!\""),
                    Button.success("comphelp", "\"Give us some help!\""),
                    Button.primary("compguard", "\"Don't let him get us!\"")
            );
        }
        if (companionPack.contains(slashCommandInteraction.getUser())) {
            companionPack.remove(slashCommandInteraction.getUser());
            wolfCount--;
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Your animal companion looks to you for a command!")
                .addComponents(companionOptions)
                .respond();
    }

    public static void CompanionAbility(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        switch (buttonInteraction.getCustomId()) {
            case "comppack":
                if (!companionPack.contains(buttonInteraction.getUser())) {
                    companionPack.add(buttonInteraction.getUser());
                    wolfCount++;
                }
                buttonInteraction.createImmediateResponder()
                        .setContent(buttonInteraction.getUser().getMentionTag() + "'s animal companion rushes Carl for **" + DamageCarl(2 + Main.rand.nextInt(6) + Main.rand.nextInt(6)) + "** damage and joins the pack!")
                        .respond();
                break;
            case "comphelp":
                if (downedRole.hasUser(buttonInteraction.getUser())) {
                    buttonInteraction.getUser().removeRole(downedRole);
                    buttonInteraction.createImmediateResponder()
                            .setContent(buttonInteraction.getUser().getMentionTag() + ": Your companion helps you up! You're no longer downed!")
                            .respond();
                } else {
                    buttonInteraction.createImmediateResponder()
                            .setContent(buttonInteraction.getUser().getMentionTag() + ": Your companion rushes to help " + ReviveRandom().getMentionTag() + " up from the ground!")
                            .respond();
                }
                break;
            case "compguard":
                if (!companionPack.contains(buttonInteraction.getUser())) {
                    companionPack.add(buttonInteraction.getUser());
                    wolfCount++;
                }
                buttonInteraction.createImmediateResponder()
                        .setContent(buttonInteraction.getUser().getMentionTag() + ": Your companion puts itself between Carl and your allies! He can't down anyone for another attack!")
                        .respond();
                skipCounterAttacks++;
                break;
        }
        source.delete();
    }

    public static void Sneak(int level, SlashCommandInteraction slashCommandInteraction) {
        int hideBonus = 2 + (int)Math.ceil(level / 2.0);
        if (RollD20(hideBonus) >= carlStats.get("hideDC")) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + ": You hide successfully! Now is your time to strike!")
                    .addComponents(ActionRow.of(Button.danger("sneakattack" + (int)Math.ceil(level / 2), "Make your attack!")))
                    .respond();
        } else {
            slashCommandInteraction.createImmediateResponder()
                    .setContent(slashCommandInteraction.getUser().getMentionTag() + " tries to hide for a sneak attack but gets spotted by Carl!")
                    .respond();
        }
    }

    public static void PerformSneakAttack(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        int d6s = 1;
        switch (buttonInteraction.getCustomId()) {
            case "sneakattack2":
                d6s = 2;
                break;
            case "sneakattack3":
                d6s = 4;
                break;
            default:
                break;
        }
        int userLevel = UserLevel(buttonInteraction.getUser());
        int toHit = userLevel + (userLevel > 3 ? 4 : 3);
        if (RollD20(toHit) >= GetCarlAC() || RollD20(toHit) >= GetCarlAC()) {
            int damage = Main.rand.nextInt(8) + 1;
            for (int i = 0; i < d6s; i++) {
                damage += Main.rand.nextInt(6) + 1;
            }
            damage *= 2;
            damage += 1 + (userLevel > 3 ? 4 : 3);
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() +
                            (Main.rand.nextBoolean() ? " leaps in from the shadows and stabs Carl for **" : " fires an arrow from far away and pierces Carl for **") +
                            DamageCarl(damage) + "** damage!")
                    .respond();
        } else {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() +
                            (Main.rand.nextBoolean() ? " leaps in from the shadows and " : " fires an arrow from far away and ") + "misses Carl!")
                    .respond();
        }
        source.delete();
    }

    public static void Metamagic(int level, SlashCommandInteraction slashCommandInteraction) {
        ActionRow metamagics = null;
        if (level < 3) {
            metamagics = ActionRow.of(Button.danger("metasubtle", "Subtle spell"));
        } else if (level < 5) {
            metamagics = ActionRow.of(
                    Button.danger("metasubtle", "Subtle spell"),
                    Button.success("metaheight", "Heightened spell")
            );
        } else {
            metamagics = ActionRow.of(
                    Button.danger("metasubtle", "Subtle spell"),
                    Button.primary("metaheight", "Heightened spell"),
                    Button.success("metaquick", "Quickened spell")
            );
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Pick a metamagic to add to your spell!")
                .addComponents(metamagics)
                .respond();
        return;
    }

    public static void CastMeta(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        }
        int userLevel = UserLevel(buttonInteraction.getUser());
        switch (buttonInteraction.getCustomId()) {
            case "metasubtle":
                skipCounterAttacks++;
                buttonInteraction.createImmediateResponder()
                        .setContent(RollAttackAndDamage(buttonInteraction.getUser(), "Class Sorcerer", UserLevel(buttonInteraction.getUser()), 1, 0, 0, false, false) +
                                "\nThis spell was cast too subtly for Carl to counter-attack!")
                        .respond();
                break;
            case "metaheight":
                int DC = 9 + userLevel + (userLevel > 3 ? 4 : 3);
                int carlSave = RollD20(carlStats.get("saveMod"));
                int damage = 0;
                for (int i = 0; i < 9; i++) {
                    damage += 1 + Main.rand.nextInt(6);
                }
                damage /= carlSave >= DC ? 2 : 1;
                buttonInteraction.createImmediateResponder()
                        .setContent(buttonInteraction.getUser().getMentionTag() + " sends a lightning bolt streaking through Carl for **" + damage + "** damage!")
                        .respond();
                break;
            case "metaquick":
                skipCounterAttacks++;
                buttonInteraction.createImmediateResponder()
                        .setContent(RollAttackAndDamage(buttonInteraction.getUser(), "Class Sorcerer", UserLevel(buttonInteraction.getUser()), 1, 0, 0, false, false) +
                                "\nThey quickly follow up with another spell!\n" +
                                RollAttackAndDamage(buttonInteraction.getUser(), "Class Sorcerer", UserLevel(buttonInteraction.getUser()), 1, 0, 0, false, false))
                        .respond();
                break;
        }
        source.delete();
    }

    public static void AddHex(int level, SlashCommandInteraction slashCommandInteraction) {
        int duration = 5 * (int)Math.ceil(level / 2.0);
        carlHexRemaining += duration;
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + " has added a hex for **1d6** extra necrotic per hit that'll last for " + duration + " attacks!")
                .respond();
    }

    public static void WizardSpell(int level, SlashCommandInteraction slashCommandInteraction) {
        ActionRow spells = null;
        if (level < 3) {
            spells = ActionRow.of(Button.primary("wizardmm3", "Magic Missile"));
        } else if (level < 5) {
            spells = ActionRow.of(
                    Button.primary("wizardmm4", "Magic Missile"),
                    Button.danger("wizardfire", "Fireball")
            );
        } else {
            spells = ActionRow.of(
                    Button.primary("wizardmm5", "Magic Missile"),
                    Button.danger("wizardfire", "Fireball"),
                    Button.success("wizardfod", "Finger of Death")
            );
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent(slashCommandInteraction.getUser().getMentionTag() + ": Pick a wizard spell!")
                .addComponents(spells)
                .respond();
    }

    public static void CastWizard(ButtonInteraction buttonInteraction) {
        Message source = buttonInteraction.getMessage();
        int userLevel = UserLevel(buttonInteraction.getUser());
        boolean carlSave = RollD20(carlStats.get("saveMod")) >= 9 + userLevel + (userLevel > 3 ? 4 : 3);
        int damage = 0;
        if (!source.getContent().contains(buttonInteraction.getUser().getMentionTag())) {
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " you can't use another player's class powers!")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
            return;
        }
        if (buttonInteraction.getCustomId().contains("mm")) {
            int missiles = Integer.parseInt(buttonInteraction.getCustomId().split("mm")[1]);
            for (int i = 0; i < missiles; i++) {
                damage += 2 + Main.rand.nextInt(4);
            }
            buttonInteraction.createImmediateResponder()
                    .setContent(buttonInteraction.getUser().getMentionTag() + " sends " + missiles + " *magic missiles* flying at Carl for **" + DamageCarl(damage) + "** damage!")
                    .respond();
        } else {
            if (buttonInteraction.getCustomId().contains("fire")) {
                List<User> inRoom = new ArrayList<>();
                for (int i = 0; i < levelRoles.length; i++) {
                    System.out.println(levelRoles[i].getUsers().size());
                    inRoom.addAll(levelRoles[i].getUsers());
                }
                List<User> collateral = new ArrayList<>();
                for (int u = 0; u < userLevel; u++) {
                    if (inRoom.size() == 0) {
                        break;
                    }
                    int randUser = Main.rand.nextInt(inRoom.size());
                    collateral.add(inRoom.get(randUser));
                    inRoom.remove(randUser);
                }
                if (!collateral.contains(buttonInteraction.getUser()) && Main.rand.nextBoolean() && Main.rand.nextBoolean() && Main.rand.nextBoolean()) {
                    collateral.remove(Main.rand.nextInt(collateral.size()));
                    collateral.add(buttonInteraction.getUser());
                }
                for (int i = 0; i < 6 + userLevel; i++) {
                    damage += 1 + Main.rand.nextInt(6);
                }
                damage /= carlSave ? 2 : 1;
                String message = buttonInteraction.getUser().getMentionTag() + " creates a massive *fireball* explosion, dealing **" + DamageCarl(damage) + "** damage to Carl, but also downs ";
                for (int u = 0; u < collateral.size(); u++) {
                    collateral.get(u).addRole(downedRole);
                    message += collateral.get(u).getMentionTag() + ", ";
                    if (u == collateral.size() - 2) {
                        message += "and ";
                    }
                }
                message += " in the process!" + (carlSave ? "\n*(Carl saved for half damage!)*" : "");
                buttonInteraction.createImmediateResponder()
                        .setContent(message)
                        .respond();
            } else {
                damage = 30;
                for (int i = 0; i < 7; i++) {
                    damage += Main.rand.nextInt(8) + 1;
                }
                damage /= carlSave ? 2 : 1;
                buttonInteraction.createImmediateResponder()
                        .setContent(buttonInteraction.getUser().getMentionTag() + " uses *finger of death* on Carl, dealing a devastating **" + DamageCarl(damage) + "** damage to him!" +
                                (carlSave ? "\n*(Carl saved for half damage!)*" : ""))
                        .respond();
            }
        }
        source.delete();
    }

    public static SlashCommandBuilder CarlStatsBuilder() {
        return SlashCommand.with("carlstats", "Increase, lower, or set Carl's stats.", Arrays.asList(
                SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "stat", "Which stat to change. Check only prints stats.", true, Arrays.asList(
                        SlashCommandOptionChoice.create("check", "check"),
                        SlashCommandOptionChoice.create("HP", "HP"),
                        SlashCommandOptionChoice.create("maxHP", "maxHP"),
                        SlashCommandOptionChoice.create("AC", "AC"),
                        SlashCommandOptionChoice.create("minAC", "minAC"),
                        SlashCommandOptionChoice.create("attackMod", "attackMod"),
                        SlashCommandOptionChoice.create("minAttackMod", "minAttackMod"),
                        SlashCommandOptionChoice.create("saveMod", "saveMod"),
                        SlashCommandOptionChoice.create("hideDC", "hideDC")
                )),
                SlashCommandOption.create(SlashCommandOptionType.INTEGER, "value", "The value to set the stat to", true)
        )).setDefaultPermission(false);
    }

    public static void SetStat(SlashCommandInteraction slashCommandInteraction) {
        String key = slashCommandInteraction.getOptionStringValueByName("stat").orElse("");
        if (!key.equalsIgnoreCase("check")) {
            if (key.length() == 0 || !carlStats.containsKey(key)) {
                slashCommandInteraction.createImmediateResponder()
                        .setContent("The selected stat doesn't exist!")
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }
            carlStats.replace(key, slashCommandInteraction.getOptionIntValueByName("value").get());
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Set the selected stat! You can find output in " + Main.sandbox.getMentionTag())
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        } else {
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Printing stats to " + Main.sandbox.getMentionTag())
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        }
        String message = "**Carl stats:**";
        for (String stat : carlStats.keySet()) {
            message += "\n" + stat + " = " + carlStats.get(stat);
        }
        Main.sandbox.sendMessage(message);
        PrintCarlBar();
    }

    public static SlashCommandBuilder ZombieWaveBuilder() {
        return SlashCommand.with("zombiewave", "Send a wave of zombies to a channel!", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "Where to send the wave to.", true),
                SlashCommandOption.create(SlashCommandOptionType.INTEGER, "size", "How large of a wave to send.", true)
        )).setDefaultPermission(false);
    }

    public static void ZombieWave(SlashCommandInteraction slashCommandInteraction) {
        ServerTextChannel toBlock = (ServerTextChannel) slashCommandInteraction.getOptionChannelValueByName("channel").orElse(null);
        if (toBlock == null) {
            slashCommandInteraction.createImmediateResponder()
                    .setContent("Couldn't send zombies to " + slashCommandInteraction.getOptionChannelValueByName("channel") + ".")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        }
        slashCommandInteraction.createImmediateResponder()
                .setContent("Attempting to send a wave of " + slashCommandInteraction.getOptionIntValueByName("size").get() + " zombies to " + toBlock.getMentionTag() + ".")
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
        try {
            if (!blockedChannels.contains(toBlock)) {
                blockedChannels.add(toBlock);
            }
            toBlock.sendMessage(ZombieMessage(slashCommandInteraction.getOptionIntValueByName("size").get(), 0)).whenComplete((message, throwable) -> {
                message.addReaction("\u2694");
                message.pin();
            });
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String ZombieMessage(int live, int dead) {
        String[] zombieEmojis = {
                ":zombie:",
                ":man_zombie:",
                ":woman_zombie:"
        };
        String toReturn = ":crossed_swords: **" + live + " ZOMBIES DESCEND UPON THE CHANNEL!** :crossed_swords:\nReact with :crossed_swords: to do your part and kill a zombie!\n";
        int lineLength = 0;
        List<Integer> headstones = new ArrayList<>();
        List<Integer> availableNums = new ArrayList<>();
        for (int i = 0; i < live; i++) {
            availableNums.add(i);
        }
        for (int i = 0; i < dead; i++) {
            int select = Main.rand.nextInt(availableNums.size());
            headstones.add(availableNums.get(select));
            availableNums.remove(select);
        }
        for (int i = 0; i < live; i++) {
            toReturn += (headstones.contains(Integer.valueOf(i)) ? ":headstone:" : zombieEmojis[Main.rand.nextInt(zombieEmojis.length)]) + " ";
            lineLength++;
            if (lineLength == 15) {
                lineLength = 0;
                toReturn += "\n";
            }
        }
        if (!toReturn.endsWith("\n")) {
            toReturn += "\n";
        }
        toReturn += "*(If your reaction gets removed, that means you need to `/chooseclass`!)*";
        return toReturn;
    }

    public static void ZombieReaction(User user, Emoji emoji, Message message, boolean added) {
        if (added) {
            int userLevel = UserLevel(user);
            if (userLevel == 0) {
                message.removeReactionsByEmoji(user, emoji);
                return;
            }
            int startingNum = Integer.parseInt(message.getContent().replaceAll("[^0-9]", ""));
            int dead = message.getReactionByEmoji("\u2694").get().getCount() - 1;
            if (startingNum == dead) {
                ZombiesDefeated(message);
            } else {
                message.edit(ZombieMessage(startingNum, dead));
            }
        } else {
            int startingNum = Integer.parseInt(message.getContent().replaceAll("[^0-9]", ""));
            int dead = 0;
            try {
                dead = message.getReactionByEmoji("\u2694").get().getCount() - 1;
            } catch (Exception e) {
                //no reactions
            }
            message.edit(ZombieMessage(startingNum, dead));
        }
    }

    public static void ZombiesDefeated(Message message) {
        try {
            Reaction attack = message.getReactionByEmoji("\u2694").get();
            Role halfLevel = Main.activeServer.getRolesByName("Half-level").get(0);
            fightChannel.sendMessage("Zombie wave from " + message.getServerTextChannel().get().getMentionTag() + " has been defeated!");
            blockedChannels.remove(message.getServerTextChannel().get());
            for (User helper : attack.getUsers().get()) {
                User servHelper = Main.activeServer.requestMember(helper).join();
                if (helper.isYourself()) {
                    continue;
                }
                int userLevel = UserLevel(servHelper);
                if (servHelper.getRoles(Main.activeServer).contains(halfLevel)) {
                    servHelper.removeRole(halfLevel);
                    servHelper.removeRole(levelRoles[userLevel - 1]);
                    servHelper.addRole(levelRoles[userLevel]);
                    fightChannel.sendMessage(helper.getMentionTag() + " has leveled up to Level" + (userLevel + 1));
                } else {
                    if (userLevel < 5) {
                        helper.addRole(halfLevel);
                    }
                }
            }
            message.delete();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static SlashCommandBuilder SetAnswerBuilder() {
        return SlashCommand.with("setanswer", "Sets the answer for the daily riddle/puzzle.", Arrays.asList(
                SlashCommandOption.create(SlashCommandOptionType.STRING, "answer", "The answer as a string.", true),
                SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "Where the answer has to be submitted.", true)
        )).setDefaultPermission(false);
    }

    public static void SetAnswer(SlashCommandInteraction slashCommandInteraction) {
        currentAnswer = slashCommandInteraction.getOptionStringValueByName("answer").orElse("");
        answerChannel = (ServerTextChannel) slashCommandInteraction.getOptionChannelValueByName("channel").orElse(null);
        slashCommandInteraction.createImmediateResponder()
                .setContent("Set the answer and answer channel to \""  + currentAnswer + "\" and " + answerChannel.getMentionTag())
                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                .respond();
        Main.sandbox.sendMessage("Set the answer and answer channel to \""  + currentAnswer + "\" and " + answerChannel.getMentionTag());
    }
}
