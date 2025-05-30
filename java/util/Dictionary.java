
/*
 * Copyright (c) 1995, 2004, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <code>Dictionary</code> 类是任何将键映射到值的类（如 <code>Hashtable</code>）的抽象父类。
 * 每个键和每个值都是一个对象。在任何 <tt>Dictionary</tt> 对象中，每个键最多与一个值相关联。
 * 给定一个 <tt>Dictionary</tt> 和一个键，可以查找关联的元素。任何非 <code>null</code> 的对象都可以用作键和值。
 * <p>
 * 通常，此类的实现应使用 <code>equals</code> 方法来决定两个键是否相同。
 * <p>
 * <strong>注意：此类已过时。新实现应实现 Map 接口，而不是扩展此类。</strong>
 *
 * @author  未署名
 * @see     java.util.Map
 * @see     java.lang.Object#equals(java.lang.Object)
 * @see     java.lang.Object#hashCode()
 * @see     java.util.Hashtable
 * @since   JDK1.0
 */
public abstract
class Dictionary<K,V> {
    /**
     * 唯一的构造函数。通常由子类构造函数隐式调用。
     */
    public Dictionary() {
    }

    /**
     * 返回此字典中的条目（不同键）数量。
     *
     * @return  此字典中的键的数量。
     */
    abstract public int size();

    /**
     * 测试此字典是否不映射任何键到值。 <tt>isEmpty</tt> 方法的一般约定是，结果为 true 当且仅当此字典不包含任何条目。
     *
     * @return  如果此字典不映射任何键到值，则返回 <code>true</code>；否则返回 <code>false</code>。
     */
    abstract public boolean isEmpty();

    /**
     * 返回此字典中的键的枚举。 <tt>keys</tt> 方法的一般约定是返回一个 <tt>Enumeration</tt> 对象，该对象将生成此字典中包含条目的所有键。
     *
     * @return  此字典中的键的枚举。
     * @see     java.util.Dictionary#elements()
     * @see     java.util.Enumeration
     */
    abstract public Enumeration<K> keys();

    /**
     * 返回此字典中的值的枚举。 <tt>elements</tt> 方法的一般约定是返回一个 <tt>Enumeration</tt>，该枚举将生成此字典中包含的所有元素。
     *
     * @return  此字典中的值的枚举。
     * @see     java.util.Dictionary#keys()
     * @see     java.util.Enumeration
     */
    abstract public Enumeration<V> elements();

    /**
     * 返回此字典中键映射的值。 <tt>isEmpty</tt> 方法的一般约定是，如果此字典包含指定键的条目，则返回关联的值；否则返回 <tt>null</tt>。
     *
     * @return  此字典中键映射的值；
     * @param   key   此字典中的一个键。
     *          如果键未映射到此字典中的任何值，则返回 <code>null</code>。
     * @exception NullPointerException 如果 <tt>key</tt> 为 <tt>null</tt>。
     * @see     java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    abstract public V get(Object key);

    /**
     * 在此字典中将指定的 <code>key</code> 映射到指定的 <code>value</code>。键和值都不能为 <code>null</code>。
     * <p>
     * 如果此字典已经包含指定 <tt>key</tt> 的条目，则返回此字典中该 <tt>key</tt> 已经存在的值，并修改条目以包含新元素。
     * <p>如果此字典尚未包含指定 <tt>key</tt> 的条目，则为指定的 <tt>key</tt> 和 <tt>value</tt> 创建一个条目，并返回 <tt>null</tt>。
     * <p>
     * 可以通过调用带有与原始 <code>key</code> 相等的 <code>key</code> 的 <code>get</code> 方法来检索 <code>value</code>。
     *
     * @param      key     哈希表的键。
     * @param      value   值。
     * @return     此字典中 <code>key</code> 映射的先前值，或者如果键没有先前映射，则返回 <code>null</code>。
     * @exception  NullPointerException  如果 <code>key</code> 或 <code>value</code> 为 <code>null</code>。
     * @see        java.lang.Object#equals(java.lang.Object)
     * @see        java.util.Dictionary#get(java.lang.Object)
     */
    abstract public V put(K key, V value);

    /**
     * 从此字典中移除 <code>key</code>（及其对应的 <code>value</code>）。如果 <code>key</code> 不在此字典中，则此方法不执行任何操作。
     *
     * @param   key   需要移除的键。
     * @return  此字典中 <code>key</code> 映射的值，或者如果键没有映射，则返回 <code>null</code>。
     * @exception NullPointerException 如果 <tt>key</tt> 为 <tt>null</tt>。
     */
    abstract public V remove(Object key);
}
