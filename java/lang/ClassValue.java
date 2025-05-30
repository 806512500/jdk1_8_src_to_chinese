
/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ClassValue.ClassValueMap;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import sun.misc.Unsafe;

import static java.lang.ClassValue.ClassValueMap.probeHomeLocation;
import static java.lang.ClassValue.ClassValueMap.probeBackupLocations;

/**
 * 懒惰地将计算值与（潜在的）每个类型关联。
 * 例如，如果动态语言需要为每个类在消息发送调用站点构造一个消息分派表，
 * 它可以使用 {@code ClassValue} 来缓存信息，以便快速执行消息发送，对于每个遇到的类。
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public abstract class ClassValue<T> {
    /**
     * 唯一的构造函数。 （通常由子类构造函数调用，通常是隐式的。）
     */
    protected ClassValue() {
    }

    /**
     * 计算给定类的派生值。
     * <p>
     * 当第一个线程访问该值时，将调用此方法。
     * <p>
     * 通常，此方法每个类最多调用一次，但如果已调用 {@link #remove remove}，则可能会再次调用。
     * <p>
     * 如果此方法抛出异常，相应的 {@code get} 调用将以该异常异常终止，且不会记录类值。
     *
     * @param type 需要计算类值的类型
     * @return 与该 {@code ClassValue} 关联的为给定类或接口新计算的值
     * @see #get
     * @see #remove
     */
    protected abstract T computeValue(Class<?> type);

    /**
     * 返回给定类的值。
     * 如果尚未计算值，则通过调用 {@link #computeValue computeValue} 方法获得。
     * <p>
     * 实际将值安装到类上是原子操作。
     * 在这一点上，如果有多个竞争线程计算了值，将选择其中一个，并返回给所有竞争线程。
     * <p>
     * {@code type} 参数通常是类，但也可以是任何类型，例如接口、原始类型（如 {@code int.class}）或 {@code void.class}。
     * <p>
     * 在没有 {@code remove} 调用的情况下，类值具有简单的状态图：未初始化和已初始化。
     * 当调用 {@code remove} 时，值观察的规则更为复杂。
     * 有关更多信息，请参阅 {@link #remove remove} 的文档。
     *
     * @param type 需要计算或检索类值的类型
     * @return 与该 {@code ClassValue} 关联的为给定类或接口的当前值
     * @throws NullPointerException 如果参数为 null
     * @see #remove
     * @see #computeValue
     */
    public T get(Class<?> type) {
        // 非竞争 this.hashCodeForCache : final int
        Entry<?>[] cache;
        Entry<T> e = probeHomeLocation(cache = getCacheCarefully(type), this);
        // 竞争 e : 当前值 <=> 来自当前缓存或陈旧缓存的陈旧值
        // 不变性： e 为 null 或具有可读 Entry.version 和 Entry.value 的 Entry
        if (match(e))
            // 不变性： 没有误匹配。 如果罕见，误否定是可以的。
            // 使这有效的主要事实：如果 this.version == e.version，
            // 则此线程有权观察（final）e.value。
            return e.value();
        // 快速路径可能因以下任何原因失败：
        // 1. 尚未计算任何条目
        // 2. 哈希码冲突（在减少模 cache.length 之前或之后）
        // 3. 已删除条目（在该类型或另一个类型上）
        // 4. GC 以某种方式删除了 e.version 并清除了引用
        return getFromBackup(cache, type);
    }

    /**
     * 移除给定类的关联值。
     * 如果随后为同一类 {@linkplain #get 读取} 该值，
     * 将通过调用其 {@link #computeValue computeValue} 方法重新初始化该值。
     * 这可能导致对给定类的 {@code computeValue} 方法的额外调用。
     * <p>
     * 为了解释 {@code get} 和 {@code remove} 调用之间的交互，
     * 我们必须对类值的状态转换进行建模，以考虑未初始化和已初始化状态之间的交替。
     * 为此，按顺序对这些状态进行编号，并注意
     * 未初始化（或已删除）状态编号为偶数，
     * 而已初始化（或重新初始化）状态编号为奇数。
     * <p>
     * 当线程 {@code T} 在状态 {@code 2N} 移除类值时，
     * 什么都不会发生，因为类值已经未初始化。
     * 否则，状态将原子地推进到 {@code 2N+1}。
     * <p>
     * 当线程 {@code T} 查询状态为 {@code 2N} 的类值时，
     * 线程首先尝试通过调用 {@code computeValue} 并安装计算结果值将类值初始化到状态 {@code 2N+1}。
     * <p>
     * 当 {@code T} 尝试安装新计算的值时，
     * 如果状态仍为 {@code 2N}，则类值将使用计算的值初始化，
     * 推进到状态 {@code 2N+1}。
     * <p>
     * 否则，无论新状态是偶数还是奇数，
     * {@code T} 将丢弃新计算的值并重试 {@code get} 操作。
     * <p>
     * 丢弃和重试是一个重要的规定，
     * 否则 {@code T} 可能会安装一个灾难性的陈旧值。 例如：
     * <ul>
     * <li>{@code T} 调用 {@code CV.get(C)} 并看到状态 {@code 2N}
     * <li>{@code T} 快速计算一个时间依赖值 {@code V0} 并准备好安装它
     * <li>{@code T} 遭遇不幸的分页或调度事件，并长时间休眠
     * <li>...同时，{@code T2} 也调用 {@code CV.get(C)} 并看到状态 {@code 2N}
     * <li>{@code T2} 快速计算一个类似的时间依赖值 {@code V1} 并安装到 {@code CV.get(C)}
     * <li>{@code T2}（或第三个线程）然后调用 {@code CV.remove(C)}，撤销 {@code T2} 的工作
     * <li> 之前的 {@code T2} 操作重复几次
     * <li> 同时，相关的计算值随时间变化：{@code V1}, {@code V2}, ...
     * <li>...同时，{@code T} 醒来并尝试安装 {@code V0}；<em>这必须失败</em>
     * </ul>
     * 我们可以假设在上述场景中 {@code CV.computeValue} 使用锁来正确观察计算 {@code V1} 等时的时间依赖状态。
     * 这并不能消除陈旧值的威胁，因为 {@code computeValue} 在 {@code T} 中返回和安装新值之间有一段时间窗口。 在此期间不可能进行用户同步。
     *
     * @param type 需要移除类值的类型
     * @throws NullPointerException 如果参数为 null
     */
    public void remove(Class<?> type) {
        ClassValueMap map = getMap(type);
        map.removeEntry(this);
    }

    // 可能的功能为 JSR 292 MR 1
    /*public*/ void put(Class<?> type, T value) {
        ClassValueMap map = getMap(type);
        map.changeEntry(this, value);
    }

    /// --------
    /// 实现...
    /// --------

    /** 返回缓存，如果存在，否则返回一个虚拟的空缓存。 */
    private static Entry<?>[] getCacheCarefully(Class<?> type) {
        // 竞争 type.classValueMap{.cacheArray} : null => new Entry[X] <=> new Entry[Y]
        ClassValueMap map = type.classValueMap;
        if (map == null)  return EMPTY_CACHE;
        Entry<?>[] cache = map.getCache();
        return cache;
        // 不变性： 返回的值是安全的，可以解引用并检查 Entry
    }

    /** 初始的、单元素的、空缓存，由所有 Class 实例使用。 必须永不填充。 */
    private static final Entry<?>[] EMPTY_CACHE = { null };

    /**
     * ClassValue.get 的慢速尾部，以在缓存中的附近位置重试，
     * 或获取慢速锁并检查哈希表。
     * 仅在第一次探测为空或发生冲突时调用。
     * 这是一个单独的方法，以便编译器可以独立处理它。
     */
    private T getFromBackup(Entry<?>[] cache, Class<?> type) {
        Entry<T> e = probeBackupLocations(cache, this);
        if (e != null)
            return e.value();
        return getFromHashMap(type);
    }

    // 抑制 (T) 铸造的警告，这是一个无操作。
    @SuppressWarnings("unchecked")
    Entry<T> castEntry(Entry<?> e) { return (Entry<T>) e; }

    /** 当 get 的快速路径失败且缓存重新探测也失败时调用。 */
    private T getFromHashMap(Class<?> type) {
        // 最后一个安全的恢复是回退到底层的 classValueMap。
        ClassValueMap map = getMap(type);
        for (;;) {
            Entry<T> e = map.startEntry(this);
            if (!e.isPromise())
                return e.value();
            try {
                // 尝试为承诺的版本创建一个实际的条目。
                e = makeEntry(e.version(), computeValue(type));
            } finally {
                // 无论 computeValue 抛出异常还是正常返回，
                // 确保移除空条目。
                e = map.finishEntry(this, e);
            }
            if (e != null)
                return e.value();
            // 否则重试，以防竞争线程调用了 remove（因此 e == null）
        }
    }

    /** 检查 e 是否为非 null，匹配此 ClassValue，并且是活动的。 */
    boolean match(Entry<?> e) {
        // 竞争 e.version : null (空白) => 唯一的 Version 令牌 => null (GC-ed 版本)
        // 非竞争 this.version : v1 => v2 => ...（从 volatile 读取的更新是忠实的）
        return (e != null && e.get() == this.version);
        // 不变性： 版本匹配没有误报。 对于误否定，null 是可以的。
        // 不变性： 如果版本匹配，则 e.value 是可读的（在 Entry.<init> 中最终设置）
    }

    /** 访问 Class.classValueMap.cacheArray 的内部哈希码。 */
    final int hashCodeForCache = nextHashCode.getAndAdd(HASH_INCREMENT) & HASH_MASK;

    /** 用于 hashCodeForCache 的值流。 参见 ThreadLocal 中的类似结构。 */
    private static final AtomicInteger nextHashCode = new AtomicInteger();

    /** 适用于二的幂表。 参见 ThreadLocal 中的类似结构。 */
    private static final int HASH_INCREMENT = 0x61c88647;

    /** 掩码哈希码使其为正但不太大，以防止溢出。 */
    static final int HASH_MASK = (-1 >>> 2);

    /**
     * 从 ClassValueMap 检索此对象的私钥。
     */
    static class Identity {
    }
    /**
     * 此 ClassValue 的身份，表示为一个不透明的对象。
     * 主对象 {@code ClassValue.this} 是不正确的，因为
     * 子类可能覆盖 {@code ClassValue.equals}，这可能会混淆 ClassValueMap 中的键。
     */
    final Identity identity = new Identity();

    /**
     * 从缓存中检索此类值的当前版本。
     * 任何数量的 computeValue 调用都可以与一个版本关联。
     * 但当执行 remove（在任何类型上）时，版本会改变。
     * 版本的改变会使受影响的 ClassValue 的所有缓存条目失效，
     * 通过将它们标记为陈旧。 陈旧的缓存条目不会强制再次调用
     * computeValue，但需要同步访问支持映射。
     * <p>
     * 所有用户可见的 ClassValue 状态更改都在
     * ClassValueMap 的同步方法中的锁内进行。
     * 读取者（ClassValue.get）在 this.version 被提升到新令牌时
     * 会收到此类状态更改的通知。
     * 此变量必须是 volatile 的，以便未同步的读取者
     * 能够立即收到通知。
     * <p>
     * 如果 version 不是 volatile 的，一个线程 T1 可能会持续持有
     * 一个陈旧值 this.value == V1，而另一个线程 T2 在锁下
     * 推进到 this.value == V2。这通常是没有害处的，
     * 但如果 T1 和 T2 通过其他渠道因果交互，使得
     * T1 的进一步操作在 JMM 中必须发生在
     * V2 事件之后，那么 T1 观察到 V1 将是一个错误。
     * <p>
     * 使 this.version 为 volatile 的实际效果是它不能
     * 被优化的 JIT 提升出循环或以其他方式缓存。
     * 一些机器可能还需要在 this.version 之前执行屏障指令。
     */
    private volatile Version<T> version = new Version<>(this);
    Version<T> version() { return version; }
    void bumpVersion() { version = new Version<>(this); }
    static class Version<T> {
        private final ClassValue<T> classValue;
        private final Entry<T> promise = new Entry<>(this);
        Version(ClassValue<T> classValue) { this.classValue = classValue; }
        ClassValue<T> classValue() { return classValue; }
        Entry<T> promise() { return promise; }
        boolean isLive() { return classValue.version() == this; }
    }

    /** 一个值与类通过 ClassValue 绑定。
     *  状态是：<ul>
     *  <li> 如果 value == Entry.this，则为 promise
     *  <li> 否则，如果 version == null，则为 dead
     *  <li> 否则，如果 version != classValue.version，则为 stale
     *  <li> 否则为 live </ul>
     *  承诺永远不会放入缓存中；它们只存在于
     *  支持映射中，当 computeValue 调用正在进行时。
     *  一旦条目变陈旧，可以随时将其重置为 dead 状态。
     */
    static class Entry<T> extends WeakReference<Version<T>> {
        final Object value;  // 通常类型为 T，但有时为 (Entry)this
        Entry(Version<T> version, T value) {
            super(version);
            this.value = value;  // 对于常规条目，value 类型为 T
        }
        private void assertNotPromise() { assert(!isPromise()); }
        /** 用于创建承诺。 */
        Entry(Version<T> version) {
            super(version);
            this.value = this;  // 对于承诺，value 不是类型 T，而是 Entry!
        }
        /** 获取值。此条目不能是承诺。 */
        @SuppressWarnings("unchecked")  // 如果 !isPromise，类型为 T
        T value() { assertNotPromise(); return (T) value; }
        boolean isPromise() { return value == this; }
        Version<T> version() { return get(); }
        ClassValue<T> classValueOrNull() {
            Version<T> v = version();
            return (v == null) ? null : v.classValue();
        }
        boolean isLive() {
            Version<T> v = version();
            if (v == null)  return false;
            if (v.isLive())  return true;
            clear();
            return false;
        }
        Entry<T> refreshVersion(Version<T> v2) {
            assertNotPromise();
            @SuppressWarnings("unchecked")  // 如果 !isPromise，类型为 T
            Entry<T> e2 = new Entry<>(v2, (T) value);
            clear();
            // value = null -- 调用者必须丢弃
            return e2;
        }
        static final Entry<?> DEAD_ENTRY = new Entry<>(null, null);
    }


                /** 返回与此类型关联的后端映射。 */
    private static ClassValueMap getMap(Class<?> type) {
        // 类型.classValueMap：null (空白) => 唯一的 ClassValueMap
        // 如果观察到 null，则会创建一个映射（惰性地、同步地、唯一地）
        // 对该映射的所有后续访问都是同步的
        ClassValueMap map = type.classValueMap;
        if (map != null)  return map;
        return initializeMap(type);
    }

    private static final Object CRITICAL_SECTION = new Object();
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static ClassValueMap initializeMap(Class<?> type) {
        ClassValueMap map;
        synchronized (CRITICAL_SECTION) {  // 私有对象以避免死锁
            // 每种类型大约发生一次
            if ((map = type.classValueMap) == null) {
                map = new ClassValueMap(type);
                // 在构造之后和发布之前放置一个存储屏障，以模拟
                // ClassValueMap 包含 final 字段。这确保它可以
                // 在非易失性字段 Class.classValueMap 中安全发布，
                // 因为对 ClassValueMap 字段的存储不会被重新排序
                // 到对字段 type.classValueMap 的存储之后
                UNSAFE.storeFence();

                type.classValueMap = map;
            }
        }
        return map;
    }

    static <T> Entry<T> makeEntry(Version<T> explicitVersion, T value) {
        // 注意 explicitVersion 可能与 this.version 不同。
        return new Entry<>(explicitVersion, value);

        // 一旦 Entry 被放入缓存，值将通过数据竞争（根据 Java 内存模型定义）可访问。
        // 如果值对象本身可以被多个线程安全地读取，这种竞争是无害的。这是用户的职责。
        //
        // entry 和 version 字段本身可以通过竞争安全地读取，因为它们是 final 的或具有受控状态。
        // 如果从 entry 到 version 的指针仍然为 null，
        // 或者 version 立即失效并被置为 null，
        // 读取者将走慢路径并在锁下重试。
    }

    // 以下类也可以是顶级的且非公共的：

    /** 为所有 ClassValues 提供后端映射，相对于给定的类型。
     *  为每对 (ClassValue cv, Class type) 提供完全序列化的“真实状态”。
     *  还管理一个未序列化的快速路径缓存。
     */
    static class ClassValueMap extends WeakHashMap<ClassValue.Identity, Entry<?>> {
        private final Class<?> type;
        private Entry<?>[] cacheArray;
        private int cacheLoad, cacheLoadLimit;

        /** 每种类型首次使用任何 ClassValue 时初始分配的条目数量。
         *  将其设置为比 Class 和 ClassValueMap 对象本身小得多是没有意义的。
         *  必须是 2 的幂。
         */
        private static final int INITIAL_ENTRIES = 32;

        /** 为 ClassValues 构建后端映射，相对于给定的类型。
         *  同时，创建一个空的缓存数组并安装在类上。
         */
        ClassValueMap(Class<?> type) {
            this.type = type;
            sizeCache(INITIAL_ENTRIES);
        }

        Entry<?>[] getCache() { return cacheArray; }

        /** 开始查询。如果没有值，则存储一个承诺（占位符）。 */
        synchronized
        <T> Entry<T> startEntry(ClassValue<T> classValue) {
            @SuppressWarnings("unchecked")  // 一个映射包含所有值类型 <T> 的条目
            Entry<T> e = (Entry<T>) get(classValue.identity);
            Version<T> v = classValue.version();
            if (e == null) {
                e = v.promise();
                // 承诺的存在意味着 v 的值正在等待中。
                // 最终，finishEntry 将覆盖承诺。
                put(classValue.identity, e);
                // 注意承诺永远不会进入缓存！
                return e;
            } else if (e.isPromise()) {
                // 其他人已经提出了相同的问题。
                // 让比赛开始吧！
                if (e.version() != v) {
                    e = v.promise();
                    put(classValue.identity, e);
                }
                return e;
            } else {
                // 这里已经有一个完成的条目；报告它
                if (e.version() != v) {
                    // 这里有一个陈旧但有效的条目；使其再次新鲜。
                    // 一旦条目在哈希表中，我们就不关心它的版本是什么。
                    e = e.refreshVersion(v);
                    put(classValue.identity, e);
                }
                // 添加到缓存中，以便下次启用快速路径。
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            }
        }

        /** 完成查询。覆盖匹配的占位符。丢弃陈旧的传入值。 */
        synchronized
        <T> Entry<T> finishEntry(ClassValue<T> classValue, Entry<T> e) {
            @SuppressWarnings("unchecked")  // 一个映射包含所有值类型 <T> 的条目
            Entry<T> e0 = (Entry<T>) get(classValue.identity);
            if (e == e0) {
                // 我们可能在异常处理期间到达这里，从 computeValue 解开。
                assert(e.isPromise());
                remove(classValue.identity);
                return null;
            } else if (e0 != null && e0.isPromise() && e0.version() == e.version()) {
                // 如果 e0 匹配预期的条目，则在前一个 startEntry 和现在之间没有调用 remove。
                // 所以现在覆盖 e0。
                Version<T> v = classValue.version();
                if (e.version() != v)
                    e = e.refreshVersion(v);
                put(classValue.identity, e);
                // 添加到缓存中，以便下次启用快速路径。
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            } else {
                // 某种不匹配；调用者必须重试。
                return null;
            }
        }

        /** 移除一个条目。 */
        synchronized
        void removeEntry(ClassValue<?> classValue) {
            Entry<?> e = remove(classValue.identity);
            if (e == null) {
                // 未初始化，且没有待处理的 computeValue 调用。没有变化。
            } else if (e.isPromise()) {
                // 状态未初始化，但有一个待处理的 finishEntry 调用。
                // 由于在这种状态下 remove 是无操作的，因此通过将其放回映射中来保持承诺。
                put(classValue.identity, e);
            } else {
                // 处于初始化状态。向前推进，并去初始化。
                classValue.bumpVersion();
                // 使该条目的所有缓存元素失效。
                removeStaleEntries(classValue);
            }
        }

        /** 更改条目的值。 */
        synchronized
        <T> void changeEntry(ClassValue<T> classValue, T value) {
            @SuppressWarnings("unchecked")  // 一个映射包含所有值类型 <T> 的条目
            Entry<T> e0 = (Entry<T>) get(classValue.identity);
            Version<T> version = classValue.version();
            if (e0 != null) {
                if (e0.version() == version && e0.value() == value)
                    // 值没有变化 => 不需要版本变化
                    return;
                classValue.bumpVersion();
                removeStaleEntries(classValue);
            }
            Entry<T> e = makeEntry(version, value);
            put(classValue.identity, e);
            // 添加到缓存中，以便下次启用快速路径。
            checkCacheLoad();
            addToCache(classValue, e);
        }

        /// --------
        /// 缓存管理。
        /// --------

        // 静态方法不需要同步。

        /** 加载给定（散列）位置的缓存条目。 */
        static Entry<?> loadFromCache(Entry<?>[] cache, int i) {
            // 非竞争缓存.length : 常量
            // 竞争缓存[i & (mask)] : null <=> Entry
            return cache[i & (cache.length-1)];
            // 不变量：返回的值为 null 或构造良好（准备好匹配）
        }

        /** 在缓存中查找给定 ClassValue 的主位置。 */
        static <T> Entry<T> probeHomeLocation(Entry<?>[] cache, ClassValue<T> classValue) {
            return classValue.castEntry(loadFromCache(cache, classValue.hashCodeForCache));
        }

        /** 假设第一次探测是冲突的，在附近位置重试。 */
        static <T> Entry<T> probeBackupLocations(Entry<?>[] cache, ClassValue<T> classValue) {
            if (PROBE_LIMIT <= 0)  return null;
            // 小心地在一系列槽位中探测缓存。
            int mask = (cache.length-1);
            int home = (classValue.hashCodeForCache & mask);
            Entry<?> e2 = cache[home];  // 如果找到真正的家伙，这是受害者
            if (e2 == null) {
                return null;   // 如果没有人在家，无需在附近搜索
            }
            // 假设 !classValue.match(e2)，但不要断言，因为有竞争
            int pos2 = -1;
            for (int i = home + 1; i < home + PROBE_LIMIT; i++) {
                Entry<?> e = cache[i & mask];
                if (e == null) {
                    break;   // 只在非空运行内搜索
                }
                if (classValue.match(e)) {
                    // 将冲突的条目 e2（来自 cache[home]）重新定位到第一个空槽
                    cache[home] = e;
                    if (pos2 >= 0) {
                        cache[i & mask] = Entry.DEAD_ENTRY;
                    } else {
                        pos2 = i;
                    }
                    cache[pos2 & mask] = ((entryDislocation(cache, pos2, e2) < PROBE_LIMIT)
                                          ? e2                  // 如果 e2 适合则放在这里
                                          : Entry.DEAD_ENTRY);
                    return classValue.castEntry(e);
                }
                // 记住第一个空槽，如果有：
                if (!e.isLive() && pos2 < 0)  pos2 = i;
            }
            return null;
        }

        /** e 偏移了多少？ */
        private static int entryDislocation(Entry<?>[] cache, int pos, Entry<?> e) {
            ClassValue<?> cv = e.classValueOrNull();
            if (cv == null)  return 0;  // 条目不是活动的！
            int mask = (cache.length-1);
            return (pos - cv.hashCodeForCache) & mask;
        }

        /// --------
        /// 以下所有函数都是私有的，并假设同步访问。
        /// --------

        private void sizeCache(int length) {
            assert((length & (length-1)) == 0);  // 必须是 2 的幂
            cacheLoad = 0;
            cacheLoadLimit = (int) ((double) length * CACHE_LOAD_LIMIT / 100);
            cacheArray = new Entry<?>[length];
        }

        /** 尽可能确保缓存负载保持在其限制以下。 */
        private void checkCacheLoad() {
            if (cacheLoad >= cacheLoadLimit) {
                reduceCacheLoad();
            }
        }
        private void reduceCacheLoad() {
            removeStaleEntries();
            if (cacheLoad < cacheLoadLimit)
                return;  // 胜利
            Entry<?>[] oldCache = getCache();
            if (oldCache.length > HASH_MASK)
                return;  // 失败
            sizeCache(oldCache.length * 2);
            for (Entry<?> e : oldCache) {
                if (e != null && e.isLive()) {
                    addToCache(e);
                }
            }
        }

        /** 移除给定范围内的陈旧条目。
         *  应在 Map 锁下执行。
         */
        private void removeStaleEntries(Entry<?>[] cache, int begin, int count) {
            if (PROBE_LIMIT <= 0)  return;
            int mask = (cache.length-1);
            int removed = 0;
            for (int i = begin; i < begin + count; i++) {
                Entry<?> e = cache[i & mask];
                if (e == null || e.isLive())
                    continue;  // 跳过 null 和活动条目
                Entry<?> replacement = null;
                if (PROBE_LIMIT > 1) {
                    // 避免打断非空运行
                    replacement = findReplacement(cache, i);
                }
                cache[i & mask] = replacement;
                if (replacement == null)  removed += 1;
            }
            cacheLoad = Math.max(0, cacheLoad - removed);
        }

        /** 清除缓存槽位可能会断开后续条目
         *  与非空运行头部的连接，这将允许它们
         *  通过重新探测找到。在 cache[begin] 之后找到一个条目
         *  以填补空缺，或返回 null 如果不需要。
         */
        private Entry<?> findReplacement(Entry<?>[] cache, int home1) {
            Entry<?> replacement = null;
            int haveReplacement = -1, replacementPos = 0;
            int mask = (cache.length-1);
            for (int i2 = home1 + 1; i2 < home1 + PROBE_LIMIT; i2++) {
                Entry<?> e2 = cache[i2 & mask];
                if (e2 == null)  break;  // 非空运行结束。
                if (!e2.isLive())  continue;  // 无论如何都会失效。
                int dis2 = entryDislocation(cache, i2, e2);
                if (dis2 == 0)  continue;  // e2 已经最佳放置
                int home2 = i2 - dis2;
                if (home2 <= home1) {
                    // e2 可以替换 cache[home1] 中的条目
                    if (home2 == home1) {
                        // 将 e2 放在它应该在的位置。
                        haveReplacement = 1;
                        replacementPos = i2;
                        replacement = e2;
                    } else if (haveReplacement <= 0) {
                        haveReplacement = 0;
                        replacementPos = i2;
                        replacement = e2;
                    }
                    // 继续前进，以便优先考虑更大的偏移。
                }
            }
            if (haveReplacement >= 0) {
                if (cache[(replacementPos+1) & mask] != null) {
                    // 保守一点，以避免打断非空运行。
                    cache[replacementPos & mask] = (Entry<?>) Entry.DEAD_ENTRY;
                } else {
                    cache[replacementPos & mask] = null;
                    cacheLoad -= 1;
                }
            }
            return replacement;
        }

        /** 移除 classValue 附近的陈旧条目。 */
        private void removeStaleEntries(ClassValue<?> classValue) {
            removeStaleEntries(getCache(), classValue.hashCodeForCache, PROBE_LIMIT);
        }


                    /** 移除所有过期的条目。 */
        private void removeStaleEntries() {
            Entry<?>[] cache = getCache();
            removeStaleEntries(cache, 0, cache.length + PROBE_LIMIT - 1);
        }

        /** 将给定的条目添加到缓存中，除非它已过期。 */
        private <T> void addToCache(Entry<T> e) {
            ClassValue<T> classValue = e.classValueOrNull();
            if (classValue != null)
                addToCache(classValue, e);
        }

        /** 将给定的条目添加到缓存中。 */
        private <T> void addToCache(ClassValue<T> classValue, Entry<T> e) {
            if (PROBE_LIMIT <= 0)  return;  // 不填充缓存
            // 将 e 添加到缓存中。
            Entry<?>[] cache = getCache();
            int mask = (cache.length-1);
            int home = classValue.hashCodeForCache & mask;
            Entry<?> e2 = placeInCache(cache, home, e, false);
            if (e2 == null)  return;  // 完成
            if (PROBE_LIMIT > 1) {
                // 尝试将 e2 移动到其探测范围内的其他位置
                int dis2 = entryDislocation(cache, home, e2);
                int home2 = home - dis2;
                for (int i2 = home2; i2 < home2 + PROBE_LIMIT; i2++) {
                    if (placeInCache(cache, i2 & mask, e2, true) == null) {
                        return;
                    }
                }
            }
            // 注意：此时，e2 从缓存中移除。
        }

        /** 存储给定的条目。更新 cacheLoad，并返回任何活的受害者。
         *  'Gently' 意味着返回自身而不是移位一个活的受害者。
         */
        private Entry<?> placeInCache(Entry<?>[] cache, int pos, Entry<?> e, boolean gently) {
            Entry<?> e2 = overwrittenEntry(cache[pos]);
            if (gently && e2 != null) {
                // 不要覆盖活的条目
                return e;
            } else {
                cache[pos] = e;
                return e2;
            }
        }

        /** 注意即将被覆盖的条目。
         *  如果它不是活的，安静地将其替换为 null。
         *  如果它是一个实际的 null，增加 cacheLoad，
         *  因为调用者将要存储一些东西
         *  在其位置。
         */
        private <T> Entry<T> overwrittenEntry(Entry<T> e2) {
            if (e2 == null)  cacheLoad += 1;
            else if (e2.isLive())  return e2;
            return null;
        }

        /** 缓存重新调整大小之前的加载百分比。 */
        private static final int CACHE_LOAD_LIMIT = 67;  // 0..100
        /** 尝试的最大探测次数。 */
        private static final int PROBE_LIMIT      =  6;       // 1..
        // 注意：将 PROBE_LIMIT 设置为 0 以禁用所有快速路径。
    }
}
