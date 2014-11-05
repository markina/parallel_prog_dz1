package ru.ifmo.pp.fgb;

import java.util.Arrays;

/**
 * Results of operations on a bank.
 *
 * @author Roman Elizarov
 */
class Results {
    private final Object[] results;
    private int count;

    Results(int n) {
        results = new Object[n];
    }

    Results(Results other) {
        results = other.results.clone();
    }

    void set(int i, Object val) {
        results[i] = val;
    }

    void incCount() {
        count++;
    }

    int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Results results1 = (Results) o;
        return Arrays.equals(results, results1.results);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(results);
    }

    @Override
    public String toString() {
        return Arrays.toString(results) + " -> " + count;
    }
}
