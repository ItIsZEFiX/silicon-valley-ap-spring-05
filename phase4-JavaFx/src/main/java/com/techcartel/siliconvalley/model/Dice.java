package com.techcartel.siliconvalley.model;

import java.io.Serializable;
import java.util.Random;

public class Dice implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 6;
    private final Random randomGenerator;
    private int die1;
    private int die2;
    private int lastRollSum;

    public Dice() {
        this.randomGenerator = new Random();
        this.die1 = 1;
        this.die2 = 1;
        this.lastRollSum = 2;
    }

    public int getDie1() {
        return die1;
    }
    public int getDie2() {
        return die2;
    }
    public int getLastRollSum() {
        return lastRollSum;
    }

    public int roll() {
        die1 = randomGenerator.nextInt(MAX_VALUE) + MIN_VALUE;
        die2 = randomGenerator.nextInt(MAX_VALUE) + MIN_VALUE;

        lastRollSum = die1 + die2;
        return lastRollSum;
    }
}
