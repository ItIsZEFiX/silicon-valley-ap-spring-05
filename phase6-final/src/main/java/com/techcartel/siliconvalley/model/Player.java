package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.FounderRole;
import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;
import com.techcartel.siliconvalley.util.AIType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final FounderRole role;
    private int capitalCount;
    private int talentCount;
    private int cloudCount;
    private int patentCount;
    private int dataCount;

    // Single source of truth for AI status
    private AIType aiType = AIType.HUMAN;

    private final List<CompanyStructure> structures;
    private boolean hasLongestNetwork;

    public Player(String name, FounderRole role) {
        this.name = name;
        this.role = role;
        this.capitalCount = 0;
        this.talentCount = 0;
        this.cloudCount = 0;
        this.patentCount = 0;
        this.dataCount = 0;
        this.structures = new ArrayList<>();
        this.hasLongestNetwork = false;

        applyRoleEffects();
    }

    public boolean isAIControlled() {
        return aiType != AIType.HUMAN;
    }

    public AIType getAIType() {
        return aiType;
    }

    public void setAIType(AIType aiType) {
        this.aiType = aiType;
    }

    private void applyRoleEffects() {
        if (role == FounderRole.THE_VC_FUNDED)
            capitalCount = GameConstants.VC_FUNDED_STARTING_CAPITAL;
    }

    public String getName() {
        return name;
    }

    public FounderRole getRole() {
        return role;
    }

    public int getVictoryPoints() {
        int points = 0;
        for (CompanyStructure s : structures) {
            points += s.getVictoryPoints();
        }
        if (role != FounderRole.NONE) {
            points -= GameConstants.ROLE_VICTORY_POINT_PENALTY;
        }
        if (hasLongestNetwork) {
            points += GameConstants.LONGEST_NETWORK_BONUS;
        }
        return points;
    }

    public void addStructure(CompanyStructure structure) {
        structures.add(structure);
    }

    public void removeStructure(CompanyStructure structure) {
        structures.remove(structure);
    }

    public List<CompanyStructure> getStructures() {
        return Collections.unmodifiableList(structures);
    }

    public boolean hasLongestNetwork() {
        return hasLongestNetwork;
    }

    public void setHasLongestNetwork(boolean value) {
        this.hasLongestNetwork = value;
    }

    // ---- Role-based helper effects ----

    /** The Hacker CEO buys from the market 1 capital cheaper per unit. */
    public int getMarketBuyDiscount() {
        return role == FounderRole.THE_HACKER_CEO ? GameConstants.HACKER_CEO_TRADE_DISCOUNT : 0;
    }

    /** The Tech Guru (CTO) upgrades to Unicorn using only 1 Cloud instead of 2. */
    public int getUnicornCloudCost() {
        return role == FounderRole.THE_TECH_GURU_CTO
                ? GameConstants.UNICORN_CLOUD_COST_TECH_GURU
                : GameConstants.UNICORN_CLOUD_COST;
    }

    /** The VC-Funded founder can hold up to 9 cards before a crisis tax instead of 7. */
    public int getCrisisCardLimit() {
        return role == FounderRole.THE_VC_FUNDED
                ? GameConstants.VC_FUNDED_CRISIS_CARD_LIMIT
                : GameConstants.STANDARD_CRISIS_CARD_LIMIT;
    }

    // ---- Resource management ----

    public int getResourceCount(ResourceType type) {
        return switch (type) {
            case CAPITAL -> capitalCount;
            case TALENT -> talentCount;
            case CLOUD -> cloudCount;
            case PATENT -> patentCount;
            case DATA -> dataCount;
            default -> 0;
        };
    }

    public int getTotalResourceCount() {
        return capitalCount + talentCount + cloudCount + patentCount + dataCount;
    }

    public void addResource(ResourceType type, int amount) {
        switch (type) {
            case CAPITAL: capitalCount += amount; break;
            case TALENT: talentCount += amount; break;
            case CLOUD: cloudCount += amount; break;
            case PATENT: patentCount += amount; break;
            case DATA: dataCount += amount; break;
        }
    }

    public void removeResource(ResourceType type, int amount) {
        switch (type) {
            case CAPITAL: capitalCount = Math.max(0, capitalCount - amount); break;
            case TALENT: talentCount = Math.max(0, talentCount - amount); break;
            case CLOUD: cloudCount = Math.max(0, cloudCount - amount); break;
            case PATENT: patentCount = Math.max(0, patentCount - amount); break;
            case DATA: dataCount = Math.max(0, dataCount - amount); break;
        }
    }

    public boolean canAfford(ResourceType type, int amount) {
        return getResourceCount(type) >= amount;
    }
}
