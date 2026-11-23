package com.tdtu.mobilebanking.common.model;

public class MortgageAccount extends Account {
    public long principal;
    public double annualRate;
    public int termMonths;
    public String frequency;    // MONTHLY | BIWEEKLY
    public String startDate;    // ISO date string
    public long outstanding;

    public MortgageAccount() {
        this.type = "MORTGAGE";
    }
}
