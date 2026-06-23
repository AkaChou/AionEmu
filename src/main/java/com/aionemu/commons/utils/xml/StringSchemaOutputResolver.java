package com.aionemu.commons.utils.xml;

import java.io.IOException;
import java.io.StringWriter;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * XML Schema字符串输出解析器
 * XML Schema String Output Resolver
 * <p>
 * 继承自JAXB SchemaOutputResolver，用于将生成的XML Schema内容保存到字符串中
 * Extends JAXB SchemaOutputResolver to save generated XML Schema content to string
 */
public class StringSchemaOutputResolver extends SchemaOutputResolver {
    private StringWriter sw = null;

    /**
     * 创建XML Schema输出结果
     * Create XML Schema output result
     *
     * @param namespaceUri    命名空间URI (Namespace URI)
     * @param suggestedFileName 建议的文件名 (Suggested file name)
     * @return 包含字符串写入器的StreamResult对象
     *         StreamResult object with string writer
     * @throws IOException 如果发生I/O错误时抛出
     *                     Thrown if I/O error occurs
     */
    public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
        this.sw = new StringWriter();
        StreamResult sr = new StreamResult();
        sr.setSystemId(String.valueOf(System.currentTimeMillis()));
        sr.setWriter(this.sw);
        return sr;
    }

    /**
     * 获取生成的XML Schema内容
     * Get generated XML Schema content
     *
     * @return Schema字符串，如果未初始化则返回null
     *         Schema string, returns null if not initialized
     */
    public String getSchema() {
        return this.sw != null ? this.sw.toString() : null;
    }
}
