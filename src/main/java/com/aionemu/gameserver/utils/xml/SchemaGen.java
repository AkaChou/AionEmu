package com.aionemu.gameserver.utils.xml;

import java.io.File;
import java.io.IOException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import com.aionemu.gameserver.dataholders.StaticData;

/**
 * 从 StaticData JAXB 模型生成 XSD 的工具。
 * Utility that generates an XSD from the StaticData JAXB model.
 */
public class SchemaGen {

	/**
	 * 在指定目录生成 static_data1.xsd。
	 * Generate static_data1.xsd under the given directory.
	 *
	 * Output directory
	 *
	 * @param baseDir @throws Exception 生成失败时 / On generation failure
	 */
	public static void generateStaticDataSchema(File baseDir) throws Exception {
		class MySchemaOutputResolver extends SchemaOutputResolver {

			/**
			 * 创建 schema 输出目标。
			 * Create the schema output target.
			 *
			 * Namespace URI
			 * @param suggestedFileName 建议文件名 / Suggested file name
			 * Output Result
			 * On I/O error
			 */
			@Override
			public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
				return new StreamResult(new File(baseDir, "static_data1.xsd"));
			}
		}
		JAXBContext context = JAXBContext.newInstance(StaticData.class);
		context.generateSchema(new MySchemaOutputResolver());
	}
}
