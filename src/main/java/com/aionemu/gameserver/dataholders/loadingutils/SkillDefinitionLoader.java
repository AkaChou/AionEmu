package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
		try {
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
			// 各 part 互不依赖，groups/fieldNames 只读共享；并行解析后在调用线程按 part
			// 顺序拼接，保证模板列表确定性。Unmarshaller 非线程安全，每个任务各自创建。
			// Parts are independent and groups/fieldNames are read-only shared; parse in parallel and
			// concatenate in part order on the calling thread for deterministic output. Unmarshaller is
			// not thread-safe, so each task creates its own.
			// 各 part 互不依赖，groups/fieldNames 只读共享；并行解析后在调用线程按 part
			// 顺序拼接，保证模板列表确定性。Unmarshaller 非线程安全，每个任务经共享
			// JAXBContext 自行创建。
			// Parts are independent and groups/fieldNames are read-only shared; parse in parallel and
			// concatenate in part order on the calling thread for deterministic output. Unmarshaller is
			// not thread-safe, so each task creates its own from the shared JAXBContext.
			JAXBContext jaxbContext = XmlDataLoader.createJaxbContext(SkillData.class);
			List<CompletableFuture<List<SkillTemplate>>> futures = new ArrayList<>(partFiles.size());
			for (File partFile : partFiles) {
				futures.add(CompletableFuture.supplyAsync(() -> loadPart(partFile, groups, fieldNames, jaxbContext),
					XmlDataLoader.staticDataExecutor()));
			}

			List<SkillTemplate> templates = new ArrayList<>(expectedTemplates);
			for (CompletableFuture<List<SkillTemplate>> future : futures) {
				templates.addAll(future.join());
			}
			if (templates.size() != expectedTemplates) {
				throw new IllegalStateException("Expected " + expectedTemplates + " skill templates, loaded " + templates.size());
			}
			SkillData data = new SkillData();
			data.setSkillTemplates(templates);
			if (bundle.hasAttribute("exclusive_attributes")) {
				Document exclusiveDocument = parse(new File(directory, requiredAttribute(bundle, "exclusive_attributes")));
				data.setExclusiveAttributes(readExclusiveDefinitions(exclusiveDocument), readExclusiveItems(exclusiveDocument));
			}
			data.initializeCooldownGroups();
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load compact skill definitions from " + directory.getPath(), e);
		}
	}

	/**
	 * 解析单个技能分卷：DOM 解析、展开组与字段引用、JAXB 反序列化。
	 * Parses one skill part: DOM parse, group/field expansion, then JAXB unmarshalling.
	 */
	private static List<SkillTemplate> loadPart(File partFile, Map<String, Element> groups, Map<String, String> fieldNames,
			JAXBContext jaxbContext) {
		try {
			Document document = parse(partFile);
			expand(document, groups, fieldNames);
			SkillData data = (SkillData) jaxbContext.createUnmarshaller().unmarshal(document);
			return data.getSkillTemplates();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load skill part " + partFile.getPath(), e);
		}
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

	private static void expand(Document document, Map<String, Element> groups, Map<String, String> fieldNames) {
		for (Element template : childElements(document.getDocumentElement(), "skill_template")) {
			for (Node node = template.getFirstChild(); node != null;) {
				Node next = node.getNextSibling();
				if (node instanceof Element element && element.getTagName().equals("group_ref")) {
					String id = requiredAttribute(element, "id");
					Element group = groups.get(id);
					if (group == null) {
						throw new IllegalStateException("Unknown skill group: " + id);
					}
					template.replaceChild(document.importNode(group, true), element);
				} else if (node instanceof Element element && element.getTagName().equals("retail")) {
					template.replaceChild(expandRetail(document, element, fieldNames), element);
				}
				node = next;
			}
		}
	}

	private static Element expandRetail(Document document, Element compact, Map<String, String> fieldNames) {
		Element retail = document.createElement("retail_fields");
		for (Element compactField : childElements(compact, "f")) {
			String id = requiredAttribute(compactField, "i");
			String name = fieldNames.get(id);
			if (name == null) {
				throw new IllegalStateException("Unknown retail skill field: " + id);
			}
			Element field = document.createElement("field");
			field.setAttribute("name", name);
			if (compactField.hasAttribute("o")) {
				field.setAttribute("occurrence", compactField.getAttribute("o"));
			}
			field.setAttribute("value", compactField.getAttribute("v"));
			retail.appendChild(field);
		}
		return retail;
	}

	private static Document parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory.newDocumentBuilder().parse(file);
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
