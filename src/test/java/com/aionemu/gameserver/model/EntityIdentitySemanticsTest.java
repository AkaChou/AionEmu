package com.aionemu.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import lombok.Data;

/**
 * 固化实体类的身份相等契约，并按规则约束 @Data 的使用范围。
 * Pins the identity-equality contract of game entities and constrains where @Data may be used.
 * <p>规则出处：{@code .agents/rules/lombok.md} → 「Beans and Data Objects」第 4 条。
 * 本测试不维护类型名单，只按规则自动判定：只要某个 @Data 类型被任何源码放进
 * {@code Map<该类型, …>} 的键位或 {@code Set<该类型>} 的元素位，该类型就依赖身份相等，
 * The rule lives in {@code .agents/rules/lombok.md}. This test keeps no type allowlist: whenever any
 * source places an @Data type in a map key position or a set element position, that type depends on
 * identity equality and @Data would silently break those containers.</p>
 * @author refactor-guard
 */
class EntityIdentitySemanticsTest {

	/**
	 * 反例：把 @Data（按字段 equals/hashCode）套到可变实体上，改成键后查不回来。
	 * Negative case: @Data on a mutable entity loses its map entry after a field mutation.
	 */
	@Test
	void fieldBasedEqualityLosesMapEntryWhenTheEntityMutates() {
		Map<FieldBasedEntity, String> byEntity = new HashMap<>();
		FieldBasedEntity entity = new FieldBasedEntity(1, 100);
		byEntity.put(entity, "registered");
		assertNotNull(byEntity.get(entity), "未改动前应能查到 / lookup works before mutation");

		entity.setAmount(150); // 模拟 Item#setItemCount 这类字段改写 / mimics Item#setItemCount

		assertNull(byEntity.get(entity), "字段改动后应按 @Data 语义查不到 / @Data semantics lose the entry");
	}

	/**
	 * 正例：保持身份相等的实体，字段改动后仍能命中同一条目（当前仓库行为）。
	 * Positive case: identity-based entities keep their map entry across field mutation.
	 */
	@Test
	void identityEqualityKeepsMapEntryWhenTheEntityMutates() {
		Map<IdentityBasedEntity, String> byEntity = new HashMap<>();
		IdentityBasedEntity entity = new IdentityBasedEntity(1, 100);
		byEntity.put(entity, "registered");
		assertNotNull(byEntity.get(entity), "未改动前应能查到 / lookup works before mutation");

		entity.setAmount(150);

		assertNotNull(byEntity.get(entity), "身份相等下字段改动不影响查表 / identity keeps the entry");
		assertTrue(byEntity.containsKey(entity), "containsKey 也应命中 / containsKey keeps working");
	}

	/**
	 * 规则护栏：带 @Data 的类不得被当作 Map 键 / Set 元素，也不得持有敏感字段。
	 * Rule guard: a class annotated with @Data must not be used as a map key or set element,
	 * and must not carry secret fields.
	 * <p>为什么扫源码：Lombok 的 @Data 只有 CLASS retention，不写进 RuntimeVisibleAnnotations，
	 * 反射检查恒为通过（见 lombok.md 使用边界第 4 条）。
	 * Why source scanning: Lombok's @Data has CLASS retention only and never reaches
	 * RuntimeVisibleAnnotations, so a reflective check would silently pass.</p>
	 */
	@Test
	void classesAnnotatedWithDataMustNotDependOnIdentityEquality() throws Exception {
		List<Path> sources = gameServerSources();
		List<String> dataTypes = new ArrayList<>();
		for (Path source : sources) {
			String text = Files.readString(source, StandardCharsets.UTF_8);
			String typeName = declaredTypeName(text);
			if (typeName != null && RegexHolder.DATA_ANNOTATION.matcher(text).find()) {
				dataTypes.add(typeName);
			}
		}
		assertTrue(!dataTypes.isEmpty(), "未扫描到任何 @Data 类，测试可能失效 / no @Data class was scanned");

		List<String> violations = new ArrayList<>();
		for (Path source : sources) {
			String text = Files.readString(source, StandardCharsets.UTF_8);
			String typeName = declaredTypeName(text);
			if (typeName != null && dataTypes.contains(typeName)
					&& RegexHolder.SECRET_FIELD.matcher(text).find()) {
				violations.add(source.getFileName() + " → 敏感字段 / secret field");
			}
			for (String dataType : dataTypes) {
				if (RegexHolder.mapKey(dataType).matcher(text).find()
						|| RegexHolder.setElement(dataType).matcher(text).find()) {
					violations.add(source.getFileName() + " → 把 @Data 类型 " + dataType
							+ " 用作 Map 键 / Set 元素（" + source + "）");
				}
			}
		}
		assertTrue(violations.isEmpty(),
				"违反 .agents/rules/lombok.md 第 4 条：@Data 类型依赖身份相等，按字段 equals/hashCode "
						+ "会让 Map/Set 静默失效 / @Data types used with identity semantics: " + violations);
	}

	/**
	 * 列出游戏服主源码目录下的全部 .java 文件。
	 * Lists all .java files under the game server main source tree.
	 */
	private static List<Path> gameServerSources() throws Exception {
		Path classesDir = Paths.get(EntityIdentitySemanticsTest.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		Path sourceRoot = classesDir.getParent().getParent().resolve("src/main/java/com/aionemu/gameserver");
		assertTrue(Files.isDirectory(sourceRoot), "未找到主源码目录 / source root not found: " + sourceRoot);
		try (Stream<Path> walk = Files.walk(sourceRoot)) {
			return walk.filter(path -> path.toString().endsWith(".java")).toList();
		}
	}

	/**
	 * 读取源码中声明的公开类型名（class / enum / record）。
	 * Reads the public type name declared in the given source.
	 */
	private static String declaredTypeName(String source) {
		var matcher = RegexHolder.TYPE_DECLARATION.matcher(source);
		return matcher.find() ? matcher.group(1) : null;
	}

	/**
	 * 预编译的正则集合。
	 * Precompiled regular expressions.
	 */
	private static final class RegexHolder {

		/** 类级 @Data 注解 / class level @Data annotation */
		private static final Pattern DATA_ANNOTATION = Pattern.compile("(?m)^\\s*@Data\\s*(?:\\(|$)");

		/** 字段级敏感信息 / secret-bearing fields */
		private static final Pattern SECRET_FIELD = Pattern.compile(
				"(?im)^\\s*(?:private|protected|public)\\s+[^;=]*\\b(?:password|passwd|secret|token|credential)\\b");

		/** 公开类型声明 / declared public type */
		private static final Pattern TYPE_DECLARATION =
				Pattern.compile("(?m)^\\s*public\\s+(?:final\\s+|abstract\\s+)?(?:class|enum|record)\\s+(\\w+)");

		/**
		 * Map 键位：要求该类型是第一个类型参数（`Map<Item,` 命中，`Map<Integer, Item>` 不命中）。
		 * Map key position: the type must be the first type argument.
		 */
		private static Pattern mapKey(String type) {
			return Pattern.compile("\\b(?:Map|HashMap|ConcurrentHashMap|WeakHashMap|IdentityHashMap|"
					+ "TreeMap|LinkedHashMap)\\s*<\\s*" + Pattern.quote(type) + "\\s*,");
		}

		/**
		 * Set 元素位：`Set<Item>` 命中（`List<Item>` 不命中）。
		 * Set element position: matches {@code Set<Item>} but not {@code List<Item>}.
		 */
		private static Pattern setElement(String type) {
			return Pattern.compile("\\b(?:Set|HashSet|CopyOnWriteArraySet|TreeSet|LinkedHashSet)"
					+ "\\s*<\\s*" + Pattern.quote(type) + "\\s*>");
		}

		private RegexHolder() {
		}
	}

	/**
	 * 演示用实体：按字段相等（等价于 @Data 生成的行为）。
	 * Demo entity with field-based equality (equivalent to what @Data generates).
	 */
	@Data
	private static class FieldBasedEntity {

		private final int id;
		private int amount;

		FieldBasedEntity(int id, int amount) {
			this.id = id;
			this.amount = amount;
		}
	}

	/**
	 * 演示用实体：保持身份相等（等价于当前仓库实体行为）。
	 * Demo entity with identity equality (equivalent to current entity behavior).
	 */
	private static class IdentityBasedEntity {

		private final int id;
		private int amount;

		IdentityBasedEntity(int id, int amount) {
			this.id = id;
			this.amount = amount;
		}

		void setAmount(int amount) {
			this.amount = amount;
		}
	}
}
