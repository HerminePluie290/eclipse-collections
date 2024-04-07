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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.set.primitive.ByteSet;

public class ByteHashSetPredicates
{
    private static final long serialVersionUID = 1L;
    private static final byte MAX_BYTE_GROUP_1 = -65;
    private static final byte MAX_BYTE_GROUP_2 = -1;
    private static final byte MAX_BYTE_GROUP_3 = 63;
    protected ByteHashSet bhset;



    public ByteHashSetPredicates(ByteHashSet set){this.bhset = set;}




    public boolean contains(byte value)
    {
        if (value <= MAX_BYTE_GROUP_1)
        {
            return ((this.bhset.getBitGroup1() >>> (byte) ((value + 1) * -1)) & 1L) != 0;
        }
        if (value <= MAX_BYTE_GROUP_2)
        {
            return ((this.bhset.getBitGroup2() >>> (byte) ((value + 1) * -1)) & 1L) != 0;
        }
        if (value <= MAX_BYTE_GROUP_3)
        {
            return ((this.bhset.getBitGroup3() >>> value) & 1L) != 0;
        }

        return ((this.bhset.getBitGroup4() >>> value) & 1L) != 0;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ByteSet))
        {
            return false;
        }

        ByteSet other = (ByteSet) obj;
        return this.bhset.size() == other.size() && this.bhset.containsAll(other.toArray());
    }

    public boolean containsAll(byte... source)
    {
        for (byte item : source)
        {
            if (!this.bhset.contains(item))
            {
                return false;
            }
        }
        return true;
    }

    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    public boolean anySatisfy(BytePredicate predicate)
    {
        ByteIterator iterator = this.bhset.byteIterator();

        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                return true;
            }
        }

        return false;
    }

    public boolean allSatisfy(BytePredicate predicate)
    {
        ByteIterator iterator = this.bhset.byteIterator();

        while (iterator.hasNext())
        {
            if (!predicate.accept(iterator.next()))
            {
                return false;
            }
        }

        return true;
    }

    public boolean noneSatisfy(BytePredicate predicate)
    {
        ByteIterator iterator = this.bhset.byteIterator();

        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                return false;
            }
        }

        return true;
    }
}
