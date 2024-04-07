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
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.set.primitive.ByteSet;

public class ByteHashSetModifiers
{
    private ByteHashSet bhset;
    private static final long serialVersionUID = 1L;
    private static final byte MAX_BYTE_GROUP_1 = -65;
    private static final byte MAX_BYTE_GROUP_2 = -1;
    private static final byte MAX_BYTE_GROUP_3 = 63;
    private long bitGroup2;

    public ByteHashSetModifiers(ByteHashSet set) {
        this.bhset = set;
    }

    public boolean add(byte element)
    {
        if (element <= MAX_BYTE_GROUP_1)
        {
            long initial = bhset.getBitGroup1();

             this.bhset.setBitGroup1(bhset.getBitGroup1() | (1L << (byte) ((element + 1) * -1)));

            if (this.bhset.getBitGroup1() != initial)
            {
                this.bhset.setSize( (short) (bhset.size()+1) );
                return true;
            }
        }
        else if (element <= MAX_BYTE_GROUP_2)
        {
            long initial = this.bhset.getBitGroup2();

            this.bhset.setBitGroup2(bhset.getBitGroup2() | (1L << (byte) ((element + 1) * -1)));

            if (this.bhset.getBitGroup2() != initial)
            {
                this.bhset.setSize( (short) (bhset.size()+1) );
                return true;
            }
        }
        else if (element <= MAX_BYTE_GROUP_3)
        {
            long initial = this.bhset.getBitGroup3();

            this.bhset.setBitGroup3(bhset.getBitGroup3() | (1L << element));

            if (this.bhset.getBitGroup3() != initial)
            {
                this.bhset.setSize( (short) (bhset.size()+1) );
                return true;
            }
        }
        else
        {
            long initial = this.bhset.getBitGroup4();

            this.bhset.setBitGroup4(bhset.getBitGroup4() | (1L << element));

            if (this.bhset.getBitGroup4() != initial)
            {
                this.bhset.setSize( (short) (bhset.size()+1) );
                return true;
            }
        }

        return false;
    }


    public boolean remove(byte value)
    {
        if (value <= MAX_BYTE_GROUP_1)
        {
            long initial = this.bhset.getBitGroup1();
            this.bhset.setBitGroup1(bhset.getBitGroup1() & (~(1L << (byte) ((value + 1) * -1))));
            if (this.bhset.getBitGroup1() == initial)
            {
                return false;
            }
            this.bhset.setSize((short) (bhset.size()-1));
            return true;
        }
        if (value <= MAX_BYTE_GROUP_2)
        {
            long initial = this.bhset.getBitGroup2();
            this.bhset.setBitGroup2(bhset.getBitGroup2() & (~(1L << (byte) ((value + 1) * -1))));

            if (this.bitGroup2 == initial)
            {
                return false;
            }
            this.bhset.setSize((short) (bhset.size()-1));
            return true;
        }
        if (value <= MAX_BYTE_GROUP_3)
        {
            long initial = this.bhset.getBitGroup3();
            this.bhset.setBitGroup3(bhset.getBitGroup3()& (~(1L << value)));
            if (this.bhset.getBitGroup3() == initial)
            {
                return false;
            }
            this.bhset.setSize((short) (bhset.size()-1));
            return true;
        }

        long initial = this.bhset.getBitGroup4();
        this.bhset.setBitGroup4(bhset.getBitGroup4()& (~(1L << value)));
        if (this.bhset.getBitGroup4() == initial)
        {
            return false;
        }
        this.bhset.setSize((short) (bhset.size()-1));;
        return true;
    }

    public boolean addAll(byte... source)
    {
        int oldSize = this.bhset.size();
        for (byte item : source)
        {
            this.add(item);
        }
        return this.bhset.size() != oldSize;
    }

    public boolean addAll(ByteIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.bhset.size();

        if (source instanceof ByteHashSet)
        {
            ByteHashSet hashSet = (ByteHashSet) source;

            this.bhset.setSize((short) 0);
            this.bhset.setBitGroup3(bhset.getBitGroup3() | (hashSet.getBitGroup3()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup3()))) ;

            this.bhset.setBitGroup4(bhset.getBitGroup4() | (hashSet.getBitGroup4()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup4()))) ;

            this.bhset.setBitGroup2(bhset.getBitGroup2() | (hashSet.getBitGroup2()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup2()))) ;

            this.bhset.setBitGroup1(bhset.getBitGroup1() | (hashSet.getBitGroup1()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup1()))) ;
        }
        else
        {
            ByteIterator iterator = source.byteIterator();
            while (iterator.hasNext())
            {
                byte item = iterator.next();
                this.add(item);
            }
        }
        return this.bhset.size() != oldSize;
    }

    public boolean removeAll(ByteIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.bhset.size();
        if (source instanceof ByteHashSet)
        {
            this.bhset.setSize((short) 0);
            ByteHashSet hashSet = (ByteHashSet) source;
            this.bhset.setBitGroup3( bhset.getBitGroup3() & (~hashSet.getBitGroup3()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup3()))) ;

            this.bhset.setBitGroup4( bhset.getBitGroup4() & (~hashSet.getBitGroup4()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup4()))) ;

            this.bhset.setBitGroup2( bhset.getBitGroup2() & (~hashSet.getBitGroup2()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup2()))) ;

            this.bhset.setBitGroup1( bhset.getBitGroup1() & (~hashSet.getBitGroup1()));
            this.bhset.setSize((short) (bhset.size() + Long.bitCount(this.bhset.getBitGroup1()))) ;
        }
        else
        {
            ByteIterator iterator = source.byteIterator();
            while (iterator.hasNext())
            {
                byte item = iterator.next();
                this.remove(item);
            }
        }
        return this.bhset.size() != oldSize;
    }


    public boolean removeAll(byte... source)
    {
    if (source.length == 0)
    {
        return false;
    }
    int oldSize = this.bhset.size();
    for (byte item : source)
    {
        this.remove(item);
    }
    return this.bhset.size() != oldSize;
    }

    public boolean retainAll(ByteIterable source)
    {
        int oldSize = this.bhset.size();
        ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();

        ByteHashSet retained = this.bhset.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.bhset.setBitGroup3(retained.getBitGroup3());
            this.bhset.setBitGroup4(retained.getBitGroup4());
            this.bhset.setBitGroup1(retained.getBitGroup1());
            this.bhset.setBitGroup2(retained.getBitGroup2());
            this.bhset.setSize((short) retained.size());
            return true;
        }

        return false;
    }

    public void clear()
    {
        this.bhset.setSize((short) 0);
        this.bhset.setBitGroup3(0L);
        this.bhset.setBitGroup4(0L);
        this.bhset.setBitGroup1(0L);
        this.bhset.setBitGroup2(0L);
    }

}
