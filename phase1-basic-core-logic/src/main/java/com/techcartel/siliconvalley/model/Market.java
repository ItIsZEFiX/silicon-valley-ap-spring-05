package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.ResourceType;

public class Market {

    private int capitalPrice;
    private int talentPrice;
    private int cloudPrice;
    private int patentPrice;
    private int dataPrice;

    public Market() {
        this.capitalPrice = 3;
        this.talentPrice = 3;
        this.cloudPrice = 3;
        this.patentPrice = 3;
        this.dataPrice = 3;
    }

    public int getPrice(ResourceType type) {
        return switch (type) {
            case CAPITAL -> capitalPrice;
            case TALENT -> talentPrice;
            case CLOUD -> cloudPrice;
            case PATENT -> patentPrice;
            case DATA -> dataPrice;
            default -> 0;
        };
    }

    public void inflatePrice(ResourceType type) {
        switch (type) {
            case CAPITAL -> capitalPrice++;
            case TALENT -> talentPrice++;
            case CLOUD -> cloudPrice++;
            case PATENT -> patentPrice++;
            case DATA -> dataPrice++;
        }
    }

    public void crashPrice(ResourceType type) {
        switch (type) {
            case CAPITAL -> capitalPrice = Math.max(1, capitalPrice - 1);
            case TALENT -> talentPrice = Math.max(1, talentPrice - 1);
            case CLOUD -> cloudPrice = Math.max(1, cloudPrice - 1);
            case PATENT -> patentPrice = Math.max(1, patentPrice - 1);
            case DATA -> dataPrice = Math.max(1, dataPrice - 1);
        }
    }

}
