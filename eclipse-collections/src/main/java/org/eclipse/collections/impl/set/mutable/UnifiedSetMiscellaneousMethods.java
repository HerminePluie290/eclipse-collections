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
import java.io.ObjectOutput;

public class UnifiedSetMiscellaneousMethods<T>
{
    private UnifiedSet<T> unifiedSet;

    public UnifiedSetMiscellaneousMethods(UnifiedSet<T> set){
        this.unifiedSet=set;
    }


    protected int index(Object key)
    {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        int h = key == null ? 0 : key.hashCode();
        h ^= h >>> 20 ^ h >>> 12;
        h ^= h >>> 7 ^ h >>> 4;
        return h & this.unifiedSet.table.length - 1;
    }


    public T getFirst()
    {
        for (int i = 0; i < this.unifiedSet.table.length; i++)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                return this.nonSentinel(((UnifiedSet.ChainedBucket) cur).zero);
            }
            if (cur != null)
            {
                return this.nonSentinel(cur);
            }
        }
        return null;
    }


    public T getLast()
    {
        for (int i = this.unifiedSet.table.length - 1; i >= 0; i--)
        {
            Object cur = this.unifiedSet.table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                return this.getLast((UnifiedSet.ChainedBucket) cur);
            }
            if (cur != null)
            {
                return this.nonSentinel(cur);
            }
        }
        return null;
    }

    private T getLast(UnifiedSet.ChainedBucket bucket)
    {
        while (bucket.three instanceof UnifiedSet.ChainedBucket)
        {
            bucket = (UnifiedSet.ChainedBucket) bucket.three;
        }

        if (bucket.three != null)
        {
            return this.nonSentinel(bucket.three);
        }
        if (bucket.two != null)
        {
            return this.nonSentinel(bucket.two);
        }
        if (bucket.one != null)
        {
            return this.nonSentinel(bucket.one);
        }
        assert bucket.zero != null;
        return this.nonSentinel(bucket.zero);
    }


    public int hashCode(Object[] table)
    {
        int hashCode = 0;
        for (int i = 0; i < table.length; i++)
        {
            Object cur = table[i];
            if (cur instanceof UnifiedSet.ChainedBucket)
            {
                hashCode += this.chainedHashCode((UnifiedSet.ChainedBucket) cur);
            }
            else if (cur != null)
            {
                hashCode += cur == UnifiedSet.NULL_KEY ? 0 : cur.hashCode();
            }
        }
        return hashCode;
    }

    private int chainedHashCode(UnifiedSet.ChainedBucket bucket)
    {
        int hashCode = 0;
        do
        {
            hashCode += bucket.zero == UnifiedSet.NULL_KEY ? 0 : bucket.zero.hashCode();
            if (bucket.one == null)
            {
                return hashCode;
            }
            hashCode += bucket.one == UnifiedSet.NULL_KEY ? 0 : bucket.one.hashCode();
            if (bucket.two == null)
            {
                return hashCode;
            }
            hashCode += bucket.two == UnifiedSet.NULL_KEY ? 0 : bucket.two.hashCode();
            if (bucket.three == null)
            {
                return hashCode;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            hashCode += bucket.three == UnifiedSet.NULL_KEY ? 0 : bucket.three.hashCode();
            return hashCode;
        }
        while (true);
    }


    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.unifiedSet.size());
        out.writeFloat(this.unifiedSet.getLoadFactor());
        for (int i = 0; i < this.unifiedSet.table.length; i++)
        {
            Object o = this.unifiedSet.table[i];
            if (o != null)
            {
                if (o instanceof UnifiedSet.ChainedBucket)
                {
                    this.writeExternalChain(out, (UnifiedSet.ChainedBucket) o);
                }
                else
                {
                    out.writeObject(this.nonSentinel(o));
                }
            }
        }
    }

    private void writeExternalChain(ObjectOutput out, UnifiedSet.ChainedBucket bucket) throws IOException
    {
        do
        {
            out.writeObject(this.nonSentinel(bucket.zero));
            if (bucket.one == null)
            {
                return;
            }
            out.writeObject(this.nonSentinel(bucket.one));
            if (bucket.two == null)
            {
                return;
            }
            out.writeObject(this.nonSentinel(bucket.two));
            if (bucket.three == null)
            {
                return;
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            out.writeObject(this.nonSentinel(bucket.three));
            return;
        }
        while (true);
    }


    public T get(T key)
    {
        int index = this.index(key);
        Object cur = this.unifiedSet.table[index];

        if (cur == null)
        {
            return null;
        }
        if (cur instanceof UnifiedSet.ChainedBucket)
        {
            return this.chainedGet(key, (UnifiedSet.ChainedBucket) cur);
        }
        if (this.nonNullTableObjectEquals(cur, key))
        {
            return (T) cur;
        }
        return null;
    }

    private T chainedGet(T key, UnifiedSet.ChainedBucket bucket)
    {
        do
        {
            if (this.nonNullTableObjectEquals(bucket.zero, key))
            {
                return this.nonSentinel(bucket.zero);
            }
            if (bucket.one == null)
            {
                return null;
            }
            if (this.nonNullTableObjectEquals(bucket.one, key))
            {
                return this.nonSentinel(bucket.one);
            }
            if (bucket.two == null)
            {
                return null;
            }
            if (this.nonNullTableObjectEquals(bucket.two, key))
            {
                return this.nonSentinel(bucket.two);
            }
            if (bucket.three instanceof UnifiedSet.ChainedBucket)
            {
                bucket = (UnifiedSet.ChainedBucket) bucket.three;
                continue;
            }
            if (bucket.three == null)
            {
                return null;
            }
            if (this.nonNullTableObjectEquals(bucket.three, key))
            {
                return this.nonSentinel(bucket.three);
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
