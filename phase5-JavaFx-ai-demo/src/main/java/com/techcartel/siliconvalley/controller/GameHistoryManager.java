package com.techcartel.siliconvalley.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Stack;

public class GameHistoryManager {

    private final Stack<byte[]> undoStack = new Stack<>();
    private final Stack<byte[]> redoStack = new Stack<>();

    public void saveSnapshot(GameController currentState) {
        try {
            undoStack.push(serialize(currentState));
            redoStack.clear();
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to create game snapshot.");
            e.printStackTrace();
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public GameController undo(GameController currentState) throws Exception {
        if (!canUndo()) return currentState;

        // Save current state to redo, then pop and load the previous state
        redoStack.push(serialize(currentState));
        return deserialize(undoStack.pop());
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public GameController redo(GameController currentState) throws Exception {
        if (!canRedo()) return currentState;

        // Save current state to undo, then pop and load the next state
        undoStack.push(serialize(currentState));
        return deserialize(redoStack.pop());
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private byte[] serialize(GameController state) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(state);
        }
        return baos.toByteArray();
    }

    private GameController deserialize(byte[] bytes) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (GameController) ois.readObject();
        }
    }
}