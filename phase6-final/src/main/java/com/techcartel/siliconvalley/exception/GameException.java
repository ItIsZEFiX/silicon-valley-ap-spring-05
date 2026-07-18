package com.techcartel.siliconvalley.exception;

/**
 * Base class for every custom, game-specific exception.
 * Kept as a checked exception so illegal actions can never silently
 * crash the game -- callers are forced to handle them.
 */
public class GameException extends Exception {
    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
