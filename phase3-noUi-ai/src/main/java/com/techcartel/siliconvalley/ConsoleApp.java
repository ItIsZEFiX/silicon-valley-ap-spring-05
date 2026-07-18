// ConsoleApp.java  (updated entry point)
package com.techcartel.siliconvalley;

import com.techcartel.siliconvalley.view.MainMenu;
import java.util.Scanner;

public class ConsoleApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        new MainMenu(scanner).show();
    }
}
