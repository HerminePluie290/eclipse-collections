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

import java.io.IOException;

import org.eclipse.collections.api.iterator.ByteIterator;

public class ByteHashStringMethods
{

    private ByteHashSet bhset;

    public ByteHashStringMethods(ByteHashSet set){this.bhset = set;}

    public String toString()
    {
        return this.makeString("[", ", ", "]");
    }

    public String makeString()
    {
        return this.makeString(", ");
    }

    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            int count = 0;
            ByteIterator iterator = this.bhset.byteIterator();

            while (iterator.hasNext())
            {
                if (count > 0)
                {
                    appendable.append(separator);
                }

                count++;
                appendable.append(String.valueOf(iterator.next()));
            }

            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
