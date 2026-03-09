package com.example.military.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MilitaryPerson {
    private String lastName;           // фамилия
    private String company;            // рота
    private String rank;               // звание
    private LocalDate birthDate;       // дата рождения
    private LocalDate enlistmentDate;  // дата поступления на службу
    private String unit;               // часть
    private double salary;             // зарплата
    private int id;

    public MilitaryPerson(String lastName, String company, String rank,
                          LocalDate birthDate, LocalDate enlistmentDate,
                          String unit, double salary) {
        this.lastName = lastName;
        this.company = company;
        this.rank = rank;
        this.birthDate = birthDate;
        this.enlistmentDate = enlistmentDate;
        this.unit = unit;
        this.salary = salary;
    }

    private int displayNumber; // только для отображения в таблице

    public int getDisplayNumber() {
        return displayNumber;
    }

    public void setDisplayNumber(int displayNumber) {
        this.displayNumber = displayNumber;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getEnlistmentDate() {
        return enlistmentDate;
    }

    public void setEnlistmentDate(LocalDate enlistmentDate) {
        this.enlistmentDate = enlistmentDate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    // Новый геттер и сеттер для salary
    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("Военнослужащий:\n");
        sb.append("  Фамилия: ").append(lastName != null ? lastName : "не указана").append("\n");
        sb.append("  Рота: ").append(company != null ? company : "не указана").append("\n");
        sb.append("  Звание: ").append(rank != null ? rank : "не указано").append("\n");
        sb.append("  Дата рождения: ").append(birthDate != null ? birthDate.format(formatter) : "не указана").append("\n");
        sb.append("  Дата поступления на службу: ").append(enlistmentDate != null ? enlistmentDate.format(formatter) : "не указана").append("\n");
        sb.append("  Часть: ").append(unit != null ? unit : "не указана").append("\n");
        sb.append("  Зарплата: ").append(salary).append(" руб.\n");
        sb.append("=====================================");
        return sb.toString();
    }
}