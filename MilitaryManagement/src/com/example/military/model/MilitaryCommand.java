package com.example.military.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MilitaryCommand extends MilitaryPerson {
    private String militaryDistrict;   // название округа
    private String position;           // должность
    private int yearsOfService;        // выслуга лет
    private double allowance;          // сумма надбавки

    public MilitaryCommand(String lastName, String company, String rank,
                           LocalDate birthDate, LocalDate enlistmentDate,
                           String unit, double salary,
                           String militaryDistrict, String position,
                           int yearsOfService, double allowance) {
        super(lastName, company, rank, birthDate, enlistmentDate, unit, salary);
        this.militaryDistrict = militaryDistrict;
        this.position = position;
        this.yearsOfService = yearsOfService;
        this.allowance = allowance;
    }

    // Геттеры и сеттеры
    public String getMilitaryDistrict() {
        return militaryDistrict;
    }

    public void setMilitaryDistrict(String militaryDistrict) {
        this.militaryDistrict = militaryDistrict;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getYearsOfService() {
        return yearsOfService;
    }

    public void setYearsOfService(int yearsOfService) {
        this.yearsOfService = yearsOfService;
    }

    public double getAllowance() {
        return allowance;
    }

    public void setAllowance(double allowance) {
        this.allowance = allowance;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return super.toString() + "\n" +
                "=====================================\n" +
                "Органы военного управления:\n" +
                "  Военный округ: " + militaryDistrict + "\n" +
                "  Должность: " + position + "\n" +
                "  Выслуга лет: " + yearsOfService + "\n" +
                "  Надбавка: " + allowance + " руб.\n" +
                "=====================================";
    }
}