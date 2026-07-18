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

import java.io.File;
import java.util.List;

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
    private final GameHistoryManager historyManager = new GameHistoryManager(); // The new manager

    @FXML
    public void initialize() {
        bootUpInterface();
    }

    public void bootUpInterface() {
        System.out.println("Main UI initialized and ready!");

        List<Player> dummyPlayers = List.of(
                new Player("Alice (Hacker CEO)", FounderRole.THE_HACKER_CEO),
                new Player("Bob (VC-Funded)", FounderRole.THE_VC_FUNDED)
        );
        gameController = new GameController(dummyPlayers);

        // Populate the market dropdown
        resourceDropdown.getItems().addAll("TALENT", "CLOUD", "PATENT", "DATA");
        resourceDropdown.getSelectionModel().selectFirst();

        // Render the base map
        generateBoardUI();
        refreshUI();
    }

    // ==========================================
    // ACTION HANDLERS
    // ==========================================

    @FXML
    public void handleRollDice() {
        if (gameController.checkWinner() != null) return; // Abort if game is over

        try {
            historyManager.saveSnapshot(gameController);
            gameController.rollDice();
            refreshUI();
        } catch (Exception e) {
            showErrorPopup(e.getMessage());
        }
    }

    @FXML
    public void handleEndTurn() {
        if (gameController.checkWinner() != null) return; // Abort if game is over

        try {
            historyManager.saveSnapshot(gameController);
            gameController.endTurn();
            refreshUI();
        } catch (Exception e) {
            showErrorPopup(e.getMessage());
        }
    }
    @FXML
    public void handleBuyResource() {
        try {
            String selected = resourceDropdown.getValue();
            if (selected == null) return;

            ResourceType type = ResourceType.valueOf(selected);

            // Take snapshot for Undo
            historyManager.saveSnapshot(gameController);

            // Trigger backend buy logic
            gameController.buyResource(type);

            refreshUI();
            System.out.println("Bought 1 " + type.name() + " from the market.");
        } catch (Exception e) {
            showErrorPopup(e.getMessage());
        }
    }

    @FXML
    public void handleSellResource() {
        try {
            String selected = resourceDropdown.getValue();
            if (selected == null) return;

            ResourceType type = ResourceType.valueOf(selected);

            // Take snapshot for Undo
            historyManager.saveSnapshot(gameController);

            // Trigger backend sell logic
            gameController.sellResource(type);

            refreshUI();
            System.out.println("Sold 1 " + type.name() + " to the market.");
        } catch (Exception e) {
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
    // UI REFRESH & BUILD LOGIC
    // ==========================================

    private void refreshUI() {
        updatePlayerStats();
        updateMarketUI();
        updateEventLog();
        generateInteractionOverlay();

        // --- NEW: CHECK FOR GAME OVER FIRST ---
        Player winner = gameController.checkWinner();
        if (winner != null) {
            handleVictory(winner);
            return; // Stop checking for setup/auditor if the game is over!
        }

        if (gameController.isSetupPhase()) {
            eventLogView.getItems().add(0, "SETUP: " + gameController.getCurrentPlayer().getName() + " place your piece.");
        }
        else if (gameController.isPendingAuditorPlacement()) {
            eventLogView.getItems().add(0, "CRISIS: " + gameController.getCurrentPlayer().getName() + " must click a sector to place the Auditor.");
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
        // We must clear the grid first so we don't draw duplicate boards on top of each other
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

                // --- NEW: DRAW THE AUDITOR ---
                // Depending on your backend, this method might be called hasAuditor() or isBlocked()
                if (sector.hasAuditor()) {
                    Circle auditorToken = new Circle(15, Color.BLACK);
                    sectorVisual.getChildren().add(auditorToken);
                }

                // --- NEW: MAKE IT CLICKABLE ---
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

    // ==========================================
    // INTERACTION OVERLAY (VERTICES & EDGES)
    // ==========================================

    private void generateInteractionOverlay() {
        interactionOverlay.getChildren().clear();
        int stepSize = 105;
        GameMap map = gameController.getGameMap();

        // 1. DRAW HORIZONTAL EDGES
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

        // 2. DRAW VERTICAL EDGES
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

        // 3. DRAW VERTICES
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

    private void handleVertexUpgradeClick(int row, int col) {
        try {
            historyManager.saveSnapshot(gameController);
            GameMap map = gameController.getGameMap();
            gameController.upgradeToUnicorn(map.getVertex(row, col));
            refreshUI();
        } catch (Exception e) {
            showErrorPopup(e.getMessage());
        }
    }

    private void handleVertexClick(int row, int col) {
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
            showErrorPopup(e.getMessage());
        }
    }

    private void handleEdgeClick(int row, int col, boolean isHorizontal) {
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
        } catch (Exception e) {
            showErrorPopup(e.getMessage());
        }
    }

    private void handleSectorClick(int row, int col) {
        try {
            // If we aren't waiting for the auditor, clicking a sector does nothing
            if (!gameController.isPendingAuditorPlacement()) {
                return;
            }

            // Always take a snapshot for undo/redo first!
            historyManager.saveSnapshot(gameController);

            // Get the actual Sector object from the map
            GameMap map = gameController.getGameMap();
            Sector targetSector = map.getSector(row, col);

            // Pass the Sector to your GameController
            gameController.placeAuditor(targetSector);

            // Redraw the board so the black circle moves
            mapGrid.getChildren().clear();
            generateBoardUI();
            refreshUI();

            System.out.println("Moved Auditor to sector " + row + ", " + col);

        } catch (Exception e) {
            showErrorPopup(e.getMessage());
        }
    }

    private void handleVictory(Player winner) {
        // 1. Freeze the board so players can't keep clicking
        interactionOverlay.setDisable(true);
        mapGrid.setDisable(true);
        resourceDropdown.setDisable(true);

        // 2. Announce the winner in the event log
        eventLogView.getItems().add(0, "🏆 GAME OVER: " + winner.getName() + " wins with " + winner.getVictoryPoints() + " VP! 🏆");

        // 3. Show a massive Victory Popup
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Victory!");
        alert.setHeaderText("Tech Monopoly Achieved!");
        alert.setContentText(winner.getName() + " has dominated Silicon Valley and won the game with " + winner.getVictoryPoints() + " Victory Points!");

        // This stops the UI until the player clicks "OK"
        alert.showAndWait();
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