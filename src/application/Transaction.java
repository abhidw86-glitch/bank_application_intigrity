package application;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Transaction {

    private int accountNumber;
    private String type;   
    private double amount;
    private LocalDateTime date;

    public Transaction(int accountNumber, String type, double amount) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.date = LocalDateTime.now();
    }

    public int getAccountNumber()     { return accountNumber; }
    public String getType()           { return type; }
    public double getAmount()         { return amount; }
    public LocalDateTime getDate()    { return date; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s]  Account #%d  —  %-8s  $%.2f",
                date.format(fmt), accountNumber, type, amount);
    }
}