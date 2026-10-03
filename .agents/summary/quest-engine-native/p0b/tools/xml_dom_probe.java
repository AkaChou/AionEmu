// 真端表 DOCTYPE/实体语义探针（分析工具，非生产代码）。
// Probe for retail table DOCTYPE/entity semantics (analysis tool, not production code).
//
// 用法 / Usage:
//   java xml_dom_probe.java dump  <file> <out>          —— 全文档规范形转储（DOM 级等价基线）
//   java xml_dom_probe.java value <file> <tag> [limit]  —— 打印指定标签的文本值（含实体展开结果）
//
// 解析器配置与运行时加载器（NativeQuestTableLoader/NativeQuestXmlTable/RetailQuestXmlTable/
// RetailSimpleHuntTable）完全一致：FEATURE_SECURE_PROCESSING=true、外部 DTD/schema 拒绝、
// 实体展开开、DOCTYPE 允许（内部子集参与解析）。输出文本按 Unicode 转义（反斜杠 u 十六进制），保证可 diff。

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class xml_dom_probe {

	public static void main(String[] args) throws Exception {
		String mode = args[0];
		Document doc = parse(new File(args[1]));
		switch (mode) {
			case "dump" -> dump(doc, Path.of(args[2]));
			case "value" -> value(doc, args[2], args.length > 3 ? Integer.parseInt(args[3]) : 20);
			case "rowshape" -> rowshape(doc, args[2]);
			default -> throw new IllegalArgumentException("mode: dump|value|rowshape");
		}
	}

	private static Document parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setExpandEntityReferences(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(file);
	}

	/** 规范形：每元素一行 path \t attrs \t text（叶节点文本精确；父节点仅空白折叠后聚合）。 */
	private static void dump(Document doc, Path out) throws Exception {
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
			Element root = doc.getDocumentElement();
			walk(root, "/" + root.getTagName(), writer);
		}
	}

	private static void walk(Element element, String path, PrintWriter writer) {
		StringBuilder attrs = new StringBuilder();
		var attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attr = attributes.item(i);
			attrs.append(attr.getNodeName()).append('=').append(esc(attr.getNodeValue())).append(';');
		}
		boolean leaf = true;
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				leaf = false;
				break;
			}
		}
		String text;
		if (leaf) {
			text = esc(element.getTextContent());
		} else {
			text = esc(element.getTextContent().replaceAll("\\s+", " ").trim());
		}
		writer.println(path + "\t" + attrs + "\t" + text);
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element child) {
				walk(child, path + "/" + child.getTagName(), writer);
			}
		}
	}

	/** 行形状：每行一行输出「直接子元素名×出现次数」（升序、逗号分隔），供聚合唯一签名。 */
	private static void rowshape(Document doc, String rowTag) {
		NodeList rows = doc.getElementsByTagName(rowTag);
		for (int i = 0; i < rows.getLength(); i++) {
			Element row = (Element) rows.item(i);
			java.util.TreeMap<String, Integer> counts = new java.util.TreeMap<>();
			NodeList children = row.getChildNodes();
			for (int c = 0; c < children.getLength(); c++) {
				if (children.item(c) instanceof Element child) {
					counts.merge(child.getTagName(), 1, Integer::sum);
				}
			}
			StringBuilder sb = new StringBuilder();
			counts.forEach((tag, n) -> sb.append(tag).append(n > 1 ? "x" + n : "").append(','));
			System.out.println(sb);
		}
	}

	private static void value(Document doc, String tag, int limit) {
		NodeList nodes = doc.getElementsByTagName(tag);
		for (int i = 0; i < nodes.getLength() && i < limit; i++) {
			System.out.println(i + ": [" + esc(nodes.item(i).getTextContent()) + "]");
		}
		System.out.println("total <" + tag + "> = " + nodes.getLength());
	}

	private static String esc(String s) {
		if (s == null) {
			return "<null>";
		}
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 0x20 && c < 0x7f) {
				sb.append(c);
			} else {
				sb.append(String.format("\\u%04x", (int) c));
			}
		}
		return sb.toString();
	}
}
