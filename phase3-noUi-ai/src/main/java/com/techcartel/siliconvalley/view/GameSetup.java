package com.techcartel.siliconvalley.view;

import com.techcartel.siliconvalley.controller.GameController;
import java.util.Scanner;

public class GameSetup {
    private final Scanner scanner;

    public GameSetup(Scanner scanner) {
        this.scanner = scanner;
    }

    public void initiate() {
        System.out.println("\n--- New Game Setup ---");
        System.out.print("Enter number of human players (1-4): ");
        int humans = Integer.parseInt(scanner.nextLine());

        System.out.print("Enter number of AI competitors: ");
        int ais = Integer.parseInt(scanner.nextLine());

        // اینجا GameController را مقداردهی اولیه می‌کنیم
        System.out.println("Initializing Game Engine...");
        // GameController controller = new GameController(humans, ais);
        // new TerminalUI(controller, scanner).run();
    }
}
