
/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import sun.misc.FloatingDecimal;
import java.util.Arrays;

/**
 * 一个可变的字符序列。
 * <p>
 * 实现一个可修改的字符串。在任何时候，它包含某些特定的字符序列，但该序列的长度和内容可以通过某些方法调用进行更改。
 *
 * <p>除非另有说明，向此类的构造函数或方法传递 {@code null} 参数将导致抛出 {@link NullPointerException}。
 *
 * @author      Michael McCloskey
 * @author      Martin Buchholz
 * @author      Ulf Zibis
 * @since       1.5
 */
abstract class AbstractStringBuilder implements Appendable, CharSequence {
    /**
     * 用于字符存储的值。
     */
    char[] value;

    /**
     * 已使用的字符数。
     */
    int count;

    /**
     * 无参构造函数是子类序列化所必需的。
     */
    AbstractStringBuilder() {
    }

    /**
     * 创建指定容量的 AbstractStringBuilder。
     */
    AbstractStringBuilder(int capacity) {
        value = new char[capacity];
    }

    /**
     * 返回长度（字符数）。
     *
     * @return  当前由该对象表示的字符序列的长度
     */
    @Override
    public int length() {
        return count;
    }

    /**
     * 返回当前容量。容量是可用于新插入字符的存储量，超过此容量将进行分配。
     *
     * @return  当前容量
     */
    public int capacity() {
        return value.length;
    }

    /**
     * 确保容量至少等于指定的最小值。
     * 如果当前容量小于参数值，则分配一个具有更大容量的新内部数组。新容量是以下两者中的较大值：
     * <ul>
     * <li>{@code minimumCapacity} 参数。
     * <li>旧容量的两倍加 {@code 2}。
     * </ul>
     * 如果 {@code minimumCapacity} 参数是非正数，此方法不采取任何操作并直接返回。
     * 请注意，后续对此对象的操作可能会使实际容量低于此处请求的容量。
     *
     * @param   minimumCapacity   所需的最小容量。
     */
    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > 0)
            ensureCapacityInternal(minimumCapacity);
    }

    /**
     * 对于正值的 {@code minimumCapacity}，此方法的行为类似于 {@code ensureCapacity}，但从未同步。
     * 如果 {@code minimumCapacity} 由于数值溢出而非正数，此方法将抛出 {@code OutOfMemoryError}。
     */
    private void ensureCapacityInternal(int minimumCapacity) {
        // 考虑溢出的代码
        if (minimumCapacity - value.length > 0) {
            value = Arrays.copyOf(value,
                    newCapacity(minimumCapacity));
        }
    }

    /**
     * 除非必要，否则分配的最大数组大小。
     * 某些虚拟机在数组中保留一些头字节。
     * 尝试分配更大的数组可能会导致
     * OutOfMemoryError: 请求的数组大小超过 VM 限制
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 返回至少与给定最小容量一样大的容量。
     * 如果当前容量增加相同数量加 2 后足够，则返回当前容量。
     * 不会返回大于 {@code MAX_ARRAY_SIZE} 的容量，除非给定的最小容量大于该值。
     *
     * @param  minCapacity 所需的最小容量
     * @throws OutOfMemoryError 如果 minCapacity 小于零或大于 Integer.MAX_VALUE
     */
    private int newCapacity(int minCapacity) {
        // 考虑溢出的代码
        int newCapacity = (value.length << 1) + 2;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
            ? hugeCapacity(minCapacity)
            : newCapacity;
    }

    private int hugeCapacity(int minCapacity) {
        if (Integer.MAX_VALUE - minCapacity < 0) { // 溢出
            throw new OutOfMemoryError();
        }
        return (minCapacity > MAX_ARRAY_SIZE)
            ? minCapacity : MAX_ARRAY_SIZE;
    }

    /**
     * 尝试减少用于字符序列的存储。
     * 如果缓冲区大于当前字符序列所需的大小，则可能会调整大小以提高空间效率。
     * 调用此方法可能会影响后续调用 {@link #capacity()} 方法的返回值。
     */
    public void trimToSize() {
        if (count < value.length) {
            value = Arrays.copyOf(value, count);
        }
    }

    /**
     * 设置字符序列的长度。
     * 序列更改为由参数指定的新字符序列。对于所有非负索引 <i>k</i> 小于 {@code newLength}，新字符序列中的索引 <i>k</i> 处的字符与旧序列中的索引 <i>k</i> 处的字符相同（如果 <i>k</i> 小于旧序列的长度）；否则，它是空字符 {@code '\u005Cu0000'}。
     *
     * 换句话说，如果 {@code newLength} 参数小于当前长度，则长度更改为指定的长度。
     * <p>
     * 如果 {@code newLength} 参数大于或等于当前长度，则追加足够的空字符
     * ({@code '\u005Cu0000'}) 使长度变为 {@code newLength} 参数。
     * <p>
     * {@code newLength} 参数必须大于或等于 {@code 0}。
     *
     * @param      newLength   新长度
     * @throws     IndexOutOfBoundsException  如果 {@code newLength} 参数为负。
     */
    public void setLength(int newLength) {
        if (newLength < 0)
            throw new StringIndexOutOfBoundsException(newLength);
        ensureCapacityInternal(newLength);

        if (count < newLength) {
            Arrays.fill(value, count, newLength, '\0');
        }

        count = newLength;
    }

    /**
     * 返回此序列中指定索引处的 {@code char} 值。
     * 第一个 {@code char} 值的索引为 {@code 0}，下一个为 {@code 1}，依此类推，就像数组索引一样。
     * <p>
     * 索引参数必须大于或等于 {@code 0}，并且小于此序列的长度。
     *
     * <p>如果指定索引处的 {@code char} 值是
     * <a href="Character.html#unicode">代理</a>，则返回代理值。
     *
     * @param      index   所需 {@code char} 值的索引。
     * @return     指定索引处的 {@code char} 值。
     * @throws     IndexOutOfBoundsException  如果 {@code index} 为负或大于或等于 {@code length()}。
     */
    @Override
    public char charAt(int index) {
        if ((index < 0) || (index >= count))
            throw new StringIndexOutOfBoundsException(index);
        return value[index];
    }

    /**
     * 返回指定索引处的字符（Unicode 代码点）。索引指的是 {@code char} 值
     * （Unicode 代码单元），范围从 {@code 0} 到
     * {@link #length()}{@code  - 1}。
     *
     * <p> 如果指定索引处的 {@code char} 值在高代理范围内，后续索引小于此序列的长度，并且
     * 后续索引处的 {@code char} 值在低代理范围内，则返回对应的补充代码点。
     * 否则，返回指定索引处的 {@code char} 值。
     *
     * @param      index 指定的 {@code char} 值的索引
     * @return     指定索引处的字符的代码点值
     * @exception  IndexOutOfBoundsException  如果 {@code index}
     *             参数为负或不小于此序列的长度。
     */
    public int codePointAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointAtImpl(value, index, count);
    }

    /**
     * 返回指定索引前的字符（Unicode 代码点）。索引指的是 {@code char} 值
     * （Unicode 代码单元），范围从 {@code 1} 到 {@link
     * #length()}。
     *
     * <p> 如果 {@code (index - 1)} 处的 {@code char} 值在低代理范围内，{@code (index - 2)} 不为负，并且
     * {@code (index - 2)} 处的 {@code char} 值在高代理范围内，则返回该代理对的补充代码点值。
     * 如果 {@code index - 1} 处的 {@code char} 值是未配对的低代理或高代理，则返回代理值。
     *
     * @param     index 应返回的代码点的索引
     * @return    指定索引前的 Unicode 代码点值。
     * @exception IndexOutOfBoundsException 如果 {@code index}
     *            参数小于 1 或大于此序列的长度。
     */
    public int codePointBefore(int index) {
        int i = index - 1;
        if ((i < 0) || (i >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointBeforeImpl(value, index, 0);
    }

    /**
     * 返回此序列中指定文本范围内的 Unicode 代码点数。文本范围从指定的
     * {@code beginIndex} 开始，扩展到 {@code char} 位于
     * 索引 {@code endIndex - 1} 处。因此，文本范围的长度（以
     * {@code char} 为单位）为
     * {@code endIndex-beginIndex}。此序列中的未配对代理计为一个代码点。
     *
     * @param beginIndex 第一个 {@code char} 的索引。
     * @param endIndex 最后一个 {@code char} 之后的索引。
     * @return 指定文本范围内的 Unicode 代码点数
     * @exception IndexOutOfBoundsException 如果
     * {@code beginIndex} 为负，或 {@code endIndex}
     * 大于此序列的长度，或
     * {@code beginIndex} 大于 {@code endIndex}。
     */
    public int codePointCount(int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > count || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }
        return Character.codePointCountImpl(value, beginIndex, endIndex-beginIndex);
    }

    /**
     * 返回从给定 {@code index} 偏移 {@code codePointOffset} 代码点的索引。文本范围由
     * {@code index} 和 {@code codePointOffset} 给出，其中未配对的代理计为一个代码点。
     *
     * @param index 要偏移的索引
     * @param codePointOffset 代码点偏移量
     * @return 此序列中的索引
     * @exception IndexOutOfBoundsException 如果 {@code index}
     *   为负或大于此序列的长度，
     *   或者 {@code codePointOffset} 为正且从 {@code index} 开始的子序列中的代码点少于
     *   {@code codePointOffset}，
     *   或者 {@code codePointOffset} 为负且 {@code index} 之前的子序列中的代码点少于
     *   {@code codePointOffset} 的绝对值。
     */
    public int offsetByCodePoints(int index, int codePointOffset) {
        if (index < 0 || index > count) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePointsImpl(value, 0, count,
                                                index, codePointOffset);
    }

    /**
     * 从此序列复制字符到目标字符数组 {@code dst}。第一个要复制的字符位于索引 {@code srcBegin}；最后一个要复制的字符位于索引 {@code srcEnd-1}。要复制的字符总数为 {@code srcEnd-srcBegin}。字符被复制到 {@code dst} 的子数组，从索引 {@code dstBegin} 开始，结束于索引：
     * <pre>{@code
     * dstbegin + (srcEnd-srcBegin) - 1
     * }</pre>
     *
     * @param      srcBegin   从这个偏移开始复制。
     * @param      srcEnd     在这个偏移处停止复制。
     * @param      dst        要复制数据到的目标数组。
     * @param      dstBegin   {@code dst} 的偏移。
     * @throws     IndexOutOfBoundsException  如果以下任何条件为真：
     *             <ul>
     *             <li>{@code srcBegin} 为负
     *             <li>{@code dstBegin} 为负
     *             <li>{@code srcBegin} 参数大于
     *             {@code srcEnd} 参数。
     *             <li>{@code srcEnd} 大于
     *             {@code this.length()}。
     *             <li>{@code dstBegin+srcEnd-srcBegin} 大于
     *             {@code dst.length}
     *             </ul>
     */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
    {
        if (srcBegin < 0)
            throw new StringIndexOutOfBoundsException(srcBegin);
        if ((srcEnd < 0) || (srcEnd > count))
            throw new StringIndexOutOfBoundsException(srcEnd);
        if (srcBegin > srcEnd)
            throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }


                /**
     * 将指定索引处的字符设置为 {@code ch}。此序列被修改以表示一个新字符序列，该序列与旧字符序列相同，只是在位置 {@code index} 处包含字符 {@code ch}。
     * <p>
     * 索引参数必须大于或等于 {@code 0}，并且小于此序列的长度。
     *
     * @param      index   要修改的字符的索引。
     * @param      ch      新字符。
     * @throws     IndexOutOfBoundsException  如果 {@code index} 为负数或大于或等于 {@code length()}。
     */
    public void setCharAt(int index, char ch) {
        if ((index < 0) || (index >= count))
            throw new StringIndexOutOfBoundsException(index);
        value[index] = ch;
    }

    /**
     * 追加 {@code Object} 参数的字符串表示形式。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(Object)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   obj   一个 {@code Object}。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    /**
     * 追加指定的字符串到此字符序列。
     * <p>
     * 参数 {@code String} 的字符按顺序追加，使此序列的长度增加参数的长度。如果 {@code str} 为 {@code null}，则追加四个字符 {@code "null"}。
     * <p>
     * 假设在执行 {@code append} 方法之前，此字符序列的长度为 <i>n</i>。那么在新的字符序列中，索引 <i>k</i> 处的字符等于旧字符序列中索引 <i>k</i> 处的字符，如果 <i>k</i> 小于 <i>n</i>；否则，它等于参数 {@code str} 中索引 <i>k-n</i> 处的字符。
     *
     * @param   str   一个字符串。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(String str) {
        if (str == null)
            return appendNull();
        int len = str.length();
        ensureCapacityInternal(count + len);
        str.getChars(0, len, value, count);
        count += len;
        return this;
    }

    // 子类中的文档，因为同步差异
    public AbstractStringBuilder append(StringBuffer sb) {
        if (sb == null)
            return appendNull();
        int len = sb.length();
        ensureCapacityInternal(count + len);
        sb.getChars(0, len, value, count);
        count += len;
        return this;
    }

    /**
     * @since 1.8
     */
    AbstractStringBuilder append(AbstractStringBuilder asb) {
        if (asb == null)
            return appendNull();
        int len = asb.length();
        ensureCapacityInternal(count + len);
        asb.getChars(0, len, value, count);
        count += len;
        return this;
    }

    // 子类中的文档，因为同步差异
    @Override
    public AbstractStringBuilder append(CharSequence s) {
        if (s == null)
            return appendNull();
        if (s instanceof String)
            return this.append((String)s);
        if (s instanceof AbstractStringBuilder)
            return this.append((AbstractStringBuilder)s);

        return this.append(s, 0, s.length());
    }

    private AbstractStringBuilder appendNull() {
        int c = count;
        ensureCapacityInternal(c + 4);
        final char[] value = this.value;
        value[c++] = 'n';
        value[c++] = 'u';
        value[c++] = 'l';
        value[c++] = 'l';
        count = c;
        return this;
    }

    /**
     * 追加指定 {@code CharSequence} 的子序列到此序列。
     * <p>
     * 参数 {@code s} 的字符，从索引 {@code start} 开始，按顺序追加到此序列的内容中，直到（不包括）索引 {@code end}。此序列的长度增加 {@code end - start} 的值。
     * <p>
     * 假设在执行 {@code append} 方法之前，此字符序列的长度为 <i>n</i>。那么在新的字符序列中，索引 <i>k</i> 处的字符等于旧字符序列中索引 <i>k</i> 处的字符，如果 <i>k</i> 小于 <i>n</i>；否则，它等于参数 {@code s} 中索引 <i>k+start-n</i> 处的字符。
     * <p>
     * 如果 {@code s} 为 {@code null}，则此方法追加字符，如同参数 s 是一个包含四个字符 {@code "null"} 的序列。
     *
     * @param   s the sequence to append.
     * @param   start   要追加的子序列的起始索引。
     * @param   end     要追加的子序列的结束索引。
     * @return  对此对象的引用。
     * @throws     IndexOutOfBoundsException 如果
     *             {@code start} 为负数，或
     *             {@code start} 大于 {@code end} 或
     *             {@code end} 大于 {@code s.length()}
     */
    @Override
    public AbstractStringBuilder append(CharSequence s, int start, int end) {
        if (s == null)
            s = "null";
        if ((start < 0) || (start > end) || (end > s.length()))
            throw new IndexOutOfBoundsException(
                "start " + start + ", end " + end + ", s.length() "
                + s.length());
        int len = end - start;
        ensureCapacityInternal(count + len);
        for (int i = start, j = count; i < end; i++, j++)
            value[j] = s.charAt(i);
        count += len;
        return this;
    }

    /**
     * 追加 {@code char} 数组参数的字符串表示形式到此序列。
     * <p>
     * 数组参数的字符按顺序追加到此序列的内容中。此序列的长度增加参数的长度。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(char[])} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   str   要追加的字符。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(char[] str) {
        int len = str.length;
        ensureCapacityInternal(count + len);
        System.arraycopy(str, 0, value, count, len);
        count += len;
        return this;
    }

    /**
     * 追加 {@code char} 数组的子数组的字符串表示形式到此序列。
     * <p>
     * 数组 {@code str} 的字符，从索引 {@code offset} 开始，按顺序追加到此序列的内容中。此序列的长度增加 {@code len} 的值。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(char[],int,int)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   str      要追加的字符。
     * @param   offset   要追加的第一个 {@code char} 的索引。
     * @param   len      要追加的 {@code char} 数。
     * @return  对此对象的引用。
     * @throws IndexOutOfBoundsException
     *         如果 {@code offset < 0} 或 {@code len < 0}
     *         或 {@code offset+len > str.length}
     */
    public AbstractStringBuilder append(char str[], int offset, int len) {
        if (len > 0)                // 让 arraycopy 报告 AIOOBE 对于 len < 0
            ensureCapacityInternal(count + len);
        System.arraycopy(str, offset, value, count, len);
        count += len;
        return this;
    }

    /**
     * 追加 {@code boolean} 参数的字符串表示形式到此序列。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(boolean)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   b   一个 {@code boolean}。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(boolean b) {
        if (b) {
            ensureCapacityInternal(count + 4);
            value[count++] = 't';
            value[count++] = 'r';
            value[count++] = 'u';
            value[count++] = 'e';
        } else {
            ensureCapacityInternal(count + 5);
            value[count++] = 'f';
            value[count++] = 'a';
            value[count++] = 'l';
            value[count++] = 's';
            value[count++] = 'e';
        }
        return this;
    }

    /**
     * 追加 {@code char} 参数的字符串表示形式到此序列。
     * <p>
     * 参数被追加到此序列的内容中。此序列的长度增加 {@code 1}。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(char)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   c   一个 {@code char}。
     * @return  对此对象的引用。
     */
    @Override
    public AbstractStringBuilder append(char c) {
        ensureCapacityInternal(count + 1);
        value[count++] = c;
        return this;
    }

    /**
     * 追加 {@code int} 参数的字符串表示形式到此序列。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(int)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   i   一个 {@code int}。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(int i) {
        if (i == Integer.MIN_VALUE) {
            append("-2147483648");
            return this;
        }
        int appendedLength = (i < 0) ? Integer.stringSize(-i) + 1
                                     : Integer.stringSize(i);
        int spaceNeeded = count + appendedLength;
        ensureCapacityInternal(spaceNeeded);
        Integer.getChars(i, spaceNeeded, value);
        count = spaceNeeded;
        return this;
    }

    /**
     * 追加 {@code long} 参数的字符串表示形式到此序列。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(long)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   l   一个 {@code long}。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(long l) {
        if (l == Long.MIN_VALUE) {
            append("-9223372036854775808");
            return this;
        }
        int appendedLength = (l < 0) ? Long.stringSize(-l) + 1
                                     : Long.stringSize(l);
        int spaceNeeded = count + appendedLength;
        ensureCapacityInternal(spaceNeeded);
        Long.getChars(l, spaceNeeded, value);
        count = spaceNeeded;
        return this;
    }

    /**
     * 追加 {@code float} 参数的字符串表示形式到此序列。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(float)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   f   一个 {@code float}。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(float f) {
        FloatingDecimal.appendTo(f,this);
        return this;
    }

    /**
     * 追加 {@code double} 参数的字符串表示形式到此序列。
     * <p>
     * 整体效果与将参数通过 {@link String#valueOf(double)} 方法转换为字符串，然后将该字符串的字符 {@link #append(String) 追加} 到此字符序列中完全相同。
     *
     * @param   d   一个 {@code double}。
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder append(double d) {
        FloatingDecimal.appendTo(d,this);
        return this;
    }

    /**
     * 从此序列中删除一个子字符串的字符。
     * 子字符串从指定的 {@code start} 开始，扩展到索引 {@code end - 1} 或者如果不存在这样的字符则扩展到序列的末尾。如果
     * {@code start} 等于 {@code end}，则不进行任何更改。
     *
     * @param      start  开始索引，包含。
     * @param      end    结束索引，不包含。
     * @return     此对象。
     * @throws     StringIndexOutOfBoundsException  如果 {@code start}
     *             为负数，大于 {@code length()}，或
     *             大于 {@code end}。
     */
    public AbstractStringBuilder delete(int start, int end) {
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (end > count)
            end = count;
        if (start > end)
            throw new StringIndexOutOfBoundsException();
        int len = end - start;
        if (len > 0) {
            System.arraycopy(value, start+len, value, start, count-end);
            count -= len;
        }
        return this;
    }

    /**
     * 追加 {@code codePoint} 参数的字符串表示形式到此序列。
     *
     * <p> 参数被追加到此序列的内容中。此序列的长度增加
     * {@link Character#charCount(int) Character.charCount(codePoint)}。
     *
     * <p> 整体效果与将参数通过 {@link Character#toChars(int)} 方法转换为 {@code char} 数组，然后将该数组中的字符 {@link #append(char[]) 追加} 到此字符序列中完全相同。
     *
     * @param   codePoint   一个 Unicode 代码点
     * @return  对此对象的引用。
     * @exception IllegalArgumentException 如果指定的
     * {@code codePoint} 不是有效的 Unicode 代码点
     */
    public AbstractStringBuilder appendCodePoint(int codePoint) {
        final int count = this.count;

        if (Character.isBmpCodePoint(codePoint)) {
            ensureCapacityInternal(count + 1);
            value[count] = (char) codePoint;
            this.count = count + 1;
        } else if (Character.isValidCodePoint(codePoint)) {
            ensureCapacityInternal(count + 2);
            Character.toSurrogates(codePoint, value, count);
            this.count = count + 2;
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }


                /**
     * 从该序列中指定位置移除 {@code char}。此序列缩短一个 {@code char}。
     *
     * <p>注意：如果给定索引处的字符是补充字符，此方法不会移除整个字符。如果需要正确处理补充字符，
     * 可以通过调用 {@code Character.charCount(thisSequence.codePointAt(index))} 来确定要移除的
     * {@code char} 数量，其中 {@code thisSequence} 是此序列。
     *
     * @param       index  要移除的 {@code char} 的索引
     * @return      此对象。
     * @throws      StringIndexOutOfBoundsException  如果 {@code index}
     *              为负数或大于等于 {@code length()}。
     */
    public AbstractStringBuilder deleteCharAt(int index) {
        if ((index < 0) || (index >= count))
            throw new StringIndexOutOfBoundsException(index);
        System.arraycopy(value, index+1, value, index, count-index-1);
        count--;
        return this;
    }

    /**
     * 用指定的 {@code String} 替换此序列中子字符串的字符。子字符串从指定的 {@code start} 开始，
     * 延伸到索引 {@code end - 1} 或到序列的末尾（如果不存在这样的字符）。首先移除子字符串中的字符，
     * 然后在 {@code start} 处插入指定的 {@code String}。（如果需要，此序列将被延长以容纳指定的字符串。）
     *
     * @param      start    开始索引，包含。
     * @param      end      结束索引，不包含。
     * @param      str   将替换先前内容的字符串。
     * @return     此对象。
     * @throws     StringIndexOutOfBoundsException  如果 {@code start}
     *             为负数，大于 {@code length()}，或大于 {@code end}。
     */
    public AbstractStringBuilder replace(int start, int end, String str) {
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (start > count)
            throw new StringIndexOutOfBoundsException("start > length()");
        if (start > end)
            throw new StringIndexOutOfBoundsException("start > end");

        if (end > count)
            end = count;
        int len = str.length();
        int newCount = count + len - (end - start);
        ensureCapacityInternal(newCount);

        System.arraycopy(value, end, value, start + len, count - end);
        str.getChars(value, start);
        count = newCount;
        return this;
    }

    /**
     * 返回一个新的 {@code String}，其中包含当前包含在此字符序列中的字符子序列。子字符串从指定的索引开始，
     * 延伸到此序列的末尾。
     *
     * @param      start    开始索引，包含。
     * @return     新字符串。
     * @throws     StringIndexOutOfBoundsException  如果 {@code start} 小于零，
     *             或大于此对象的长度。
     */
    public String substring(int start) {
        return substring(start, count);
    }

    /**
     * 返回一个新的字符序列，它是此序列的子序列。
     *
     * <p>此方法的调用形式
     *
     * <pre>{@code
     * sb.subSequence(begin,&nbsp;end)}</pre>
     *
     * 与以下调用形式的行为完全相同
     *
     * <pre>{@code
     * sb.substring(begin,&nbsp;end)}</pre>
     *
     * 提供此方法是为了使此类能够实现 {@link CharSequence} 接口。
     *
     * @param      start   开始索引，包含。
     * @param      end     结束索引，不包含。
     * @return     指定的子序列。
     *
     * @throws  IndexOutOfBoundsException
     *          如果 {@code start} 或 {@code end} 为负数，
     *          如果 {@code end} 大于 {@code length()}，
     *          或如果 {@code start} 大于 {@code end}
     * @spec JSR-51
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    /**
     * 返回一个新的 {@code String}，其中包含当前包含在此序列中的字符子序列。子字符串从指定的 {@code start} 开始，
     * 延伸到索引 {@code end - 1}。
     *
     * @param      start    开始索引，包含。
     * @param      end      结束索引，不包含。
     * @return     新字符串。
     * @throws     StringIndexOutOfBoundsException  如果 {@code start}
     *             或 {@code end} 为负数或大于 {@code length()}，或 {@code start}
     *             大于 {@code end}。
     */
    public String substring(int start, int end) {
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (end > count)
            throw new StringIndexOutOfBoundsException(end);
        if (start > end)
            throw new StringIndexOutOfBoundsException(end - start);
        return new String(value, start, end - start);
    }

    /**
     * 将 {@code str} 数组的子数组的字符串表示形式插入到此序列中。子数组从指定的 {@code offset} 开始，
     * 延伸 {@code len} 个 {@code char}。子数组的字符在指定的 {@code index} 处插入到此序列中。
     * 此序列的长度增加 {@code len} 个 {@code char}。
     *
     * @param      index    插入子数组的位置。
     * @param      str       一个 {@code char} 数组。
     * @param      offset   子数组中第一个 {@code char} 的索引。
     * @param      len      要插入的子数组中的 {@code char} 数量。
     * @return     此对象
     * @throws     StringIndexOutOfBoundsException  如果 {@code index}
     *             为负数或大于 {@code length()}，或 {@code offset} 或 {@code len} 为负数，或
     *             {@code (offset+len)} 大于 {@code str.length}。
     */
    public AbstractStringBuilder insert(int index, char[] str, int offset,
                                        int len)
    {
        if ((index < 0) || (index > length()))
            throw new StringIndexOutOfBoundsException(index);
        if ((offset < 0) || (len < 0) || (offset > str.length - len))
            throw new StringIndexOutOfBoundsException(
                "offset " + offset + ", len " + len + ", str.length "
                + str.length);
        ensureCapacityInternal(count + len);
        System.arraycopy(value, index, value, index + len, count - index);
        System.arraycopy(str, offset, value, index, len);
        count += len;
        return this;
    }

    /**
     * 将 {@code Object} 参数的字符串表示形式插入到此字符序列中。
     * <p>
     * 整体效果与将第二个参数通过 {@link String#valueOf(Object)} 方法转换为字符串，
     * 然后将该字符串的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于或等于 {@code 0}，并且小于或等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      obj      一个 {@code Object}。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, Object obj) {
        return insert(offset, String.valueOf(obj));
    }

    /**
     * 将字符串插入到此字符序列中。
     * <p>
     * 按顺序将 {@code String} 参数的字符插入到此序列的指定偏移量处，将原来在该位置之上的任何字符上移，
     * 并将此序列的长度增加参数的长度。如果 {@code str} 为 {@code null}，则将四个字符
     * {@code "null"} 插入到此序列中。
     * <p>
     * 新字符序列中索引 <i>k</i> 处的字符等于：
     * <ul>
     * <li>如果 <i>k</i> 小于 {@code offset}，则为旧字符序列中索引 <i>k</i> 处的字符
     * <li>如果 <i>k</i> 不小于 {@code offset} 但小于 {@code offset+str.length()}，则为
     * 参数 {@code str} 中索引 <i>k</i>{@code -offset} 处的字符
     * <li>如果 <i>k</i> 不小于 {@code offset+str.length()}，则为旧字符序列中索引 <i>k</i>{@code -str.length()} 处的字符
     * </ul><p>
     * {@code offset} 参数必须大于或等于 {@code 0}，并且小于或等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      str      一个字符串。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, String str) {
        if ((offset < 0) || (offset > length()))
            throw new StringIndexOutOfBoundsException(offset);
        if (str == null)
            str = "null";
        int len = str.length();
        ensureCapacityInternal(count + len);
        System.arraycopy(value, offset, value, offset + len, count - offset);
        str.getChars(value, offset);
        count += len;
        return this;
    }

    /**
     * 将 {@code char} 数组参数的字符串表示形式插入到此序列中。
     * <p>
     * 按顺序将数组参数的字符插入到此序列的指定偏移量处。此序列的长度增加参数的长度。
     * <p>
     * 整体效果与将第二个参数通过 {@link String#valueOf(char[])} 方法转换为字符串，
     * 然后将该字符串的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于或等于 {@code 0}，并且小于或等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      str      一个字符数组。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, char[] str) {
        if ((offset < 0) || (offset > length()))
            throw new StringIndexOutOfBoundsException(offset);
        int len = str.length;
        ensureCapacityInternal(count + len);
        System.arraycopy(value, offset, value, offset + len, count - offset);
        System.arraycopy(str, 0, value, offset, len);
        count += len;
        return this;
    }

    /**
     * 将指定的 {@code CharSequence} 插入到此序列中。
     * <p>
     * 按顺序将 {@code CharSequence} 参数的字符插入到此序列的指定偏移量处，将原来在该位置之上的任何字符上移，
     * 并将此序列的长度增加参数的长度。
     * <p>
     * 此方法的结果与调用此对象的
     * {@link #insert(int,CharSequence,int,int) insert}(dstOffset, s, 0, s.length())
     * 方法完全相同。
     *
     * <p>如果 {@code s} 为 {@code null}，则将四个字符
     * {@code "null"} 插入到此序列中。
     *
     * @param      dstOffset   偏移量。
     * @param      s the 要插入的序列
     * @return     对此对象的引用。
     * @throws     IndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int dstOffset, CharSequence s) {
        if (s == null)
            s = "null";
        if (s instanceof String)
            return this.insert(dstOffset, (String)s);
        return this.insert(dstOffset, s, 0, s.length());
    }

    /**
     * 将指定的 {@code CharSequence} 的子序列插入到此序列中。
     * <p>
     * 按顺序将参数 {@code s} 指定的子序列从 {@code start} 到 {@code end} 插入到此序列的指定目标偏移量处，
     * 将原来在该位置之上的任何字符上移。此序列的长度增加 {@code end - start}。
     * <p>
     * 此序列中索引 <i>k</i> 处的字符等于：
     * <ul>
     * <li>如果 <i>k</i> 小于 {@code dstOffset}，则为此序列中索引 <i>k</i> 处的字符
     * <li>如果 <i>k</i> 大于或等于 {@code dstOffset} 但小于 {@code dstOffset+end-start}，则为
     * 参数 {@code s} 中索引 <i>k</i>{@code +start-dstOffset} 处的字符
     * <li>如果 <i>k</i> 大于或等于 {@code dstOffset+end-start}，则为
     * 此序列中索引 <i>k</i>{@code -(end-start)} 处的字符
     * </ul><p>
     * {@code dstOffset} 参数必须大于或等于 {@code 0}，并且小于或等于此序列的 {@linkplain #length() 长度}。
     * <p>{@code start} 参数必须非负，且不大于 {@code end}。
     * <p>{@code end} 参数必须大于或等于 {@code start}，且不大于 s 的长度。
     *
     * <p>如果 {@code s} 为 {@code null}，则此方法插入字符，如同 s 参数是一个包含四个字符
     * {@code "null"} 的序列。
     *
     * @param      dstOffset   目标偏移量。
     * @param      s       要插入的序列。
     * @param      start   要插入的子序列的开始索引。
     * @param      end     要插入的子序列的结束索引。
     * @return     对此对象的引用。
     * @throws     IndexOutOfBoundsException  如果 {@code dstOffset}
     *             为负数或大于 {@code this.length()}，或
     *              {@code start} 或 {@code end} 为负数，或
     *              {@code start} 大于 {@code end} 或
     *              {@code end} 大于 {@code s.length()}
     */
     public AbstractStringBuilder insert(int dstOffset, CharSequence s,
                                         int start, int end) {
        if (s == null)
            s = "null";
        if ((dstOffset < 0) || (dstOffset > this.length()))
            throw new IndexOutOfBoundsException("dstOffset "+dstOffset);
        if ((start < 0) || (end < 0) || (start > end) || (end > s.length()))
            throw new IndexOutOfBoundsException(
                "start " + start + ", end " + end + ", s.length() "
                + s.length());
        int len = end - start;
        ensureCapacityInternal(count + len);
        System.arraycopy(value, dstOffset, value, dstOffset + len,
                         count - dstOffset);
        for (int i=start; i<end; i++)
            value[dstOffset++] = s.charAt(i);
        count += len;
        return this;
    }


                /**
     * 将 {@code boolean} 参数的字符串表示形式插入到此序列中。
     * <p>
     * 整体效果与第二个参数通过 {@link String#valueOf(boolean)} 方法转换为字符串，
     * 然后该字符串中的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于等于 {@code 0}，并且小于等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      b        一个 {@code boolean}。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, boolean b) {
        return insert(offset, String.valueOf(b));
    }

    /**
     * 将 {@code char} 参数的字符串表示形式插入到此序列中。
     * <p>
     * 整体效果与第二个参数通过 {@link String#valueOf(char)} 方法转换为字符串，
     * 然后该字符串中的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于等于 {@code 0}，并且小于等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      c        一个 {@code char}。
     * @return     对此对象的引用。
     * @throws     IndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, char c) {
        ensureCapacityInternal(count + 1);
        System.arraycopy(value, offset, value, offset + 1, count - offset);
        value[offset] = c;
        count += 1;
        return this;
    }

    /**
     * 将第二个 {@code int} 参数的字符串表示形式插入到此序列中。
     * <p>
     * 整体效果与第二个参数通过 {@link String#valueOf(int)} 方法转换为字符串，
     * 然后该字符串中的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于等于 {@code 0}，并且小于等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      i        一个 {@code int}。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, int i) {
        return insert(offset, String.valueOf(i));
    }

    /**
     * 将 {@code long} 参数的字符串表示形式插入到此序列中。
     * <p>
     * 整体效果与第二个参数通过 {@link String#valueOf(long)} 方法转换为字符串，
     * 然后该字符串中的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于等于 {@code 0}，并且小于等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      l        一个 {@code long}。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, long l) {
        return insert(offset, String.valueOf(l));
    }

    /**
     * 将 {@code float} 参数的字符串表示形式插入到此序列中。
     * <p>
     * 整体效果与第二个参数通过 {@link String#valueOf(float)} 方法转换为字符串，
     * 然后该字符串中的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于等于 {@code 0}，并且小于等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      f        一个 {@code float}。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, float f) {
        return insert(offset, String.valueOf(f));
    }

    /**
     * 将 {@code double} 参数的字符串表示形式插入到此序列中。
     * <p>
     * 整体效果与第二个参数通过 {@link String#valueOf(double)} 方法转换为字符串，
     * 然后该字符串中的字符 {@link #insert(int,String) 插入} 到此字符序列的指定偏移量处完全相同。
     * <p>
     * {@code offset} 参数必须大于等于 {@code 0}，并且小于等于此序列的 {@linkplain #length() 长度}。
     *
     * @param      offset   偏移量。
     * @param      d        一个 {@code double}。
     * @return     对此对象的引用。
     * @throws     StringIndexOutOfBoundsException  如果偏移量无效。
     */
    public AbstractStringBuilder insert(int offset, double d) {
        return insert(offset, String.valueOf(d));
    }

    /**
     * 返回此字符串中指定子字符串第一次出现的索引。返回的整数是最小值
     * <i>k</i>，使得：
     * <pre>{@code
     * this.toString().startsWith(str, <i>k</i>)
     * }</pre>
     * 为 {@code true}。
     *
     * @param   str   任何字符串。
     * @return  如果字符串参数作为子字符串在此对象中出现，则返回该子字符串的第一个字符的索引；
     *          如果它不作为子字符串出现，则返回 {@code -1}。
     */
    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    /**
     * 返回从指定索引开始的指定子字符串在此字符串中第一次出现的索引。返回的整数
     * 是最小值 {@code k}，使得：
     * <pre>{@code
     *     k >= Math.min(fromIndex, this.length()) &&
     *                   this.toString().startsWith(str, k)
     * }</pre>
     * 如果不存在这样的 <i>k</i> 值，则返回 -1。
     *
     * @param   str         要搜索的子字符串。
     * @param   fromIndex   开始搜索的索引。
     * @return  从指定索引开始的指定子字符串在此字符串中第一次出现的索引。
     */
    public int indexOf(String str, int fromIndex) {
        return String.indexOf(value, 0, count, str, fromIndex);
    }

    /**
     * 返回指定子字符串在此字符串中最后一次出现的索引。最右边的空字符串 "" 被认为出现在索引值
     * {@code this.length()} 处。返回的索引是最大值 <i>k</i>，使得
     * <pre>{@code
     * this.toString().startsWith(str, k)
     * }</pre>
     * 为 true。
     *
     * @param   str   要搜索的子字符串。
     * @return  如果字符串参数作为子字符串在此对象中出现一次或多次，则返回该子字符串的第一个字符的索引；
     *          如果它不作为子字符串出现，则返回 {@code -1}。
     */
    public int lastIndexOf(String str) {
        return lastIndexOf(str, count);
    }

    /**
     * 返回指定子字符串在此字符串中最后一次出现的索引。返回的整数是最大值 <i>k</i>，使得：
     * <pre>{@code
     *     k <= Math.min(fromIndex, this.length()) &&
     *                   this.toString().startsWith(str, k)
     * }</pre>
     * 如果不存在这样的 <i>k</i> 值，则返回 -1。
     *
     * @param   str         要搜索的子字符串。
     * @param   fromIndex   开始搜索的索引。
     * @return  指定子字符串在此序列中最后一次出现的索引。
     */
    public int lastIndexOf(String str, int fromIndex) {
        return String.lastIndexOf(value, 0, count, str, fromIndex);
    }

    /**
     * 使此字符序列被其逆序序列替换。如果序列中包含任何代理对，则这些代理对在逆序操作中被视为单个字符。
     * 因此，高-低代理对的顺序永远不会被颠倒。
     *
     * 设 <i>n</i> 为执行 {@code reverse} 方法之前此字符序列的字符长度（不是 {@code char} 值的长度）。
     * 则新字符序列中索引 <i>k</i> 处的字符等于旧字符序列中索引 <i>n-k-1</i> 处的字符。
     *
     * <p>注意，逆序操作可能会生成之前为未配对的低代理和高代理的有效代理对。例如，逆序
     * "\u005CuDC00\u005CuD800" 生成 "\u005CuD800\u005CuDC00"，这是一个有效的代理对。
     *
     * @return  对此对象的引用。
     */
    public AbstractStringBuilder reverse() {
        boolean hasSurrogates = false;
        int n = count - 1;
        for (int j = (n-1) >> 1; j >= 0; j--) {
            int k = n - j;
            char cj = value[j];
            char ck = value[k];
            value[j] = ck;
            value[k] = cj;
            if (Character.isSurrogate(cj) ||
                Character.isSurrogate(ck)) {
                hasSurrogates = true;
            }
        }
        if (hasSurrogates) {
            reverseAllValidSurrogatePairs();
        }
        return this;
    }

    /** 用于 reverse() 的辅助方法 */
    private void reverseAllValidSurrogatePairs() {
        for (int i = 0; i < count - 1; i++) {
            char c2 = value[i];
            if (Character.isLowSurrogate(c2)) {
                char c1 = value[i + 1];
                if (Character.isHighSurrogate(c1)) {
                    value[i++] = c1;
                    value[i] = c2;
                }
            }
        }
    }

    /**
     * 返回表示此序列中数据的字符串。
     * 分配并初始化一个新的 {@code String} 对象，以包含当前由此对象表示的字符序列。
     * 然后返回此 {@code String}。此序列的后续更改不会影响 {@code String} 的内容。
     *
     * @return  此字符序列的字符串表示形式。
     */
    @Override
    public abstract String toString();

    /**
     * 由 {@code String} 用于 contentEquals 方法。
     */
    final char[] getValue() {
        return value;
    }

}
