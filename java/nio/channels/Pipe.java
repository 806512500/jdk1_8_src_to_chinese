/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.*;


/**
 * 一对通道实现单向管道。
 *
 * <p> 管道由一对通道组成：一个可写的 {@link
 * Pipe.SinkChannel 沉降} 通道和一个可读的 {@link Pipe.SourceChannel 源}
 * 通道。一旦有一些字节写入沉降通道，它们就可以从源通道中按写入的顺序读取。
 *
 * <p> 线程写入管道的字节是否会阻塞，直到另一个线程从管道中读取这些字节或之前写入的字节，这是系统依赖的，因此未指定。许多管道实现会在沉降和源通道之间缓冲一定数量的字节，但不应假设这种缓冲。 </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 专家组
 * @since 1.4
 */

public abstract class Pipe {

    /**
     * 表示 {@link Pipe} 可读端的通道。
     *
     * @since 1.4
     */
    public static abstract class SourceChannel
        extends AbstractSelectableChannel
        implements ReadableByteChannel, ScatteringByteChannel
    {
        /**
         * 构造此类的新实例。
         *
         * @param  provider
         *         选择器提供者
         */
        protected SourceChannel(SelectorProvider provider) {
            super(provider);
        }

        /**
         * 返回一个操作集，标识此通道支持的操作。
         *
         * <p> 管道源通道仅支持读取，因此此方法返回 {@link SelectionKey#OP_READ}。 </p>
         *
         * @return  有效操作集
         */
        public final int validOps() {
            return SelectionKey.OP_READ;
        }

    }

    /**
     * 表示 {@link Pipe} 可写端的通道。
     *
     * @since 1.4
     */
    public static abstract class SinkChannel
        extends AbstractSelectableChannel
        implements WritableByteChannel, GatheringByteChannel
    {
        /**
         * 初始化此类的新实例。
         *
         * @param  provider
         *         选择器提供者
         */
        protected SinkChannel(SelectorProvider provider) {
            super(provider);
        }

        /**
         * 返回一个操作集，标识此通道支持的操作。
         *
         * <p> 管道沉降通道仅支持写入，因此此方法返回
         * {@link SelectionKey#OP_WRITE}。 </p>
         *
         * @return  有效操作集
         */
        public final int validOps() {
            return SelectionKey.OP_WRITE;
        }

    }

    /**
     * 初始化此类的新实例。
     */
    protected Pipe() { }

    /**
     * 返回此管道的源通道。
     *
     * @return  此管道的源通道
     */
    public abstract SourceChannel source();

    /**
     * 返回此管道的沉降通道。
     *
     * @return  此管道的沉降通道
     */
    public abstract SinkChannel sink();

    /**
     * 打开一个管道。
     *
     * <p> 通过调用系统默认 {@link java.nio.channels.spi.SelectorProvider}
     * 对象的 {@link
     * java.nio.channels.spi.SelectorProvider#openPipe openPipe} 方法创建新的管道。 </p>
     *
     * @return  一个新的管道
     *
     * @throws  IOException
     *          如果发生 I/O 错误
     */
    public static Pipe open() throws IOException {
        return SelectorProvider.provider().openPipe();
    }

}
