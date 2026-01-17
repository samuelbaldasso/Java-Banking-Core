package com.sbaldasso.java_banking_core.domain.valueobject;

/**
 * Account type following the standard accounting equation.
 * 
 * Assets = Liabilities + Equity
 * Revenue - Expenses = Net Income (part of Equity)
 */
public enum AccountType {
    /**
     * Assets - Resources owned by the entity
     * Normal balance: DEBIT
     * Examples: Cash, Accounts Receivable, Customer Accounts
     */
    ASSET,

    /**
     * Liabilities - Obligations owed to others
     * Normal balance: CREDIT
     * Examples: Loans, Accounts Payable
     */
    LIABILITY,

    /**
     * Equity - Owner's stake in the entity
     * Normal balance: CREDIT
     * Examples: Capital, Retained Earnings
     */
    EQUITY,

    /**
     * Revenue - Income from operations
     * Normal balance: CREDIT
     * Examples: Interest Income, Fee Revenue
     */
    REVENUE,

    /**
     * Expense - Costs incurred in operations
     * Normal balance: DEBIT
     * Examples: Operating Costs, Processing Fees
     */
    EXPENSE;

    /**
     * Determines if a debit increases this account type's balance.
     * 
     * @return true if debit increases balance, false otherwise
     */
    public boolean isDebitIncrease() {
        return this == ASSET || this == EXPENSE;
    }

    /**
     * Determines if a credit increases this account type's balance.
     * 
     * @return true if credit increases balance, false otherwise
     */
    public boolean isCreditIncrease() {
        return this == LIABILITY || this == EQUITY || this == REVENUE;
    }
}
