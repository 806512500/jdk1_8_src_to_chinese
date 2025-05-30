
/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.io.*;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import javax.crypto.SecretKey;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.callback.*;

import sun.security.util.Debug;

/**
 * 该类表示一个用于存储加密密钥和证书的存储设施。
 *
 * <p> {@code KeyStore} 管理不同类型的条目。每种类型的条目实现了 {@code KeyStore.Entry} 接口。
 * 提供了三种基本的 {@code KeyStore.Entry} 实现：
 *
 * <ul>
 * <li><b>KeyStore.PrivateKeyEntry</b>
 * <p> 这种类型的条目保存一个加密的 {@code PrivateKey}，可以以受保护的格式存储以防止未经授权的访问。
 * 它还附带了一个证书链，用于对应的公钥。
 *
 * <p> 私钥和证书链用于实体的自我认证。例如，软件分发组织在发布和/或许可软件时会签署 JAR 文件。
 *
 * <li><b>KeyStore.SecretKeyEntry</b>
 * <p> 这种类型的条目保存一个加密的 {@code SecretKey}，可以以受保护的格式存储以防止未经授权的访问。
 *
 * <li><b>KeyStore.TrustedCertificateEntry</b>
 * <p> 这种类型的条目包含一个属于另一方的单个公钥 {@code Certificate}。它被称为 <i>受信任的证书</i>
 * 因为密钥库所有者信任证书中的公钥确实属于证书的 <i>主体</i>（所有者）。
 *
 * <p> 这种类型的条目可以用于认证其他方。
 * </ul>
 *
 * <p> 密钥库中的每个条目都由一个“别名”字符串标识。在私钥及其关联的证书链的情况下，这些字符串区分实体自我认证的不同方式。
 * 例如，实体可以使用不同的证书颁发机构或不同的公钥算法进行自我认证。
 *
 * <p> 别名是否区分大小写取决于实现。为了避免问题，建议不要在密钥库中使用仅大小写不同的别名。
 *
 * <p> 密钥库是否持久化，以及如果持久化使用的机制在这里没有指定。这允许使用各种技术来保护敏感（例如，私有或秘密）密钥。
 * 例如，可以使用智能卡或其他集成的加密引擎（如 SafeKeyper），也可以使用文件（各种格式）。
 *
 * <p> 请求 KeyStore 对象的典型方式包括依赖默认类型和提供特定的密钥库类型。
 *
 * <ul>
 * <li>依赖默认类型：
 * <pre>
 *    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
 * </pre>
 * 系统将返回默认类型的密钥库实现。
 *
 * <li>提供特定的密钥库类型：
 * <pre>
 *      KeyStore ks = KeyStore.getInstance("JKS");
 * </pre>
 * 系统将返回环境中可用的指定密钥库类型的最优先实现。 <p>
 * </ul>
 *
 * <p> 在访问密钥库之前，必须先
 * {@link #load(java.io.InputStream, char[]) 加载}。
 * <pre>
 *    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
 *
 *    // 获取用户密码和文件输入流
 *    char[] password = getPassword();
 *
 *    try (FileInputStream fis = new FileInputStream("keyStoreName")) {
 *        ks.load(fis, password);
 *    }
 * </pre>
 *
 * 要使用上述 {@code load} 方法创建一个空的密钥库，可以将 {@code null} 作为 {@code InputStream} 参数传递。
 *
 * <p> 一旦密钥库被加载，就可以从密钥库中读取现有条目，或者将新条目写入密钥库：
 * <pre>
 *    KeyStore.ProtectionParameter protParam =
 *        new KeyStore.PasswordProtection(password);
 *
 *    // 获取我的私钥
 *    KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry)
 *        ks.getEntry("privateKeyAlias", protParam);
 *    PrivateKey myPrivateKey = pkEntry.getPrivateKey();
 *
 *    // 保存我的秘密密钥
 *    javax.crypto.SecretKey mySecretKey;
 *    KeyStore.SecretKeyEntry skEntry =
 *        new KeyStore.SecretKeyEntry(mySecretKey);
 *    ks.setEntry("secretKeyAlias", skEntry, protParam);
 *
 *    // 保存密钥库
 *    try (FileOutputStream fos = new FileOutputStream("newKeyStoreName")) {
 *        ks.store(fos, password);
 *    }
 * </pre>
 *
 * 注意，虽然可以使用相同的密码来加载密钥库、保护私钥条目、保护秘密密钥条目和存储密钥库（如上述示例代码所示），
 * 也可以使用不同的密码或其他保护参数。
 *
 * <p> 每个 Java 平台的实现都必须支持以下标准的 {@code KeyStore} 类型：
 * <ul>
 * <li>{@code PKCS12}</li>
 * </ul>
 * 该类型在 <a href=
 * "{@docRoot}/../technotes/guides/security/StandardNames.html#KeyStore">
 * KeyStore 部分</a>的
 * Java 加密架构标准算法名称文档中有描述。请参阅实现的发行文档，以了解是否支持其他类型。
 *
 * @author Jan Luehe
 *
 * @see java.security.PrivateKey
 * @see javax.crypto.SecretKey
 * @see java.security.cert.Certificate
 *
 * @since 1.2
 */

public class KeyStore {

    private static final Debug pdebug =
                        Debug.getInstance("provider", "Provider");
    private static final boolean skipDebug =
        Debug.isOn("engine=") && !Debug.isOn("keystore");

    /*
     * 在安全属性文件中查找以确定默认密钥库类型。
     * 在安全属性文件中，默认密钥库类型如下所示：
     * <pre>
     * keystore.type=jks
     * </pre>
     */
    private static final String KEYSTORE_TYPE = "keystore.type";

    // 密钥库类型
    private String type;

    // 提供者
    private Provider provider;

    // 提供者实现
    private KeyStoreSpi keyStoreSpi;

    // 该密钥库是否已初始化（加载）？
    private boolean initialized = false;

    /**
     * 用于 {@code KeyStore}
     * {@link #load(KeyStore.LoadStoreParameter) 加载}
     * 和
     * {@link #store(KeyStore.LoadStoreParameter) 存储} 参数的标记接口。
     *
     * @since 1.5
     */
    public static interface LoadStoreParameter {
        /**
         * 获取用于保护密钥库数据的参数。
         *
         * @return 用于保护密钥库数据的参数，或 null
         */
        public ProtectionParameter getProtectionParameter();
    }

    /**
     * 用于密钥库保护参数的标记接口。
     *
     * <p> 存储在 {@code ProtectionParameter} 对象中的信息用于保护密钥库的内容。
     * 例如，保护参数可以用于检查密钥库数据的完整性，或保护密钥库数据的机密性
     * （如 {@code PrivateKey}）。
     *
     * @since 1.5
     */
    public static interface ProtectionParameter { }

    /**
     * 基于密码的 {@code ProtectionParameter} 实现。
     *
     * @since 1.5
     */
    public static class PasswordProtection implements
                ProtectionParameter, javax.security.auth.Destroyable {

        private final char[] password;
        private final String protectionAlgorithm;
        private final AlgorithmParameterSpec protectionParameters;
        private volatile boolean destroyed = false;

        /**
         * 创建一个密码参数。
         *
         * <p> 指定的 {@code password} 在存储到新的 {@code PasswordProtection} 对象之前会被克隆。
         *
         * @param password 密码，可以为 {@code null}
         */
        public PasswordProtection(char[] password) {
            this.password = (password == null) ? null : password.clone();
            this.protectionAlgorithm = null;
            this.protectionParameters = null;
        }

        /**
         * 创建一个密码参数并指定加密密钥库条目时使用的保护算法及其关联参数。
         * <p>
         * 指定的 {@code password} 在存储到新的 {@code PasswordProtection} 对象之前会被克隆。
         *
         * @param password 密码，可以为 {@code null}
         * @param protectionAlgorithm 加密算法名称，例如，{@code PBEWithHmacSHA256AndAES_256}。
         *     有关标准加密算法名称的信息，请参阅 <a href=
         * "{@docRoot}/../technotes/guides/security/StandardNames.html#Cipher">
         * Java 加密架构标准算法名称文档</a> 中的 Cipher 部分。
         * @param protectionParameters 加密算法参数规范，可以为 {@code null}
         * @exception NullPointerException 如果 {@code protectionAlgorithm} 为
         *     {@code null}
         *
         * @since 1.8
         */
        public PasswordProtection(char[] password, String protectionAlgorithm,
            AlgorithmParameterSpec protectionParameters) {
            if (protectionAlgorithm == null) {
                throw new NullPointerException("invalid null input");
            }
            this.password = (password == null) ? null : password.clone();
            this.protectionAlgorithm = protectionAlgorithm;
            this.protectionParameters = protectionParameters;
        }

        /**
         * 获取保护算法的名称。
         * 如果未设置，则密钥库提供者将使用其默认的保护算法。给定密钥库类型的默认保护算法名称
         * 通过 {@code 'keystore.<type>.keyProtectionAlgorithm'} 安全属性设置。
         * 例如，{@code keystore.PKCS12.keyProtectionAlgorithm} 属性存储了
         * PKCS12 密钥库使用的默认密钥保护算法名称。如果未设置安全属性，则使用实现特定的算法。
         *
         * @return 算法名称，如果未设置则为 {@code null}
         *
         * @since 1.8
         */
        public String getProtectionAlgorithm() {
            return protectionAlgorithm;
        }

        /**
         * 获取保护算法的参数。
         *
         * @return 算法参数规范，如果未设置则为 {@code  null}
         *
         * @since 1.8
         */
        public AlgorithmParameterSpec getProtectionParameters() {
            return protectionParameters;
        }

        /**
         * 获取密码。
         *
         * <p> 注意，此方法返回密码的引用。如果创建了数组的克隆，则调用者负责在不再需要时清除密码信息。
         *
         * @see #destroy()
         * @return 密码，可以为 {@code null}
         * @exception IllegalStateException 如果密码已被清除（销毁）
         */
        public synchronized char[] getPassword() {
            if (destroyed) {
                throw new IllegalStateException("password has been cleared");
            }
            return password;
        }

        /**
         * 清除密码。
         *
         * @exception DestroyFailedException 如果此方法无法清除密码
         */
        public synchronized void destroy() throws DestroyFailedException {
            destroyed = true;
            if (password != null) {
                Arrays.fill(password, ' ');
            }
        }

        /**
         * 确定密码是否已被清除。
         *
         * @return 如果密码已被清除则返回 true，否则返回 false
         */
        public synchronized boolean isDestroyed() {
            return destroyed;
        }
    }

    /**
     * 包含 CallbackHandler 的 ProtectionParameter。
     *
     * @since 1.5
     */
    public static class CallbackHandlerProtection
            implements ProtectionParameter {

        private final CallbackHandler handler;

        /**
         * 从 CallbackHandler 构造新的 CallbackHandlerProtection。
         *
         * @param handler CallbackHandler
         * @exception NullPointerException 如果 handler 为 null
         */
        public CallbackHandlerProtection(CallbackHandler handler) {
            if (handler == null) {
                throw new NullPointerException("handler must not be null");
            }
            this.handler = handler;
        }

        /**
         * 返回 CallbackHandler。
         *
         * @return CallbackHandler。
         */
        public CallbackHandler getCallbackHandler() {
            return handler;
        }

    }

    /**
     * 用于 {@code KeyStore} 条目类型的标记接口。
     *
     * @since 1.5
     */
    public static interface Entry {

        /**
         * 检索与条目关联的属性。
         * <p>
         * 默认实现返回一个空的 {@code Set}。
         *
         * @return 一个不可修改的 {@code Set} 属性，可能为空
         *
         * @since 1.8
         */
        public default Set<Attribute> getAttributes() {
            return Collections.<Attribute>emptySet();
        }


                    /**
         * 与密钥库条目关联的属性。
         * 它包含一个名称和一个或多个值。
         *
         * @since 1.8
         */
        public interface Attribute {
            /**
             * 返回属性的名称。
             *
             * @return 属性名称
             */
            public String getName();

            /**
             * 返回属性的值。
             * 多值属性将其值编码为单个字符串。
             *
             * @return 属性值
             */
            public String getValue();
        }
    }

    /**
     * 一个包含 {@code PrivateKey} 和相应证书链的 {@code KeyStore} 条目。
     *
     * @since 1.5
     */
    public static final class PrivateKeyEntry implements Entry {

        private final PrivateKey privKey;
        private final Certificate[] chain;
        private final Set<Attribute> attributes;

        /**
         * 构造一个包含 {@code PrivateKey} 和相应证书链的 {@code PrivateKeyEntry}。
         *
         * <p> 指定的 {@code chain} 在存储到新的 {@code PrivateKeyEntry} 对象之前会被克隆。
         *
         * @param privateKey {@code PrivateKey}
         * @param chain 一个表示证书链的 {@code Certificate} 数组。
         *      证书链必须有序，并且在索引 0 处包含一个与私钥对应的 {@code Certificate}。
         *
         * @exception NullPointerException 如果 {@code privateKey} 或 {@code chain}
         *      为 {@code null}
         * @exception IllegalArgumentException 如果指定的链长度为 0，如果指定的链不包含
         *      相同类型的 {@code Certificate}，或者如果 {@code PrivateKey} 算法
         *      与端实体 {@code Certificate}（索引 0）中的 {@code PublicKey} 算法不匹配
         */
        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain) {
            this(privateKey, chain, Collections.<Attribute>emptySet());
        }

        /**
         * 构造一个包含 {@code PrivateKey} 和相应证书链以及关联条目属性的 {@code PrivateKeyEntry}。
         *
         * <p> 指定的 {@code chain} 和 {@code attributes} 在存储到新的 {@code PrivateKeyEntry} 对象之前会被克隆。
         *
         * @param privateKey {@code PrivateKey}
         * @param chain 一个表示证书链的 {@code Certificate} 数组。
         *      证书链必须有序，并且在索引 0 处包含一个与私钥对应的 {@code Certificate}。
         * @param attributes 属性
         *
         * @exception NullPointerException 如果 {@code privateKey}、{@code chain}
         *      或 {@code attributes} 为 {@code null}
         * @exception IllegalArgumentException 如果指定的链长度为 0，如果指定的链不包含
         *      相同类型的 {@code Certificate}，或者如果 {@code PrivateKey} 算法
         *      与端实体 {@code Certificate}（索引 0）中的 {@code PublicKey} 算法不匹配
         *
         * @since 1.8
         */
        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain,
           Set<Attribute> attributes) {

            if (privateKey == null || chain == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            if (chain.length == 0) {
                throw new IllegalArgumentException
                                ("invalid zero-length input chain");
            }

            Certificate[] clonedChain = chain.clone();
            String certType = clonedChain[0].getType();
            for (int i = 1; i < clonedChain.length; i++) {
                if (!certType.equals(clonedChain[i].getType())) {
                    throw new IllegalArgumentException
                                ("chain does not contain certificates " +
                                "of the same type");
                }
            }
            if (!privateKey.getAlgorithm().equals
                        (clonedChain[0].getPublicKey().getAlgorithm())) {
                throw new IllegalArgumentException
                                ("private key algorithm does not match " +
                                "algorithm of public key in end entity " +
                                "certificate (at index 0)");
            }
            this.privKey = privateKey;

            if (clonedChain[0] instanceof X509Certificate &&
                !(clonedChain instanceof X509Certificate[])) {

                this.chain = new X509Certificate[clonedChain.length];
                System.arraycopy(clonedChain, 0,
                                this.chain, 0, clonedChain.length);
            } else {
                this.chain = clonedChain;
            }

            this.attributes =
                Collections.unmodifiableSet(new HashSet<>(attributes));
        }

        /**
         * 从该条目中获取 {@code PrivateKey}。
         *
         * @return 从该条目中获取的 {@code PrivateKey}
         */
        public PrivateKey getPrivateKey() {
            return privKey;
        }

        /**
         * 从该条目中获取 {@code Certificate} 链。
         *
         * <p> 存储的链在返回前会被克隆。
         *
         * @return 一个表示公钥证书链的 {@code Certificate} 数组。
         *      如果证书是 X.509 类型，返回数组的运行时类型是
         *      {@code X509Certificate[]}。
         */
        public Certificate[] getCertificateChain() {
            return chain.clone();
        }

        /**
         * 从该条目的证书链中获取端实体 {@code Certificate}。
         *
         * @return 从该条目的证书链中获取的端实体 {@code Certificate}（索引 0）。
         *      如果证书是 X.509 类型，返回证书的运行时类型是
         *      {@code X509Certificate}。
         */
        public Certificate getCertificate() {
            return chain[0];
        }

        /**
         * 获取与条目关联的属性。
         * <p>
         *
         * @return 一个不可修改的属性 {@code Set}，可能为空
         *
         * @since 1.8
         */
        @Override
        public Set<Attribute> getAttributes() {
            return attributes;
        }

        /**
         * 返回此 PrivateKeyEntry 的字符串表示形式。
         * @return 此 PrivateKeyEntry 的字符串表示形式。
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Private key entry and certificate chain with "
                + chain.length + " elements:\r\n");
            for (Certificate cert : chain) {
                sb.append(cert);
                sb.append("\r\n");
            }
            return sb.toString();
        }

    }

    /**
     * 一个包含 {@code SecretKey} 的 {@code KeyStore} 条目。
     *
     * @since 1.5
     */
    public static final class SecretKeyEntry implements Entry {

        private final SecretKey sKey;
        private final Set<Attribute> attributes;

        /**
         * 构造一个包含 {@code SecretKey} 的 {@code SecretKeyEntry}。
         *
         * @param secretKey {@code SecretKey}
         *
         * @exception NullPointerException 如果 {@code secretKey}
         *      为 {@code null}
         */
        public SecretKeyEntry(SecretKey secretKey) {
            if (secretKey == null) {
                throw new NullPointerException("invalid null input");
            }
            this.sKey = secretKey;
            this.attributes = Collections.<Attribute>emptySet();
        }

        /**
         * 构造一个包含 {@code SecretKey} 和关联条目属性的 {@code SecretKeyEntry}。
         *
         * <p> 指定的 {@code attributes} 在存储到新的 {@code SecretKeyEntry} 对象之前会被克隆。
         *
         * @param secretKey {@code SecretKey}
         * @param attributes 属性
         *
         * @exception NullPointerException 如果 {@code secretKey} 或
         *     {@code attributes} 为 {@code null}
         *
         * @since 1.8
         */
        public SecretKeyEntry(SecretKey secretKey, Set<Attribute> attributes) {

            if (secretKey == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            this.sKey = secretKey;
            this.attributes =
                Collections.unmodifiableSet(new HashSet<>(attributes));
        }

        /**
         * 从该条目中获取 {@code SecretKey}。
         *
         * @return 从该条目中获取的 {@code SecretKey}
         */
        public SecretKey getSecretKey() {
            return sKey;
        }

        /**
         * 获取与条目关联的属性。
         * <p>
         *
         * @return 一个不可修改的属性 {@code Set}，可能为空
         *
         * @since 1.8
         */
        @Override
        public Set<Attribute> getAttributes() {
            return attributes;
        }

        /**
         * 返回此 SecretKeyEntry 的字符串表示形式。
         * @return 此 SecretKeyEntry 的字符串表示形式。
         */
        public String toString() {
            return "Secret key entry with algorithm " + sKey.getAlgorithm();
        }
    }

    /**
     * 一个包含受信任的 {@code Certificate} 的 {@code KeyStore} 条目。
     *
     * @since 1.5
     */
    public static final class TrustedCertificateEntry implements Entry {

        private final Certificate cert;
        private final Set<Attribute> attributes;

        /**
         * 构造一个包含受信任的 {@code Certificate} 的 {@code TrustedCertificateEntry}。
         *
         * @param trustedCert 受信任的 {@code Certificate}
         *
         * @exception NullPointerException 如果
         *      {@code trustedCert} 为 {@code null}
         */
        public TrustedCertificateEntry(Certificate trustedCert) {
            if (trustedCert == null) {
                throw new NullPointerException("invalid null input");
            }
            this.cert = trustedCert;
            this.attributes = Collections.<Attribute>emptySet();
        }

        /**
         * 构造一个包含受信任的 {@code Certificate} 和关联条目属性的 {@code TrustedCertificateEntry}。
         *
         * <p> 指定的 {@code attributes} 在存储到新的 {@code TrustedCertificateEntry} 对象之前会被克隆。
         *
         * @param trustedCert 受信任的 {@code Certificate}
         * @param attributes 属性
         *
         * @exception NullPointerException 如果 {@code trustedCert} 或
         *     {@code attributes} 为 {@code null}
         *
         * @since 1.8
         */
        public TrustedCertificateEntry(Certificate trustedCert,
           Set<Attribute> attributes) {
            if (trustedCert == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            this.cert = trustedCert;
            this.attributes =
                Collections.unmodifiableSet(new HashSet<>(attributes));
        }

        /**
         * 从该条目中获取受信任的 {@code Certificate}。
         *
         * @return 从该条目中获取的受信任的 {@code Certificate}
         */
        public Certificate getTrustedCertificate() {
            return cert;
        }

        /**
         * 获取与条目关联的属性。
         * <p>
         *
         * @return 一个不可修改的属性 {@code Set}，可能为空
         *
         * @since 1.8
         */
        @Override
        public Set<Attribute> getAttributes() {
            return attributes;
        }

        /**
         * 返回此 TrustedCertificateEntry 的字符串表示形式。
         * @return 此 TrustedCertificateEntry 的字符串表示形式。
         */
        public String toString() {
            return "Trusted certificate entry:\r\n" + cert.toString();
        }
    }

    /**
     * 创建一个给定类型的 KeyStore 对象，并将给定的提供者实现（SPI 对象）封装在其中。
     *
     * @param keyStoreSpi 提供者实现。
     * @param provider 提供者。
     * @param type 密钥库类型。
     */
    protected KeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type)
    {
        this.keyStoreSpi = keyStoreSpi;
        this.provider = provider;
        this.type = type;

        if (!skipDebug && pdebug != null) {
            pdebug.println("KeyStore." + type.toUpperCase() + " type from: " +
                this.provider.getName());
        }
    }

    /**
     * 返回指定类型的密钥库对象。
     *
     * <p> 该方法遍历注册的安全提供者列表，从最优先的提供者开始。
     * 返回一个新的 KeyStore 对象，封装了第一个支持指定类型的
     * KeyStoreSpi 实现的提供者。
     *
     * <p> 注册的提供者列表可以通过
     * {@link Security#getProviders() Security.getProviders()} 方法获取。
     *
     * @param type 密钥库类型。
     * 有关标准密钥库类型的详细信息，请参阅 <a href=
     * "{@docRoot}/../technotes/guides/security/StandardNames.html#KeyStore">
     * Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     * 中的 KeyStore 部分。
     *
     * @return 指定类型的密钥库对象。
     *
     * @exception KeyStoreException 如果没有提供者支持指定类型的
     *          KeyStoreSpi 实现。
     *
     * @see Provider
     */
    public static KeyStore getInstance(String type)
        throws KeyStoreException
    {
        try {
            Object[] objs = Security.getImpl(type, "KeyStore", (String)null);
            return new KeyStore((KeyStoreSpi)objs[0], (Provider)objs[1], type);
        } catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreException(type + " not found", nsae);
        } catch (NoSuchProviderException nspe) {
            throw new KeyStoreException(type + " not found", nspe);
        }
    }

    /**
     * 返回指定类型的密钥库对象。
     *
     * <p> 返回一个新的 KeyStore 对象，封装了指定提供者的
     * KeyStoreSpi 实现。指定的提供者必须在安全提供者列表中注册。
     *
     * <p> 注册的提供者列表可以通过
     * {@link Security#getProviders() Security.getProviders()} 方法获取。
     *
     * @param type 密钥库类型。
     * 有关标准密钥库类型的详细信息，请参阅 <a href=
     * "{@docRoot}/../technotes/guides/security/StandardNames.html#KeyStore">
     * Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     * 中的 KeyStore 部分。
     *
     * @param provider 提供者的名称。
     *
     * @return 指定类型的密钥库对象。
     *
     * @exception KeyStoreException 如果指定提供者不支持指定类型的
     *          KeyStoreSpi 实现。
     *
     * @exception NoSuchProviderException 如果指定的提供者未在安全提供者列表中注册。
     *
     * @exception IllegalArgumentException 如果提供者名称为 null
     *          或为空。
     *
     * @see Provider
     */
    public static KeyStore getInstance(String type, String provider)
        throws KeyStoreException, NoSuchProviderException
    {
        if (provider == null || provider.length() == 0)
            throw new IllegalArgumentException("missing provider");
        try {
            Object[] objs = Security.getImpl(type, "KeyStore", provider);
            return new KeyStore((KeyStoreSpi)objs[0], (Provider)objs[1], type);
        } catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreException(type + " not found", nsae);
        }
    }


                /**
     * 返回指定类型的密钥库对象。
     *
     * <p> 返回一个新的 KeyStore 对象，封装了从指定的 Provider 对象中获取的
     * KeyStoreSpi 实现。注意，指定的 Provider 对象不必注册在提供者列表中。
     *
     * @param type 密钥库的类型。
     * 有关标准密钥库类型的详细信息，请参阅 <a href=
     * "{@docRoot}/../technotes/guides/security/StandardNames.html#KeyStore">
     * Java 密码架构标准算法名称文档</a> 中的密钥库部分。
     *
     * @param provider 提供者。
     *
     * @return 指定类型的密钥库对象。
     *
     * @exception KeyStoreException 如果从指定的 Provider 对象中无法获取指定类型的 KeyStoreSpi
     *          实现。
     *
     * @exception IllegalArgumentException 如果指定的提供者为 null。
     *
     * @see Provider
     *
     * @since 1.4
     */
    public static KeyStore getInstance(String type, Provider provider)
        throws KeyStoreException
    {
        if (provider == null)
            throw new IllegalArgumentException("缺少提供者");
        try {
            Object[] objs = Security.getImpl(type, "KeyStore", provider);
            return new KeyStore((KeyStoreSpi)objs[0], (Provider)objs[1], type);
        } catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreException(type + " 未找到", nsae);
        }
    }

    /**
     * 返回由 {@code keystore.type} 安全属性指定的默认密钥库类型，或如果不存在此类属性，则返回字符串
     * {@literal "jks"}（代表 {@literal "Java 密钥库"}）。
     *
     * <p> 应用程序可以使用默认的密钥库类型，而无需在调用 {@code getInstance} 方法时使用硬编码的密钥库类型，
     * 并且可以在用户未指定自己的密钥库类型时提供默认的密钥库类型。
     *
     * <p> 可以通过将 {@code keystore.type} 安全属性的值设置为所需的密钥库类型来更改默认的密钥库类型。
     *
     * @return 由 {@code keystore.type} 安全属性指定的默认密钥库类型，或如果不存在此类属性，则返回字符串 {@literal "jks"}。
     * @see java.security.Security 安全属性
     */
    public final static String getDefaultType() {
        String kstype;
        kstype = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return Security.getProperty(KEYSTORE_TYPE);
            }
        });
        if (kstype == null) {
            kstype = "jks";
        }
        return kstype;
    }

    /**
     * 返回此密钥库的提供者。
     *
     * @return 此密钥库的提供者。
     */
    public final Provider getProvider()
    {
        return this.provider;
    }

    /**
     * 返回此密钥库的类型。
     *
     * @return 此密钥库的类型。
     */
    public final String getType()
    {
        return this.type;
    }

    /**
     * 使用给定的密码恢复与给定别名关联的密钥。该密钥必须通过调用 {@code setKeyEntry} 或通过调用
     * {@code setEntry} 并使用 {@code PrivateKeyEntry} 或 {@code SecretKeyEntry} 来与别名关联。
     *
     * @param alias 别名名称
     * @param password 用于恢复密钥的密码
     *
     * @return 请求的密钥，如果给定的别名不存在或不标识密钥相关的条目，则返回 null。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     * @exception NoSuchAlgorithmException 如果无法找到恢复密钥的算法
     * @exception UnrecoverableKeyException 如果无法恢复密钥（例如，给定的密码错误）。
     */
    public final Key getKey(String alias, char[] password)
        throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineGetKey(alias, password);
    }

    /**
     * 返回与给定别名关联的证书链。该证书链必须通过调用 {@code setKeyEntry} 或通过调用
     * {@code setEntry} 并使用 {@code PrivateKeyEntry} 来与别名关联。
     *
     * @param alias 别名名称
     *
     * @return 证书链（按顺序排列，用户证书在前，后跟零个或多个证书颁发机构），如果给定的别名不存在或不包含证书链，则返回 null。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final Certificate[] getCertificateChain(String alias)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineGetCertificateChain(alias);
    }

    /**
     * 返回与给定别名关联的证书。
     *
     * <p> 如果给定的别名标识通过调用 {@code setCertificateEntry} 创建的条目，或通过调用
     * {@code setEntry} 并使用 {@code TrustedCertificateEntry} 创建的条目，则返回该条目中包含的受信任证书。
     *
     * <p> 如果给定的别名标识通过调用 {@code setKeyEntry} 创建的条目，或通过调用
     * {@code setEntry} 并使用 {@code PrivateKeyEntry} 创建的条目，则返回该条目中证书链的第一个元素。
     *
     * @param alias 别名名称
     *
     * @return 证书，如果给定的别名不存在或不包含证书，则返回 null。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final Certificate getCertificate(String alias)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineGetCertificate(alias);
    }

    /**
     * 返回由给定别名标识的条目的创建日期。
     *
     * @param alias 别名名称
     *
     * @return 该条目的创建日期，如果给定的别名不存在，则返回 null。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final Date getCreationDate(String alias)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineGetCreationDate(alias);
    }

    /**
     * 将给定的密钥分配给给定的别名，并使用给定的密码保护它。
     *
     * <p> 如果给定的密钥类型为 {@code java.security.PrivateKey}，则必须附带一个证书链来认证相应的公钥。
     *
     * <p> 如果给定的别名已存在，则与该别名关联的密钥库信息将被给定的密钥（和可能的证书链）覆盖。
     *
     * @param alias 别名名称
     * @param key 要与别名关联的密钥
     * @param password 用于保护密钥的密码
     * @param chain 相应公钥的证书链（仅在给定的密钥类型为
     * {@code java.security.PrivateKey} 时需要）。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载），给定的密钥无法保护，或此操作因其他原因失败。
     */
    public final void setKeyEntry(String alias, Key key, char[] password,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        if ((key instanceof PrivateKey) &&
            (chain == null || chain.length == 0)) {
            throw new IllegalArgumentException("私钥必须附带证书链");
        }
        keyStoreSpi.engineSetKeyEntry(alias, key, password, chain);
    }

    /**
     * 将已受保护的密钥分配给给定的别名。
     *
     * <p> 如果受保护的密钥类型为 {@code java.security.PrivateKey}，则必须附带一个证书链来认证相应的公钥。如果底层密钥库实现类型为 {@code jks}，
     * {@code key} 必须按照 PKCS #8 标准编码为 {@code EncryptedPrivateKeyInfo}。
     *
     * <p> 如果给定的别名已存在，则与该别名关联的密钥库信息将被给定的密钥（和可能的证书链）覆盖。
     *
     * @param alias 别名名称
     * @param key 要与别名关联的密钥（受保护格式）
     * @param chain 相应公钥的证书链（仅在受保护的密钥类型为
     * {@code java.security.PrivateKey} 时有用）。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载），或此操作因其他原因失败。
     */
    public final void setKeyEntry(String alias, byte[] key,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        keyStoreSpi.engineSetKeyEntry(alias, key, chain);
    }

    /**
     * 将给定的受信任证书分配给给定的别名。
     *
     * <p> 如果给定的别名标识通过调用 {@code setCertificateEntry} 创建的现有条目，或通过调用
     * {@code setEntry} 并使用 {@code TrustedCertificateEntry} 创建的条目，则现有条目中的受信任证书将被给定的证书覆盖。
     *
     * @param alias 别名名称
     * @param cert 证书
     *
     * @exception KeyStoreException 如果密钥库未初始化，或给定的别名已存在但不标识包含受信任证书的条目，
     * 或此操作因其他原因失败。
     */
    public final void setCertificateEntry(String alias, Certificate cert)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        keyStoreSpi.engineSetCertificateEntry(alias, cert);
    }

    /**
     * 从此密钥库中删除由给定别名标识的条目。
     *
     * @param alias 别名名称
     *
     * @exception KeyStoreException 如果密钥库未初始化，或条目无法删除。
     */
    public final void deleteEntry(String alias)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        keyStoreSpi.engineDeleteEntry(alias);
    }

    /**
     * 列出此密钥库中所有别名的名称。
     *
     * @return 别名名称的枚举
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final Enumeration<String> aliases()
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineAliases();
    }

    /**
     * 检查给定的别名是否存在于此密钥库中。
     *
     * @param alias 别名名称
     *
     * @return 如果别名存在，则返回 true，否则返回 false。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final boolean containsAlias(String alias)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineContainsAlias(alias);
    }

    /**
     * 检索此密钥库中的条目数量。
     *
     * @return 此密钥库中的条目数量。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final int size()
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineSize();
    }

    /**
     * 如果由给定别名标识的条目是通过调用 {@code setKeyEntry} 创建的，或通过调用
     * {@code setEntry} 并使用 {@code PrivateKeyEntry} 或 {@code SecretKeyEntry} 创建的，则返回 true。
     *
     * @param alias 要检查的密钥库条目的别名
     *
     * @return 如果由给定别名标识的条目是密钥相关的条目，则返回 true，否则返回 false。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final boolean isKeyEntry(String alias)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineIsKeyEntry(alias);
    }

    /**
     * 如果由给定别名标识的条目是通过调用 {@code setCertificateEntry} 创建的，或通过调用
     * {@code setEntry} 并使用 {@code TrustedCertificateEntry} 创建的，则返回 true。
     *
     * @param alias 要检查的密钥库条目的别名
     *
     * @return 如果由给定别名标识的条目包含受信任的证书，则返回 true，否则返回 false。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final boolean isCertificateEntry(String alias)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineIsCertificateEntry(alias);
    }

    /**
     * 返回证书与给定证书匹配的第一个密钥库条目的（别名）名称。
     *
     * <p> 该方法尝试将给定的证书与每个密钥库条目匹配。如果正在考虑的条目是通过调用
     * {@code setCertificateEntry} 创建的，或通过调用 {@code setEntry} 并使用
     * {@code TrustedCertificateEntry} 创建的，则将给定的证书与该条目的证书进行比较。
     *
     * <p> 如果正在考虑的条目是通过调用 {@code setKeyEntry} 创建的，或通过调用
     * {@code setEntry} 并使用 {@code PrivateKeyEntry} 创建的，则将给定的证书与该条目证书链的第一个元素进行比较。
     *
     * @param cert 要匹配的证书。
     *
     * @return 与给定证书匹配的第一个条目的别名名称，如果此密钥库中不存在此类条目，则返回 null。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     */
    public final String getCertificateAlias(Certificate cert)
        throws KeyStoreException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineGetCertificateAlias(cert);
    }


                /**
     * 将此密钥库存储到给定的输出流中，并使用给定的密码保护其完整性。
     *
     * @param stream 密钥库写入的输出流。
     * @param password 用于生成密钥库完整性检查的密码。
     *
     * @exception KeyStoreException 如果密钥库未初始化（加载）。
     * @exception IOException 如果数据存在 I/O 问题。
     * @exception NoSuchAlgorithmException 如果找不到适当的数据完整性算法。
     * @exception CertificateException 如果密钥库数据中的任何证书无法存储。
     */
    public final void store(OutputStream stream, char[] password)
        throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException
    {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        keyStoreSpi.engineStore(stream, password);
    }

    /**
     * 使用给定的 {@code LoadStoreParameter} 存储此密钥库。
     *
     * @param param 指定如何存储密钥库的 {@code LoadStoreParameter}，
     *          可能为 {@code null}。
     *
     * @exception IllegalArgumentException 如果给定的
     *          {@code LoadStoreParameter}
     *          输入未被识别。
     * @exception KeyStoreException 如果密钥库未初始化
     *          （加载）。
     * @exception IOException 如果数据存在 I/O 问题。
     * @exception NoSuchAlgorithmException 如果找不到适当的数据完整性算法。
     * @exception CertificateException 如果密钥库数据中的任何证书无法存储。
     *
     * @since 1.5
     */
    public final void store(LoadStoreParameter param)
                throws KeyStoreException, IOException,
                NoSuchAlgorithmException, CertificateException {
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        keyStoreSpi.engineStore(param);
    }

    /**
     * 从给定的输入流加载此密钥库。
     *
     * <p>可以提供密码以解锁密钥库
     * （例如，密钥库位于硬件令牌设备上），
     * 或检查密钥库数据的完整性。
     * 如果未提供密码进行完整性检查，
     * 则不执行完整性检查。
     *
     * <p>为了创建空的密钥库，或如果密钥库无法从流中初始化，
     * 请将 {@code null}
     * 作为 {@code stream} 参数传递。
     *
     * <p>注意，如果此密钥库已加载，则会重新初始化并再次从给定的输入流加载。
     *
     * @param stream 从其中加载密钥库的输入流，
     * 或 {@code null}
     * @param password 用于检查密钥库完整性的密码，
     * 用于解锁密钥库的密码，
     * 或 {@code null}
     *
     * @exception IOException 如果密钥库数据存在 I/O 或格式问题，
     * 如果需要密码但未提供，
     * 或提供的密码不正确。如果错误是由于错误的密码，
     * {@code IOException} 的 {@link Throwable#getCause 原因} 应该是
     * {@code UnrecoverableKeyException}
     * @exception NoSuchAlgorithmException 如果用于检查密钥库完整性的算法找不到。
     * @exception CertificateException 如果密钥库中的任何证书无法加载。
     */
    public final void load(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStoreSpi.engineLoad(stream, password);
        initialized = true;
    }

    /**
     * 使用给定的 {@code LoadStoreParameter} 加载此密钥库。
     *
     * <p>注意，如果此密钥库已加载，则会重新初始化并再次从给定的参数加载。
     *
     * @param param 指定如何加载密钥库的 {@code LoadStoreParameter}，
     *          可能为 {@code null}。
     *
     * @exception IllegalArgumentException 如果给定的
     *          {@code LoadStoreParameter}
     *          输入未被识别。
     * @exception IOException 如果密钥库数据存在 I/O 或格式问题。
     * 如果错误是由于错误的 {@code ProtectionParameter}（例如，错误的密码），
     * {@code IOException} 的 {@link Throwable#getCause 原因} 应该是
     * {@code UnrecoverableKeyException}
     * @exception NoSuchAlgorithmException 如果用于检查密钥库完整性的算法找不到。
     * @exception CertificateException 如果密钥库中的任何证书无法加载。
     *
     * @since 1.5
     */
    public final void load(LoadStoreParameter param)
                throws IOException, NoSuchAlgorithmException,
                CertificateException {

        keyStoreSpi.engineLoad(param);
        initialized = true;
    }

    /**
     * 获取指定别名的密钥库 {@code Entry}，
     * 使用指定的保护参数。
     *
     * @param alias 获取密钥库 {@code Entry} 的别名。
     * @param protParam 用于保护 {@code Entry} 的 {@code ProtectionParameter}，
     *          可能为 {@code null}。
     *
     * @return 指定别名的密钥库 {@code Entry}，
     *          如果没有这样的条目，则返回 {@code null}。
     *
     * @exception NullPointerException 如果
     *          {@code alias} 为 {@code null}。
     * @exception NoSuchAlgorithmException 如果找不到恢复条目的算法。
     * @exception UnrecoverableEntryException 如果指定的
     *          {@code protParam} 不充分或无效。
     * @exception UnrecoverableKeyException 如果条目是
     *          {@code PrivateKeyEntry} 或 {@code SecretKeyEntry}
     *          且指定的 {@code protParam} 不包含恢复密钥所需的信息（例如，错误的密码）。
     * @exception KeyStoreException 如果密钥库未初始化
     *          （加载）。
     * @see #setEntry(String, KeyStore.Entry, KeyStore.ProtectionParameter)
     *
     * @since 1.5
     */
    public final Entry getEntry(String alias, ProtectionParameter protParam)
                throws NoSuchAlgorithmException, UnrecoverableEntryException,
                KeyStoreException {

        if (alias == null) {
            throw new NullPointerException("无效的 null 输入");
        }
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineGetEntry(alias, protParam);
    }

    /**
     * 在指定的别名下保存密钥库 {@code Entry}。
     * 保护参数用于保护
     * {@code Entry}。
     *
     * <p>如果指定的别名已存在条目，
     * 则会覆盖该条目。
     *
     * @param alias 在此别名下保存密钥库 {@code Entry}。
     * @param entry 要保存的 {@code Entry}。
     * @param protParam 用于保护 {@code Entry} 的 {@code ProtectionParameter}，
     *          可能为 {@code null}。
     *
     * @exception NullPointerException 如果
     *          {@code alias} 或 {@code entry}
     *          为 {@code null}。
     * @exception KeyStoreException 如果密钥库未初始化
     *          （加载），或此操作因其他原因失败。
     *
     * @see #getEntry(String, KeyStore.ProtectionParameter)
     *
     * @since 1.5
     */
    public final void setEntry(String alias, Entry entry,
                        ProtectionParameter protParam)
                throws KeyStoreException {
        if (alias == null || entry == null) {
            throw new NullPointerException("无效的 null 输入");
        }
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        keyStoreSpi.engineSetEntry(alias, entry, protParam);
    }

    /**
     * 确定指定的 {@code alias} 的密钥库 {@code Entry}
     * 是否是指定的 {@code entryClass} 的实例或子类。
     *
     * @param alias 别名名称。
     * @param entryClass 条目类。
     *
     * @return 如果指定的 {@code alias} 的密钥库 {@code Entry}
     *          是指定的 {@code entryClass} 的实例或子类，则返回 true，否则返回 false。
     *
     * @exception NullPointerException 如果
     *          {@code alias} 或 {@code entryClass}
     *          为 {@code null}。
     * @exception KeyStoreException 如果密钥库未初始化
     *          （加载）。
     *
     * @since 1.5
     */
    public final boolean
        entryInstanceOf(String alias,
                        Class<? extends KeyStore.Entry> entryClass)
        throws KeyStoreException
    {

        if (alias == null || entryClass == null) {
            throw new NullPointerException("无效的 null 输入");
        }
        if (!initialized) {
            throw new KeyStoreException("未初始化的密钥库");
        }
        return keyStoreSpi.engineEntryInstanceOf(alias, entryClass);
    }

    /**
     * 要实例化的密钥库对象的描述。
     *
     * <p>此类的实例封装了实例化和初始化密钥库对象所需的信息。当调用
     * {@linkplain #getKeyStore} 方法时，该过程被触发。
     *
     * <p>这使得配置可以与密钥库对象的创建分离，例如，可以延迟密码提示，直到需要时再提示。
     *
     * @see KeyStore
     * @see javax.net.ssl.KeyStoreBuilderParameters
     * @since 1.5
     */
    public static abstract class Builder {

        // 如果密码错误，尝试回调处理器的最大次数
        static final int MAX_CALLBACK_TRIES = 3;

        /**
         * 构造一个新的 Builder。
         */
        protected Builder() {
            // 空
        }

        /**
         * 返回此对象描述的密钥库。
         *
         * @return 此对象描述的 {@code KeyStore}。
         * @exception KeyStoreException 如果操作过程中发生错误，例如密钥库无法
         *   实例化或加载。
         */
        public abstract KeyStore getKeyStore() throws KeyStoreException;

        /**
         * 返回用于获取具有给定别名的 {@link KeyStore.Entry Entry} 的
         * 保护参数。必须先调用 {@code getKeyStore} 方法，然后才能调用此方法。
         *
         * @return 用于获取具有给定别名的 {@link KeyStore.Entry Entry} 的
         *   保护参数。
         * @param alias 密钥库条目的别名。
         * @throws NullPointerException 如果别名为 null。
         * @throws KeyStoreException 如果操作过程中发生错误。
         * @throws IllegalStateException 如果在调用此方法之前未调用 {@code getKeyStore} 方法。
         */
        public abstract ProtectionParameter getProtectionParameter(String alias)
            throws KeyStoreException;

        /**
         * 返回一个封装给定密钥库的新 Builder。
         * 返回对象的 {@linkplain #getKeyStore} 方法将返回 {@code keyStore}，
         * {@linkplain #getProtectionParameter getProtectionParameter()} 方法将
         * 返回 {@code protectionParameters}。
         *
         * <p>如果需要使用现有的密钥库对象与基于 Builder 的 API 一起使用，这非常有用。
         *
         * @return 一个新的 Builder 对象。
         * @param keyStore 要封装的密钥库。
         * @param protectionParameter 用于保护密钥库条目的保护参数。
         * @throws NullPointerException 如果 keyStore 或
         *   protectionParameters 为 null。
         * @throws IllegalArgumentException 如果密钥库未初始化。
         */
        public static Builder newInstance(final KeyStore keyStore,
                final ProtectionParameter protectionParameter) {
            if ((keyStore == null) || (protectionParameter == null)) {
                throw new NullPointerException();
            }
            if (keyStore.initialized == false) {
                throw new IllegalArgumentException("密钥库未初始化");
            }
            return new Builder() {
                private volatile boolean getCalled;

                public KeyStore getKeyStore() {
                    getCalled = true;
                    return keyStore;
                }

                public ProtectionParameter getProtectionParameter(String alias)
                {
                    if (alias == null) {
                        throw new NullPointerException();
                    }
                    if (getCalled == false) {
                        throw new IllegalStateException
                            ("必须先调用 getKeyStore()");
                    }
                    return protectionParameter;
                }
            };
        }

        /**
         * 返回一个新的 Builder 对象。
         *
         * <p>对返回的构建器的 {@link #getKeyStore} 方法的首次调用将创建类型为 {@code type} 的密钥库，
         * 并调用其 {@link KeyStore#load load()} 方法。
         * {@code inputStream} 参数从
         * {@code file} 构建。
         * 如果 {@code protection} 是
         * {@code PasswordProtection}，则通过调用 {@code getPassword} 方法获取密码。
         * 否则，如果 {@code protection} 是
         * {@code CallbackHandlerProtection}，则通过调用 CallbackHandler 获取密码。
         *
         * <p>对 {@link #getKeyStore} 的后续调用将返回与首次调用相同的对象。
         * 如果首次调用失败并抛出 KeyStoreException，后续调用也将抛出 KeyStoreException。
         *
         * <p>如果 {@code provider} 非空，则从 {@code provider} 实例化密钥库。
         * 否则，搜索所有已安装的提供程序。
         *
         * <p>对 {@link #getProtectionParameter getProtectionParameter()} 的调用将返回一个
         * {@link KeyStore.PasswordProtection PasswordProtection} 对象，封装用于调用
         * {@code load} 方法的密码。
         *
         * <p><em>注意</em>，{@link #getKeyStore} 方法在调用此方法的代码的
         * {@link AccessControlContext} 中执行。
         *
         * @return 一个新的 Builder 对象。
         * @param type 要构建的密钥库类型。
         * @param provider 密钥库实例化的提供程序（或 null）。
         * @param file 包含密钥库数据的文件。
         * @param protection 保护密钥库数据的保护参数。
         * @throws NullPointerException 如果 type、file 或 protection 为 null。
         * @throws IllegalArgumentException 如果 protection 不是
         *   PasswordProtection 或 CallbackHandlerProtection 的实例；或
         *   file 不存在或不是普通文件。
         */
        public static Builder newInstance(String type, Provider provider,
                File file, ProtectionParameter protection) {
            if ((type == null) || (file == null) || (protection == null)) {
                throw new NullPointerException();
            }
            if ((protection instanceof PasswordProtection == false) &&
                (protection instanceof CallbackHandlerProtection == false)) {
                throw new IllegalArgumentException
                ("保护必须是 PasswordProtection 或 " +
                 "CallbackHandlerProtection");
            }
            if (file.isFile() == false) {
                throw new IllegalArgumentException
                    ("文件不存在或不是普通文件: " + file);
            }
            return new FileBuilder(type, provider, file, protection,
                AccessController.getContext());
        }


                    private static final class FileBuilder extends Builder {

            private final String type;
            private final Provider provider;
            private final File file;
            private ProtectionParameter protection;
            private ProtectionParameter keyProtection;
            private final AccessControlContext context;

            private KeyStore keyStore;

            private Throwable oldException;

            FileBuilder(String type, Provider provider, File file,
                    ProtectionParameter protection,
                    AccessControlContext context) {
                this.type = type;
                this.provider = provider;
                this.file = file;
                this.protection = protection;
                this.context = context;
            }

            public synchronized KeyStore getKeyStore() throws KeyStoreException
            {
                if (keyStore != null) {
                    return keyStore;
                }
                if (oldException != null) {
                    throw new KeyStoreException
                        ("之前的 KeyStore 实例化失败",
                         oldException);
                }
                PrivilegedExceptionAction<KeyStore> action =
                        new PrivilegedExceptionAction<KeyStore>() {
                    public KeyStore run() throws Exception {
                        if (protection instanceof CallbackHandlerProtection == false) {
                            return run0();
                        }
                        // 使用 CallbackHandler 时，
                        // 如果密码错误则重新提示
                        int tries = 0;
                        while (true) {
                            tries++;
                            try {
                                return run0();
                            } catch (IOException e) {
                                if ((tries < MAX_CALLBACK_TRIES)
                                        && (e.getCause() instanceof UnrecoverableKeyException)) {
                                    continue;
                                }
                                throw e;
                            }
                        }
                    }
                    public KeyStore run0() throws Exception {
                        KeyStore ks;
                        if (provider == null) {
                            ks = KeyStore.getInstance(type);
                        } else {
                            ks = KeyStore.getInstance(type, provider);
                        }
                        InputStream in = null;
                        char[] password = null;
                        try {
                            in = new FileInputStream(file);
                            if (protection instanceof PasswordProtection) {
                                password =
                                ((PasswordProtection)protection).getPassword();
                                keyProtection = protection;
                            } else {
                                CallbackHandler handler =
                                    ((CallbackHandlerProtection)protection)
                                    .getCallbackHandler();
                                PasswordCallback callback = new PasswordCallback
                                    ("密钥库 " + file.getName() + " 的密码",
                                    false);
                                handler.handle(new Callback[] {callback});
                                password = callback.getPassword();
                                if (password == null) {
                                    throw new KeyStoreException("未提供密码");
                                }
                                callback.clearPassword();
                                keyProtection = new PasswordProtection(password);
                            }
                            ks.load(in, password);
                            return ks;
                        } finally {
                            if (in != null) {
                                in.close();
                            }
                        }
                    }
                };
                try {
                    keyStore = AccessController.doPrivileged(action, context);
                    return keyStore;
                } catch (PrivilegedActionException e) {
                    oldException = e.getCause();
                    throw new KeyStoreException
                        ("KeyStore 实例化失败", oldException);
                }
            }

            public synchronized ProtectionParameter
                        getProtectionParameter(String alias) {
                if (alias == null) {
                    throw new NullPointerException();
                }
                if (keyStore == null) {
                    throw new IllegalStateException
                        ("必须先调用 getKeyStore()");
                }
                return keyProtection;
            }
        }

        /**
         * 返回一个新的 Builder 对象。
         *
         * <p>每次调用返回的构建器上的 {@link #getKeyStore} 方法时，
         * 都会返回一个新的类型为 {@code type} 的 KeyStore 对象。
         * 其 {@link KeyStore#load(KeyStore.LoadStoreParameter) load()}
         * 方法使用封装了 {@code protection} 的
         * {@code LoadStoreParameter} 调用。
         *
         * <p>如果 {@code provider} 不为 null，则从 {@code provider} 实例化 KeyStore。
         * 否则，搜索所有已安装的提供者。
         *
         * <p>调用 {@link #getProtectionParameter getProtectionParameter()}
         * 将返回 {@code protection}。
         *
         * <p><em>注意</em>，{@link #getKeyStore} 方法在调用此方法的代码的
         * {@link AccessControlContext} 中执行。
         *
         * @return 一个新的 Builder 对象
         * @param type 要构建的 KeyStore 类型
         * @param provider 实例化 KeyStore 的提供者（或 null）
         * @param protection 保护 KeyStore 的 ProtectionParameter
         * @throws NullPointerException 如果 type 或 protection 为 null
         */
        public static Builder newInstance(final String type,
                final Provider provider, final ProtectionParameter protection) {
            if ((type == null) || (protection == null)) {
                throw new NullPointerException();
            }
            final AccessControlContext context = AccessController.getContext();
            return new Builder() {
                private volatile boolean getCalled;
                private IOException oldException;

                private final PrivilegedExceptionAction<KeyStore> action
                        = new PrivilegedExceptionAction<KeyStore>() {

                    public KeyStore run() throws Exception {
                        KeyStore ks;
                        if (provider == null) {
                            ks = KeyStore.getInstance(type);
                        } else {
                            ks = KeyStore.getInstance(type, provider);
                        }
                        LoadStoreParameter param = new SimpleLoadStoreParameter(protection);
                        if (protection instanceof CallbackHandlerProtection == false) {
                            ks.load(param);
                        } else {
                            // 使用 CallbackHandler 时，
                            // 如果密码错误则重新提示
                            int tries = 0;
                            while (true) {
                                tries++;
                                try {
                                    ks.load(param);
                                    break;
                                } catch (IOException e) {
                                    if (e.getCause() instanceof UnrecoverableKeyException) {
                                        if (tries < MAX_CALLBACK_TRIES) {
                                            continue;
                                        } else {
                                            oldException = e;
                                        }
                                    }
                                    throw e;
                                }
                            }
                        }
                        getCalled = true;
                        return ks;
                    }
                };

                public synchronized KeyStore getKeyStore()
                        throws KeyStoreException {
                    if (oldException != null) {
                        throw new KeyStoreException
                            ("之前的 KeyStore 实例化失败",
                             oldException);
                    }
                    try {
                        return AccessController.doPrivileged(action, context);
                    } catch (PrivilegedActionException e) {
                        Throwable cause = e.getCause();
                        throw new KeyStoreException
                            ("KeyStore 实例化失败", cause);
                    }
                }

                public ProtectionParameter getProtectionParameter(String alias)
                {
                    if (alias == null) {
                        throw new NullPointerException();
                    }
                    if (getCalled == false) {
                        throw new IllegalStateException
                            ("必须先调用 getKeyStore()");
                    }
                    return protection;
                }
            };
        }

    }

    static class SimpleLoadStoreParameter implements LoadStoreParameter {

        private final ProtectionParameter protection;

        SimpleLoadStoreParameter(ProtectionParameter protection) {
            this.protection = protection;
        }

        public ProtectionParameter getProtectionParameter() {
            return protection;
        }
    }

}
