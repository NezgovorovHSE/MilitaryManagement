package com.example.military.client;

public class ClientMain {
    public static void main(String[] args) {
        System.out.println("==============");
        System.out.println("ЗАПУСК КЛИЕНТА");
        System.out.println("==============");

        try {
            ClientMenu menu = new ClientMenu();
            menu.start();
        } catch (Exception e) {
            System.out.println("❌ Ошибка при запуске меню: " + e.getMessage());
            e.printStackTrace();
        }
    }
}