package com.ga.bankapp;

public interface DebitCard {
    // Get daily withdraw limit
    double getWithdrawLimitPerDay();

    // Get daily transfer limit (to other customers)
    double getTransferLimitPerDay();

    // Get daily transfer limit (to own accounts)
    double getTransferLimitPerDayOwnAccount();

    // Get daily deposit limit (from external sources)
    double getDepositLimitPerDay();

    // Get daily deposit limit (to own accounts)
    double getDepositLimitPerDayOwnAccount();

    // Get card type name
    String getCardType();
}
