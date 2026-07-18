package com.techcartel.siliconvalley.exception;

/**
 * Thrown when an action is attempted out of turn, in the wrong game
 * phase (e.g. trying to build before rolling), or on a target that
 * does not make logical sense (e.g. upgrading a vertex with no MVP).
 */
public class IllegalGameActionException extends GameException {
    public IllegalGameActionException(String message) {
        super(message);
    }
}
