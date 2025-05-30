
/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.util.Objects;
import java.util.Formatter;
import java.util.Locale;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * 将对象的格式化表示形式打印到文本输出流。此类实现了 {@link
 * PrintStream} 中的所有 <tt>print</tt> 方法。它不包含用于写入原始字节的方法，程序应使用未编码的字节流。
 *
 * <p> 与 {@link PrintStream} 类不同，如果启用了自动刷新，则仅在调用 <tt>println</tt>、<tt>printf</tt> 或
 * <tt>format</tt> 方法时才会进行刷新，而不是在输出换行符时进行刷新。这些方法使用平台自身的行分隔符，而不是换行符。
 *
 * <p> 本类中的方法从不抛出 I/O 异常，尽管某些构造方法可能会抛出。客户端可以通过调用 {@link #checkError checkError()} 来查询是否发生错误。
 *
 * @author      Frank Yellin
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public class PrintWriter extends Writer {

    /**
     * 此 <code>PrintWriter</code> 的底层字符输出流。
     *
     * @since 1.2
     */
    protected Writer out;

    private final boolean autoFlush;
    private boolean trouble = false;
    private Formatter formatter;
    private PrintStream psOut = null;

    /**
     * 行分隔符字符串。这是在创建流时的行分隔符属性的值。
     */
    private final String lineSeparator;

    /**
     * 返回给定字符集名称的字符集对象。
     * @throws NullPointerException          如果 csn 为 null
     * @throws UnsupportedEncodingException  如果字符集不受支持
     */
    private static Charset toCharset(String csn)
        throws UnsupportedEncodingException
    {
        Objects.requireNonNull(csn, "charsetName");
        try {
            return Charset.forName(csn);
        } catch (IllegalCharsetNameException|UnsupportedCharsetException unused) {
            // 应该抛出 UnsupportedEncodingException
            throw new UnsupportedEncodingException(csn);
        }
    }

    /**
     * 创建一个新的 PrintWriter，不启用自动行刷新。
     *
     * @param  out        字符输出流
     */
    public PrintWriter (Writer out) {
        this(out, false);
    }

    /**
     * 创建一个新的 PrintWriter。
     *
     * @param  out        字符输出流
     * @param  autoFlush  布尔值；如果为 true，则 <tt>println</tt>、
     *                    <tt>printf</tt> 或 <tt>format</tt> 方法将刷新输出缓冲区
     */
    public PrintWriter(Writer out,
                       boolean autoFlush) {
        super(out);
        this.out = out;
        this.autoFlush = autoFlush;
        lineSeparator = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("line.separator"));
    }

    /**
     * 从现有的 OutputStream 创建一个新的 PrintWriter，不启用自动行刷新。此方便构造函数创建必要的中间 OutputStreamWriter，该对象将使用默认字符编码将字符转换为字节。
     *
     * @param  out        输出流
     *
     * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream)
     */
    public PrintWriter(OutputStream out) {
        this(out, false);
    }

    /**
     * 从现有的 OutputStream 创建一个新的 PrintWriter。此方便构造函数创建必要的中间 OutputStreamWriter，该对象将使用默认字符编码将字符转换为字节。
     *
     * @param  out        输出流
     * @param  autoFlush  布尔值；如果为 true，则 <tt>println</tt>、
     *                    <tt>printf</tt> 或 <tt>format</tt> 方法将刷新输出缓冲区
     *
     * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream)
     */
    public PrintWriter(OutputStream out, boolean autoFlush) {
        this(new BufferedWriter(new OutputStreamWriter(out)), autoFlush);

        // 保存输出流以传播错误
        if (out instanceof java.io.PrintStream) {
            psOut = (PrintStream) out;
        }
    }

    /**
     * 使用指定的文件名创建一个新的 PrintWriter，不启用自动行刷新。此方便构造函数创建必要的中间 {@link java.io.OutputStreamWriter OutputStreamWriter}，
     * 该对象将使用此 Java 虚拟机实例的默认字符集对字符进行编码。
     *
     * @param  fileName
     *         用作此写入器目标的文件名。如果文件存在，则将其截断为零大小；否则，将创建一个新文件。输出将写入文件并缓冲。
     *
     * @throws  FileNotFoundException
     *          如果给定的字符串不表示现有的可写常规文件，并且无法创建同名的新常规文件，或者在打开或创建文件时发生其他错误
     *
     * @throws  SecurityException
     *          如果存在安全管理器，并且 {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} 拒绝对文件的写访问
     *
     * @since  1.5
     */
    public PrintWriter(String fileName) throws FileNotFoundException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))),
             false);
    }

    /* 私有构造函数 */
    private PrintWriter(Charset charset, File file)
        throws FileNotFoundException
    {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset)),
             false);
    }

    /**
     * 使用指定的文件名和字符集创建一个新的 PrintWriter，不启用自动行刷新。此方便构造函数创建必要的中间 {@link java.io.OutputStreamWriter
     * OutputStreamWriter}，该对象将使用提供的字符集对字符进行编码。
     *
     * @param  fileName
     *         用作此写入器目标的文件名。如果文件存在，则将其截断为零大小；否则，将创建一个新文件。输出将写入文件并缓冲。
     *
     * @param  csn
     *         支持的 {@linkplain java.nio.charset.Charset
     *         字符集} 的名称
     *
     * @throws  FileNotFoundException
     *          如果给定的字符串不表示现有的可写常规文件，并且无法创建同名的新常规文件，或者在打开或创建文件时发生其他错误
     *
     * @throws  SecurityException
     *          如果存在安全管理器，并且 {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} 拒绝对文件的写访问
     *
     * @throws  UnsupportedEncodingException
     *          如果命名的字符集不受支持
     *
     * @since  1.5
     */
    public PrintWriter(String fileName, String csn)
        throws FileNotFoundException, UnsupportedEncodingException
    {
        this(toCharset(csn), new File(fileName));
    }

    /**
     * 使用指定的文件创建一个新的 PrintWriter，不启用自动行刷新。此方便构造函数创建必要的中间 {@link java.io.OutputStreamWriter OutputStreamWriter}，
     * 该对象将使用此 Java 虚拟机实例的默认字符集对字符进行编码。
     *
     * @param  file
     *         用作此写入器目标的文件。如果文件存在，则将其截断为零大小；否则，将创建一个新文件。输出将写入文件并缓冲。
     *
     * @throws  FileNotFoundException
     *          如果给定的文件对象不表示现有的可写常规文件，并且无法创建同名的新常规文件，或者在打开或创建文件时发生其他错误
     *
     * @throws  SecurityException
     *          如果存在安全管理器，并且 {@link
     *          SecurityManager#checkWrite checkWrite(file.getPath())}
     *          拒绝对文件的写访问
     *
     * @since  1.5
     */
    public PrintWriter(File file) throws FileNotFoundException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))),
             false);
    }

    /**
     * 使用指定的文件和字符集创建一个新的 PrintWriter，不启用自动行刷新。此方便构造函数创建必要的中间 {@link java.io.OutputStreamWriter
     * OutputStreamWriter}，该对象将使用提供的字符集对字符进行编码。
     *
     * @param  file
     *         用作此写入器目标的文件。如果文件存在，则将其截断为零大小；否则，将创建一个新文件。输出将写入文件并缓冲。
     *
     * @param  csn
     *         支持的 {@linkplain java.nio.charset.Charset
     *         字符集} 的名称
     *
     * @throws  FileNotFoundException
     *          如果给定的文件对象不表示现有的可写常规文件，并且无法创建同名的新常规文件，或者在打开或创建文件时发生其他错误
     *
     * @throws  SecurityException
     *          如果存在安全管理器，并且 {@link
     *          SecurityManager#checkWrite checkWrite(file.getPath())}
     *          拒绝对文件的写访问
     *
     * @throws  UnsupportedEncodingException
     *          如果命名的字符集不受支持
     *
     * @since  1.5
     */
    public PrintWriter(File file, String csn)
        throws FileNotFoundException, UnsupportedEncodingException
    {
        this(toCharset(csn), file);
    }

    /** 检查流是否已关闭 */
    private void ensureOpen() throws IOException {
        if (out == null)
            throw new IOException("Stream closed");
    }

    /**
     * 刷新流。
     * @see #checkError()
     */
    public void flush() {
        try {
            synchronized (lock) {
                ensureOpen();
                out.flush();
            }
        }
        catch (IOException x) {
            trouble = true;
        }
    }

    /**
     * 关闭流并释放与之关联的任何系统资源。关闭已关闭的流没有效果。
     *
     * @see #checkError()
     */
    public void close() {
        try {
            synchronized (lock) {
                if (out == null)
                    return;
                out.close();
                out = null;
            }
        }
        catch (IOException x) {
            trouble = true;
        }
    }

    /**
     * 如果流未关闭，则刷新流并检查其错误状态。
     *
     * @return <code>true</code> 如果打印流遇到错误，无论是底层输出流中的错误还是格式转换中的错误。
     */
    public boolean checkError() {
        if (out != null) {
            flush();
        }
        if (out instanceof java.io.PrintWriter) {
            PrintWriter pw = (PrintWriter) out;
            return pw.checkError();
        } else if (psOut != null) {
            return psOut.checkError();
        }
        return trouble;
    }

    /**
     * 指示已发生错误。
     *
     * <p> 此方法将导致后续调用 {@link
     * #checkError()} 返回 <tt>true</tt>，直到调用 {@link
     * #clearError()}。
     */
    protected void setError() {
        trouble = true;
    }

    /**
     * 清除此流的错误状态。
     *
     * <p> 此方法将导致后续调用 {@link
     * #checkError()} 返回 <tt>false</tt>，直到另一个写操作失败并调用 {@link #setError()}。
     *
     * @since 1.6
     */
    protected void clearError() {
        trouble = false;
    }

    /*
     * 捕获异常、同步的输出操作，
     * 也实现了 Writer 的 write() 方法
     */

    /**
     * 写入单个字符。
     * @param c 指定要写入的字符的 int 值。
     */
    public void write(int c) {
        try {
            synchronized (lock) {
                ensureOpen();
                out.write(c);
            }
        }
        catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        }
        catch (IOException x) {
            trouble = true;
        }
    }

    /**
     * 写入字符数组的一部分。
     * @param buf 字符数组
     * @param off 开始写入字符的偏移量
     * @param len 要写入的字符数
     */
    public void write(char buf[], int off, int len) {
        try {
            synchronized (lock) {
                ensureOpen();
                out.write(buf, off, len);
            }
        }
        catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        }
        catch (IOException x) {
            trouble = true;
        }
    }


                /**
     * 写入字符数组。此方法不能从 Writer 类继承，因为它必须抑制 I/O 异常。
     * @param buf 要写入的字符数组
     */
    public void write(char buf[]) {
        write(buf, 0, buf.length);
    }

    /**
     * 写入字符串的一部分。
     * @param s 一个字符串
     * @param off 开始写入字符的偏移量
     * @param len 要写入的字符数
     */
    public void write(String s, int off, int len) {
        try {
            synchronized (lock) {
                ensureOpen();
                out.write(s, off, len);
            }
        }
        catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        }
        catch (IOException x) {
            trouble = true;
        }
    }

    /**
     * 写入一个字符串。此方法不能从 Writer 类继承，因为它必须抑制 I/O 异常。
     * @param s 要写入的字符串
     */
    public void write(String s) {
        write(s, 0, s.length());
    }

    private void newLine() {
        try {
            synchronized (lock) {
                ensureOpen();
                out.write(lineSeparator);
                if (autoFlush)
                    out.flush();
            }
        }
        catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        }
        catch (IOException x) {
            trouble = true;
        }
    }

    /* 不终止行的方法 */

    /**
     * 打印一个布尔值。由 <code>{@link
     * java.lang.String#valueOf(boolean)}</code> 生成的字符串根据平台的默认字符编码转换为字节，
     * 并以与 <code>{@link
     * #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      b   要打印的 <code>boolean</code>
     */
    public void print(boolean b) {
        write(b ? "true" : "false");
    }

    /**
     * 打印一个字符。该字符根据平台的默认字符编码转换为一个或多个字节，
     * 并以与 <code>{@link
     * #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      c   要打印的 <code>char</code>
     */
    public void print(char c) {
        write(c);
    }

    /**
     * 打印一个整数。由 <code>{@link
     * java.lang.String#valueOf(int)}</code> 生成的字符串根据平台的默认字符编码转换为字节，
     * 并以与 <code>{@link #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      i   要打印的 <code>int</code>
     * @see        java.lang.Integer#toString(int)
     */
    public void print(int i) {
        write(String.valueOf(i));
    }

    /**
     * 打印一个长整数。由 <code>{@link
     * java.lang.String#valueOf(long)}</code> 生成的字符串根据平台的默认字符编码转换为字节，
     * 并以与 <code>{@link #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      l   要打印的 <code>long</code>
     * @see        java.lang.Long#toString(long)
     */
    public void print(long l) {
        write(String.valueOf(l));
    }

    /**
     * 打印一个浮点数。由 <code>{@link
     * java.lang.String#valueOf(float)}</code> 生成的字符串根据平台的默认字符编码转换为字节，
     * 并以与 <code>{@link #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      f   要打印的 <code>float</code>
     * @see        java.lang.Float#toString(float)
     */
    public void print(float f) {
        write(String.valueOf(f));
    }

    /**
     * 打印一个双精度浮点数。由 <code>{@link java.lang.String#valueOf(double)}</code> 生成的字符串
     * 根据平台的默认字符编码转换为字节，并以与 <code>{@link
     * #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      d   要打印的 <code>double</code>
     * @see        java.lang.Double#toString(double)
     */
    public void print(double d) {
        write(String.valueOf(d));
    }

    /**
     * 打印一个字符数组。字符根据平台的默认字符编码转换为字节，
     * 并以与 <code>{@link #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      s   要打印的字符数组
     *
     * @throws  NullPointerException  如果 <code>s</code> 为 <code>null</code>
     */
    public void print(char s[]) {
        write(s);
    }

    /**
     * 打印一个字符串。如果参数为 <code>null</code>，则打印字符串 <code>"null"</code>。
     * 否则，字符串的字符根据平台的默认字符编码转换为字节，
     * 并以与 <code>{@link #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      s   要打印的 <code>String</code>
     */
    public void print(String s) {
        if (s == null) {
            s = "null";
        }
        write(s);
    }

    /**
     * 打印一个对象。由 <code>{@link
     * java.lang.String#valueOf(Object)}</code> 方法生成的字符串根据平台的默认字符编码转换为字节，
     * 并以与 <code>{@link #write(int)}</code> 方法完全相同的方式写入这些字节。
     *
     * @param      obj   要打印的 <code>Object</code>
     * @see        java.lang.Object#toString()
     */
    public void print(Object obj) {
        write(String.valueOf(obj));
    }

    /* 终止行的方法 */

    /**
     * 通过写入行分隔符字符串来终止当前行。行分隔符字符串由系统属性
     * <code>line.separator</code> 定义，不一定是单个换行符 (<code>'\n'</code>)。
     */
    public void println() {
        newLine();
    }

    /**
     * 打印一个布尔值并终止行。此方法的行为类似于调用 <code>{@link #print(boolean)}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>boolean</code> 值
     */
    public void println(boolean x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个字符并终止行。此方法的行为类似于调用 <code>{@link #print(char)}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>char</code> 值
     */
    public void println(char x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个整数并终止行。此方法的行为类似于调用 <code>{@link #print(int)}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>int</code> 值
     */
    public void println(int x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个长整数并终止行。此方法的行为类似于调用 <code>{@link #print(long)}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>long</code> 值
     */
    public void println(long x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个浮点数并终止行。此方法的行为类似于调用 <code>{@link #print(float)}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>float</code> 值
     */
    public void println(float x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个双精度浮点数并终止行。此方法的行为类似于调用 <code>{@link
     * #print(double)}</code> 然后 <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>double</code> 值
     */
    public void println(double x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个字符数组并终止行。此方法的行为类似于调用 <code>{@link #print(char[])}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>char</code> 数组
     */
    public void println(char x[]) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个字符串并终止行。此方法的行为类似于调用 <code>{@link #print(String)}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x 要打印的 <code>String</code> 值
     */
    public void println(String x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * 打印一个对象并终止行。此方法首先调用 String.valueOf(x) 获取要打印的对象的字符串值，
     * 然后的行为类似于调用 <code>{@link #print(String)}</code> 然后
     * <code>{@link #println()}</code>。
     *
     * @param x  要打印的 <code>Object</code>。
     */
    public void println(Object x) {
        String s = String.valueOf(x);
        synchronized (lock) {
            print(s);
            println();
        }
    }

    /**
     * 一个方便的方法，使用指定的格式字符串和参数将格式化的字符串写入此写入器。如果启用了自动刷新，
     * 调用此方法将刷新输出缓冲区。
     *
     * <p> 以 <tt>out.printf(format, args)</tt> 形式调用此方法的行为与以下调用完全相同
     *
     * <pre>
     *     out.format(format, args) </pre>
     *
     * @param  format
     *         一个格式字符串，如 <a
     *         href="../util/Formatter.html#syntax">格式字符串语法</a> 所述。
     *
     * @param  args
     *         由格式字符串中的格式说明符引用的参数。如果有更多的参数比格式说明符多，多余的参数将被忽略。
     *         参数的数量是可变的，可以为零。参数的最大数量由 <cite>The Java&trade; Virtual Machine Specification</cite>
     *         定义的 Java 数组的最大维度限制。对 <tt>null</tt> 参数的行为取决于 <a
     *         href="../util/Formatter.html#syntax">转换</a>。
     *
     * @throws  java.util.IllegalFormatException
     *          如果格式字符串包含非法语法，格式说明符与给定参数不兼容，格式字符串的参数不足，或其他非法条件。
     *          有关所有可能的格式错误的规范，请参阅格式化器类规范的 <a
     *          href="../util/Formatter.html#detail">详细信息</a> 部分。
     *
     * @throws  NullPointerException
     *          如果 <tt>format</tt> 为 <tt>null</tt>
     *
     * @return  此写入器
     *
     * @since  1.5
     */
    public PrintWriter printf(String format, Object ... args) {
        return format(format, args);
    }

    /**
     * 一个方便的方法，使用指定的格式字符串和参数将格式化的字符串写入此写入器。如果启用了自动刷新，
     * 调用此方法将刷新输出缓冲区。
     *
     * <p> 以 <tt>out.printf(l, format, args)</tt> 形式调用此方法的行为与以下调用完全相同
     *
     * <pre>
     *     out.format(l, format, args) </pre>
     *
     * @param  l
     *         在格式化期间应用的 {@linkplain java.util.Locale 本地化}。如果 <tt>l</tt> 为 <tt>null</tt>，
     *         则不应用本地化。
     *
     * @param  format
     *         一个格式字符串，如 <a
     *         href="../util/Formatter.html#syntax">格式字符串语法</a> 所述。
     *
     * @param  args
     *         由格式字符串中的格式说明符引用的参数。如果有更多的参数比格式说明符多，多余的参数将被忽略。
     *         参数的数量是可变的，可以为零。参数的最大数量由 <cite>The Java&trade; Virtual Machine Specification</cite>
     *         定义的 Java 数组的最大维度限制。对 <tt>null</tt> 参数的行为取决于 <a
     *         href="../util/Formatter.html#syntax">转换</a>。
     *
     * @throws  java.util.IllegalFormatException
     *          如果格式字符串包含非法语法，格式说明符与给定参数不兼容，格式字符串的参数不足，或其他非法条件。
     *          有关所有可能的格式错误的规范，请参阅格式化器类规范的 <a
     *          href="../util/Formatter.html#detail">详细信息</a> 部分。
     *
     * @throws  NullPointerException
     *          如果 <tt>format</tt> 为 <tt>null</tt>
     *
     * @return  此写入器
     *
     * @since  1.5
     */
    public PrintWriter printf(Locale l, String format, Object ... args) {
        return format(l, format, args);
    }

    /**
     * 使用指定的格式字符串和参数将格式化的字符串写入此写入器。如果启用了自动刷新，
     * 调用此方法将刷新输出缓冲区。
     *
     * <p> 始终使用的本地化是 {@link
     * java.util.Locale#getDefault() Locale.getDefault()} 返回的本地化，无论此对象上任何先前的格式化方法调用如何。
     *
     * @param  format
     *         一个格式字符串，如 <a
     *         href="../util/Formatter.html#syntax">格式字符串语法</a> 所述。
     *
     * @param  args
     *         由格式字符串中的格式说明符引用的参数。如果有更多的参数比格式说明符多，多余的参数将被忽略。
     *         参数的数量是可变的，可以为零。参数的最大数量由 <cite>The Java&trade; Virtual Machine Specification</cite>
     *         定义的 Java 数组的最大维度限制。对 <tt>null</tt> 参数的行为取决于 <a
     *         href="../util/Formatter.html#syntax">转换</a>。
     *
     * @throws  java.util.IllegalFormatException
     *          如果格式字符串包含非法语法，格式说明符与给定参数不兼容，格式字符串的参数不足，或其他非法条件。
     *          有关所有可能的格式错误的规范，请参阅格式化器类规范的 <a
     *          href="../util/Formatter.html#detail">详细信息</a> 部分。
     *
     * @throws  NullPointerException
     *          如果 <tt>format</tt> 为 <tt>null</tt>
     *
     * @return  此写入器
     *
     * @since  1.5
     */
    public PrintWriter format(String format, Object ... args) {
        try {
            synchronized (lock) {
                ensureOpen();
                if ((formatter == null)
                    || (formatter.locale() != Locale.getDefault()))
                    formatter = new Formatter(this);
                formatter.format(Locale.getDefault(), format, args);
                if (autoFlush)
                    out.flush();
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            trouble = true;
        }
        return this;
    }


                /**
     * 使用指定的格式字符串和参数将格式化的字符串写入此写入器。如果启用了自动刷新，调用此方法将刷新输出缓冲区。
     *
     * @param  l
     *         在格式化期间应用的 {@linkplain java.util.Locale 本地化}。如果 <tt>l</tt> 为 <tt>null</tt>，则不应用本地化。
     *
     * @param  format
     *         一个格式字符串，如 <a
     *         href="../util/Formatter.html#syntax">格式字符串语法</a> 中所述。
     *
     * @param  args
     *         由格式字符串中的格式说明符引用的参数。如果有更多的参数比格式说明符多，多余的参数将被忽略。参数的数量是可变的，可以为零。参数的最大数量由 <cite>The Java&trade; Virtual Machine Specification</cite> 定义的 Java 数组的最大维度限制。对 <tt>null</tt> 参数的行为取决于 <a
     *         href="../util/Formatter.html#syntax">转换</a>。
     *
     * @throws  java.util.IllegalFormatException
     *          如果格式字符串包含非法语法，格式说明符与给定参数不兼容，格式字符串的参数不足，或其他非法条件。有关所有可能的格式错误的详细说明，请参阅格式化器类规范的 <a
     *          href="../util/Formatter.html#detail">详细</a> 部分。
     *
     * @throws  NullPointerException
     *          如果 <tt>format</tt> 为 <tt>null</tt>
     *
     * @return  此写入器
     *
     * @since  1.5
     */
    public PrintWriter format(Locale l, String format, Object ... args) {
        try {
            synchronized (lock) {
                ensureOpen();
                if ((formatter == null) || (formatter.locale() != l))
                    formatter = new Formatter(this, l);
                formatter.format(l, format, args);
                if (autoFlush)
                    out.flush();
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            trouble = true;
        }
        return this;
    }

    /**
     * 将指定的字符序列追加到此写入器。
     *
     * <p> 以 <tt>out.append(csq)</tt> 形式调用此方法的行为与以下调用完全相同：
     *
     * <pre>
     *     out.write(csq.toString()) </pre>
     *
     * <p> 根据字符序列 <tt>csq</tt> 的 <tt>toString</tt> 方法的规范，整个序列可能不会被追加。例如，调用字符缓冲区的 <tt>toString</tt> 方法将返回一个子序列，其内容取决于缓冲区的位置和限制。
     *
     * @param  csq
     *         要追加的字符序列。如果 <tt>csq</tt> 为 <tt>null</tt>，则将四个字符 <tt>"null"</tt> 追加到此写入器。
     *
     * @return  此写入器
     *
     * @since  1.5
     */
    public PrintWriter append(CharSequence csq) {
        if (csq == null)
            write("null");
        else
            write(csq.toString());
        return this;
    }

    /**
     * 将指定字符序列的子序列追加到此写入器。
     *
     * <p> 当 <tt>csq</tt> 不为 <tt>null</tt> 时，以 <tt>out.append(csq, start,
     * end)</tt> 形式调用此方法的行为与以下调用完全相同：
     *
     * <pre>
     *     out.write(csq.subSequence(start, end).toString()) </pre>
     *
     * @param  csq
     *         从中追加子序列的字符序列。如果 <tt>csq</tt> 为 <tt>null</tt>，则将四个字符 <tt>"null"</tt> 追加到此写入器。
     *
     * @param  start
     *         子序列中第一个字符的索引
     *
     * @param  end
     *         子序列中最后一个字符之后的字符的索引
     *
     * @return  此写入器
     *
     * @throws  IndexOutOfBoundsException
     *          如果 <tt>start</tt> 或 <tt>end</tt> 为负数，<tt>start</tt> 大于 <tt>end</tt>，或 <tt>end</tt> 大于 <tt>csq.length()</tt>
     *
     * @since  1.5
     */
    public PrintWriter append(CharSequence csq, int start, int end) {
        CharSequence cs = (csq == null ? "null" : csq);
        write(cs.subSequence(start, end).toString());
        return this;
    }

    /**
     * 将指定的字符追加到此写入器。
     *
     * <p> 以 <tt>out.append(c)</tt> 形式调用此方法的行为与以下调用完全相同：
     *
     * <pre>
     *     out.write(c) </pre>
     *
     * @param  c
     *         要追加的 16 位字符
     *
     * @return  此写入器
     *
     * @since 1.5
     */
    public PrintWriter append(char c) {
        write(c);
        return this;
    }
}
