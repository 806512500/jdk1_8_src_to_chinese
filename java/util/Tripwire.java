/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import sun.util.logging.PlatformLogger;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 用于检测在 {@code java.util} 类中无意使用装箱的实用类。检测是否开启基于系统属性
 * {@code org.openjdk.java.util.stream.tripwire} 是否被 {@link Boolean#getBoolean(String)} 认为是 {@code true}。
 * 这通常应在生产环境中关闭。
 *
 * @apiNote
 * 典型用法是在装箱代码中执行以下操作：
 * <pre>{@code
 *     if (Tripwire.ENABLED)
 *         Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfInt.nextInt()");
 * }</pre>
 *
 * @since 1.8
 */
final class Tripwire {
    private static final String TRIPWIRE_PROPERTY = "org.openjdk.java.util.stream.tripwire";

    /** 是否应启用调试检查？ */
    static final boolean ENABLED = AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> Boolean.getBoolean(TRIPWIRE_PROPERTY));

    private Tripwire() { }

    /**
     * 使用 {@code PlatformLogger.getLogger(className)} 生成日志警告，
     * 使用提供的消息。{@code trippingClass} 的类名将作为消息的第一个参数。
     *
     * @param trippingClass 生成消息的类名
     * @param msg 由 {@link PlatformLogger} 期望的消息格式字符串
     */
    static void trip(Class<?> trippingClass, String msg) {
        PlatformLogger.getLogger(trippingClass.getName()).warning(msg, trippingClass.getName());
    }
}
