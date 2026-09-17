package com.aionemu.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

import lombok.Data;

/**
 * 固化实体类的"身份相等"契约：实体不能套 @Data。
 * Pins the identity-equality contract of game entities: entity classes must not use @Data.
 *
 * <p>背景：{@code TemporaryTradeTimeTask} 用 {@code HashMap<Item, Collection<Integer>>} 记录临时交易窗口，
 * 且 Item 的 count/color/enchant 等字段在交易期间会被改写。若 Item 获得按字段比较的 equals/hashCode，
 * {@code items.get(item)} 会因哈希值变化而失效，交易窗口静默丢失。
 * Background: {@code TemporaryTradeTimeTask} keeps a {@code HashMap<Item, Collection<Integer>>} for
 * temporary-trade windows while item fields such as count/color/enchant are mutated during the trade.
 * If Item gained field-based equals/hashCode, {@code items.get(item)} would silently fail.</p>
 *
 * @author refactor-guard
 */
class EntityIdentitySemanticsTest {

	/**
	 * 反例：把 @Data（按字段 equals/hashCode）套到可变实体上，改成键后查不回来。
	 * Negative case: @Data (field-based equals/hashCode) on a mutable entity loses its map entry after mutation.
	 */
	@Test
	void fieldBasedEqualityLosesMapEntryWhenTheEntityMutates() {
		Map<FieldBasedEntity, String> byEntity = new HashMap<>();
		FieldBasedEntity entity = new FieldBasedEntity(1, 100);
		byEntity.put(entity, "registered");
		assertNotNull(byEntity.get(entity), "未改动前应能查到 / lookup works before mutation");

		entity.setAmount(150); // 模拟 Item#setItemCount 这类字段改写 / mimics Item#setItemCount

		assertNull(byEntity.get(entity), "字段改动后应按 @Data 语义查不到 / @Data semantics loses the entry");
	}

	/**
	 * 正例：保持身份相等的实体，字段改动后仍能命中同一条目（当前仓库行为）。
	 * Positive case: identity-based entities keep their map entry across field mutation (current behavior).
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
	 * 护栏：核心实体类的源码不得出现 @Data / @EqualsAndHashCode / @ToString。
	 * Guard: the source of core entity classes must not carry @Data / @EqualsAndHashCode / @ToString.
	 *
	 * <p>为什么读源码而不是反射：Lombok 的 @Data 只有 CLASS retention，不进运行时可见注解，
	 * {@code clazz.isAnnotationPresent(Data.class)} 永远为 false，起不到护栏作用。
	 * Why source-level instead of reflection: Lombok's @Data keeps CLASS retention only, so it never
	 * appears in RuntimeVisibleAnnotations and a reflective guard would silently pass.</p>
	 *
	 * <p>为什么禁用这三个注解：这些类的字段可变、会被当作 Map 键（见 {@code TemporaryTradeTimeTask}），
	 * 且对象图存在双向引用。按字段 equals/hashCode 会让 Map/Set 静默失效，递归 toString 有栈溢出风险。
	 * Why these three: the fields are mutable, the instances are used as map keys (see
	 * {@code TemporaryTradeTimeTask}) and the object graphs contain cycles, so field-based
	 * equals/hashCode breaks lookups and a recursive toString risks stack overflow.</p>
	 */
	@Test
	void coreEntitySourcesMustNotDeclareFieldBasedEqualityOrToString() throws Exception {
		Class<?>[] entities = { Player.class, Creature.class, Npc.class, Item.class, Effect.class };
		for (Class<?> entity : entities) {
			String source = readSource(entity);
			for (String forbidden : new String[] { "@Data", "@EqualsAndHashCode", "@ToString" }) {
				assertFalse(hasAnnotationLine(source, forbidden),
						entity.getSimpleName() + " 的源码不应出现 " + forbidden
								+ "：字段可变且被当作 Map 键，按字段比较会让 Map/Set 静默失效"
								+ " / must not use field-based equality: mutable fields are used as map keys");
			}
		}
	}

	/**
	 * 读取被测类的源码。
	 * Reads the source file of the given class.
	 */
	private static String readSource(Class<?> type) throws Exception {
		Path classesDir = Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path projectRoot = classesDir.getParent().getParent();
		Path source = projectRoot.resolve("src/main/java").resolve(type.getName().replace('.', '/') + ".java");
		assertTrue(Files.exists(source), "未找到源码文件 / source file not found: " + source);
		return Files.readString(source, StandardCharsets.UTF_8);
	}

	/**
	 * 判断源码里是否存在该注解声明的独立行（避免匹配注释中的文字）。
	 * Whether the source declares the annotation on its own line (avoids matching comments).
	 */
	private static boolean hasAnnotationLine(String source, String annotation) {
		for (String line : source.split(System.lineSeparator())) {
			String trimmed = line.trim();
			if (trimmed.equals(annotation) || trimmed.startsWith(annotation + "(")) {
				return true;
			}
		}
		return false;
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
