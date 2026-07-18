package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;
import com.techcartel.siliconvalley.util.StructureType;

public class MVP extends CompanyStructure {

    private static final long serialVersionUID = 1L;

    public MVP(Player owner) {
        super(owner, StructureType.MVP);
    }

    @Override
    public int produce(ResourceType sectorResource) {
        if (sectorResource == ResourceType.NONE) {
            return 0;
        }
        return GameConstants.MVP_PRODUCTION;
    }

    @Override
    public int getVictoryPoints() {
        return GameConstants.MVP_VICTORY_POINTS;
    }
}
