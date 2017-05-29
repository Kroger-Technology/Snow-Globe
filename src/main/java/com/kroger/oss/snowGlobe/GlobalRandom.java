package com.kroger.oss.snowGlobe;

import java.util.Random;

import static java.lang.Math.abs;

public class GlobalRandom {

    private static Random random = new Random(System.nanoTime());

    public static int getRandomPrefix() {
        return abs(random.nextInt());
    }
}
