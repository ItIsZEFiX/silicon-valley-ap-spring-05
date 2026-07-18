package com.techcartel.siliconvalley.view;

import com.techcartel.siliconvalley.controller.GameController;
import com.techcartel.siliconvalley.exception.GameException;
import com.techcartel.siliconvalley.model.Edge;
import com.techcartel.siliconvalley.model.GameMap;
import com.techcartel.siliconvalley.model.Market;
import com.techcartel.siliconvalley.model.Player;
import com.techcartel.siliconvalley.model.Sector;
import com.techcartel.siliconvalley.model.Vertex;
import com.techcartel.siliconvalley.util.FounderRole;
import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;
import com.techcartel.siliconvalley.controller.SaveLoadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class ConsoleUI {
    private final Scanner scanner;
    private final GameController controller;
    private int loggedUpTo = 0;

    public ConsoleUI(GameController controller) {
        this.controller = controller;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printWelcome();

        printNewLogs();

        runSetupPhase();
        runMainLoop();
    }

    private void printWelcome() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║      Silicon Valley: The Tech Cartel        ║");
        System.out.println("║              Terminal Session               ║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    private void runSetupPhase() {
        while (controller.isSetupPhase()) {
            Player current = controller.getCurrentPlayer();

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════╗");
            System.out.println("║ Setup Phase                                  ║");
            System.out.println("║ Current Player: " + current.getName());
            System.out.println("╚══════════════════════════════════════════════╝");

            if (current.isAIControlled()) {
                System.out.println("AI is making setup move...");
                performAIAssistedSetup();
                printNewLogs();
                continue;
            }

            try {
                if (!controller.isSetupAwaitingPartnership()) {
                    Vertex vertex = chooseVertexForSetupMVP();
                    if (vertex == null) {
                        System.out.println("Setup cancelled.");
                        continue;
                    }
                    controller.placeSetupMVP(vertex);
                } else {
                    Edge edge = chooseEdgeForSetupPartnership();
                    if (edge == null) {
                        System.out.println("Setup cancelled.");
                        continue;
                    }
                    controller.placeSetupPartnership(edge);
                }
            } catch (GameException e) {
                System.out.println("Error: " + e.getMessage());
            }

            printNewLogs();
        }
    }

    private void performAIAssistedSetup() {
        // اگر بعداً AI setup logic دقیق‌تر شد، اینجا وصلش می‌کنیم.
        // فعلاً فقط turn-based flow را نگه می‌داریم.
        System.out.println("AI setup logic is not fully implemented here yet.");
    }

    private void runMainLoop() {
        while (true) {
            if (controller.checkWinner() != null) {
                System.out.println();
                System.out.println("══════════════════════════════════════════════");
                System.out.println("Winner: " + controller.checkWinner().getName());
                System.out.println("══════════════════════════════════════════════");
                controller.shutdownAI();
                return;
            }

            Player current = controller.getCurrentPlayer();

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════╗");
            System.out.println("║ Current Turn                                 ║");
            System.out.println("║ Player: " + current.getName());
            System.out.println("╚══════════════════════════════════════════════╝");

            if (current.isAIControlled()) {
                runAITurnAndWait();
                printNewLogs();
                continue;
            }

            printShortStatus(current);
            printMenu();

            int choice = readIntInRange(0, 11);

            try {
                handleAction(choice);
            } catch (GameException e) {
                System.out.println("Error: " + e.getMessage());
            }

            printNewLogs();
        }
    }

    private void handleAction(int choice) throws GameException {
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
            case 0 -> controller.endTurn();
            default -> System.out.println("Invalid choice.");
        }
    }

    private void printShortStatus(Player current) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║ Status                                       ║");
        System.out.println("║ Name: " + current.getName());
        System.out.println("║ AI: " + (current.isAIControlled() ? "Yes" : "No"));
        System.out.println("║ Role: " + current.getFounderRole());
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║ 1) Roll Dice                                 ║");
        System.out.println("║ 2) Handle Auditor                            ║");
        System.out.println("║ 3) Build MVP                                 ║");
        System.out.println("║ 4) Build Partnership                         ║");
        System.out.println("║ 5) Upgrade Unicorn                            ║");
        System.out.println("║ 6) Buy Resource                              ║");
        System.out.println("║ 7) Sell Resource                             ║");
        System.out.println("║ 8) Trade With Player                         ║");
        System.out.println("║ 9) Save Game                                 ║");
        System.out.println("║ 10) Load Game                                ║");
        System.out.println("║ 11) Show Board                               ║");
        System.out.println("║ 0) End Turn                                  ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.print("Select: ");
    }

    private int readIntInRange(int min, int max) {
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.print("Enter a number between " + min + " and " + max + ": ");
        }
    }

    private void runAITurnAndWait() {
        System.out.println("AI turn is processing...");
        controller.endTurn();
    }

    private void buildMVPFlow() throws GameException {
        Vertex vertex = chooseVertexForSetupMVP();
        if (vertex != null) {
            controller.placeSetupMVP(vertex);
        }
    }

    private void buildPartnershipFlow() throws GameException {
        Edge edge = chooseEdgeForSetupPartnership();
        if (edge != null) {
            controller.placeSetupPartnership(edge);
        }
    }

    private void upgradeUnicornFlow() {
        System.out.println("Upgrade Unicorn flow is not implemented yet.");
    }

    private void buyResourceFlow() {
        System.out.println("Buy resource flow is not implemented yet.");
    }

    private void sellResourceFlow() {
        System.out.println("Sell resource flow is not implemented yet.");
    }

    private void tradeWithPlayerFlow() {
        System.out.println("Trade with player flow is not implemented yet.");
    }

    private void saveGameFlow() {
        System.out.print("Enter save file name: ");
        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            System.out.println("Invalid file name.");
            return;
        }

        File file = new File(fileName);
        CountDownLatch latch = new CountDownLatch(1);

        SaveLoadManager.saveAsync(controller, file, result -> {
            System.out.println(result);
            latch.countDown();
        });

        awaitLatch(latch);
    }

    private void loadGameFlow() {
        System.out.print("Enter load file name: ");
        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            System.out.println("Invalid file name.");
            return;
        }

        File file = new File(fileName);
        CountDownLatch latch = new CountDownLatch(1);

        SaveLoadManager.loadAsync(file, result -> {
            System.out.println(result);
            latch.countDown();
        });

        awaitLatch(latch);
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Operation interrupted.");
        }
    }

    private void handlePendingAuditorIfNeeded() {
        System.out.println("Auditor handling is not implemented yet.");
    }

    private void printBoard() {
        GameMap map = controller.getGameMap();
        Market market = controller.getMarket();

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║ Board                                        ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Sector sector = map.getSector(r, c);
                if (sector == null) continue;

                System.out.printf("[%s %d%s] ",
                        sector.getResourceProduced(),
                        sector.getActivationNumber(),
                        sector.hasAuditor() ? "!" : "");
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("Market:");
        for (ResourceType type : ResourceType.values()) {
            System.out.println("- " + type + ": " + market.getPrice(type));
        }
    }

    private void printNewLogs() {
        List<String> logs = controller.getEventLog();
        for (int i = loggedUpTo; i < logs.size(); i++) {
            System.out.println(logs.get(i));
        }
        loggedUpTo = logs.size();
    }

    private Vertex chooseVertexForSetupMVP() {
        List<Vertex> validVertices = new ArrayList<>();
        GameMap map = controller.getGameMap();

        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Vertex v = map.getVertex(r, c);
                if (v != null) validVertices.add(v);
            }
        }

        return chooseFromValidVertices(validVertices, "Choose vertex for MVP");
    }

    private Edge chooseEdgeForSetupPartnership() {
        List<Edge> validEdges = new ArrayList<>();
        GameMap map = controller.getGameMap();

        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Edge h = map.getHorizontalEdge(r, c);
                if (h != null && !h.hasPartnership()) validEdges.add(h);

                Edge v = map.getVerticalEdge(r, c);
                if (v != null && !v.hasPartnership()) validEdges.add(v);
            }
        }

        return chooseFromValidEdges(validEdges, "Choose edge for Partnership");
    }

    private Vertex chooseFromValidVertices(List<Vertex> vertices, String title) {
        if (vertices.isEmpty()) {
            System.out.println("No valid vertices available.");
            return null;
        }

        System.out.println(title);
        for (int i = 0; i < vertices.size(); i++) {
            System.out.println((i + 1) + ") " + vertices.get(i));
        }
        System.out.println("0) Cancel");
        System.out.print("Select: ");

        int choice = readIntInRange(0, vertices.size());
        if (choice == 0) return null;
        return vertices.get(choice - 1);
    }

    private Edge chooseFromValidEdges(List<Edge> edges, String title) {
        if (edges.isEmpty()) {
            System.out.println("No valid edges available.");
            return null;
        }

        System.out.println(title);
        for (int i = 0; i < edges.size(); i++) {
            System.out.println((i + 1) + ") " + edges.get(i));
        }
        System.out.println("0) Cancel");
        System.out.print("Select: ");

        int choice = readIntInRange(0, edges.size());
        if (choice == 0) return null;
        return edges.get(choice - 1);
    }
}
