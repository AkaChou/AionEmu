package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 台账 XML 解析助手（2026-10-03 台账 XML 化批）：八张自造台账（原 TSV）的统一安全解析面。
 * <p>
 * 口径 = 原 {@code RetailQuestDriver.parse} 的加固配置（FEATURE_SECURE_PROCESSING、禁 DOCTYPE、
 * 禁外部 DTD/Schema）——台账由本仓生成、永不带 DTD；行 = 根的直接子元素（保文档序，注释节点
 * 天然跳过）；列 = 行内同名直接子元素文本（trim；缺席返回 {@code null}，镜像旧「列缺席」语义）。
 * <p>
 * 领域语义与 fail-closed 错误码留在各消费方；本类不碰语义。既有五处同义工厂配置
 * （RetailQuestXmlTable / RetailSimpleHuntTable / DataDrivenQuestTable / NativeQuestXmlTable /
 * HtmlPagesRegistry）为批前遗产，不在本批统一。
 * <p>
 * Shared secure-parsing helper for the eight self-made ledger XMLs (the 2026-10-03 ledger-XML
 * batch): parser hardening copied from the former {@code RetailQuestDriver.parse} (secure
 * processing, DOCTYPE disallowed, external DTD/Schema access denied — the ledgers are generated
 * in-repo and never carry a DTD). Rows are the root's direct element children in document order
 * (comment nodes are skipped naturally); a column is the row's same-name direct child text
 * (trimmed; absent = {@code null}, mirroring the old missing-column semantics). Domain semantics
 * and fail-closed codes stay with the callers; the five pre-batch factory setups
 * (RetailQuestXmlTable / RetailSimpleHuntTable / DataDrivenQuestTable / NativeQuestXmlTable /
 * HtmlPagesRegistry) are legacy and intentionally not unified here.
 */
public final class RetailLedgerXml {

	private RetailLedgerXml() {
	}

	/**
	 * 安全解析台账 XML 并关闭输入流。 / Securely parses a ledger XML and closes the input stream.
	 */
	public static Document parse(InputStream input) throws IOException {
		try (input) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			return factory.newDocumentBuilder().parse(input);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("ledger xml parse failed", e);
		}
	}

	/**
	 * 根的直接子元素中指定行标签的行（保文档序）。 / The root's direct children with the given row tag, in document order.
	 */
	public static List<Element> rows(Document document, String rowTag) {
		List<Element> rows = new ArrayList<>();
		for (Node node = document.getDocumentElement().getFirstChild(); node != null;
				node = node.getNextSibling()) {
			if (node instanceof Element element && rowTag.equals(element.getTagName())) {
				rows.add(element);
			}
		}
		return rows;
	}

	/**
	 * 行内某列文本（trim；列缺席返回 {@code null}）。 / The column text (trimmed; absent column = {@code null}).
	 */
	public static String text(Element row, String tag) {
		for (Node node = row.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && tag.equals(element.getTagName())) {
				return element.getTextContent().trim();
			}
		}
		return null;
	}
}
