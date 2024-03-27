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

import java.lang.reflect.Array;
import java.util.concurrent.ExecutorService;

import org.eclipse.collections.api.set.ParallelUnsortedSetIterable;

public class UnifiedSetConverterMethods<T>
{
    private UnifiedSet<T> unifiedSet;

    public UnifiedSetConverterMethods(UnifiedSet<T> set){
        this.unifiedSet=set;
    }


    protected boolean copySet(UnifiedSet<?> unifiedset)
    {
        //todo: optimize for current size == 0
        boolean changed = false;
        for (int i = 0; i < unifiedset.table.length; i++)
        {
            Object cur = unifiedset.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                changed |= this.copyChain((UnifiedSet.ChainedBucket) cur);
            }
            else if (cur != null)
            {
                changed |= this.unifiedSet.add(this.nonSentinel(cur));
            }
        }
        return changed;
    }


    private boolean copyChain(UnifiedSet.ChainedBucket bucket)
    {
        boolean changed = false;
        do
        {
            changed |= this.unifiedSet.add(this.nonSentinel(bucket.zero));
            if (bucket.one == null)
            {
                return changed;
            }
            changed |= this.unifiedSet.add(this.nonSentinel(bucket.one));
            if (bucket.two == null)
            {
                return changed;
            }
            changed |= this.unifiedSet.add(this.nonSentinel(bucket.two));
            if (bucket.three == null)
            {
                return changed;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            changed |= this.unifiedSet.add(this.nonSentinel(bucket.three));
            return changed;
        }
        while (true);
    }


    public Object[] toArray()
    {
        Object[] result = new Object[this.unifiedSet.occupied];
        this.copyToArray(result);
        return result;
    }


    private void copyToArray(Object[] result)
    {
        Object[] table = this.unifiedSet.table;
        int count = 0;
        for (int i = 0; i < table.length; i++)
        {
            Object cur = table[i];
            if (cur != null)
            {
                if (cur instanceof UnifiedSet.ChainedBucket)
                {
                    UnifiedSet.ChainedBucket bucket = (UnifiedSet.ChainedBucket) cur;
                    count = this.copyBucketToArray(result, bucket, count);
                }
                else
                {
                    result[count++] = this.nonSentinel(cur);
                }
            }
        }
    }


    private int copyBucketToArray(Object[] result, UnifiedSet.ChainedBucket bucket, int count)
    {
        do
        {
            result[count++] = this.nonSentinel(bucket.zero);
            if (bucket.one == null)
            {
                break;
            }
            result[count++] = this.nonSentinel(bucket.one);
            if (bucket.two == null)
            {
                break;
            }
            result[count++] = this.nonSentinel(bucket.two);
            if (bucket.three == null)
            {
                break;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            result[count++] = this.nonSentinel(bucket.three);
            break;
        }
        while (true);
        return count;
    }


    public <T> T[] toArray(T[] array)
    {
        int size = this.unifiedSet.size();
        T[] result = array.length < size
                ? (T[]) Array.newInstance(array.getClass().getComponentType(), size)
                : array;

        this.copyToArray(result);
        if (size < result.length)
        {
            result[size] = null;
        }
        return result;
    }


    private T nonSentinel(Object key)
    {
        return key == UnifiedSet.NULL_KEY ? null : (T) key;
    }
}
