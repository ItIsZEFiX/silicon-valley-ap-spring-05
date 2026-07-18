package com.techcartel.siliconvalley.view;

import com.techcartel.siliconvalley.controller.GameController;
import com.techcartel.siliconvalley.controller.SaveLoadManager;
import com.techcartel.siliconvalley.exception.GameException;
import com.techcartel.siliconvalley.model.Edge;
import com.techcartel.siliconvalley.model.GameMap;
import com.techcartel.siliconvalley.model.Market;
import com.techcartel.siliconvalley.model.Player;
import com.techcartel.siliconvalley.model.Sector;
import com.techcartel.siliconvalley.model.Vertex;
import com.techcartel.siliconvalley.util.AIType;
import com.techcartel.siliconvalley.util.FounderRole;
import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * Plain terminal UI. No JavaFX, no Swing -- just System.out / Scanner.
 * Keeps the same GameController used by the whole engine, so all
 * rules, exceptions, and AI threading behave identically regardless
 * of the front end.
 */
public class ConsoleUI {

    private final Scanner scanner = new Scanner(System.in);
    private GameController controller;
    private int loggedUpTo = 0;

    public void start() {
        System.out.println("=== Silicon Valley: The Tech Cartel ===");
        List<Player> players = setupPlayers();
        controller = new GameController(players);
        printNewLogs();

        runSetupPhase();
        runMainLoop();
    }

    // ------------------------------------------------------------------
    // Player setup
    // ------------------------------------------------------------------

    private List<Player> setupPlayers() {
        int count = readIntInRange("How many players? (" + GameConstants.MIN_PLAYERS
                + "-" + GameConstants.MAX_PLAYERS + "): ", GameConstants.MIN_PLAYERS, GameConstants.MAX_PLAYERS);

        List<Player> players = new ArrayList<>();
        List<FounderRole> takenRoles = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            System.out.println("--- Player " + i + " ---");
            System.out.print("Name: ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) name = "Player" + i;

            FounderRole role = chooseRole(takenRoles);
            if (role != FounderRole.NONE) takenRoles.add(role);

            boolean ai = readYesNo("Should this player be AI-controlled? (y/n): ");

            Player player = new Player(name, role);
            player.setAIType(AIType.NORMAL);            players.add(player);
        }
        return players;
    }

    private FounderRole chooseRole(List<FounderRole> taken) {
        System.out.println("Choose a founder role (optional):");
        System.out.println("  0) None");
        System.out.println("  1) The Hacker CEO (3:1 market trades)");
        System.out.println("  2) The Tech Guru / CTO (cheaper Unicorn upgrade)");
        System.out.println("  3) The VC-Funded (extra starting Capital, higher crisis limit)");
        while (true) {
            int choice = readIntInRange("Choice: ", 0, 3);
            FounderRole role = switch (choice) {
                case 1 -> FounderRole.THE_HACKER_CEO;
                case 2 -> FounderRole.THE_TECH_GURU_CTO;
                case 3 -> FounderRole.THE_VC_FUNDED;
                default -> FounderRole.NONE;
            };
            if (role != FounderRole.NONE && taken.contains(role)) {
                System.out.println("That role is already taken by another player. Pick another.");
                continue;
            }
            return role;
        }
    }

    // ------------------------------------------------------------------
    // Setup phase (free initial MVP + Partnership placement)
    // ------------------------------------------------------------------

    private void runSetupPhase() {
        while (controller.isSetupPhase()) {
            Player current = controller.getCurrentPlayer();
            System.out.println();
            System.out.println("== Setup phase: " + current.getName() + "'s turn ==");

            if (current.getAIType() != AIType.HUMAN) {
                runAITurnAndWait();
                printNewLogs();
                continue;
            }

            if (!controller.isSetupAwaitingPartnership()) {
                Vertex vertex = chooseVertexForSetupMVP();
                if (vertex == null) continue;
                try {
                    controller.placeSetupMVP(vertex);
                } catch (GameException e) {
                    System.out.println("Invalid placement: " + e.getMessage());
                    continue;
                }
            } else {
                Edge edge = chooseEdgeForSetupPartnership();
                if (edge == null) continue;
                try {
                    controller.placeSetupPartnership(edge);
                } catch (GameException e) {
                    System.out.println("Invalid placement: " + e.getMessage());
                    continue;
                }
            }
            printNewLogs();
        }
        System.out.println();
        System.out.println("== Setup complete. The main game begins! ==");
    }

    // ------------------------------------------------------------------
    // Main game loop
    // ------------------------------------------------------------------

    private void runMainLoop() {
        while (true) {
            Player winner = controller.checkWinner();
            if (winner != null) {
                System.out.println();
                System.out.println("*** " + winner.getName() + " wins with " + winner.getVictoryPoints() + " victory points! ***");
                controller.shutdownAI();
                return;
            }

            Player current = controller.getCurrentPlayer();
            System.out.println();
            System.out.println("== " + current.getName() + "'s turn ==");

            if (current.getAIType() != AIType.HUMAN) {
                runAITurnAndWait();
                printNewLogs();
                continue;
            }

            boolean turnOver = false;
            while (!turnOver) {
                printShortStatus(current);
                printMenu();
                int choice = readIntInRange("Choose an action: ", 0, 11);
                try {
                    switch (choice) {
                        case 1 -> controller.rollDice();
                        case 2 -> handlePendingAuditorIfNeeded();
                        case 3 -> buildMVPFlow();
                        case 4 -> buildPartnershipFlow();
                        case 5 -> upgradeUnicornFlow();
                        case 6 -> buyResourceFlow();
                        case 7 -> sellResourceFlow();
                        case 8 -> tradeWithPlayerFlow();
                        case 9 -> saveGameFlow();
                        case 10 -> loadGameFlow();
                        case 11 -> printBoard();
                        case 0 -> {
                            controller.endTurn();
                            turnOver = true;
                        }
                        default -> System.out.println("Unknown option.");
                    }
                } catch (GameException e) {
                    System.out.println("Action failed: " + e.getMessage());
                }
                printNewLogs();
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1) Roll dice           2) Place Auditor (after a 7)");
        System.out.println("3) Build MVP           4) Build Partnership");
        System.out.println("5) Upgrade to Unicorn  6) Buy from market");
        System.out.println("7) Sell to market      8) Trade with a player");
        System.out.println("9) Save game           10) Load game");
        System.out.println("11) Show board         0) End turn");
    }

    // ------------------------------------------------------------------
    // Action flows
    // ------------------------------------------------------------------

    private void handlePendingAuditorIfNeeded() throws GameException {
        if (!controller.isPendingAuditorPlacement()) {
            System.out.println("No auditor placement is pending right now.");
            return;
        }
        List<Sector> options = controller.getPlaceableAuditorSectors();
        System.out.println("Choose a sector for the Auditor:");
        for (int i = 0; i < options.size(); i++) {
            Sector s = options.get(i);
            System.out.println("  " + i + ") sector (" + s.getRow() + "," + s.getCol() + ") "
                    + s.getResourceProduced() + " #" + s.getActivationNumber());
        }
        int idx = readIntInRange("Choice: ", 0, options.size() - 1);
        controller.placeAuditor(options.get(idx));
    }

    private void buildMVPFlow() throws GameException {
        Vertex vertex = chooseFromValidVertices(v -> controller.getGameMap().isValidMVPPlacement(v, controller.getCurrentPlayer(), false),
                "Choose a vertex for the new MVP:");
        if (vertex == null) return;
        controller.buildMVP(vertex);
    }

    private void buildPartnershipFlow() throws GameException {
        Edge edge = chooseFromValidEdges(e -> controller.getGameMap().isValidPartnershipPlacement(e, controller.getCurrentPlayer(), false),
                "Choose an edge for the new Partnership:");
        if (edge == null) return;
        controller.buildPartnership(edge);
    }

    private void upgradeUnicornFlow() throws GameException {
        Player current = controller.getCurrentPlayer();
        Vertex vertex = chooseFromValidVertices(
                v -> v.hasBuilding() && v.getOwner() == current && v.getBuildingType() == Vertex.BuildingType.MVP,
                "Choose your MVP to upgrade:");
        if (vertex == null) return;
        controller.upgradeToUnicorn(vertex);
    }

    private void buyResourceFlow() throws GameException {
        ResourceType type = chooseTradableResource();
        if (type == null) return;
        controller.buyResource(type);
    }

    private void sellResourceFlow() throws GameException {
        ResourceType type = chooseTradableResource();
        if (type == null) return;
        controller.sellResource(type);
    }

    private void tradeWithPlayerFlow() throws GameException {
        List<Player> others = new ArrayList<>(controller.getPlayers());
        others.remove(controller.getCurrentPlayer());
        if (others.isEmpty()) return;

        System.out.println("Trade with:");
        for (int i = 0; i < others.size(); i++) {
            System.out.println("  " + i + ") " + others.get(i).getName());
        }
        int idx = readIntInRange("Choice: ", 0, others.size() - 1);
        Player other = others.get(idx);

        ResourceType give = chooseAnyResource("What do you give? ");
        int giveAmount = readIntInRange("How many? ", 1, 20);
        ResourceType receive = chooseAnyResource("What do you receive? ");
        int receiveAmount = readIntInRange("How many? ", 1, 20);

        controller.tradeWithPlayer(other, give, giveAmount, receive, receiveAmount);
    }

    private void saveGameFlow() {
        System.out.print("Save file name (e.g. save1.dat): ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) name = "save.dat";
        File file = new File(name);

        CountDownLatch latch = new CountDownLatch(1);
        SaveLoadManager.saveAsync(controller, file, new SaveLoadManager.SaveCallback() {
            @Override
            public void onSuccess() {
                System.out.println("Game saved to " + file.getAbsolutePath());
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                System.out.println("Save failed: " + e.getMessage());
                latch.countDown();
            }
        });
        await(latch);
    }

    private void loadGameFlow() {
        System.out.print("Save file name to load: ");
        String name = scanner.nextLine().trim();
        File file = new File(name);

        CountDownLatch latch = new CountDownLatch(1);
        SaveLoadManager.loadAsync(file, new SaveLoadManager.LoadCallback() {
            @Override
            public void onSuccess(GameController loaded) {
                controller = loaded;
                loggedUpTo = 0;
                System.out.println("Game loaded from " + file.getAbsolutePath());
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                System.out.println("Load failed: " + e.getMessage());
                latch.countDown();
            }
        });
        await(latch);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runAITurnAndWait() {
        CountDownLatch latch = new CountDownLatch(1);
        controller.playAITurnAsync(latch::countDown);
        await(latch);
    }

    // ------------------------------------------------------------------
    // Board / status printing
    // ------------------------------------------------------------------

    private void printShortStatus(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getName()).append(" | VP: ").append(player.getVictoryPoints());
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.NONE) continue;
            sb.append(" | ").append(type).append(": ").append(player.getResourceCount(type));
        }
        System.out.println(sb);
        if (!controller.hasRolledThisTurn()) {
            System.out.println("(You haven't rolled the dice yet this turn.)");
        }
        if (controller.isPendingAuditorPlacement()) {
            System.out.println("(A regulatory crisis occurred -- place the Auditor!)");
        }
    }

    private void printBoard() {
        GameMap map = controller.getGameMap();
        System.out.println();
        System.out.println("--- Sectors ---");
        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Sector s = map.getSector(r, c);
                String label = s.getResourceProduced() == ResourceType.NONE
                        ? "[ ---- ]"
                        : "[" + s.getResourceProduced().toString().substring(0, 3) + " " + s.getActivationNumber() + "]";
                if (s.hasAuditor()) label = "*" + label;
                row.append(String.format("%-9s", label));
            }
            System.out.println(row);
        }

        System.out.println("--- Companies ---");
        boolean any = false;
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v != null && v.hasBuilding()) {
                    any = true;
                    System.out.println("  MVP/Unicorn at (" + r + "," + c + "): " + v.getOwner().getName()
                            + " [" + v.getBuildingType() + "]");
                }
            }
        }
        if (!any) System.out.println("  (none yet)");

        System.out.println("--- Partnerships ---");
        any = false;
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Edge e = map.getHorizontalEdge(r, c);
                if (e != null && e.hasPartnership()) {
                    any = true;
                    System.out.println("  Edge (" + r + "," + c + ", H): " + e.getOwner().getName());
                }
            }
        }
        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Edge e = map.getVerticalEdge(r, c);
                if (e != null && e.hasPartnership()) {
                    any = true;
                    System.out.println("  Edge (" + r + "," + c + ", V): " + e.getOwner().getName());
                }
            }
        }
        if (!any) System.out.println("  (none yet)");

        Market market = controller.getMarket();
        System.out.println("--- Market prices (in Capital) ---");
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.CAPITAL || type == ResourceType.NONE) continue;
            System.out.println("  " + type + ": " + market.getPrice(type));
        }
    }

    private void printNewLogs() {
        List<String> log = controller.getEventLog();
        for (int i = loggedUpTo; i < log.size(); i++) {
            System.out.println("  > " + log.get(i));
        }
        loggedUpTo = log.size();
    }

    // ------------------------------------------------------------------
    // Selection helpers
    // ------------------------------------------------------------------

    private interface VertexFilter { boolean test(Vertex v); }
    private interface EdgeFilter { boolean test(Edge e); }

    private Vertex chooseVertexForSetupMVP() {
        return chooseFromValidVertices(controller.getGameMap()::respectsDistanceRule, "Choose a vertex for your free starting MVP:");
    }

    private Edge chooseEdgeForSetupPartnership() {
        return chooseFromValidEdges(e -> !e.hasPartnership(), "Choose an edge for your free starting Partnership (must touch the MVP you just placed):");
    }

    private Vertex chooseFromValidVertices(VertexFilter filter, String prompt) {
        GameMap map = controller.getGameMap();
        List<Vertex> options = new ArrayList<>();
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v != null && filter.test(v)) options.add(v);
            }
        }
        if (options.isEmpty()) {
            System.out.println("No legal vertices available right now.");
            return null;
        }
        System.out.println(prompt);
        for (int i = 0; i < options.size(); i++) {
            Vertex v = options.get(i);
            System.out.println("  " + i + ") (" + v.getRow() + "," + v.getCol() + ")");
        }
        int idx = readIntInRange("Choice (-1 to cancel): ", -1, options.size() - 1);
        return idx < 0 ? null : options.get(idx);
    }

    private Edge chooseFromValidEdges(EdgeFilter filter, String prompt) {
        GameMap map = controller.getGameMap();
        List<Edge> options = new ArrayList<>();
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Edge e = map.getHorizontalEdge(r, c);
                if (e != null && filter.test(e)) options.add(e);
            }
        }
        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Edge e = map.getVerticalEdge(r, c);
                if (e != null && filter.test(e)) options.add(e);
            }
        }
        if (options.isEmpty()) {
            System.out.println("No legal edges available right now.");
            return null;
        }
        System.out.println(prompt);
        for (int i = 0; i < options.size(); i++) {
            Edge e = options.get(i);
            System.out.println("  " + i + ") (" + e.getRow() + "," + e.getCol() + ", "
                    + (e.isHorizontal() ? "H" : "V") + ")");
        }
        int idx = readIntInRange("Choice (-1 to cancel): ", -1, options.size() - 1);
        return idx < 0 ? null : options.get(idx);
    }

    private ResourceType chooseTradableResource() {
        ResourceType[] types = { ResourceType.TALENT, ResourceType.CLOUD, ResourceType.PATENT, ResourceType.DATA };
        System.out.println("Choose a resource:");
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + i + ") " + types[i]);
        }
        int idx = readIntInRange("Choice (-1 to cancel): ", -1, types.length - 1);
        return idx < 0 ? null : types[idx];
    }

    private ResourceType chooseAnyResource(String prompt) {
        ResourceType[] types = ResourceType.values();
        System.out.println(prompt);
        List<ResourceType> real = new ArrayList<>();
        for (ResourceType t : types) {
            if (t == ResourceType.NONE) continue;
            real.add(t);
        }
        for (int i = 0; i < real.size(); i++) {
            System.out.println("  " + i + ") " + real.get(i));
        }
        int idx = readIntInRange("Choice: ", 0, real.size() - 1);
        return real.get(idx);
    }

    // ------------------------------------------------------------------
    // Raw input helpers
    // ------------------------------------------------------------------

    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                // fall through to error message
            }
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("no")) return false;
            System.out.println("Please answer y or n.");
        }
    }
}
