
/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import sun.misc.JavaSecurityAccess;
import sun.misc.JavaSecurityProtectionDomainAccess;
import static sun.misc.JavaSecurityProtectionDomainAccess.ProtectionDomainCache;
import sun.misc.SharedSecrets;
import sun.security.util.Debug;
import sun.security.util.SecurityConstants;

/**
 * ProtectionDomain 类封装了域的特性，该域包含了一组类，这些类的实例在代表给定的一组 Principal 执行时会被授予一组权限。
 * <p>
 * 当构造 ProtectionDomain 时，可以绑定一个静态的权限集；这些权限无论当前的 Policy 如何都会被授予该域。然而，为了支持动态安全策略，
 * ProtectionDomain 也可以被构造为在每次检查权限时由当前 Policy 动态映射到一组权限。
 * <p>
 *
 * @author Li Gong
 * @author Roland Schemers
 * @author Gary Ellison
 */

public class ProtectionDomain {
    private static class JavaSecurityAccessImpl implements JavaSecurityAccess {

        private JavaSecurityAccessImpl() {
        }

        @Override
        public <T> T doIntersectionPrivilege(
                PrivilegedAction<T> action,
                final AccessControlContext stack,
                final AccessControlContext context) {
            if (action == null) {
                throw new NullPointerException();
            }

            return AccessController.doPrivileged(
                action,
                getCombinedACC(context, stack)
            );
        }

        @Override
        public <T> T doIntersectionPrivilege(
                PrivilegedAction<T> action,
                AccessControlContext context) {
            return doIntersectionPrivilege(action,
                AccessController.getContext(), context);
        }

        private static AccessControlContext getCombinedACC(AccessControlContext context, AccessControlContext stack) {
            AccessControlContext acc = new AccessControlContext(context, stack.getCombiner(), true);

            return new AccessControlContext(stack.getContext(), acc).optimize();
        }
    }

    static {
        // 在 SharedSecrets 中设置 JavaSecurityAccess
        SharedSecrets.setJavaSecurityAccess(new JavaSecurityAccessImpl());
    }

    /* CodeSource */
    private CodeSource codesource ;

    /* 创建此保护域的 ClassLoader */
    private ClassLoader classloader;

    /* 在此保护域内运行的 Principal */
    private Principal[] principals;

    /* 授予此保护域的权利 */
    private PermissionCollection permissions;

    /* 如果权限对象包含 AllPermission */
    private boolean hasAllPerm = false;

    /* 权限集合是静态的（1.4 之前的构造函数）
       或动态的（通过策略刷新） */
    private boolean staticPermissions;

    /*
     * 当 ProtectionDomain 存储在 Map 中时用作键的对象。
     */
    final Key key = new Key();

    private static final Debug debug = Debug.getInstance("domain");

    /**
     * 使用给定的 CodeSource 和 Permissions 创建一个新的 ProtectionDomain。如果权限对象不为 null，则
     * 会在传递的 Permissions 对象上调用 {@code setReadOnly()}。授予此域的唯一权限是指定的权限；
     * 不会咨询当前的 Policy。
     *
     * @param codesource 与此域关联的代码源
     * @param permissions 授予此域的权限
     */
    public ProtectionDomain(CodeSource codesource,
                            PermissionCollection permissions) {
        this.codesource = codesource;
        if (permissions != null) {
            this.permissions = permissions;
            this.permissions.setReadOnly();
            if (permissions instanceof Permissions &&
                ((Permissions)permissions).allPermission != null) {
                hasAllPerm = true;
            }
        }
        this.classloader = null;
        this.principals = new Principal[0];
        staticPermissions = true;
    }

    /**
     * 使用给定的 CodeSource、Permissions、ClassLoader 和 Principal 数组创建一个新的 ProtectionDomain。
     * 如果权限对象不为 null，则会在传递的 Permissions 对象上调用 {@code setReadOnly()}。授予此域的权限是动态的；
     * 它们包括传递给此构造函数的静态权限，以及当前 Policy 在检查权限时授予此域的任何权限。
     * <p>
     * 此构造函数通常由 {@link SecureClassLoader ClassLoaders} 和
     * {@link DomainCombiner DomainCombiners} 使用，它们委托给 {@code Policy} 以积极地关联授予此域的权限。
     * 此构造函数为 Policy 提供者提供了增强提供的 PermissionCollection 以反映策略更改的机会。
     * <p>
     *
     * @param codesource 与此域关联的代码源
     * @param permissions 授予此域的权限
     * @param classloader 与此域关联的 ClassLoader
     * @param principals 与此域关联的 Principal 数组。数组的内容会被复制以防止后续修改。
     * @see Policy#refresh
     * @see Policy#getPermissions(ProtectionDomain)
     * @since 1.4
     */
    public ProtectionDomain(CodeSource codesource,
                            PermissionCollection permissions,
                            ClassLoader classloader,
                            Principal[] principals) {
        this.codesource = codesource;
        if (permissions != null) {
            this.permissions = permissions;
            this.permissions.setReadOnly();
            if (permissions instanceof Permissions &&
                ((Permissions)permissions).allPermission != null) {
                hasAllPerm = true;
            }
        }
        this.classloader = classloader;
        this.principals = (principals != null ? principals.clone():
                           new Principal[0]);
        staticPermissions = false;
    }

    /**
     * 返回此域的 CodeSource。
     * @return 此域的 CodeSource，可能为 null。
     * @since 1.2
     */
    public final CodeSource getCodeSource() {
        return this.codesource;
    }


    /**
     * 返回此域的 ClassLoader。
     * @return 此域的 ClassLoader，可能为 null。
     *
     * @since 1.4
     */
    public final ClassLoader getClassLoader() {
        return this.classloader;
    }


    /**
     * 返回此域的 Principal 数组。
     * @return 此域的 Principal 数组，不为 null。每次调用此方法时都会返回一个新的数组。
     *
     * @since 1.4
     */
    public final Principal[] getPrincipals() {
        return this.principals.clone();
    }

    /**
     * 返回授予此域的静态权限。
     *
     * @return 此域的静态权限集，可能为 null。
     * @see Policy#refresh
     * @see Policy#getPermissions(ProtectionDomain)
     */
    public final PermissionCollection getPermissions() {
        return permissions;
    }

    /**
     * 检查并确定此 ProtectionDomain 是否隐含 Permission 对象中表达的权限。
     * <p>
     * 评估的权限集取决于 ProtectionDomain 是使用静态权限集构造的，还是绑定到动态映射的权限集。
     * <p>
     * 如果 ProtectionDomain 是使用
     * {@link #ProtectionDomain(CodeSource, PermissionCollection)
     * 静态绑定} 的 PermissionCollection 构造的，那么权限只会与构造时提供的 PermissionCollection 进行检查。
     * <p>
     * 然而，如果 ProtectionDomain 是使用支持
     * {@link #ProtectionDomain(CodeSource, PermissionCollection,
     * ClassLoader, java.security.Principal[]) 动态绑定} 权限的构造函数变体构造的，那么权限将与构造时提供的
     * PermissionCollection 和当前 Policy 绑定的组合进行检查。
     * <p>
     *
     * @param permission 要检查的 Permission 对象。
     *
     * @return 如果 "permission" 对此 ProtectionDomain 是隐含的，则返回 true。
     */
    public boolean implies(Permission permission) {

        if (hasAllPerm) {
            // 内部权限集合已经包含 AllPermission -
            // 无需查询策略
            return true;
        }

        if (!staticPermissions &&
            Policy.getPolicyNoCheck().implies(this, permission))
            return true;
        if (permissions != null)
            return permissions.implies(permission);

        return false;
    }

    // 由 VM 调用 -- 不要删除
    boolean impliesCreateAccessControlContext() {
        return implies(SecurityConstants.CREATE_ACC_PERMISSION);
    }

    /**
     * 将 ProtectionDomain 转换为字符串。
     */
    @Override public String toString() {
        String pals = "<no principals>";
        if (principals != null && principals.length > 0) {
            StringBuilder palBuf = new StringBuilder("(principals ");

            for (int i = 0; i < principals.length; i++) {
                palBuf.append(principals[i].getClass().getName() +
                            " \"" + principals[i].getName() +
                            "\"");
                if (i < principals.length-1)
                    palBuf.append(",\n");
                else
                    palBuf.append(")\n");
            }
            pals = palBuf.toString();
        }

        // 检查策略是否已设置；我们不想在这里过早加载策略
        PermissionCollection pc = Policy.isSet() && seeAllp() ?
                                      mergePermissions():
                                      getPermissions();

        return "ProtectionDomain "+
            " "+codesource+"\n"+
            " "+classloader+"\n"+
            " "+pals+"\n"+
            " "+pc+"\n";
    }

    /**
     * 在以下情况下返回 true（合并策略权限）：
     *
     * . SecurityManager 为 null
     *
     * . SecurityManager 不为 null，
     *          debug 不为 null，
     *          SecurityManager 实现位于引导类路径中，
     *          策略实现位于引导类路径中
     *          （引导类路径限制避免递归）
     *
     * . SecurityManager 不为 null，
     *          debug 为 null，
     *          调用者具有 Policy.getPolicy 权限
     */
    private static boolean seeAllp() {
        SecurityManager sm = System.getSecurityManager();

        if (sm == null) {
            return true;
        } else {
            if (debug != null) {
                if (sm.getClass().getClassLoader() == null &&
                    Policy.getPolicyNoCheck().getClass().getClassLoader()
                                                                == null) {
                    return true;
                }
            } else {
                try {
                    sm.checkPermission(SecurityConstants.GET_POLICY_PERMISSION);
                    return true;
                } catch (SecurityException se) {
                    // 继续并返回 false
                }
            }
        }

        return false;
    }

    private PermissionCollection mergePermissions() {
        if (staticPermissions)
            return permissions;

        PermissionCollection perms =
            java.security.AccessController.doPrivileged
            (new java.security.PrivilegedAction<PermissionCollection>() {
                    public PermissionCollection run() {
                        Policy p = Policy.getPolicyNoCheck();
                        return p.getPermissions(ProtectionDomain.this);
                    }
                });

        Permissions mergedPerms = new Permissions();
        int swag = 32;
        int vcap = 8;
        Enumeration<Permission> e;
        List<Permission> pdVector = new ArrayList<>(vcap);
        List<Permission> plVector = new ArrayList<>(swag);

        //
        // 构建域权限的向量以供后续合并
        if (permissions != null) {
            synchronized (permissions) {
                e = permissions.elements();
                while (e.hasMoreElements()) {
                    pdVector.add(e.nextElement());
                }
            }
        }

        //
        // 构建策略权限的向量以供后续合并
        if (perms != null) {
            synchronized (perms) {
                e = perms.elements();
                while (e.hasMoreElements()) {
                    plVector.add(e.nextElement());
                    vcap++;
                }
            }
        }

        if (perms != null && permissions != null) {
            //
            // 从策略中删除重复项。除非自 pd 创建以来发生了刷新，否则这应该导致一个空向量。
            synchronized (permissions) {
                e = permissions.elements();   // 域 vs 策略
                while (e.hasMoreElements()) {
                    Permission pdp = e.nextElement();
                    Class<?> pdpClass = pdp.getClass();
                    String pdpActions = pdp.getActions();
                    String pdpName = pdp.getName();
                    for (int i = 0; i < plVector.size(); i++) {
                        Permission pp = plVector.get(i);
                        if (pdpClass.isInstance(pp)) {
                            // 一些权限的 equals() 方法有一些副作用，因此这种手动比较就足够了。
                            if (pdpName.equals(pp.getName()) &&
                                pdpActions.equals(pp.getActions())) {
                                plVector.remove(i);
                                break;
                            }
                        }
                    }
                }
            }
        }


                    if (perms != null) {
            // 添加到合并权限和权限的顺序
            // 需要保留 bugfix 4301064

            for (int i = plVector.size() - 1; i >= 0; i--) {
                mergedPerms.add(plVector.get(i));
            }
        }
        if (permissions != null) {
            for (int i = pdVector.size() - 1; i >= 0; i--) {
                mergedPerms.add(pdVector.get(i));
            }
        }

        return mergedPerms;
    }

    /**
     * 用于在 Map 中存储 ProtectionDomains 作为键。
     */
    final class Key {}

    static {
        SharedSecrets.setJavaSecurityProtectionDomainAccess(
            new JavaSecurityProtectionDomainAccess() {
                @Override
                public ProtectionDomainCache getProtectionDomainCache() {
                    return new ProtectionDomainCache() {
                        private final Map<Key, PermissionCollection> map =
                            Collections.synchronizedMap
                                (new WeakHashMap<Key, PermissionCollection>());
                        public void put(ProtectionDomain pd,
                            PermissionCollection pc) {
                            map.put((pd == null ? null : pd.key), pc);
                        }
                        public PermissionCollection get(ProtectionDomain pd) {
                            return pd == null ? map.get(null) : map.get(pd.key);
                        }
                    };
                }

                @Override
                public boolean getStaticPermissionsField(ProtectionDomain pd) {
                    return pd.staticPermissions;
                }
            });
    }
}
