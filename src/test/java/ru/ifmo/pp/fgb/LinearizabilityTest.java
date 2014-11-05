package ru.ifmo.pp.fgb;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Automated test of linearizability of multi-threaded bank implementation.
 *
 * @author Roman Elizarov
 */
public class LinearizabilityTest extends TestCase {
    private static final int N = 10;
    private static final int RUN_ACCOUNTS = 3;
    private static final int THREADS = 4;
    private static final int RUNS = 2000;
    private static final int EXECUTIONS = 1000; // executions per run
    private static final int RESULTS_HASH_SIZE = 1024; // must be 2^N;

    private final Random rnd = new Random(20141101);
    private final Phaser phaser = new Phaser(THREADS + 1);

    private final int[] runAccounts = new int[RUN_ACCOUNTS];
    private final long[] baseAmount = new long[RUN_ACCOUNTS];
    private final Operation[] runOps = new Operation[THREADS];
    private final Results results = new Results(THREADS);
    private final Results[] resultsHash = new Results[RESULTS_HASH_SIZE];

    private Bank bank;

    public void testLinearizability() {
        for (int threadNo = 0; threadNo < THREADS; threadNo++)
            new TestThread(threadNo).start();
        for (int runNo = 1; runNo <= RUNS; runNo++)
            doOneRun(runNo);
    }

    private void doOneRun(int runNo) {
        for (int i = 0; i < RUN_ACCOUNTS; i++) {
            boolean ok;
            do {
                runAccounts[i] = rnd.nextInt(N);
                ok = true;
                for (int j = 0; j < i; j++)
                    if (runAccounts[i] == runAccounts[j])
                        ok = false;
            } while (!ok);
            baseAmount[i] = nextRndAmount();
        }
        for (int threadNo = 0; threadNo < THREADS; threadNo++) {
            Operation op;
            switch (rnd.nextInt(5)) {
                case 0:
                    op = new Operation.GetAmount(nextRndRunAccount());
                    break;
                case 1:
                    op = new Operation.GetTotalAmount();
                    break;
                case 2:
                    op = new Operation.Deposit(nextRndRunAccount(), nextRndAmountOrInvalid());
                    break;
                case 3:
                    op = new Operation.Withdraw(nextRndRunAccount(), nextRndAmountOrInvalid());
                    break;
                case 4:
                    int i;
                    int j;
                    do {
                        i = nextRndRunAccount();
                        j = nextRndRunAccount();
                    } while (i == j);
                    op = new Operation.Transfer(i, j, nextRndAmountOrInvalid());
                    break;
                default:
                    throw new AssertionError();
            }
            runOps[threadNo] = op;
        }
        Arrays.fill(resultsHash, null);
        serialScan(0, 0, new int[THREADS]);
        for (int i = 0; i < EXECUTIONS; i++) {
            doOneExecution();
        }
        int totalResult = 0;
        int seenResult = 0;
        for (Results results : resultsHash) {
            if (results != null) {
                totalResult++;
                if (results.getCount() > 0)
                    seenResult++;
            }
        }
        System.out.printf("Run #%d (%d%%) completed, seen %d out of %d results %n",
                runNo, (runNo * 100 / RUNS), seenResult, totalResult);
    }

    private void dumpRun() {
        System.out.println(Arrays.toString(runAccounts));
        System.out.println(Arrays.toString(baseAmount));
        System.out.println(Arrays.toString(runOps));
        for (Results results : resultsHash)
            if (results != null)
                System.out.println(results);
    }

    private void serialScan(int i, int used, int[] order) {
        if (i >= THREADS) {
            initBank(new SequentialBank(N));
            for (int k = 0; k < THREADS; k++)
                results.set(order[k], runOps[order[k]].invoke(bank));
            findOrCreateResults();
            return;
        }
        for (int j = 0; j < THREADS; j++)
            if ((used & (1 << j)) == 0) {
                order[i] = j;
                serialScan(i + 1, used | (1 << j), order);
            }
    }

    private void doOneExecution() {
        initBank(new BankImpl(N));
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndAwaitAdvance();
        Results hRes = findResults();
        if (hRes == null) {
            System.out.println("Non-linearizable execution: " + results);
            dumpRun();
            throw new AssertionError();
        }
        hRes.incCount();
    }

    private void initBank(Bank bank) {
        this.bank = bank;
        for (int i = 0; i < RUN_ACCOUNTS; i++)
            this.bank.deposit(runAccounts[i], baseAmount[i]);
    }

    private Results findOrCreateResults() {
        int hIndex = results.hashCode() & (RESULTS_HASH_SIZE - 1);
        Results hRes;
        while (true) {
            hRes = resultsHash[hIndex];
            if (hRes == null) {
                hRes = new Results(results);
                resultsHash[hIndex] = hRes;
                break;
            }
            if (hRes.equals(results))
                break;
            if (hIndex == 0)
                hIndex = RESULTS_HASH_SIZE;
            hIndex--;
        }
        return hRes;
    }

    private Results findResults() {
        int hIndex = results.hashCode() & (RESULTS_HASH_SIZE - 1);
        Results hRes;
        while (true) {
            hRes = resultsHash[hIndex];
            if (hRes == null || hRes.equals(results))
                break;
            if (hIndex == 0)
                hIndex = RESULTS_HASH_SIZE;
            hIndex--;
        }
        return hRes;
    }

    private int nextRndRunAccount() {
        return runAccounts[rnd.nextInt(RUN_ACCOUNTS)];
    }

    private long nextRndAmountOrInvalid() {
        if (rnd.nextInt(100) == 0) { // 1% of invalid amounts
            switch (rnd.nextInt(6)) {
                case 0: return 0;
                case 1: return -1;
                case 2: return Long.MIN_VALUE;
                case 3: return Bank.MAX_AMOUNT + 1;
                case 4: return Bank.MAX_AMOUNT + 2;
                case 5: return Long.MAX_VALUE;
            }
        }
        return nextRndAmount();
    }

    private long nextRndAmount() {
        int base = 1_000_000_000;
        return 1 + rnd.nextInt(base) + rnd.nextInt((int)(Bank.MAX_AMOUNT / base)) * (long)base;
    }

    private class TestThread extends Thread {
        private final int threadNo;
        private ThreadLocalRandom rnd;

        public TestThread(int threadNo) {
            super("TestThread-" + threadNo);
            this.threadNo = threadNo;
        }

        @Override
        public void run() {
            for (int i = 0; i < RUNS * EXECUTIONS; i++)
                doOneExecution();
        }

        private void doOneExecution() {
            phaser.arriveAndAwaitAdvance();
            results.set(threadNo, runOps[threadNo].invoke(bank));
            phaser.arriveAndAwaitAdvance();
        }
    }
}