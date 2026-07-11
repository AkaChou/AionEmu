package com.aionemu.commons.utils.xml;

import java.io.IOException;
import java.io.StringWriter;
import jakarta.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * 将生成的 XML Schema 写入字符串的输出解析器。
 * Schema output resolver that writes generated XML Schema into a string.
 */
public class StringSchemaOutputResolver extends SchemaOutputResolver {

    private StringWriter sw = null;

    /**
     * 创建 Schema 输出结果。
     * Create the schema output result.
     *
     * Namespace URI
     * @param suggestedFileName 建议文件名 / Suggested file name
     * StreamResult
     * On I/O failure
     */
    @Override
    public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
        this.sw = new StringWriter();
        StreamResult sr = new StreamResult();
        sr.setSystemId(String.valueOf(System.currentTimeMillis()));
        sr.setWriter(this.sw);
        return sr;
    }

    /**
     * 获取已生成的 Schema 字符串。
     * Get the generated schema string.
     *
     * @return Schema 文本，未初始化则为 null / Schema text, or null if not initialized
     */
    public String getSchema() {
        return this.sw != null ? this.sw.toString() : null;
    }
}
