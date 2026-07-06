package com.example.military.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MilitaryContract extends MilitaryPerson {
    private String contractPeriod;      // период договора
    private LocalDate contractDate;     // дата договора
    private String protocolNumber;      // номер протокола
    private double salary;              // сумма зарплаты (переопределяем поле)

    public MilitaryContract(String lastName, String company, String rank,
                            LocalDate birthDate, LocalDate enlistmentDate,
                            String unit, double salary,
                            String contractPeriod, LocalDate contractDate,
                            String protocolNumber) {
        super(lastName, company, rank, birthDate, enlistmentDate, unit, salary);
        this.contractPeriod = contractPeriod;
        this.contractDate = contractDate;
        this.protocolNumber = protocolNumber;
        this.salary = salary;
    }

    // Геттеры и сеттеры
    public String getContractPeriod() {
        return contractPeriod;
    }

    public void setContractPeriod(String contractPeriod) {
        this.contractPeriod = contractPeriod;
    }

    public LocalDate getContractDate() {
        return contractDate;
    }

    public void setContractDate(LocalDate contractDate) {
        this.contractDate = contractDate;
    }

    public String getProtocolNumber() {
        return protocolNumber;
    }

    public void setProtocolNumber(String protocolNumber) {
        this.protocolNumber = protocolNumber;
    }

    @Override
    public double getSalary() {
        return salary;
    }

    @Override
    public void setSalary(double salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return super.toString() + "\n" +
                "=====================================\n" +
                "Военная служба по контракту:\n" +
                "  Период договора: " + contractPeriod + "\n" +
                "  Дата договора: " + (contractDate != null ? contractDate.format(formatter) : "не указана") + "\n" +
                "  Номер протокола: " + protocolNumber + "\n" +
                "  Сумма зарплаты: " + salary + " руб.\n" +
                "=====================================";
    }
}