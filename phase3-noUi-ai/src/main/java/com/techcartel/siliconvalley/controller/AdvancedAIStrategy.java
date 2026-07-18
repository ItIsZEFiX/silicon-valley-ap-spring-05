package com.techcartel.siliconvalley.controller;

import com.techcartel.siliconvalley.model.*;
import com.techcartel.siliconvalley.util.ResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AdvancedAIStrategy {

    private AdvancedAIStrategy() {}

    static void playTurn(GameController controller, Random random) {
        if (controller.isSetupPhase()) { playSetupTurn(controller); return; }

        try { controller.rollDice(); } catch (Exception ignored) {}

        if (controller.isPendingAuditorPlacement()) {
            List<Sector> options = controller.getPlaceableAuditorSectors();
            if (!options.isEmpty()) {
                Sector best = options.stream()
                        .max((a, b) -> auditorScore(a, controller) - auditorScore(b, controller))
                        .get();
                try { controller.placeAuditor(best); } catch (Exception ignored) {}
            }
        }

        tryUpgradeBestMVP(controller);
        tryBuildBestMVP(controller);
        tryBuildBestPartnership(controller);
        tryBuyNeeded(controller);

        try { controller.endTurn(); } catch (Exception ignored) {}
    }

    private static void playSetupTurn(GameController controller) {
        GameMap map = controller.getGameMap();
        Player player = controller.getCurrentPlayer();

        if (!controller.isSetupAwaitingPartnership()) {
            Vertex best = null; int bestScore = -1;
            for (int r = 0; r < GameMap.VERTEX_SIZE; r++)
                for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                    Vertex v = map.getVertex(r, c);
                    if (v != null && map.respectsDistanceRule(v)) {
                        int s = map.getAdjacentSectors(v).size();
                        if (s > bestScore) { bestScore = s; best = v; }
                    }
                }
            if (best == null) return;
            try { controller.placeSetupMVP(best); } catch (Exception ignored) { return; }
        }

        for (int r = 0; r < GameMap.VERTEX_SIZE; r++)
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v == null || v.getOwner() != player) continue;
                for (Edge e : map.getAdjacentEdges(v))
                    if (!e.hasPartnership())
                        try { controller.placeSetupPartnership(e); return; } catch (Exception ignored) {}
            }
    }

    private static void tryUpgradeBestMVP(GameController controller) {
        GameMap map = controller.getGameMap();
        Player player = controller.getCurrentPlayer();
        Vertex best = null; int bestScore = -1;
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++)
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v != null && v.hasBuilding() && v.getOwner() == player
                        && v.getBuildingType() == Vertex.BuildingType.MVP) {
                    int s = map.getAdjacentSectors(v).size();
                    if (s > bestScore) { bestScore = s; best = v; }
                }
            }
        if (best != null) try { controller.upgradeToUnicorn(best); } catch (Exception ignored) {}
    }

    private static void tryBuildBestMVP(GameController controller) {
        GameMap map = controller.getGameMap();
        Player player = controller.getCurrentPlayer();
        Vertex best = null; int bestScore = -1;
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++)
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v != null && map.isValidMVPPlacement(v, player, false)) {
                    int s = map.getAdjacentSectors(v).size();
                    if (s > bestScore) { bestScore = s; best = v; }
                }
            }
        if (best != null) try { controller.buildMVP(best); } catch (Exception ignored) {}
    }

    private static void tryBuildBestPartnership(GameController controller) {
        GameMap map = controller.getGameMap();
        Player player = controller.getCurrentPlayer();
        Edge best = null; int bestScore = -1;

        for (int r = 0; r < GameMap.VERTEX_SIZE; r++)
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Edge e = map.getHorizontalEdge(r, c);
                if (e != null && map.isValidPartnershipPlacement(e, player, false)) {
                    int s = edgeScore(e, map);
                    if (s > bestScore) { bestScore = s; best = e; }
                }
            }
        for (int r = 0; r < GameMap.GRID_SIZE; r++)
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Edge e = map.getVerticalEdge(r, c);
                if (e != null && map.isValidPartnershipPlacement(e, player, false)) {
                    int s = edgeScore(e, map);
                    if (s > bestScore) { bestScore = s; best = e; }
                }
            }
        if (best != null) try { controller.buildPartnership(best); } catch (Exception ignored) {}
    }

    private static void tryBuyNeeded(GameController controller) {
        Player player = controller.getCurrentPlayer();
        if (player.getResourceCount(ResourceType.CAPITAL) <= 3) return;

        // buy the resource the player has least of
        ResourceType[] tradable = { ResourceType.TALENT, ResourceType.CLOUD, ResourceType.PATENT, ResourceType.DATA };
        ResourceType choice = tradable[0];
        int min = player.getResourceCount(tradable[0]);
        for (ResourceType t : tradable) {
            int cnt = player.getResourceCount(t);
            if (cnt < min) { min = cnt; choice = t; }
        }
        try { controller.buyResource(choice); } catch (Exception ignored) {}
    }

    private static int auditorScore(Sector s, GameController controller) {
        GameMap map = controller.getGameMap();
        Player me = controller.getCurrentPlayer();
        int score = 0;
        for (Vertex v : map.getVerticesForSector(s)) {
            if (v == null || !v.hasBuilding() || v.getOwner() == me) continue;
            score += v.getBuildingType() == Vertex.BuildingType.UNICORN ? 3 : 2;
        }
        return score;
    }

    private static int edgeScore(Edge e, GameMap map) {
        Vertex[] ep = map.getEndpoints(e);
        int s = 0;
        if (ep[0] != null) s += map.getAdjacentSectors(ep[0]).size();
        if (ep[1] != null) s += map.getAdjacentSectors(ep[1]).size();
        return s;
    }
}
