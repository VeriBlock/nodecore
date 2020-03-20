// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.crypto;

import java.util.Random;

/**
 * This code is entirely mine (Maxwell Sanchez), and was originally released in late 2015 under MIT licensing.
 * It can easily be replaced with something more powerful.
 * <p>
 * This is an extremely simple class to introduce additional entropy based on unpredictable
 * computational time of variable-length computational routines.
 * <p>
 * Computational 'events' (the wasteTime() method) act to do an unknown amount of work,
 * so that time gap can be measured and used as entropy for the system. Even computations
 * of a known length and difficulty take variable amounts of time given the nature of
 * modern _runtime environments. Timing the _runtime of these events allows the introduction
 * of entropy based on properties outside the program, such as processor load, computer
 * performance, and thread scheduling.
 * <p>
 * Furthermore, available environment states (such as memory availability) provide additional
 * entropy.
 * <p>
 * At its core, this class utilizes Java's Random object to further mix internal state
 * and easily provide non-biased numbers. All top-level state modifications are based
 * on XOR, as XOR does not favor either zeroes or ones, and so will not cause incremental
 * decay towards a lower-entropy state.
 * <p>
 * This class focuses far more on the unpredictability of output rather than the even
 * distribution of results, but experimentally this class has shown to produce well-distributed
 * results.
 * <p>
 * In 1,000 trials, each consisting of averaging the result from 100,000 calls to Entropy.nextDouble()
 * and Random.nextDouble() (both initialized with no constructor arguments), the averages of
 * Entropy tended to be, on average, moderately farther away from the expected value of 0.5.
 * However, All differences from both classes were less than 0.8% from the expected value, which
 * for most purposes is a negligible bias. Additionally, biases did not favor either above
 * or below the expected value.
 * <p>
 * System.nanoTime() used for additional granularity in timing unknown-length computational
 * events.
 * <p>
 * TODO: Improve entropy gathering, add things like network latency, CPU temp (passthrough to a C-family library?),
 * possibly use secure entropy that modern CPUs provide based on external conditions (RDRAND), maybe use /dev/random and/or
 * /dev/urandom on *nix, available system memory, etc. Improving this class will reduce the chance of side-channel attacks
 * against users who use the default implementation to produce public/private keypairs. Private keys are only as
 * secure as the way in which they are generated, and the way in which that can be reproduced/front-run.
 *
 */
public class Entropy {
    // Updated whenever wasteTime() is called, to introduce additional entropy from variables
    // used for consuming CPU cycles in calculations.
    private long _stateMixinFromWaste = 0x3955170223037924L;

    // State variables for default initialization values of the Entropy object
    // All have an equal number of ones and zeroes when represented in binary
    private long _state1 = 0x967ca962dd134c55L;
    private long _state2 = 0x8e678ec4fa4a3721L;
    private long _state3 = 0x741677f32bf14850L;
    private long _state4 = 0x82359b9bbd5e1708L;

    // The initial instantiation time of an Entropy object
    private long _startTimeNS;

    // The Runtime object to use for lookups regarding memory availability for additional entropy
    private final Runtime _runtime;

    /**
     * Constructor sets initial states dependent on timing and avalanche timing several related
     * computational events.
     * <p>
     * Results of these timings are used to set a reasonably mixed initial state well-removed
     * from start-time seeding.
     */
    public Entropy() {
        _startTimeNS = System.nanoTime();

        _runtime = Runtime.getRuntime();

        wasteTime();

        long startMix1NS = System.nanoTime();

        long time1 = System.nanoTime() - startMix1NS;
        wasteTime();
        long time2 = System.nanoTime() - startMix1NS;
        wasteTime();
        long time3 = System.nanoTime() - startMix1NS;
        wasteTime();
        long time4 = System.nanoTime() - startMix1NS;
        wasteTime();

        // Many new random objects are created to exploit the changing system time used for seeding Random
        Random mixer = new Random();

        // All time variables have a value in a predictable range, move it to an unpredicted location
        // for fairer mixing
        long mix1 = time1 << mixer.nextInt(64);
        mix1 += time2 << mixer.nextInt(64);
        mix1 += time3 << mixer.nextInt(64);
        mix1 += time4 << mixer.nextInt(64);

        _state1 ^= new Random().nextLong() ^ mix1;

        long startMix2NS = System.nanoTime();

        long time5 = System.nanoTime() - startMix2NS;
        wasteTime();
        long time6 = System.nanoTime() - startMix2NS;
        wasteTime();
        long time7 = System.nanoTime() - startMix2NS;
        wasteTime();
        long time8 = System.nanoTime() - startMix2NS;
        wasteTime();

        mixer = new Random();

        long mix2 = time6 << mixer.nextInt(64);
        mix2 += time7 << mixer.nextInt(64);
        mix2 += time5 << mixer.nextInt(64);
        mix2 += time8 << mixer.nextInt(64);

        _state2 ^= new Random().nextLong() ^ mix2;

        long startMix3NS = System.nanoTime();

        long time9 = System.nanoTime() - startMix3NS;
        wasteTime();
        long time10 = System.nanoTime() - startMix3NS;
        wasteTime();
        long time11 = System.nanoTime() - startMix3NS;
        wasteTime();
        long time12 = System.nanoTime() - startMix3NS;
        wasteTime();

        mixer = new Random();

        long mix3 = time11 << mixer.nextInt(64);
        mix3 += time9 << mixer.nextInt(64);
        mix3 += time12 << mixer.nextInt(64);
        mix3 += time10 << mixer.nextInt(64);

        _state3 ^= new Random().nextLong() ^ mix3;

        long startMix4NS = System.nanoTime();

        long time13 = System.nanoTime() - startMix4NS;
        wasteTime();
        long time14 = System.nanoTime() - startMix4NS;
        wasteTime();
        long time15 = System.nanoTime() - startMix4NS;
        wasteTime();
        long time16 = System.nanoTime() - startMix4NS;
        wasteTime();

        mixer = new Random();

        long mix4 = time16 << mixer.nextInt(64);
        mix4 += time13 << mixer.nextInt(64);
        mix4 += time14 << mixer.nextInt(64);
        mix4 += time15 << mixer.nextInt(64);

        mix1 ^= _runtime.freeMemory();

        _state4 ^= new Random().nextLong() ^ mix4;
    }

    /**
     * Adds additional event-time-based mixing to the state.
     * Uses same event (wasteTime()) as constructor.
     * Called after every method call which pushes entropy to the outside world
     * (nextLong(), and all dependent methods).
     */
    public void entropize() {
        long start = System.nanoTime();
        wasteTime();
        Random stateShifter = new Random(System.nanoTime() - start);

        _state1 ^= stateShifter.nextLong();
        _state2 ^= stateShifter.nextLong();

        if ((System.nanoTime() - start) % 10 == 0)
            stateShifter.setSeed(_stateMixinFromWaste);

        _state3 ^= stateShifter.nextLong();
        _state4 ^= stateShifter.nextLong();

        int route = new Random().nextInt(4);
        if (route == 0)
            _state1 ^= (_runtime.freeMemory() << stateShifter.nextInt(64));
        else if (route == 1)
            _state2 ^= (_runtime.freeMemory() << stateShifter.nextInt(64));
        else if (route == 3)
            _state3 ^= (_runtime.freeMemory() << stateShifter.nextInt(64));
        else
            _state4 ^= (_runtime.freeMemory() << stateShifter.nextInt(64));
    }

    /**
     * Allows additional entropy to be introduced from an external source.
     *
     * @param entropy byte[] representation of external entropy.
     */
    public void addEntropy(byte[] entropy) {
        long startTime = System.nanoTime();

        int i = 0;
        for (i = 0; i < entropy.length % 8; i += 8) {
            // Produce a fully-filled long from the provided entropy
            long mixin = entropy[i] << 56;
            mixin += entropy[i + 1] << 48;
            mixin += entropy[i + 2] << 40;
            mixin += entropy[i + 3] << 32;
            mixin += entropy[i + 4] << 24;
            mixin += entropy[i + 5] << 16;
            mixin += entropy[i + 6] << 8;
            mixin += entropy[i + 7];

            // Decide where to mix in the current 8 bytes of entropy
            Random router = new Random(System.nanoTime() - startTime);
            int route = router.nextInt(4);

            // Execute choice of mix in location
            if (route % 4 == 0)
                _state1 ^= (mixin | _state2);
            else if (route % 4 == 1)
                _state2 ^= (mixin | _state3);
            else if (route % 4 == 2)
                _state3 ^= (mixin | _state4);
            else
                _state4 ^= (mixin | _state1);
        }

        // TODO: can we do this with a stream instead?
        // Ugly way to use variable 'i' from before without reassignment. Consume final bytes not accounted for.
        for (; i < entropy.length; i++) {
            Random router = new Random(System.nanoTime() - startTime);
            int route = router.nextInt(4);

            if (route % 4 == 0)
                _state1 ^= (new Random(entropy[i]).nextLong());
            else if (route % 4 == 1)
                _state2 ^= (new Random(entropy[i]).nextLong());
            else if (route % 4 == 2)
                _state3 ^= (new Random(entropy[i]).nextLong());
            else
                _state4 ^= (new Random(entropy[i]).nextLong());
        }
    }

    /**
     * Returns a pseudorandom boolean value based on present entropy.
     *
     * @return boolean A pseudorandom boolean value based on present entropy.
     */
    public boolean nextBoolean() {
        return nextInt(2) == 1;
    }

    /**
     * Fills the provided byte array with psuedorandomly generated bytes.
     */
    public void nextBytes(byte[] bytes) {
        // 0 - 255 is the range of an unsigned bit, casting an int above 127 will give a two's complement-based negative.
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) nextInt(256);
    }

    /**
     * Returns a pseudorandom double value based on present entropy.
     *
     * @return double A pseudorandom double value based on present entropy.
     */
    private double nextDouble() {
        double randomDouble = 0.0;

        // Just use the long I already wrote generation for to create a double between 0 (inc.) nd 1 (not inc.)
        randomDouble += ((double) nextLong() / (double) Long.MAX_VALUE);
        if (randomDouble < 0) randomDouble *= -1;
        return randomDouble;
    }

    /**
     * Returns a pseudorandom float value based on present entropy.
     *
     * @return float A pseudorandom float value based on present entropy.
     */
    public float nextFloat() {
        // Just use the double I already wrote generation for and reduce precision for a float
        return (float) nextDouble();
    }

    /**
     * Returns a pseudorandom long value based on present entropy.
     *
     * @return long A pseudorandom long value based on present entropy.
     */
    public long nextLong() {
        // Use Java's Random with seeds determined by state to create and mix a pseudorandom long
        Random shifters = new Random();
        long randomLong = new Random(_state3 << shifters.nextInt(64)).nextLong();
        randomLong ^= new Random(_state1 << shifters.nextInt(64)).nextInt();
        randomLong ^= new Random(_state2).nextInt();
        randomLong ^= (_state4 << shifters.nextInt(64));
        randomLong ^= _stateMixinFromWaste;
        randomLong ^= new Random(new Random().nextLong() + _startTimeNS).nextLong();

        // More state mixing
        entropize();

        // Even more state mixing
        _state1 <<= shifters.nextInt(64);
        _state2 <<= shifters.nextInt(64);
        _state3 <<= shifters.nextInt(64);
        _state4 <<= shifters.nextInt(64);
        _state1 ^= (_state2 << shifters.nextInt(64) | _stateMixinFromWaste);
        _state2 ^= (_state3 << shifters.nextInt(64) | new Random().nextLong());
        _state3 ^= (_state4 << shifters.nextInt(64) | _state1);
        _state4 ^= (_state1 << shifters.nextInt(64) | _state2);
        long swapState = _state4;
        _state4 = _state1;
        _state1 = _state2;
        _state2 = _state3;
        _state3 = swapState;

        return randomLong;
    }

    /**
     * Returns a pseudorandom long value based on present entropy below the provided limit.
     *
     * @param limit The (non-inclusive) limit of the returned long.
     * @return long A pseudorandom long value based on present entropy below the provided limit.
     */
    public long nextLong(long limit) {
        // Wrap a boundless pseudorandom long around the provided limit
        long toLimit = nextLong();
        if (toLimit < 0) toLimit *= -1;
        return toLimit % limit;
    }

    /**
     * Returns a pseudorandom int value based on present entropy.
     *
     * @return int A pseudorandom int value based on present entropy.
     */
    private int nextInt() {
        // Just grab a pseudorandom long and reduce precision
        return (int) nextLong();
    }

    /**
     * Returns a pseudorandom int value based on present entropy below the provided limit.
     *
     * @param limit The (non-inclusive) int of the returned int.
     * @return int A pseudorandom int value based on present entropy below the provided limit.
     */
    private int nextInt(int limit) {
        // Wrap a boundless pseudorandom int around a provided limit
        int toLimit = nextInt();
        if (toLimit < 0) toLimit *= -1;
        return toLimit % limit;
    }

    /**
     * A method designed to take an unknown (but miniscule) amount of time to complete.
     * Also adds slightly to the entropy of the object.
     */
    private void wasteTime() {
        // Keep firstCycleCount random but within expected range
        int firstCycleCount = new Random().nextInt(1000) + 2000;

        long meaninglessState1 = new Random().nextLong();
        long meaninglessState2 = new Random().nextLong();

        Random cycleOneRandom = new Random();

        for (int i = 0; i < firstCycleCount; i++) {
            meaninglessState1 ^= (meaninglessState2 & cycleOneRandom.nextLong() | cycleOneRandom.nextLong());
            meaninglessState2 ^= (cycleOneRandom.nextLong() ^ cycleOneRandom.nextLong());
        }

        // Keep secondCycleCount random but within expected range
        int secondCycleCount = new Random().nextInt(10000) + 20000;

        Random cycleTwoRandom = new Random();

        for (int i = 0; i < secondCycleCount; i++) {
            meaninglessState2 ^= cycleTwoRandom.nextLong();
            meaninglessState1 ^= (cycleTwoRandom.nextLong() | cycleTwoRandom.nextLong());

            if (meaninglessState2 % 100 == 0)
                cycleTwoRandom = new Random(new Random().nextLong() ^ meaninglessState1);
        }

        // Mind as well put the working variables to good use doing *something*
        _stateMixinFromWaste ^= meaninglessState1 ^ meaninglessState2;
    }
}
