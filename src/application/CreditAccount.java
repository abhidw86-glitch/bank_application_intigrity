package application;

public class CreditAccount extends BankAccount {

    private double creditLimit;

    public CreditAccount(String holderName, double initialDeposit, double creditLimit) {
        super(holderName, initialDeposit);
        this.creditLimit = creditLimit;
    }

    // Overridden withdraw — allowed to go negative up to the credit limit
    @Override
    public boolean withdraw(double amount) throws InsufficientBalanceException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        if ((balance - amount) < -creditLimit) {
            throw new InsufficientBalanceException(
                    "Withdrawal exceeds credit limit of $"
                            + String.format("%.2f", creditLimit));
        }
        balance -= amount;
        return true;
    }

    @Override
    public String displayAccountDetails() {
        return super.displayAccountDetails()
                + "\nType:      Credit"
                + "\nLimit:     $" + String.format("%.2f", creditLimit);
    }

    public double getCreditLimit() { return creditLimit; }
}