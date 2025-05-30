/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * 一个 {@link BlockingQueue}，其中生产者可以等待消费者接收元素。例如，在消息传递应用程序中，生产者有时（使用方法 {@link #transfer}）等待消费者调用 {@code take} 或 {@code poll} 接收元素，而在其他时候则通过方法 {@code put} 将元素入队而不等待接收。
 * {@linkplain #tryTransfer(Object) 非阻塞} 和 {@linkplain #tryTransfer(Object,long,TimeUnit) 带超时} 的 {@code tryTransfer} 版本也可用。
 * 可以通过 {@link #hasWaitingConsumer} 查询是否有线程正在等待项目，这与 {@code peek} 操作相反。
 *
 * <p>像其他阻塞队列一样，一个 {@code TransferQueue} 可能是容量受限的。如果是这样，尝试传输操作可能最初会阻塞等待可用空间，然后等待消费者接收。注意，在容量为零的队列中，例如 {@link SynchronousQueue}，{@code put} 和 {@code transfer} 实际上是同义的。
 *
 * <p>此接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a> 的成员。
 *
 * @since 1.7
 * @author Doug Lea
 * @param <E> 此集合中持有的元素类型
 */
public interface TransferQueue<E> extends BlockingQueue<E> {
    /**
     * 如果可能，立即将元素传输给等待的消费者。
     *
     * <p>更准确地说，如果存在一个消费者已经在等待接收它（在 {@link #take} 或带超时的 {@link #poll(long,TimeUnit) poll} 中），则立即将指定的元素传输，否则不将元素入队并返回 {@code false}。
     *
     * @param e 要传输的元素
     * @return 如果元素被传输，则返回 {@code true}，否则返回 {@code false}
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此队列中
     * @throws NullPointerException 如果指定的元素为 null
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此队列中
     */
    boolean tryTransfer(E e);

    /**
     * 等待必要的时间将元素传输给消费者。
     *
     * <p>更准确地说，如果存在一个消费者已经在等待接收它（在 {@link #take} 或带超时的 {@link #poll(long,TimeUnit) poll} 中），则立即将指定的元素传输，否则等待直到元素被消费者接收。
     *
     * @param e 要传输的元素
     * @throws InterruptedException 如果在等待过程中被中断，此时元素不会留在队列中
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此队列中
     * @throws NullPointerException 如果指定的元素为 null
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此队列中
     */
    void transfer(E e) throws InterruptedException;

    /**
     * 如果在超时前可以传输元素，则将元素传输给消费者。
     *
     * <p>更准确地说，如果存在一个消费者已经在等待接收它（在 {@link #take} 或带超时的 {@link #poll(long,TimeUnit) poll} 中），则立即将指定的元素传输，否则等待直到元素被消费者接收，如果指定的等待时间到期前无法传输元素，则返回 {@code false}。
     *
     * @param e 要传输的元素
     * @param timeout 在放弃前等待的时间，以 {@code unit} 为单位
     * @param unit 一个 {@code TimeUnit}，用于解释 {@code timeout} 参数
     * @return 如果成功，则返回 {@code true}，如果指定的等待时间到期前无法完成传输，则返回 {@code false}，此时元素不会留在队列中
     * @throws InterruptedException 如果在等待过程中被中断，此时元素不会留在队列中
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此队列中
     * @throws NullPointerException 如果指定的元素为 null
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此队列中
     */
    boolean tryTransfer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 如果至少有一个消费者正在通过 {@link #take} 或带超时的 {@link #poll(long,TimeUnit) poll} 等待接收元素，则返回 {@code true}。返回值表示一个瞬时状态。
     *
     * @return 如果至少有一个等待的消费者，则返回 {@code true}
     */
    boolean hasWaitingConsumer();

    /**
     * 返回通过 {@link #take} 或带超时的 {@link #poll(long,TimeUnit) poll} 等待接收元素的消费者数量的估计值。返回值是一个瞬时状态的近似值，可能不准确，因为消费者可能已经完成或放弃等待。该值对于监控和启发式分析有用，但不适合同步控制。此方法的实现可能明显慢于 {@link #hasWaitingConsumer}。
     *
     * @return 等待接收元素的消费者数量
     */
    int getWaitingConsumerCount();
}
