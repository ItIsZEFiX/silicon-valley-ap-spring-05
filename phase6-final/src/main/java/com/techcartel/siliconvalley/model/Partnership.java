package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.ResourceType;
import com.techcartel.siliconvalley.util.StructureType;

public class Partnership extends CompanyStructure {

    private static final long serialVersionUID = 1L;

    public Partnership(Player owner) {
        super(owner, StructureType.PARTNERSHIP);
    }

    @Override
    public int produce(ResourceType sectorResource) {
        // Partnerships (edges) never produce resources directly.
        return 0;
    }

    @Override
    public int getVictoryPoints() {
        // No direct victory points; only indirectly via the
        // "longest network" bonus computed in GameController.
        return 0;
    }
}
