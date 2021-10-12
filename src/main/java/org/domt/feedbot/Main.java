package org.domt.feedbot;

import java.io.*;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

public class Main {
    public static SessionInfo botInfo = null;
    public static DiscordApi api;
    public static Server activeServer;
    public static ServerTextChannel sandbox;
    public static Random rand;
    public static void main(String[] args) {
        String token = "";
        //Token is read from botkey.txt
        try {
            File f = new File("botkey.txt");
            FileReader fr = new FileReader(f);
            BufferedReader inStream = new BufferedReader(fr);
            token = inStream.readLine();
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return;
        }
        Gson gson = new GsonBuilder().setLenient().create();
        String json = "";
        try {
            File f = new File("session_info.json");
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    json += line;
                }
            }
            System.out.println(json);
            botInfo = gson.fromJson(json, SessionInfo.class);
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return;
        }
        if (botInfo == null) {
            System.out.println("Bot info couldn't be grabbed.");
            return;
        }


        api = new DiscordApiBuilder()
                .setToken(token)
                .setAllIntents()
                .setUserCacheEnabled(true)
                .login()
                .join();
        activeServer = Main.api.getServerById(Main.botInfo.activeServerID).orElse(null);
        if (activeServer == null) {
            return;
        }
        sandbox = (ServerTextChannel) activeServer.getChannelById(botInfo.sandboxID).orElse(null);
        if (sandbox == null) {
            return;
        }
        System.out.println("Feedbot is connected and initializing.");
        api.updateActivity("Initializing...");
        sandbox.sendMessage("Feedbot is starting up!\nAnother message will send when initialization finishes.");
        rand = new Random();
        TimerContainer.StartLFGPurger(60*60, 60*60*24*14); //60*60 == 1 hour, 60*60*24*14 == 2 weeks
        OnMessageReceived.stickies = botInfo.HashStickies();
        //CarlFightStuff.SetupFight();
        ApiCommandUpdater.UpdateAllSlashCommands(false);
        api.addMessageCreateListener(new OnMessageReceived());
        ColorRoleSelection.SetColorRoleEmojiTable();
        api.addReactionAddListener(new OnReactionAdded());
        api.addReactionRemoveListener(new OnReactionRemoved());
        api.addSlashCommandCreateListener(new SlashCommandListener());
        api.addButtonClickListener(new ButtonPressListener());
        api.addSelectMenuChooseListener(new SelectionMenuListener());
        System.out.println("Feedbot is finished initialization!");
        api.unsetActivity();
        sandbox.sendMessage("Feedbot is finished initialization!");
    }
}
