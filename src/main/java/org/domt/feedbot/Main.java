package org.domt.feedbot;

import java.io.*;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

public class Main {
    public static SessionInfo botInfo = null;
    public static DiscordApi api;
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
        OnMessageReceived.stickies = botInfo.HashStickies();


        api = new DiscordApiBuilder().setToken(token).login().join();
        System.out.println("Feedbot is connected and initializing.");
        api.updateActivity("Initializing...");
        try {
            ApiCommandUpdater.RemoveSlashCommands(null);
            ApiCommandUpdater.AddAllNormalSlashCommands();
        } catch (Exception e) {
            System.out.println(e);
        }
        api.addSlashCommandCreateListener(new SlashCommandListener());
    }
}
