package com.aionemu.boot.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * 本地化日志参数的静态闸门：{@code I18n.get} 的每个参数都必须被模板占位符消费。
 * Static gate for localized log arguments: every {@code I18n.get} argument must be consumed by a
 * template placeholder.
 * <p>
 * 不变量 / Invariants:
 * <ul>
 *   <li>参数个数等于两套语言包中占位符最大序号加一；否则参数被静默丢弃，或模板取值落空。</li>
 *   <li>异常对象不得传给 {@code I18n.get}：占位符只渲染 {@code toString()}，堆栈会丢失；
 *       异常必须作为日志调用的最后一个参数传递。</li>
 *   <li>两套语言包的键集合、非空值与占位符集合保持一致。</li>
 * </ul>
 * 参数必须被占位符消费；异常必须走日志调用的最后一个参数，才能保留堆栈。
 * Arguments must be consumed by placeholders, and throwables must travel as the trailing argument
 * of the logging call so that the stack trace survives.
 */
class LocalizedLogArgumentsTest {

	private static final Pattern I18N_CALL = Pattern.compile("\\bI18n\\.get\\s*\\(");
	private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)");
	private static final Pattern THROWABLE_ARG =
		Pattern.compile("(?:e|ex|exc|error|t|th|thr|throwable|cause|exception|var\\d+)");
	private static final int MIN_CALL_SITES = 1000;
	private static final Path SOURCE_ROOT = Path.of("src/main/java");
	private static final Path ENGLISH_BUNDLE = Path.of("src/main/resources/messages.properties");
	private static final Path CHINESE_BUNDLE = Path.of("src/main/resources/messages_zh_CN.properties");

	@Test
	void argumentsMatchPlaceholdersInBothBundles() throws IOException {
		Map<String, String> english = loadBundle(ENGLISH_BUNDLE);
		Map<String, String> chinese = loadBundle(CHINESE_BUNDLE);
		List<String> problems = new ArrayList<>();
		int callSites = 0;
		for (Path path : javaSources()) {
			String code = stripComments(Files.readString(path, StandardCharsets.UTF_8));
			Matcher matcher = I18N_CALL.matcher(code);
			while (matcher.find()) {
				int open = code.indexOf('(', matcher.start());
				int end = matchingParen(code, open);
				if (end < 0) {
					continue;
				}
				List<String> arguments = splitTopLevel(code.substring(open + 1, end - 1));
				Matcher literal = STRING_LITERAL.matcher(arguments.get(0).trim());
				if (!literal.matches()) {
					continue;
				}
				callSites++;
				String key = literal.group(1);
				String where = path + ":" + lineOf(code, matcher.start());
				int passed = arguments.size() - 1;
				checkPlaceholderCount(problems, where, key, passed, "en", english);
				checkPlaceholderCount(problems, where, key, passed, "zh", chinese);
				for (String argument : arguments.subList(1, arguments.size())) {
					String trimmed = argument.trim();
					if (THROWABLE_ARG.matcher(trimmed).matches()
						|| trimmed.startsWith("new ") && trimmed.contains("Exception")) {
						problems.add(where + " " + key + ": throwable passed to I18n.get -> " + trimmed);
					}
				}
			}
		}
		assertTrue(callSites > MIN_CALL_SITES, "localized call sites scanned: " + callSites);
		assertTrue(problems.isEmpty(), "localized log arguments must match both bundles:\n"
			+ String.join("\n", problems));
	}

	@Test
	void bilingualBundlesStaySynchronized() throws IOException {
		Map<String, String> english = loadBundle(ENGLISH_BUNDLE);
		Map<String, String> chinese = loadBundle(CHINESE_BUNDLE);
		List<String> problems = new ArrayList<>();
		Set<String> missingInChinese = new HashSet<>(english.keySet());
		missingInChinese.removeAll(chinese.keySet());
		Set<String> missingInEnglish = new HashSet<>(chinese.keySet());
		missingInEnglish.removeAll(english.keySet());
		if (!missingInChinese.isEmpty() || !missingInEnglish.isEmpty()) {
			problems.add("key sets differ: missing in zh=" + missingInChinese + " missing in en=" + missingInEnglish);
		}
		for (String key : english.keySet()) {
			if (!chinese.containsKey(key)) {
				continue;
			}
			String en = english.get(key);
			String zh = chinese.get(key);
			if (en.isBlank() || zh.isBlank()) {
				problems.add(key + ": blank message in one bundle");
			}
			Set<Integer> enIndices = placeholderIndices(en);
			Set<Integer> zhIndices = placeholderIndices(zh);
			if (!enIndices.equals(zhIndices)) {
				problems.add(key + ": placeholder mismatch en=" + enIndices + " zh=" + zhIndices);
			}
		}
		assertTrue(problems.isEmpty(), "bilingual bundles must stay in sync:\n" + String.join("\n", problems));
	}

	private void checkPlaceholderCount(
		List<String> problems, String where, String key, int passed, String locale, Map<String, String> bundle) {
		String message = bundle.get(key);
		if (message == null) {
			problems.add(where + " " + key + ": missing from " + locale + " bundle");
			return;
		}
		Set<Integer> indices = placeholderIndices(message);
		int wanted = indices.isEmpty() ? 0 : indices.stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
		if (wanted != passed) {
			problems.add(where + " " + key + ": " + locale + " template wants " + wanted
				+ " args, call passes " + passed);
		}
	}

	private Set<Integer> placeholderIndices(String message) {
		Set<Integer> indices = new HashSet<>();
		Matcher matcher = PLACEHOLDER.matcher(message);
		while (matcher.find()) {
			indices.add(Integer.parseInt(matcher.group(1)));
		}
		return indices;
	}

	private List<Path> javaSources() throws IOException {
		List<Path> sources = new ArrayList<>();
		try (var paths = Files.walk(SOURCE_ROOT)) {
			paths.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
		}
		return sources;
	}

	private Map<String, String> loadBundle(Path path) throws IOException {
		Properties properties = new Properties();
		try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		Map<String, String> messages = new HashMap<>();
		for (String name : properties.stringPropertyNames()) {
			messages.put(name, properties.getProperty(name));
		}
		return messages;
	}

	private String stripComments(String source) {
		StringBuilder out = new StringBuilder(source);
		int index = 0;
		while (index < source.length()) {
			char current = source.charAt(index);
			if (current == '"' || current == '\'') {
				index = skipQuoted(source, index, current);
			} else if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
				while (index < source.length() && source.charAt(index) != '\n') {
					out.setCharAt(index++, ' ');
				}
			} else if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
				out.setCharAt(index++, ' ');
				out.setCharAt(index++, ' ');
				while (index < source.length()
					&& !(source.charAt(index) == '*' && index + 1 < source.length() && source.charAt(index + 1) == '/')) {
					if (source.charAt(index) != '\n') {
						out.setCharAt(index, ' ');
					}
					index++;
				}
				if (index < source.length()) {
					out.setCharAt(index++, ' ');
					out.setCharAt(index++, ' ');
				}
			} else {
				index++;
			}
		}
		return out.toString();
	}

	private int skipQuoted(String source, int start, char quote) {
		int index = start + 1;
		while (index < source.length()) {
			char current = source.charAt(index);
			if (current == '\\') {
				index += 2;
				continue;
			}
			if (current == quote || quote == '"' && current == '\n') {
				return index + 1;
			}
			index++;
		}
		return index;
	}

	private int matchingParen(String code, int open) {
		int depth = 0;
		int index = open;
		while (index < code.length()) {
			char current = code.charAt(index);
			if (current == '"' || current == '\'') {
				index = skipQuoted(code, index, current);
				continue;
			}
			if (current == '(') {
				depth++;
			} else if (current == ')') {
				depth--;
				if (depth == 0) {
					return index + 1;
				}
			}
			index++;
		}
		return -1;
	}

	private List<String> splitTopLevel(String arguments) {
		List<String> parts = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int depth = 0;
		int index = 0;
		while (index < arguments.length()) {
			char c = arguments.charAt(index);
			if (c == '"' || c == '\'') {
				int next = skipQuoted(arguments, index, c);
				current.append(arguments, index, next);
				index = next;
				continue;
			}
			if (c == '(' || c == '[' || c == '{') {
				depth++;
			} else if (c == ')' || c == ']' || c == '}') {
				depth--;
			}
			if (c == ',' && depth == 0) {
				parts.add(current.toString().trim());
				current.setLength(0);
			} else {
				current.append(c);
			}
			index++;
		}
		if (!current.toString().isBlank()) {
			parts.add(current.toString().trim());
		}
		return parts;
	}

	private int lineOf(String code, int offset) {
		int line = 1;
		for (int index = 0; index < offset && index < code.length(); index++) {
			if (code.charAt(index) == '\n') {
				line++;
			}
		}
		return line;
	}
}
