package com.techcartel.siliconvalley.controller;

import com.techcartel.siliconvalley.model.Edge;
import com.techcartel.siliconvalley.model.GameMap;
import com.techcartel.siliconvalley.model.Player;
import com.techcartel.siliconvalley.model.Sector;
import com.techcartel.siliconvalley.model.Vertex;
import com.techcartel.siliconvalley.util.ResourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple rule-based AI (see extra-credit "AI" section: random / rule
 * based). Runs entirely through GameController's synchronized public
 * methods, so it is safe to execute on a background thread while the
 * Swing UI thread is idle/waiting.
 */
final class AIStrategy {

    private AIStrategy() {}

    static void playTurn(GameController controller, Random random) {
        if (controller.isSetupPhase()) {
            playSetupTurn(controller, random);
            return;
        }

        Player player = controller.getCurrentPlayer();

        try {
            controller.rollDice();
        } catch (Exception ignored) {
            // already rolled somehow; continue
        }

        if (controller.isPendingAuditorPlacement()) {
            List<Sector> options = controller.getPlaceableAuditorSectors();
            if (!options.isEmpty()) {
                Sector choice = options.get(random.nextInt(options.size()));
                try {
                    controller.placeAuditor(choice);
                } catch (Exception ignored) {
                    // no legal move somehow; skip
                }
            }
        }

        // Greedy: try to upgrade an MVP to Unicorn, then build a Partnership,
        // then build an MVP, then buy whatever helps, in that priority order.
        tryUpgradeAnyMVP(controller, player);
        tryBuildPartnership(controller, player, random);
        tryBuildMVP(controller, player, random);
        tryBuySomething(controller, player, random);

        try {
            controller.endTurn();
        } catch (Exception ignored) {
        }
    }

    private static void playSetupTurn(GameController controller, Random random) {
        GameMap map = controller.getGameMap();
        Player player = controller.getCurrentPlayer();

        if (!controller.isSetupAwaitingPartnership()) {
            List<Vertex> candidates = new ArrayList<>();
            for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
                for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                    Vertex v = map.getVertex(r, c);
                    if (v != null && map.respectsDistanceRule(v)) {
                        candidates.add(v);
                    }
                }
            }
            if (candidates.isEmpty()) return;
            Vertex choice = candidates.get(random.nextInt(candidates.size()));
            try {
                controller.placeSetupMVP(choice);
            } catch (Exception ignored) {
                return;
            }
        }

        // Now place the connecting Partnership.
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v == null || v.getOwner() != player) continue;
                List<Edge> edges = map.getAdjacentEdges(v);
                for (Edge e : edges) {
                    if (!e.hasPartnership()) {
                        try {
                            controller.placeSetupPartnership(e);
                            return;
                        } catch (Exception ignored) {
                            // try next edge
                        }
                    }
                }
            }
        }
    }

    private static void tryUpgradeAnyMVP(GameController controller, Player player) {
        GameMap map = controller.getGameMap();
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v != null && v.hasBuilding() && v.getOwner() == player
                        && v.getBuildingType() == Vertex.BuildingType.MVP) {
                    try {
                        controller.upgradeToUnicorn(v);
                        return;
                    } catch (Exception ignored) {
                        // can't afford or invalid; keep looking
                    }
                }
            }
        }
    }

    private static void tryBuildPartnership(GameController controller, Player player, Random random) {
        GameMap map = controller.getGameMap();
        List<Edge> candidates = new ArrayList<>();
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Edge e = map.getHorizontalEdge(r, c);
                if (e != null && map.isValidPartnershipPlacement(e, player, false)) candidates.add(e);
            }
        }
        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Edge e = map.getVerticalEdge(r, c);
                if (e != null && map.isValidPartnershipPlacement(e, player, false)) candidates.add(e);
            }
        }
        if (candidates.isEmpty()) return;
        Edge choice = candidates.get(random.nextInt(candidates.size()));
        try {
            controller.buildPartnership(choice);
        } catch (Exception ignored) {
        }
    }

    private static void tryBuildMVP(GameController controller, Player player, Random random) {
        GameMap map = controller.getGameMap();
        List<Vertex> candidates = new ArrayList<>();
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v != null && map.isValidMVPPlacement(v, player, false)) candidates.add(v);
            }
        }
        if (candidates.isEmpty()) return;
        Vertex choice = candidates.get(random.nextInt(candidates.size()));
        try {
            controller.buildMVP(choice);
        } catch (Exception ignored) {
        }
    }

    private static void tryBuySomething(GameController controller, Player player, Random random) {
        ResourceType[] tradable = { ResourceType.TALENT, ResourceType.CLOUD, ResourceType.PATENT, ResourceType.DATA };
        // Only buy if we have a comfortable capital surplus, so AI doesn't bankrupt itself.
        if (player.getResourceCount(ResourceType.CAPITAL) <= 3) return;
        ResourceType choice = tradable[random.nextInt(tradable.length)];
        try {
            controller.buyResource(choice);
        } catch (Exception ignored) {
        }
    }
}
