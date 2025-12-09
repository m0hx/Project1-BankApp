package com.ga.bankapp;

public class MastercardStandard extends Mastercard {
    @Override
    public double getWithdrawLimitPerDay() {
        return 5000.0; // $5,000
    }

    @Override
    public double getTransferLimitPerDay() {
        return 10000.0; // $10,000
    }

    @Override
    public double getTransferLimitPerDayOwnAccount() {
        return 20000.0; // $20,000
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
        return "Standard";
    }
}
