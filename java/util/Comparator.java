
/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.ToDoubleFunction;
import java.util.Comparators;

/**
 * 比较函数，对某些对象集合施加一个<i>全序</i>。比较器可以传递给排序方法（如 {@link Collections#sort(List,Comparator) Collections.sort} 或 {@link
 * Arrays#sort(Object[],Comparator) Arrays.sort}）以允许对排序顺序进行精确控制。比较器也可以用于控制某些数据结构（如 {@link SortedSet 排序集} 或 {@link
 * SortedMap 排序映射}）的顺序，或者为没有 {@link Comparable 自然顺序} 的对象集合提供排序。<p>
 *
 * 如果对于集合 <tt>S</tt> 中的每个 <tt>e1</tt> 和 <tt>e2</tt>，<tt>c.compare(e1, e2)==0</tt> 的布尔值与 <tt>e1.equals(e2)</tt> 相同，则称比较器 <tt>c</tt> 对集合 <tt>S</tt> 施加的排序与 <tt>equals</tt> 一致。<p>
 *
 * 使用能够施加与 <tt>equals</tt> 不一致的排序的比较器来排序集合（或映射）时应谨慎。假设使用显式比较器 <tt>c</tt> 的排序集（或排序映射）中的元素（或键）来自集合 <tt>S</tt>。如果 <tt>c</tt> 对 <tt>S</tt> 施加的排序与 <tt>equals</tt> 不一致，则排序集（或排序映射）的行为将“奇怪”。特别是，排序集（或排序映射）将违反以 <tt>equals</tt> 定义的集合（或映射）的一般合同。<p>
 *
 * 例如，假设向一个空的 {@code TreeSet} 添加两个元素 {@code a} 和 {@code b}，使得 {@code (a.equals(b) && c.compare(a, b) != 0)}，则第二个 {@code add} 操作将返回
 * true（并且树集的大小将增加），因为从树集的角度来看，{@code a} 和 {@code b} 不等，尽管这与 {@link Set#add Set.add} 方法的规范相矛盾。<p>
 *
 * 注意：通常建议比较器也实现 <tt>java.io.Serializable</tt>，因为它们可能用作可序列化数据结构（如 {@link TreeSet}、{@link TreeMap}）的排序方法。为了使数据结构成功序列化，比较器（如果提供）必须实现 <tt>Serializable</tt>。<p>
 *
 * 对于数学爱好者，给定比较器 <tt>c</tt> 对给定对象集合 <tt>S</tt> 施加的<i>关系</i>定义为：<pre>
 *       {(x, y) such that c.compare(x, y) &lt;= 0}.
 * </pre> 该全序的<i>商集</i>为：<pre>
 *       {(x, y) such that c.compare(x, y) == 0}.
 * </pre>
 *
 * 从 <tt>compare</tt> 的合同可以立即得出，商集是 <tt>S</tt> 上的<i>等价关系</i>，而施加的排序是 <tt>S</tt> 上的<i>全序</i>。当我们说比较器 <tt>c</tt> 对 <tt>S</tt> 施加的排序与 <tt>equals</tt> 一致时，我们的意思是该排序的商集是由对象的 {@link Object#equals(Object)
 * equals(Object)} 方法定义的等价关系：<pre>
 *     {(x, y) such that x.equals(y)}. </pre>
 *
 * <p>与 {@code Comparable} 不同，比较器可以选择允许比较 null 参数，同时保持等价关系的要求。
 *
 * <p>此接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a> 的成员。
 *
 * @param <T> 可能由该比较器比较的对象类型
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Comparable
 * @see java.io.Serializable
 * @since 1.2
 */
@FunctionalInterface
public interface Comparator<T> {
    /**
     * 比较其两个参数的顺序。如果第一个参数小于、等于或大于第二个参数，则分别返回负整数、零或正整数。<p>
     *
     * 在上述描述中，符号 <tt>sgn(</tt><i>expression</i><tt>)</tt> 表示数学上的<i>符号函数</i>，定义为根据 <i>expression</i> 的值是负数、零还是正数返回 <tt>-1</tt>、<tt>0</tt> 或 <tt>1</tt>。<p>
     *
     * 实现者必须确保 <tt>sgn(compare(x, y)) == -sgn(compare(y, x))</tt> 对所有 <tt>x</tt> 和 <tt>y</tt> 成立。（这意味着 <tt>compare(x, y)</tt> 抛出异常当且仅当 <tt>compare(y, x)</tt> 抛出异常。）<p>
     *
     * 实现者还必须确保关系是传递的：<tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> 意味着 <tt>compare(x, z)&gt;0</tt>。<p>
     *
     * 最后，实现者必须确保 <tt>compare(x, y)==0</tt> 意味着 <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> 对所有 <tt>z</tt> 成立。<p>
     *
     * 通常情况下，但<i>并非</i>严格要求，<tt>(compare(x, y)==0) == (x.equals(y))</tt>。通常来说，任何违反此条件的比较器都应明确指出这一事实。推荐的语言是“注意：此比较器施加的排序与 equals 不一致。”
     *
     * @param o1 要比较的第一个对象。
     * @param o2 要比较的第二个对象。
     * @return 如果第一个参数小于、等于或大于第二个参数，则分别返回负整数、零或正整数。
     * @throws NullPointerException 如果参数为 null 且此比较器不允许 null 参数。
     * @throws ClassCastException 如果参数的类型阻止它们被此比较器比较。
     */
    int compare(T o1, T o2);


                /**
     * 表示是否有其他对象“等于”此比较器。此方法必须遵守 {@link Object#equals(Object)} 的一般约定。此外，此方法只能返回 <tt>true</tt> <i>仅当</i> 指定的对象也是一个比较器，并且它施加与本比较器相同的顺序。因此，<code>comp1.equals(comp2)</code> 意味着 <tt>sgn(comp1.compare(o1, o2))==sgn(comp2.compare(o1, o2))</tt> 对于每个对象引用 <tt>o1</tt> 和 <tt>o2</tt>。<p>
     *
     * 注意，不覆盖 <tt>Object.equals(Object)</tt> 总是<i>安全的</i>。然而，在某些情况下，覆盖此方法可以提高性能，允许程序确定两个不同的比较器施加相同的顺序。
     *
     * @param   obj   要比较的引用对象。
     * @return  <code>true</code> 仅当指定的对象也是一个比较器，并且它施加与本比较器相同的顺序。
     * @see Object#equals(Object)
     * @see Object#hashCode()
     */
    boolean equals(Object obj);

    /**
     * 返回一个施加与此比较器相反顺序的比较器。
     *
     * @return 一个施加与此比较器相反顺序的比较器。
     * @since 1.8
     */
    default Comparator<T> reversed() {
        return Collections.reverseOrder(this);
    }

    /**
     * 返回一个词典顺序的比较器，与另一个比较器组合。如果此 {@code Comparator} 认为两个元素相等，即 {@code compare(a, b) == 0}，则使用 {@code other} 来确定顺序。
     *
     * <p>返回的比较器是可序列化的，如果指定的比较器也是可序列化的。
     *
     * @apiNote
     * 例如，为了基于长度和然后按大小写不敏感的自然顺序对 {@code String} 集合进行排序，可以使用以下代码组合比较器，
     *
     * <pre>{@code
     *     Comparator<String> cmp = Comparator.comparingInt(String::length)
     *             .thenComparing(String.CASE_INSENSITIVE_ORDER);
     * }</pre>
     *
     * @param  other 当此比较器认为两个对象相等时使用的另一个比较器。
     * @return 由本比较器和另一个比较器组合而成的词典顺序比较器
     * @throws NullPointerException 如果参数为 null。
     * @since 1.8
     */
    default Comparator<T> thenComparing(Comparator<? super T> other) {
        Objects.requireNonNull(other);
        return (Comparator<T> & Serializable) (c1, c2) -> {
            int res = compare(c1, c2);
            return (res != 0) ? res : other.compare(c1, c2);
        };
    }

    /**
     * 返回一个词典顺序的比较器，与一个提取键进行比较的函数组合。
     *
     * @implSpec 此默认实现的行为类似于 {@code thenComparing(comparing(keyExtractor, cmp))}。
     *
     * @param  <U>  排序键的类型
     * @param  keyExtractor 用于提取排序键的函数
     * @param  keyComparator 用于比较排序键的 {@code Comparator}
     * @return 由本比较器和提取键的函数组合而成的词典顺序比较器
     * @throws NullPointerException 如果任一参数为 null。
     * @see #comparing(Function, Comparator)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default <U> Comparator<T> thenComparing(
            Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator)
    {
        return thenComparing(comparing(keyExtractor, keyComparator));
    }

    /**
     * 返回一个词典顺序的比较器，与一个提取 {@code Comparable} 排序键的函数组合。
     *
     * @implSpec 此默认实现的行为类似于 {@code thenComparing(comparing(keyExtractor))}。
     *
     * @param  <U>  {@link Comparable} 排序键的类型
     * @param  keyExtractor 用于提取 {@link Comparable} 排序键的函数
     * @return 由本比较器和 {@link Comparable} 排序键组合而成的词典顺序比较器。
     * @throws NullPointerException 如果参数为 null。
     * @see #comparing(Function)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default <U extends Comparable<? super U>> Comparator<T> thenComparing(
            Function<? super T, ? extends U> keyExtractor)
    {
        return thenComparing(comparing(keyExtractor));
    }

    /**
     * 返回一个词典顺序的比较器，与一个提取 {@code int} 排序键的函数组合。
     *
     * @implSpec 此默认实现的行为类似于 {@code thenComparing(comparingInt(keyExtractor))}。
     *
     * @param  keyExtractor 用于提取整数排序键的函数
     * @return 由本比较器和 {@code int} 排序键组合而成的词典顺序比较器
     * @throws NullPointerException 如果参数为 null。
     * @see #comparingInt(ToIntFunction)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default Comparator<T> thenComparingInt(ToIntFunction<? super T> keyExtractor) {
        return thenComparing(comparingInt(keyExtractor));
    }

    /**
     * 返回一个词典顺序的比较器，与一个提取 {@code long} 排序键的函数组合。
     *
     * @implSpec 此默认实现的行为类似于 {@code thenComparing(comparingLong(keyExtractor))}。
     *
     * @param  keyExtractor 用于提取长整数排序键的函数
     * @return 由本比较器和 {@code long} 排序键组合而成的词典顺序比较器
     * @throws NullPointerException 如果参数为 null。
     * @see #comparingLong(ToLongFunction)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
        return thenComparing(comparingLong(keyExtractor));
    }


                /**
     * 返回一个词典序比较器，该比较器使用提取 {@code double} 排序键的函数。
     *
     * @implSpec 此默认实现的行为类似于 {@code
     *           thenComparing(comparingDouble(keyExtractor))}。
     *
     * @param  keyExtractor 用于提取 double 排序键的函数
     * @return 由此比较器和随后的 {@code double} 排序键组成的词典序比较器
     * @throws NullPointerException 如果参数为 null。
     * @see #comparingDouble(ToDoubleFunction)
     * @see #thenComparing(Comparator)
     * @since 1.8
     */
    default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        return thenComparing(comparingDouble(keyExtractor));
    }

    /**
     * 返回一个比较器，该比较器强制执行 <em>自然顺序</em> 的反序。
     *
     * <p>返回的比较器是可序列化的，并在比较 {@code null} 时抛出 {@link
     * NullPointerException}。
     *
     * @param  <T> 要比较的 {@link Comparable} 元素类型
     * @return 一个对 {@code Comparable} 对象强制执行 <i>自然顺序</i> 反序的比较器。
     * @see Comparable
     * @since 1.8
     */
    public static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }

    /**
     * 返回一个比较器，该比较器按自然顺序比较 {@link Comparable} 对象。
     *
     * <p>返回的比较器是可序列化的，并在比较 {@code null} 时抛出 {@link
     * NullPointerException}。
     *
     * @param  <T> 要比较的 {@link Comparable} 元素类型
     * @return 一个对 {@code Comparable} 对象强制执行 <i>自然顺序</i> 的比较器。
     * @see Comparable
     * @since 1.8
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (Comparator<T>) Comparators.NaturalOrderComparator.INSTANCE;
    }

    /**
     * 返回一个对 {@code null} 友好的比较器，该比较器认为 {@code null} 小于非 {@code null}。当两者都是 {@code null} 时，它们被认为是相等的。如果两者都不是 {@code null}，则使用指定的 {@code Comparator} 确定顺序。如果指定的比较器为 {@code null}，则返回的比较器认为所有非 {@code null} 值都是相等的。
     *
     * <p>如果指定的比较器是可序列化的，则返回的比较器也是可序列化的。
     *
     * @param  <T> 要比较的元素类型
     * @param  comparator 用于比较非 {@code null} 值的 {@code Comparator}
     * @return 一个认为 {@code null} 小于非 {@code null}，并使用提供的 {@code Comparator} 比较非 {@code null} 对象的比较器。
     * @since 1.8
     */
    public static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        return new Comparators.NullComparator<>(true, comparator);
    }

    /**
     * 返回一个对 {@code null} 友好的比较器，该比较器认为 {@code null} 大于非 {@code null}。当两者都是 {@code null} 时，它们被认为是相等的。如果两者都不是 {@code null}，则使用指定的 {@code Comparator} 确定顺序。如果指定的比较器为 {@code null}，则返回的比较器认为所有非 {@code null} 值都是相等的。
     *
     * <p>如果指定的比较器是可序列化的，则返回的比较器也是可序列化的。
     *
     * @param  <T> 要比较的元素类型
     * @param  comparator 用于比较非 {@code null} 值的 {@code Comparator}
     * @return 一个认为 {@code null} 大于非 {@code null}，并使用提供的 {@code Comparator} 比较非 {@code null} 对象的比较器。
     * @since 1.8
     */
    public static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        return new Comparators.NullComparator<>(false, comparator);
    }

    /**
     * 接受一个从类型 {@code T} 提取排序键的函数，并返回一个使用指定 {@link Comparator} 比较该排序键的 {@code Comparator<T>}。
     *
     * <p>如果指定的函数和比较器都是可序列化的，则返回的比较器也是可序列化的。
     *
     * @apiNote
     * 例如，要获得一个按忽略大小写的姓氏比较 {@code Person} 对象的 {@code Comparator}，
     *
     * <pre>{@code
     *     Comparator<Person> cmp = Comparator.comparing(
     *             Person::getLastName,
     *             String.CASE_INSENSITIVE_ORDER);
     * }</pre>
     *
     * @param  <T> 要比较的元素类型
     * @param  <U> 排序键的类型
     * @param  keyExtractor 用于提取排序键的函数
     * @param  keyComparator 用于比较排序键的 {@code Comparator}
     * @return 一个使用指定 {@code Comparator} 比较提取键的比较器
     * @throws NullPointerException 如果任一参数为 null
     * @since 1.8
     */
    public static <T, U> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator)
    {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return (Comparator<T> & Serializable)
            (c1, c2) -> keyComparator.compare(keyExtractor.apply(c1),
                                              keyExtractor.apply(c2));
    }

    /**
     * 接受一个从类型 {@code T} 提取 {@link java.lang.Comparable Comparable} 排序键的函数，并返回一个按该排序键比较的 {@code Comparator<T>}。
     *
     * <p>如果指定的函数是可序列化的，则返回的比较器也是可序列化的。
     *
     * @apiNote
     * 例如，要获得一个按姓氏比较 {@code Person} 对象的 {@code Comparator}，
     *
     * <pre>{@code
     *     Comparator<Person> byLastName = Comparator.comparing(Person::getLastName);
     * }</pre>
     *
     * @param  <T> 要比较的元素类型
     * @param  <U> {@code Comparable} 排序键的类型
     * @param  keyExtractor 用于提取 {@link Comparable} 排序键的函数
     * @return 一个按提取键比较的比较器
     * @throws NullPointerException 如果参数为 null
     * @since 1.8
     */
    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor)
    {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> keyExtractor.apply(c1).compareTo(keyExtractor.apply(c2));
    }


                /**
     * 接受一个从类型 {@code T} 提取 {@code int} 排序键的函数，并返回一个 {@code Comparator<T>}，该比较器通过该排序键进行比较。
     *
     * <p>如果指定的函数也是可序列化的，则返回的比较器是可序列化的。
     *
     * @param  <T> 要比较的元素类型
     * @param  keyExtractor 用于提取整数排序键的函数
     * @return 通过提取的键进行比较的比较器
     * @see #comparing(Function)
     * @throws NullPointerException 如果参数为 null
     * @since 1.8
     */
    public static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> Integer.compare(keyExtractor.applyAsInt(c1), keyExtractor.applyAsInt(c2));
    }

    /**
     * 接受一个从类型 {@code T} 提取 {@code long} 排序键的函数，并返回一个 {@code Comparator<T>}，该比较器通过该排序键进行比较。
     *
     * <p>如果指定的函数也是可序列化的，则返回的比较器是可序列化的。
     *
     * @param  <T> 要比较的元素类型
     * @param  keyExtractor 用于提取长整数排序键的函数
     * @return 通过提取的键进行比较的比较器
     * @see #comparing(Function)
     * @throws NullPointerException 如果参数为 null
     * @since 1.8
     */
    public static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> Long.compare(keyExtractor.applyAsLong(c1), keyExtractor.applyAsLong(c2));
    }

    /**
     * 接受一个从类型 {@code T} 提取 {@code double} 排序键的函数，并返回一个 {@code Comparator<T>}，该比较器通过该排序键进行比较。
     *
     * <p>如果指定的函数也是可序列化的，则返回的比较器是可序列化的。
     *
     * @param  <T> 要比较的元素类型
     * @param  keyExtractor 用于提取双精度浮点数排序键的函数
     * @return 通过提取的键进行比较的比较器
     * @see #comparing(Function)
     * @throws NullPointerException 如果参数为 null
     * @since 1.8
     */
    public static<T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (Comparator<T> & Serializable)
            (c1, c2) -> Double.compare(keyExtractor.applyAsDouble(c1), keyExtractor.applyAsDouble(c2));
    }
}
