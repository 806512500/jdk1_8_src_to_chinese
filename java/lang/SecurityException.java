/*
 * Copyright (c) 1995, 2003, Oracle and/or its affiliates. All rights reserved.
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

/**
 * 由安全管理者抛出，表示安全违规。
 *
 * @author  未署名
 * @see     java.lang.SecurityManager
 * @since   JDK1.0
 */
public class SecurityException extends RuntimeException {

    private static final long serialVersionUID = 6878364983674394167L;

    /**
     * 构造一个没有详细消息的 <code>SecurityException</code>。
     */
    public SecurityException() {
        super();
    }

    /**
     * 使用指定的详细消息构造一个 <code>SecurityException</code>。
     *
     * @param   s   详细消息。
     */
    public SecurityException(String s) {
        super(s);
    }

    /**
     * 使用指定的详细消息和原因创建一个 <code>SecurityException</code>。
     *
     * @param message 详细消息（通过 {@link #getMessage()} 方法稍后检索）。
     * @param cause 原因（通过 {@link #getCause()} 方法稍后检索）。允许为 <tt>null</tt>，
     *        表示原因不存在或未知。
     * @since 1.5
     */
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的原因创建一个 <code>SecurityException</code>，详细消息为 <tt>(cause==null ? null : cause.toString())</tt>
     * （通常包含 <tt>cause</tt> 的类和详细消息）。
     *
     * @param cause 原因（通过 {@link #getCause()} 方法稍后检索）。允许为 <tt>null</tt>，
     *        表示原因不存在或未知。
     * @since 1.5
     */
    public SecurityException(Throwable cause) {
        super(cause);
    }
}
