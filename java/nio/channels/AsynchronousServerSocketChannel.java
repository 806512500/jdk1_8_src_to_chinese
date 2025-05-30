/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.nio.channels.spi.*;
import java.net.SocketOption;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.io.IOException;

/**
 * 用于流导向监听套接字的异步通道。
 *
 * <p> 通过调用此类的 {@link #open open} 方法创建异步服务器套接字通道。
 * 新创建的异步服务器套接字通道是打开的，但尚未绑定。
 * 通过调用 {@link #bind(SocketAddress,int) bind} 方法可以将其绑定到本地地址并配置为监听连接。
 * 一旦绑定，可以使用 {@link #accept(Object,CompletionHandler) accept} 方法
 * 开始接受连接到通道的套接字。尝试在未绑定的通道上调用 <tt>accept</tt> 方法将导致
 * 抛出 {@link NotYetBoundException}。
 *
 * <p> 此类型的通道可以由多个并发线程安全使用，但一次最多只能有一个接受操作。
 * 如果一个线程在前一个接受操作完成之前启动接受操作，则会抛出 {@link AcceptPendingException}。
 *
 * <p> 使用 {@link #setOption(SocketOption,Object) setOption} 方法配置套接字选项。
 * 此类型的通道支持以下选项：
 * <blockquote>
 * <table border summary="Socket options">
 *   <tr>
 *     <th>选项名称</th>
 *     <th>描述</th>
 *   </tr>
 *   <tr>
 *     <td> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </td>
 *     <td> 套接字接收缓冲区的大小 </td>
 *   </tr>
 *   <tr>
 *     <td> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </td>
 *     <td> 重用地址 </td>
 *   </tr>
 * </table>
 * </blockquote>
 * 可能还支持其他（实现特定的）选项。
 *
 * <p> <b>使用示例：</b>
 * <pre>
 *  final AsynchronousServerSocketChannel listener =
 *      AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(5000));
 *
 *  listener.accept(null, new CompletionHandler&lt;AsynchronousSocketChannel,Void&gt;() {
 *      public void completed(AsynchronousSocketChannel ch, Void att) {
 *          // 接受下一个连接
 *          listener.accept(null, this);
 *
 *          // 处理此连接
 *          handle(ch);
 *      }
 *      public void failed(Throwable exc, Void att) {
 *          ...
 *      }
 *  });
 * </pre>
 *
 * @since 1.7
 */

public abstract class AsynchronousServerSocketChannel
    implements AsynchronousChannel, NetworkChannel
{
    private final AsynchronousChannelProvider provider;

    /**
     * 初始化此类的新实例。
     *
     * @param  provider
     *         创建此通道的提供者
     */
    protected AsynchronousServerSocketChannel(AsynchronousChannelProvider provider) {
        this.provider = provider;
    }

    /**
     * 返回创建此通道的提供者。
     *
     * @return  创建此通道的提供者
     */
    public final AsynchronousChannelProvider provider() {
        return provider;
    }

    /**
     * 打开异步服务器套接字通道。
     *
     * <p> 通过调用 {@link
     * java.nio.channels.spi.AsynchronousChannelProvider#openAsynchronousServerSocketChannel
     * openAsynchronousServerSocketChannel} 方法在 {@link
     * java.nio.channels.spi.AsynchronousChannelProvider} 对象上创建新的通道。
     * 如果 group 参数为 <tt>null</tt>，则通过系统范围的默认提供者创建结果通道，并绑定到 <em>默认组</em>。
     *
     * @param   group
     *          新构造的通道应绑定的组，或 <tt>null</tt> 以绑定到默认组
     *
     * @return  新的异步服务器套接字通道
     *
     * @throws  ShutdownChannelGroupException
     *          如果通道组已关闭
     * @throws  IOException
     *          如果发生 I/O 错误
     */
    public static AsynchronousServerSocketChannel open(AsynchronousChannelGroup group)
        throws IOException
    {
        AsynchronousChannelProvider provider = (group == null) ?
            AsynchronousChannelProvider.provider() : group.provider();
        return provider.openAsynchronousServerSocketChannel(group);
    }

    /**
     * 打开异步服务器套接字通道。
     *
     * <p> 此方法返回一个绑定到 <em>默认组</em> 的异步服务器套接字通道。此方法等同于评估以下表达式：
     * <blockquote><pre>
     * open((AsynchronousChannelGroup)null);
     * </pre></blockquote>
     *
     * @return  新的异步服务器套接字通道
     *
     * @throws  IOException
     *          如果发生 I/O 错误
     */
    public static AsynchronousServerSocketChannel open()
        throws IOException
    {
        return open(null);
    }

    /**
     * 将通道的套接字绑定到本地地址并配置套接字以监听连接。
     *
     * <p> 调用此方法等同于以下操作：
     * <blockquote><pre>
     * bind(local, 0);
     * </pre></blockquote>
     *
     * @param   local
     *          要绑定套接字的本地地址，或 <tt>null</tt> 以绑定到自动分配的套接字地址
     *
     * @return  此通道
     *
     * @throws  AlreadyBoundException               {@inheritDoc}
     * @throws  UnsupportedAddressTypeException     {@inheritDoc}
     * @throws  SecurityException                   {@inheritDoc}
     * @throws  ClosedChannelException              {@inheritDoc}
     * @throws  IOException                         {@inheritDoc}
     */
    public final AsynchronousServerSocketChannel bind(SocketAddress local)
        throws IOException
    {
        return bind(local, 0);
    }

    /**
     * 将通道的套接字绑定到本地地址并配置套接字以监听连接。
     *
     * <p> 此方法用于在套接字和本地地址之间建立关联。一旦建立关联，套接字将保持绑定状态，直到关联的通道关闭。
     *
     * <p> {@code backlog} 参数是套接字上待处理连接的最大数量。其确切语义是实现特定的。
     * 特别是，实现可能施加最大长度或完全忽略该参数。如果 {@code backlog} 参数的值为 {@code 0} 或负值，
     * 则使用实现特定的默认值。
     *
     * @param   local
     *          要绑定套接字的本地地址，或 {@code null} 以绑定到自动分配的套接字地址
     * @param   backlog
     *          待处理连接的最大数量
     *
     * @return  此通道
     *
     * @throws  AlreadyBoundException
     *          如果套接字已绑定
     * @throws  UnsupportedAddressTypeException
     *          如果给定地址的类型不受支持
     * @throws  SecurityException
     *          如果已安装安全管理器，且其 {@link
     *          SecurityManager#checkListen checkListen} 方法拒绝操作
     * @throws  ClosedChannelException
     *          如果通道已关闭
     * @throws  IOException
     *          如果发生其他 I/O 错误
     */
    public abstract AsynchronousServerSocketChannel bind(SocketAddress local, int backlog)
        throws IOException;

    /**
     * @throws  IllegalArgumentException                {@inheritDoc}
     * @throws  ClosedChannelException                  {@inheritDoc}
     * @throws  IOException                             {@inheritDoc}
     */
    public abstract <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value)
        throws IOException;

    /**
     * 接受连接。
     *
     * <p> 此方法启动异步操作以接受对此通道套接字的连接。{@code handler} 参数是完成处理程序，
     * 当连接被接受（或操作失败）时调用。传递给完成处理程序的结果是
     * 新连接的 {@link AsynchronousSocketChannel}。
     *
     * <p> 当接受新连接时，结果的 {@code
     * AsynchronousSocketChannel} 将绑定到与此通道相同的 {@link
     * AsynchronousChannelGroup}。如果组已 {@link
     * AsynchronousChannelGroup#isShutdown shutdown} 并且接受连接，
     * 则连接将关闭，操作将以 {@code
     * IOException} 和原因 {@link ShutdownChannelGroupException} 完成。
     *
     * <p> 为了允许并发处理新连接，当立即接受新连接时，完成处理程序不会由启动线程直接调用
     * （参见 <a
     * href="AsynchronousChannelGroup.html#threading">线程</a>）。
     *
     * <p> 如果已安装安全管理器，则它会验证连接的远程端点的地址和端口号是否
     * 由安全管理器的 {@link SecurityManager#checkAccept checkAccept} 方法允许。
     * 权限检查是在此方法的调用上下文中受限的权限下执行的。如果权限检查失败，
     * 则连接将关闭，操作将以 {@link
     * SecurityException} 完成。
     *
     * @param   <A>
     *          附件的类型
     * @param   attachment
     *          要附加到 I/O 操作的对象；可以为 {@code null}
     * @param   handler
     *          用于处理结果的处理程序
     *
     * @throws  AcceptPendingException
     *          如果此通道上已有接受操作正在进行
     * @throws  NotYetBoundException
     *          如果此通道的套接字尚未绑定
     * @throws  ShutdownChannelGroupException
     *          如果通道组已终止
     */
    public abstract <A> void accept(A attachment,
                                    CompletionHandler<AsynchronousSocketChannel,? super A> handler);

    /**
     * 接受连接。
     *
     * <p> 此方法启动异步操作以接受对此通道套接字的连接。此方法的行为与
     * {@link #accept(Object, CompletionHandler)} 方法完全相同，只是不指定完成处理程序，
     * 而是返回一个表示待处理结果的 {@code Future}。如果操作成功完成，
     * {@code Future} 的 {@link Future#get() get} 方法将返回新连接的 {@link
     * AsynchronousSocketChannel}。
     *
     * @return  表示待处理结果的 {@code Future} 对象
     *
     * @throws  AcceptPendingException
     *          如果此通道上已有接受操作正在进行
     * @throws  NotYetBoundException
     *          如果此通道的套接字尚未绑定
     */
    public abstract Future<AsynchronousSocketChannel> accept();

    /**
     * {@inheritDoc}
     * <p>
     * 如果已设置安全管理器，其 {@code checkConnect} 方法将被调用，
     * 以本地地址和 {@code -1} 作为参数，以检查操作是否允许。如果操作不允许，
     * 则返回一个表示 {@link java.net.InetAddress#getLoopbackAddress loopback} 地址和
     * 通道套接字的本地端口的 {@code SocketAddress}。
     *
     * @return  套接字绑定到的 {@code SocketAddress}，或如果安全管理器拒绝，
     *          则返回表示循环地址的 {@code SocketAddress}，或如果通道的套接字未绑定，则返回 {@code null}
     *
     * @throws  ClosedChannelException     {@inheritDoc}
     * @throws  IOException                {@inheritDoc}
     */
    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;
}
