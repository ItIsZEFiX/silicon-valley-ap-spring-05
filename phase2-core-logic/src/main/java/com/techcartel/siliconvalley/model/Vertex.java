package com.techcartel.siliconvalley.model;

import java.io.Serializable;

public class Vertex implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;

    public enum BuildingType { MVP, UNICORN }

    // Internally we now store the actual CompanyStructure (MVP or Unicorn)
    // so that produce()/getVictoryPoints() come from real polymorphism,
    // as required by the assignment's class hierarchy. The original
    // owner/BuildingType-based API is kept 100% intact so the rest of
    // the codebase (GameMap BFS, etc.) does not need to change.
    private CompanyStructure structure;

    public Vertex(int row, int col) {
        this.row = row;
        this.col = col;
        this.structure = null;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public Player getOwner() {
        return structure == null ? null : structure.getOwner();
    }

    public BuildingType getBuildingType() {
        if (structure instanceof Unicorn) return BuildingType.UNICORN;
        if (structure instanceof MVP) return BuildingType.MVP;
        return null;
    }

    public boolean hasBuilding() { return structure != null; }

    public CompanyStructure getStructure() { return structure; }

    /**
     * Original convenience method kept for backward compatibility.
     * Builds a fresh MVP or Unicorn structure at this vertex.
     */
    public void setBuilding(Player owner, BuildingType buildingType) {
        if (buildingType == BuildingType.UNICORN) {
            this.structure = new Unicorn(owner);
        } else {
            this.structure = new MVP(owner);
        }
    }

    /** Places a brand new MVP (used by GameController when building). */
    public void buildMVP(Player owner) {
        this.structure = new MVP(owner);
    }

    /** Upgrades an existing MVP to a Unicorn, keeping the same owner. */
    public void upgradeToUnicorn() {
        if (structure instanceof MVP) {
            this.structure = new Unicorn(structure.getOwner());
        }
    }
}
