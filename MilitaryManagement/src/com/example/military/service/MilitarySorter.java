package com.example.military.service;

import com.example.military.model.MilitaryPerson;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MilitarySorter {

    // Сортировка по фамилии (по возрастанию - А→Я)
    public static void sortByLastNameAscending(List<MilitaryPerson> list) {
        Collections.sort(list, new Comparator<MilitaryPerson>() {
            @Override
            public int compare(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                String lastName1 = p1.getLastName();
                String lastName2 = p2.getLastName();

                if (lastName1 == null && lastName2 == null) return 0;
                if (lastName1 == null) return 1;
                if (lastName2 == null) return -1;

                return lastName1.compareTo(lastName2); // А→Я
            }
        });
    }

    // Сортировка по фамилии (по убыванию - Я→А)
    public static void sortByLastNameDescending(List<MilitaryPerson> list) {
        Collections.sort(list, new Comparator<MilitaryPerson>() {
            @Override
            public int compare(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                String lastName1 = p1.getLastName();
                String lastName2 = p2.getLastName();

                if (lastName1 == null && lastName2 == null) return 0;
                if (lastName1 == null) return 1;
                if (lastName2 == null) return -1;

                return lastName2.compareTo(lastName1); // Я→А (обратный порядок)
            }
        });
    }

    // Сортировка по зарплате (по возрастанию)
    public static void sortBySalaryAscending(List<MilitaryPerson> list) {
        Collections.sort(list, new Comparator<MilitaryPerson>() {
            @Override
            public int compare(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                return Double.compare(p1.getSalary(), p2.getSalary());
            }
        });
    }

    // Сортировка по зарплате (по убыванию)
    public static void sortBySalaryDescending(List<MilitaryPerson> list) {
        Collections.sort(list, new Comparator<MilitaryPerson>() {
            @Override
            public int compare(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                return Double.compare(p2.getSalary(), p1.getSalary());
            }
        });
    }

    // Комбинированная: по фамилии (А→Я), затем по зарплате (возрастание)
    public static void sortByLastNameAscThenSalary(List<MilitaryPerson> list) {
        Collections.sort(list, new Comparator<MilitaryPerson>() {
            @Override
            public int compare(MilitaryPerson p1, MilitaryPerson p2) {
                // Сначала сравниваем по фамилии (А→Я)
                int lastNameCompare = compareByLastNameAsc(p1, p2);
                if (lastNameCompare != 0) {
                    return lastNameCompare;
                }
                // Если фамилии одинаковые, сравниваем по зарплате
                return compareBySalaryAsc(p1, p2);
            }

            private int compareByLastNameAsc(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                String lastName1 = p1.getLastName();
                String lastName2 = p2.getLastName();

                if (lastName1 == null && lastName2 == null) return 0;
                if (lastName1 == null) return 1;
                if (lastName2 == null) return -1;

                return lastName1.compareTo(lastName2);
            }

            private int compareBySalaryAsc(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                return Double.compare(p1.getSalary(), p2.getSalary());
            }
        });
    }

    // Комбинированная: по фамилии (Я→А), затем по зарплате (убывание)
    public static void sortByLastNameDescThenSalary(List<MilitaryPerson> list) {
        Collections.sort(list, new Comparator<MilitaryPerson>() {
            @Override
            public int compare(MilitaryPerson p1, MilitaryPerson p2) {
                // Сначала сравниваем по фамилии (Я→А)
                int lastNameCompare = compareByLastNameDesc(p1, p2);
                if (lastNameCompare != 0) {
                    return lastNameCompare;
                }
                // Если фамилии одинаковые, сравниваем по зарплате (убывание)
                return compareBySalaryDesc(p1, p2);
            }

            private int compareByLastNameDesc(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                String lastName1 = p1.getLastName();
                String lastName2 = p2.getLastName();

                if (lastName1 == null && lastName2 == null) return 0;
                if (lastName1 == null) return 1;
                if (lastName2 == null) return -1;

                return lastName2.compareTo(lastName1);
            }

            private int compareBySalaryDesc(MilitaryPerson p1, MilitaryPerson p2) {
                if (p1 == null && p2 == null) return 0;
                if (p1 == null) return 1;
                if (p2 == null) return -1;

                return Double.compare(p2.getSalary(), p1.getSalary());
            }
        });
    }

    // Вспомогательный метод для печати отсортированного списка
    public static void printSortedList(List<MilitaryPerson> list, String sortType) {
        System.out.println("\n========== СПИСОК, ОТСОРТИРОВАННЫЙ " + sortType + " ==========");
        if (list.isEmpty()) {
            System.out.println("Список пуст.");
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            MilitaryPerson p = list.get(i);
            System.out.println("\n----- " + (i + 1) + ". " +
                    (p.getLastName() != null ? p.getLastName() : "БЕЗ ФАМИЛИИ") +
                    " (Зарплата: " + p.getSalary() + " руб.) -----");
            System.out.println(p);
        }
        System.out.println("\n========== ВСЕГО: " + list.size() + " человек ==========\n");
    }
}