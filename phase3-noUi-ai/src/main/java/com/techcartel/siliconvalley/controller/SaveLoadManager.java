package com.techcartel.siliconvalley.controller;

import com.techcartel.siliconvalley.exception.CorruptSaveFileException;
import com.techcartel.siliconvalley.model.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles saving/loading the whole GameController state to disk.
 * File I/O always runs on its own background thread (never on the
 * console's main input thread) so a slow disk write never freezes
 * the game, satisfying the "save/load must run on a separate thread"
 * requirement even in a text UI.
 */
public final class SaveLoadManager {

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "save-load-thread");
        t.setDaemon(true);
        return t;
    });

    private SaveLoadManager() {}

    public interface SaveCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface LoadCallback {
        void onSuccess(GameController controller);
        void onFailure(Exception e);
    }

    public static void saveAsync(GameController controller, File file, SaveCallback callback) {
        IO_EXECUTOR.submit(() -> {
            try {
                save(controller, file);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    public static void loadAsync(File file, LoadCallback callback) {
        IO_EXECUTOR.submit(() -> {
            try {
                GameController controller = load(file);
                if (callback != null) callback.onSuccess(controller);
            } catch (Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    public static void save(GameController controller, File file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(controller);
        }
    }

    public static GameController load(File file) throws CorruptSaveFileException {
        if (!file.exists() || file.length() == 0) {
            throw new CorruptSaveFileException("Save file is missing or empty: " + file.getName());
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = in.readObject();
            if (!(obj instanceof GameController)) {
                throw new CorruptSaveFileException("Save file does not contain a valid game state.");
            }
            GameController controller = (GameController) obj;
            for (Player p : controller.getPlayers()) {
                p.ensurePlayerId();
            }
            return controller;
        } catch (StreamCorruptedException | InvalidClassException e) {
            throw new CorruptSaveFileException("Save file is corrupted or from an incompatible version.", e);
        } catch (ClassNotFoundException | IOException e) {
            throw new CorruptSaveFileException("Could not read save file: " + e.getMessage(), e);
        }
    }
}
