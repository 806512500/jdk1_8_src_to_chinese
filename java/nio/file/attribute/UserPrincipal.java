/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import java.security.Principal;

/**
 * 一个表示用于确定文件系统中对象访问权限的身份的 {@code Principal}。
 *
 * <p> 在许多平台和文件系统中，实体需要适当的访问权限或权限才能访问文件系统中的对象。访问权限通常通过检查实体的身份来执行。例如，在使用访问控制列表 (ACL) 来强制执行特权分离的实现中，文件系统中的文件可能有一个关联的 ACL，该 ACL 确定了 ACL 中指定的身份的访问权限。
 *
 * <p> {@code UserPrincipal} 对象是身份的抽象表示。它有一个 {@link #getName() 名称}，通常是它所代表的用户名或帐户名。可以使用 {@link UserPrincipalLookupService} 获取用户主体对象，或者由提供身份相关属性访问的 {@link FileAttributeView} 实现返回。例如，{@link AclFileAttributeView} 和 {@link PosixFileAttributeView} 提供了对文件的 {@link PosixFileAttributes#owner 所有者} 的访问。
 *
 * @since 1.7
 */

public interface UserPrincipal extends Principal { }
