package com.techcartel.siliconvalley.model;

public class Edge {

    private final int row;
    private final int col;
    private final boolean isHorizontal;
    private Player owner;

    public Edge(int row, int col, boolean isHorizontal) {
        this.row = row;
        this.col = col;
        this.isHorizontal = isHorizontal;
        this.owner = null; // No one owns it at the start of the game
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean isHorizontal() { return isHorizontal; }
    public Player getOwner() { return owner; }
    public boolean hasPartnership() {
        return owner != null;
    }
    public void setPartnership(Player owner) {
        this.owner = owner;
    }
}
