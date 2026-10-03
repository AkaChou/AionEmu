package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.w3c.dom.Element;

/**
 * 测试夹具：台账 XML 行读取（2026-10-03 台账 XML 化批）。复用 main 的 {@link RetailLedgerXml}，
 * 路径参数化以同时支持 main 副本（{@code /aion/data/static_data/quest/retail/…}）与 test 副本
 * （{@code /quest/…}）；夹具不合成语义——各测试保留自己的列语义与断言。
 * <p>
 * Test fixture: row access for the ledger XMLs (the 2026-10-03 ledger-XML batch), reusing the
 * main-side {@link RetailLedgerXml}. The classpath resource is a parameter so both the main copy
 * ({@code /aion/data/static_data/quest/retail/…}) and the test copy ({@code /quest/…}) work. The
 * fixture never synthesizes semantics — each test keeps its own column semantics and assertions.
 */
public final class RetailLedgerRows {

	private RetailLedgerRows() {
	}

	/**
	 * 读取 classpath 资源中指定行标签的行（保文档序；缺资源即断言失败）。
	 * The rows with the given row tag from the classpath resource, in document order.
	 */
	public static List<Element> rows(String classpathResource, String rowTag) throws IOException {
		InputStream input = RetailLedgerRows.class.getResourceAsStream(classpathResource);
		if (input == null) {
			throw new IOException("missing resource " + classpathResource);
		}
		return RetailLedgerXml.rows(RetailLedgerXml.parse(input), rowTag);
	}

	/** 行内某列文本（trim；列缺席返回 {@code null}）。 / The column text (trimmed; absent = null). */
	public static String cell(Element row, String tag) {
		return RetailLedgerXml.text(row, tag);
	}

	/** 行内某列的 int 值。 / The int value of a column. */
	public static int intCell(Element row, String tag) {
		return Integer.parseInt(RetailLedgerXml.text(row, tag));
	}
}
