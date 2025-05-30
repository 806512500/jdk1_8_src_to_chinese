/*
 * Copyright (c) 1999, 2022, Oracle and/or its affiliates. All rights reserved.
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

package java.security.interfaces;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;

/**
 * PKCS#1 v2.2 标准中的公钥或私钥接口，例如 RSA 或 RSASSA-PSS 算法。
 *
 * @author Jan Luehe
 *
 * @see RSAPublicKey
 * @see RSAPrivateKey
 *
 * @since 1.3
 */

public interface RSAKey {

    /**
     * 返回模数。
     *
     * @return 模数
     */
    public BigInteger getModulus();

    /**
     * 返回与此密钥关联的参数。
     * 参数是可选的，可以在密钥对生成期间显式指定或隐式创建。
     *
     * @apiNote 此方法在 Java SE 8 Maintenance Release 3 中定义。
     * @implSpec
     * 默认实现返回 {@code null}。
     *
     * @return 关联的参数，可能为 null
     * @since 8
     */
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
