/*
 * Copyright (c) 1996, 2005, Oracle and/or its affiliates. All rights reserved.
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

package java.io;


/**
 * 用于读取过滤字符流的抽象类。
 * 抽象类 <code>FilterReader</code> 本身
 * 提供了将所有请求传递给
 * 包含的流的默认方法。 <code>FilterReader</code> 的子类
 * 应该覆盖其中的一些方法，并且可以提供
 * 额外的方法和字段。
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public abstract class FilterReader extends Reader {

    /**
     * 包含的字符输入流。
     */
    protected Reader in;

    /**
     * 创建一个新的过滤读取器。
     *
     * @param in  提供底层流的 Reader 对象。
     * @throws NullPointerException 如果 <code>in</code> 为 <code>null</code>
     */
    protected FilterReader(Reader in) {
        super(in);
        this.in = in;
    }

    /**
     * 读取单个字符。
     *
     * @exception  IOException  如果发生 I/O 错误
     */
    public int read() throws IOException {
        return in.read();
    }

    /**
     * 读取字符到数组的一部分。
     *
     * @exception  IOException  如果发生 I/O 错误
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        return in.read(cbuf, off, len);
    }

    /**
     * 跳过字符。
     *
     * @exception  IOException  如果发生 I/O 错误
     */
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    /**
     * 告诉此流是否准备好被读取。
     *
     * @exception  IOException  如果发生 I/O 错误
     */
    public boolean ready() throws IOException {
        return in.ready();
    }

    /**
     * 告诉此流是否支持 mark() 操作。
     */
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * 标记流中的当前位置。
     *
     * @exception  IOException  如果发生 I/O 错误
     */
    public void mark(int readAheadLimit) throws IOException {
        in.mark(readAheadLimit);
    }

    /**
     * 重置流。
     *
     * @exception  IOException  如果发生 I/O 错误
     */
    public void reset() throws IOException {
        in.reset();
    }

    public void close() throws IOException {
        in.close();
    }

}
