package com.techcartel.siliconvalley.view;

import com.techcartel.siliconvalley.controller.GameController;
import com.techcartel.siliconvalley.model.Player;
import com.techcartel.siliconvalley.util.FounderRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GameLauncher {
    private final Scanner scanner;

    public GameLauncher(Scanner scanner) {
        this.scanner = scanner;
    }

    public void startSinglePlayer() {
        List<Player> players = new ArrayList<>();
        // بازیکن انسانی
        players.add(new Player("Player 1", FounderRole.THE_HACKER_CEO));
        // هوش مصنوعی با انتخاب سطح دشواری
        AISelector.Difficulty difficulty = AISelector.select(scanner, "AI Competitor");
        players.add(new Player("Silicon Valley Bot", FounderRole.THE_HACKER_CEO, true));
        // نکته: منطق اعمال Difficulty باید در لایه Controller یا AI Strategy هندل شود.

        launch(players);
    }

    public void startSkirmish() {
        System.out.println("\n--- Skirmish Mode Setup ---");
        int totalPlayers = readInt("Enter total number of players (2-4): ", 2, 4);
        int humanCount = readInt("How many human players? (0-" + totalPlayers + "): ", 0, totalPlayers);

        List<Player> players = new ArrayList<>();

        for (int i = 1; i <= humanCount; i++) {
            players.add(new Player("Human " + i, FounderRole.values()[i % FounderRole.values().length], false));
        }

        for (int i = 1; i <= (totalPlayers - humanCount); i++) {
            players.add(new Player("AI Bot " + i, FounderRole.values()[(humanCount + i) % FounderRole.values().length], true));
        }

        launch(players);
    }

    private void launch(List<Player> players) {
        System.out.println("\nInitializing Game Engine...");
        // ایجاد کنترلر با لیست بازیکنان نهایی
        GameController controller = new GameController(players);

        // تزریق کنترلر به UI به جای ایجاد مجدد در داخل آن
        ConsoleUI gui = new ConsoleUI(controller, scanner);
        gui.start();
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid input. Please enter a value between " + min + " and " + max + ".");
        }
    }
}
