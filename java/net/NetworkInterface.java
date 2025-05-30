
/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import sun.security.action.*;
import java.security.AccessController;

/**
 * 此类表示由名称和分配给此接口的IP地址列表组成的网络接口。
 * 它用于标识加入多播组的本地接口。
 *
 * 接口通常以名称如 "le0" 为人所知。
 *
 * @since 1.4
 */
public final class NetworkInterface {
    private String name;
    private String displayName;
    private int index;
    private InetAddress addrs[];
    private InterfaceAddress bindings[];
    private NetworkInterface childs[];
    private NetworkInterface parent = null;
    private boolean virtual = false;
    private static final NetworkInterface defaultInterface;
    private static final int defaultIndex; /* defaultInterface 的索引 */

    static {
        AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("net");
                    return null;
                }
            });

        init();
        defaultInterface = DefaultInterface.getDefault();
        if (defaultInterface != null) {
            defaultIndex = defaultInterface.getIndex();
        } else {
            defaultIndex = 0;
        }
    }

    /**
     * 返回一个索引设置为 0 且名称为 null 的 NetworkInterface 对象。
     * 在 MulticastSocket 上设置这样的接口将导致内核选择一个接口来发送多播数据包。
     *
     */
    NetworkInterface() {
    }

    NetworkInterface(String name, int index, InetAddress[] addrs) {
        this.name = name;
        this.index = index;
        this.addrs = addrs;
    }

    /**
     * 获取此网络接口的名称。
     *
     * @return 此网络接口的名称
     */
    public String getName() {
            return name;
    }

    /**
     * 返回一个包含此网络接口绑定的所有或部分 InetAddresses 的 Enumeration。
     * <p>
     * 如果存在安全管理器，其 {@code checkConnect} 方法将被调用以检查每个 InetAddress。
     * 只有 {@code checkConnect} 不抛出 SecurityException 的 InetAddresses 将被返回到 Enumeration 中。
     * 但是，如果调用者具有 {@link NetPermission}("getNetworkInformation") 权限，则所有 InetAddresses 都将被返回。
     * @return 一个包含此网络接口绑定的所有或部分 InetAddresses 的 Enumeration 对象
     */
    public Enumeration<InetAddress> getInetAddresses() {

        class checkedAddresses implements Enumeration<InetAddress> {

            private int i=0, count=0;
            private InetAddress local_addrs[];

            checkedAddresses() {
                local_addrs = new InetAddress[addrs.length];
                boolean trusted = true;

                SecurityManager sec = System.getSecurityManager();
                if (sec != null) {
                    try {
                        sec.checkPermission(new NetPermission("getNetworkInformation"));
                    } catch (SecurityException e) {
                        trusted = false;
                    }
                }
                for (int j=0; j<addrs.length; j++) {
                    try {
                        if (sec != null && !trusted) {
                            sec.checkConnect(addrs[j].getHostAddress(), -1);
                        }
                        local_addrs[count++] = addrs[j];
                    } catch (SecurityException e) { }
                }

            }

            public InetAddress nextElement() {
                if (i < count) {
                    return local_addrs[i++];
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return (i < count);
            }
        }
        return new checkedAddresses();

    }

    /**
     * 获取此网络接口的所有或部分 {@code InterfaceAddresses} 的列表。
     * <p>
     * 如果存在安全管理器，其 {@code checkConnect} 方法将被调用以检查每个 InterfaceAddress 的 InetAddress。
     * 只有 {@code checkConnect} 不抛出 SecurityException 的 InterfaceAddresses 将被返回到列表中。
     *
     * @return 一个包含此网络接口的所有或部分 InterfaceAddresss 的 {@code List} 对象
     * @since 1.6
     */
    public java.util.List<InterfaceAddress> getInterfaceAddresses() {
        java.util.List<InterfaceAddress> lst = new java.util.ArrayList<InterfaceAddress>(1);
        SecurityManager sec = System.getSecurityManager();
        for (int j=0; j<bindings.length; j++) {
            try {
                if (sec != null) {
                    sec.checkConnect(bindings[j].getAddress().getHostAddress(), -1);
                }
                lst.add(bindings[j]);
            } catch (SecurityException e) { }
        }
        return lst;
    }

    /**
     * 获取此网络接口的所有子接口（也称为虚拟接口）的 Enumeration。
     * <p>
     * 例如，eth0:1 将是 eth0 的子接口。
     *
     * @return 一个包含此网络接口所有子接口的 Enumeration 对象
     * @since 1.6
     */
    public Enumeration<NetworkInterface> getSubInterfaces() {
        class subIFs implements Enumeration<NetworkInterface> {

            private int i=0;

            subIFs() {
            }

            public NetworkInterface nextElement() {
                if (i < childs.length) {
                    return childs[i++];
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return (i < childs.length);
            }
        }
        return new subIFs();

    }

    /**
     * 如果此接口是子接口，则返回其父 NetworkInterface，否则返回 {@code null}。
     *
     * @return 此接口所属的 {@code NetworkInterface}，如果此接口是物理接口或没有父接口，则返回 {@code null}
     * @since 1.6
     */
    public NetworkInterface getParent() {
        return parent;
    }

    /**
     * 返回此网络接口的索引。索引是一个大于或等于零的整数，或未知时为 {@code -1}。
     * 这是一个系统特定的值，具有相同名称的接口在不同的机器上可能具有不同的索引。
     *
     * @return 此网络接口的索引，如果索引未知则返回 {@code -1}
     * @see #getByIndex(int)
     * @since 1.7
     */
    public int getIndex() {
        return index;
    }

    /**
     * 获取此网络接口的显示名称。
     * 显示名称是描述网络设备的人类可读字符串。
     *
     * @return 一个非空字符串，表示此网络接口的显示名称，如果没有可用的显示名称则返回 null
     */
    public String getDisplayName() {
        /* 严格的 TCK 符合性 */
        return "".equals(displayName) ? null : displayName;
    }

    /**
     * 搜索具有指定名称的网络接口。
     *
     * @param   name
     *          网络接口的名称。
     *
     * @return  具有指定名称的 {@code NetworkInterface}，如果不存在具有指定名称的网络接口，则返回 {@code null}。
     *
     * @throws  SocketException
     *          如果发生 I/O 错误。
     *
     * @throws  NullPointerException
     *          如果指定的名称为 {@code null}。
     */
    public static NetworkInterface getByName(String name) throws SocketException {
        if (name == null)
            throw new NullPointerException();
        return getByName0(name);
    }

    /**
     * 根据索引获取网络接口。
     *
     * @param index 一个整数，表示接口的索引
     * @return 从其索引获取的 NetworkInterface，如果系统中没有具有该索引的接口，则返回 {@code null}
     * @throws  SocketException  如果发生 I/O 错误。
     * @throws  IllegalArgumentException 如果索引为负值
     * @see #getIndex()
     * @since 1.7
     */
    public static NetworkInterface getByIndex(int index) throws SocketException {
        if (index < 0)
            throw new IllegalArgumentException("接口索引不能为负值");
        return getByIndex0(index);
    }

    /**
     * 搜索具有指定 Internet 协议 (IP) 地址的网络接口。
     * <p>
     * 如果指定的 IP 地址绑定到多个网络接口，则返回哪个网络接口是未定义的。
     *
     * @param   addr
     *          要搜索的 {@code InetAddress}。
     *
     * @return  一个 {@code NetworkInterface}，如果不存在具有指定 IP 地址的网络接口，则返回 {@code null}。
     *
     * @throws  SocketException
     *          如果发生 I/O 错误。
     *
     * @throws  NullPointerException
     *          如果指定的地址为 {@code null}。
     */
    public static NetworkInterface getByInetAddress(InetAddress addr) throws SocketException {
        if (addr == null) {
            throw new NullPointerException();
        }
        if (addr instanceof Inet4Address) {
            Inet4Address inet4Address = (Inet4Address) addr;
            if (inet4Address.holder.family != InetAddress.IPv4) {
                throw new IllegalArgumentException("无效的家族类型: "
                        + inet4Address.holder.family);
            }
        } else if (addr instanceof Inet6Address) {
            Inet6Address inet6Address = (Inet6Address) addr;
            if (inet6Address.holder.family != InetAddress.IPv6) {
                throw new IllegalArgumentException("无效的家族类型: "
                        + inet6Address.holder.family);
            }
        } else {
            throw new IllegalArgumentException("无效的地址类型: " + addr);
        }
        return getByInetAddress0(addr);
    }

    /**
     * 返回此机器上的所有接口。枚举至少包含一个元素，可能是仅支持此机器内实体间通信的回环接口。
     *
     * 注意：可以使用 getNetworkInterfaces()+getInetAddresses() 获取此节点的所有 IP 地址
     *
     * @return 一个包含此机器上所有 NetworkInterfaces 的枚举
     * @exception  SocketException  如果发生 I/O 错误。
     */

    public static Enumeration<NetworkInterface> getNetworkInterfaces()
        throws SocketException {
        final NetworkInterface[] netifs = getAll();

        // 指定如果没有网络接口则返回 null
        if (netifs == null)
            return null;

        return new Enumeration<NetworkInterface>() {
            private int i = 0;
            public NetworkInterface nextElement() {
                if (netifs != null && i < netifs.length) {
                    NetworkInterface netif = netifs[i++];
                    return netif;
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return (netifs != null && i < netifs.length);
            }
        };
    }

    private native static NetworkInterface[] getAll()
        throws SocketException;

    private native static NetworkInterface getByName0(String name)
        throws SocketException;

    private native static NetworkInterface getByIndex0(int index)
        throws SocketException;

    private native static NetworkInterface getByInetAddress0(InetAddress addr)
        throws SocketException;

    /**
     * 返回网络接口是否处于运行状态。
     *
     * @return  如果接口处于运行状态，则返回 {@code true}。
     * @exception       SocketException 如果发生 I/O 错误。
     * @since 1.6
     */

    public boolean isUp() throws SocketException {
        return isUp0(name, index);
    }

    /**
     * 返回网络接口是否为回环接口。
     *
     * @return  如果接口为回环接口，则返回 {@code true}。
     * @exception       SocketException 如果发生 I/O 错误。
     * @since 1.6
     */

    public boolean isLoopback() throws SocketException {
        return isLoopback0(name, index);
    }

    /**
     * 返回网络接口是否为点对点接口。
     * 典型的点对点接口是通过调制解调器的 PPP 连接。
     *
     * @return  如果接口为点对点接口，则返回 {@code true}。
     * @exception       SocketException 如果发生 I/O 错误。
     * @since 1.6
     */

    public boolean isPointToPoint() throws SocketException {
        return isP2P0(name, index);
    }

    /**
     * 返回网络接口是否支持多播。
     *
     * @return  如果接口支持多播，则返回 {@code true}。
     * @exception       SocketException 如果发生 I/O 错误。
     * @since 1.6
     */

    public boolean supportsMulticast() throws SocketException {
        return supportsMulticast0(name, index);
    }

    /**
     * 返回接口的硬件地址（通常是 MAC 地址），如果接口具有硬件地址且当前权限允许访问，则返回。
     * 如果设置了安全管理器，则调用者必须具有 {@link NetPermission}("getNetworkInformation") 权限。
     *
     * @return  包含地址的字节数组，如果地址不存在、不可访问或设置了安全管理器且调用者没有
     *          NetPermission("getNetworkInformation") 权限，则返回 {@code null}。
     *
     * @exception       SocketException 如果发生 I/O 错误。
     * @since 1.6
     */
    public byte[] getHardwareAddress() throws SocketException {
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            try {
                sec.checkPermission(new NetPermission("getNetworkInformation"));
            } catch (SecurityException e) {
                if (!getInetAddresses().hasMoreElements()) {
                    // 没有连接权限到任何本地地址
                    return null;
                }
            }
        }
        for (InetAddress addr : addrs) {
            if (addr instanceof Inet4Address) {
                return getMacAddr0(((Inet4Address)addr).getAddress(), name, index);
            }
        }
        return getMacAddr0(null, name, index);
    }


                /**
     * 返回此接口的最大传输单元 (MTU)。
     *
     * @return 该接口的 MTU 值。
     * @exception       SocketException 如果发生 I/O 错误。
     * @since 1.6
     */
    public int getMTU() throws SocketException {
        return getMTU0(name, index);
    }

    /**
     * 返回此接口是否为虚拟接口（也称为子接口）。
     * 在某些系统上，虚拟接口是作为物理接口的子接口创建的，并赋予不同的设置（如地址或 MTU）。通常，接口的名称会是父接口的名称后跟一个冒号 (:) 和一个标识子接口的数字，因为一个物理接口可以附加多个虚拟接口。
     *
     * @return 如果此接口是虚拟接口，则返回 {@code true}。
     * @since 1.6
     */
    public boolean isVirtual() {
        return virtual;
    }

    private native static boolean isUp0(String name, int ind) throws SocketException;
    private native static boolean isLoopback0(String name, int ind) throws SocketException;
    private native static boolean supportsMulticast0(String name, int ind) throws SocketException;
    private native static boolean isP2P0(String name, int ind) throws SocketException;
    private native static byte[] getMacAddr0(byte[] inAddr, String name, int ind) throws SocketException;
    private native static int getMTU0(String name, int ind) throws SocketException;

    /**
     * 将此对象与指定的对象进行比较。
     * 结果为 {@code true} 当且仅当参数不为 {@code null} 并且它表示与该对象相同的 NetworkInterface。
     * <p>
     * 两个 {@code NetworkInterface} 实例表示相同的 NetworkInterface 当且仅当它们的名称和地址相同。
     *
     * @param   obj   要比较的对象。
     * @return  如果对象相同，则返回 {@code true}；
     *          否则返回 {@code false}。
     * @see     java.net.InetAddress#getAddress()
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkInterface)) {
            return false;
        }
        NetworkInterface that = (NetworkInterface)obj;
        if (this.name != null ) {
            if (!this.name.equals(that.name)) {
                return false;
            }
        } else {
            if (that.name != null) {
                return false;
            }
        }

        if (this.addrs == null) {
            return that.addrs == null;
        } else if (that.addrs == null) {
            return false;
        }

        /* 两个地址都不为空。比较地址数量 */

        if (this.addrs.length != that.addrs.length) {
            return false;
        }

        InetAddress[] thatAddrs = that.addrs;
        int count = thatAddrs.length;

        for (int i=0; i<count; i++) {
            boolean found = false;
            for (int j=0; j<count; j++) {
                if (addrs[i].equals(thatAddrs[j])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return name == null? 0: name.hashCode();
    }

    public String toString() {
        String result = "name:";
        result += name == null? "null": name;
        if (displayName != null) {
            result += " (" + displayName + ")";
        }
        return result;
    }

    private static native void init();

    /**
     * 返回此系统的默认网络接口
     *
     * @return 默认接口
     */
    static NetworkInterface getDefault() {
        return defaultInterface;
    }
}
