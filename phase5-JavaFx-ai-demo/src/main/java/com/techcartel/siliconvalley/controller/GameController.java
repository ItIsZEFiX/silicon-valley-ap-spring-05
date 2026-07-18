package com.techcartel.siliconvalley.controller;

import com.techcartel.siliconvalley.exception.IllegalGameActionException;
import com.techcartel.siliconvalley.exception.InsufficientResourcesException;
import com.techcartel.siliconvalley.exception.InvalidPlacementException;
import com.techcartel.siliconvalley.model.CompanyStructure;
import com.techcartel.siliconvalley.model.Dice;
import com.techcartel.siliconvalley.model.Edge;
import com.techcartel.siliconvalley.model.GameMap;
import com.techcartel.siliconvalley.model.Market;
import com.techcartel.siliconvalley.model.Player;
import com.techcartel.siliconvalley.model.Sector;
import com.techcartel.siliconvalley.model.Vertex;
import com.techcartel.siliconvalley.util.AIType;
import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Core game engine. Every method that touches shared mutable state
 * (market, map, players, turn order) is `synchronized` so that a
 * background AI thread and the console's main thread can never
 * corrupt state by acting at the same time.
 */
public class GameController implements Serializable {

    private static final long serialVersionUID = 1L;

    private final GameMap gameMap;
    private final Market market;
    private final Dice dice;
    private final List<Player> players;
    private int currentPlayerIndex;
    private boolean rolledThisTurn;
    private boolean pendingAuditorPlacement;
    private Sector auditorSector;

    // ---- Setup phase state ----
    private boolean setupPhase;
    private final List<Player> setupOrder;
    private int setupIndex;
    private boolean setupAwaitingPartnership;
    private Vertex setupPendingVertex;
    private final java.util.Map<Player, Vertex> secondSetupMVP;

    // ---- Longest network ----
    private Player longestNetworkHolder;
    private int longestNetworkLength;

    // ---- Event log for the UI ----
    private final List<String> eventLog;

    // ---- AI thread management (not serialized) ----
    private transient ExecutorService aiExecutor;
    private final transient Random random = new Random();

    public GameController(List<Player> startingPlayers) {
        this.market = new Market();
        this.dice = new Dice();
        this.players = new ArrayList<>(startingPlayers);
        this.currentPlayerIndex = 0;
        this.rolledThisTurn = false;
        this.pendingAuditorPlacement = false;
        this.auditorSector = null;
        this.eventLog = new ArrayList<>();

        this.gameMap = new GameMap();

        this.setupPhase = true;
        this.setupOrder = new ArrayList<>(players);
        List<Player> reversed = new ArrayList<>(players);
        java.util.Collections.reverse(reversed);
        this.setupOrder.addAll(reversed);
        this.setupIndex = 0;
        this.setupAwaitingPartnership = false;
        this.setupPendingVertex = null;
        this.secondSetupMVP = new java.util.HashMap<>();

        addLog("Game started with " + players.size() + " players. Setup phase begins.");
    }

    // ------------------------------------------------------------------
    // Basic getters
    // ------------------------------------------------------------------

    public GameMap getGameMap() { return gameMap; }
    public Market getMarket() { return market; }
    public Dice getDice() { return dice; }
    public List<Player> getPlayers() { return players; }
    public boolean isSetupPhase() { return setupPhase; }
    public boolean hasRolledThisTurn() { return rolledThisTurn; }
    public boolean isPendingAuditorPlacement() { return pendingAuditorPlacement; }
    public Sector getAuditorSector() { return auditorSector; }
    public List<String> getEventLog() { return eventLog; }

    public synchronized Player getCurrentPlayer() {
        if (setupPhase) {
            return setupOrder.get(setupIndex);
        }
        return players.get(currentPlayerIndex);
    }

    private void addLog(String message) {
        eventLog.add(message);
        // Cap the log so it doesn't grow forever in a long game.
        if (eventLog.size() > 500) {
            eventLog.remove(0);
        }
    }

    // ------------------------------------------------------------------
    // Setup phase
    // ------------------------------------------------------------------

    public synchronized boolean isSetupAwaitingPartnership() {
        return setupAwaitingPartnership;
    }

    public synchronized void placeSetupMVP(Vertex vertex) throws InvalidPlacementException {
        if (!setupPhase) throw new IllegalStateExceptionWrapper("Setup phase already finished.");
        if (setupAwaitingPartnership) {
            throw new InvalidPlacementException("You must place a Partnership before your next MVP.");
        }
        if (!gameMap.respectsDistanceRule(vertex)) {
            throw new InvalidPlacementException("That vertex is too close to another company.");
        }
        Player player = getCurrentPlayer();
        vertex.buildMVP(player);
        player.addStructure(vertex.getStructure());

        if (setupIndex >= players.size()) {
            secondSetupMVP.put(player, vertex);
        }

        setupPendingVertex = vertex;
        setupAwaitingPartnership = true;
        addLog(player.getName() + " placed a free starting MVP.");
    }

    public synchronized void placeSetupPartnership(Edge edge) throws InvalidPlacementException {
        if (!setupPhase) throw new IllegalStateExceptionWrapper("Setup phase already finished.");
        if (!setupAwaitingPartnership) {
            throw new InvalidPlacementException("Place your MVP first.");
        }
        if (edge.hasPartnership()) {
            throw new InvalidPlacementException("That edge is already taken.");
        }
        Vertex[] endpoints = gameMap.getEndpoints(edge);
        boolean touchesPendingVertex = endpoints[0] == setupPendingVertex || endpoints[1] == setupPendingVertex;
        if (!touchesPendingVertex) {
            throw new InvalidPlacementException("The starting Partnership must connect to the MVP you just placed.");
        }

        Player player = getCurrentPlayer();
        edge.setPartnership(player);
        player.addStructure(edge.getStructure());
        addLog(player.getName() + " placed a free starting Partnership.");

        setupAwaitingPartnership = false;
        setupPendingVertex = null;
        advanceSetupTurn();
    }

    private void advanceSetupTurn() {
        setupIndex++;
        if (setupIndex >= setupOrder.size()) {
            finishSetupPhase();
        }
    }

    private void finishSetupPhase() {
        for (Player player : players) {
            Vertex vertex = secondSetupMVP.get(player);
            if (vertex == null) continue;
            for (Sector sector : gameMap.getAdjacentSectors(vertex)) {
                if (sector.getResourceProduced() != ResourceType.NONE) {
                    player.addResource(sector.getResourceProduced(), 1);
                }
            }
        }
        setupPhase = false;
        currentPlayerIndex = 0;
        addLog("Setup phase complete. " + getCurrentPlayer().getName() + " goes first.");
    }

    // ------------------------------------------------------------------
    // Turn flow
    // ------------------------------------------------------------------

    public synchronized int rollDice() throws IllegalGameActionException {
        ensureMainPhase();
        if (rolledThisTurn) {
            throw new IllegalGameActionException("You already rolled this turn.");
        }
        int roll = dice.roll();
        rolledThisTurn = true;
        addLog(getCurrentPlayer().getName() + " rolled a " + roll + ".");

        if (roll == GameConstants.CRISIS_ROLL) {
            triggerRegulatoryCrisis();
        } else {
            distributeResources(roll);
        }
        return roll;
    }

    private void distributeResources(int roll) {
        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Sector sector = gameMap.getSector(r, c);
                if (sector == null || sector.getActivationNumber() != roll) continue;
                if (sector.hasAuditor()) continue;

                for (Vertex vertex : gameMap.getVerticesForSector(sector)) {
                    if (vertex == null || !vertex.hasBuilding()) continue;
                    Player owner = vertex.getOwner();
                    CompanyStructure structure = vertex.getStructure();
                    int amount = structure.produce(sector.getResourceProduced());
                    if (amount > 0) {
                        owner.addResource(sector.getResourceProduced(), amount);
                    }
                }
            }
        }
        addLog("Sectors with number " + roll + " produced resources.");
    }

    private void triggerRegulatoryCrisis() {
        addLog("Regulatory crisis! (rolled a 7)");
        for (Player player : players) {
            int limit = player.getCrisisCardLimit();
            int total = player.getTotalResourceCount();
            if (total > limit) {
                int toDiscard = total / 2;
                autoDiscard(player, toDiscard);
                addLog(player.getName() + " discarded " + toDiscard + " cards (tax).");
            }
        }
        pendingAuditorPlacement = true;
    }

    /** Greedy auto-discard: always remove from whichever pile is currently largest. */
    private void autoDiscard(Player player, int amount) {
        for (int i = 0; i < amount; i++) {
            ResourceType biggest = null;
            int biggestCount = 0;
            for (ResourceType type : ResourceType.values()) {
                if (type == ResourceType.NONE) continue;
                int count = player.getResourceCount(type);
                if (count > biggestCount) {
                    biggestCount = count;
                    biggest = type;
                }
            }
            if (biggest == null) break;
            player.removeResource(biggest, 1);
        }
    }

    public synchronized List<Sector> getPlaceableAuditorSectors() {
        List<Sector> result = new ArrayList<>();
        boolean anySectorHasOpponentBuilding = false;
        List<Sector> withOpponents = new ArrayList<>();

        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Sector sector = gameMap.getSector(r, c);
                if (sector == null || sector == auditorSector) continue;
                result.add(sector);
                for (Vertex v : gameMap.getVerticesForSector(sector)) {
                    if (v != null && v.hasBuilding() && v.getOwner() != getCurrentPlayer()) {
                        anySectorHasOpponentBuilding = true;
                        withOpponents.add(sector);
                        break;
                    }
                }
            }
        }
        return anySectorHasOpponentBuilding ? withOpponents : result;
    }

    public synchronized void placeAuditor(Sector sector) throws IllegalGameActionException {
        if (!pendingAuditorPlacement) {
            throw new IllegalGameActionException("No auditor placement is pending.");
        }
        if (!getPlaceableAuditorSectors().contains(sector)) {
            throw new IllegalGameActionException("You must place the Auditor on a sector with an opponent's company (if one exists).");
        }
        if (auditorSector != null) {
            auditorSector.setAuditor(false);
        }
        auditorSector = sector;
        sector.setAuditor(true);
        pendingAuditorPlacement = false;
        addLog(getCurrentPlayer().getName() + " moved the Auditor.");
    }

    private void ensureMainPhase() throws IllegalGameActionException {
        if (setupPhase) {
            throw new IllegalGameActionException("Still in setup phase.");
        }
    }

    private void ensureReadyForActions() throws IllegalGameActionException {
        ensureMainPhase();
        if (!rolledThisTurn) {
            throw new IllegalGameActionException("Roll the dice before taking actions.");
        }
        if (pendingAuditorPlacement) {
            throw new IllegalGameActionException("Place the Auditor before taking other actions.");
        }
    }

    // ------------------------------------------------------------------
    // Market
    // ------------------------------------------------------------------

    public synchronized void buyResource(ResourceType type) throws IllegalGameActionException, InsufficientResourcesException {
        ensureReadyForActions();
        if (type == ResourceType.CAPITAL || type == ResourceType.NONE) {
            throw new IllegalGameActionException("Cannot buy that resource.");
        }
        Player player = getCurrentPlayer();
        int cost = Math.max(1, market.getPrice(type) - player.getMarketBuyDiscount());

        if (!player.canAfford(ResourceType.CAPITAL, cost)) {
            throw new InsufficientResourcesException(
                    player.getName() + " needs " + cost + " Capital to buy " + type + ".");
        }
        player.removeResource(ResourceType.CAPITAL, cost);
        player.addResource(type, 1);
        market.inflatePrice(type);
        addLog(player.getName() + " bought 1 " + type + " for " + cost + " Capital.");
    }

    public synchronized void sellResource(ResourceType type) throws IllegalGameActionException, InsufficientResourcesException {
        ensureReadyForActions();
        if (type == ResourceType.CAPITAL || type == ResourceType.NONE) {
            throw new IllegalGameActionException("Cannot sell that resource.");
        }
        Player player = getCurrentPlayer();
        if (!player.canAfford(type, 1)) {
            throw new InsufficientResourcesException(player.getName() + " has no " + type + " to sell.");
        }
        int payout = market.getPrice(type);
        player.removeResource(type, 1);
        player.addResource(ResourceType.CAPITAL, payout);
        market.crashPrice(type);
        addLog(player.getName() + " sold 1 " + type + " for " + payout + " Capital.");
    }

    public synchronized void tradeWithPlayer(Player other, ResourceType give, int giveAmount,
                                              ResourceType receive, int receiveAmount)
            throws IllegalGameActionException, InsufficientResourcesException {
        ensureReadyForActions();
        Player current = getCurrentPlayer();
        if (other == current) {
            throw new IllegalGameActionException("Cannot trade with yourself.");
        }
        if (!current.canAfford(give, giveAmount)) {
            throw new InsufficientResourcesException(current.getName() + " does not have enough " + give + ".");
        }
        if (!other.canAfford(receive, receiveAmount)) {
            throw new InsufficientResourcesException(other.getName() + " does not have enough " + receive + ".");
        }
        current.removeResource(give, giveAmount);
        current.addResource(receive, receiveAmount);
        other.removeResource(receive, receiveAmount);
        other.addResource(give, giveAmount);
        addLog(current.getName() + " traded " + giveAmount + " " + give + " with " + other.getName()
                + " for " + receiveAmount + " " + receive + ".");
    }

    // ------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------

    public synchronized void buildMVP(Vertex vertex) throws IllegalGameActionException, InvalidPlacementException, InsufficientResourcesException {
        ensureReadyForActions();
        Player player = getCurrentPlayer();

        if (!gameMap.isValidMVPPlacement(vertex, player, false)) {
            throw new InvalidPlacementException("Illegal MVP placement.");
        }
        requireResources(player, ResourceType.CAPITAL, GameConstants.MVP_CAPITAL_COST);
        requireResources(player, ResourceType.TALENT, GameConstants.MVP_TALENT_COST);
        requireResources(player, ResourceType.CLOUD, GameConstants.MVP_CLOUD_COST);
        requireResources(player, ResourceType.DATA, GameConstants.MVP_DATA_COST);

        player.removeResource(ResourceType.CAPITAL, GameConstants.MVP_CAPITAL_COST);
        player.removeResource(ResourceType.TALENT, GameConstants.MVP_TALENT_COST);
        player.removeResource(ResourceType.CLOUD, GameConstants.MVP_CLOUD_COST);
        player.removeResource(ResourceType.DATA, GameConstants.MVP_DATA_COST);

        vertex.buildMVP(player);
        player.addStructure(vertex.getStructure());
        addLog(player.getName() + " built an MVP.");
    }

    public synchronized void upgradeToUnicorn(Vertex vertex) throws IllegalGameActionException, InvalidPlacementException, InsufficientResourcesException {
        ensureReadyForActions();
        Player player = getCurrentPlayer();

        if (!vertex.hasBuilding() || vertex.getOwner() != player || vertex.getBuildingType() != Vertex.BuildingType.MVP) {
            throw new InvalidPlacementException("You can only upgrade your own MVP.");
        }
        int cloudCost = player.getUnicornCloudCost();
        int dataCost = GameConstants.UNICORN_DATA_COST;
        requireResources(player, ResourceType.CLOUD, cloudCost);
        requireResources(player, ResourceType.DATA, dataCost);

        player.removeResource(ResourceType.CLOUD, cloudCost);
        player.removeResource(ResourceType.DATA, dataCost);

        CompanyStructure oldStructure = vertex.getStructure();
        player.removeStructure(oldStructure);
        vertex.upgradeToUnicorn();
        player.addStructure(vertex.getStructure());
        addLog(player.getName() + " upgraded an MVP to a Unicorn.");
    }

    public synchronized void buildPartnership(Edge edge) throws IllegalGameActionException, InvalidPlacementException, InsufficientResourcesException {
        ensureReadyForActions();
        Player player = getCurrentPlayer();

        if (!gameMap.isValidPartnershipPlacement(edge, player, false)) {
            throw new InvalidPlacementException("Illegal Partnership placement.");
        }
        requireResources(player, ResourceType.CAPITAL, GameConstants.PARTNERSHIP_CAPITAL_COST);
        requireResources(player, ResourceType.PATENT, GameConstants.PARTNERSHIP_PATENT_COST);

        player.removeResource(ResourceType.CAPITAL, GameConstants.PARTNERSHIP_CAPITAL_COST);
        player.removeResource(ResourceType.PATENT, GameConstants.PARTNERSHIP_PATENT_COST);

        edge.setPartnership(player);
        player.addStructure(edge.getStructure());
        addLog(player.getName() + " built a Partnership.");

        updateLongestNetwork();
    }

    private void requireResources(Player player, ResourceType type, int amount) throws InsufficientResourcesException {
        if (!player.canAfford(type, amount)) {
            throw new InsufficientResourcesException(
                    player.getName() + " needs " + amount + " " + type + " (has " + player.getResourceCount(type) + ").");
        }
    }

    // ------------------------------------------------------------------
    // Longest network
    // ------------------------------------------------------------------

    private void updateLongestNetwork() {
        for (Player player : players) {
            int length = computeLongestChain(player);
            if (length >= GameConstants.LONGEST_NETWORK_MIN_LENGTH && length > longestNetworkLength) {
                if (longestNetworkHolder != null) {
                    longestNetworkHolder.setHasLongestNetwork(false);
                }
                longestNetworkHolder = player;
                longestNetworkLength = length;
                player.setHasLongestNetwork(true);
                addLog(player.getName() + " now holds the longest network (" + length + " partnerships)!");
            }
        }
    }

    private int computeLongestChain(Player player) {
        List<Edge> ownedEdges = new ArrayList<>();
        for (int r = 0; r < GameMap.VERTEX_SIZE; r++) {
            for (int c = 0; c < GameMap.GRID_SIZE; c++) {
                Edge e = gameMap.getHorizontalEdge(r, c);
                if (e != null && e.hasPartnership() && e.getOwner() == player) ownedEdges.add(e);
            }
        }
        for (int r = 0; r < GameMap.GRID_SIZE; r++) {
            for (int c = 0; c < GameMap.VERTEX_SIZE; c++) {
                Edge e = gameMap.getVerticalEdge(r, c);
                if (e != null && e.hasPartnership() && e.getOwner() == player) ownedEdges.add(e);
            }
        }
        if (ownedEdges.isEmpty()) return 0;

        int best = 0;
        for (Edge startEdge : ownedEdges) {
            for (Vertex start : gameMap.getEndpoints(startEdge)) {
                Set<Edge> visited = new HashSet<>();
                best = Math.max(best, dfsLongestChain(start, player, visited, ownedEdges));
            }
        }
        return best;
    }

    private int dfsLongestChain(Vertex current, Player player, Set<Edge> visited, List<Edge> ownedEdges) {
        int best = 0;
        for (Edge edge : gameMap.getAdjacentEdges(current)) {
            if (!ownedEdges.contains(edge) || visited.contains(edge)) continue;
            visited.add(edge);
            Vertex[] endpoints = gameMap.getEndpoints(edge);
            Vertex next = endpoints[0] == current ? endpoints[1] : endpoints[0];
            int depth = 1 + dfsLongestChain(next, player, visited, ownedEdges);
            best = Math.max(best, depth);
            visited.remove(edge);
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Turn end / win condition
    // ------------------------------------------------------------------

    public synchronized Player checkWinner() {
        for (Player p : players) {
            if (p.getVictoryPoints() >= GameConstants.WINNING_SCORE) {
                return p;
            }
        }
        return null;
    }

    public synchronized void endTurn() throws IllegalGameActionException {
        ensureMainPhase();

        // Add this check to strictly enforce mandatory dice rolls
        if (!rolledThisTurn) {
            throw new IllegalGameActionException("You must roll the dice before ending your turn.");
        }

        market.advanceRound();
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        rolledThisTurn = false;
        addLog("Turn passed to " + getCurrentPlayer().getName() + ".");
    }

    // ------------------------------------------------------------------
    // AI turn execution on a background thread
    // ------------------------------------------------------------------

    private synchronized ExecutorService getExecutor() {
        if (aiExecutor == null || aiExecutor.isShutdown()) {
            aiExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ai-turn-thread");
                t.setDaemon(true);
                return t;
            });
        }
        return aiExecutor;
    }

    /**
     * Runs one AI player's turn on a background thread so a slow AI
     * decision never freezes the console's input loop. All state
     * mutation still goes through this class's `synchronized` methods,
     * so it stays safe even if called while the console thread is also
     * reading/printing. When the turn finishes, onComplete runs.
     */


    public void playAITurnAsync(Runnable onComplete) {

        getExecutor().submit(() -> {

            try {

                Player player = getCurrentPlayer();

                switch (player.getAIType()) {

                    case ADVANCED:
                        AdvancedAIStrategy.playTurn(this, random);
                        break;

                    case NORMAL:
                        AIStrategy.playTurn(this, random);
                        break;

                    default:
                        break;
                }

            } catch (Exception e) {

                addLog("AI encountered an error: " + e.getMessage());

            } finally {

                if (onComplete != null)
                    onComplete.run();

            }

        });

    }

    public void shutdownAI() {
        if (aiExecutor != null) {
            aiExecutor.shutdownNow();
        }
    }

    /** Small helper so we don't need a checked/unchecked exception mismatch above. */
    private static class IllegalStateExceptionWrapper extends InvalidPlacementException {
        IllegalStateExceptionWrapper(String message) { super(message); }
    }
}
