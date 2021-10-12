package org.domt.feedbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.HashMap;

public class CarlFightClassAttacks {
    public ClassList[] classes;

    public static class ClassList {
        public String className;
        public Level[] levels;

        public static class Level {
            public int level;
            public Attack[] attacks;

            public static class Attack {
                public String message;
                public String dice;
                public int extra;
                public boolean addMod;
            }
        }
    }

    public HashMap<String, ClassList.Level[]> classNameHashMap = new HashMap<>();

    public void ConvertToMap() {
        for (ClassList classList : classes) {
            classNameHashMap.put(classList.className, classList.levels);
        }
    }

    public static void GenerateJSON() {
        String[] classNames = {
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
        CarlFightClassAttacks classAttacks = new CarlFightClassAttacks();
        classAttacks.classes = new ClassList[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            classAttacks.classes[i] = new ClassList();
            classAttacks.classes[i].className = classNames[i];
            classAttacks.classes[i].levels = new ClassList.Level[5];
            for (int l = 0; l < 5; l++) {
                classAttacks.classes[i].levels[l] = new ClassList.Level();
                classAttacks.classes[i].levels[l].level = l + 1;
                classAttacks.classes[i].levels[l].attacks = new ClassList.Level.Attack[3];
                for (int a = 0; a < 3; a++) {
                    classAttacks.classes[i].levels[l].attacks[a] = new ClassList.Level.Attack();
                    classAttacks.classes[i].levels[l].attacks[a].message = "Test message";
                    classAttacks.classes[i].levels[l].attacks[a].dice = "1d8";
                    classAttacks.classes[i].levels[l].attacks[a].extra = 0;
                    classAttacks.classes[i].levels[l].attacks[a].addMod = false;
                }
            }
        }
        Gson gson = new GsonBuilder().setLenient().create();
        String json = gson.toJson(classAttacks);
        try {
            File f = new File("carl_fight.json");
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            System.out.println(e);
            return;
        }
    }

    //public static void main(String[] args) { GenerateJSON(); }
}
