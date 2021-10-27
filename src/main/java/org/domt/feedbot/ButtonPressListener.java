package org.domt.feedbot;

import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.listener.interaction.ButtonClickListener;

public class ButtonPressListener implements ButtonClickListener {
    @Override
    public void onButtonClick(ButtonClickEvent event) {
        ButtonInteraction buttonInteraction = event.getButtonInteraction();
        String buttonID = buttonInteraction.getCustomId();
        if (buttonID.contains("unsend")) {
            SuggestionCommands.UnsendMessage(buttonInteraction);
            return;
        }
        if (buttonID.contains("guess")) {
            CarlFightStuff.GuessRespond(buttonInteraction);
            return;
        }
        if (buttonID.contains("sabo")) {
            CarlFightStuff.SabotageButton(buttonInteraction);
            return;
        }
        if (buttonID.contains("brage")) {
            CarlFightStuff.RecklessAttack(buttonInteraction);
            return;
        }
        if (buttonID.contains("shape")) {
            CarlFightStuff.WildShapeTurn(buttonInteraction);
            return;
        }
        if (buttonID.contains("return")) {
            CarlFightStuff.EndEarly(buttonInteraction);
            return;
        }
        if (buttonID.contains("manu")) {
            CarlFightStuff.DoManeuver(buttonInteraction);
            return;
        }
        if (buttonID.contains("monk")) {
            CarlFightStuff.UseKiAbility(buttonInteraction);
            return;
        }
        if (buttonID.contains("smite")) {
            CarlFightStuff.Smite(buttonInteraction);
            return;
        }
        if (buttonID.contains("layhands")) {
            CarlFightStuff.LayOnHands(buttonInteraction);
            return;
        }
        if (buttonID.contains("comp")) {
            CarlFightStuff.CompanionAbility(buttonInteraction);
            return;
        }
        if (buttonID.contains("sneakattack")) {
            CarlFightStuff.PerformSneakAttack(buttonInteraction);
            return;
        }
        if (buttonID.contains("meta")) {
            CarlFightStuff.CastMeta(buttonInteraction);
            return;
        }
        if (buttonID.contains("wizard")) {
            CarlFightStuff.CastWizard(buttonInteraction);
            return;
        }
    }
}
