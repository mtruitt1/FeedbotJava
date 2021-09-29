package org.domt.feedbot;

import java.util.Hashtable;

public class SessionInfo {
    public long activeServerID;
    public long staffRoleID;
    public long sandboxID;
    public long suggestionLogID;
    public long lfgChannelID;
    public long promptMasterRoleID;
    public long promptChannelID;
    public long roleSelectChannelID;
    public long[] roleSelectMessages;
    public StickyMessage[] stickyMessages;
    public String[] prompt_roles = {
            "Achiever",
            "Anthropologist",
            "Arcanist",
            "Architect",
            "Artificer",
            "Chronicler",
            "Connoisseur",
            "Gourmet",
            "Pride",
            "Skald",
            "Survivor",
            "Tinker",
            "Virtuoso",
            "Zoologist"};

    public Hashtable<Long, String> HashStickies() {
        Hashtable<Long, String> stickies = new Hashtable<>();
        for (StickyMessage sticky : stickyMessages) {
            if (!stickies.containsKey(sticky.channelID)) {
                stickies.put(sticky.channelID, sticky.message);
            }
        }
        return stickies;
    }

    public class StickyMessage {
        public long channelID;
        public String message;
    }
}
