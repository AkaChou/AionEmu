package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.ExclusiveAttribute;

import jakarta.xml.bind.JAXBContext;

/**
 * 从拆分包中的分卷 XML 加载技能定义，并展开技能组与字段引用。
 * Loads skill definitions from the split XML parts, expanding skill groups and field references.
 */
final class SkillDefinitionLoader {

	static SkillData load(File directory) {
		return load(directory, new ConcurrentHashMap<>());
	}

	/**
	 * 加载全部技能分片，并把串行墙钟阶段与并行分片工作量写入共享计时表。
	 * Loads all skill parts and writes serial wall-time phases plus parallel part work into the shared timing table.
	 *
	 * @param directory 技能定义目录 / skill definition directory
	 * @param phaseTimings 线程安全的阶段计时表 / thread-safe phase timing map
	 * @return 已初始化的技能数据 / initialized skill data
	 */
	static SkillData load(File directory, ConcurrentMap<String, Long> phaseTimings) {
		try {
			long indexGroupsStart = System.nanoTime();
			Document index = parse(new File(directory, "index.xml"));
			Element bundle = index.getDocumentElement();
			Document groupsDocument = parse(new File(directory, requiredAttribute(bundle, "groups")));
			Map<String, String> fieldNames = readFieldNames(groupsDocument);
			Map<String, Element> groups = readGroups(groupsDocument);
			int expectedTemplates = Integer.parseInt(requiredAttribute(bundle, "templates"));
			List<File> partFiles = new ArrayList<>();
			for (Element part : childElements(bundle, "part")) {
				partFiles.add(new File(directory, requiredAttribute(part, "file")));
			}
			phaseTimings.put("SkillIndexGroupsWall", elapsedMillis(System.nanoTime() - indexGroupsStart));
			// 各 part 互不依赖，groups/fieldNames 只读共享；并行解析后在调用线程按 part
			// 顺序拼接，保证模板列表确定性。Unmarshaller 非线程安全，每个任务经共享
			// JAXBContext 自行创建。
			// Parts are independent and groups/fieldNames are read-only shared; parse in parallel and
			// concatenate in part order on the calling thread for deterministic output. Unmarshaller is
			// not thread-safe, so each task creates its own from the shared JAXBContext.
			long jaxbStart = System.nanoTime();
			JAXBContext jaxbContext = XmlDataLoader.createJaxbContext(SkillData.class);
			phaseTimings.put("SkillJaxbContextWall", elapsedMillis(System.nanoTime() - jaxbStart));
			List<CompletableFuture<LoadedSkillPart>> futures = new ArrayList<>(partFiles.size());
			for (File partFile : partFiles) {
				futures.add(CompletableFuture.supplyAsync(() -> loadPart(partFile, groups, fieldNames, jaxbContext),
					XmlDataLoader.staticDataExecutor()));
			}

			List<SkillTemplate> templates = new ArrayList<>(expectedTemplates);
			long streamJaxbNanos = 0;
			for (CompletableFuture<LoadedSkillPart> future : futures) {
				LoadedSkillPart part = future.join();
				templates.addAll(part.templates());
				streamJaxbNanos += part.streamJaxbNanos();
			}
			// 流解析与 JAXB 同步消费，无法无干扰地拆开计时；这里记录全部并行分片的总工作量。
			// Streaming parse and JAXB consume synchronously and cannot be timed separately without interference;
			// this records the total work across all parallel parts.
			phaseTimings.put("SkillStreamJaxbWork", elapsedMillis(streamJaxbNanos));
			if (templates.size() != expectedTemplates) {
				throw new IllegalStateException("Expected " + expectedTemplates + " skill templates, loaded " + templates.size());
			}
			SkillData data = new SkillData();
			data.setSkillTemplates(templates);
			if (bundle.hasAttribute("exclusive_attributes")) {
				Document exclusiveDocument = parse(new File(directory, requiredAttribute(bundle, "exclusive_attributes")));
				data.setExclusiveAttributes(readExclusiveDefinitions(exclusiveDocument), readExclusiveItems(exclusiveDocument));
			}
			long cooldownStart = System.nanoTime();
			data.initializeCooldownGroups();
			phaseTimings.put("SkillCooldownWall", elapsedMillis(System.nanoTime() - cooldownStart));
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load compact skill definitions from " + directory.getPath(), e);
		}
	}

	/**
	 * 单遍解析技能分卷：SAX 在流中展开组与字段引用，并把事件直接交给 JAXB。
	 * Parses one skill part in one pass: SAX expands group/field references and feeds events directly to JAXB.
	 */
	private static LoadedSkillPart loadPart(File partFile, Map<String, Element> groups, Map<String, String> fieldNames,
			JAXBContext jaxbContext) {
		try (FileInputStream stream = new FileInputStream(partFile)) {
			long start = System.nanoTime();
			SkillExpansionFilter filter = new SkillExpansionFilter(createPartReader(), groups, fieldNames);
			InputSource input = new InputSource(stream);
			input.setSystemId(partFile.toURI().toString());
			SkillData data = (SkillData) jaxbContext.createUnmarshaller().unmarshal(new SAXSource(filter, input));
			return new LoadedSkillPart(data.getSkillTemplates(), System.nanoTime() - start);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load skill part " + partFile.getPath(), e);
		}
	}

	private static XMLReader createPartReader() throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setXIncludeAware(false);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		return factory.newSAXParser().getXMLReader();
	}

	private static Map<String, ExclusiveAttribute> readExclusiveDefinitions(Document document) {
		Map<String, ExclusiveAttribute> result = new HashMap<>();
		for (Element definition : childElements(document.getDocumentElement(), "definition")) {
			String name = requiredAttribute(definition, "name");
			ExclusiveAttribute value = new ExclusiveAttribute(name, requiredAttribute(definition, "tag"),
				integerAttribute(definition, "normal_percent"), integerAttribute(definition, "normal_flat"),
				integerAttribute(definition, "skill_percent"), integerAttribute(definition, "skill_flat"),
				integerAttribute(definition, "status_immune"));
			if (result.put(name, value) != null) {
				throw new IllegalStateException("Duplicate exclusive attribute: " + name);
			}
		}
		return result;
	}

	private static Map<Integer, Set<String>> readExclusiveItems(Document document) {
		Map<Integer, Set<String>> result = new HashMap<>();
		for (Element item : childElements(document.getDocumentElement(), "item")) {
			int id = Integer.parseInt(requiredAttribute(item, "id"));
			Set<String> names = Set.of(requiredAttribute(item, "names").split(" "));
			if (result.put(id, names) != null) {
				throw new IllegalStateException("Duplicate exclusive item: " + id);
			}
		}
		return result;
	}

	private static int integerAttribute(Element element, String name) {
		return element.hasAttribute(name) ? Integer.parseInt(element.getAttribute(name)) : 0;
	}

	private static Map<String, String> readFieldNames(Document document) {
		Map<String, String> fieldNames = new HashMap<>();
		for (Element fieldNamesElement : childElements(document.getDocumentElement(), "field_names")) {
			for (Element field : childElements(fieldNamesElement, "field")) {
				fieldNames.put(requiredAttribute(field, "id"), requiredAttribute(field, "name"));
			}
		}
		return fieldNames;
	}

	private static Map<String, Element> readGroups(Document document) {
		Map<String, Element> groups = new HashMap<>();
		for (Element groupsElement : childElements(document.getDocumentElement(), "groups")) {
			for (Element group : childElements(groupsElement, "group")) {
				Element fragment = firstChildElement(group);
				if (fragment == null) {
					throw new IllegalStateException("Empty skill group: " + requiredAttribute(group, "id"));
				}
				groups.put(requiredAttribute(group, "id"), fragment);
			}
		}
		return groups;
	}

	private static Document parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory.newDocumentBuilder().parse(file);
	}

	private static long elapsedMillis(long elapsedNanos) {
		return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
	}

	/**
	 * 保存一个并行技能分片的结果及流解析工作量。
	 * Holds one parallel skill part result and its streaming parse work.
	 */
	private record LoadedSkillPart(List<SkillTemplate> templates, long streamJaxbNanos) {
	}

	/**
	 * 把紧凑技能节点转换为完整 JAXB 事件流，不构建分片 DOM；每个加载任务独占实例。
	 * Converts compact skill nodes into the complete JAXB event stream without a part DOM; each load task owns
	 * one instance.
	 */
	private static final class SkillExpansionFilter extends XMLFilterImpl {

		private final Map<String, Element> groups;
		private final Map<String, String> fieldNames;
		private final Deque<String> elements = new ArrayDeque<>();
		private int suppressedGroupDepth;
		private int retailDepth;

		private SkillExpansionFilter(XMLReader parent, Map<String, Element> groups, Map<String, String> fieldNames) {
			super(parent);
			this.groups = groups;
			this.fieldNames = fieldNames;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (suppressedGroupDepth > 0) {
				suppressedGroupDepth++;
				return;
			}
			if (retailDepth > 0) {
				emitRetailField(localName, qName, attributes);
				retailDepth++;
				return;
			}

			String name = elementName(localName, qName);
			String parent = elements.peek();
			if ("skill_template".equals(parent) && "group_ref".equals(name)) {
				String id = requiredAttribute(attributes, "id", name);
				Element group = groups.get(id);
				if (group == null) {
					throw new SAXException("Unknown skill group: " + id);
				}
				emitElement(group);
				suppressedGroupDepth = 1;
				return;
			}
			if ("skill_template".equals(parent) && "retail".equals(name)) {
				super.startElement("", "retail_fields", "retail_fields", new AttributesImpl());
				retailDepth = 1;
				return;
			}

			super.startElement(uri, localName, qName, attributes);
			elements.push(name);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (suppressedGroupDepth > 0) {
				suppressedGroupDepth--;
				return;
			}
			if (retailDepth > 0) {
				retailDepth--;
				if (retailDepth == 0) {
					super.endElement("", "retail_fields", "retail_fields");
				}
				return;
			}

			super.endElement(uri, localName, qName);
			elements.pop();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (suppressedGroupDepth == 0 && retailDepth == 0) {
				super.characters(ch, start, length);
			}
		}

		private void emitRetailField(String localName, String qName, Attributes attributes) throws SAXException {
			if (retailDepth != 1 || !"f".equals(elementName(localName, qName))) {
				return;
			}
			String id = requiredAttribute(attributes, "i", "f");
			String name = fieldNames.get(id);
			if (name == null) {
				throw new SAXException("Unknown retail skill field: " + id);
			}
			AttributesImpl expanded = new AttributesImpl();
			addAttribute(expanded, "name", name);
			String occurrence = attributes.getValue("o");
			if (occurrence != null) {
				addAttribute(expanded, "occurrence", occurrence);
			}
			addAttribute(expanded, "value", attributes.getValue("v"));
			super.startElement("", "field", "field", expanded);
			super.endElement("", "field", "field");
		}

		private void emitElement(Element element) throws SAXException {
			AttributesImpl attributes = new AttributesImpl();
			for (int i = 0; i < element.getAttributes().getLength(); i++) {
				Node attribute = element.getAttributes().item(i);
				addAttribute(attributes, attribute.getNodeName(), attribute.getNodeValue());
			}
			String name = element.getTagName();
			super.startElement("", name, name, attributes);
			for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child instanceof Element childElement) {
					emitElement(childElement);
				} else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
					String value = child.getNodeValue();
					if (!value.isBlank()) {
						char[] text = value.toCharArray();
						super.characters(text, 0, text.length);
					}
				}
			}
			super.endElement("", name, name);
		}

		private static void addAttribute(AttributesImpl attributes, String name, String value) {
			attributes.addAttribute("", name, name, "CDATA", value == null ? "" : value);
		}

		private static String requiredAttribute(Attributes attributes, String name, String element) throws SAXException {
			String value = attributes.getValue(name);
			if (value == null || value.isEmpty()) {
				throw new SAXException("Missing " + name + " on " + element);
			}
			return value;
		}

		private static String elementName(String localName, String qName) {
			return localName == null || localName.isEmpty() ? qName : localName;
		}
	}

	private static List<Element> childElements(Element parent, String name) {
		List<Element> elements = new ArrayList<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && element.getTagName().equals(name)) {
				elements.add(element);
			}
		}
		return elements;
	}

	private static Element firstChildElement(Element parent) {
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element) {
				return element;
			}
		}
		return null;
	}

	private static String requiredAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if (value.isEmpty()) {
			throw new IllegalStateException("Missing " + name + " on " + element.getTagName());
		}
		return value;
	}
}
