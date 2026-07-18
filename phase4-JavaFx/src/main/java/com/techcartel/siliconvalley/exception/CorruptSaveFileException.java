package com.techcartel.siliconvalley.exception;

/**
 * Thrown when a save file is missing, unreadable, truncated, or was
 * produced by an incompatible version of the game.
 */
public class CorruptSaveFileException extends GameException {
    public CorruptSaveFileException(String message) {
        super(message);
    }

    public CorruptSaveFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
