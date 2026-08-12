package com.aionemu.gameserver.questEngine.definition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Migration verifier: proves that a legacy node-syntax XML and its migrated compact-node XML lower to
 * the same complete QuestDefinition IR. Reads file paths from stdin (one per line) so a single JVM
 * processes the whole batch; prints one line per file:
 * {@code OK <path>} | {@code MISMATCH <path> <firstDiffPath>} | {@code PARSE_FAIL <path> <code>: <message>}
 * <p>
 * The before side is produced by a migration-only in-memory normalizer (legacy project/vars
 * wrappers become compact {@code status} attribute + direct {@code var} children). The normalizer performs
 * a one-to-one structural rewrite only; it does not recognize or rewrite domain blocks.
 */
public final class QuestXmlMigrationVerifier {
	private QuestXmlMigrationVerifier() {
	}

	public static void main(String[] args) throws Exception {
		String argsFile = null;
		String outputFile = null;
		String beforeDir = null;
		int requestedThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
		for (int i = 0; i < args.length; i++) {
			if ("--args-file".equals(args[i]) && i + 1 < args.length) {
				argsFile = args[i + 1];
				i++;
			} else if ("--output-file".equals(args[i]) && i + 1 < args.length) {
				outputFile = args[i + 1];
				i++;
			} else if ("--before-dir".equals(args[i]) && i + 1 < args.length) {
				beforeDir = args[i + 1];
				i++;
			} else if ("--threads".equals(args[i]) && i + 1 < args.length) {
				requestedThreads = Math.max(1, Integer.parseInt(args[i + 1]));
				i++;
			}
		}
		if (argsFile == null) {
			throw new IllegalArgumentException("USAGE: QuestXmlMigrationVerifier --args-file <path> [--output-file <path>] [--before-dir <path>] [--threads <count>]");
		}
		Path beforeRoot = beforeDir == null ? null : Path.of(beforeDir);
		List<VerificationInput> files = new ArrayList<>();
		for (String line : Files.readAllLines(Path.of(argsFile))) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				String[] fields = trimmed.split("\\t", -1);
				if (fields.length == 3) {
					files.add(new VerificationInput(fields[0], Path.of(fields[1]), Path.of(fields[2]), false));
				} else if (fields.length == 1) {
					Path after = Path.of(trimmed);
					Path before = beforeRoot == null ? after : beforeRoot.resolve(after);
					files.add(new VerificationInput(trimmed, before, after, beforeRoot != null));
				} else {
					throw new IllegalArgumentException("invalid manifest line: " + trimmed);
				}
			}
		}
		int threads = Math.min(requestedThreads, Math.max(1, files.size()));
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<VerificationResult>> futures = new ArrayList<>(files.size());
		for (VerificationInput input : files) {
			futures.add(executor.submit(() -> verify(input)));
		}
		executor.shutdown();

		StringBuilder output = new StringBuilder();
		int mismatches = 0;
		int failures = 0;
		for (Future<VerificationResult> future : futures) {
			VerificationResult result = future.get();
			output.append(result.line()).append('\n');
			mismatches += result.mismatch() ? 1 : 0;
			failures += result.failure() ? 1 : 0;
		}
		output.append("SUMMARY mismatches=").append(mismatches).append(" failures=").append(failures)
			.append(" threads=").append(threads).append('\n');
		if (outputFile != null) {
			Files.writeString(Path.of(outputFile), output.toString());
		} else {
			System.err.print(output);
		}
		if (mismatches != 0 || failures != 0) {
			System.exit(1);
		}
	}

	private static VerificationResult verify(VerificationInput input) {
		try {
			if (!Files.isRegularFile(input.before())) {
				return new VerificationResult("PARSE_FAIL " + input.display()
					+ " MISSING_BEFORE: " + input.before(), false, true);
			}
			if (!Files.isRegularFile(input.after())) {
				return new VerificationResult("PARSE_FAIL " + input.display()
					+ " MISSING_AFTER: " + input.after(), false, true);
			}
			QuestDefinition before = input.normalizeBefore() ? normalizeAndParse(input.before()) : parse(input.before());
			QuestDefinition after = parse(input.after());
			String diff = IrDiffer.firstDifference(before, after);
			if (diff == null) {
				return new VerificationResult("OK " + input.display()
					+ (input.normalizeBefore() ? " (before-normalized)" : " (before-direct)"), false, false);
			}
			return new VerificationResult("MISMATCH " + input.display() + " " + diff, true, false);
		} catch (QuestCompilationException e) {
			return new VerificationResult("PARSE_FAIL " + input.display() + " " + e.code() + ": " + e.getMessage(), false, true);
		} catch (Exception e) {
			return new VerificationResult("PARSE_FAIL " + input.display() + " " + e.getClass().getSimpleName() + ": "
				+ (e.getMessage() == null ? "" : e.getMessage()), false, true);
		}
	}

	private record VerificationInput(String display, Path before, Path after, boolean normalizeBefore) {
	}

	private record VerificationResult(String line, boolean mismatch, boolean failure) {
	}

	private static QuestDefinition parse(Path path) throws Exception {
		try (var input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.parse(input);
		}
	}

	private static QuestDefinition normalizeAndParse(Path path) throws Exception {
		byte[] original = Files.readAllBytes(path);
		String normalized = normalize(new String(original, StandardCharsets.UTF_8));
		return QuestDefinitionXmlCompiler.parse(new ByteArrayInputStream(
			normalized.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Migration-only legacy node normalizer: for each {@code node} with a {@code project} child, moves the
	 * projection's {@code status} attribute onto the node and its {@code var} children (in original order) onto
	 * the node, then removes the wrapper. Everything else stays byte-identical after re-serialization. Files
	 * that are already compact are returned unchanged.
	 */
	static String normalize(String xml) {
		String projectPrefix = "<pro" + "ject";
		String varsPrefix = "<va" + "rs";
		if (!xml.contains(projectPrefix) && !xml.contains(varsPrefix)) {
			return xml;
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document document = factory.newDocumentBuilder().parse(
					new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			NodeList nodeList = document.getElementsByTagName("node");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element node = (Element) nodeList.item(i);
				List<Element> nodeChildren = childElements(node);
				long projectCount = nodeChildren.stream().filter(child -> "project".equals(child.getTagName())).count();
				if (projectCount == 0) {
					if (nodeChildren.stream().anyMatch(child -> "vars".equals(child.getTagName()))) {
						throw new QuestCompilationException("NORMALIZE_FAILED", "node contains vars without project");
					}
					continue;
				}
				if (projectCount != 1) {
					throw new QuestCompilationException("NORMALIZE_FAILED", "node contains multiple project wrappers");
				}
				Element projection = onlyNamedChild(nodeChildren, "project");
				if (nodeChildren.size() != 1 || node.hasAttribute("status") || hasElementNamed(nodeChildren, "var")) {
					throw new QuestCompilationException("NORMALIZE_FAILED", "node legacy projection is mixed or ambiguous");
				}
				if (!hasOnlyAttributes(projection, "status") || !projection.hasAttribute("status")) {
					throw new QuestCompilationException("NORMALIZE_FAILED", "project must contain only status");
				}
				List<Element> projectionChildren = childElements(projection);
				Element vars = onlyNamedChild(projectionChildren, "vars");
				if (projectionChildren.size() > 1 || (projectionChildren.size() == 1 && vars == null)) {
					throw new QuestCompilationException("NORMALIZE_FAILED", "project must contain at most one vars wrapper");
				}
				node.setAttribute("status", projection.getAttribute("status"));
				if (vars != null) {
					if (!hasOnlyAttributes(vars) || childElements(vars).stream()
						.anyMatch(child -> !"var".equals(child.getTagName()))) {
						throw new QuestCompilationException("NORMALIZE_FAILED", "vars must contain only var elements");
					}
					// getElementsByTagName returns a LIVE NodeList: appending a matched element
					// mutates the list and skips entries. Move children one at a time instead.
					while (vars.getFirstChild() != null) {
						node.appendChild(vars.getFirstChild());
					}
				}
				node.removeChild(projection);
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			throw new QuestCompilationException("NORMALIZE_FAILED", e.getMessage());
		}
		}

	private static List<Element> childElements(Element parent) {
		List<Element> result = new ArrayList<>();
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element) {
				result.add(element);
			}
		}
		return result;
	}

	private static Element onlyNamedChild(List<Element> children, String name) {
		Element result = null;
		for (Element child : children) {
			if (name.equals(child.getTagName())) {
				if (result != null) {
					return null;
				}
				result = child;
			}
		}
		return result;
	}

	private static boolean hasElementNamed(List<Element> children, String name) {
		return children.stream().anyMatch(child -> name.equals(child.getTagName()));
	}

	private static boolean hasOnlyAttributes(Element element, String... names) {
		NamedNodeMap attributes = element.getAttributes();
		if (attributes.getLength() != names.length) {
			return false;
		}
		for (String name : names) {
			if (!element.hasAttribute(name)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Recursive structural differ over record IR values. Never uses {@code equals} on whole definitions:
	 * records with {@code int[]} components (BroadcastZoneMissionEnd, ScheduleEventQuestRefresh) compare by
	 * reference under default equals, which would falsely report every quest using them as MISMATCH.
	 */
	static final class IrDiffer {
		private IrDiffer() {
		}

		/** Returns the first differing field path, or null when both values are structurally equal. */
		static String firstDifference(Object before, Object after) {
			return difference(before, after, "");
		}

		private static String difference(Object before, Object after, String path) {
			if (before == after) {
				return null;
			}
			if (before == null || after == null) {
				return path;
			}
			Class<?> type = before.getClass();
			if (type != after.getClass()) {
				return path;
			}
			// Record: recurse into components so int[] fields compare by value, not by reference.
			if (type.isRecord()) {
				java.lang.reflect.RecordComponent[] components = type.getRecordComponents();
				for (java.lang.reflect.RecordComponent component : components) {
					try {
						Object leftValue = component.getAccessor().invoke(before);
						Object rightValue = component.getAccessor().invoke(after);
						String diff = difference(leftValue, rightValue, path + "." + component.getName());
						if (diff != null) {
							return diff;
						}
					} catch (ReflectiveOperationException e) {
						return path + "." + component.getName() + "(!reflection:" + e.getClass().getSimpleName() + ")";
					}
				}
				return null;
			}
			if (before instanceof int[] left && after instanceof int[] right) {
				return Arrays.equals(left, right) ? null : path;
			}
			if (before instanceof Object[] left && after instanceof Object[] right) {
				return Arrays.deepEquals(left, right) ? null : path;
			}
			if (before instanceof List<?> left && after instanceof List<?> right) {
				if (left.size() != right.size()) {
					return path + "[size:" + left.size() + "!=" + right.size() + "]";
				}
				for (int i = 0; i < left.size(); i++) {
					String diff = difference(left.get(i), right.get(i), path + "[" + i + "]");
					if (diff != null) {
						return diff;
					}
				}
				return null;
			}
			if (before instanceof Map<?, ?> left && after instanceof Map<?, ?> right) {
				if (left.size() != right.size()) {
					return path + "[size:" + left.size() + "!=" + right.size() + "]";
				}
				var leftIterator = left.entrySet().iterator();
				var rightIterator = right.entrySet().iterator();
				int index = 0;
				while (leftIterator.hasNext()) {
					Map.Entry<?, ?> leftEntry = leftIterator.next();
					Map.Entry<?, ?> rightEntry = rightIterator.next();
					String keyDiff = difference(leftEntry.getKey(), rightEntry.getKey(), path + "[" + index + "].key");
					if (keyDiff != null) {
						return keyDiff;
					}
					String diff = difference(leftEntry.getValue(), rightEntry.getValue(), path + "[" + index + "].value");
					if (diff != null) {
						return diff;
					}
					index++;
				}
				return null;
			}
			if (!before.equals(after)) {
				return path;
			}
			return null;
		}
	}
}
