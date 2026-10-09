package com.aionemu.gameserver.questEngine.tablelane;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 原版 {@code HtmlPages.xml} 页注册表（P0b 数据基础，计划 §4.6.1/§6.2）。
 * <p>
 * 数据来源为原版入仓副本的精简形（UTF-8 转码、DOCTYPE/实体子集移除、重排版；字符级语义等价、
 * 5904 行逐条对拍，schema = 同目录 {@code HtmlPages.xsd}），
 * 本类只读、启动期一次构建、
 * 不可变；未知页/重复 id/重复页名一律 fail-closed（语义错误码 {@code NATIVE_PAGE_UNREGISTERED} /
 * {@code NATIVE_TABLE_PARSE_FAILED}）。本类不接任务路由、不做任何语义修正——那是后续批次的事。
 * <p>
 * The retail {@code HtmlPages.xml} page registry (P0b data foundation, plan §4.6.1/§6.2). Backed by the
 * simplified form of the ingested retail copy (UTF-8 transcode, DOCTYPE/entity subset removed, reformatted;
 * char-level semantic equal, all 5904 rows verified item by item; schema in the sibling
 * {@code HtmlPages.xsd}); read-only, built once at
 * startup, immutable; unknown pages, duplicate ids and duplicate page names fail closed (semantic codes
 * {@code NATIVE_PAGE_UNREGISTERED} / {@code NATIVE_TABLE_PARSE_FAILED}). No routing and no semantic
 * fix-ups happen here — those belong to later batches.
 */
public final class HtmlPagesRegistry {

	/** 单页行：id + 常量名 + 可选页文件名。 / One page row: id + constant name + optional page file name. */
	public record HtmlPage(int id, String name, String htmlPagename) {
	}

	private static final String RESOURCE = "aion/data/static_data/quest/retail/HtmlPages.xml";
	private static final String EXPECTED_ROOT = "htmlpages";
	private static final String ROW_TAG = "htmlpage";

	private static volatile HtmlPagesRegistry instance;

	private final Map<Integer, HtmlPage> pagesById;
	private final Map<String, Integer> idsByHtmlPagename;

	private HtmlPagesRegistry(Map<Integer, HtmlPage> pagesById, Map<String, Integer> idsByHtmlPagename) {
		this.pagesById = pagesById;
		this.idsByHtmlPagename = idsByHtmlPagename;
	}

	/** 已装载的注册表（未装载则先装载）。 / The loaded registry; loads it first when absent. */
	public static HtmlPagesRegistry instance() {
		HtmlPagesRegistry local = instance;
		if (local == null) {
			synchronized (HtmlPagesRegistry.class) {
				local = instance;
				if (local == null) {
					local = load(HtmlPagesRegistry.class.getClassLoader());
					instance = local;
				}
			}
		}
		return local;
	}

	/** 启动期强制装载（失败即异常，由调用方决定是否终止启动）。 / Eagerly loads at startup; throws on failure. */
	public static void ensureLoaded() {
		instance();
	}

	/** 从 classpath 装载并解析页表。 / Loads and parses the page table from the classpath. */
	static HtmlPagesRegistry load(ClassLoader loader) {
		try (InputStream input = loader.getResourceAsStream(RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: missing " + RESOURCE);
			}
			return parse(input);
		} catch (IOException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: cannot read " + RESOURCE, e);
		}
	}

	/** 解析页表字节流（包内可见供负例测试）。 / Parses the page-table stream (package-visible for negative tests). */
	static HtmlPagesRegistry parse(InputStream input) throws IOException {
		Document document;
		try {
			document = newDocumentBuilder().parse(input);
		} catch (org.xml.sax.SAXException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: malformed HtmlPages.xml", e);
		}
		Element root = document.getDocumentElement();
		if (root == null || !EXPECTED_ROOT.equals(root.getTagName())) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: expected root <" + EXPECTED_ROOT + ">, got <"
					+ (root == null ? "(none)" : root.getTagName()) + ">");
		}
		Map<Integer, HtmlPage> pagesById = new LinkedHashMap<>();
		Map<String, Integer> idsByHtmlPagename = new LinkedHashMap<>();
		NodeList rows = root.getChildNodes();
		for (int i = 0; i < rows.getLength(); i++) {
			Node row = rows.item(i);
			if (!(row instanceof Element element) || !ROW_TAG.equals(element.getTagName())) {
				continue;
			}
			int id = requireInt(element, "id");
			String name = requireText(element, "name");
			String htmlPagename = optionalText(element, "htmlpagename");
			if (htmlPagename != null && htmlPagename.isBlank()) {
				// 空元素视为未声明，避免空串成为页名索引键。
				// Treat an empty element as undeclared so "" never becomes a page-name key.
				htmlPagename = null;
			}
			if (pagesById.putIfAbsent(id, new HtmlPage(id, name, htmlPagename)) != null) {
				throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: duplicate htmlpage id " + id);
			}
			if (htmlPagename != null && idsByHtmlPagename.putIfAbsent(htmlPagename, id) != null) {
				throw new IllegalStateException(
						"NATIVE_TABLE_PARSE_FAILED: duplicate htmlpagename " + htmlPagename);
			}
		}
		if (pagesById.isEmpty()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: " + RESOURCE + " has no rows");
		}
		return new HtmlPagesRegistry(pagesById, Map.copyOf(idsByHtmlPagename));
	}

	private static DocumentBuilder newDocumentBuilder() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// 保留内部 DTD 子集兼容（解析器能力面，负例测试仍依赖）；拒绝一切外部 DTD/实体为纵深防御
			// （当前精简副本已无 DOCTYPE）。
			// Keep internal-DTD-subset support (parser capability; negative tests still rely on it) and
			// refuse all external DTDs/entities as defense in depth (the simplified copy carries no DOCTYPE).
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			factory.setExpandEntityReferences(true);
			return factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: cannot configure XML parser", e);
		}
	}

	private static int requireInt(Element row, String tag) {
		String raw = requireText(row, tag);
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: <" + tag + "> is not a number: " + raw);
		}
	}

	private static String requireText(Element row, String tag) {
		String value = optionalText(row, tag);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: <" + ROW_TAG + "> is missing <" + tag + ">");
		}
		return value;
	}

	private static String optionalText(Element row, String tag) {
		NodeList children = row.getElementsByTagName(tag);
		if (children.getLength() == 0) {
			return null;
		}
		return children.item(0).getTextContent().trim();
	}

	/** 注册表行数。 / The number of page rows. */
	public int size() {
		return pagesById.size();
	}

	/** 按页 id 查询。 / Looks a page up by id. */
	public Optional<HtmlPage> find(int id) {
		return Optional.ofNullable(pagesById.get(id));
	}

	/** 按页 id 查询，未知页 fail-closed。 / Looks a page up by id; unknown pages fail closed. */
	public HtmlPage require(int id) {
		HtmlPage page = pagesById.get(id);
		if (page == null) {
			throw new IllegalStateException("NATIVE_PAGE_UNREGISTERED: html page id " + id);
		}
		return page;
	}

	/** 页文件名 → 页 id（仅覆盖声明了 htmlpagename 的行）。 / Page file name → page id (declared names only). */
	public Optional<Integer> idByHtmlPagename(String htmlPagename) {
		return Optional.ofNullable(idsByHtmlPagename.get(htmlPagename));
	}
}
