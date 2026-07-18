package com.techcartel.siliconvalley.controller;

import com.techcartel.siliconvalley.model.Dice;
import com.techcartel.siliconvalley.model.Market;
import com.techcartel.siliconvalley.model.Player;
import com.techcartel.siliconvalley.util.ResourceType;

import java.util.ArrayList;
import java.util.List;

public class GameController {

    private final Market market;
    private final Dice dice;
    private final List<Player> players;
    private int currentPlayerIndex;

    public GameController(List<Player> startingPlayers) {
        this.market = new Market();
        this.dice = new Dice();
        this.players = new ArrayList<>(startingPlayers);
        this.currentPlayerIndex = 0;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void endTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public int rollDice() {
        int roll = dice.roll();

        if (roll == 7)
            triggerRegulatoryCrisis();
        else
            distributeResources(roll);

        return roll;
    }

    private void triggerRegulatoryCrisis() {
        System.out.println("CRISIS! Stand ready for Auditor's arrival!!! Move the Auditor's token.");
    }

    private void distributeResources(int roll) {
        System.out.println("Sectors with the number " + roll + " are now producing resources.");
    }

    public boolean buyResource(ResourceType type) {
        if (type == ResourceType.CAPITAL || type == ResourceType.NONE)
            return false;

        Player activePlayer = getCurrentPlayer();
        int cost = market.getPrice(type);

        if (activePlayer.getResourceCount(ResourceType.CAPITAL) >= cost) {
            activePlayer.removeResource(ResourceType.CAPITAL, cost);
            activePlayer.addResource(type, 1);

            market.inflatePrice(type);

            return true;
        }
        return false;
    }

    public boolean sellResource(ResourceType type) {
        if (type == ResourceType.CAPITAL || type == ResourceType.NONE)
            return false;

        Player activePlayer = getCurrentPlayer();

        if (activePlayer.getResourceCount(type) > 0) {
            int payout = market.getPrice(type);
            activePlayer.removeResource(type, 1);
            activePlayer.addResource(ResourceType.CAPITAL, payout);

            market.crashPrice(type);

            return true;
        }
        return false;
    }

    public Player checkWinner() {
        int winningScore = 10;

        for (Player p : players) {
            if (p.getVictoryPoints() >= winningScore) {
                return p;
            }
        }
        return null;
    }

}
