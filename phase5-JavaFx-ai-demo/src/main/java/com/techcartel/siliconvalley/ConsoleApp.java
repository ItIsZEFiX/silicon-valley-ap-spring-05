package com.techcartel.siliconvalley;

import com.techcartel.siliconvalley.view.ConsoleUI;

/**
 * Entry point for the terminal/console version of the game.
 * Completely independent of GameApp/Launcher (JavaFX), which are left
 * untouched. Run this class directly to play in the terminal.
 */
public class ConsoleApp {
    public static void main(String[] args) {
        new ConsoleUI().start();
    }
}
