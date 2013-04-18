/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.research;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * A buffer that holds a fixed number of LogUnits.
 *
 * LogUnits are added in and shifted out in temporal order.  Only a subset of the LogUnits are
 * actual words; the other LogUnits do not count toward the word limit.  Once the buffer reaches
 * capacity, adding another LogUnit that is a word evicts the oldest LogUnits out one at a time to
 * stay under the capacity limit.
 *
 * This variant of a LogBuffer has a limited memory footprint because of its limited size.  This
 * makes it useful, for example, for recording a window of the user's most recent actions in case
 * they want to report an observed error that they do not know how to reproduce.
 */
public class FixedLogBuffer extends LogBuffer {
    /* package for test */ int mWordCapacity;
    // The number of members of mLogUnits that are actual words.
    private int mNumActualWords;

    /**
     * Create a new LogBuffer that can hold a fixed number of LogUnits that are words (and
     * unlimited number of non-word LogUnits), and that outputs its result to a researchLog.
     *
     * @param wordCapacity maximum number of words
     */
    public FixedLogBuffer(final int wordCapacity) {
        super();
        if (wordCapacity <= 0) {
            throw new IllegalArgumentException("wordCapacity must be 1 or greater.");
        }
        mWordCapacity = wordCapacity;
        mNumActualWords = 0;
    }

    /**
     * Adds a new LogUnit to the front of the LIFO queue, evicting existing LogUnit's
     * (oldest first) if word capacity is reached.
     */
    @Override
    public void shiftIn(final LogUnit newLogUnit) {
        if (!newLogUnit.hasWord()) {
            // This LogUnit isn't a word, so it doesn't count toward the word-limit.
            super.shiftIn(newLogUnit);
            return;
        }
        if (mNumActualWords >= mWordCapacity) {
            // Give subclass a chance to handle the buffer full condition by shifting out logUnits.
            onBufferFull();
            // If still full, evict.
            if (mNumActualWords >= mWordCapacity) {
                shiftOutWords(1);
            }
        }
        super.shiftIn(newLogUnit);
        mNumActualWords++; // Must be a word, or we wouldn't be here.
    }

    @Override
    public LogUnit unshiftIn() {
        final LogUnit logUnit = super.unshiftIn();
        if (logUnit != null && logUnit.hasWord()) {
            mNumActualWords--;
        }
        return logUnit;
    }

    public int getNumWords() {
        return mNumActualWords;
    }

    /**
     * Removes all LogUnits from the buffer without calling onShiftOut().
     */
    @Override
    public void clear() {
        super.clear();
        mNumActualWords = 0;
    }

    /**
     * Called when the buffer has just shifted in one more word than its maximum, and its about to
     * shift out LogUnits to bring it back down to the maximum.
     *
     * Base class does nothing; subclasses may override if they want to record non-privacy sensitive
     * events that fall off the end.
     */
    protected void onBufferFull() {
    }

    @Override
    public LogUnit shiftOut() {
        final LogUnit logUnit = super.shiftOut();
        if (logUnit != null && logUnit.hasWord()) {
            mNumActualWords--;
        }
        return logUnit;
    }

    /**
     * Remove LogUnits from the front of the LogBuffer until {@code numWords} have been removed.
     *
     * If there are less than {@code numWords} word-containing {@link LogUnit}s, shifts out
     * all {@code LogUnit}s in the buffer.
     *
     * @param numWords the number of word-containing {@link LogUnit}s to shift out
     * @return the number of actual {@code LogUnit}s shifted out
     */
    protected int shiftOutWords(final int numWords) {
        int numWordContainingLogUnitsShiftedOut = 0;
        for (LogUnit logUnit = shiftOut(); logUnit != null
                && numWordContainingLogUnitsShiftedOut < numWords; logUnit = shiftOut()) {
            if (logUnit.hasWord()) {
                numWordContainingLogUnitsShiftedOut++;
            }
        }
        return numWordContainingLogUnitsShiftedOut;
    }

    public void shiftOutAll() {
        final LinkedList<LogUnit> logUnits = getLogUnits();
        while (!logUnits.isEmpty()) {
            shiftOut();
        }
        mNumActualWords = 0;
    }

    /**
     * Returns a list of {@link LogUnit}s at the front of the buffer that have associated words.  No
     * more than {@code n} LogUnits will have words associated with them.  If there are not enough
     * LogUnits in the buffer to meet the word requirement, returns the all LogUnits.
     *
     * @param n The maximum number of {@link LogUnit}s with words to return.
     * @return The list of the {@link LogUnit}s containing the first n words
     */
    public ArrayList<LogUnit> peekAtFirstNWords(int n) {
        final LinkedList<LogUnit> logUnits = getLogUnits();
        final int length = logUnits.size();
        // Allocate space for n*2 logUnits.  There will be at least n, one for each word, and
        // there may be additional for punctuation, between-word commands, etc.  This should be
        // enough that reallocation won't be necessary.
        final ArrayList<LogUnit> list = new ArrayList<LogUnit>(n * 2);
        for (int i = 0; i < length && n > 0; i++) {
            final LogUnit logUnit = logUnits.get(i);
            list.add(logUnit);
            if (logUnit.hasWord()) {
                n--;
            }
        }
        return list;
    }
}
