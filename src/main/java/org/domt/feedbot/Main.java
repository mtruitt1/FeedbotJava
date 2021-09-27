package org.domt.feedbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

public class Main {
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
        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();
    }
}
