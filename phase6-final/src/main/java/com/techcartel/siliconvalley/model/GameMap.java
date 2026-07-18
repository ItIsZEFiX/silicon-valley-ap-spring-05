package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.GameConstants;
import com.techcartel.siliconvalley.util.ResourceType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameMap implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int GRID_SIZE = 5;
    public static final int VERTEX_SIZE = 6;

    private final Sector[][] sectors;
    private final Vertex[][] vertices;
    private final Edge[][] horizontalEdges;
    private final Edge[][] verticalEdges;

    public GameMap() {
        this.sectors = new Sector[GRID_SIZE][GRID_SIZE];
        this.vertices = new Vertex[VERTEX_SIZE][VERTEX_SIZE];
        this.horizontalEdges = new Edge[VERTEX_SIZE][GRID_SIZE];
        this.verticalEdges = new Edge[GRID_SIZE][VERTEX_SIZE];

        initializeVerticesAndEdges();
        generateSectors();
    }

    private void initializeVerticesAndEdges() {
        for (int r = 0; r < VERTEX_SIZE; r++) {
            for (int c = 0; c < VERTEX_SIZE; c++) {
                vertices[r][c] = new Vertex(r, c);
            }
        }

        for (int r = 0; r < VERTEX_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                horizontalEdges[r][c] = new Edge(r, c, true);
            }
        }

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < VERTEX_SIZE; c++) {
                verticalEdges[r][c] = new Edge(r, c, false);
            }
        }
    }

    private void generateSectors() {
        List<ResourceType> resources = new ArrayList<>();
        resources.add(ResourceType.NONE);

        for (int i = 0; i < 5; i++) {
            resources.add(ResourceType.TALENT);
            resources.add(ResourceType.CLOUD);
            resources.add(ResourceType.PATENT);
            resources.add(ResourceType.DATA);

            // Only add 4 Capital sectors so the total perfectly hits 24

            if (i < 4) {
                resources.add(ResourceType.CAPITAL);
            }
        }

        Collections.shuffle(resources);

        List<Integer> numbers = new ArrayList<>();
        int[] standardNumbers = {
                2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12,
                3, 4, 5, 8, 9, 10
        };
        for (int num : standardNumbers) {
            numbers.add(num);
        }
        Collections.shuffle(numbers);

        int numberIndex = 0;
        int resourceIndex = 0;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {

                ResourceType type = resources.get(resourceIndex++);

                if (type == ResourceType.NONE)
                    sectors[r][c] = new Sector(r, c, type, 0);
                else
                    sectors[r][c] = new Sector(r, c, type, numbers.get(numberIndex++));

            }
        }
    }

    public Sector getSector(int r, int c) {
        if (r >= 0 && r < GRID_SIZE && c >= 0 && c < GRID_SIZE) {
            return sectors[r][c];
        }
        return null;
    }

    public Vertex getVertex(int r, int c) {
        if (r >= 0 && r < VERTEX_SIZE && c >= 0 && c < VERTEX_SIZE) {
            return vertices[r][c];
        }
        return null;
    }

    public Edge getHorizontalEdge(int r, int c) {
        if (r >= 0 && r < VERTEX_SIZE && c >= 0 && c < GRID_SIZE) {
            return horizontalEdges[r][c];
        }
        return null;
    }

    public Edge getVerticalEdge(int r, int c) {
        if (r >= 0 && r < GRID_SIZE && c >= 0 && c < VERTEX_SIZE) {
            return verticalEdges[r][c];
        }
        return null;
    }

    /**
     * Uses Breadth-First Search (BFS) to verify if a target Vertex is connected
     * to a player's existing network via an unbroken chain of their owned Partnerships.
     */
    public boolean isConnectedToPlayerNetwork(Vertex target, Player player) {
        // 1. Initialization for BFS tracking
        java.util.Queue<Vertex> queue = new java.util.LinkedList<>();
        boolean[][] visited = new boolean[VERTEX_SIZE][VERTEX_SIZE];

        // Start the search right from our target corner
        queue.add(target);
        visited[target.getRow()][target.getCol()] = true;

        while (!queue.isEmpty()) {
            Vertex current = queue.poll();
            int r = current.getRow();
            int c = current.getCol();

            // Success Condition: If we hit an MVP or Unicorn owned by this player, a valid path exists!
            if (current.hasBuilding() && current.getOwner() == player) {
                return true;
            }

            // 2. Traversal Logic: Explore all 4 cardinal directions across owned edges

            // Check Left: Horizontal Edge [r][c-1] leading to Vertex [r][c-1]
            if (c > 0) {
                Edge leftEdge = getHorizontalEdge(r, c - 1);
                if (leftEdge != null && leftEdge.hasPartnership() && leftEdge.getOwner() == player) {
                    Vertex neighbor = getVertex(r, c - 1);
                    if (!visited[r][c - 1]) {
                        visited[r][c - 1] = true;
                        queue.add(neighbor);
                    }
                }
            }

            // Check Right: Horizontal Edge [r][c] leading to Vertex [r][c+1]
            if (c < GRID_SIZE) {
                Edge rightEdge = getHorizontalEdge(r, c);
                if (rightEdge != null && rightEdge.hasPartnership() && rightEdge.getOwner() == player) {
                    Vertex neighbor = getVertex(r, c + 1);
                    if (!visited[r][c + 1]) {
                        visited[r][c + 1] = true;
                        queue.add(neighbor);
                    }
                }
            }

            // Check Up: Vertical Edge [r-1][c] leading to Vertex [r-1][c]
            if (r > 0) {
                Edge topEdge = getVerticalEdge(r - 1, c);
                if (topEdge != null && topEdge.hasPartnership() && topEdge.getOwner() == player) {
                    Vertex neighbor = getVertex(r - 1, c);
                    if (!visited[r - 1][c]) {
                        visited[r - 1][c] = true;
                        queue.add(neighbor);
                    }
                }
            }

            // Check Down: Vertical Edge [r][c] leading to Vertex [r+1][c]
            if (r < GRID_SIZE) {
                Edge bottomEdge = getVerticalEdge(r, c);
                if (bottomEdge != null && bottomEdge.hasPartnership() && bottomEdge.getOwner() == player) {
                    Vertex neighbor = getVertex(r + 1, c);
                    if (!visited[r + 1][c]) {
                        visited[r + 1][c] = true;
                        queue.add(neighbor);
                    }
                }
            }
        }

        return false;
    }

    // ------------------------------------------------------------------
    // Placement-rule helpers (distance rule + connection rule)
    // ------------------------------------------------------------------

    /** All geometric neighbor vertices of (r,c), regardless of edge ownership. */
    public List<Vertex> getAdjacentVertices(Vertex v) {
        List<Vertex> neighbors = new ArrayList<>();
        int r = v.getRow();
        int c = v.getCol();
        if (c > 0) neighbors.add(getVertex(r, c - 1));
        if (c < VERTEX_SIZE - 1) neighbors.add(getVertex(r, c + 1));
        if (r > 0) neighbors.add(getVertex(r - 1, c));
        if (r < VERTEX_SIZE - 1) neighbors.add(getVertex(r + 1, c));
        return neighbors;
    }

    /** The (up to 4) edges directly touching a vertex. */
    public List<Edge> getAdjacentEdges(Vertex v) {
        List<Edge> edges = new ArrayList<>();
        int r = v.getRow();
        int c = v.getCol();
        if (c > 0) edges.add(getHorizontalEdge(r, c - 1));
        if (c < GRID_SIZE) edges.add(getHorizontalEdge(r, c));
        if (r > 0) edges.add(getVerticalEdge(r - 1, c));
        if (r < GRID_SIZE) edges.add(getVerticalEdge(r, c));
        edges.removeIf(e -> e == null);
        return edges;
    }

    /** The two vertices at the endpoints of an edge. */
    public Vertex[] getEndpoints(Edge edge) {
        int r = edge.getRow();
        int c = edge.getCol();
        if (edge.isHorizontal()) {
            return new Vertex[] { getVertex(r, c), getVertex(r, c + 1) };
        } else {
            return new Vertex[] { getVertex(r, c), getVertex(r + 1, c) };
        }
    }

    /** The (up to 4) sectors surrounding a vertex (its corners touch these sectors). */
    public List<Sector> getAdjacentSectors(Vertex v) {
        List<Sector> result = new ArrayList<>();
        int r = v.getRow();
        int c = v.getCol();
        Sector[] candidates = {
                getSector(r - 1, c - 1),
                getSector(r - 1, c),
                getSector(r, c - 1),
                getSector(r, c)
        };
        for (Sector s : candidates) {
            if (s != null) result.add(s);
        }
        return result;
    }

    /** The 4 vertices at the corners of a sector. */
    public List<Vertex> getVerticesForSector(Sector sector) {
        int r = sector.getRow();
        int c = sector.getCol();
        List<Vertex> result = new ArrayList<>();
        result.add(getVertex(r, c));
        result.add(getVertex(r, c + 1));
        result.add(getVertex(r + 1, c));
        result.add(getVertex(r + 1, c + 1));
        return result;
    }

    /**
     * Distance rule: a vertex is legal for a brand-new MVP only if it and
     * none of its direct neighbors already have a building
     * (i.e. at least GameConstants.MIN_VERTEX_DISTANCE edges from any
     * other company).
     */
    public boolean respectsDistanceRule(Vertex vertex) {
        if (vertex.hasBuilding()) return false;
        for (Vertex neighbor : getAdjacentVertices(vertex)) {
            if (neighbor != null && neighbor.hasBuilding()) return false;
        }
        return true;
    }

    /**
     * Full legality check for building a new MVP.
     * During setup, only the distance rule applies (free placement).
     * After setup, the vertex must also touch one of the player's own
     * Partnerships.
     */
    public boolean isValidMVPPlacement(Vertex vertex, Player player, boolean setupPhase) {
        if (vertex == null) return false;
        if (!respectsDistanceRule(vertex)) return false;
        if (setupPhase) return true;

        for (Edge e : getAdjacentEdges(vertex)) {
            if (e.hasPartnership() && e.getOwner() == player) return true;
        }
        return false;
    }

    /**
     * Full legality check for building a new Partnership.
     * During setup it just needs to be free and touch the vertex the
     * player is currently allowed to connect from (validated by the
     * controller). After setup it must connect to one of the player's
     * own structures (MVP/Unicorn) or existing Partnerships.
     */
    public boolean isValidPartnershipPlacement(Edge edge, Player player, boolean setupPhase) {
        if (edge == null || edge.hasPartnership()) return false;
        if (setupPhase) return true;

        Vertex[] endpoints = getEndpoints(edge);
        for (Vertex endpoint : endpoints) {
            if (endpoint == null) continue;
            if (endpoint.hasBuilding() && endpoint.getOwner() == player) return true;
            for (Edge adj : getAdjacentEdges(endpoint)) {
                if (adj != edge && adj.hasPartnership() && adj.getOwner() == player) return true;
            }
        }
        return false;
    }
}
