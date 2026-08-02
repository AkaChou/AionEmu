package com.aionemu.gameserver.questEngine.definition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Explicit, repository-local input manifest for candidate definition generation. */
public final class QuestDefinitionManifest {
	public record Input(String id, String family, String kind, String path) {
		public Input {
			id = requireText(id, "id");
			family = requireText(family, "family");
			kind = requireText(kind, "kind");
			path = repositoryPath(path);
		}
	}

	private final int version;
	private final List<Input> inputs;

	private QuestDefinitionManifest(int version, List<Input> inputs) {
		if (version <= 0) {
			throw new QuestCompilationException("INVALID_MANIFEST_VERSION", "manifest version must be positive");
		}
		this.version = version;
		this.inputs = List.copyOf(inputs);
	}

	public static QuestDefinitionManifest load(InputStream input) {
		Objects.requireNonNull(input, "input");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(input);
			Element root = document.getDocumentElement();
			if (!"quest-definition-manifest".equals(root.getTagName())) {
				fail("INVALID_MANIFEST_ROOT", root.getTagName());
			}
			if (!root.hasAttribute("production-owner-switch")) {
				fail("MISSING_MANIFEST_ATTRIBUTE", "quest-definition-manifest.production-owner-switch");
			}
			String ownerSwitch = root.getAttribute("production-owner-switch");
			if ("1".equals(ownerSwitch) || "true".equalsIgnoreCase(ownerSwitch)) {
				fail("PRODUCTION_OWNER_SWITCH_FORBIDDEN", "manifest cannot switch production owners");
			}
			if (!"0".equals(ownerSwitch) && !"false".equalsIgnoreCase(ownerSwitch)) {
				fail("INVALID_MANIFEST_BOOLEAN", "production-owner-switch");
			}
			int version = integer(root, "version");
			List<Input> inputs = new ArrayList<>();
			Set<String> ids = new HashSet<>();
			for (Element element : children(root, "input")) {
				Input parsed = new Input(attribute(element, "id"), attribute(element, "family"),
					attribute(element, "kind"), attribute(element, "path"));
				if (!ids.add(parsed.id())) {
					fail("DUPLICATE_MANIFEST_INPUT", parsed.id());
				}
				inputs.add(parsed);
			}
			if (inputs.isEmpty()) {
				fail("EMPTY_MANIFEST", "manifest must list at least one input");
			}
			return new QuestDefinitionManifest(version, inputs);
		} catch (QuestCompilationException e) {
			throw e;
		} catch (Exception e) {
			throw new QuestCompilationException("INVALID_MANIFEST", e.getMessage() == null
				? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	public int version() {
		return version;
	}

	public List<Input> inputs() {
		return inputs;
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

	private static String attribute(Element element, String name) {
		if (!element.hasAttribute(name) || element.getAttribute(name).isBlank()) {
			fail("MISSING_MANIFEST_ATTRIBUTE", element.getTagName() + "." + name);
		}
		return element.getAttribute(name);
	}

	private static int integer(Element element, String name) {
		try {
			return Integer.parseInt(attribute(element, name));
		} catch (NumberFormatException e) {
			fail("INVALID_MANIFEST_INTEGER", element.getTagName() + "." + name);
			return 0;
		}
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	private static String repositoryPath(String value) {
		value = requireText(value, "path");
		if (!value.startsWith("repo:")) {
			fail("EXTERNAL_MANIFEST_PATH_FORBIDDEN", value);
		}
		String relative = value.substring("repo:".length());
		if (relative.isBlank() || relative.indexOf('\\') >= 0) {
			fail("UNSAFE_MANIFEST_PATH", value);
		}
		try {
			Path path = Path.of(relative);
			Path normalized = path.normalize();
			if (path.isAbsolute() || normalized.startsWith("..") || !path.equals(normalized)) {
				fail("UNSAFE_MANIFEST_PATH", value);
			}
		} catch (InvalidPathException e) {
			fail("UNSAFE_MANIFEST_PATH", value);
		}
		return value;
	}

	private static void fail(String code, String message) {
		throw new QuestCompilationException(code, message);
	}
}
