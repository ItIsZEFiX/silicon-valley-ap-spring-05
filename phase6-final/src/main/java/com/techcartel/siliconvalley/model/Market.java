package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class Market implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<ResourceType, Integer> prices;
    // How many consecutive rounds each resource has gone WITHOUT being bought.
    // After ROUNDS_BEFORE_PRICE_DROP rounds of no purchases, the price drops.
    private final Map<ResourceType, Integer> roundsSinceLastPurchase;

    public Market() {
        prices = new EnumMap<>(ResourceType.class);
        roundsSinceLastPurchase = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.NONE) continue;
            prices.put(type, GameConstants.BASE_PRICE);
            roundsSinceLastPurchase.put(type, 0);
        }
    }

    public int getPrice(ResourceType type) {
        return prices.getOrDefault(type, 0);
    }

    public void inflatePrice(ResourceType type) {
        if (!prices.containsKey(type)) return;
        int newPrice = Math.min(GameConstants.MAX_PRICE, prices.get(type) + 1);
        prices.put(type, newPrice);
        roundsSinceLastPurchase.put(type, 0);
    }

    public void crashPrice(ResourceType type) {
        if (!prices.containsKey(type)) return;
        int newPrice = Math.max(GameConstants.MIN_PRICE, prices.get(type) - 1);
        prices.put(type, newPrice);
    }

    /**
     * Call once per completed turn. Any resource that has gone
     * ROUNDS_BEFORE_PRICE_DROP consecutive rounds without being bought
     * drops in price by 1 (floor MIN_PRICE).
     */
    public void advanceRound() {
        for (ResourceType type : prices.keySet()) {
            int rounds = roundsSinceLastPurchase.getOrDefault(type, 0) + 1;
            if (rounds >= GameConstants.ROUNDS_BEFORE_PRICE_DROP) {
                int newPrice = Math.max(GameConstants.MIN_PRICE, prices.get(type) - 1);
                prices.put(type, newPrice);
                rounds = 0;
            }
            roundsSinceLastPurchase.put(type, rounds);
        }
    }

    /** Called when a resource is bought from the market, resetting its drop timer and inflating its price. */
    public void recordPurchase(ResourceType type) {
        inflatePrice(type);
    }
}
