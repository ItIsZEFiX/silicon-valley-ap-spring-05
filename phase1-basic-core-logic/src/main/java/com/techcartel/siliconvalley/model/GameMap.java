package com.techcartel.siliconvalley.model;

import com.techcartel.siliconvalley.util.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameMap {

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

        for (int i = 0; i < 6; i++) {
            resources.add(ResourceType.TALENT);
            resources.add(ResourceType.CLOUD);
            resources.add(ResourceType.PATENT);
            resources.add(ResourceType.DATA);
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
}
