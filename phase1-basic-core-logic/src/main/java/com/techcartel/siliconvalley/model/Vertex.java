package com.techcartel.siliconvalley.model;

public class Vertex {
    private final int row;
    private final int col;

    public enum BuildingType { MVP, UNICORN }

    private Player owner;
    private BuildingType buildingType;

    public Vertex(int row, int col) {
        this.row = row;
        this.col = col;
        this.owner = null;
        this.buildingType = null;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public Player getOwner() { return owner; }
    public BuildingType getBuildingType() { return buildingType; }

    public boolean hasBuilding() { return owner != null; }

    public void setBuilding(Player owner, BuildingType buildingType) {
        this.owner = owner;
        this.buildingType = buildingType;
    }
}
