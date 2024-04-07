/*
 * Copyright (c) 2024 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable.primitive;

import java.util.NoSuchElementException;

import org.eclipse.collections.api.iterator.ByteIterator;

public class ByteHashSetMathsMethods
{

    private ByteHashSet bhset;

    public ByteHashSetMathsMethods (ByteHashSet set) {this.bhset = set;}

    public long sum()
    {
        long result = 0L;

        ByteIterator iterator = this.bhset.byteIterator();

        while (iterator.hasNext())
        {
            result += iterator.next();
        }

        return result;
    }

    public byte max()
    {
        if (this.bhset.isEmpty())
        {
            throw new NoSuchElementException();
        }
        byte max = 0;

        if (this.bhset.getBitGroup4() != 0L)
        {
            //the highest has to be from this
            max = (byte) (127 - Long.numberOfLeadingZeros(this.bhset.getBitGroup4()));
        }
        else if (this.bhset.getBitGroup3() != 0L)
        {
            max = (byte) (63 - Long.numberOfLeadingZeros(this.bhset.getBitGroup3()));
        }
        else if (this.bhset.getBitGroup2() != 0L)
        {
            max = (byte) ((Long.numberOfTrailingZeros(this.bhset.getBitGroup2()) + 1) * -1);
        }
        else if (this.bhset.getBitGroup1() != 0L)
        {
            max = (byte) ((Long.numberOfTrailingZeros(this.bhset.getBitGroup1()) + 65) * -1);
        }

        return max;
    }

    public byte maxIfEmpty(byte defaultValue)
    {
        if (this.bhset.isEmpty())
        {
            return defaultValue;
        }
        return this.max();
    }

    public byte min()
    {
        if (this.bhset.isEmpty())
        {
            throw new NoSuchElementException();
        }

        byte min = 0;

        if (this.bhset.getBitGroup1() != 0L)
        {
            //the minimum has to be from this
            min = (byte) (128 - Long.numberOfLeadingZeros(this.bhset.getBitGroup1()));
            min *= -1;
        }
        else if (this.bhset.getBitGroup2() != 0L)
        {
            min = (byte) ((64 - Long.numberOfLeadingZeros(this.bhset.getBitGroup2())) * -1);
        }
        else if (this.bhset.getBitGroup3() != 0L)
        {
            min = (byte) Long.numberOfTrailingZeros(this.bhset.getBitGroup3());
        }
        else if (this.bhset.getBitGroup4() != 0L)
        {
            min = (byte) (Long.numberOfTrailingZeros(this.bhset.getBitGroup4()) + 64);
        }

        return min;
    }

    public byte minIfEmpty(byte defaultValue)
    {
        if (this.bhset.isEmpty())
        {
            return defaultValue;
        }
        return this.min();
    }

    public double average()
    {
        if (this.bhset.isEmpty())
        {
            throw new ArithmeticException();
        }
        return (double) this.sum() / (double) this.bhset.size();
    }

    public double median()
    {
        if (this.bhset.isEmpty())
        {
            throw new ArithmeticException();
        }
        byte[] sortedArray = this.bhset.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            byte first = sortedArray[middleIndex];
            byte second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }


}
