package application;

public class BankAccount {

    // Field visibility
    public int accountNumber;
    protected double balance;
    private String holderName;

    // Static variable
    private static int nextAccountNumber = 1001;

    public BankAccount(String holderName, double initialDeposit) {
        this.accountNumber = generateAccountNumber();
        this.holderName = holderName;
        this.balance = initialDeposit;
    }

    // Static method 
    public static int generateAccountNumber() {
        return nextAccountNumber++;
    }

    // Deposit with input validation
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        balance += amount;
    }

    // Base withdraw 
    public boolean withdraw(double amount) throws InsufficientBalanceException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(
                    "Insufficient funds. Available balance: $" + String.format("%.2f", balance));
        }
        balance -= amount;
        return true;
    }

    // Returns a formatted string of account details (subclasses extend this)
    public String displayAccountDetails() {
        return "Account #: " + accountNumber
                + "\nHolder:    " + holderName
                + "\nBalance:   $" + String.format("%.2f", balance);
    }

    // Getters
    public String getHolderName()  { return holderName; }
    public double getBalance()     { return balance; }
    public int getAccountNumber()  { return accountNumber; }

    @Override
    public String toString() {
        return "#" + accountNumber + " — " + holderName
                + " ($" + String.format("%.2f", balance) + ")";
    }
}