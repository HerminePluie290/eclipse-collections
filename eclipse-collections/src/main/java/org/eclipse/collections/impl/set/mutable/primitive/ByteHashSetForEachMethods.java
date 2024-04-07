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

import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;

public class ByteHashSetForEachMethods{
    private ByteHashSet bhset;

    public ByteHashSetForEachMethods(ByteHashSet set){this.bhset = set;}

    public void forEach(ByteProcedure procedure)
    {
        this.each(procedure);
    }

    public void each(ByteProcedure procedure)
    {
        long bitGroup1 = this.bhset.getBitGroup1();
        while (bitGroup1 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup1);
            procedure.value((byte) ((value + 65) * -1));
            bitGroup1 &= ~(1L << (byte) (value + 64));
        }

        long bitGroup2 = this.bhset.getBitGroup2();
        while (bitGroup2 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup2);
            procedure.value((byte) ((value + 1) * -1));
            bitGroup2 &= ~(1L << value);
        }

        long bitGroup3 = this.bhset.getBitGroup3();
        while (bitGroup3 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup3);
            procedure.value(value);
            bitGroup3 &= ~(1L << value);
        }

        long bitGroup4 = this.bhset.getBitGroup4();
        while (bitGroup4 != 0L)
        {
            byte value = (byte) Long.numberOfTrailingZeros(bitGroup4);
            procedure.value((byte) (value + 64));
            bitGroup4 &= ~(1L << (byte) (value + 64));
        }
    }

    public ByteHashSet select(BytePredicate predicate)
    {
        ByteHashSet result = new ByteHashSet();

        this.forEach(value -> {
            if (predicate.accept(value))
            {
                result.add(value);
            }
        });

        return result;
    }

    public MutableByteSet reject(BytePredicate predicate)
    {
        MutableByteSet result = new ByteHashSet();

        this.forEach(value -> {
            if (!predicate.accept(value))
            {
                result.add(value);
            }
        });

        return result;
    }

    public <V> MutableSet<V> collect(ByteToObjectFunction<? extends V> function)
    {
        MutableSet<V> target = Sets.mutable.withInitialCapacity(this.bhset.size());

        this.forEach(each -> target.add(function.valueOf(each)));

        return target;
    }

    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        ByteIterator iterator = this.bhset.byteIterator();

        while (iterator.hasNext())
        {
            byte nextByte = iterator.next();

            if (predicate.accept(nextByte))
            {
                return nextByte;
            }
        }

        return ifNone;
    }

    public int count(BytePredicate predicate)
    {
        int count = 0;
        ByteIterator iterator = this.bhset.byteIterator();

        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                count++;
            }
        }

        return count;
    }


}
