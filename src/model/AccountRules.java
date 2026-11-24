package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class AccountRules {
    public static final BigDecimal INVESTMENT_MINIMUM = new BigDecimal("500.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("999999999999.99");

    private AccountRules() {}

    public static boolean allowsWithdrawal(String accountType) {
        return "INVESTMENT".equals(accountType) || "CHEQUE".equals(accountType);
    }

    public static BigDecimal monthlyRate(String accountType) {
        return switch (accountType.toUpperCase(Locale.ROOT)) {
            case "SAVINGS" -> new BigDecimal("0.0005");
            case "INVESTMENT" -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
    }

    public static BigDecimal monthlyInterest(String accountType, BigDecimal balance) {
        return balance.multiply(monthlyRate(accountType)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean canOpenCheque(boolean individual, EmployeeDetails details) {
        return individual && details != null && details.verifyEmployment();
    }

    public static boolean meetsInvestmentMinimum(BigDecimal deposit) {
        return deposit.compareTo(INVESTMENT_MINIMUM) >= 0;
    }

    public static BigDecimal transactionBalance(String accountType, BigDecimal balance,
                                                BigDecimal amount, boolean withdrawal) {
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2
                || amount.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException(
                    "Enter a positive amount with up to two decimal places");
        }
        if (withdrawal && !allowsWithdrawal(accountType)) {
            throw new IllegalArgumentException("Savings accounts do not allow withdrawals");
        }

        BigDecimal result = withdrawal ? balance.subtract(amount) : balance.add(amount);
        if (result.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Account balance exceeds the supported limit");
        }
        return result;
    }
}