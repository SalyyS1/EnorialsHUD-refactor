/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.trade;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TradeMoney {
    private TradeMoney() {
    }

    public static BigDecimal parse(String string, int n, BigDecimal bigDecimal) {
        String string2 = string.strip();
        if (!string2.matches("[0-9]{1,12}(\\.[0-9]{1,6})?")) {
            throw new IllegalArgumentException("Enter a positive amount, for example 100 or 100.50 (0 removes money).");
        }
        BigDecimal bigDecimal2 = new BigDecimal(string2);
        try {
            bigDecimal2 = bigDecimal2.setScale(Math.max(0, Math.min(6, n)), RoundingMode.UNNECESSARY);
        }
        catch (ArithmeticException arithmeticException) {
            throw new IllegalArgumentException("This economy supports " + n + " decimal places.");
        }
        if (bigDecimal2.compareTo(bigDecimal) > 0) {
            throw new IllegalArgumentException("Maximum offer: " + bigDecimal.toPlainString());
        }
        return bigDecimal2.stripTrailingZeros();
    }

    public static Outcome transfer(BigDecimal bigDecimal, BigDecimal bigDecimal2, Wallet wallet, Journal journal) {
        BigDecimal bigDecimal3 = bigDecimal.subtract(bigDecimal2);
        if (bigDecimal3.signum() == 0) {
            return Outcome.PAID;
        }
        int n = bigDecimal3.signum() > 0 ? 0 : 1;
        int n2 = 1 - n;
        BigDecimal bigDecimal4 = bigDecimal3.abs();
        try {
            journal.stage("WITHDRAW_" + n + "_PENDING");
            if (!wallet.withdraw(n, bigDecimal4)) {
                journal.stage("WITHDRAW_FAILED");
                return Outcome.CANCELLED;
            }
            journal.stage("WITHDRAWN_" + n);
            journal.stage("DEPOSIT_" + n2 + "_PENDING");
            if (wallet.deposit(n2, bigDecimal4)) {
                journal.stage("PAID");
                return Outcome.PAID;
            }
            journal.stage("REFUND_" + n + "_PENDING");
            if (wallet.deposit(n, bigDecimal4)) {
                journal.stage("REFUNDED");
                return Outcome.CANCELLED;
            }
            journal.stage("REFUND_FAILED");
        }
        catch (Exception exception) {
            try {
                journal.stage("REVIEW: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
            catch (Exception exception2) {
                // empty catch block
            }
        }
        return Outcome.REVIEW;
    }

    public static enum Outcome {
        PAID,
        CANCELLED,
        REVIEW;

    }

    public static interface Journal {
        public void stage(String var1) throws Exception;
    }

    public static interface Wallet {
        public boolean withdraw(int var1, BigDecimal var2) throws Exception;

        public boolean deposit(int var1, BigDecimal var2) throws Exception;
    }
}

