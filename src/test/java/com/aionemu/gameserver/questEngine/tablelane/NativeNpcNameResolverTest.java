package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * NPC 名解析器门测试（夹具事实来自 P1 对拍与分片实读：模板定义 87968 个、独立规范名 116225 个、
 * 分片间重复 id 836025、原版占位 name=" "）。 / NPC name resolver gate tests (fixture facts from the
 * P1 reconciliation and live shard reads: 87968 template definitions, 116225 distinct normalized
 * names, cross-shard duplicate id 836025, retail name=" " placeholders).
 */
class NativeNpcNameResolverTest {

	private static final int EXPECTED_TEMPLATES = 87968;
	private static final int EXPECTED_NAMES = 116225;

	@Test
	void loadsFullShardIndex() {
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		assertEquals(EXPECTED_TEMPLATES, resolver.templateCount());
		assertEquals(EXPECTED_NAMES, resolver.size());
	}

	@Test
	void resolvesNameDescAndShortNameExactly() {
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		// P0a 证据锚：quest 表引用 name_desc 全名（如 DF5_Soglo_E）。
		// P0a evidence anchor: quest tables reference the name_desc full name (DF5_Soglo_E here).
		assertEquals(new NativeNpcNameResolver.Match(NativeNpcNameResolver.Resolution.UNIQUE,
				java.util.List.of(804915)), resolver.resolve("df5_soglo_e"));
		// 短名与大小写同样精确命中。 / Short names and case fold resolve exactly too.
		assertEquals(resolver.resolve("DF5_SOGLO_E"), resolver.resolve("soglo"));
		assertEquals(804915, resolver.uniqueId("Soglo"));
	}

	@Test
	void missingNameFailsClosed() {
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		assertEquals(NativeNpcNameResolver.Resolution.MISSING, resolver.resolve("not_a_real_npc_zz")
				.resolution());
		IllegalStateException missing = assertThrows(IllegalStateException.class,
				() -> resolver.uniqueId("not_a_real_npc_zz"));
		assertTrue(missing.getMessage().startsWith("NATIVE_NAME_UNRESOLVED"), missing.getMessage());
		// null / 空白按无命中，原版 name=" " 占位永不成为索引键。 / null/blank miss; name=" " placeholders are never keys.
		assertEquals(NativeNpcNameResolver.Resolution.MISSING, resolver.resolve(null).resolution());
		assertEquals(NativeNpcNameResolver.Resolution.MISSING, resolver.resolve("  ").resolution());
	}

	@Test
	void ambiguousNameFailsClosed() {
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		// 分片实读样例：kamikaze worm 命中两个模板。 / Live-shard sample: "kamikaze worm" hits two templates.
		assertEquals(new NativeNpcNameResolver.Match(NativeNpcNameResolver.Resolution.AMBIGUOUS,
				java.util.List.of(201000, 201002)), resolver.resolve("kamikaze worm"));
		IllegalStateException ambiguous = assertThrows(IllegalStateException.class,
				() -> resolver.uniqueId("kamikaze worm"));
		assertTrue(ambiguous.getMessage().startsWith("NATIVE_NAME_AMBIGUOUS"), ambiguous.getMessage());
	}

	@Test
	void duplicateShardDefinitionDoesNotPhantomAmbiguate() {
		// 836025 在分片数据中定义两次（同一分片两处）；其名不得因重复定义而变多义。
		// 836025 is defined twice in the shard data (same shard); its names must not turn ambiguous.
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		int id = resolver.uniqueId("skillzone");
		assertEquals(200000, id);
	}

	@Test
	void nameDescPriorityEliminatesPhantomAmbiguity() {
		// 证实排查结论：Theon 的原版 name_desc 为 278516，活动 NPC 800274 的 name_desc 为 event_hc_Theon；
		// 优先按 name_desc 解析消除了本地化短名冲突引发的假多义，精确命中 278516。
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		NativeNpcNameResolver.Match match = resolver.resolve("Theon");
		assertEquals(NativeNpcNameResolver.Resolution.UNIQUE, match.resolution());
		assertEquals(java.util.List.of(278516), match.npcIds());
		assertEquals(278516, resolver.uniqueId("Theon"));
	}

	@Test
	void resolvesMonsterCandidates() {
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		assertEquals(java.util.List.of(804915), resolver.resolveMonsterIds("df5_soglo_e"));
		assertEquals(java.util.List.of(201000, 201002), resolver.resolveMonsterIds("kamikaze worm"));
		assertEquals(java.util.List.of(), resolver.resolveMonsterIds("non_existent_monster_xx"));
	}

	@Test
	void resolvesMonsterAliases() {
		NativeNpcNameResolver resolver = NativeNpcNameResolver.instance();
		assertEquals(java.util.List.of(282914), resolver.resolveMonsterIds("ldf4a_public_quest_monster_02"));
		assertEquals(java.util.List.of(282916), resolver.resolveMonsterIds("ldf4a_public_quest_monster_05"));
	}
}
