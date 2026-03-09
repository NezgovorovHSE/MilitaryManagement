package com.example.military.repository;

import com.example.military.model.MilitaryPerson;
import java.util.ArrayList;
import java.util.List;

public class MilitaryList {
    private List<MilitaryPerson> personnel;

    // Конструктор
    public MilitaryList() {
        personnel = new ArrayList<>();
    }

    // Метод для добавления военнослужащего
    public void addMilitary(MilitaryPerson person) {
        if (person != null) {
            personnel.add(person);
            System.out.println("Военнослужащий успешно добавлен в список.");
        } else {
            System.out.println("Ошибка: объект не может быть null.");
        }
    }

    // Метод для получения всего списка
    public List<MilitaryPerson> getAll() {
        return new ArrayList<>(personnel); // Возвращаем копию списка
    }

    // Метод для получения количества элементов
    public int getSize() {
        return personnel.size();
    }

    // Метод для проверки, пуст ли список
    public boolean isEmpty() {
        return personnel.isEmpty();
    }

    // Метод для печати всех военнослужащих
    public void printAll() {
        if (personnel.isEmpty()) {
            System.out.println("Список военнослужащих пуст.");
            return;
        }

        System.out.println("\n========== СПИСОК ВОЕННОСЛУЖАЩИХ ==========");
        for (int i = 0; i < personnel.size(); i++) {
            System.out.println("\n----- Военнослужащий №" + (i + 1) + " -----");
            System.out.println(personnel.get(i));
        }
        System.out.println("\n========== ВСЕГО: " + personnel.size() + " человек ==========\n");
    }

    // Метод для очистки списка
    public void clear() {
        personnel.clear();
        System.out.println("Список очищен.");
    }

    // Метод для получения военнослужащего по индексу
    public MilitaryPerson get(int index) {
        if (index >= 0 && index < personnel.size()) {
            return personnel.get(index);
        }
        return null;
    }
}