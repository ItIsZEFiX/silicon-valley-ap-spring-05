package com.techcartel.siliconvalley.exception;

/**
 * Thrown when a player does not have enough resources to pay for
 * a build, upgrade, or market purchase.
 */
public class InsufficientResourcesException extends GameException {
    public InsufficientResourcesException(String message) {
        super(message);
    }
}
