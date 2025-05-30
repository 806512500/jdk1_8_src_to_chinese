
/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

/**
 * Java 编程语言中 SQL XML 类型的映射。
 * XML 是一个内置类型，用于存储数据库表中某行某列的 XML 值。
 * 默认情况下，驱动程序将 SQLXML 对象实现为指向 XML 数据的逻辑指针，而不是数据本身。
 * SQLXML 对象在其创建的事务期间有效。
 * <p>
 * SQLXML 接口提供了访问 XML 值的方法，可以将其作为字符串、Reader 或 Writer，或作为流访问。XML 值
 * 也可以通过 Source 或设置为 Result 来访问，这些方法用于 DOM、SAX 和 StAX 等 XML 解析 API，
 * 以及 XSLT 转换和 XPath 评估。
 * <p>
 * ResultSet、CallableStatement 和 PreparedStatement 接口中的方法，如 getSQLXML 允许程序员访问 XML 值。
 * 此外，此接口还提供了更新 XML 值的方法。
 * <p>
 * 可以使用以下代码将 SQLXML 实例的 XML 值作为 BinaryStream 获取：
 * <pre>
 *   SQLXML sqlxml = resultSet.getSQLXML(column);
 *   InputStream binaryStream = sqlxml.getBinaryStream();
 * </pre>
 * 例如，使用 DOM 解析器解析 XML 值：
 * <pre>
 *   DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
 *   Document result = parser.parse(binaryStream);
 * </pre>
 * 或使用 SAX 解析器将 XML 值解析到您的处理程序：
 * <pre>
 *   SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
 *   parser.parse(binaryStream, myHandler);
 * </pre>
 * 或使用 StAX 解析器解析 XML 值：
 * <pre>
 *   XMLInputFactory factory = XMLInputFactory.newInstance();
 *   XMLStreamReader streamReader = factory.createXMLStreamReader(binaryStream);
 * </pre>
 * <p>
 * 由于数据库可能使用优化的 XML 表示形式，通过 getSource() 和
 * setResult() 访问值可以提高处理性能
 * 而无需序列化为流表示形式并解析 XML。
 * <p>
 * 例如，获取一个 DOM Document 节点：
 * <pre>
 *   DOMSource domSource = sqlxml.getSource(DOMSource.class);
 *   Document document = (Document) domSource.getNode();
 * </pre>
 * 或将值设置为 DOM Document 节点 myNode：
 * <pre>
 *   DOMResult domResult = sqlxml.setResult(DOMResult.class);
 *   domResult.setNode(myNode);
 * </pre>
 * 或将 SAX 事件发送到您的处理程序：
 * <pre>
 *   SAXSource saxSource = sqlxml.getSource(SAXSource.class);
 *   XMLReader xmlReader = saxSource.getXMLReader();
 *   xmlReader.setContentHandler(myHandler);
 *   xmlReader.parse(saxSource.getInputSource());
 * </pre>
 * 或从 SAX 事件设置结果值：
 * <pre>
 *   SAXResult saxResult = sqlxml.setResult(SAXResult.class);
 *   ContentHandler contentHandler = saxResult.getHandler();
 *   contentHandler.startDocument();
 *   // 将 XML 元素和属性设置到结果中
 *   contentHandler.endDocument();
 * </pre>
 * 或获取 StAX 事件：
 * <pre>
 *   StAXSource staxSource = sqlxml.getSource(StAXSource.class);
 *   XMLStreamReader streamReader = staxSource.getXMLStreamReader();
 * </pre>
 * 或从 StAX 事件设置结果值：
 * <pre>
 *   StAXResult staxResult = sqlxml.setResult(StAXResult.class);
 *   XMLStreamWriter streamWriter = staxResult.getXMLStreamWriter();
 * </pre>
 * 或使用 xsltFile 中的 XSLT 对 XML 值执行 XSLT 转换
 * 输出到文件 resultFile：
 * <pre>
 *   File xsltFile = new File("a.xslt");
 *   File myFile = new File("result.xml");
 *   Transformer xslt = TransformerFactory.newInstance().newTransformer(new StreamSource(xsltFile));
 *   Source source = sqlxml.getSource(null);
 *   Result result = new StreamResult(myFile);
 *   xslt.transform(source, result);
 * </pre>
 * 或在 XML 值上评估 XPath 表达式：
 * <pre>
 *   XPath xpath = XPathFactory.newInstance().newXPath();
 *   DOMSource domSource = sqlxml.getSource(DOMSource.class);
 *   Document document = (Document) domSource.getNode();
 *   String expression = "/foo/@bar";
 *   String barValue = xpath.evaluate(expression, document);
 * </pre>
 * 将 XML 值设置为 XSLT 转换的结果：
 * <pre>
 *   File sourceFile = new File("source.xml");
 *   Transformer xslt = TransformerFactory.newInstance().newTransformer(new StreamSource(xsltFile));
 *   Source streamSource = new StreamSource(sourceFile);
 *   Result result = sqlxml.setResult(null);
 *   xslt.transform(streamSource, result);
 * </pre>
 * 可以使用 newTransformer() 指定的身份转换将任何 Source 转换为 Result：
 * <pre>
 *   Transformer identity = TransformerFactory.newInstance().newTransformer();
 *   Source source = sqlxml.getSource(null);
 *   File myFile = new File("result.xml");
 *   Result result = new StreamResult(myFile);
 *   identity.transform(source, result);
 * </pre>
 * 将 Source 的内容写入标准输出：
 * <pre>
 *   Transformer identity = TransformerFactory.newInstance().newTransformer();
 *   Source source = sqlxml.getSource(null);
 *   Result result = new StreamResult(System.out);
 *   identity.transform(source, result);
 * </pre>
 * 从 DOMResult 创建 DOMSource：
 * <pre>
 *    DOMSource domSource = new DOMSource(domResult.getNode());
 * </pre>
 * <p>
 * 不完整或无效的 XML 值在设置时或在 execute() 发生时可能会导致 SQLException。所有流
 * 必须在 execute() 发生之前关闭，否则将抛出 SQLException。
 * <p>
 * 读取或写入 XML 值到或从 SQLXML 对象最多只能发生一次。
 * 可读和不可读的概念状态决定了读取 API 是否会返回值或抛出异常。
 * 可写和不可写的概念状态决定了写入 API 是否会设置值或抛出异常。
 * <p>
 * 调用 free() 或任何读取 API（getBinaryStream()、getCharacterStream()、getSource() 和 getString()）后，
 * 状态从可读变为不可读。实现也可能在此时将状态更改为不可写。
 * <p>
 * 调用 free() 或任何写入 API（setBinaryStream()、setCharacterStream()、setResult() 和 setString()）后，
 * 状态从可写变为不可写。实现也可能在此时将状态更改为不可读。
 *
 * <p>
 * 如果 JDBC 驱动程序支持该数据类型，则必须完全实现 SQLXML 接口的所有方法。
 *
 * @see javax.xml.parsers
 * @see javax.xml.stream
 * @see javax.xml.transform
 * @see javax.xml.xpath
 * @since 1.6
 */
public interface SQLXML
{
  /**
   * 关闭此对象并释放其持有的资源。
   * 调用此方法后，SQL XML 对象将变得无效且既不可读也不可写。
   *
   * 调用 <code>free</code> 后，任何尝试调用 <code>free</code> 以外的方法都将导致抛出 <code>SQLException</code>。
   * 如果多次调用 <code>free</code>，后续的 <code>free</code> 调用将被视为无操作。
   * @throws SQLException 如果释放 XML 值时发生错误。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持此方法
   * @since 1.6
   */
  void free() throws SQLException;

  /**
   * 以流的形式检索由此 SQLXML 实例指定的 XML 值。
   * 输入流中的字节根据 XML 1.0 规范的附录 F 进行解释。
   * 当 ResultSet 的指定列类型为 java.sql.Types 的 SQLXML 时，此方法的行为与 ResultSet.getBinaryStream() 相同。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可读，并且根据实现也可能变得不可写。
   *
   * @return 包含 XML 数据的流。
   * @throws SQLException 如果处理 XML 值时发生错误。
   *   如果状态不可读，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持此方法
   * @since 1.6
   */
  InputStream getBinaryStream() throws SQLException;

  /**
   * 获取一个可以用于写入此 SQLXML 实例表示的 XML 值的流。
   * 流从位置 0 开始。
   * 流中的字节根据 XML 1.0 规范的附录 F 进行解释。
   * 当 ResultSet 的指定列类型为 java.sql.Types 的 SQLXML 时，此方法的行为与 ResultSet.updateBinaryStream() 相同。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可写，并且根据实现也可能变得不可读。
   *
   * @return 可以写入数据的流。
   * @throws SQLException 如果处理 XML 值时发生错误。
   *   如果状态不可写，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持此方法
   * @since 1.6
   */
  OutputStream setBinaryStream() throws SQLException;

  /**
   * 以 java.io.Reader 对象的形式检索由此 SQLXML 实例指定的 XML 值。
   * 此流的格式由 org.xml.sax.InputSource 定义，
   * 其中流中的字符表示 XML 的 unicode 代码点，根据 XML 1.0 规范的第 2 节和附录 B。
   * 尽管可能存在其他编码声明，但流的编码为 unicode。
   * 当 ResultSet 的指定列类型为 java.sql.Types 的 SQLXML 时，此方法的行为与 ResultSet.getCharacterStream() 相同。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可读，并且根据实现也可能变得不可写。
   *
   * @return 包含 XML 数据的流。
   * @throws SQLException 如果处理 XML 值时发生错误。
   *   异常的 getCause() 方法可能提供更详细的异常，例如，如果流中包含无效字符。
   *   如果状态不可读，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持此方法
   * @since 1.6
   */
  Reader getCharacterStream() throws SQLException;

  /**
   * 获取一个可以用于写入此 SQLXML 实例表示的 XML 值的流。
   * 此流的格式由 org.xml.sax.InputSource 定义，
   * 其中流中的字符表示 XML 的 unicode 代码点，根据 XML 1.0 规范的第 2 节和附录 B。
   * 尽管可能存在其他编码声明，但流的编码为 unicode。
   * 当 ResultSet 的指定列类型为 java.sql.Types 的 SQLXML 时，此方法的行为与 ResultSet.updateCharacterStream() 相同。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可写，并且根据实现也可能变得不可读。
   *
   * @return 可以写入数据的流。
   * @throws SQLException 如果处理 XML 值时发生错误。
   *   异常的 getCause() 方法可能提供更详细的异常，例如，如果流中包含无效字符。
   *   如果状态不可写，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持此方法
   * @since 1.6
   */
  Writer setCharacterStream() throws SQLException;

  /**
   * 返回由此 SQLXML 实例指定的 XML 值的字符串表示形式。
   * 此字符串的格式由 org.xml.sax.InputSource 定义，
   * 其中流中的字符表示 XML 的 unicode 代码点，根据 XML 1.0 规范的第 2 节和附录 B。
   * 尽管可能存在其他编码声明，但字符串的编码为 unicode。
   * 当 ResultSet 的指定列类型为 java.sql.Types 的 SQLXML 时，此方法的行为与 ResultSet.getString() 相同。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可读，并且根据实现也可能变得不可写。
   *
   * @return 由此 SQLXML 实例指定的 XML 值的字符串表示形式。
   * @throws SQLException 如果处理 XML 值时发生错误。
   *   异常的 getCause() 方法可能提供更详细的异常，例如，如果流中包含无效字符。
   *   如果状态不可读，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持此方法
   * @since 1.6
   */
  String getString() throws SQLException;

  /**
   * 将由此 SQLXML 实例指定的 XML 值设置为给定的字符串表示形式。
   * 此字符串的格式由 org.xml.sax.InputSource 定义，
   * 其中流中的字符表示 XML 的 unicode 代码点，根据 XML 1.0 规范的第 2 节和附录 B。
   * 尽管可能存在其他编码声明，但字符串的编码为 unicode。
   * 当 ResultSet 的指定列类型为 java.sql.Types 的 SQLXML 时，此方法的行为与 ResultSet.updateString() 相同。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可写，并且根据实现也可能变得不可读。
   *
   * @param value XML 值
   * @throws SQLException 如果处理 XML 值时发生错误。
   *   异常的 getCause() 方法可能提供更详细的异常，例如，如果流中包含无效字符。
   *   如果状态不可写，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持此方法
   * @since 1.6
   */
  void setString(String value) throws SQLException;


  /**
   * 返回一个用于读取此 SQLXML 实例指定的 XML 值的 Source。
   * Source 用作 XML 解析器和 XSLT 转换器的输入。
   * <p>
   * 用于 XML 解析器的 Source 默认情况下将启用命名空间处理。
   * Source 的 systemID 取决于实现。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可读，并且根据实现也可能变得不可写。
   * <p>
   * 请注意，SAX 是一种回调架构，因此返回的
   * SAXSource 应设置一个内容处理器，该处理器将
   * 从解析中接收 SAX 事件。内容处理器
   * 将根据 XML 的内容接收回调。
   * <pre>
   *   SAXSource saxSource = sqlxml.getSource(SAXSource.class);
   *   XMLReader xmlReader = saxSource.getXMLReader();
   *   xmlReader.setContentHandler(myHandler);
   *   xmlReader.parse(saxSource.getInputSource());
   * </pre>
   *
   * @param <T> 由此类对象建模的类的类型
   * @param sourceClass 源的类，或 null。
   * 如果类为 null，则将返回特定于供应商的 Source 实现。
   * 以下类至少支持以下类：
   * <pre>
   *   javax.xml.transform.dom.DOMSource - 返回一个 DOMSource
   *   javax.xml.transform.sax.SAXSource - 返回一个 SAXSource
   *   javax.xml.transform.stax.StAXSource - 返回一个 StAXSource
   *   javax.xml.transform.stream.StreamSource - 返回一个 StreamSource
   * </pre>
   * @return 用于读取 XML 值的 Source。
   * @throws SQLException 如果处理 XML 值时发生错误
   *   或者此功能不受支持。
   *   异常的 getCause() 方法可能提供更详细的异常，例如，
   *   如果发生 XML 解析器异常。
   *   如果状态不可读，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持
   * 此方法
   * @since 1.6
   */
  <T extends Source> T getSource(Class<T> sourceClass) throws SQLException;

  /**
   * 返回一个用于设置此 SQLXML 实例指定的 XML 值的 Result。
   * <p>
   * Result 的 systemID 取决于实现。
   * <p>
   * 调用此方法后，SQL XML 对象将变得不可写，并且根据实现也可能变得不可读。
   * <p>
   * 请注意，SAX 是一种回调架构，返回的
   * SAXResult 已分配了一个内容处理器，该处理器将接收
   * 基于 XML 内容的 SAX 事件。使用 XML 文档的内容调用
   * 内容处理器以分配值。
   * <pre>
   *   SAXResult saxResult = sqlxml.setResult(SAXResult.class);
   *   ContentHandler contentHandler = saxResult.getXMLReader().getContentHandler();
   *   contentHandler.startDocument();
   *   // 将 XML 元素和属性设置到结果中
   *   contentHandler.endDocument();
   * </pre>
   *
   * @param <T> 由此类对象建模的类的类型
   * @param resultClass 结果的类，或 null。
   * 如果 resultClass 为 null，则将返回特定于供应商的 Result 实现。
   * 以下类至少支持以下类：
   * <pre>
   *   javax.xml.transform.dom.DOMResult - 返回一个 DOMResult
   *   javax.xml.transform.sax.SAXResult - 返回一个 SAXResult
   *   javax.xml.transform.stax.StAXResult - 返回一个 StAXResult
   *   javax.xml.transform.stream.StreamResult - 返回一个 StreamResult
   * </pre>
   * @return 返回一个用于设置 XML 值的 Result。
   * @throws SQLException 如果处理 XML 值时发生错误
   *   或者此功能不受支持。
   *   异常的 getCause() 方法可能提供更详细的异常，例如，
   *   如果发生 XML 解析器异常。
   *   如果状态不可写，则抛出异常。
   * @exception SQLFeatureNotSupportedException 如果 JDBC 驱动程序不支持
   * 此方法
   * @since 1.6
   */
  <T extends Result> T setResult(Class<T> resultClass) throws SQLException;

}
