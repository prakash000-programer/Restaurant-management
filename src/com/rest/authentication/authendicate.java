package com.rest.authentication;

import java.util.Scanner;

public class authendicate {
    private final String password = "admin@123123";
    private int tries = 3;

    public boolean check_password() {
        Scanner scanner = new Scanner(System.in);
        while (tries > 0) {
            System.out.println("Enter password: ");
            String input = scanner.nextLine();
            if (input.equals(password)) {
                return true;
            } else {
                tries--;
                System.out.println("Incorrect password. Tries remaining: " + tries);
            }
        }
        scanner.close();
        return false;
    }

}
