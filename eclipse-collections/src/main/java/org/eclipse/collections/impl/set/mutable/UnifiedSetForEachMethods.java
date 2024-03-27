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

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;

public class UnifiedSetForEachMethods<T>
{
    private UnifiedSet<T> unifiedSet;

    public UnifiedSetForEachMethods(UnifiedSet<T> set){
        this.unifiedSet=set;
    }


    public void batchForEach(Procedure<? super T> procedure, int sectionIndex, int sectionCount)
    {
        Object[] set = this.unifiedSet.table;
        int sectionSize = set.length / sectionCount;
        int start = sectionSize * sectionIndex;
        int end = sectionIndex == sectionCount - 1 ? set.length : start + sectionSize;
        for (int i = start; i < end; i++)
        {
            Object cur = set[i];
            if (cur != null)
            {
                if (cur instanceof UnifiedSet.ChainedBucket)
                {
                    this.chainedForEach((UnifiedSet.ChainedBucket) cur, procedure);
                }
                else
                {
                    procedure.value(this.nonSentinel(cur));
                }
            }
        }
    }


    public MutableSet<T> tap(Procedure<? super T> procedure)
    {
        this.unifiedSet.forEach(procedure);
        return this.unifiedSet;
    }


    public void each(Procedure<? super T> procedure)
    {
        this.each(procedure, 0, this.unifiedSet.table.length);
    }

    protected void each(Procedure<? super T> procedure, int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                this.chainedForEach((UnifiedSet.ChainedBucket) cur, procedure);
            }
            else if (cur != null)
            {
                procedure.value(this.nonSentinel(cur));
            }
        }
    }


    private void chainedForEach(UnifiedSet.ChainedBucket bucket, Procedure<? super T> procedure)
    {
        do
        {
            procedure.value(this.nonSentinel(bucket.zero));
            if (bucket.one == null)
            {
                return;
            }
            procedure.value(this.nonSentinel(bucket.one));
            if (bucket.two == null)
            {
                return;
            }
            procedure.value(this.nonSentinel(bucket.two));
            if (bucket.three == null)
            {
                return;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            procedure.value(this.nonSentinel(bucket.three));
            return;
        }
        while (true);
    }


    public <P> void forEachWith(Procedure2<? super T, ? super P> procedure, P parameter)
    {
        for (int i = 0; i < this.unifiedSet.table.length; i++)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                this.chainedForEachWith((UnifiedSet.ChainedBucket) cur, procedure, parameter);
            }
            else if (cur != null)
            {
                procedure.value(this.nonSentinel(cur), parameter);
            }
        }
    }


    private <P> void chainedForEachWith(
            UnifiedSet.ChainedBucket bucket,
            Procedure2<? super T, ? super P> procedure,
            P parameter)
    {
        do
        {
            procedure.value(this.nonSentinel(bucket.zero), parameter);
            if (bucket.one == null)
            {
                return;
            }
            procedure.value(this.nonSentinel(bucket.one), parameter);
            if (bucket.two == null)
            {
                return;
            }
            procedure.value(this.nonSentinel(bucket.two), parameter);
            if (bucket.three == null)
            {
                return;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            procedure.value(this.nonSentinel(bucket.three), parameter);
            return;
        }
        while (true);
    }


    public void forEachWithIndex(ObjectIntProcedure<? super T> objectIntProcedure)
    {
        int count = 0;
        for (int i = 0; i < this.unifiedSet.table.length; i++)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                count = this.chainedForEachWithIndex((UnifiedSet.ChainedBucket) cur, objectIntProcedure, count);
            }
            else if (cur != null)
            {
                objectIntProcedure.value(this.nonSentinel(cur), count++);
            }
        }
    }


    private int chainedForEachWithIndex(UnifiedSet.ChainedBucket bucket, ObjectIntProcedure<? super T> procedure, int count)
     {
     do
     {
     procedure.value(this.nonSentinel(bucket.zero), count++);
     if (bucket.one == null)
     {
     return count;
     }
     procedure.value(this.nonSentinel(bucket.one), count++);
     if (bucket.two == null)
     {
     return count;
     }
     procedure.value(this.nonSentinel(bucket.two), count++);
     if (bucket.three == null)
     {
     return count;
     }
     if (bucket.three instanceof UnifiedSet.ChainedBucket)
     {
     bucket = (UnifiedSet.ChainedBucket) bucket.three;
     continue;
     }
     procedure.value(this.nonSentinel(bucket.three), count++);
     return count;
     }
     while (true);
     }

    private T nonSentinel(Object key)
    {
        return key == UnifiedSet.NULL_KEY ? null : (T) key;
    }
}
