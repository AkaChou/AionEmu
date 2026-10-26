package com.aionemu.boot.web.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 行级保留的 {@code .properties} 文档模型：保留每一行的原文，只在值发生变化时替换该行。
 * Line-preserving {@code .properties} document model: every line keeps its original text and only the
 * lines whose value actually changed get replaced.
 * <p>
 * 配置文件里的双语注释块（{@code # 默认值: 3 / Default: 3}）承载着运维语义，绝不能被整表重写抹掉，
 * 因此本模型以「行」为单位而不是以「键值对」为单位。
 * <p>
 * The bilingual comment blocks in the configuration files ({@code # 默认值: 3 / Default: 3}) carry
 * operational meaning and must never be wiped by a whole-table rewrite, so this model works in lines rather
 * than in key-value pairs.
 *
 * @author AionEmu
 */
public final class PropertiesDocument {

	/**
	 * 文档行列表。
	 * Document lines.
	 */
	private final List<Line> lines;

	/**
	 * 以给定行构造文档。
	 * Constructs a document from the given lines.
	 * @param lines 文档行 / document lines
	 */
	private PropertiesDocument(List<Line> lines) {
		this.lines = lines;
	}

	/**
	 * 解析 {@code .properties} 文本。
	 * <p>
	 * 只识别「首个非空白字符为 {@code =}」之前的部分为键；{@code #} 与 {@code !} 开头的行为注释。
	 * 键值两端的空白被去除，与 {@code Properties.load} 的语义一致；值内部的空白保留。
	 * <p>
	 * Parses {@code .properties} text.
	 * <p>
	 * A line is a key/value pair when it contains an {@code =} outside a leading comment; lines starting with
	 * {@code #} or {@code !} are comments. Whitespace around the key and the value is stripped, matching
	 * {@code Properties.load}; whitespace inside the value is preserved.
	 * @param text 文件文本 / file text
	 * @return 文档模型 / document model
	 */
	public static PropertiesDocument parse(String text) {
		List<Line> parsed = new ArrayList<>();
		for (String raw : text.split("\n", -1)) {
			parsed.add(Line.of(raw));
		}
		return new PropertiesDocument(parsed);
	}

	/**
	 * 返回文档中的全部键值对（按出现顺序）。
	 * Returns every key-value pair of the document, in document order.
	 * @return 键值表 / key-value map
	 */
	public Map<String, String> values() {
		Map<String, String> values = new LinkedHashMap<>();
		for (Line line : lines) {
			if (line.isEntry()) {
				values.put(line.key(), line.value());
			}
		}
		return values;
	}

	/**
	 * 返回按出现顺序排列的键列表（含重复）。
	 * Returns the keys in document order, including duplicates.
	 * @return 键列表 / key list
	 */
	public List<String> keys() {
		List<String> keys = new ArrayList<>();
		for (Line line : lines) {
			line.entryKey().ifPresent(keys::add);
		}
		return keys;
	}

	/**
	 * 返回键在文档中出现的行号（从 0 开始）；键不存在时返回空。
	 * Returns the line index of a key (0-based), or an empty optional when the key is absent.
	 * @param key 键 / key
	 * @return 行号 / line index
	 */
	public Optional<Integer> lineIndexOf(String key) {
		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			if (line.entryKey().isPresent() && line.entryKey().get().equals(key)) {
				return Optional.of(i);
			}
		}
		return Optional.empty();
	}

	/**
	 * 返回键在文档中出现的次数。
	 * Returns how many times a key occurs in the document.
	 * @param key 键 / key
	 * @return 出现次数 / occurrence count
	 */
	public int occurrencesOf(String key) {
		int count = 0;
		for (Line line : lines) {
			if (line.entryKey().isPresent() && line.entryKey().get().equals(key)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 以新值替换键所在的行，返回新文档；键不存在时返回原文档。
	 * <p>
	 * 仅替换该行的值部分，键与分隔符之间的空白原样保留；其余行一个字节都不动。
	 * <p>
	 * Replaces the value of a key's line and returns the new document; the original document is returned when
	 * the key is absent.
	 * <p>
	 * Only the value part of that line is replaced, the whitespace between the key and the separator is kept
	 * as-is, and every other line stays byte-identical.
	 * @param key 键 / key
	 * @param newValue 新值 / new value
	 * @return 新文档 / new document
	 */
	public PropertiesDocument withValue(String key, String newValue) {
		int index = lineIndexOf(key).orElse(-1);
		if (index < 0) {
			return this;
		}
		List<Line> replaced = new ArrayList<>(lines);
		replaced.set(index, replaced.get(index).withValue(newValue));
		return new PropertiesDocument(replaced);
	}

	/**
	 * 以新值替换键所在的全部行。
	 * Replaces the value on every line carrying the given key.
	 * @param key 键 / key
	 * @param newValue 新值 / new value
	 * @return 新文档 / new document
	 */
	public PropertiesDocument withAllValues(String key, String newValue) {
		List<Line> replaced = new ArrayList<>(lines.size());
		for (Line line : lines) {
			replaced.add(line.entryKey().isPresent() && line.entryKey().get().equals(key) ? line.withValue(newValue) : line);
		}
		return new PropertiesDocument(replaced);
	}

	/**
	 * 渲染回文本，保持原始行分隔符（LF）。
	 * Renders back to text, keeping the original line separator (LF).
	 * @return 文本 / text
	 */
	public String render() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) {
				builder.append('\n');
			}
			builder.append(lines.get(i).original());
		}
		return builder.toString();
	}

	/**
	 * 文档行：键值行或注释行。
	 * A document line: either a key-value line or a comment line.
	 *
	 * @param original 原始行文本 / original line text
	 * @param key 键；注释行为空 / key, empty for comment lines
	 * @param value 值；注释行为空 / value, empty for comment lines
	 * @param valueStart 值在原始行中的起始偏移；注释行为 -1 / offset where the value starts within the
	 *                    original line, -1 for comment lines
	 * @param valueEnd 值在原始行中的结束偏移（不含）；注释行为 -1 / exclusive end offset of the value within
	 *                 the original line, -1 for comment lines
	 */
	public record Line(String original, String key, String value, int valueStart, int valueEnd) {

		/**
		 * 解析一行。
		 * Parses one line.
		 * @param raw 原始行文本 / raw line text
		 * @return 文档行 / document line
		 */
		static Line of(String raw) {
			String trimmed = raw.strip();
			if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
				return new Line(raw, null, null, -1, -1);
			}
			int separator = raw.indexOf('=');
			if (separator < 0) {
				return new Line(raw, null, null, -1, -1);
			}
			String key = raw.substring(0, separator).strip();
			int valueStart = separator + 1;
			while (valueStart < raw.length() && Character.isWhitespace(raw.charAt(valueStart))) {
				valueStart++;
			}
			int valueEnd = raw.length();
			while (valueEnd > valueStart && Character.isWhitespace(raw.charAt(valueEnd - 1))) {
				valueEnd--;
			}
			String value = raw.substring(valueStart, valueEnd);
			return new Line(raw, key, value, valueStart, valueEnd);
		}

		/**
		 * 返回键；注释行返回空。
		 * Returns the key, or an empty optional for comment lines.
		 * @return 键 / key
		 */
		public Optional<String> entryKey() {
			return Optional.ofNullable(key);
		}

		/**
		 * 返回值；注释行返回空。
		 * Returns the value, or an empty optional for comment lines.
		 * @return 值 / value
		 */
		public Optional<String> entryValue() {
			return Optional.ofNullable(value);
		}

		/**
		 * 返回该行是否承载键值对。
		 * Returns whether this line carries a key-value pair.
		 * @return 承载键值对则为 true / true when it carries a key-value pair
		 */
		public boolean isEntry() {
			return key != null;
		}

		/**
		 * 以新值生成新行：键与分隔符之间的空白保留，值替换为新值。
		 * Creates a new line with the given value: the whitespace between the key and the separator is kept,
		 * the value is replaced.
		 * @param newValue 新值 / new value
		 * @return 新行 / new line
		 */
		Line withValue(String newValue) {
			String prefix = original.substring(0, valueStart);
			String suffix = original.substring(valueEnd);
			return new Line(prefix + newValue + suffix, key, newValue, valueStart, valueStart + newValue.length());
		}
	}
}
