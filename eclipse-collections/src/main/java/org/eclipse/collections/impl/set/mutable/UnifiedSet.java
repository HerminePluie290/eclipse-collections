/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.annotation.Beta;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.set.PartitionMutableSet;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.ParallelUnsortedSetIterable;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.block.factory.Procedures2;
import org.eclipse.collections.impl.block.procedure.PartitionPredicate2Procedure;
import org.eclipse.collections.impl.block.procedure.PartitionProcedure;
import org.eclipse.collections.impl.block.procedure.SelectInstancesOfProcedure;
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
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;
import org.eclipse.collections.impl.partition.set.PartitionUnifiedSet;
import org.eclipse.collections.impl.set.AbstractUnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;

public class UnifiedSet<T>
        extends AbstractUnifiedSet<T>
        implements Externalizable
{
    protected static final Object NULL_KEY = new Object()
    {
        @Override
        public boolean equals(Object obj)
        {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        @Override
        public int hashCode()
        {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        @Override
        public String toString()
        {
            return "UnifiedSet.NULL_KEY";
        }
    };

    private static final long serialVersionUID = 1L;

    protected transient Object[] table;

    protected transient int occupied;

    protected UnifiedSetModifierMethods<T> modifierMethods;

    protected UnifiedSetForEachMethods<T> forEachMethods;

    protected UnifiedSetFilterMethods<T> filterMethods;

    protected UnifiedSetConverterMethods<T> converterMethods;

    protected UnifiedSetPredicateMethods<T> predicateMethods;

    protected UnifiedSetMiscellaneousMethods<T> miscellaneousMethods;

    public UnifiedSet()
    {
        this.allocate(DEFAULT_INITIAL_CAPACITY << 1);
        this.loadFactor = loadFactor;
        this.modifierMethods = new UnifiedSetModifierMethods<>(this);
        this.forEachMethods = new UnifiedSetForEachMethods<>(this);
        this.filterMethods = new UnifiedSetFilterMethods<>(this);
        this.converterMethods = new UnifiedSetConverterMethods<>(this);
        this.predicateMethods = new UnifiedSetPredicateMethods<>(this);
        this.miscellaneousMethods = new UnifiedSetMiscellaneousMethods<>(this);
    }

    public UnifiedSet(int initialCapacity)
    {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public UnifiedSet(int initialCapacity, float loadFactor)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        if (loadFactor <= 0.0)
        {
            throw new IllegalArgumentException("load factor cannot be less than or equal to 0");
        }
        if (loadFactor > 1.0)
        {
            throw new IllegalArgumentException("load factor cannot be greater than 1");
        }
        this.loadFactor = loadFactor;
        this.modifierMethods = new UnifiedSetModifierMethods<>(this);
        this.forEachMethods = new UnifiedSetForEachMethods<>(this);
        this.filterMethods = new UnifiedSetFilterMethods<>(this);
        this.converterMethods = new UnifiedSetConverterMethods<>(this);
        this.predicateMethods = new UnifiedSetPredicateMethods<>(this);
        this.miscellaneousMethods = new UnifiedSetMiscellaneousMethods<>(this);
        this.init(this.fastCeil(initialCapacity / loadFactor));
    }

    public UnifiedSet(Collection<? extends T> collection)
    {
        this(Math.max(collection.size(), DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        this.addAll(collection);
    }

    public UnifiedSet(UnifiedSet<T> set)
    {
        this.maxSize = set.maxSize;
        this.loadFactor = set.loadFactor;
        this.occupied = set.occupied;
        this.allocateTable(set.table.length);
        this.modifierMethods = set.modifierMethods;
        this.forEachMethods = set.forEachMethods;
        this.filterMethods = set.filterMethods;
        this.converterMethods = set.converterMethods;
        this.predicateMethods = set.predicateMethods;
        this.miscellaneousMethods = set.miscellaneousMethods;

        for (int i = 0; i < set.table.length; i++)
        {
            Object key = set.table[i];
            if (key instanceof ChainedBucket)
            {
                this.table[i] = ((ChainedBucket) key).copy();
            }
            else if (key != null)
            {
                this.table[i] = key;
            }
        }
    }

    public static <K> UnifiedSet<K> newSet()
    {
        return new UnifiedSet<>();  
    }

    public static <K> UnifiedSet<K> newSet(int size)
    {
        return new UnifiedSet<>(size);
    }

    public static <K> UnifiedSet<K> newSet(Iterable<? extends K> source)
    {
        if (source instanceof UnifiedSet)
        {
            return new UnifiedSet<>((UnifiedSet<K>) source);
        }
        if (source instanceof Collection)
        {
            return new UnifiedSet<>((Collection<K>) source);
        }
        if (source == null)
        {
            throw new NullPointerException();
        }
        UnifiedSet<K> result = source instanceof RichIterable
                ? UnifiedSet.newSet(((RichIterable<?>) source).size())
                : UnifiedSet.newSet();
        Iterate.forEachWith(source, Procedures2.addToCollection(), result);
        return result;
    }

    public static <K> UnifiedSet<K> newSet(int size, float loadFactor)
    {
        return new UnifiedSet<>(size, loadFactor);
    }

    public static <K> UnifiedSet<K> newSetWith(K... elements)
    {
        return UnifiedSet.<K>newSet(elements.length).with(elements);
    }

    private int fastCeil(float v)
    {
        int possibleResult = (int) v;
        if (v - possibleResult > 0.0F)
        {
            possibleResult++;
        }
        return possibleResult;
    }

    public float getLoadFactor(){
        return this.loadFactor;
    }

    @Override
    protected Object[] getTable()
    {
        return this.table;
    }

    @Override
    protected void allocateTable(int sizeToAllocate)
    {
        this.table = new Object[sizeToAllocate];
    }

    protected int index(Object key)
    {
        return this.miscellaneousMethods.index(key);
    }

    @Override
    public void clear()
    {
        this.modifierMethods.clear();
    }

    @Override
    public boolean add(T key)
    {
        int index = this.index(key);
        Object cur = this.table[index];
        if (cur == null)
        {
            this.table[index] = UnifiedSet.toSentinelIfNull(key);
            if (++this.occupied > this.maxSize)
            {
                this.rehash();
            }
            return true;
        }
        if (cur instanceof ChainedBucket || !this.nonNullTableObjectEquals(cur, key))
        {
            return this.chainedAdd(key, index);
        }
        return false;
    }

    private boolean chainedAdd(T key, int index)
    {
        Object realKey = UnifiedSet.toSentinelIfNull(key);
        if (this.table[index] instanceof ChainedBucket)
        {
            ChainedBucket bucket = (ChainedBucket) this.table[index];
            do
            {
                if (this.nonNullTableObjectEquals(bucket.zero, key))
                {
                    return false;
                }
                if (bucket.one == null)
                {
                    bucket.one = realKey;
                    if (++this.occupied > this.maxSize)
                    {
                        this.rehash();
                    }
                    return true;
                }
                if (this.nonNullTableObjectEquals(bucket.one, key))
                {
                    return false;
                }
                if (bucket.two == null)
                {
                    bucket.two = realKey;
                    if (++this.occupied > this.maxSize)
                    {
                        this.rehash();
                    }
                    return true;
                }
                if (this.nonNullTableObjectEquals(bucket.two, key))
                {
                    return false;
                }
                if (bucket.three instanceof ChainedBucket)
                {
                    bucket = (ChainedBucket) bucket.three;
                    continue;
                }
                if (bucket.three == null)
                {
                    bucket.three = realKey;
                    if (++this.occupied > this.maxSize)
                    {
                        this.rehash();
                    }
                    return true;
                }
                if (this.nonNullTableObjectEquals(bucket.three, key))
                {
                    return false;
                }
                bucket.three = new ChainedBucket(bucket.three, realKey);
                if (++this.occupied > this.maxSize)
                {
                    this.rehash();
                }
                return true;
            }
            while (true);
        }
        ChainedBucket newBucket = new ChainedBucket(this.table[index], realKey);
        this.table[index] = newBucket;
        if (++this.occupied > this.maxSize)
        {
            this.rehash();
        }
        return true;
    }

    @Override
    protected void rehash(int newCapacity)
    {
        int oldLength = this.table.length;
        Object[] old = this.table;
        this.allocate(newCapacity);
        this.occupied = 0;

        for (int i = 0; i < oldLength; i++)
        {
            Object oldKey = old[i];
            if (oldKey instanceof ChainedBucket)
            {
                ChainedBucket bucket = (ChainedBucket) oldKey;
                do
                {
                    if (bucket.zero != null)
                    {
                        this.add(this.nonSentinel(bucket.zero));
                    }
                    if (bucket.one == null)
                    {
                        break;
                    }
                    this.add(this.nonSentinel(bucket.one));
                    if (bucket.two == null)
                    {
                        break;
                    }
                    this.add(this.nonSentinel(bucket.two));
                    if (bucket.three != null)
                    {
                        if (bucket.three instanceof ChainedBucket)
                        {
                            bucket = (ChainedBucket) bucket.three;
                            continue;
                        }
                        this.add(this.nonSentinel(bucket.three));
                    }
                    break;
                }
                while (true);
            }
            else if (oldKey != null)
            {
                this.add(this.nonSentinel(oldKey));
            }
        }
    }

    @Override
    public boolean contains(Object key)
    {
        int index = this.index(key);
        Object cur = this.table[index];
        if (cur == null)
        {
            return false;
        }
        if (cur instanceof ChainedBucket)
        {
            return this.chainContains((ChainedBucket) cur, (T) key);
        }
        return this.nonNullTableObjectEquals(cur, (T) key);
    }

    private boolean chainContains(ChainedBucket bucket, T key)
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
            if (bucket.three instanceof ChainedBucket)
            {
                bucket = (ChainedBucket) bucket.three;
                continue;
            }
            return this.nonNullTableObjectEquals(bucket.three, key);
        }
        while (true);
    }

    @Override
    public void batchForEach(Procedure<? super T> procedure, int sectionIndex, int sectionCount)
    {
        this.forEachMethods.batchForEach(procedure, sectionIndex, sectionCount);
    }

    @Override
    public MutableSet<T> tap(Procedure<? super T> procedure)
    {
        return this.forEachMethods.tap(procedure);
    }

    @Override
    public void each(Procedure<? super T> procedure)
    {
        this.forEachMethods.each(procedure);
    }


    @Override
    public <P> void forEachWith(Procedure2<? super T, ? super P> procedure, P parameter)
    {
        this.forEachMethods.forEachWith(procedure, parameter);
    }


    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super T> objectIntProcedure)
    {
        this.forEachMethods.forEachWithIndex(objectIntProcedure);
    }


    @Override
    public UnifiedSet<T> newEmpty()
    {
        return UnifiedSet.newSet();
    }

    @Override
    public UnifiedSet<T> newEmpty(int size)
    {
        return UnifiedSet.newSet(size, this.loadFactor);
    }

    @Override
    public T getFirst()
    {
        return this.miscellaneousMethods.getFirst();
    }

    @Override
    public T getLast()
    {
        return this.miscellaneousMethods.getLast();
    }


    @Override
    public UnifiedSet<T> select(Predicate<? super T> predicate)
    {
        return this.select(predicate, this.newEmpty());
    }

    @Override
    public <P> UnifiedSet<T> selectWith(
            Predicate2<? super T, ? super P> predicate,
            P parameter)
    {
        return this.selectWith(predicate, parameter, this.newEmpty());
    }

    @Override
    public UnifiedSet<T> reject(Predicate<? super T> predicate)
    {
        return this.reject(predicate, this.newEmpty());
    }

    @Override
    public <P> UnifiedSet<T> rejectWith(
            Predicate2<? super T, ? super P> predicate,
            P parameter)
    {
        return this.rejectWith(predicate, parameter, this.newEmpty());
    }

    @Override
    public <P> Twin<MutableList<T>> selectAndRejectWith(
            Predicate2<? super T, ? super P> predicate,
            P parameter)
    {
        return this.filterMethods.selectAndRejectWith(predicate, parameter);
    }

    @Override
    public PartitionMutableSet<T> partition(Predicate<? super T> predicate)
    {
        return this.modifierMethods.partition(predicate);
    }

    @Override
    public <P> PartitionMutableSet<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return this.modifierMethods.partitionWith(predicate, parameter);
    }

    @Override
    public <S> UnifiedSet<S> selectInstancesOf(Class<S> clazz)
    {
        return this.filterMethods.selectInstancesOf(clazz);
    }

    @Override
    protected T detect(Predicate<? super T> predicate, int start, int end)
    {
        return this.filterMethods.detect(predicate,start,end);
    }

    @Override
    protected Optional<T> detectOptional(Predicate<? super T> predicate, int start, int end)
    {
        return this.filterMethods.detectOptional(predicate,start,end);
    }


    @Override
    protected boolean shortCircuit(
            Predicate<? super T> predicate,
            boolean expected,
            boolean onShortCircuit,
            boolean atEnd,
            int start,
            int end)
    {
        return this.predicateMethods.shortCircuit(predicate, expected, onShortCircuit, atEnd, start, end);
    }


    @Override
    protected <P> boolean shortCircuitWith(
            Predicate2<? super T, ? super P> predicate2,
            P parameter,
            boolean expected,
            boolean onShortCircuit,
            boolean atEnd)
    {
        return this.predicateMethods.shortCircuitWith(predicate2, parameter, expected, onShortCircuit, atEnd, this.table);
    }


    @Override
    public ImmutableSet<T> toImmutable()
    {
        return Sets.immutable.withAll(this);
    }

    @Override
    public UnifiedSet<T> with(T element)
    {
        this.add(element);
        return this;
        //return this.modifierMethods.with(element);
    }

    public UnifiedSet<T> with(T element1, T element2)
    {
        return this.modifierMethods.with(element1, element2);
    }

    public UnifiedSet<T> with(T element1, T element2, T element3)
    {
        return this.modifierMethods.with(element1, element2, element3);
    }

    public UnifiedSet<T> with(T... elements)
    {
        return this.modifierMethods.with(elements);
    }

    @Override
    public UnifiedSet<T> withAll(Iterable<? extends T> iterable)
    {
        return this.modifierMethods.withAll(iterable);
    }

    @Override
    public UnifiedSet<T> without(T element)
    {
        return this.modifierMethods.without(element);
    }

    @Override
    public UnifiedSet<T> withoutAll(Iterable<? extends T> elements)
    {
        return this.modifierMethods.withoutAll(elements);
    }

    @Override
    public boolean addAllIterable(Iterable<? extends T> iterable)
    {
        if (iterable instanceof UnifiedSet)
        {
            return this.copySet((UnifiedSet<?>) iterable);
        }

        int size = Iterate.sizeOf(iterable);
        this.ensureCapacity(size);
        int oldSize = this.size();

        if (iterable instanceof List && iterable instanceof RandomAccess)
        {
            List<T> list = (List<T>) iterable;
            for (int i = 0; i < size; i++)
            {
                this.add(list.get(i));
            }
        }
        else
        {
            Iterate.forEachWith(iterable, Procedures2.addToCollection(), this);
        }
        return this.size() != oldSize;
    }

    private void ensureCapacity(int size)
    {
        if (size > this.maxSize)
        {
            size = (int) (size / this.loadFactor) + 1;
            int capacity = Integer.highestOneBit(size);
            if (size != capacity)
            {
                capacity <<= 1;
            }
            this.rehash(capacity);
        }
    }

    protected boolean copySet(UnifiedSet<?> unifiedset)
    {
        return this.converterMethods.copySet(unifiedset);
    }


    @Override
    public boolean remove(Object key)
    {
        return this.modifierMethods.remove(key);
    }


    @Override
    public int size()
    {
        return this.occupied;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object)
        {
            return true;
        }

        if (!(object instanceof Set))
        {
            return false;
        }

        Set<?> other = (Set<?>) object;
        return this.size() == other.size() && this.containsAll(other);
    }

    @Override
    public int hashCode()
    {
        return this.miscellaneousMethods.hashCode(this.table);
    }


    public boolean trimToSize()
    {
        if (this.table.length <= (this.fastCeil(this.occupied / this.loadFactor) << 1))
        {
            return false;
        }

        Object[] temp = this.table;
        this.init(this.fastCeil(this.occupied / this.loadFactor));
        if (this.isEmpty())
        {
            return true;
        }

        int mask = this.table.length - 1;
        for (int j = 0; j < temp.length; j++)
        {
            Object cur = temp[j];
            if (cur instanceof ChainedBucket)
            {
                ChainedBucket bucket = (ChainedBucket) cur;
                this.chainedTrimToSize(bucket, j, mask);
            }
            else if (cur != null)
            {
                this.addForTrim(cur, j, mask);
            }
        }
        return true;
    }

    private void chainedTrimToSize(ChainedBucket bucket, int oldIndex, int mask)
    {
        do
        {
            this.addForTrim(bucket.zero, oldIndex, mask);
            if (bucket.one == null)
            {
                return;
            }
            this.addForTrim(bucket.one, oldIndex, mask);
            if (bucket.two == null)
            {
                return;
            }
            this.addForTrim(bucket.two, oldIndex, mask);
            if (bucket.three == null)
            {
                return;
            }
            if (bucket.three instanceof ChainedBucket)
            {
                bucket = (ChainedBucket) bucket.three;
                continue;
            }
            this.addForTrim(bucket.three, oldIndex, mask);
            return;
        }
        while (true);
        //return this.modifierMethods.chainedTrimToSize(bucket, oldIndex, mask);
    }

    private void addForTrim(Object key, int oldIndex, int mask)
    {
        int index = oldIndex & mask;
        Object cur = this.table[index];
        if (cur == null)
        {
            this.table[index] = key;
            return;
        }
        this.chainedAddForTrim(key, index);
    }

    private void chainedAddForTrim(Object key, int index)
    {
        if (this.table[index] instanceof ChainedBucket)
        {
            ChainedBucket bucket = (ChainedBucket) this.table[index];
            do
            {
                if (bucket.one == null)
                {
                    bucket.one = key;
                    return;
                }
                if (bucket.two == null)
                {
                    bucket.two = key;
                    return;
                }
                if (bucket.three instanceof ChainedBucket)
                {
                    bucket = (ChainedBucket) bucket.three;
                    continue;
                }
                if (bucket.three == null)
                {
                    bucket.three = key;
                    return;
                }
                bucket.three = new ChainedBucket(bucket.three, key);
                return;
            }
            while (true);
        }
        ChainedBucket newBucket = new ChainedBucket(this.table[index], key);
        this.table[index] = newBucket;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        this.loadFactor = in.readFloat();
        this.init(Math.max((int) (size / this.loadFactor) + 1, DEFAULT_INITIAL_CAPACITY));
        for (int i = 0; i < size; i++)
        {
            this.add((T) in.readObject());
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        this.miscellaneousMethods.writeExternal(out);
    }


    private void addIfFound(T key, UnifiedSet<T> other)
    {
        int index = this.index(key);

        Object cur = this.table[index];
        if (cur == null)
        {
            return;
        }
        if (cur instanceof ChainedBucket)
        {
            this.addIfFoundFromChain((ChainedBucket) cur, key, other);
            return;
        }
        if (this.nonNullTableObjectEquals(cur, key))
        {
            other.add(this.nonSentinel(cur));
        }
    }

    private void addIfFoundFromChain(ChainedBucket bucket, T key, UnifiedSet<T> other)
    {
        do
        {
            if (this.nonNullTableObjectEquals(bucket.zero, key))
            {
                other.add(this.nonSentinel(bucket.zero));
                return;
            }
            if (bucket.one == null)
            {
                return;
            }
            if (this.nonNullTableObjectEquals(bucket.one, key))
            {
                other.add(this.nonSentinel(bucket.one));
                return;
            }
            if (bucket.two == null)
            {
                return;
            }
            if (this.nonNullTableObjectEquals(bucket.two, key))
            {
                other.add(this.nonSentinel(bucket.two));
                return;
            }
            if (bucket.three == null)
            {
                return;
            }
            if (bucket.three instanceof ChainedBucket)
            {
                bucket = (ChainedBucket) bucket.three;
                continue;
            }
            if (this.nonNullTableObjectEquals(bucket.three, key))
            {
                other.add(this.nonSentinel(bucket.three));
                return;
            }
            return;
        }
        while (true);
    }

    @Override
    public boolean retainAllIterable(Iterable<?> iterable)
    {
        if (iterable instanceof Set)
        {
            return this.retainAllFromSet((Set<?>) iterable);
        }
        return this.retainAllFromNonSet(iterable);
    }

    private boolean retainAllFromNonSet(Iterable<?> iterable)
    {
        int retainedSize = Iterate.sizeOf(iterable);
        UnifiedSet<T> retainedCopy = this.newEmpty(retainedSize);
        for (Object key : iterable)
        {
            this.addIfFound((T) key, retainedCopy);
        }
        if (retainedCopy.size() < this.size())
        {
            this.maxSize = retainedCopy.maxSize;
            this.occupied = retainedCopy.occupied;
            this.table = retainedCopy.table;
            return true;
        }
        return false;
    }

    private boolean retainAllFromSet(Set<?> collection)
    {
        // TODO: turn iterator into a loop
        boolean result = false;
        Iterator<T> e = this.iterator();
        while (e.hasNext())
        {
            if (!collection.contains(e.next()))
            {
                e.remove();
                result = true;
            }
        }
        return result;
    }

    @Override
    public UnifiedSet<T> clone()
    {
        return new UnifiedSet<>(this);
    }

    @Override
    public Object[] toArray()
    {
        return this.converterMethods.toArray();
    }


    @Override
    public <T> T[] toArray(T[] array)
    {
        return this.converterMethods.toArray(array);
    }

    @Override
    public Iterator<T> iterator()
    {
        return new PositionalIterator();
    }

    protected class PositionalIterator implements Iterator<T>
    {
        protected int count;
        protected int position;
        protected int chainPosition;
        protected boolean lastReturned;

        @Override
        public boolean hasNext()
        {
            return this.count < UnifiedSet.this.size();
        }

        @Override
        public void remove()
        {
            if (!this.lastReturned)
            {
                throw new IllegalStateException("next() must be called as many times as remove()");
            }
            this.count--;
            UnifiedSet.this.occupied--;

            if (this.chainPosition != 0)
            {
                this.removeFromChain();
                return;
            }

            int pos = this.position - 1;
            Object key = UnifiedSet.this.table[pos];
            if (key instanceof ChainedBucket)
            {
                this.removeLastFromChain((ChainedBucket) key, pos);
                return;
            }
            UnifiedSet.this.table[pos] = null;
            this.position = pos;
            this.lastReturned = false;
        }

        protected void removeFromChain()
        {
            ChainedBucket chain = (ChainedBucket) UnifiedSet.this.table[this.position];
            chain.remove(--this.chainPosition);
            this.lastReturned = false;
        }

        protected void removeLastFromChain(ChainedBucket bucket, int tableIndex)
        {
            bucket.removeLast(0);
            if (bucket.zero == null)
            {
                UnifiedSet.this.table[tableIndex] = null;
            }
            this.lastReturned = false;
        }

        protected T nextFromChain()
        {
            ChainedBucket bucket = (ChainedBucket) UnifiedSet.this.table[this.position];
            Object cur = bucket.get(this.chainPosition);
            this.chainPosition++;
            if (bucket.get(this.chainPosition) == null)
            {
                this.chainPosition = 0;
                this.position++;
            }
            this.lastReturned = true;
            return UnifiedSet.this.nonSentinel(cur);
        }

        @Override
        public T next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;
            Object[] table = UnifiedSet.this.table;
            if (this.chainPosition != 0)
            {
                return this.nextFromChain();
            }
            while (table[this.position] == null)
            {
                this.position++;
            }
            Object cur = table[this.position];
            if (cur instanceof ChainedBucket)
            {
                return this.nextFromChain();
            }
            this.position++;
            this.lastReturned = true;
            return UnifiedSet.this.nonSentinel(cur);
        }
    }

    public static final class ChainedBucket
    {
        public Object zero;
        public Object one;
        public Object two;
        public Object three;

        public ChainedBucket()
        {
        }

        public ChainedBucket(Object first, Object second)
        {
            this.zero = first;
            this.one = second;
        }

        public void remove(int i)
        {
            if (i > 3)
            {
                this.removeLongChain(this, i - 3);
            }
            else
            {
                switch (i)
                {
                    case 0:
                        this.zero = this.removeLast(0);
                        return;
                    case 1:
                        this.one = this.removeLast(1);
                        return;
                    case 2:
                        this.two = this.removeLast(2);
                        return;
                    case 3:
                        if (this.three instanceof ChainedBucket)
                        {
                            this.removeLongChain(this, i - 3);
                            return;
                        }
                        this.three = null;
                        return;
                    default:
                        throw new AssertionError();
                }
            }
        }

        private void removeLongChain(ChainedBucket oldBucket, int i)
        {
            do
            {
                ChainedBucket bucket = (ChainedBucket) oldBucket.three;
                switch (i)
                {
                    case 0:
                        bucket.zero = bucket.removeLast(0);
                        return;
                    case 1:
                        bucket.one = bucket.removeLast(1);
                        return;
                    case 2:
                        bucket.two = bucket.removeLast(2);
                        return;
                    case 3:
                        if (bucket.three instanceof ChainedBucket)
                        {
                            i -= 3;
                            oldBucket = bucket;
                            continue;
                        }
                        bucket.three = null;
                        return;
                    default:
                        if (bucket.three instanceof ChainedBucket)
                        {
                            i -= 3;
                            oldBucket = bucket;
                            continue;
                        }
                        throw new AssertionError();
                }
            }
            while (true);
        }

        public Object get(int i)
        {
            ChainedBucket bucket = this;
            while (i > 3 && bucket.three instanceof ChainedBucket)
            {
                bucket = (ChainedBucket) bucket.three;
                i -= 3;
            }
            do
            {
                switch (i)
                {
                    case 0:
                        return bucket.zero;
                    case 1:
                        return bucket.one;
                    case 2:
                        return bucket.two;
                    case 3:
                        if (bucket.three instanceof ChainedBucket)
                        {
                            i -= 3;
                            bucket = (ChainedBucket) bucket.three;
                            continue;
                        }
                        return bucket.three;
                    case 4:
                        return null; // this happens when a bucket is exactly full, and we're iterating
                    default:
                        throw new AssertionError();
                }
            }
            while (true);
        }

        public Object removeLast(int cur)
        {
            if (this.three instanceof ChainedBucket)
            {
                return this.removeLast(this);
            }
            if (this.three != null)
            {
                Object result = this.three;
                this.three = null;
                return cur == 3 ? null : result;
            }
            if (this.two != null)
            {
                Object result = this.two;
                this.two = null;
                return cur == 2 ? null : result;
            }
            if (this.one != null)
            {
                Object result = this.one;
                this.one = null;
                return cur == 1 ? null : result;
            }
            this.zero = null;
            return null;
        }

        private Object removeLast(ChainedBucket oldBucket)
        {
            do
            {
                ChainedBucket bucket = (ChainedBucket) oldBucket.three;
                if (bucket.three instanceof ChainedBucket)
                {
                    oldBucket = bucket;
                    continue;
                }
                if (bucket.three != null)
                {
                    Object result = bucket.three;
                    bucket.three = null;
                    return result;
                }
                if (bucket.two != null)
                {
                    Object result = bucket.two;
                    bucket.two = null;
                    return result;
                }
                if (bucket.one != null)
                {
                    Object result = bucket.one;
                    bucket.one = null;
                    return result;
                }
                Object result = bucket.zero;
                oldBucket.three = null;
                return result;
            }
            while (true);
        }

        public ChainedBucket copy()
        {
            ChainedBucket result = new ChainedBucket();
            ChainedBucket dest = result;
            ChainedBucket src = this;
            do
            {
                dest.zero = src.zero;
                dest.one = src.one;
                dest.two = src.two;
                if (src.three instanceof ChainedBucket)
                {
                    dest.three = new ChainedBucket();
                    src = (ChainedBucket) src.three;
                    dest = (ChainedBucket) dest.three;
                    continue;
                }
                dest.three = src.three;
                return result;
            }
            while (true);
        }
    }

    @Override
    public <V> UnifiedSetMultimap<V, T> groupBy(
            Function<? super T, ? extends V> function)
    {
        return this.groupBy(function, UnifiedSetMultimap.newMultimap());
    }

    @Override
    public <V> UnifiedSetMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        return this.groupByEach(function, new UnifiedSetMultimap<>());
    }

    @Override
    public T get(T key)
    {
        return this.miscellaneousMethods.get(key);
    }


    @Override
    public T put(T key)
    {
        int index = this.index(key);
        Object cur = this.table[index];

        if (cur == null)
        {
            this.table[index] = UnifiedSet.toSentinelIfNull(key);
            if (++this.occupied > this.maxSize)
            {
                this.rehash();
            }
            return key;
        }

        if (cur instanceof ChainedBucket || !this.nonNullTableObjectEquals(cur, key))
        {
            return this.chainedPut(key, index);
        }
        return this.nonSentinel(cur);
    }

    private T chainedPut(T key, int index)
    {
        Object realKey = UnifiedSet.toSentinelIfNull(key);
        if (this.table[index] instanceof ChainedBucket)
        {
            ChainedBucket bucket = (ChainedBucket) this.table[index];
            do
            {
                if (this.nonNullTableObjectEquals(bucket.zero, key))
                {
                    return this.nonSentinel(bucket.zero);
                }
                if (bucket.one == null)
                {
                    bucket.one = realKey;
                    if (++this.occupied > this.maxSize)
                    {
                        this.rehash();
                    }
                    return key;
                }
                if (this.nonNullTableObjectEquals(bucket.one, key))
                {
                    return this.nonSentinel(bucket.one);
                }
                if (bucket.two == null)
                {
                    bucket.two = realKey;
                    if (++this.occupied > this.maxSize)
                    {
                        this.rehash();
                    }
                    return key;
                }
                if (this.nonNullTableObjectEquals(bucket.two, key))
                {
                    return this.nonSentinel(bucket.two);
                }
                if (bucket.three instanceof ChainedBucket)
                {
                    bucket = (ChainedBucket) bucket.three;
                    continue;
                }
                if (bucket.three == null)
                {
                    bucket.three = realKey;
                    if (++this.occupied > this.maxSize)
                    {
                        this.rehash();
                    }
                    return key;
                }
                if (this.nonNullTableObjectEquals(bucket.three, key))
                {
                    return this.nonSentinel(bucket.three);
                }
                bucket.three = new ChainedBucket(bucket.three, realKey);
                if (++this.occupied > this.maxSize)
                {
                    this.rehash();
                }
                return key;
            }
            while (true);
        }
        ChainedBucket newBucket = new ChainedBucket(this.table[index], realKey);
        this.table[index] = newBucket;
        if (++this.occupied > this.maxSize)
        {
            this.rehash();
        }
        return key;
    }

    @Override
    public T removeFromPool(T key)
    {
        return this.modifierMethods.removeFromPool(key);
    }


    private T nonSentinel(Object key)
    {
        return key == NULL_KEY ? null : (T) key;
    }

    private static Object toSentinelIfNull(Object key)
    {
        if (key == null)
        {
            return NULL_KEY;
        }
        return key;
    }

    private boolean nonNullTableObjectEquals(Object cur, T key)
    {
        return cur == key || (cur == NULL_KEY ? key == null : cur.equals(key));
    }

    @Override
    @Beta
    public ParallelUnsortedSetIterable<T> asParallel(ExecutorService executorService, int batchSize)
    {
        return this.converterMethods.asParallel(executorService, batchSize);
    }
}