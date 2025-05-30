/*
 * Copyright (c) 1996, 1999, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * 一个输出流，同时维护正在写入的数据的校验和。该校验和可用于验证输出数据的完整性。
 *
 * @see         Checksum
 * @author      David Connelly
 */
public
class CheckedOutputStream extends FilterOutputStream {
    private Checksum cksum;

    /**
     * 创建一个具有指定 Checksum 的输出流。
     * @param out 输出流
     * @param cksum 校验和
     */
    public CheckedOutputStream(OutputStream out, Checksum cksum) {
        super(out);
        this.cksum = cksum;
    }

    /**
     * 写入一个字节。将阻塞直到字节实际写入。
     * @param b 要写入的字节
     * @exception IOException 如果发生 I/O 错误
     */
    public void write(int b) throws IOException {
        out.write(b);
        cksum.update(b);
    }

    /**
     * 写入一个字节数组。将阻塞直到字节实际写入。
     * @param b 要写入的数据
     * @param off 数据的起始偏移量
     * @param len 要写入的字节数
     * @exception IOException 如果发生 I/O 错误
     */
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        cksum.update(b, off, len);
    }

    /**
     * 返回此输出流的 Checksum。
     * @return 校验和
     */
    public Checksum getChecksum() {
        return cksum;
    }
}
