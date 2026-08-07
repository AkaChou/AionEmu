package com.aionemu.gameserver.questEngine.definition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Explicit production owner allow-list for definitions in the quests directory. */
public final class QuestDefinitionCatalogManifest {
	private static final String RESOURCE =
		"aion/data/static_data/quest_definition/quest_definition_catalog.xml";
	private static final String SCHEMA =
		"/aion/data/static_data/quest_definition/quest_definition_catalog.xsd";

	public record Entry(int id, String resource) {
		public Entry {
			if (id <= 0) {
				throw new IllegalArgumentException("quest id must be positive");
			}
			resource = requireResource(resource);
		}
	}

	private final int version;
	private final List<Entry> entries;

	private QuestDefinitionCatalogManifest(int version, List<Entry> entries) {
		if (version <= 0) {
			fail("INVALID_CATALOG_VERSION", "version must be positive");
		}
		if (entries.isEmpty()) {
			fail("EMPTY_PRODUCTION_CATALOG", "production catalog must list definitions");
		}
		this.version = version;
		this.entries = List.copyOf(entries);
	}

	/** Load the explicit production owner manifest from classpath resources. */
	public static QuestCatalog compile(ClassLoader loader) {
		Objects.requireNonNull(loader, "loader");
		try (InputStream input = loader.getResourceAsStream(RESOURCE)) {
			if (input == null) {
				fail("PRODUCTION_CATALOG_MISSING", RESOURCE);
			}
			return compile(input, loader);
		} catch (QuestCompilationException e) {
			throw e;
		} catch (Exception e) {
			throw new QuestCompilationException("PRODUCTION_CATALOG_READ_FAILED", e.getMessage());
		}
	}

	/** Parse and validate an explicit production owner manifest. */
	public static QuestDefinitionCatalogManifest load(InputStream input) {
		Objects.requireNonNull(input, "input");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			try (InputStream schema = QuestDefinitionCatalogManifest.class.getResourceAsStream(SCHEMA)) {
				if (schema == null) {
					fail("CATALOG_SCHEMA_MISSING", SCHEMA);
				}
				factory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
					.newSchema(new StreamSource(schema)));
			}
			var builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new DefaultHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;
				}
			});
			Document document = builder.parse(input);
			Element root = document.getDocumentElement();
			if (!"quest-definition-catalog".equals(root.getTagName())) {
				fail("INVALID_CATALOG_ROOT", root.getTagName());
			}
			List<Entry> entries = new ArrayList<>();
			Set<Integer> ids = new HashSet<>();
			Set<String> resources = new HashSet<>();
			for (Element definition : children(root, "definition")) {
				Entry entry = new Entry(integer(definition, "id"), required(definition, "resource"));
				if (!ids.add(entry.id())) {
					fail("DUPLICATE_CATALOG_OWNER", Integer.toString(entry.id()));
				}
				if (!resources.add(entry.resource())) {
					fail("DUPLICATE_CATALOG_RESOURCE", entry.resource());
				}
				entries.add(entry);
			}
			return new QuestDefinitionCatalogManifest(integer(root, "version"), entries);
		} catch (QuestCompilationException e) {
			throw e;
		} catch (Exception e) {
			throw new QuestCompilationException("INVALID_PRODUCTION_CATALOG",
				e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	/** Compile only the entries explicitly promoted to production ownership. */
	public static QuestCatalog compile(InputStream manifest, ClassLoader loader) {
		Objects.requireNonNull(loader, "loader");
		QuestDefinitionCatalogManifest parsed = load(manifest);
		List<CompiledQuestDefinition> definitions = new ArrayList<>();
		for (Entry entry : parsed.entries) {
			byte[] xml;
			try (InputStream input = loader.getResourceAsStream(entry.resource())) {
				if (input == null) {
					fail("CATALOG_RESOURCE_MISSING", entry.resource());
				}
				xml = input.readAllBytes();
			} catch (QuestCompilationException e) {
				throw e;
			} catch (Exception e) {
				fail("CATALOG_RESOURCE_READ_FAILED", entry.resource());
				return null;
			}
			CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml));
			if (definition.id() != entry.id()) {
				fail("CATALOG_ID_MISMATCH", entry.resource() + " expected=" + entry.id()
					+ " actual=" + definition.id());
			}
			definitions.add(definition);
		}
		return new ImmutableQuestCatalog(definitions);
	}

	public int version() {
		return version;
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
			fail("MISSING_CATALOG_ATTRIBUTE", element.getTagName() + "." + name);
		}
		return element.getAttribute(name);
	}

	private static int integer(Element element, String name) {
		try {
			return Integer.parseInt(required(element, name));
		} catch (NumberFormatException e) {
			fail("INVALID_CATALOG_INTEGER", element.getTagName() + "." + name);
			return 0;
		}
	}

	private static String requireResource(String value) {
		Objects.requireNonNull(value, "resource");
		if (value.isBlank() || value.startsWith("/") || value.indexOf('\\') >= 0) {
			throw new IllegalArgumentException("catalog resource must be a relative classpath path");
		}
		for (String part : value.split("/")) {
			if (part.isBlank() || ".".equals(part) || "..".equals(part)) {
				throw new IllegalArgumentException("catalog resource contains an unsafe path segment");
			}
		}
		return value;
	}

	private static void fail(String code, String message) {
		throw new QuestCompilationException(code, message);
	}
}
