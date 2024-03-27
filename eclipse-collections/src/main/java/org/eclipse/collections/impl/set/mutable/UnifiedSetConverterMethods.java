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
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.set.ParallelUnsortedSetIterable;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.parallel.AbstractBatch;
import org.eclipse.collections.impl.lazy.parallel.AbstractParallelIterable;
import org.eclipse.collections.impl.lazy.parallel.bag.CollectUnsortedBagBatch;
import org.eclipse.collections.impl.lazy.parallel.bag.FlatCollectUnsortedBagBatch;
import org.eclipse.collections.impl.lazy.parallel.bag.UnsortedBagBatch;
import org.eclipse.collections.impl.lazy.parallel.set.AbstractParallelUnsortedSetIterable;
import org.eclipse.collections.impl.lazy.parallel.set.RootUnsortedSetBatch;
import org.eclipse.collections.impl.lazy.parallel.set.SelectUnsortedSetBatch;
import org.eclipse.collections.impl.lazy.parallel.set.UnsortedSetBatch;

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


    private final class UnifiedUnsortedSetBatch extends AbstractBatch<T> implements RootUnsortedSetBatch<T>
    {
        private final int chunkStartIndex;
        private final int chunkEndIndex;

        private UnifiedUnsortedSetBatch(int chunkStartIndex, int chunkEndIndex)
        {
            this.chunkStartIndex = chunkStartIndex;
            this.chunkEndIndex = chunkEndIndex;
        }

        @Override
        public void forEach(Procedure<? super T> procedure)
        {
            UnifiedSetConverterMethods.this.unifiedSet.forEachMethods.each(procedure, this.chunkStartIndex, this.chunkEndIndex);
        }

        @Override
        public boolean anySatisfy(Predicate<? super T> predicate)
        {
            return UnifiedSetConverterMethods.this.unifiedSet.shortCircuit(predicate, true, true, false, this.chunkStartIndex, this.chunkEndIndex);
        }

        @Override
        public boolean allSatisfy(Predicate<? super T> predicate)
        {
            return UnifiedSetConverterMethods.this.unifiedSet.shortCircuit(predicate, false, false, true, this.chunkStartIndex, this.chunkEndIndex);
        }

        @Override
        public T detect(Predicate<? super T> predicate)
        {
            return UnifiedSetConverterMethods.this.unifiedSet.detect(predicate, this.chunkStartIndex, this.chunkEndIndex);
        }

        @Override
        public UnsortedSetBatch<T> select(Predicate<? super T> predicate)
        {
            return new SelectUnsortedSetBatch<>(this, predicate);
        }

        @Override
        public <V> UnsortedBagBatch<V> collect(Function<? super T, ? extends V> function)
        {
            return new CollectUnsortedBagBatch<>(this, function);
        }

        @Override
        public <V> UnsortedBagBatch<V> flatCollect(Function<? super T, ? extends Iterable<V>> function)
        {
            return new FlatCollectUnsortedBagBatch<>(this, function);
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


    public ParallelUnsortedSetIterable<T> asParallel(ExecutorService executorService, int batchSize)
    {
        if (executorService == null)
        {
            throw new NullPointerException();
        }
        if (batchSize < 1)
        {
            throw new IllegalArgumentException();
        }
        return new UnifiedSetConverterMethods.UnifiedSetParallelUnsortedIterable(executorService, batchSize);
    }


    private final class UnifiedSetParallelUnsortedIterable extends AbstractParallelUnsortedSetIterable<T, RootUnsortedSetBatch<T>>
    {
        private final ExecutorService executorService;
        private final int batchSize;

        private UnifiedSetParallelUnsortedIterable(ExecutorService executorService, int batchSize)
        {
            this.executorService = executorService;
            this.batchSize = batchSize;
        }

        @Override
        public ExecutorService getExecutorService()
        {
            return this.executorService;
        }

        @Override
        public int getBatchSize()
        {
            return this.batchSize;
        }

        @Override
        public LazyIterable<RootUnsortedSetBatch<T>> split()
        {
            return new UnifiedSetConverterMethods.UnifiedSetParallelUnsortedIterable.UnifiedSetParallelSplitLazyIterable();
        }

        @Override
        public void forEach(Procedure<? super T> procedure)
        {
            AbstractParallelIterable.forEach(this, procedure);
        }

        @Override
        public boolean anySatisfy(Predicate<? super T> predicate)
        {
            return AbstractParallelIterable.anySatisfy(this, predicate);
        }

        @Override
        public boolean allSatisfy(Predicate<? super T> predicate)
        {
            return AbstractParallelIterable.allSatisfy(this, predicate);
        }

        @Override
        public T detect(Predicate<? super T> predicate)
        {
            return AbstractParallelIterable.detect(this, predicate);
        }

        @Override
        public Object[] toArray()
        {
            // TODO: Implement in parallel
            return UnifiedSetConverterMethods.this.toArray();
        }

        @Override
        public <E> E[] toArray(E[] array)
        {
            // TODO: Implement in parallel
            return UnifiedSetConverterMethods.this.toArray(array);
        }

        private class UnifiedSetParallelSplitIterator implements Iterator<UnifiedUnsortedSetBatch>
        {
            protected int chunkIndex;

            @Override
            public boolean hasNext()
            {
                return this.chunkIndex * UnifiedSetConverterMethods.UnifiedSetParallelUnsortedIterable.this.batchSize < UnifiedSetConverterMethods.this.unifiedSet.table.length;
            }

            @Override
            public UnifiedUnsortedSetBatch next()
            {
                int chunkStartIndex = this.chunkIndex * UnifiedSetConverterMethods.UnifiedSetParallelUnsortedIterable.this.batchSize;
                int chunkEndIndex = (this.chunkIndex + 1) * UnifiedSetConverterMethods.UnifiedSetParallelUnsortedIterable.this.batchSize;
                int truncatedChunkEndIndex = Math.min(chunkEndIndex, UnifiedSetConverterMethods.this.unifiedSet.table.length);
                this.chunkIndex++;
                return new UnifiedSetConverterMethods.UnifiedUnsortedSetBatch(chunkStartIndex, truncatedChunkEndIndex);
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
            }
        }

        private class UnifiedSetParallelSplitLazyIterable
                extends AbstractLazyIterable<RootUnsortedSetBatch<T>>
        {
            @Override
            public void each(Procedure<? super RootUnsortedSetBatch<T>> procedure)
            {
                for (RootUnsortedSetBatch<T> chunk : this)
                {
                    procedure.value(chunk);
                }
            }

            @Override
            public Iterator<RootUnsortedSetBatch<T>> iterator()
            {
                return new UnifiedSetConverterMethods.UnifiedSetParallelUnsortedIterable.UnifiedSetParallelSplitIterator();
            }
        }
    }


    private T nonSentinel(Object key)
    {
        return key == UnifiedSet.NULL_KEY ? null : (T) key;
    }
}
