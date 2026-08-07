import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts the retained quest-definition XML source into checked-in Java DSL source.
 *
 * <p>The XML compiler is used only while generating source. The generated classes
 * construct the same typed records and lower through {@link QuestDsl} at runtime.</p>
 */
public final class QuestDslSourceGenerator {
	private static final String GENERATED_PACKAGE =
		"com.aionemu.gameserver.questEngine.definition.generated";
	private static final int MAX_HELPER_ITEMS = 128;

	private QuestDslSourceGenerator() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("usage: QuestDslSourceGenerator <xml-dir> <java-dir>");
		}
		Path xmlDirectory = Path.of(args[0]);
		Path javaDirectory = Path.of(args[1]);
		List<Definition> definitions = readDefinitions(xmlDirectory);
		Files.createDirectories(javaDirectory);
		for (Definition definition : definitions) {
			Path target = javaDirectory.resolve("Quest" + definition.id() + ".java");
			Files.writeString(target, renderDefinition(definition.compiled()), StandardCharsets.UTF_8);
		}
		Files.writeString(javaDirectory.resolve("GeneratedQuestDslCatalog.java"),
			renderCatalog(definitions), StandardCharsets.UTF_8);
	}

	private static List<Definition> readDefinitions(Path xmlDirectory) throws IOException {
		try (var paths = Files.list(xmlDirectory)) {
			List<Path> xmlFiles = paths.filter(path -> path.getFileName().toString().endsWith(".xml"))
				.sorted(Comparator.comparingInt(QuestDslSourceGenerator::filenameId))
				.toList();
			List<Definition> definitions = new ArrayList<>(xmlFiles.size());
			for (Path xmlFile : xmlFiles) {
				try (InputStream input = Files.newInputStream(xmlFile)) {
					CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(input);
					int filenameId = filenameId(xmlFile);
					if (compiled.id() != filenameId) {
						throw new IllegalStateException(xmlFile + " has id " + compiled.id()
							+ " but filename id is " + filenameId);
					}
					definitions.add(new Definition(filenameId, compiled));
				}
			}
			return List.copyOf(definitions);
		}
	}

	private static int filenameId(Path path) {
		String filename = path.getFileName().toString();
		return Integer.parseInt(filename.substring(0, filename.length() - ".xml".length()));
	}

	private static String renderDefinition(CompiledQuestDefinition compiled) {
		QuestDefinition definition = compiled.definition();
		StringBuilder source = new StringBuilder(32_768);
		source.append("package ").append(GENERATED_PACKAGE).append(";\n\n")
			.append("import com.aionemu.gameserver.questEngine.definition.*;\n\n")
			.append("import com.aionemu.gameserver.model.*;\n")
			.append("import com.aionemu.gameserver.questEngine.model.*;\n")
			.append("import java.util.*;\n\n")
			.append("/** Generated from quest definition XML; edit the XML and regenerate. */\n")
			.append("public final class Quest").append(definition.id()).append(" {\n")
			.append("\tprivate Quest").append(definition.id()).append("() {}\n\n")
			.append("\tpublic static CompiledQuestDefinition definition() {\n")
			.append("\t\tQuestDsl.QuestBuilder builder = QuestDsl.quest(")
			.append(definition.id()).append(")\n")
			.append("\t\t\t.version(").append(definition.version()).append(")\n")
			.append("\t\t\t.metadata(metadata());\n");
		source.append("\t\tconfigureNodes(builder);\n");
		source.append("\t\taddTransitions(builder);\n");
		source.append("\n\t\treturn builder.compile();\n\t}\n\n")
			.append("\tprivate static QuestMetadata metadata() {\n")
			.append("\t\treturn ").append(expression(definition.metadata(), null)).append(";\n")
			.append("\t}\n\n")
			.append("\tprivate static void configureNodes(QuestDsl.QuestBuilder builder) {\n");
		if (!definition.progressLayout().fields().isEmpty()) {
			source.append("\t\tconfigureProgress(builder);\n");
		}
		for (int start = 0, batch = 0; start < definition.nodes().size(); start += MAX_HELPER_ITEMS, batch++) {
			source.append("\t\tconfigureNodeBatch").append(batch).append("(builder);\n");
		}
		source.append("\t}\n");
		if (!definition.progressLayout().fields().isEmpty()) {
			source.append("\n\tprivate static void configureProgress(QuestDsl.QuestBuilder builder) {\n")
				.append("\t\tbuilder.progress(");
			appendExpressions(source, definition.progressLayout().fields(), "BitField.class");
			source.append(");\n\t}\n");
		}
		for (int start = 0, batch = 0; start < definition.nodes().size(); start += MAX_HELPER_ITEMS, batch++) {
			int end = Math.min(start + MAX_HELPER_ITEMS, definition.nodes().size());
			source.append("\n\tprivate static void configureNodeBatch").append(batch)
				.append("(QuestDsl.QuestBuilder builder) {\n");
			for (int index = start; index < end; index++) {
				var node = definition.nodes().get(index);
				source.append("\t\tbuilder.node(").append(stringLiteral(node.label())).append(", ")
					.append(expression(node.projection(), null)).append(");\n");
			}
			source.append("\t}\n");
		}
		source.append("\n\tprivate static void addTransitions(QuestDsl.QuestBuilder builder) {\n");
		for (int start = 0, batch = 0; start < definition.transitions().size(); start += MAX_HELPER_ITEMS, batch++) {
			source.append("\t\taddTransitionBatch").append(batch).append("(builder);\n");
		}
		source.append("\t}\n");
		for (int start = 0, batch = 0; start < definition.transitions().size(); start += MAX_HELPER_ITEMS, batch++) {
			int end = Math.min(start + MAX_HELPER_ITEMS, definition.transitions().size());
			source.append("\n\tprivate static void addTransitionBatch").append(batch)
				.append("(QuestDsl.QuestBuilder builder) {\n");
			for (int index = start; index < end; index++) {
				source.append("\t\taddTransition").append(index).append("(builder);\n");
			}
			source.append("\t}\n");
		}
		for (int index = 0; index < definition.transitions().size(); index++) {
			QuestTransition transition = definition.transitions().get(index);
			source.append("\n\tprivate static void addTransition").append(index)
				.append("(QuestDsl.QuestBuilder builder) {\n")
				.append("\t\tbuilder.on(").append(expression(transition.event(), null)).append(")");
			if (transition.sourceNode() != null) {
				source.append(".from(").append(stringLiteral(transition.sourceNode())).append(")");
			}
			if (transition.priority() != null) {
				source.append(".priority(").append(transition.priority()).append(")");
			}
			for (QuestCondition condition : transition.conditions()) {
				source.append(".when(").append(expression(condition, null)).append(")");
			}
			for (QuestAction action : transition.actions()) {
				source.append(".then(").append(expression(action, null)).append(")");
			}
			source.append(".goTo(").append(stringLiteral(transition.targetNode())).append(");\n");
			for (AfterCommitAction action : transition.afterCommit()) {
				source.append("\t\tbuilder.afterCommit(").append(expression(action, null)).append(");\n");
			}
			source.append("\t}\n");
		}
		source.append("}\n");
		return source.toString();
	}

	private static String renderCatalog(List<Definition> definitions) {
		StringBuilder source = new StringBuilder(definitions.size() * 48);
		source.append("package ").append(GENERATED_PACKAGE).append(";\n\n")
			.append("import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;\n")
			.append("import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;\n")
			.append("import com.aionemu.gameserver.questEngine.definition.QuestCatalog;\n")
			.append("import java.util.ArrayList;\n")
			.append("import java.util.List;\n\n")
			.append("/** Generated registry for every quest-definition Java DSL owner. */\n")
			.append("public final class GeneratedQuestDslCatalog {\n")
			.append("\tprivate GeneratedQuestDslCatalog() {}\n\n")
			.append("\tpublic static QuestCatalog compile() {\n")
			.append("\t\tList<CompiledQuestDefinition> definitions = new ArrayList<>(")
			.append(definitions.size()).append(");\n");
		for (Definition definition : definitions) {
			source.append("\t\tdefinitions.add(Quest").append(definition.id()).append(".definition());\n");
		}
		source.append("\t\treturn new ImmutableQuestCatalog(definitions);\n\t}\n}\n");
		return source.toString();
	}

	private static void appendExpressions(StringBuilder source, Collection<?> values, String ignored) {
		boolean first = true;
		for (Object value : values) {
			if (!first) {
				source.append(", ");
			}
			first = false;
			source.append(expression(value, null));
		}
	}

	private static String expression(Object value, Class<?> declaredType) {
		if (value == null) {
			return "null";
		}
		if (value instanceof String string) {
			return stringLiteral(string);
		}
		if (value instanceof Character character) {
			return "'" + escapeCharacter(character) + "'";
		}
		if (value instanceof Boolean || value instanceof Integer || value instanceof Short) {
			return value.toString();
		}
		if (value instanceof Byte byteValue) {
			return "(byte) " + byteValue;
		}
		if (value instanceof Long) {
			return value + "L";
		}
		if (value instanceof Float floatValue) {
			return floatLiteral(floatValue);
		}
		if (value instanceof Double doubleValue) {
			return doubleLiteral(doubleValue);
		}
		if (value instanceof Enum<?> enumValue) {
			return typeName(enumValue.getDeclaringClass()) + "." + enumValue.name();
		}
		if (value instanceof Map<?, ?> map) {
			if (map.isEmpty()) {
				return "Map.of()";
			}
			return "Map.ofEntries(" + map.entrySet().stream()
				.map(entry -> "Map.entry(" + expression(entry.getKey(), null) + ", "
					+ expression(entry.getValue(), null) + ")")
				.collect(Collectors.joining(", ")) + ")";
		}
		if (value instanceof Set<?> set) {
			return collectionLiteral("Set.of", set);
		}
		if (value instanceof List<?> list) {
			return collectionLiteral("List.of", list);
		}
		if (value instanceof Collection<?> collection) {
			return collectionLiteral("List.of", collection);
		}
		Class<?> valueType = value.getClass();
		if (valueType.isArray()) {
			StringBuilder array = new StringBuilder("new ").append(typeName(valueType.getComponentType()))
				.append("[] {");
			for (int i = 0; i < Array.getLength(value); i++) {
				if (i > 0) {
					array.append(", ");
				}
				array.append(expression(Array.get(value, i), valueType.getComponentType()));
			}
			return array.append("}").toString();
		}
		if (valueType.isRecord()) {
			RecordComponent[] components = valueType.getRecordComponents();
			String args = java.util.Arrays.stream(components)
				.map(component -> expression(readComponent(value, component), component.getType()))
				.collect(Collectors.joining(", "));
			return "new " + typeName(valueType) + "(" + args + ")";
		}
		throw new IllegalArgumentException("unsupported generated DSL value: " + valueType.getName()
			+ " declared as " + (declaredType == null ? "unknown" : declaredType.getName()));
	}

	private static Object readComponent(Object value, RecordComponent component) {
		try {
			return component.getAccessor().invoke(value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("cannot read " + component.getName(), e);
		}
	}

	private static String collectionLiteral(String factory, Collection<?> values) {
		return factory + "(" + values.stream().map(value -> expression(value, null))
			.collect(Collectors.joining(", ")) + ")";
	}

	private static String typeName(Class<?> type) {
		if (type.isArray()) {
			return typeName(type.getComponentType()) + "[]";
		}
		if (type.isPrimitive()) {
			return type.getName();
		}
		String canonicalName = type.getCanonicalName();
		String name = Objects.requireNonNullElse(canonicalName, type.getName().replace('$', '.'));
		String definitionPrefix = "com.aionemu.gameserver.questEngine.definition.";
		if (name.startsWith(definitionPrefix)) {
			return name.substring(definitionPrefix.length());
		}
		String gameModelPrefix = "com.aionemu.gameserver.model.";
		if (name.startsWith(gameModelPrefix)) {
			return name.substring(gameModelPrefix.length());
		}
		String questModelPrefix = "com.aionemu.gameserver.questEngine.model.";
		if (name.startsWith(questModelPrefix)) {
			return name.substring(questModelPrefix.length());
		}
		return name;
	}

	private static String stringLiteral(String value) {
		StringBuilder literal = new StringBuilder("\"");
		for (char character : value.toCharArray()) {
			switch (character) {
				case '\\' -> literal.append("\\\\");
				case '"' -> literal.append("\\\"");
				case '\n' -> literal.append("\\n");
				case '\r' -> literal.append("\\r");
				case '\t' -> literal.append("\\t");
				case '\b' -> literal.append("\\b");
				case '\f' -> literal.append("\\f");
				default -> {
					if (character < 0x20 || character > 0x7e) {
						literal.append(String.format("\\u%04x", (int) character));
					} else {
						literal.append(character);
					}
				}
			}
		}
		return literal.append('"').toString();
	}

	private static String escapeCharacter(char value) {
		return switch (value) {
			case '\\' -> "\\\\";
			case '\'' -> "\\'";
			case '\n' -> "\\n";
			case '\r' -> "\\r";
			case '\t' -> "\\t";
			default -> value < 0x20 || value > 0x7e
				? String.format("\\u%04x", (int) value) : Character.toString(value);
		};
	}

	private static String floatLiteral(float value) {
		if (Float.isNaN(value)) {
			return "Float.NaN";
		}
		if (Float.isInfinite(value)) {
			return value > 0 ? "Float.POSITIVE_INFINITY" : "Float.NEGATIVE_INFINITY";
		}
		return Float.toString(value) + "f";
	}

	private static String doubleLiteral(double value) {
		if (Double.isNaN(value)) {
			return "Double.NaN";
		}
		if (Double.isInfinite(value)) {
			return value > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
		}
		return Double.toString(value) + "d";
	}

	private record Definition(int id, CompiledQuestDefinition compiled) {
	}
}
