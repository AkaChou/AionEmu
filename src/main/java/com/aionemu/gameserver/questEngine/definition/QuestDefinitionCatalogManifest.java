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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 正式 typed 任务定义的确定性 classpath catalog。
 * Deterministic classpath catalog for typed definitions that own live quest execution.
 */
public final class QuestDefinitionCatalogManifest {
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

	/**
	 * 解析并校验正式 catalog。
	 * Parse and validate the production catalog.
	 */
	public static QuestDefinitionCatalogManifest load(InputStream input) {
		Objects.requireNonNull(input, "input");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			try (InputStream schemaStream = QuestDefinitionCatalogManifest.class.getResourceAsStream(SCHEMA)) {
				if (schemaStream == null) {
					fail("CATALOG_SCHEMA_MISSING", "production catalog schema is not packaged");
				}
				factory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
					.newSchema(new StreamSource(schemaStream)));
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

	/**
	 * 加载 catalog 中列出的 XML 并编译为统一 IR。
	 * Load the listed XML definitions and compile them into the unified IR.
	 */
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

	/**
	 * 直接从 quest_definition/quests 目录扫描全部 XML 编译为 catalog（无需手写条目清单）。
	 * Compile every quests/*.xml in the packaged directory into the catalog, so the
	 * manifest file no longer needs to list each definition.
	 */
	public static QuestCatalog compileFromQuestsDirectory(ClassLoader loader) {
		Objects.requireNonNull(loader, "loader");
		String dir = "aion/data/static_data/quest_definition/quests";
		URL url = loader.getResource(dir);
		if (url == null) {
			fail("QUEST_DIR_MISSING", dir);
		}
		List<Path> files = new ArrayList<>();
		if ("file".equals(url.getProtocol())) {
			try {
				File directory = new File(url.toURI());
				File[] xml = directory.listFiles(f -> f.getName().endsWith(".xml"));
				if (xml == null) {
					fail("QUEST_DIR_UNREADABLE", dir);
				}
				for (File f : xml) {
					files.add(Path.of(dir, f.getName()));
				}
			} catch (URISyntaxException e) {
				fail("QUEST_DIR_BAD_URI", url.toString());
			}
		} else if ("jar".equals(url.getProtocol())) {
			try {
				JarURLConnection connection = (JarURLConnection) url.openConnection();
				try (JarFile jar = connection.getJarFile()) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						if (!entry.isDirectory() && entry.getName().startsWith(dir + "/")
							&& entry.getName().endsWith(".xml")) {
							files.add(Path.of(entry.getName()));
						}
					}
				}
			} catch (IOException e) {
				fail("QUEST_DIR_READ_FAILED", dir);
			}
		} else {
			fail("QUEST_DIR_UNSUPPORTED_PROTOCOL", url.getProtocol());
		}
		files.sort(Comparator.comparingInt(p -> Integer.parseInt(p.getFileName().toString().replace(".xml", ""))));
		List<CompiledQuestDefinition> definitions = new ArrayList<>();
		for (Path path : files) {
			String resource = path.toString().replace('\\', '/');
			try (InputStream input = loader.getResourceAsStream(resource)) {
				if (input == null) {
					fail("QUEST_RESOURCE_MISSING", resource);
				}
				definitions.add(QuestDefinitionXmlCompiler.compile(input));
			} catch (IOException e) {
				fail("QUEST_RESOURCE_READ_FAILED", resource);
			}
		}
		return new ImmutableQuestCatalog(definitions);
	}

	/** 返回 catalog 版本。 Return the catalog version. */
	public int version() {
		return version;
	}

	/** 返回正式 owner 条目。 Return the production owner entries. */
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
