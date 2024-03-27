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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;

public class UnifiedSetPredicateMethods<T>
{
    private UnifiedSet<T> unifiedSet;

    public UnifiedSetPredicateMethods(UnifiedSet<T> set){
        this.unifiedSet=set;
    }


    public boolean contains(Object key, int index, Object[] table)
    {
        Object cur = table[index];
        if (cur == null)
        {
            return false;
        }
        if (cur instanceof UnifiedSet.ChainedBucket)
        {
            return this.chainContains((UnifiedSet.ChainedBucket) cur, (T) key);
        }
        return this.nonNullTableObjectEquals(cur, (T) key);
    }


    public boolean chainContains(UnifiedSet.ChainedBucket bucket, T key)
    {
        do
        {
            if (this.nonNullTableObjectEquals(bucket.zero, key))
            {
                return true;
            }
            if (bucket.one == null)
            {
                return false;
            }
            if (this.nonNullTableObjectEquals(bucket.one, key))
            {
                return true;
            }
            if (bucket.two == null)
            {
                return false;
            }
            if (this.nonNullTableObjectEquals(bucket.two, key))
            {
                return true;
            }
            if (bucket.three == null)
            {
                return false;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            return this.nonNullTableObjectEquals(bucket.three, key);
        }
        while (true);
    }


    protected boolean shortCircuit(
            Predicate<? super T> predicate,
            boolean expected,
            boolean onShortCircuit,
            boolean atEnd,
            int start,
            int end)
    {
        for (int i = start; i < end; i++)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                if (this.chainedShortCircuit((UnifiedSet.ChainedBucket) cur, predicate, expected))
                {
                    return onShortCircuit;
                }
            }
            else if (cur != null)
            {
                T each = this.nonSentinel(cur);
                if (predicate.accept(each) == expected)
                {
                    return onShortCircuit;
                }
            }
        }
        return atEnd;
    }


    protected <P> boolean shortCircuitWith(
            Predicate2<? super T, ? super P> predicate2,
            P parameter,
            boolean expected,
            boolean onShortCircuit,
            boolean atEnd, Object[] table)
    {
        for (int i = 0; i < table.length; i++)
        {
            Object cur = table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                if (this.chainedShortCircuitWith((UnifiedSet.ChainedBucket) cur, predicate2, parameter, expected))
                {
                    return onShortCircuit;
                }
            }
            else if (cur != null)
            {
                T each = this.nonSentinel(cur);
                if (predicate2.accept(each, parameter) == expected)
                {
                    return onShortCircuit;
                }
            }
        }
        return atEnd;
    }

    private <P> boolean chainedShortCircuitWith(
            UnifiedSet.ChainedBucket bucket,
            Predicate2<? super T, ? super P> predicate,
            P parameter,
            boolean expected)
    {
        do
        {
            if (predicate.accept(this.nonSentinel(bucket.zero), parameter) == expected)
            {
                return true;
            }
            if (bucket.one == null)
            {
                return false;
            }
            if (predicate.accept(this.nonSentinel(bucket.one), parameter) == expected)
            {
                return true;
            }
            if (bucket.two == null)
            {
                return false;
            }
            if (predicate.accept(this.nonSentinel(bucket.two), parameter) == expected)
            {
                return true;
            }
            if (bucket.three == null)
            {
                return false;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            return predicate.accept(this.nonSentinel(bucket.three), parameter) == expected;
        }
        while (true);
    }


    private boolean chainedShortCircuit(
            UnifiedSet.ChainedBucket bucket,
            Predicate<? super T> predicate,
            boolean expected)
    {
        do
        {
            if (predicate.accept(this.nonSentinel(bucket.zero)) == expected)
            {
                return true;
            }
            if (bucket.one == null)
            {
                return false;
            }
            if (predicate.accept(this.nonSentinel(bucket.one)) == expected)
            {
                return true;
            }
            if (bucket.two == null)
            {
                return false;
            }
            if (predicate.accept(this.nonSentinel(bucket.two)) == expected)
            {
                return true;
            }
            if (bucket.three == null)
            {
                return false;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            return predicate.accept(this.nonSentinel(bucket.three)) == expected;
        }
        while (true);
    }


    private T nonSentinel(Object key)
    {
        return key == UnifiedSet.NULL_KEY ? null : (T) key;
    }


    private boolean nonNullTableObjectEquals(Object cur, T key)
    {
        return cur.equals(key) || (cur.equals(UnifiedSet.NULL_KEY) ? key == null : cur.equals(key));
    }
}
