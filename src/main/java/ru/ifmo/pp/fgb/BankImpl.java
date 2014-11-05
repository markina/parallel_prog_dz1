package ru.ifmo.pp.fgb;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bank implementation.
 * <p/>
 * <p>:TODO: This implementation has to be made thread-safe.
 *
 * @author Markina Margarita
 */
public class BankImpl implements Bank {
    /**
     * An array of accounts by index.
     */
    private final Account[] accounts;


    /**
     * Creates new bank instance.
     *
     * @param n the number of accounts (numbered from 0 to n-1).
     */
    public BankImpl(int n) {
        accounts = new Account[n];
        for (int i = 0; i < n; i++) {
            accounts[i] = new Account();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfAccounts() {
        return accounts.length;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long getAmount(int index) {
        accounts[index].setLock();
        long result = accounts[index].amount;
        accounts[index].setUnlock();
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long getTotalAmount() {
        for(int i = 0; i < accounts.length; i++) {
            accounts[i].setLock();
        }
        long sum = 0;
        for (Account account : accounts) {
            sum += account.amount;
        }
        for(int i = accounts.length - 1; i >= 0; i--) {
            accounts[i].setUnlock();
        }
        return sum;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long deposit(int index, long amount) {
        accounts[index].setLock();
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        Account account = accounts[index];
        if (amount > MAX_AMOUNT || account.amount + amount > MAX_AMOUNT)
            throw new IllegalStateException("Overflow");
        account.amount += amount;
        long result = account.amount;
        accounts[index].setUnlock();
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long withdraw(int index, long amount) {
        accounts[index].setLock();
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        Account account = accounts[index];
        if (account.amount - amount < 0)
            throw new IllegalStateException("Underflow");
        account.amount -= amount;
        long result = account.amount;
        accounts[index].setUnlock();
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public synchronized void transfer(int fromIndex, int toIndex, long amount) {
        if(fromIndex < toIndex) {
            accounts[fromIndex].setLock();
            accounts[toIndex].setLock();
        } else {
            accounts[toIndex].setLock();
            accounts[fromIndex].setLock();
        }
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        if (fromIndex == toIndex)
            throw new IllegalArgumentException("fromIndex == toIndex");
        Account from = accounts[fromIndex];
        Account to = accounts[toIndex];
        if (amount > from.amount)
            throw new IllegalStateException("Underflow");
        else if (amount > MAX_AMOUNT || to.amount + amount > MAX_AMOUNT)
            throw new IllegalStateException("Overflow");
        from.amount -= amount;
        to.amount += amount;
        if(toIndex > fromIndex) {
            accounts[toIndex].setUnlock();
            accounts[fromIndex].setUnlock();
        } else {
            accounts[fromIndex].setUnlock();
            accounts[toIndex].setUnlock();
        }
    }

    /**
     * Private account data structure.
     */
    private static class Account {
        /**
         * Amount of funds in this account.
         */
        Lock lock = new ReentrantLock();
        long amount;

        public void setLock() {
            lock.lock();
        }


        public void setUnlock() {
            lock.unlock();
        }


    }
}
