/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.oak.stats;

import org.apache.jackrabbit.oak.commons.conditions.Validate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A stop watch based on {@link java.time.Clock}, measuring time in milliseconds.
 * <p>
 * (inspired by Guava's)
 */
public class Stopwatch {

    private long starttime;
    private long accumulated;
    private boolean running;
    private final java.time.Clock clock;

    private Stopwatch(java.time.Clock clock, boolean running) {
        this.clock = clock;
        this.accumulated = 0L;
        this.starttime = this.clock.millis();
        this.running = running;
    }

    /**
     * @return a running stop watch, using {@link Clock#SIMPLE}.
     */
    public static Stopwatch createStarted() {
        return new Stopwatch(Clock.SIMPLE, true);
    }

    /**
     * @return a running stop watch, using the supplied clock.
     */
    public static Stopwatch createStarted(java.time.Clock clock) {
        return new Stopwatch(clock, true);
    }

    /**
     * @return a non-running stop watch, using {@link Clock#SIMPLE}.
     */
    public static Stopwatch createUnstarted() {
        return new Stopwatch(Clock.SIMPLE, false);
    }

    /**
     * Starts the stop watch, will fail when running.
     * @return the stop watch
     */
    public Stopwatch start() {
        Validate.checkState(!this.running, "Stopwatch already started.");
        this.starttime = clock.millis();
        this.running = true;
        return this;
    }

    /**
     * Stops the stop watch, will fail when stopped.
     * @return the stop watch
     */
    public Stopwatch stop() {
        Validate.checkState(this.running, "Stopwatch not running.");
        this.accumulated += this.clock.millis() - this.starttime;
        this.starttime = 0L;
        this.running = false;
        return this;
    }

    /**
     * Resets the stop watch, and puts it into stopped state.
     * @return the stop watch
     */
    public Stopwatch reset() {
        this.accumulated = 0L;
        this.starttime = 0;
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
        return timeunit.convert(elapsedMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Gets elapsed time as {@link Duration}.
     * @return elapsed time
     */
    public Duration elapsed() {
        return Duration.ofMillis(elapsedMillis());
    }

    @Override
    public String toString() {
        return java.time.Duration.ofMillis(elapsedMillis()).toString();
    }

    private long elapsedMillis() {
        long delta = this.running ? this.clock.millis() - this.starttime : 0;
        return this.accumulated + delta;
    }
}
