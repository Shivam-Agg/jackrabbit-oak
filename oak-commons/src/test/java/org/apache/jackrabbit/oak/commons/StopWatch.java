/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.commons;

import org.apache.jackrabbit.oak.commons.conditions.Validate;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A stop watch based either on a {@link Supplier} of nanoseconds, or a {@link java.time.Clock}.
 * <p>
 * The accuracy of measurements depends on the precision of the time source, which likely depends on platform and
 * configuration.
 * <p>
 * Inspired by Guava's.
 */
public class StopWatch {

    private static final long NANOS_PER_SECOND = 1000 * 1000 * 1000;

    private long startTime;
    private long accumulated;
    private boolean running;
    private final Supplier<Long> ticker;

    private StopWatch(Supplier<Long> ticker) {
        this.ticker = ticker;
        this.accumulated = 0L;
        this.startTime = ticker.get();
        this.running = false;
    }

    /**
     * @return a running stop watch, using {@link System#nanoTime()}.
     */
    public static StopWatch createStarted() {
        return new StopWatch(StopWatch::tick).start();
    }

    /**
     * @return a running stop watch, using the supplied provider.
     */
    public static StopWatch createStarted(Supplier<Long> ticker) {
        return new StopWatch(ticker).start();
    }

    /**
     * @return a running stop watch, using the supplied clock.
     * <p>
     * Note that only {@link Clock#millis()} will be used, thus the watch will have ms precision at most.
     */
    public static StopWatch createStarted(Clock clock) {
        return new StopWatch(clockAsLongSupplier(clock)).start();
    }

    /**
     * @return a non-running stop watch, using {@link System#nanoTime()}.
     */
    public static StopWatch createUnstarted() {
        return new StopWatch(StopWatch::tick);
    }

    /**
     * creates a running stop watch, using {@link System#nanoTime()}.
     * <p>
     * @deprecated use factory methods, such as {@link #createStarted()}
     */
    @Deprecated
    public StopWatch() {
        this.ticker = StopWatch::tick;
        createStarted();
    }

    /**
     * Starts the stop watch, will fail when running.
     * @return the stop watch
     */
    public StopWatch start() {
        Validate.checkState(!this.running, "StopWatch already running.");
        this.startTime = this.ticker.get();
        this.running = true;
        return this;
    }

    /**
     * Stops the stop watch, will fail when not running.
     * @return the stop watch
     */
    public StopWatch stop() {
        Validate.checkState(this.running, "StopWatch not running.");
        this.accumulated += elapsedNanos();
        this.startTime = 0L;
        this.running = false;
        return this;
    }

    /**
     * Resets the stop watch, and puts it into stopped state.
     * @return the stop watch
     */
    public StopWatch reset() {
        this.accumulated = 0L;
        this.startTime = 0;
        this.running = false;
        return this;
    }

    /**
     * @return whether the stop watch is running
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Gets elapsed time using the supplied {@link TimeUnit}.
     * @param timeunit time unit
     * @return elapsed time in the specified unit
     */
    public long elapsed(TimeUnit timeunit) {
        return timeunit.convert(elapsedNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Gets elapsed time as {@link Duration}.
     * @return elapsed time
     */
    public Duration elapsed() {
        return Duration.ofMillis(elapsedNanos());
    }

    @Override
    public String toString() {
        return java.time.Duration.ofNanos(elapsedNanos()).toString();
    }

    // private parts

    private long elapsedNanos() {
        long delta = this.running ? this.ticker.get() - this.startTime : 0;
        return this.accumulated + delta;
    }

    private static long tick() {
        return System.nanoTime();
    }

    private static Supplier<Long> clockAsLongSupplier(java.time.Clock clock) {
        return () -> TimeUnit.MILLISECONDS.toNanos(clock.millis());
    }

    // "legacy methods" (pre OAK-11620)

    private long lastLog = startTime;

    public long time() {
        return ticker.get() - startTime;
    }

    public String seconds() {
        double s = (double) time() / NANOS_PER_SECOND;
        return String.format("%.2f seconds", s);
    }

    public String operationsPerSecond(int operations) {
        long t = time();
        double s = (double) t / NANOS_PER_SECOND;
        if (t == 0) {
            t = 1;
        }
        int ops = (int) (operations * NANOS_PER_SECOND / t);
        return String.format("%.2f seconds (%d ops; %d op/s)", s, operations, ops);
    }

    /**
     * Returns true once 5 seconds.
     *
     * @return true once every 5 seconds
     */
    public boolean log() {
        long t = ticker.get();
        if (t - lastLog > 5 * NANOS_PER_SECOND) {
            lastLog = t;
            return true;
        }
        return false;
    }
}
