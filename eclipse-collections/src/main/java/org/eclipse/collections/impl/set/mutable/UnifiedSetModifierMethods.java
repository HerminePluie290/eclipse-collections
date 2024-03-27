/*
 * Copyright (c) 2024 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.partition.set.PartitionMutableSet;
import org.eclipse.collections.impl.block.factory.Procedures2;
import org.eclipse.collections.impl.block.procedure.PartitionPredicate2Procedure;
import org.eclipse.collections.impl.block.procedure.PartitionProcedure;
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;
import org.eclipse.collections.impl.partition.set.PartitionUnifiedSet;
import org.eclipse.collections.impl.utility.Iterate;

public class UnifiedSetModifierMethods<T>
{
    private final UnifiedSet<T>  unifiedSet;


    public UnifiedSetModifierMethods(UnifiedSet<T> set){
        this.unifiedSet=set;
    }


    public void clear()
    {
        if (this.unifiedSet.occupied == 0)
        {
            return;
        }
        this.unifiedSet.occupied = 0;
        Object[] set = this.unifiedSet.table;

        for (int i = set.length; i-- > 0; )
        {
            set[i] = null;
        }
    }


    private static Object toSentinelIfNull(Object key)
    {
        if (key == null)
        {
            return UnifiedSet.NULL_KEY;
        }
        return key;
    }



    public PartitionMutableSet<T> partition(Predicate<? super T> predicate)
    {
        PartitionMutableSet<T> partitionMutableSet = new PartitionUnifiedSet<>();
        this.unifiedSet.forEach(new PartitionProcedure<>(predicate, partitionMutableSet));
        return partitionMutableSet;
    }


    public <P> PartitionMutableSet<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        PartitionMutableSet<T> partitionMutableSet = new PartitionUnifiedSet<>();
        this.unifiedSet.forEach(new PartitionPredicate2Procedure<>(predicate, parameter, partitionMutableSet));
        return partitionMutableSet;
    }


    public UnifiedSet<T> with(T element)
    {
        this.unifiedSet.add(element);
        return this.unifiedSet;
    }


    public UnifiedSet<T> with(T element1, T element2)
    {
        this.unifiedSet.add(element1);
        this.unifiedSet.add(element2);
        return this.unifiedSet;
    }

    public UnifiedSet<T> with(T element1, T element2, T element3)
    {
        this.unifiedSet.add(element1);
        this.unifiedSet.add(element2);
        this.unifiedSet.add(element3);
        return this.unifiedSet;
    }

    public UnifiedSet<T> with(T... elements)
    {
        this.unifiedSet.addAll(Arrays.asList(elements));
        return this.unifiedSet;
    }


    public UnifiedSet<T> withAll(Iterable<? extends T> iterable)
    {
        this.unifiedSet.addAllIterable(iterable);
        return this.unifiedSet;
    }


    public UnifiedSet<T> without(T element)
    {
        this.unifiedSet.remove(element);
        return this.unifiedSet;
    }


    public UnifiedSet<T> withoutAll(Iterable<? extends T> elements)
    {
        this.unifiedSet.removeAllIterable(elements);
        return this.unifiedSet;
    }


    public boolean remove(Object key)
    {
        int index = this.unifiedSet.index(key);

        Object cur = this.unifiedSet.table[index];
        if (cur == null)
        {
            return false;
        }
        if (cur instanceof UnifiedSet.ChainedBucket)
        {
            return this.removeFromChain((UnifiedSet.ChainedBucket) cur, (T) key, index);
        }
        if (this.nonNullTableObjectEquals(cur, (T) key))
        {
            this.unifiedSet.table[index] = null;
            this.unifiedSet.occupied--;
            return true;
        }
        return false;
    }


    private boolean removeFromChain(UnifiedSet.ChainedBucket bucket, T key, int index)
    {
        if (this.nonNullTableObjectEquals(bucket.zero, key))
        {
            bucket.zero = bucket.removeLast(0);
            if (bucket.zero == null)
            {
                this.unifiedSet.table[index] = null;
            }
            this.unifiedSet.occupied--;
            return true;
        }
        if (bucket.one == null)
        {
            return false;
        }
        if (this.nonNullTableObjectEquals(bucket.one, key))
        {
            bucket.one = bucket.removeLast(1);
            this.unifiedSet.occupied--;
            return true;
        }
        if (bucket.two == null)
        {
            return false;
        }
        if (this.nonNullTableObjectEquals(bucket.two, key))
        {
            bucket.two = bucket.removeLast(2);
            this.unifiedSet.occupied--;
            return true;
        }
        if (bucket.three == null)
        {
            return false;
        }
        if (bucket.three instanceof UnifiedSet.ChainedBucket)
        {
            return this.removeDeepChain(bucket, key);
        }
        if (this.nonNullTableObjectEquals(bucket.three, key))
        {
            bucket.three = bucket.removeLast(3);
            this.unifiedSet.occupied--;
            return true;
        }
        return false;
    }


    private boolean removeDeepChain(UnifiedSet.ChainedBucket oldBucket, T key)
    {
        do
        {
            UnifiedSet.ChainedBucket bucket = (UnifiedSet.ChainedBucket) oldBucket.three;
            if (this.nonNullTableObjectEquals(bucket.zero, key))
            {
                bucket.zero = bucket.removeLast(0);
                if (bucket.zero == null)
                {
                    oldBucket.three = null;
                }
                this.unifiedSet.occupied--;
                return true;
            }
            if (bucket.one == null)
            {
                return false;
            }
            if (this.nonNullTableObjectEquals(bucket.one, key))
            {
                bucket.one = bucket.removeLast(1);
                this.unifiedSet.occupied--;
                return true;
            }
            if (bucket.two == null)
            {
                return false;
            }
            if (this.nonNullTableObjectEquals(bucket.two, key))
            {
                bucket.two = bucket.removeLast(2);
                this.unifiedSet.occupied--;
                return true;
            }
            if (bucket.three == null)
            {
                return false;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                oldBucket = bucket;
                continue;
            }
            if (this.nonNullTableObjectEquals(bucket.three, key))
            {
                bucket.three = bucket.removeLast(3);
                this.unifiedSet.occupied--;
                return true;
            }
            return false;
        }
        while (true);
    }


    public T removeFromPool(T key)
    {
        int index = this.unifiedSet.index(key);
        Object cur = this.unifiedSet.table[index];
        if (cur == null)
        {
            return null;
        }
        if (cur instanceof UnifiedSet.ChainedBucket)
        {
            return this.removeFromChainForPool((UnifiedSet.ChainedBucket) cur, key, index);
        }
        if (this.nonNullTableObjectEquals(cur, key))
        {
            this.unifiedSet.table[index] = null;
            this.unifiedSet.occupied--;
            return this.nonSentinel(cur);
        }
        return null;
    }


    private T removeFromChainForPool(UnifiedSet.ChainedBucket bucket, T key, int index)
    {
        if (this.nonNullTableObjectEquals(bucket.zero, key))
        {
            Object result = bucket.zero;
            bucket.zero = bucket.removeLast(0);
            if (bucket.zero == null)
            {
                this.unifiedSet.table[index] = null;
            }
            this.unifiedSet.occupied--;
            return this.nonSentinel(result);
        }
        if (bucket.one == null)
        {
            return null;
        }
        if (this.nonNullTableObjectEquals(bucket.one, key))
        {
            Object result = bucket.one;
            bucket.one = bucket.removeLast(1);
            this.unifiedSet.occupied--;
            return this.nonSentinel(result);
        }
        if (bucket.two == null)
        {
            return null;
        }
        if (this.nonNullTableObjectEquals(bucket.two, key))
        {
            Object result = bucket.two;
            bucket.two = bucket.removeLast(2);
            this.unifiedSet.occupied--;
            return this.nonSentinel(result);
        }
        if (bucket.three == null)
        {
            return null;
        }
        if (bucket.three instanceof UnifiedSet.ChainedBucket)
        {
            return this.removeDeepChainForPool(bucket, key);
        }
        if (this.nonNullTableObjectEquals(bucket.three, key))
        {
            Object result = bucket.three;
            bucket.three = bucket.removeLast(3);
            this.unifiedSet.occupied--;
            return this.nonSentinel(result);
        }
        return null;
    }

    private T removeDeepChainForPool(UnifiedSet.ChainedBucket oldBucket, T key)
    {
        do
        {
            UnifiedSet.ChainedBucket bucket = (UnifiedSet.ChainedBucket) oldBucket.three;
            if (this.nonNullTableObjectEquals(bucket.zero, key))
            {
                Object result = bucket.zero;
                bucket.zero = bucket.removeLast(0);
                if (bucket.zero == null)
                {
                    oldBucket.three = null;
                }
                this.unifiedSet.occupied--;
                return this.nonSentinel(result);
            }
            if (bucket.one == null)
            {
                return null;
            }
            if (this.nonNullTableObjectEquals(bucket.one, key))
            {
                Object result = bucket.one;
                bucket.one = bucket.removeLast(1);
                this.unifiedSet.occupied--;
                return this.nonSentinel(result);
            }
            if (bucket.two == null)
            {
                return null;
            }
            if (this.nonNullTableObjectEquals(bucket.two, key))
            {
                Object result = bucket.two;
                bucket.two = bucket.removeLast(2);
                this.unifiedSet.occupied--;
                return this.nonSentinel(result);
            }
            if (bucket.three == null)
            {
                return null;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                oldBucket = bucket;
                continue;
            }
            if (this.nonNullTableObjectEquals(bucket.three, key))
            {
                Object result = bucket.three;
                bucket.three = bucket.removeLast(3);
                this.unifiedSet.occupied--;
                return this.nonSentinel(result);
            }
            return null;
        }
        while (true);
    }
    private boolean nonNullTableObjectEquals(Object cur, T key)
    {
        return cur == key || (cur == UnifiedSet.NULL_KEY ? key == null : cur.equals(key));
    }


    private T nonSentinel(Object key)
    {
        return key == UnifiedSet.NULL_KEY ? null : (T) key;
    }

}
