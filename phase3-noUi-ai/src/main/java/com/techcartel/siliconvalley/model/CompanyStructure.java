package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.ResourceType;
import com.techcartel.siliconvalley.util.StructureType;

import java.io.Serializable;

/**
 * Abstract base class for everything a player can build on the map:
 * MVP, Unicorn, and Partnership. Required by the assignment spec
 * ("abstract class CompanyStructure with produce() and
 * getVictoryPoints() overridden by MVP, Unicorn and Partnership").
 */
public abstract class CompanyStructure implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Player owner;
    protected final StructureType type;

    protected CompanyStructure(Player owner, StructureType type) {
        this.owner = owner;
        this.type = type;
    }

    public Player getOwner() {
        return owner;
    }

    public StructureType getType() {
        return type;
    }

    /**
     * How many units of the given sector's resource this structure
     * yields when that sector activates. Partnerships never produce
     * resources directly, so they simply return 0.
     */
    public abstract int produce(ResourceType sectorResource);

    /**
     * Victory points this structure is currently worth.
     */
    public abstract int getVictoryPoints();
}
