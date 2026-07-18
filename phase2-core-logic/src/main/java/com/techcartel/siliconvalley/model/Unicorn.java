package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;
import com.techcartel.siliconvalley.util.StructureType;

public class Unicorn extends CompanyStructure {

    private static final long serialVersionUID = 1L;

    public Unicorn(Player owner) {
        super(owner, StructureType.UNICORN);
    }

    @Override
    public int produce(ResourceType sectorResource) {
        if (sectorResource == ResourceType.NONE) {
            return 0;
        }
        return GameConstants.UNICORN_PRODUCTION;
    }

    @Override
    public int getVictoryPoints() {
        return GameConstants.UNICORN_VICTORY_POINTS;
    }
}
