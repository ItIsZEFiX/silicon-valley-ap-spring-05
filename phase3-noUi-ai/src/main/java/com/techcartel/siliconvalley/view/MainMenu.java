package com.techcartel.siliconvalley.view;

import java.util.Scanner;

public class MainMenu {
    private final Scanner scanner;

    public MainMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        while (true) {
            printHeader();
            printMenu();

            int choice = readInt(1, 3);

            switch (choice) {
                case 1 -> new GameLauncher(scanner).startSinglePlayer();
                case 2 -> new GameLauncher(scanner).startSkirmish();
                case 3 -> {
                    System.out.println("\nGoodbye. Thanks for playing Silicon Valley: The Tech Cartel.");
                    return;
                }
            }
        }
    }

    private void printHeader() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║      Silicon Valley: The Tech Cartel        ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║        Terminal Strategy Game Interface      ║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    private void printMenu() {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  1) Single Player vs AI                      ║");
        System.out.println("║  2) Skirmish (Human + AI Mix)                ║");
        System.out.println("║  3) Exit                                     ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.print("Select an option (1-3): ");
    }

    private int readInt(int min, int max) {
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to re-prompt
            }

            System.out.print("Invalid input. Enter a number between " + min + " and " + max + ": ");
        }
    }
}
