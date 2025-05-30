
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

package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

/**
 * 一个对象，可用于在文件系统中定位文件。它通常表示一个系统依赖的文件路径。
 *
 * <p> {@code Path} 表示一个层次结构的路径，由一系列目录和文件名元素组成，这些元素由一个特殊的分隔符或定界符分隔。一个 <em>根组件</em>，用于标识文件系统的层次结构，也可能存在。距离目录层次结构的 <em>最远</em> 的名称元素是文件或目录的名称。其他名称元素是目录名称。一个 {@code Path} 可以表示一个根，一个根和一系列名称，或者仅仅是一个或多个名称元素。一个 {@code Path} 如果仅由一个空名称元素组成，则被认为是 <i>空路径</i>。使用 <i>空路径</i> 访问文件等同于访问文件系统的默认目录。{@code Path} 定义了 {@link #getFileName() getFileName}、{@link #getParent getParent}、{@link #getRoot getRoot} 和 {@link #subpath subpath} 方法来访问路径组件或其名称元素的子序列。
 *
 * <p> 除了访问路径组件外，{@code Path} 还定义了 {@link #resolve(Path) resolve} 和 {@link #resolveSibling(Path) resolveSibling} 方法来组合路径。{@link #relativize relativize} 方法可以用于构造两个路径之间的相对路径。路径可以 {@link #compareTo 比较}，并使用 {@link #startsWith startsWith} 和 {@link #endsWith endsWith} 方法进行测试。
 *
 * <p> 该接口扩展了 {@link Watchable} 接口，因此可以通过路径定位的目录可以 {@link #register 注册} 到 {@link WatchService} 并监视目录中的条目。 </p>
 *
 * <p> <b>警告：</b> 该接口仅打算由那些开发自定义文件系统实现的人实现。未来版本中可能会向此接口添加方法。 </p>
 *
 * <h2>访问文件</h2>
 * <p> 可以使用 {@link Files} 类与路径一起操作文件、目录和其他类型的文件。例如，假设我们想要一个 {@link java.io.BufferedReader} 来从文件 "{@code access.log}" 读取文本。该文件位于相对于当前工作目录的目录 "{@code logs}" 中，并且是 UTF-8 编码的。
 * <pre>
 *     Path path = FileSystems.getDefault().getPath("logs", "access.log");
 *     BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
 * </pre>
 *
 * <a name="interop"></a><h2>互操作性</h2>
 * <p> 与默认 {@link java.nio.file.spi.FileSystemProvider 提供者} 关联的路径通常与 {@link java.io.File java.io.File} 类互操作。由其他提供者创建的路径可能无法与 {@code java.io.File} 表示的抽象路径名互操作。可以使用 {@link java.io.File#toPath toPath} 方法从 {@code java.io.File} 对象表示的抽象路径名中获取 {@code Path}。结果的 {@code Path} 可以用于操作与 {@code java.io.File} 对象相同的文件。此外，{@link #toFile toFile} 方法可用于从 {@code Path} 的字符串表示构造一个 {@code File}。
 *
 * <h2>并发</h2>
 * <p> 该接口的实现是不可变的，可以安全地由多个并发线程使用。
 *
 * @since 1.7
 * @see Paths
 */

public interface Path
    extends Comparable<Path>, Iterable<Path>, Watchable
{
    /**
     * 返回创建此对象的文件系统。
     *
     * @return 创建此对象的文件系统
     */
    FileSystem getFileSystem();

    /**
     * 告诉此路径是否为绝对路径。
     *
     * <p> 绝对路径是完整的，不需要与其他路径信息结合即可定位文件。
     *
     * @return 如果此路径为绝对路径，则返回 {@code true}，否则返回 {@code false}
     */
    boolean isAbsolute();

    /**
     * 返回此路径的根组件作为 {@code Path} 对象，如果此路径没有根组件，则返回 {@code null}。
     *
     * @return 表示此路径的根组件的路径，或 {@code null}
     */
    Path getRoot();

    /**
     * 返回此路径表示的文件或目录的名称作为 {@code Path} 对象。文件名是目录层次结构中 <em>最远</em> 的元素。
     *
     * @return 表示文件或目录名称的路径，或如果此路径没有元素，则返回 {@code null}
     */
    Path getFileName();

    /**
     * 返回 <em>父路径</em>，如果此路径没有父路径，则返回 {@code null}。
     *
     * <p> 该路径对象的父路径由此路径的根组件（如果有）和路径中的每个元素组成，但不包括目录层次结构中 <em>最远</em> 的元素。此方法不访问文件系统；路径或其父路径可能不存在。此外，此方法不会消除某些实现中可能使用的特殊名称，例如 "." 和 ".."。例如，在 UNIX 上，"{@code /a/b/c}" 的父路径是 "{@code /a/b}"，而 {@code "x/y/."} 的父路径是 "{@code x/y}"。此方法可以与 {@link #normalize normalize} 方法一起使用，以消除冗余名称，适用于需要 <em>shell-like</em> 导航的情况。
     *
     * <p> 如果此路径有一个或多个元素，且没有根组件，则此方法等同于评估以下表达式：
     * <blockquote><pre>
     * subpath(0,&nbsp;getNameCount()-1);
     * </pre></blockquote>
     *
     * @return 表示路径的父路径
     */
    Path getParent();

    /**
     * 返回路径中的名称元素数量。
     *
     * @return 路径中的元素数量，或如果此路径仅表示根组件，则返回 {@code 0}
     */
    int getNameCount();

    /**
     * 返回此路径的名称元素作为 {@code Path} 对象。
     *
     * <p> {@code index} 参数是要返回的名称元素的索引。目录层次结构中 <em>最接近</em> 根的元素的索引为 {@code 0}。距离根 <em>最远</em> 的元素的索引为 {@link #getNameCount count}{@code -1}。
     *
     * @param   index
     *          元素的索引
     *
     * @return  名称元素
     *
     * @throws  IllegalArgumentException
     *          如果 {@code index} 为负数，{@code index} 大于或等于元素数量，或此路径没有名称元素
     */
    Path getName(int index);

    /**
     * 返回一个相对的 {@code Path}，它是此路径的名称元素的子序列。
     *
     * <p> {@code beginIndex} 和 {@code endIndex} 参数指定名称元素的子序列。目录层次结构中 <em>最接近</em> 根的名称的索引为 {@code 0}。距离根 <em>最远</em> 的名称的索引为 {@link #getNameCount count}{@code -1}。返回的 {@code Path} 对象包含从 {@code beginIndex} 开始到 {@code endIndex-1} 的名称元素。
     *
     * @param   beginIndex
     *          第一个元素的索引，包含
     * @param   endIndex
     *          最后一个元素的索引，不包含
     *
     * @return  一个新的 {@code Path} 对象，它是此 {@code Path} 中名称元素的子序列
     *
     * @throws  IllegalArgumentException
     *          如果 {@code beginIndex} 为负数，或大于或等于元素数量。如果 {@code endIndex} 小于或等于 {@code beginIndex}，或大于元素数量。
     */
    Path subpath(int beginIndex, int endIndex);

    /**
     * 测试此路径是否以给定路径开头。
     *
     * <p> 如果此路径的根组件以给定路径的根组件开头，并且此路径以与给定路径相同的名称元素开头，则此路径 <em>开头</em> 为给定路径。如果给定路径的名称元素多于此路径，则返回 {@code false}。
     *
     * <p> 此路径的根组件是否以给定路径的根组件开头是文件系统特定的。如果此路径没有根组件而给定路径有根组件，则此路径不以给定路径开头。
     *
     * <p> 如果给定路径与不同的 {@code FileSystem} 关联，则返回 {@code false}。
     *
     * @param   other
     *          给定路径
     *
     * @return  如果此路径以给定路径开头，则返回 {@code true}；否则返回 {@code false}
     */
    boolean startsWith(Path other);

    /**
     * 测试此路径是否以给定路径字符串转换的 {@code Path} 开头，具体方式与 {@link #startsWith(Path) startsWith(Path)} 方法指定的完全相同。例如，在 UNIX 上，路径 "{@code foo/bar}" 以 "{@code foo}" 和 "{@code foo/bar}" 开头。它不以 "{@code f}" 或 "{@code fo}" 开头。
     *
     * @param   other
     *          给定路径字符串
     *
     * @return  如果此路径以给定路径开头，则返回 {@code true}；否则返回 {@code false}
     *
     * @throws  InvalidPathException
     *          如果路径字符串无法转换为 Path。
     */
    boolean startsWith(String other);

    /**
     * 测试此路径是否以给定路径结尾。
     *
     * <p> 如果给定路径有 <em>N</em> 个元素，且没有根组件，且此路径有 <em>N</em> 个或更多元素，则此路径以给定路径结尾，如果每个路径的最后 <em>N</em> 个元素，从距离根 <em>最远</em> 的元素开始，都相等。
     *
     * <p> 如果给定路径有根组件，则此路径以给定路径结尾，如果此路径的根组件以给定路径的根组件结尾，并且两个路径的相应元素相等。此路径的根组件是否以给定路径的根组件结尾是文件系统特定的。如果此路径没有根组件而给定路径有根组件，则此路径不以给定路径结尾。
     *
     * <p> 如果给定路径与不同的 {@code FileSystem} 关联，则返回 {@code false}。
     *
     * @param   other
     *          给定路径
     *
     * @return  如果此路径以给定路径结尾，则返回 {@code true}；否则返回 {@code false}
     */
    boolean endsWith(Path other);

    /**
     * 测试此路径是否以给定路径字符串转换的 {@code Path} 结尾，具体方式与 {@link #endsWith(Path) endsWith(Path)} 方法指定的完全相同。例如，在 UNIX 上，路径 "{@code foo/bar}" 以 "{@code foo/bar}" 和 "{@code bar}" 结尾。它不以 "{@code r}" 或 "{@code /bar}" 结尾。注意，尾部分隔符不被考虑在内，因此在此 {@code Path}"{@code foo/bar}" 上调用此方法并使用 {@code String} "{@code bar/}" 会返回 {@code true}。
     *
     * @param   other
     *          给定路径字符串
     *
     * @return  如果此路径以给定路径结尾，则返回 {@code true}；否则返回 {@code false}
     *
     * @throws  InvalidPathException
     *          如果路径字符串无法转换为 Path。
     */
    boolean endsWith(String other);

    /**
     * 返回一个路径，该路径是此路径，但消除了冗余的名称元素。
     *
     * <p> 该方法的精确定义取决于实现，但通常情况下，它从此路径派生出一个不包含 <em>冗余</em> 名称元素的路径。在许多文件系统中，"{@code .}" 和 "{@code ..}" 是用于表示当前目录和父目录的特殊名称。在这样的文件系统中，所有出现的 "{@code .}" 都被认为是冗余的。如果一个 "{@code ..}" 前面有一个非 "{@code ..}" 名称，则这两个名称都被认为是冗余的（识别这些名称的过程会重复进行，直到不再适用）。
     *
     * <p> 该方法不访问文件系统；路径可能不定位到一个存在的文件。从路径中消除 "{@code ..}" 和一个前导名称可能导致定位到一个与原始路径不同的文件。当前导名称是一个符号链接时，可能会出现这种情况。
     *
     * @return  结果路径或此路径，如果此路径不包含冗余名称元素；如果此路径有根组件且所有名称元素都是冗余的，则返回空路径
     *
     * @see #getParent
     * @see #toRealPath
     */
    Path normalize();

    // -- resolution and relativization --

    /**
     * 将给定路径解析为此路径。
     *
     * <p> 如果 {@code other} 参数是 {@link #isAbsolute() 绝对} 路径，则此方法简单地返回 {@code other}。如果 {@code other} 是 <i>空路径</i>，则此方法简单地返回此路径。否则，此方法将此路径视为目录，并将给定路径解析为此路径。在最简单的情况下，给定路径没有 {@link #getRoot 根} 组件，此时此方法将给定路径 <em>连接</em> 到此路径，并返回一个结果路径，该路径 {@link #endsWith 以} 给定路径结尾。如果给定路径有根组件，则解析高度依赖于实现，因此未指定。
     *
     * @param   other
     *          要解析为此路径的路径
     *
     * @return  结果路径
     *
     * @see #relativize
     */
    Path resolve(Path other);


    /**
     * 将给定的路径字符串转换为 {@code Path} 并以与 {@link
     * #resolve(Path) resolve} 方法指定的方式完全相同的方式解析它。例如，假设名称分隔符为 "{@code /}"，并且路径表示 "{@code foo/bar}"，那么调用此方法并传递路径字符串 "{@code gus}" 将导致 {@code Path} "{@code foo/bar/gus}"。
     *
     * @param   other
     *          要解析的路径字符串
     *
     * @return  结果路径
     *
     * @throws  InvalidPathException
     *          如果路径字符串无法转换为 Path。
     *
     * @see FileSystem#getPath
     */
    Path resolve(String other);

    /**
     * 将给定的路径解析为相对于此路径的 {@link #getParent 父路径}。这在需要将文件名 <i>替换</i> 为另一个文件名时非常有用。例如，假设名称分隔符为 "{@code /}"，并且路径表示 "{@code dir1/dir2/foo}"，那么调用此方法并传递 {@code Path} "{@code bar}" 将导致 {@code Path} "{@code dir1/dir2/bar}"。如果此路径没有父路径，或者 {@code other} 是 {@link #isAbsolute() 绝对路径}，则此方法返回 {@code other}。如果 {@code other} 是空路径，则此方法返回此路径的父路径，或者如果此路径没有父路径，则返回空路径。
     *
     * @param   other
     *          要解析的路径
     *
     * @return  结果路径
     *
     * @see #resolve(Path)
     */
    Path resolveSibling(Path other);

    /**
     * 将给定的路径字符串转换为 {@code Path} 并以与 {@link #resolveSibling(Path) resolveSibling} 方法指定的方式完全相同的方式解析它。
     *
     * @param   other
     *          要解析的路径字符串
     *
     * @return  结果路径
     *
     * @throws  InvalidPathException
     *          如果路径字符串无法转换为 Path。
     *
     * @see FileSystem#getPath
     */
    Path resolveSibling(String other);

    /**
     * 构造此路径和给定路径之间的相对路径。
     *
     * <p> 相对化是 {@link #resolve(Path) 解析} 的逆操作。此方法尝试构造一个 {@link #isAbsolute 相对} 路径，该路径在解析时会生成一个与给定路径定位相同文件的路径。例如，在 UNIX 上，如果此路径为 {@code "/a/b"}，给定路径为 {@code "/a/b/c/d"}，则结果相对路径将为 {@code "c/d"}。如果此路径和给定路径都没有 {@link #getRoot 根} 组件，则可以构造相对路径。如果只有一个路径有根组件，则无法构造相对路径。如果两个路径都有根组件，则是否可以构造相对路径取决于实现。如果此路径和给定路径 {@link #equals 相等}，则返回一个 <i>空路径</i>。
     *
     * <p> 对于任何两个 {@link #normalize 规范化} 路径 <i>p</i> 和 <i>q</i>，其中 <i>q</i> 没有根组件，
     * <blockquote>
     *   <i>p</i><tt>.relativize(</tt><i>p</i><tt>.resolve(</tt><i>q</i><tt>)).equals(</tt><i>q</i><tt>)</tt>
     * </blockquote>
     *
     * <p> 当支持符号链接时，结果路径在解析时是否可以用于定位与 {@code other} 相同的文件取决于实现。例如，如果此路径为 {@code "/a/b"}，给定路径为 {@code "/a/x"}，则结果相对路径可能是 {@code "../x"}。如果 {@code "b"} 是一个符号链接，则 {@code "a/b/../x"} 是否可以定位与 {@code "/a/x"} 相同的文件取决于实现。
     *
     * @param   other
     *          要相对化的路径
     *
     * @return  结果相对路径，或者如果两个路径相等，则返回空路径
     *
     * @throws  IllegalArgumentException
     *          如果 {@code other} 不能相对化为此路径
     */
    Path relativize(Path other);

    /**
     * 返回一个表示此路径的 URI。
     *
     * <p> 此方法构造一个绝对 {@link URI}，其 {@link URI#getScheme() 方案} 等于标识提供者的 URI 方案。方案特定部分的确切形式高度依赖于提供者。
     *
     * <p> 在默认提供者的情况下，URI 是分层的，具有一个绝对的 {@link URI#getPath() 路径} 组件。查询和片段组件未定义。权威组件是否定义取决于实现。不能保证 {@code URI} 可用于构造 {@link java.io.File java.io.File}。特别是，如果此路径表示一个通用命名约定 (UNC) 路径，则 UNC 服务器名称可能编码在结果 URI 的权威组件中。在默认提供者的情况下，如果文件存在，并且可以确定文件是一个目录，则结果 {@code URI} 将以斜杠结尾。
     *
     * <p> 默认提供者提供了与 {@link java.io.File} 类类似的 <em>往返</em> 保证。对于给定的 {@code Path} <i>p</i>，保证
     * <blockquote><tt>
     * {@link Paths#get(URI) Paths.get}(</tt><i>p</i><tt>.toUri()).equals(</tt><i>p</i>
     * <tt>.{@link #toAbsolutePath() toAbsolutePath}())</tt>
     * </blockquote>
     * 只要原始 {@code Path}、{@code URI} 和新 {@code Path} 都是在（可能不同的调用中）同一个 Java 虚拟机中创建的。其他提供者是否提供任何保证是特定于提供者的，因此未指定。
     *
     * <p> 当文件系统被构造以访问文件的内容作为文件系统时，返回的 URI 是表示给定路径在文件系统中的路径，还是表示一个编码了包含文件系统的 URI 的 <em>复合</em> URI，高度依赖于实现。此版本中未定义复合 URI 的格式；此类方案可能在未来的版本中添加。
     *
     * @return  表示此路径的 URI
     *
     * @throws  java.io.IOError
     *          如果在获取绝对路径时发生 I/O 错误，或者当文件系统被构造以访问文件的内容作为文件系统时，无法获取包含文件系统的 URI。
     *
     * @throws  SecurityException
     *          在默认提供者的情况下，如果安装了安全管理器，则 {@link #toAbsolutePath toAbsolutePath} 方法会抛出安全异常。
     */
    URI toUri();

    /**
     * 返回表示此路径绝对路径的 {@code Path} 对象。
     *
     * <p> 如果此路径已经是 {@link Path#isAbsolute 绝对路径}，则此方法直接返回此路径。否则，此方法以实现依赖的方式解析路径，通常通过解析路径与文件系统的默认目录。根据实现，此方法可能在文件系统不可访问时抛出 I/O 错误。
     *
     * @return  表示绝对路径的 {@code Path} 对象
     *
     * @throws  java.io.IOError
     *          如果发生 I/O 错误
     * @throws  SecurityException
     *          在默认提供者的情况下，如果安装了安全管理器，并且此路径不是绝对路径，则安全管理器的 {@link SecurityManager#checkPropertyAccess(String)
     *          checkPropertyAccess} 方法将被调用以检查对系统属性 {@code user.dir} 的访问。
     */
    Path toAbsolutePath();

    /**
     * 返回现有文件的 <em>实际</em> 路径。
     *
     * <p> 此方法的精确定义取决于实现，但通常情况下，它从此路径派生一个 {@link #isAbsolute 绝对路径}，该路径定位与此路径相同的文件，但名称元素表示目录和文件的实际名称。例如，如果文件系统中的文件名比较不区分大小写，则名称元素表示实际大小写的名称。此外，结果路径将移除冗余的名称元素。
     *
     * <p> 如果此路径是相对路径，则首先获取其绝对路径，就像调用 {@link #toAbsolutePath toAbsolutePath} 方法一样。
     *
     * <p> 可以使用 {@code options} 数组来指示如何处理符号链接。默认情况下，符号链接解析为最终目标。如果存在选项 {@link LinkOption#NOFOLLOW_LINKS NOFOLLOW_LINKS}，则此方法不会解析符号链接。
     *
     * 一些实现允许使用特殊名称如 "{@code ..}" 来引用父目录。在派生 <em>实际路径</em> 时，如果一个 "{@code ..}"（或等效名称）前面有一个非 "{@code ..}" 名称，则实现通常会移除这两个名称。当不解析符号链接且前面的名称是符号链接时，只有在保证结果路径可以定位与此路径相同的文件时，才会移除这些名称。
     *
     * @param   options
     *          指示如何处理符号链接的选项
     *
     * @return  表示此对象定位文件的 <em>实际</em> 路径的绝对路径
     *
     * @throws  IOException
     *          如果文件不存在或发生 I/O 错误
     * @throws  SecurityException
     *          在默认提供者的情况下，如果安装了安全管理器，其 {@link SecurityManager#checkRead(String) checkRead}
     *          方法将被调用以检查对文件的读取访问，如果此路径不是绝对路径，其 {@link SecurityManager#checkPropertyAccess(String)
     *          checkPropertyAccess} 方法将被调用以检查对系统属性 {@code user.dir} 的访问。
     */
    Path toRealPath(LinkOption... options) throws IOException;

    /**
     * 返回表示此路径的 {@link File} 对象。如果此 {@code Path} 与默认提供者关联，则此方法等同于使用此路径的 {@code String} 表示形式构造的 {@code File} 对象。
     *
     * <p> 如果此路径是通过调用 {@code File} {@link File#toPath toPath} 方法创建的，则不能保证此方法返回的 {@code File} 对象与原始 {@code File} {@link #equals 相等}。
     *
     * @return  表示此路径的 {@code File} 对象
     *
     * @throws  UnsupportedOperationException
     *          如果此 {@code Path} 未与默认提供者关联
     */
    File toFile();

    // -- watchable --

    /**
     * 将此路径定位的文件注册到监视服务。
     *
     * <p> 在此版本中，此路径定位一个存在的目录。该目录注册到监视服务，以便可以监视目录中的条目。{@code events} 参数是要注册的事件，可能包含以下事件：
     * <ul>
     *   <li>{@link StandardWatchEventKinds#ENTRY_CREATE ENTRY_CREATE} -
     *       条目创建或移动到目录中</li>
     *   <li>{@link StandardWatchEventKinds#ENTRY_DELETE ENTRY_DELETE} -
     *        条目删除或移出目录</li>
     *   <li>{@link StandardWatchEventKinds#ENTRY_MODIFY ENTRY_MODIFY} -
     *        目录中的条目被修改</li>
     * </ul>
     *
     * <p> 这些事件的 {@link WatchEvent#context 上下文} 是此路径定位的目录与定位目录条目（创建、删除或修改）的路径之间的相对路径。
     *
     * <p> 事件集可能包括未由枚举 {@link StandardWatchEventKinds} 定义的其他实现特定事件。
     *
     * <p> {@code modifiers} 参数指定 <em>修饰符</em>，用于限定目录注册的方式。此版本未定义任何 <em>标准</em> 修饰符。可能包含实现特定修饰符。
     *
     * <p> 如果通过符号链接注册文件到监视服务，则在注册后是否继续依赖符号链接的存在取决于实现。
     *
     * @param   watcher
     *          要注册此对象的监视服务
     * @param   events
     *          要为此对象注册的事件
     * @param   modifiers
     *          修饰符，如果有的话，用于限定对象的注册方式
     *
     * @return  一个键，表示此对象在给定监视服务中的注册
     *
     * @throws  UnsupportedOperationException
     *          如果指定了不受支持的事件或修饰符
     * @throws  IllegalArgumentException
     *          如果指定了无效的事件或修饰符组合
     * @throws  ClosedWatchServiceException
     *          如果监视服务已关闭
     * @throws  NotDirectoryException
     *          如果文件注册为监视目录中的条目，但文件不是目录 <i>(可选特定异常)</i>
     * @throws  IOException
     *          如果发生 I/O 错误
     * @throws  SecurityException
     *          在默认提供者的情况下，如果安装了安全管理器，其 {@link SecurityManager#checkRead(String) checkRead}
     *          方法将被调用以检查对文件的读取访问。
     */
    @Override
    WatchKey register(WatchService watcher,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
        throws IOException;

    /**
     * 将此路径定位的文件注册到监视服务。
     *
     * <p> 调用此方法的行为与调用
     * <pre>
     *     watchable.{@link #register(WatchService,WatchEvent.Kind[],WatchEvent.Modifier[]) register}(watcher, events, new WatchEvent.Modifier[0]);
     * </pre>
     * 完全相同。
     *
     * <p> <b>使用示例：</b>
     * 假设我们希望注册一个目录以监视条目创建、删除和修改事件：
     * <pre>
     *     Path dir = ...
     *     WatchService watcher = ...
     *
     *     WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
     * </pre>
     * @param   watcher
     *          要注册此对象的监视服务
     * @param   events
     *          要为此对象注册的事件
     *
     * @return  一个键，表示此对象在给定监视服务中的注册
     *
     * @throws  UnsupportedOperationException
     *          如果指定了不受支持的事件
     * @throws  IllegalArgumentException
     *          如果指定了无效的事件组合
     * @throws  ClosedWatchServiceException
     *          如果监视服务已关闭
     * @throws  NotDirectoryException
     *          如果文件注册为监视目录中的条目，但文件不是目录 <i>(可选特定异常)</i>
     * @throws  IOException
     *          如果发生 I/O 错误
     * @throws  SecurityException
     *          在默认提供者的情况下，如果安装了安全管理器，其 {@link SecurityManager#checkRead(String) checkRead}
     *          方法将被调用以检查对文件的读取访问。
     */
    @Override
    WatchKey register(WatchService watcher,
                      WatchEvent.Kind<?>... events)
        throws IOException;


                // -- Iterable --

    /**
     * 返回此路径的名称元素的迭代器。
     *
     * <p> 迭代器返回的第一个元素表示目录层次结构中最接近根的名称元素，第二个元素是次接近的，依此类推。返回的最后一个元素是此路径表示的文件或目录的名称。如果存在，{@link
     * #getRoot 根} 组件不会被迭代器返回。
     *
     * @return  此路径的名称元素的迭代器。
     */
    @Override
    Iterator<Path> iterator();

    // -- compareTo/equals/hashCode --

    /**
     * 按字典顺序比较两个抽象路径。此方法定义的顺序是提供程序特定的，并且在默认提供程序的情况下，是平台特定的。此方法不会访问文件系统，也不要求文件存在。
     *
     * <p> 不能使用此方法来比较与不同文件系统提供程序关联的路径。
     *
     * @param   other  与本路径比较的路径。
     *
     * @return  如果参数与本路径 {@link #equals 相等}，则返回零；如果本路径按字典顺序小于参数，则返回一个小于零的值；如果本路径按字典顺序大于参数，则返回一个大于零的值。
     *
     * @throws  ClassCastException
     *          如果路径与不同的提供程序关联。
     */
    @Override
    int compareTo(Path other);

    /**
     * 测试此路径是否与给定的对象相等。
     *
     * <p> 如果给定的对象不是 Path，或者是一个与不同的 {@code FileSystem} 关联的 Path，则此方法返回 {@code false}。
     *
     * <p> 两个路径是否相等取决于文件系统实现。在某些情况下，路径比较时不区分大小写，而在其他情况下则区分大小写。此方法不会访问文件系统，也不要求文件存在。如有需要，可以使用 {@link Files#isSameFile isSameFile} 方法来检查两个路径是否指向同一个文件。
     *
     * <p> 此方法满足 {@link
     * java.lang.Object#equals(Object) Object.equals} 方法的一般约定。 </p>
     *
     * @param   other
     *          要与之比较的对象。
     *
     * @return  {@code true} 如果且仅当给定对象是一个与本 {@code Path} 相同的 {@code Path}。
     */
    boolean equals(Object other);

    /**
     * 计算此路径的哈希码。
     *
     * <p> 哈希码基于路径的组件，并满足 {@link Object#hashCode
     * Object.hashCode} 方法的一般约定。
     *
     * @return  此路径的哈希码值。
     */
    int hashCode();

    /**
     * 返回此路径的字符串表示形式。
     *
     * <p> 如果此路径是通过使用 {@link FileSystem#getPath getPath} 方法从路径字符串转换而来的，则此方法返回的路径字符串可能与用于创建路径的原始字符串不同。
     *
     * <p> 返回的路径字符串使用默认名称 {@link
     * FileSystem#getSeparator 分隔符} 来分隔路径中的名称。
     *
     * @return  此路径的字符串表示形式。
     */
    String toString();
}
