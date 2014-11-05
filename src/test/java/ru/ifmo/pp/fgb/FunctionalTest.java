package ru.ifmo.pp.fgb;

import junit.framework.TestCase;

/**
 * Functional single-threaded test-suite for bank implementation.
 *
 * @author Roman Elizarov
 */
public class FunctionalTest extends TestCase {
    private static final int N = 10;

    private final Bank bank = new BankImpl(N);

    public void testEmptyBank() {
        long start = System.currentTimeMillis();
        assertEquals(N, bank.getNumberOfAccounts());
        assertEquals(0, bank.getTotalAmount());
        for (int i = 0; i < N; i++)
            assertEquals(0, bank.getAmount(i));
        long finish = System.currentTimeMillis();
        System.out.println("TIME_EmptyBank = " + (finish - start));

    }

    public void testDeposit() {
        long start = System.currentTimeMillis();
        long amount = 1234;
        long result = bank.deposit(1, amount);
        assertEquals(amount, result);
        assertEquals(amount, bank.getAmount(1));
        assertEquals(amount, bank.getTotalAmount());
        long finish = System.currentTimeMillis();
        System.out.println("TIME_Deposit = " + (finish - start));
    }

    public void testWithdraw() {
        long start = System.currentTimeMillis();
        int depositAmount = 2345;
        long depositResult = bank.deposit(1, depositAmount);
        assertEquals(depositAmount, depositResult);
        assertEquals(depositAmount, bank.getAmount(1));
        assertEquals(depositAmount, bank.getTotalAmount());
        long withdrawAmount = 1234;
        long withdrawResult = bank.withdraw(1, withdrawAmount);
        assertEquals(depositAmount - withdrawAmount, withdrawResult);
        assertEquals(depositAmount - withdrawAmount, bank.getAmount(1));
        assertEquals(depositAmount - withdrawAmount, bank.getTotalAmount());
        long finish = System.currentTimeMillis();
        System.out.println("TIME_Withdraw = " + (finish - start));
    }

    public void testTotalAmount() {
        long start = System.currentTimeMillis();
        long deposit1 = 4567;
        long depositResult1 = bank.deposit(1, deposit1);
        assertEquals(deposit1, depositResult1);
        assertEquals(deposit1, bank.getTotalAmount());
        long deposit2 = 6789;
        long depositResult2 = bank.deposit(2, deposit2);
        assertEquals(deposit2, depositResult2);
        assertEquals(deposit2, bank.getAmount(2));
        assertEquals(deposit1 + deposit2, bank.getTotalAmount());
        long finish = System.currentTimeMillis();
        System.out.println("TIME_TotalAmount = " + (finish - start));
    }

    public void testTransfer() {
        long start = System.currentTimeMillis();
        int depositAmount = 9876;
        long depositResult = bank.deposit(1, depositAmount);
        assertEquals(depositAmount, depositResult);
        assertEquals(depositAmount, bank.getAmount(1));
        assertEquals(depositAmount, bank.getTotalAmount());
        long transferAmount = 5432;
        bank.transfer(1, 2, transferAmount);
        assertEquals(depositAmount - transferAmount, bank.getAmount(1));
        assertEquals(transferAmount, bank.getAmount(2));
        assertEquals(depositAmount, bank.getTotalAmount());
        long finish = System.currentTimeMillis();
        System.out.println("TIME_Transfer = " + (finish - start));
    }
}
