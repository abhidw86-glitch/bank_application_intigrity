package application;

public class SavingsAccount extends BankAccount {

    private double minimumBalance;

    public SavingsAccount(String holderName, double initialDeposit, double minimumBalance) {
        super(holderName, initialDeposit);
        this.minimumBalance = minimumBalance;
    }

    // Overridden withdraw — cannot drop below the minimum balance
    @Override
    public boolean withdraw(double amount) throws InsufficientBalanceException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        if ((balance - amount) < minimumBalance) {
            throw new InsufficientBalanceException(
                    "Withdrawal denied. Savings accounts must keep at least $"
                            + String.format("%.2f", minimumBalance));
        }
        balance -= amount;
        return true;
    }

    @Override
    public String displayAccountDetails() {
        return super.displayAccountDetails()
                + "\nType:      Savings"
                + "\nMin. Bal:  $" + String.format("%.2f", minimumBalance);
    }

    public double getMinimumBalance() { return minimumBalance; }
}