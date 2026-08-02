package com.aionemu.gameserver.questEngine.definition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Explicit manifest for deterministic candidate-definition loading. */
public final class QuestDefinitionCandidateManifest {
	public record Entry(int id, String resource) {
		public Entry {
			if (id <= 0) {
				throw new IllegalArgumentException("candidate quest id must be positive");
			}
			resource = requireResource(resource);
		}
	}

	private final int version;
	private final QuestOwnership ownership;
	private final List<Entry> entries;

	private QuestDefinitionCandidateManifest(int version, QuestOwnership ownership, List<Entry> entries) {
		if (version <= 0) {
			throw new QuestCompilationException("INVALID_CANDIDATE_MANIFEST_VERSION", "version must be positive");
		}
		if (ownership == null) {
			throw new QuestCompilationException("CANDIDATE_MANIFEST_OWNERSHIP_FORBIDDEN", "null ownership");
		}
		if (entries.isEmpty()) {
			throw new QuestCompilationException("EMPTY_CANDIDATE_MANIFEST", "manifest must list definitions");
		}
		this.version = version;
		this.ownership = ownership;
		this.entries = List.copyOf(entries);
	}

	public static QuestDefinitionCandidateManifest load(InputStream input) {
		Objects.requireNonNull(input, "input");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			try (InputStream schemaStream = QuestDefinitionCandidateManifest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quest_definition_candidate_manifest.xsd")) {
				if (schemaStream == null) {
					fail("CANDIDATE_MANIFEST_SCHEMA_MISSING", "candidate manifest schema is not packaged");
				}
				factory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
					.newSchema(new StreamSource(schemaStream)));
			}
			Document document = factory.newDocumentBuilder().parse(input);
			Element root = document.getDocumentElement();
			if (!"quest-definition-candidate-manifest".equals(root.getTagName())) {
				fail("INVALID_CANDIDATE_MANIFEST_ROOT", root.getTagName());
			}
			String ownership = required(root, "ownership");
			QuestOwnership declared;
			switch (ownership) {
				case "CATALOG_ONLY", "COMPILED_CANDIDATE" -> declared = QuestOwnership.valueOf(ownership);
				default -> {
					fail("CANDIDATE_MANIFEST_OWNERSHIP_FORBIDDEN", ownership);
					throw new AssertionError("unreachable");
				}
			}
			int version = integer(root, "version");
			List<Entry> entries = new ArrayList<>();
			Set<Integer> ids = new HashSet<>();
			Set<String> resources = new HashSet<>();
			for (Element definition : children(root, "definition")) {
				int id = integer(definition, "id");
				Entry entry = new Entry(id, required(definition, "resource"));
				if (!ids.add(entry.id())) {
					fail("DUPLICATE_CANDIDATE_ID", Integer.toString(entry.id()));
				}
				if (!resources.add(entry.resource())) {
					fail("DUPLICATE_CANDIDATE_RESOURCE", entry.resource());
				}
				entries.add(entry);
			}
			return new QuestDefinitionCandidateManifest(version, declared, entries);
		} catch (QuestCompilationException e) {
			throw e;
		} catch (Exception e) {
			throw new QuestCompilationException("INVALID_CANDIDATE_MANIFEST", e.getMessage() == null
				? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	public static QuestCatalog compile(InputStream manifest, ClassLoader loader) {
		Objects.requireNonNull(loader, "loader");
		QuestDefinitionCandidateManifest parsed = load(manifest);
		List<QuestDefinitionCandidateImporter.Source> sources = new ArrayList<>();
		for (Entry entry : parsed.entries) {
			try (InputStream input = loader.getResourceAsStream(entry.resource())) {
				if (input == null) {
					fail("CANDIDATE_RESOURCE_MISSING", entry.resource());
				}
				sources.add(compileSource(parsed, entry, input.readAllBytes()));
			} catch (QuestCompilationException e) {
				throw e;
			} catch (Exception e) {
				throw new QuestCompilationException("CANDIDATE_READ_FAILED", entry.resource());
			}
		}
		return QuestDefinitionCandidateImporter.compile(sources);
	}

	/** 从文件系统根目录（每个 manifest resource 的相对基目录）加载候选定义。 */
	public static QuestCatalog compileFiles(InputStream manifest, List<Path> roots) {
		Objects.requireNonNull(roots, "roots");
		if (roots.isEmpty()) {
			throw new QuestCompilationException("CANDIDATE_FILES_NO_ROOT", "at least one candidate root is required");
		}
		QuestDefinitionCandidateManifest parsed = load(manifest);
		List<QuestDefinitionCandidateImporter.Source> sources = new ArrayList<>();
		for (Entry entry : parsed.entries) {
			byte[] xml = null;
			for (Path root : roots) {
				Path candidate = root.resolve(entry.resource()).normalize();
				if (!candidate.startsWith(root.normalize())) {
					fail("CANDIDATE_FILES_PATH_ESCAPE", entry.resource());
				}
				if (Files.isRegularFile(candidate)) {
					try {
						xml = Files.readAllBytes(candidate);
					} catch (IOException e) {
						fail("CANDIDATE_READ_FAILED", entry.resource());
					}
					break;
				}
			}
			if (xml == null) {
				fail("CANDIDATE_RESOURCE_MISSING", entry.resource());
			}
			sources.add(compileSource(parsed, entry, xml));
		}
		return QuestDefinitionCandidateImporter.compile(sources);
	}

	private static QuestDefinitionCandidateImporter.Source compileSource(QuestDefinitionCandidateManifest manifest,
			Entry entry, byte[] xml) {
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml));
		if (definition.id() != entry.id()) {
			fail("CANDIDATE_ID_MISMATCH", entry.resource() + " expected=" + entry.id()
				+ " actual=" + definition.id());
		}
		if (definition.ownership() != manifest.ownership) {
			fail("CANDIDATE_OWNERSHIP_MISMATCH", entry.resource() + " manifest=" + manifest.ownership
				+ " actual=" + definition.ownership());
		}
		return new QuestDefinitionCandidateImporter.Source(entry.resource(), xml);
	}

	public int version() {
		return version;
	}

	public QuestOwnership ownership() {
		return ownership;
	}

	public List<Entry> entries() {
		return entries;
	}

	private static List<Element> children(Element parent, String name) {
		List<Element> result = new ArrayList<>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element element && name.equals(element.getTagName())) {
				result.add(element);
			}
		}
		return result;
	}

	private static String required(Element element, String name) {
		if (!element.hasAttribute(name) || element.getAttribute(name).isBlank()) {
			fail("MISSING_CANDIDATE_MANIFEST_ATTRIBUTE", element.getTagName() + "." + name);
		}
		return element.getAttribute(name);
	}

	private static int integer(Element element, String name) {
		try {
			return Integer.parseInt(required(element, name));
		} catch (NumberFormatException e) {
			fail("INVALID_CANDIDATE_MANIFEST_INTEGER", element.getTagName() + "." + name);
			return 0;
		}
	}

	private static String requireResource(String value) {
		Objects.requireNonNull(value, "resource");
		if (value.isBlank() || value.startsWith("/") || value.indexOf('\\') >= 0) {
			throw new IllegalArgumentException("candidate resource must be a relative classpath path");
		}
		for (String part : value.split("/")) {
			if (part.isBlank() || ".".equals(part) || "..".equals(part)) {
				throw new IllegalArgumentException("candidate resource contains an unsafe path segment");
			}
		}
		return value;
	}

	private static void fail(String code, String message) {
		throw new QuestCompilationException(code, message);
	}
}
