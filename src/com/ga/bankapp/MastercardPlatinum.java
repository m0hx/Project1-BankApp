package com.ga.bankapp;

public class MastercardPlatinum extends Mastercard {

    @Override
    public double getWithdrawLimitPerDay() {
        return 20000.0; // $20,000
    }

    @Override
    public double getTransferLimitPerDay() {
        return 40000.0; // $40,000
    }

    @Override
    public double getTransferLimitPerDayOwnAccount() {
        return 80000.0; // $80,000
    }

    @Override
    public double getDepositLimitPerDay() {
        return 100000.0; // $100,000
    }

    @Override
    public double getDepositLimitPerDayOwnAccount() {
        return 200000.0; // $200,000
    }

    @Override
    public String getCardType() {
        return "Platinum";
    }
}
