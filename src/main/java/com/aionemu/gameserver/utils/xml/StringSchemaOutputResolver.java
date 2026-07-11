package com.aionemu.gameserver.utils.xml;

import jakarta.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 将 JAXB 生成的 Schema 写入内存字节流的输出解析器。
 * SchemaOutputResolver that captures generated schema into an in-memory stream.
 *
 * @author ginho1
 */
public class StringSchemaOutputResolver extends SchemaOutputResolver {

	/**
	 * Schema 内容缓冲。
	 * Schema content buffer.
	 */
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();

	/**
	 * 创建指向内存流的输出 Result。
	 * Create an output Result backed by the in-memory stream.
	 *
	 * Namespace URI
	 * @param suggestedFileName 建议文件名（设为 systemId） / Suggested file name (used as systemId)
	 * Output Result
	 * On I/O error
	 */
	@Override
	public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
		StreamResult result = new StreamResult(baos);
		result.setSystemId(suggestedFileName);
		return result;
	}

	/**
	 * 获取已生成的 Schema 文本。
	 * Get the generated schema text.
	 *
	 * Schema string
	 */
	public String getSchemaString() {
		return baos.toString();
	}
}
