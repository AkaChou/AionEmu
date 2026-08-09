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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Explicit production owner allow-list for definitions in the quests directory. */
public final class QuestDefinitionCatalogManifest {
	private static final String RESOURCE =
		"aion/data/static_data/quest_definition/quest_definition_catalog.xml";
	private static final String SCHEMA =
		"/aion/data/static_data/quest_definition/quest_definition_catalog.xsd";
	private static final String EXTERNAL_RESOURCE_PREFIX =
		"aion/data/static_data/quest_definition/";
	private static final String CATALOG_FILE = "quest_definition_catalog.xml";
	private static final String CATALOG_SCHEMA_FILE = "quest_definition_catalog.xsd";
	private static final String DEFINITION_SCHEMA_FILE = "quest_definition.xsd";

	public record Entry(int id, String resource, QuestCatalogEntryMode mode) {
		public Entry {
			if (id <= 0) {
				throw new IllegalArgumentException("quest id must be positive");
			}
			resource = requireResource(resource);
			mode = Objects.requireNonNull(mode, "mode");
		}
	}

	private final int version;
	private final List<Entry> entries;

	private QuestDefinitionCatalogManifest(int version, List<Entry> entries) {
		if (version != 2) {
			fail("INVALID_CATALOG_VERSION", "catalog schema version must be 2");
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

	/** Load the production catalog and quest definitions from the external game-data directory. */
	public static QuestCatalog compile(Path questDefinitionDirectory) {
		Objects.requireNonNull(questDefinitionDirectory, "questDefinitionDirectory");
		Path directory = questDefinitionDirectory.toAbsolutePath().normalize();
		Path catalogFile = requireFile(directory.resolve(CATALOG_FILE), "PRODUCTION_CATALOG_MISSING");
		Path catalogSchemaFile = requireFile(directory.resolve(CATALOG_SCHEMA_FILE), "CATALOG_SCHEMA_MISSING");
		Path definitionSchemaFile = requireFile(directory.resolve(DEFINITION_SCHEMA_FILE), "SCHEMA_MISSING");
		try (InputStream catalog = Files.newInputStream(catalogFile);
			 InputStream catalogSchemaInput = Files.newInputStream(catalogSchemaFile);
			 InputStream definitionSchemaInput = Files.newInputStream(definitionSchemaFile)) {
			Schema catalogSchema = loadCatalogSchema(catalogSchemaInput);
			Schema definitionSchema = QuestDefinitionXmlCompiler.loadSchema(definitionSchemaInput);
			QuestDefinitionCatalogManifest parsed = load(catalog, catalogSchema);
			return compile(parsed, resource -> openExternalResource(directory, resource), definitionSchema);
		} catch (QuestCompilationException e) {
			throw e;
		} catch (IOException e) {
			throw new QuestCompilationException("PRODUCTION_CATALOG_READ_FAILED",
				e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	/** Parse and validate an explicit production owner manifest. */
	public static QuestDefinitionCatalogManifest load(InputStream input) {
		Objects.requireNonNull(input, "input");
		try (InputStream schema = QuestDefinitionCatalogManifest.class.getResourceAsStream(SCHEMA)) {
			if (schema == null) {
				fail("CATALOG_SCHEMA_MISSING", SCHEMA);
			}
			return load(input, loadCatalogSchema(schema));
		} catch (QuestCompilationException e) {
			throw e;
		} catch (Exception e) {
			throw new QuestCompilationException("INVALID_PRODUCTION_CATALOG",
				e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	private static QuestDefinitionCatalogManifest load(InputStream input, Schema schema) {
		Objects.requireNonNull(input, "input");
		Objects.requireNonNull(schema, "schema");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			factory.setSchema(schema);
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
				Entry entry = new Entry(integer(definition, "id"), required(definition, "resource"),
					entryMode(definition));
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
		return compile(parsed, loader::getResourceAsStream, null);
	}

	private static QuestCatalog compile(QuestDefinitionCatalogManifest parsed, ResourceOpener resourceOpener,
			Schema definitionSchema) {
		List<QuestCatalogEntry> definitions = compileEntries(parsed.entries, resourceOpener, definitionSchema);
		for (int i = 0; i < parsed.entries.size(); i++) {
			Entry entry = parsed.entries.get(i);
			if (definitions.get(i).id() != entry.id()) {
				fail("CATALOG_ID_MISMATCH", entry.resource() + " expected=" + entry.id()
					+ " actual=" + definitions.get(i).id());
			}
		}
		return ImmutableQuestCatalog.fromEntries(definitions);
	}

	private static List<QuestCatalogEntry> compileEntries(List<Entry> entries, ResourceOpener resourceOpener,
			Schema definitionSchema) {
		int processors = java.lang.management.ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(Math.min(processors, 8));
		try {
			List<Future<QuestCatalogEntry>> futures = new ArrayList<>(entries.size());
			for (Entry entry : entries) {
				futures.add(pool.submit(() -> {
					try (InputStream input = resourceOpener.open(entry.resource())) {
						if (input == null) {
							throw new QuestCompilationException("CATALOG_RESOURCE_MISSING", entry.resource());
						}
						if (definitionSchema == null) {
							return switch (entry.mode()) {
								case EXECUTABLE -> QuestCatalogEntry.executable(QuestDefinitionXmlCompiler.compile(input));
								case METADATA_ONLY -> QuestCatalogEntry.metadataOnly(QuestDefinitionXmlCompiler.parse(input));
							};
						}
						return switch (entry.mode()) {
							case EXECUTABLE -> QuestCatalogEntry.executable(
								QuestDefinitionXmlCompiler.compile(input, definitionSchema));
							case METADATA_ONLY -> QuestCatalogEntry.metadataOnly(
								QuestDefinitionXmlCompiler.parse(input, definitionSchema));
						};
					} catch (IOException e) {
						throw new QuestCompilationException("CATALOG_RESOURCE_READ_FAILED", entry.resource());
					}
				}));
			}
			List<QuestCatalogEntry> compiled = new ArrayList<>(entries.size());
			for (Future<QuestCatalogEntry> future : futures) {
				try {
					compiled.add(future.get());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new QuestCompilationException("COMPILE_INTERRUPTED", e.getMessage());
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (cause instanceof QuestCompilationException qce) {
						throw qce;
					}
					throw new QuestCompilationException("COMPILE_FAILED",
						cause == null ? null : cause.getMessage());
				}
			}
			return compiled;
		} finally {
			pool.shutdown();
		}
	}

	private static Schema loadCatalogSchema(InputStream input) {
		try {
			return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(new StreamSource(input));
		} catch (SAXException e) {
			throw new QuestCompilationException("CATALOG_SCHEMA_INVALID",
				e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	private static Path requireFile(Path file, String code) {
		if (!Files.isRegularFile(file)) {
			fail(code, file.toString());
		}
		return file;
	}

	private static InputStream openExternalResource(Path directory, String resource) throws IOException {
		if (!resource.startsWith(EXTERNAL_RESOURCE_PREFIX)) {
			fail("CATALOG_RESOURCE_OUTSIDE_DIRECTORY", resource);
		}
		Path file = directory.resolve(resource.substring(EXTERNAL_RESOURCE_PREFIX.length())).normalize();
		if (!file.startsWith(directory)) {
			fail("CATALOG_RESOURCE_OUTSIDE_DIRECTORY", resource);
		}
		return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
	}

	@FunctionalInterface
	private interface ResourceOpener {
		InputStream open(String resource) throws IOException;
	}

	/**
	 * 并行编译任务 XML 资源，按输入顺序返回结果。Compiles quest XML resources in parallel, preserving input order.
	 * 编译管线无共享可变状态，Schema 静态化后线程安全。The compile pipeline has no shared mutable state,
	 * and the shared Schema is thread-safe, so per-resource work can run concurrently.
	 */
	static List<QuestCatalogEntry> compileResourceEntries(List<String> resources, ClassLoader loader,
			String missingCode, String readFailedCode) {
		int processors = java.lang.management.ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(Math.min(processors, 8));
		try {
			List<Future<QuestCatalogEntry>> futures = new ArrayList<>(resources.size());
			for (String resource : resources) {
				futures.add(pool.submit(() -> {
					try (InputStream input = loader.getResourceAsStream(resource)) {
						if (input == null) {
							throw new QuestCompilationException(missingCode, resource);
						}
						QuestDefinition definition = QuestDefinitionXmlCompiler.parse(input);
						if (definition.nodes().isEmpty() && definition.transitions().isEmpty()) {
							return QuestCatalogEntry.metadataOnly(definition);
						}
						return QuestCatalogEntry.executable(QuestDefinitionCompiler.compile(definition));
					} catch (IOException e) {
						throw new QuestCompilationException(readFailedCode, resource);
					}
				}));
			}
			List<QuestCatalogEntry> definitions = new ArrayList<>(resources.size());
			for (Future<QuestCatalogEntry> future : futures) {
				try {
					definitions.add(future.get());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new QuestCompilationException("COMPILE_INTERRUPTED", e.getMessage());
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (cause instanceof QuestCompilationException qce) {
						throw qce;
					}
					throw new QuestCompilationException("COMPILE_FAILED",
						cause == null ? null : cause.getMessage());
				}
			}
			return definitions;
		} finally {
			pool.shutdown();
		}
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

	private static QuestCatalogEntryMode entryMode(Element element) {
		try {
			return QuestCatalogEntryMode.valueOf(required(element, "mode"));
		} catch (IllegalArgumentException e) {
			fail("INVALID_CATALOG_MODE", element.getAttribute("mode"));
			return null;
		}
	}

	private static String requireResource(String value) {
		Objects.requireNonNull(value, "resource");
		if (value.isBlank() || value.startsWith("/") || value.indexOf('\\') >= 0) {
			throw new IllegalArgumentException("catalog resource must be a relative path");
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
