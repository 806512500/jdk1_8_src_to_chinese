/*
 * Copyright (c) 1996, 2002, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.datatransfer;

/**
 * 定义了将提供数据到剪贴板的类的接口。如果将此接口的实例作为参数传递给
 * 剪贴板的 {@link java.awt.datatransfer.Clipboard#setContents} 方法，并且此方法成功返回，
 * 则该实例将成为剪贴板内容的所有者（剪贴板所有者）。
 * 该实例将保持剪贴板所有者的身份，直到其他应用程序或此应用程序内的其他对象声明对剪贴板的所有权。
 *
 * @see java.awt.datatransfer.Clipboard
 *
 * @author      Amy Fowler
 */

public interface ClipboardOwner {

    /**
     * 通知此对象它不再是剪贴板所有者。
     * 当其他应用程序或此应用程序内的其他对象声明对剪贴板的所有权时，将调用此方法。
     *
     * @param clipboard 不再拥有的剪贴板
     * @param contents 此所有者放置在剪贴板上的内容
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents);

}
