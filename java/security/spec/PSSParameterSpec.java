/*
 * Copyright (c) 2001, 2022, Oracle and/or its affiliates. All rights reserved.
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

package java.security.spec;

import java.util.Objects;

/**
 * 该类指定了 RSASSA-PSS 签名方案的参数规范，
 * 如在 <a href="https://tools.ietf.org/rfc/rfc8017.txt">PKCS#1 v2.2</a> 标准中定义的。
 *
 * <p>其在 PKCS#1 标准中的 ASN.1 定义如下：
 * <pre>
 * RSASSA-PSS-params ::= SEQUENCE {
 *   hashAlgorithm      [0] HashAlgorithm      DEFAULT sha1,
 *   maskGenAlgorithm   [1] MaskGenAlgorithm   DEFAULT mgf1SHA1,
 *   saltLength         [2] INTEGER            DEFAULT 20,
 *   trailerField       [3] TrailerField       DEFAULT trailerFieldBC(1)
 * }
 * </pre>
 * 其中
 * <pre>
 * HashAlgorithm ::= AlgorithmIdentifier {
 *   {OAEP-PSSDigestAlgorithms}
 * }
 * MaskGenAlgorithm ::= AlgorithmIdentifier { {PKCS1MGFAlgorithms} }
 * TrailerField ::= INTEGER { trailerFieldBC(1) }
 *
 * OAEP-PSSDigestAlgorithms    ALGORITHM-IDENTIFIER ::= {
 *   { OID id-sha1       PARAMETERS NULL }|
 *   { OID id-sha224     PARAMETERS NULL }|
 *   { OID id-sha256     PARAMETERS NULL }|
 *   { OID id-sha384     PARAMETERS NULL }|
 *   { OID id-sha512     PARAMETERS NULL }|
 *   { OID id-sha512-224 PARAMETERS NULL }|
 *   { OID id-sha512-256 PARAMETERS NULL },
 *   ...  -- 允许未来扩展 --
 * }
 * PKCS1MGFAlgorithms    ALGORITHM-IDENTIFIER ::= {
 *   { OID id-mgf1 PARAMETERS HashAlgorithm },
 *   ...  -- 允许未来扩展 --
 * }
 * </pre>
 * <p>注意：PSSParameterSpec.DEFAULT 使用以下默认值：
 *     消息摘要  -- "SHA-1"
 *     掩码生成函数 (mgf) -- "MGF1"
 *     mgf 的参数 -- MGF1ParameterSpec.SHA1
 *     盐长度   -- 20
 *     尾字段 -- 1
 *
 * @see MGF1ParameterSpec
 * @see AlgorithmParameterSpec
 * @see java.security.Signature
 *
 * @author Valerie Peng
 *
 *
 * @since 1.4
 */

public class PSSParameterSpec implements AlgorithmParameterSpec {

    private final String mdName;

    private final String mgfName;

    private final AlgorithmParameterSpec mgfSpec;

    private final int saltLen;

    private final int trailerField;

    /**
     * 如 PKCS#1 中定义的 {@code TrailerFieldBC} 常量
     *
     * @apiNote 该字段在 Java SE 8 Maintenance Release 3 中定义。
     * @since 8
     */
    public static final int TRAILER_FIELD_BC = 1;

    /**
     * 使用所有默认值的 PSS 参数集
     *
     * @since 1.5
     */
    public static final PSSParameterSpec DEFAULT = new PSSParameterSpec
        ("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, 20, TRAILER_FIELD_BC);


    // 禁用
    private PSSParameterSpec() {
        throw new RuntimeException("默认构造函数不允许");
    }


    /**
     * 根据 PKCS #1 标准，使用指定的消息摘要、
     * 掩码生成函数、掩码生成函数的参数、盐长度和尾字段值创建新的 {@code PSSParameterSpec}。
     *
     * @param mdName       消息摘要算法的名称
     * @param mgfName      掩码生成函数的算法名称
     * @param mgfSpec      掩码生成函数的参数。
     *         如果指定为 null，则 getMGFParameters() 将返回 null。
     * @param saltLen      盐的长度
     * @param trailerField 尾字段的值
     * @exception NullPointerException 如果 {@code mdName} 或 {@code mgfName} 为 null
     * @exception IllegalArgumentException 如果 {@code saltLen} 或 {@code trailerField} 小于 0
     * @since 1.5
     */
    public PSSParameterSpec(String mdName, String mgfName,
            AlgorithmParameterSpec mgfSpec, int saltLen, int trailerField) {
        Objects.requireNonNull(mdName, "摘要算法为 null");
        Objects.requireNonNull(mgfName,
            "掩码生成函数算法为 null");
        if (saltLen < 0) {
            throw new IllegalArgumentException("盐长度值为负: " +
                                               saltLen);
        }
        if (trailerField < 0) {
            throw new IllegalArgumentException("尾字段为负: " +
                                               trailerField);
        }
        this.mdName = mdName;
        this.mgfName = mgfName;
        this.mgfSpec = mgfSpec;
        this.saltLen = saltLen;
        this.trailerField = trailerField;
    }

    /**
     * 使用指定的盐长度和其他默认值（如 PKCS#1 中定义的）创建新的 {@code PSSParameterSpec}。
     *
     * @param saltLen 在 PKCS#1 PSS 编码中使用的盐长度（字节）
     * @exception IllegalArgumentException 如果 {@code saltLen} 小于 0
     */
    public PSSParameterSpec(int saltLen) {
        this("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, saltLen, TRAILER_FIELD_BC);
    }

    /**
     * 返回消息摘要算法的名称。
     *
     * @return 消息摘要算法的名称
     * @since 1.5
     */
    public String getDigestAlgorithm() {
        return mdName;
    }

    /**
     * 返回掩码生成函数算法的名称。
     *
     * @return 掩码生成函数算法的名称
     *
     * @since 1.5
     */
    public String getMGFAlgorithm() {
        return mgfName;
    }

    /**
     * 返回掩码生成函数的参数。
     *
     * @return 掩码生成函数的参数
     * @since 1.5
     */
    public AlgorithmParameterSpec getMGFParameters() {
        return mgfSpec;
    }

    /**
     * 返回盐的长度（字节）。
     *
     * @return 盐的长度
     */
    public int getSaltLength() {
        return saltLen;
    }

    /**
     * 返回尾字段的值。
     *
     * @return 尾字段的值
     * @since 1.5
     */
    public int getTrailerField() {
        return trailerField;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MD: " + mdName + "\n")
                .append("MGF: " + mgfSpec + "\n")
                .append("SaltLength: " + saltLen + "\n")
                .append("TrailerField: " + trailerField + "\n");
        return sb.toString();
    }
}
