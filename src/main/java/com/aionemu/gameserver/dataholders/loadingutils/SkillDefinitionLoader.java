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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.aionemu.commons.utils.xml.StreamingXmlBinder;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.ExclusiveAttribute;

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
			Map<String, ExpansionElement> groups = readGroups(groupsDocument);
			int expectedTemplates = Integer.parseInt(requiredAttribute(bundle, "templates"));
			List<File> partFiles = new ArrayList<>();
			for (Element part : childElements(bundle, "part")) {
				partFiles.add(new File(directory, requiredAttribute(part, "file")));
			}
			phaseTimings.put("SkillIndexGroupsWall", elapsedMillis(System.nanoTime() - indexGroupsStart));
			// 各 part 互不依赖，groups/fieldNames 只读共享；并行解析后在调用线程按 part
			// 顺序拼接，保证模板列表确定性。
			// Parts are independent and groups/fieldNames are read-only shared; parse in parallel and
			// concatenate in part order on the calling thread for deterministic output.
			List<LoadedSkillPart> loadedParts = XmlDataLoader.submitLargestFirst(partFiles,
				partFile -> loadPart(partFile, groups, fieldNames));
			List<SkillTemplate> templates = new ArrayList<>(expectedTemplates);
			long streamNanos = 0;
			for (LoadedSkillPart part : loadedParts) {
				templates.addAll(part.templates());
				streamNanos += part.streamNanos();
			}
			phaseTimings.put("SkillStreamWork", elapsedMillis(streamNanos));
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
	 * 单遍解析技能分卷：StAX 在流中展开组与字段引用，并直接绑定技能模板。
	 * Parses one skill part in one pass: StAX expands group/field references and binds templates directly.
	 */
	private static LoadedSkillPart loadPart(File partFile, Map<String, ExpansionElement> groups,
			Map<String, String> fieldNames) {
		try (FileInputStream stream = new FileInputStream(partFile)) {
			long start = System.nanoTime();
			XMLInputFactory factory = createPartInputFactory();
			XMLStreamReader delegate = factory.createXMLStreamReader(stream);
			SkillExpansionReader reader = new SkillExpansionReader(delegate, groups, fieldNames);
			List<SkillTemplate> templates = new ArrayList<>();
			try {
				while (reader.hasNext()) {
					int event = reader.next();
					if (event == XMLStreamConstants.START_ELEMENT
						&& "skill_template".equals(localName(reader))) {
						templates.add(StreamingXmlBinder.unmarshal(reader, SkillTemplate.class));
					}
				}
			} finally {
				reader.close();
			}
			return new LoadedSkillPart(templates, System.nanoTime() - start);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load skill part " + partFile.getPath(), e);
		}
	}

	private static XMLInputFactory createPartInputFactory() throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);
		return factory;
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
		return Map.copyOf(fieldNames);
	}

	private static Map<String, ExpansionElement> readGroups(Document document) {
		Map<String, ExpansionElement> groups = new HashMap<>();
		for (Element groupsElement : childElements(document.getDocumentElement(), "groups")) {
			for (Element group : childElements(groupsElement, "group")) {
				Element fragment = firstChildElement(group);
				if (fragment == null) {
					throw new IllegalStateException("Empty skill group: " + requiredAttribute(group, "id")
						+ " (children=" + group.getChildNodes().getLength() + ")");
				}
				groups.put(requiredAttribute(group, "id"), toExpansionElement(fragment));
			}
		}
		return Map.copyOf(groups);
	}

	/**
	 * 在串行阶段复制技能组 DOM，避免并行 SAX 任务共享非线程安全节点。
	 * Copies skill-group DOM during the serial phase so parallel SAX tasks never share non-thread-safe nodes.
	 * @param element 待复制的 DOM 元素 / DOM element to copy
	 * @return 不可变展开元素 / immutable expansion element
	 */
	private static ExpansionElement toExpansionElement(Element element) {
		List<ExpansionAttribute> attributes = new ArrayList<>();
		for (int i = 0; i < element.getAttributes().getLength(); i++) {
			Node attribute = element.getAttributes().item(i);
			attributes.add(new ExpansionAttribute(attribute.getNodeName(), attribute.getNodeValue()));
		}
		List<ExpansionNode> children = new ArrayList<>();
		for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element childElement) {
				children.add(toExpansionElement(childElement));
			} else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
				String value = child.getNodeValue();
				if (!value.isBlank()) {
					children.add(new ExpansionText(value));
				}
			}
		}
		return new ExpansionElement(element.getTagName(), List.copyOf(attributes), List.copyOf(children));
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
	private record LoadedSkillPart(List<SkillTemplate> templates, long streamNanos) {
	}

	/**
	 * 把紧凑技能节点转换为完整 StAX 事件流；组展开数据在并行任务之间以不可变树共享。
	 * Converts compact skill nodes into a complete StAX event stream; immutable expansion trees are shared
	 * between parallel tasks.
	 */
	private static final class SkillExpansionReader extends StreamReaderDelegate {

		private final Map<String, ExpansionElement> groups;
		private final Map<String, String> fieldNames;
		private final Deque<String> elements = new ArrayDeque<>();
		private final Deque<SyntheticEvent> pending = new ArrayDeque<>();
		private SyntheticEvent synthetic;
		private boolean insideRetail;

		private SkillExpansionReader(XMLStreamReader parent, Map<String, ExpansionElement> groups,
			Map<String, String> fieldNames) {
			super(parent);
			this.groups = groups;
			this.fieldNames = fieldNames;
		}

		@Override
		public boolean hasNext() {
			try {
				return !pending.isEmpty() || super.hasNext();
			} catch (XMLStreamException e) {
				throw new IllegalStateException("Failed to inspect skill part stream", e);
			}
		}

		@Override
		public int next() throws XMLStreamException {
			if (nextPending()) {
				return synthetic.type();
			}
			while (super.hasNext()) {
				int event = super.next();
				synthetic = null;
				if (insideRetail) {
					if (event == XMLStreamConstants.START_ELEMENT) {
						consumeElement();
						continue;
					}
					if (event == XMLStreamConstants.END_ELEMENT) {
						insideRetail = false;
						queue(new SyntheticEvent(XMLStreamConstants.END_ELEMENT, "retail_fields", List.of(), null));
						nextPending();
						return synthetic.type();
					}
					continue;
				}
				if (event == XMLStreamConstants.START_ELEMENT) {
					String name = localName(this);
					String parent = elements.peek();
					if ("skill_template".equals(parent) && "group_ref".equals(name)) {
						expandGroupReference();
						if (nextPending()) {
							return synthetic.type();
						}
						continue;
					}
					if ("skill_template".equals(parent) && "retail".equals(name)) {
						insideRetail = true;
						queue(new SyntheticEvent(XMLStreamConstants.START_ELEMENT, "retail_fields", List.of(), null));
						nextPending();
						return synthetic.type();
					}
					elements.push(name);
					return event;
				}
				if (event == XMLStreamConstants.END_ELEMENT) {
					elements.poll();
				}
				return event;
			}
			throw new XMLStreamException("Unexpected end of skill part");
		}

		@Override
		public int getEventType() {
			return synthetic == null ? super.getEventType() : synthetic.type();
		}

		@Override
		public String getLocalName() {
			return synthetic == null ? super.getLocalName() : synthetic.name();
		}

		@Override
		public int getAttributeCount() {
			return synthetic == null ? super.getAttributeCount() : synthetic.attributes().size();
		}

		@Override
		public String getAttributeLocalName(int index) {
			return synthetic == null ? super.getAttributeLocalName(index)
				: synthetic.attributes().get(index).name();
		}

		@Override
		public String getAttributeValue(int index) {
			return synthetic == null ? super.getAttributeValue(index)
				: synthetic.attributes().get(index).value();
		}

		@Override
		public String getText() {
			return synthetic == null ? super.getText() : synthetic.text();
		}

		private boolean nextPending() {
			if (pending.isEmpty()) {
				synthetic = null;
				return false;
			}
			synthetic = pending.removeFirst();
			return true;
		}

		private void expandGroupReference() throws XMLStreamException {
			String id = requiredAttribute(this, "id", "group_ref");
			ExpansionElement group = groups.get(id);
			if (group == null) {
				throw new XMLStreamException("Unknown skill group: " + id);
			}
			consumeElement();
			queueExpansion(group);
		}

		private ExpansionElement readExpansionElement() throws XMLStreamException {
			String name = localName(this);
			List<ExpansionAttribute> attributes = new ArrayList<>(getAttributeCount());
			for (int index = 0; index < getAttributeCount(); index++) {
				attributes.add(new ExpansionAttribute(getAttributeLocalName(index), getAttributeValue(index)));
			}
			List<ExpansionNode> children = new ArrayList<>();
			StringBuilder text = new StringBuilder();
			while (super.hasNext()) {
				int event = super.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					flushText(children, text);
					children.add(readExpansionElement());
				} else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
					text.append(super.getText());
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					flushText(children, text);
					return new ExpansionElement(name, List.copyOf(attributes), List.copyOf(children));
				}
			}
			throw new XMLStreamException("Unexpected end of skill group element " + name);
		}

		private void consumeElement() throws XMLStreamException {
			if (insideRetail && "f".equals(localName(this))) {
				String id = requiredAttribute(this, "i", "f");
				String name = fieldNames.get(id);
				if (name == null) {
					throw new XMLStreamException("Unknown retail skill field: " + id);
				}
				List<ExpansionAttribute> attributes = new ArrayList<>();
				attributes.add(new ExpansionAttribute("name", name));
				String occurrence = getAttributeValue(null, "o");
				if (occurrence != null) {
					attributes.add(new ExpansionAttribute("occurrence", occurrence));
				}
				attributes.add(new ExpansionAttribute("value", getAttributeValue(null, "v")));
				queue(new SyntheticEvent(XMLStreamConstants.START_ELEMENT, "field", attributes, null));
				queue(new SyntheticEvent(XMLStreamConstants.END_ELEMENT, "field", List.of(), null));
			}
			int depth = 0;
			while (super.hasNext()) {
				int event = super.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					depth++;
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					if (depth == 0) {
						return;
					}
					depth--;
				}
			}
			throw new XMLStreamException("Unexpected end of nested element");
		}

		private void queueExpansion(ExpansionElement element) {
			queue(new SyntheticEvent(XMLStreamConstants.START_ELEMENT, element.name(), element.attributes(), null));
			for (ExpansionNode child : element.children()) {
				if (child instanceof ExpansionElement childElement) {
					queueExpansion(childElement);
				} else if (child instanceof ExpansionText(String value)) {
					queue(new SyntheticEvent(XMLStreamConstants.CHARACTERS, null, List.of(), value));
				}
			}
			queue(new SyntheticEvent(XMLStreamConstants.END_ELEMENT, element.name(), List.of(), null));
		}

		private void queue(SyntheticEvent event) {
			pending.addLast(event);
		}

		private static void flushText(List<ExpansionNode> children, StringBuilder text) {
			if (!text.toString().isBlank()) {
				children.add(new ExpansionText(text.toString()));
			}
			text.setLength(0);
		}

		private static String requiredAttribute(XMLStreamReader reader, String name, String element)
			throws XMLStreamException {
			String value = reader.getAttributeValue(null, name);
			if (value == null || value.isEmpty()) {
				throw new XMLStreamException("Missing " + name + " on " + element);
			}
			return value;
		}
	}

	private record SyntheticEvent(int type, String name, List<ExpansionAttribute> attributes, String text) {
	}

	/**
	 * 技能组展开树中的不可变节点。
	 * Immutable node in a skill-group expansion tree.
	 */
	private sealed interface ExpansionNode permits ExpansionElement, ExpansionText {
	}

	/**
	 * 保留原始元素名称、属性和子节点顺序的不可变技能组元素。
	 * Immutable skill-group element preserving the original name, attributes, and child order.
	 */
	private record ExpansionElement(String name, List<ExpansionAttribute> attributes, List<ExpansionNode> children)
		implements ExpansionNode {
	}

	/**
	 * 技能组中的非空文本或 CDATA 节点。
	 * Non-blank text or CDATA node in a skill group.
	 */
	private record ExpansionText(String value) implements ExpansionNode {
	}

	/**
	 * 技能组元素的不可变属性。
	 * Immutable attribute of a skill-group element.
	 */
	private record ExpansionAttribute(String name, String value) {
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

	private static String localName(XMLStreamReader reader) {
		String localName = reader.getLocalName();
		return localName == null || localName.isEmpty() ? reader.getName().getLocalPart() : localName;
	}
}
