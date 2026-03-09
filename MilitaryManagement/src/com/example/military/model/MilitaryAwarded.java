package com.example.military.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MilitaryAwarded extends MilitaryPerson {
    private String awardName;           // название награды
    private double prize;                // премия
    private double allowance;             // сумма надбавки

    public MilitaryAwarded(String lastName, String company, String rank,
                           LocalDate birthDate, LocalDate enlistmentDate,
                           String unit, double salary,
                           String awardName, double prize, double allowance) {
        super(lastName, company, rank, birthDate, enlistmentDate, unit, salary);
        this.awardName = awardName;
        this.prize = prize;
        this.allowance = allowance;
    }

    // Геттеры и сеттеры
    public String getAwardName() {
        return awardName;
    }

    public void setAwardName(String awardName) {
        this.awardName = awardName;
    }

    public double getPrize() {
        return prize;
    }

    public void setPrize(double prize) {
        this.prize = prize;
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
                "Награжденный:\n" +
                "  Название награды: " + awardName + "\n" +
                "  Премия: " + prize + " руб.\n" +
                "  Надбавка: " + allowance + " руб.\n" +
                "=====================================";
    }
}