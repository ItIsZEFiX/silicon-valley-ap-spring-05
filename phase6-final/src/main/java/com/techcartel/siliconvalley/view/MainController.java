package com.techcartel.siliconvalley.view;

import com.techcartel.siliconvalley.controller.GameController;
import com.techcartel.siliconvalley.controller.GameHistoryManager;
import com.techcartel.siliconvalley.controller.SaveLoadManager;
import com.techcartel.siliconvalley.model.GameMap;
import com.techcartel.siliconvalley.model.Market;
import com.techcartel.siliconvalley.model.Player;
import com.techcartel.siliconvalley.model.Sector;
import com.techcartel.siliconvalley.model.Vertex;
import com.techcartel.siliconvalley.model.Edge;
import com.techcartel.siliconvalley.util.FounderRole;
import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceDialog;
import java.util.ArrayList;
import java.util.Optional;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Spinner;
import java.io.File;
import java.util.List;
import com.techcartel.siliconvalley.util.AIType;

public class MainController {

    @FXML
    private VBox playerStatsArea;

    @FXML
    private GridPane mapGrid;

    @FXML
    private Pane interactionOverlay;

    @FXML
    private VBox marketArea;

    @FXML
    private VBox playersListContainer;

    @FXML
    private VBox marketListContainer;

    @FXML
    private ListView<String> eventLogView;

    @FXML
    private ComboBox<String> resourceDropdown;

    private GameController gameController;
    private final GameHistoryManager historyManager = new GameHistoryManager();

    @FXML
    public void initialize() {
        bootUpInterface();
    }

    public void bootUpInterface() {
        System.out.println("Main UI initialized and ready!");

        // 1. Ask for total players
        Optional<Integer> totalPlayersOpt = getPlayerCount();
        if (totalPlayersOpt.isEmpty()) {
            Platform.exit(); System.exit(0); return;
        }
        int totalPlayers = totalPlayersOpt.get();

        // 2. Ask how many of them should be AI
        Optional<Integer> aiCountOpt = getAICount(totalPlayers);
        if (aiCountOpt.isEmpty()) {
            Platform.exit(); System.exit(0); return;
        }
        int aiCount = aiCountOpt.get();

        // 3. Ask for AI Difficulty (Only if there are actually AIs playing)
        AIType aiDifficulty = AIType.NORMAL;
        if (aiCount > 0) {
            Optional<AIType> diffOpt = getAIDifficulty();
            if (diffOpt.isEmpty()) {
                Platform.exit(); System.exit(0); return;
            }
            aiDifficulty = diffOpt.get();
        }

        // 4. Dynamically build the player list
        List<Player> startingPlayers = new ArrayList<>();
        FounderRole[] roles = {
                FounderRole.THE_HACKER_CEO,
                FounderRole.THE_VC_FUNDED,
                FounderRole.THE_TECH_GURU_CTO,
                FounderRole.NONE
        };

        int humanCount = totalPlayers - aiCount;

        for (int i = 0; i < totalPlayers; i++) {
            boolean isAI = i >= humanCount;
            // Append the difficulty to their name so it shows on the UI scoreboard
            String nameSuffix = isAI ? " (" + aiDifficulty.name() + " Bot)" : "";

            Player p = new Player("Player " + (i + 1) + nameSuffix, roles[i]);

            // Set the correct enum type
            p.setAIType(isAI ? aiDifficulty : AIType.HUMAN);
            startingPlayers.add(p);
        }

        // 5. Pass the dynamic list to the backend engine
        gameController = new GameController(startingPlayers);

        resourceDropdown.getItems().addAll("TALENT", "CLOUD", "PATENT", "DATA");
        resourceDropdown.getSelectionModel().selectFirst();

        generateBoardUI();
        refreshUI();

        processAITurns();
    }

    private Optional<Integer> getPlayerCount() {
        List<Integer> choices = List.of(2, 3, 4);
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(2, choices);
        dialog.setTitle("Game Setup");
        dialog.setHeaderText("Welcome to Silicon Valley: The Tech Cartel");
        dialog.setContentText("Select TOTAL number of players (Humans + Bots):");

        // Return the Optional directly so bootUpInterface can check if it's empty
        return dialog.showAndWait();
    }

    private Optional<Integer> getAICount(int maxPlayers) {
        List<Integer> choices = new ArrayList<>();
        for (int i = 0; i <= maxPlayers; i++) {
            choices.add(i);
        }

        int defaultSelection = (maxPlayers == 2) ? 1 : 0;

        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(defaultSelection, choices);
        dialog.setTitle("AI Setup");
        dialog.setHeaderText("Configure AI Opponents");
        dialog.setContentText("How many of these " + maxPlayers + " players should be AI-controlled?");

        // Return the Optional directly so bootUpInterface can check if it's empty
        return dialog.showAndWait();
    }

    private Optional<AIType> getAIDifficulty() {
        List<AIType> choices = List.of(AIType.NORMAL, AIType.ADVANCED);
        ChoiceDialog<AIType> dialog = new ChoiceDialog<>(AIType.NORMAL, choices);
        dialog.setTitle("AI Difficulty");
        dialog.setHeaderText("Configure AI Intelligence");
        dialog.setContentText("Select the difficulty level for the bots:");

        return dialog.showAndWait();
    }

    // ==========================================
    // ACTION HANDLERS
    // ==========================================

    @FXML
    public void handleRollDice() {
        if (gameController.checkWinner() != null || gameController.isStalemate()) return;
        if (gameController.getCurrentPlayer().isAIControlled()) return;

        try {
            historyManager.saveSnapshot(gameController);
            gameController.rollDice();
            refreshUI();
        } catch (Exception e) {
            historyManager.discardLastSnapshot(); // Throw away broken state!
            showErrorPopup(e.getMessage());
        }
    }

    @FXML
    public void handleEndTurn() {
        if (gameController.checkWinner() != null || gameController.isStalemate()) return;
        if (gameController.getCurrentPlayer().isAIControlled()) return;

        try {
            historyManager.saveSnapshot(gameController);
            gameController.endTurn();
            refreshUI();

            // Trigger AI if the next player is a computer
            processAITurns();
        } catch (Exception e) {
            historyManager.discardLastSnapshot();
            showErrorPopup(e.getMessage());
        }
    }

    @FXML
    public void handleBuyResource() {
        if (gameController.getCurrentPlayer().isAIControlled()) return;
        try {
            String selected = resourceDropdown.getValue();
            if (selected == null) return;
            ResourceType type = ResourceType.valueOf(selected);

            historyManager.saveSnapshot(gameController);
            gameController.buyResource(type);
            refreshUI();
            System.out.println("Bought 1 " + type.name() + " from the market.");
        } catch (Exception e) {
            historyManager.discardLastSnapshot();
            showErrorPopup(e.getMessage());
        }
    }

    @FXML
    public void handleSellResource() {
        if (gameController.getCurrentPlayer().isAIControlled()) return;
        try {
            String selected = resourceDropdown.getValue();
            if (selected == null) return;
            ResourceType type = ResourceType.valueOf(selected);

            historyManager.saveSnapshot(gameController);
            gameController.sellResource(type);
            refreshUI();
            System.out.println("Sold 1 " + type.name() + " to the market.");
        } catch (Exception e) {
            historyManager.discardLastSnapshot();
            showErrorPopup(e.getMessage());
        }
    }

    @FXML
    public void handlePlayerTrade() {
        if (gameController.getCurrentPlayer().isAIControlled()) return;

        // 1. Setup the Custom Dialog Window
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Player Trade");
        dialog.setHeaderText("Negotiate a trade with another player.");

        ButtonType tradeButtonType = new ButtonType("Propose Trade", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(tradeButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // 2. Build the Dropdowns and Spinners
        ComboBox<String> playerBox = new ComboBox<>();
        for (Player p : gameController.getPlayers()) {
            // Populate everyone EXCEPT the person currently taking their turn
            if (p != gameController.getCurrentPlayer()) {
                playerBox.getItems().add(p.getName());
            }
        }
        if (!playerBox.getItems().isEmpty()) playerBox.getSelectionModel().selectFirst();

        ComboBox<String> giveResBox = new ComboBox<>();
        giveResBox.getItems().addAll("CAPITAL","TALENT", "CLOUD", "PATENT", "DATA");
        giveResBox.getSelectionModel().selectFirst();

        Spinner<Integer> giveAmount = new Spinner<>(1, 10, 1);

        ComboBox<String> receiveResBox = new ComboBox<>();
        receiveResBox.getItems().addAll("TALENT", "CLOUD", "PATENT", "DATA");
        receiveResBox.getSelectionModel().selectFirst();

        Spinner<Integer> receiveAmount = new Spinner<>(1, 10, 1);

        // 3. Add Elements to the Layout
        grid.add(new Label("Trade With:"), 0, 0);
        grid.add(playerBox, 1, 0);

        grid.add(new Label("You Give:"), 0, 1);
        grid.add(giveResBox, 1, 1);
        grid.add(giveAmount, 2, 1);

        grid.add(new Label("You Receive:"), 0, 2);
        grid.add(receiveResBox, 1, 2);
        grid.add(receiveAmount, 2, 2);

        dialog.getDialogPane().setContent(grid);

        // 4. Wait for the user to click "Propose Trade"
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == tradeButtonType) {
            try {
                // Read all the inputs from the form
                String targetName = playerBox.getValue();
                Player target = gameController.getPlayers().stream()
                        .filter(p -> p.getName().equals(targetName))
                        .findFirst()
                        .orElse(null);

                ResourceType giveType = ResourceType.valueOf(giveResBox.getValue());
                ResourceType receiveType = ResourceType.valueOf(receiveResBox.getValue());
                int gAmt = giveAmount.getValue();
                int rAmt = receiveAmount.getValue();

                if (target != null) {
                    // Save for Undo, then execute the trade!
                    historyManager.saveSnapshot(gameController);
                    gameController.tradeWithPlayer(target, giveType, gAmt, receiveType, rAmt);
                    refreshUI();
                }
            } catch (Exception e) {
                // If they try to trade resources they don't have, catch it and dump the snapshot
                historyManager.discardLastSnapshot();
                showErrorPopup(e.getMessage());
            }
        }
    }

    // ==========================================
    // INTERACTION HANDLERS (MAP CLICKS)
    // ==========================================

    private void handleVertexUpgradeClick(int row, int col) {
        if (gameController.getCurrentPlayer().isAIControlled()) return;
        try {
            historyManager.saveSnapshot(gameController);
            GameMap map = gameController.getGameMap();
            gameController.upgradeToUnicorn(map.getVertex(row, col));
            refreshUI();
        } catch (Exception e) {
            historyManager.discardLastSnapshot();
            showErrorPopup(e.getMessage());
        }
    }

    private void handleVertexClick(int row, int col) {
        if (gameController.getCurrentPlayer().isAIControlled()) return;
        try {
            historyManager.saveSnapshot(gameController);
            GameMap map = gameController.getGameMap();

            if (gameController.isSetupPhase()) {
                gameController.placeSetupMVP(map.getVertex(row, col));
            } else {
                gameController.buildMVP(map.getVertex(row, col));
            }

            refreshUI();
        } catch (Exception e) {
            historyManager.discardLastSnapshot();
            showErrorPopup(e.getMessage());
        }
    }

    private void handleEdgeClick(int row, int col, boolean isHorizontal) {
        if (gameController.getCurrentPlayer().isAIControlled()) return;
        try {
            historyManager.saveSnapshot(gameController);
            GameMap map = gameController.getGameMap();
            Edge edge = isHorizontal ? map.getHorizontalEdge(row, col) : map.getVerticalEdge(row, col);

            if (gameController.isSetupPhase()) {
                gameController.placeSetupPartnership(edge);
            } else {
                gameController.buildPartnership(edge);
            }

            refreshUI();

            // During setup, placing a partnership advances the turn. Trigger AI if needed.
            if (gameController.isSetupPhase()) {
                processAITurns();
            }
        } catch (Exception e) {
            historyManager.discardLastSnapshot();
            showErrorPopup(e.getMessage());
        }
    }

    private void handleSectorClick(int row, int col) {
        if (gameController.getCurrentPlayer().isAIControlled()) return;
        try {
            if (!gameController.isPendingAuditorPlacement()) return;

            historyManager.saveSnapshot(gameController);
            GameMap map = gameController.getGameMap();
            Sector targetSector = map.getSector(row, col);
            gameController.placeAuditor(targetSector);

            mapGrid.getChildren().clear();
            generateBoardUI();
            refreshUI();
            System.out.println("Moved Auditor to sector " + row + ", " + col);
        } catch (Exception e) {
            historyManager.discardLastSnapshot();
            showErrorPopup(e.getMessage());
        }
    }

    // ==========================================
    // TIME TRAVEL (UNDO / REDO)
    // ==========================================

    @FXML
    public void handleUndo() {
        try {
            if (historyManager.canUndo()) {
                gameController = historyManager.undo(gameController);
                mapGrid.getChildren().clear();
                generateBoardUI();
                refreshUI();
                System.out.println("Action undone.");
            }
        } catch (Exception e) {
            showErrorPopup("Failed to undo: " + e.getMessage());
        }
    }

    @FXML
    public void handleRedo() {
        try {
            if (historyManager.canRedo()) {
                gameController = historyManager.redo(gameController);
                mapGrid.getChildren().clear();
                generateBoardUI();
                refreshUI();
                System.out.println("Action redone.");
            }
        } catch (Exception e) {
            showErrorPopup("Failed to redo: " + e.getMessage());
        }
    }

    // ==========================================
    // SYSTEM I/O (SAVE & LOAD)
    // ==========================================

    @FXML
    public void handleSaveGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Silicon Valley Game");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Save Files", "*.dat", "*.json", "*.txt"));

        File file = fileChooser.showSaveDialog(mapGrid.getScene().getWindow());

        if (file != null) {
            SaveLoadManager.saveAsync(gameController, file, new SaveLoadManager.SaveCallback() {
                @Override
                public void onSuccess() {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Save Successful");
                        alert.setHeaderText(null);
                        alert.setContentText("Game state saved securely to disk.");
                        alert.showAndWait();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Platform.runLater(() -> showErrorPopup("Failed to save game: " + e.getMessage()));
                }
            });
        }
    }

    @FXML
    public void handleLoadGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Silicon Valley Game");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Save Files", "*.dat", "*.json", "*.txt"));

        File file = fileChooser.showOpenDialog(mapGrid.getScene().getWindow());

        if (file != null) {
            SaveLoadManager.loadAsync(file, new SaveLoadManager.LoadCallback() {
                @Override
                public void onSuccess(GameController loadedController) {
                    Platform.runLater(() -> {
                        gameController = loadedController;
                        mapGrid.getChildren().clear();
                        generateBoardUI();
                        refreshUI();

                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Load Successful");
                        alert.setHeaderText(null);
                        alert.setContentText("Game state restored successfully.");
                        alert.showAndWait();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Platform.runLater(() -> showErrorPopup("Failed to load game: " + e.getMessage()));
                }
            });
        }
    }

    // ==========================================
    // AI INTEGRATION
    // ==========================================

    private void processAITurns() {
        if (gameController.checkWinner() != null || gameController.isStalemate()) return;

        if (gameController.getCurrentPlayer().isAIControlled()) {
            System.out.println(gameController.getCurrentPlayer().getName() + " (AI) is thinking...");

            gameController.playAITurnAsync(() -> {
                Platform.runLater(() -> {
                    mapGrid.getChildren().clear();
                    generateBoardUI();
                    refreshUI();

                    // Call recursively in case the next player is also an AI
                    processAITurns();
                });
            });
        }
    }

    // ==========================================
    // UI REFRESH & BUILD LOGIC
    // ==========================================

    private void refreshUI() {
        updatePlayerStats();
        updateMarketUI();
        updateEventLog();
        generateInteractionOverlay();

        // --- UPDATED: CHECK FOR GAME OVER FIRST ---
        Player winner = gameController.checkWinner();
        if (winner != null) {
            handleVictory(winner);
            return;
        }

        // --- NEW: CHECK FOR STALEMATE ---
        if (gameController.isStalemate()) {
            handleStalemate();
            return;
        }

        if (gameController.isSetupPhase()) {
            eventLogView.getItems().add(0, "SETUP: " + gameController.getCurrentPlayer().getName() + " place your piece.");
        }
        else if (gameController.isPendingAuditorPlacement()) {
            eventLogView.getItems().add(0, "CRISIS: " + gameController.getCurrentPlayer().getName() + " must click a red sector to place the Auditor.");
        }
    }

    private void updateMarketUI() {
        if (marketListContainer == null) return;
        marketListContainer.getChildren().clear();

        Market market = gameController.getMarket();
        ResourceType[] tradables = {
                ResourceType.TALENT, ResourceType.CLOUD, ResourceType.PATENT, ResourceType.DATA
        };

        for (ResourceType type : tradables) {
            int price = market.getPrice(type);
            Label priceLabel = new Label(type.name() + ": " + price + " Capital");
            priceLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-family: 'Consolas', monospace; -fx-font-size: 14px;");
            marketListContainer.getChildren().add(priceLabel);
        }
    }

    private void updateEventLog() {
        if (eventLogView == null) return;
        eventLogView.getItems().clear();
        List<String> logs = gameController.getEventLog();
        for (String log : logs) {
            eventLogView.getItems().add("> " + log);
        }
        if (!logs.isEmpty()) {
            eventLogView.scrollTo(logs.size() - 1);
        }
    }

    private void generateBoardUI() {
        mapGrid.getChildren().clear();

        GameMap map = gameController.getGameMap();
        for (int row = 0; row < GameConstants.GRID_SIZE; row++) {
            for (int col = 0; col < GameConstants.GRID_SIZE; col++) {
                Sector sector = map.getSector(row, col);
                StackPane sectorVisual = new StackPane();
                Rectangle background = new Rectangle(100, 100);

                Color sectorColor = switch (sector.getResourceProduced()) {
                    case CAPITAL -> Color.web("#DAA520");
                    case TALENT -> Color.web("#4169E1");
                    case CLOUD -> Color.web("#87CEEB");
                    case PATENT -> Color.web("#9370DB");
                    case DATA -> Color.web("#32CD32");
                    case NONE -> Color.web("#808080");
                };

                background.setFill(sectorColor);
                background.setStroke(Color.web("#2b2b36"));
                background.setStrokeWidth(3);

                String labelText = sector.getResourceProduced().name() + "\n#" + sector.getActivationNumber();
                if (sector.getResourceProduced() == ResourceType.NONE) {
                    labelText = "REGULATORY\nZONE";
                }

                Label infoLabel = new Label(labelText);
                infoLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-alignment: center; -fx-text-alignment: center;");

                sectorVisual.getChildren().addAll(background, infoLabel);

                if (sector.hasAuditor()) {
                    Circle auditorToken = new Circle(15, Color.BLACK);
                    sectorVisual.getChildren().add(auditorToken);
                }

                // Highlight legal targets for the Auditor during a crisis
                if (gameController.isPendingAuditorPlacement()) {
                    if (gameController.getPlaceableAuditorSectors().contains(sector)) {
                        background.setStroke(Color.RED);
                        background.setStrokeWidth(4);
                        sectorVisual.setStyle("-fx-cursor: hand;");
                    } else {
                        background.setOpacity(0.4); // Dim illegal sectors
                    }
                }

                final int finalRow = row;
                final int finalCol = col;
                sectorVisual.setOnMouseClicked(e -> handleSectorClick(finalRow, finalCol));

                mapGrid.add(sectorVisual, col, row);
            }
        }
    }

    private void updatePlayerStats() {
        if (playersListContainer == null) return;
        playersListContainer.getChildren().clear();

        for (Player player : gameController.getPlayers()) {
            VBox playerCard = new VBox();
            playerCard.setSpacing(5);
            playerCard.setStyle("-fx-background-color: #3a3a4a; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #555; -fx-border-radius: 5;");

            Label nameLabel = new Label(player.getName() + " - " + player.getVictoryPoints() + " VP");
            nameLabel.setStyle("-fx-text-fill: #00FFCC; -fx-font-weight: bold; -fx-font-size: 14px;");

            String resourceText = String.format(
                    "CAP: %d  |  TAL: %d  |  CLD: %d\nPAT: %d  |  DAT: %d",
                    player.getResourceCount(ResourceType.CAPITAL),
                    player.getResourceCount(ResourceType.TALENT),
                    player.getResourceCount(ResourceType.CLOUD),
                    player.getResourceCount(ResourceType.PATENT),
                    player.getResourceCount(ResourceType.DATA)
            );

            Label resourcesLabel = new Label(resourceText);
            resourcesLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-family: 'Consolas', monospace;");

            playerCard.getChildren().addAll(nameLabel, resourcesLabel);
            playersListContainer.getChildren().add(playerCard);
        }
    }

    private void showErrorPopup(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Invalid Action");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void generateInteractionOverlay() {
        interactionOverlay.getChildren().clear();
        int stepSize = 105;
        GameMap map = gameController.getGameMap();

        for (int row = 0; row < GameConstants.VERTEX_SIZE; row++) {
            for (int col = 0; col < GameConstants.GRID_SIZE; col++) {
                Edge hEdge = map.getHorizontalEdge(row, col);
                if (hEdge == null) continue;

                double xPixel = col * stepSize + 12;
                double yPixel = row * stepSize - 6;

                Rectangle edgeNode = new Rectangle(stepSize - 24, 12);
                edgeNode.setX(xPixel);
                edgeNode.setY(yPixel);

                if (hEdge.hasPartnership()) {
                    edgeNode.setFill(getPlayerColor(hEdge.getOwner()));
                } else {
                    edgeNode.setFill(Color.web("#ffffff", 0.2));
                    edgeNode.setOnMouseEntered(e -> edgeNode.setFill(Color.web("#00FFCC", 0.6)));
                    edgeNode.setOnMouseExited(e -> edgeNode.setFill(Color.web("#ffffff", 0.2)));

                    final int r = row;
                    final int c = col;
                    edgeNode.setOnMouseClicked(e -> handleEdgeClick(r, c, true));
                }
                interactionOverlay.getChildren().add(edgeNode);
            }
        }

        for (int row = 0; row < GameConstants.GRID_SIZE; row++) {
            for (int col = 0; col < GameConstants.VERTEX_SIZE; col++) {
                Edge vEdge = map.getVerticalEdge(row, col);
                if (vEdge == null) continue;

                double xPixel = col * stepSize - 6;
                double yPixel = row * stepSize + 12;

                Rectangle edgeNode = new Rectangle(12, stepSize - 24);
                edgeNode.setX(xPixel);
                edgeNode.setY(yPixel);

                if (vEdge.hasPartnership()) {
                    edgeNode.setFill(getPlayerColor(vEdge.getOwner()));
                } else {
                    edgeNode.setFill(Color.web("#ffffff", 0.2));
                    edgeNode.setOnMouseEntered(e -> edgeNode.setFill(Color.web("#00FFCC", 0.6)));
                    edgeNode.setOnMouseExited(e -> edgeNode.setFill(Color.web("#ffffff", 0.2)));

                    final int r = row;
                    final int c = col;
                    edgeNode.setOnMouseClicked(e -> handleEdgeClick(r, c, false));
                }
                interactionOverlay.getChildren().add(edgeNode);
            }
        }

        for (int row = 0; row < GameConstants.VERTEX_SIZE; row++) {
            for (int col = 0; col < GameConstants.VERTEX_SIZE; col++) {
                double xPixel = col * stepSize;
                double yPixel = row * stepSize;
                Vertex vertex = map.getVertex(row, col);

                Circle vertexNode = new Circle(12);
                vertexNode.setCenterX(xPixel);
                vertexNode.setCenterY(yPixel);

                if (vertex != null && vertex.hasBuilding()) {
                    Color playerColor = getPlayerColor(vertex.getOwner());
                    vertexNode.setFill(playerColor);

                    if (vertex.getBuildingType() == Vertex.BuildingType.UNICORN) {
                        vertexNode.setRadius(16);
                        vertexNode.setStroke(Color.WHITE);
                        vertexNode.setStrokeWidth(3);
                    } else {
                        vertexNode.setStroke(Color.BLACK);
                        vertexNode.setStrokeWidth(2);
                        final int finalRow = row;
                        final int finalCol = col;
                        vertexNode.setOnMouseClicked(e -> handleVertexUpgradeClick(finalRow, finalCol));
                    }
                } else {
                    vertexNode.setFill(Color.web("#ffffff", 0.5));
                    vertexNode.setStroke(Color.web("#00FFCC"));
                    vertexNode.setStrokeWidth(2);

                    vertexNode.setOnMouseEntered(e -> vertexNode.setFill(Color.web("#00FFCC", 0.8)));
                    vertexNode.setOnMouseExited(e -> vertexNode.setFill(Color.web("#ffffff", 0.5)));

                    final int finalRow = row;
                    final int finalCol = col;
                    vertexNode.setOnMouseClicked(e -> handleVertexClick(finalRow, finalCol));
                }
                interactionOverlay.getChildren().add(vertexNode);
            }
        }
    }

    private void handleVictory(Player winner) {
        interactionOverlay.setDisable(true);
        mapGrid.setDisable(true);
        resourceDropdown.setDisable(true);

        eventLogView.getItems().add(0, "🏆 GAME OVER: " + winner.getName() + " wins with " + winner.getVictoryPoints() + " VP! 🏆");

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Victory!");
        alert.setHeaderText("Tech Monopoly Achieved!");
        alert.setContentText(winner.getName() + " has dominated Silicon Valley and won the game with " + winner.getVictoryPoints() + " Victory Points!");

        alert.showAndWait();

        Platform.exit();
        System.exit(0);
    }

    private void handleStalemate() {
        // 1. Freeze the board
        interactionOverlay.setDisable(true);
        mapGrid.setDisable(true);
        resourceDropdown.setDisable(true);

        // 2. Announce the draw in the log
        eventLogView.getItems().add(0, "⚖️ STALEMATE: The grid is full and no one can win!");

        // 3. Show a Warning Popup
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Stalemate!");
        alert.setHeaderText("Economic Deadlock!");
        alert.setContentText("The board is completely full, all companies are upgraded, and no player can mathematically reach the winning score. The game is a draw!");

        // This pauses the game and waits for the user to click "OK"
        alert.showAndWait();

        // Instantly shut down the application after they click OK ---
        Platform.exit();
        System.exit(0);
    }

    private Color getPlayerColor(Player player) {
        int index = gameController.getPlayers().indexOf(player);
        return switch (index) {
            case 0 -> Color.web("#00FFFF");
            case 1 -> Color.web("#FF8C00");
            case 2 -> Color.web("#FF1493");
            case 3 -> Color.web("#32CD32");
            default -> Color.WHITE;
        };
    }
}