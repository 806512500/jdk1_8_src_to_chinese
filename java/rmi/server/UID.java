/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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
package java.rmi.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;

/**
 * <code>UID</code> 表示一个标识符，该标识符在其生成的主机上是唯一的，或者是一个 2<sup>16</sup>
 * 个“知名”标识符之一。
 *
 * <p>{@link #UID()} 构造函数可以用于生成一个在其生成的主机上是唯一的标识符。{@link #UID(short)} 构造函数可以用于
 * 创建一个 2<sup>16</sup> 个知名标识符之一。
 *
 * <p><code>UID</code> 实例包含三个基本值：
 * <ul>
 * <li><code>unique</code>，一个 <code>int</code>，唯一标识生成此 <code>UID</code> 的虚拟机，相对于其主机和
 * <code>time</code> 值表示的时间（<code>unique</code> 值的一个示例实现可以是进程标识符），
 * 或者对于知名 <code>UID</code> 为零
 * <li><code>time</code>，一个 <code>long</code>，等于生成此 <code>UID</code> 的虚拟机在某个时间点的系统时间（由
 * {@link System#currentTimeMillis()} 返回），或者对于知名 <code>UID</code> 为零
 * <li><code>count</code>，一个 <code>short</code>，用于区分在同一虚拟机中生成且具有相同 <code>time</code> 值的
 * <code>UID</code>
 * </ul>
 *
 * <p>只要主机重启需要超过一毫秒的时间且系统时钟从未被设置回退，独立生成的 <code>UID</code> 实例在其生成的主机上是唯一的。
 * 通过将 <code>UID</code> 实例与唯一的主机标识符（如 IP 地址）配对，可以构造一个全局唯一的标识符。
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 * @since       JDK1.1
 */
public final class UID implements Serializable {

    private static int hostUnique;
    private static boolean hostUniqueSet = false;

    private static final Object lock = new Object();
    private static long lastTime = System.currentTimeMillis();
    private static short lastCount = Short.MIN_VALUE;

    /** 表示与 JDK 1.1.x 版本的类兼容 */
    private static final long serialVersionUID = 1086053664494604050L;

    /**
     * 唯一标识生成此 <code>UID</code> 的虚拟机，相对于其主机和给定时间
     * @serial
     */
    private final int unique;

    /**
     * 生成此 <code>UID</code> 的虚拟机在某个时间点的系统时间（由 {@link System#currentTimeMillis()} 返回）
     * @serial
     */
    private final long time;

    /**
     * 16 位数字，用于区分在同一虚拟机中生成且具有相同时间值的 <code>UID</code> 实例
     * @serial
     */
    private final short count;

    /**
     * 生成一个在其生成的主机上是唯一的 <code>UID</code>。
     */
    public UID() {

        synchronized (lock) {
            if (!hostUniqueSet) {
                hostUnique = (new SecureRandom()).nextInt();
                hostUniqueSet = true;
            }
            unique = hostUnique;
            if (lastCount == Short.MAX_VALUE) {
                boolean interrupted = Thread.interrupted();
                boolean done = false;
                while (!done) {
                    long now = System.currentTimeMillis();
                    if (now == lastTime) {
                        // 等待时间变化
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    } else {
                        // 如果系统时间回退，增加原始时间 1 毫秒以保持唯一性
                        lastTime = (now < lastTime) ? lastTime + 1 : now;
                        lastCount = Short.MIN_VALUE;
                        done = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            time = lastTime;
            count = lastCount++;
        }
    }

    /**
     * 创建一个“知名”<code>UID</code>。
     *
     * 有 2<sup>16</sup> 个可能的知名标识符。
     *
     * <p>通过此构造函数创建的 <code>UID</code> 不会与通过无参构造函数生成的任何 <code>UID</code> 冲突。
     *
     * @param   num 知名 <code>UID</code> 的编号
     */
    public UID(short num) {
        unique = 0;
        time = 0;
        count = num;
    }

    /**
     * 根据从流中读取的数据构造一个 <code>UID</code>。
     */
    private UID(int unique, long time, short count) {
        this.unique = unique;
        this.time = time;
        this.count = count;
    }

    /**
     * 返回此 <code>UID</code> 的哈希码值。
     *
     * @return  此 <code>UID</code> 的哈希码值
     */
    public int hashCode() {
        return (int) time + (int) count;
    }

    /**
     * 将指定对象与此 <code>UID</code> 进行比较以确定是否相等。
     *
     * 仅当指定对象是一个具有与本 <code>UID</code> 相同的 <code>unique</code>、<code>time</code> 和 <code>count</code>
     * 值的 <code>UID</code> 实例时，此方法才返回 <code>true</code>。
     *
     * @param   obj 要与此 <code>UID</code> 比较的对象
     *
     * @return  如果给定对象与此对象等效，则返回 <code>true</code>，否则返回 <code>false</code>
     */
    public boolean equals(Object obj) {
        if (obj instanceof UID) {
            UID uid = (UID) obj;
            return (unique == uid.unique &&
                    count == uid.count &&
                    time == uid.time);
        } else {
            return false;
        }
    }

    /**
     * 返回此 <code>UID</code> 的字符串表示形式。
     *
     * @return  此 <code>UID</code> 的字符串表示形式
     */
    public String toString() {
        return Integer.toString(unique, 16) + ":" +
            Long.toString(time, 16) + ":" +
            Integer.toString(count, 16);
    }

    /**
     * 将此 <code>UID</code> 的二进制表示形式写入 <code>DataOutput</code> 实例。
     *
     * <p>具体来说，此方法首先调用给定流的 {@link DataOutput#writeInt(int)} 方法，将此 <code>UID</code> 的
     * <code>unique</code> 值写入，然后调用流的 {@link DataOutput#writeLong(long)} 方法，将此 <code>UID</code> 的
     * <code>time</code> 值写入，最后调用流的 {@link DataOutput#writeShort(int)} 方法，将此 <code>UID</code> 的
     * <code>count</code> 值写入。
     *
     * @param   out 要写入此 <code>UID</code> 的 <code>DataOutput</code> 实例
     *
     * @throws  IOException 如果在执行此操作时发生 I/O 错误
     */
    public void write(DataOutput out) throws IOException {
        out.writeInt(unique);
        out.writeLong(time);
        out.writeShort(count);
    }

    /**
     * 从 <code>DataInput</code> 实例中反序列化二进制表示形式，构造并返回一个新的 <code>UID</code> 实例。
     *
     * <p>具体来说，此方法首先调用给定流的 {@link DataInput#readInt()} 方法读取 <code>unique</code> 值，
     * 然后调用流的 {@link DataInput#readLong()} 方法读取 <code>time</code> 值，再调用流的
     * {@link DataInput#readShort()} 方法读取 <code>count</code> 值，最后创建并返回一个新的包含从流中读取的
     * <code>unique</code>、<code>time</code> 和 <code>count</code> 值的 <code>UID</code> 实例。
     *
     * @param   in 要从中读取 <code>UID</code> 的 <code>DataInput</code> 实例
     *
     * @return  反序列化的 <code>UID</code> 实例
     *
     * @throws  IOException 如果在执行此操作时发生 I/O 错误
     */
    public static UID read(DataInput in) throws IOException {
        int unique = in.readInt();
        long time = in.readLong();
        short count = in.readShort();
        return new UID(unique, time, count);
    }
}
