package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.ResourceType;

public class Sector {

    private final int row;
    private final int col;
    private final ResourceType resourceProduced;
    private final int activationNumber; // The dice roll (2-12)
    private boolean hasAuditor;

    public Sector(int row, int col, ResourceType resourceProduced, int activationNumber) {
        this.row = row;
        this.col = col;
        this.resourceProduced = resourceProduced;
        this.activationNumber = activationNumber;
        this.hasAuditor = false;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public ResourceType getResourceProduced() { return resourceProduced; }
    public int getActivationNumber() { return activationNumber; }

    public boolean hasAuditor() { return hasAuditor; }
    public void setAuditor(boolean hasAuditor) { this.hasAuditor = hasAuditor; }
}