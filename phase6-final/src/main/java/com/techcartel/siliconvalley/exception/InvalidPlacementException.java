package com.techcartel.siliconvalley.exception;

/**
 * Thrown when a player attempts to build an MVP / Unicorn / Partnership
 * on a position that violates the placement rules (distance rule,
 * connection rule, already occupied, out of bounds, wrong phase, etc.)
 */
public class InvalidPlacementException extends GameException {
    public InvalidPlacementException(String message) {
        super(message);
    }
}
