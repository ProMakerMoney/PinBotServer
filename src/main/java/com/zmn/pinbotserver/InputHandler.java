package com.zmn.pinbotserver;

import java.util.Scanner;

public class InputHandler {

    public static String getStringInput(String prompt) {
        Scanner scanner = new Scanner(System.in);
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public static int getIntInput(String prompt) {
        Scanner scanner = new Scanner(System.in);
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter an integer.");
            System.out.print(prompt);
            scanner.next();
        }
        return scanner.nextInt();
    }

    public static double getDoubleInput(String prompt) {
        Scanner scanner = new Scanner(System.in);
        System.out.print(prompt);
        while (!scanner.hasNextDouble()) {
            System.out.println("Invalid input. Please enter a double.");
            System.out.print(prompt);
            scanner.next();
        }
        return scanner.nextDouble();
    }
}