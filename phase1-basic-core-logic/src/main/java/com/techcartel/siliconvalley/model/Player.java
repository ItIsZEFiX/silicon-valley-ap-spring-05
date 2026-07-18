package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.FounderRole;
import com.techcartel.siliconvalley.util.ResourceType;


public class Player {

    private final String name;
    private final FounderRole role;
    private int victoryPoints;
    private int capitalCount;
    private int talentCount;
    private int cloudCount;
    private int patentCount;
    private int dataCount;

    public Player(String name, FounderRole role) {
        this.name = name;
        this.role = role;
        this.victoryPoints = 0;
        this.capitalCount = 0;
        this.talentCount = 0;
        this.cloudCount = 0;
        this.patentCount = 0;
        this.dataCount = 0;

        applyRoleEffects();
    }

    private void applyRoleEffects() {
        if (role != FounderRole.NONE)
            victoryPoints = -1;

        if (role == FounderRole.THE_VC_FUNDED)
             capitalCount = 2;
    }

    public String getName() {
        return name;
    }
    public FounderRole getRole() {
        return role;
    }
    public int getVictoryPoints() {
        return victoryPoints;
    }

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
}
