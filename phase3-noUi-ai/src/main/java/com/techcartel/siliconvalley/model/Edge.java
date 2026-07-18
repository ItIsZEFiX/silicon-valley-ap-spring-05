package com.techcartel.siliconvalley.model;

import java.io.Serializable;

public class Edge implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;
    private final boolean isHorizontal;

    // Backed by a real Partnership (CompanyStructure) instead of a bare
    // owner reference, so produce()/getVictoryPoints() use polymorphism
    // like the rest of the structure hierarchy. Public API unchanged.
    private CompanyStructure structure;

    public Edge(int row, int col, boolean isHorizontal) {
        this.row = row;
        this.col = col;
        this.isHorizontal = isHorizontal;
        this.structure = null; // No one owns it at the start of the game
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean isHorizontal() { return isHorizontal; }

    public Player getOwner() {
        return structure == null ? null : structure.getOwner();
    }

    public boolean hasPartnership() {
        return structure != null;
    }

    public CompanyStructure getStructure() { return structure; }

    public void setPartnership(Player owner) {
        this.structure = new Partnership(owner);
    }
}
