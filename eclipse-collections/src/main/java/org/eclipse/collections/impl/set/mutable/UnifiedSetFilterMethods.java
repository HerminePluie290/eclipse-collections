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

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.block.procedure.SelectInstancesOfProcedure;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;

public class UnifiedSetFilterMethods<T>
{
    private UnifiedSet<T> unifiedSet;

    public UnifiedSetFilterMethods(UnifiedSet<T> set){
        this.unifiedSet=set;
    }


    public <P> Twin<MutableList<T>> selectAndRejectWith(
            Predicate2<? super T, ? super P> predicate,
            P parameter)
    {
        MutableList<T> positiveResult = Lists.mutable.empty();
        MutableList<T> negativeResult = Lists.mutable.empty();
        this.unifiedSet.forEachWith((each, parm) -> (predicate.accept(each, parm) ? positiveResult : negativeResult).add(each), parameter);
        return Tuples.twin(positiveResult, negativeResult);
    }


    public <S> UnifiedSet<S> selectInstancesOf(Class<S> clazz)
    {
        UnifiedSet<S> result = UnifiedSet.newSet();
        this.unifiedSet.forEach(new SelectInstancesOfProcedure<>(clazz, result));
        return result;
    }


    protected T detect(Predicate<? super T> predicate, int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                Object chainedDetect = this.chainedDetect((UnifiedSet.ChainedBucket) cur, predicate);
                if (chainedDetect != null)
                {
                    return this.nonSentinel(chainedDetect);
                }
            }
            else if (cur != null)
            {
                T each = this.nonSentinel(cur);
                if (predicate.accept(each))
                {
                    return each;
                }
            }
        }
        return null;
    }


    protected Optional<T> detectOptional(Predicate<? super T> predicate, int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                Object chainedDetect = this.chainedDetect((UnifiedSet.ChainedBucket) cur, predicate);
                if (chainedDetect != null)
                {
                    return Optional.of(this.nonSentinel(chainedDetect));
                }
            }
            else if (cur != null)
            {
                T each = this.nonSentinel(cur);
                if (predicate.accept(each))
                {
                    return Optional.of(each);
                }
            }
        }
        return Optional.empty();
    }


    private Object chainedDetect(UnifiedSet.ChainedBucket bucket, Predicate<? super T> predicate)
    {
        do
        {
            if (predicate.accept(this.nonSentinel(bucket.zero)))
            {
                return bucket.zero;
            }
            if (bucket.one == null)
            {
                return null;
            }
            if (predicate.accept(this.nonSentinel(bucket.one)))
            {
                return bucket.one;
            }
            if (bucket.two == null)
            {
                return null;
            }
            if (predicate.accept(this.nonSentinel(bucket.two)))
            {
                return bucket.two;
            }
            if (bucket.three == null)
            {
                return null;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            if (predicate.accept(this.nonSentinel(bucket.three)))
            {
                return bucket.three;
            }
            return null;
        }
        while (true);
    }


    private T nonSentinel(Object key)
    {
        return key == UnifiedSet.NULL_KEY ? null : (T) key;
    }
}
