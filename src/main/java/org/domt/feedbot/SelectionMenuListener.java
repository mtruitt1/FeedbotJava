package org.domt.feedbot;

import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.interaction.SelectMenuInteraction;
import org.javacord.api.listener.interaction.SelectMenuChooseListener;

public class SelectionMenuListener implements SelectMenuChooseListener {
    @Override
    public void onSelectMenuChoose(SelectMenuChooseEvent event) {
        SelectMenuInteraction selectMenuInteraction = event.getSelectMenuInteraction();
        if (selectMenuInteraction.getCustomId().equals("classchoice")) {
            CarlFightStuff.ClassChosen(selectMenuInteraction);
            return;
        }
        if (selectMenuInteraction.getCustomId().equals("reviveselection")) {
            CarlFightStuff.PerformRevive(selectMenuInteraction);
            return;
        }
        if (selectMenuInteraction.getCustomId().equals("channelSelect")) {
            CarlFightStuff.FightChannelSelection(selectMenuInteraction);
            return;
        }
    }
}
