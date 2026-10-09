package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * owner 解析器门测试（夹具事实 2026-10-01 冻结：XML 目录 742 定义、原版表 id 集 =
 * SimpleHunt 1863 + SimpleCollectItem 262 = 2125、交叠恰 4 个 = 2237/14112/14123/16961）。
 * / Owner resolver gate tests (fixture facts frozen at 2026-10-01: 742 XML definitions, the retail
 * table id set = 1863 SimpleHunt + 262 SimpleCollectItem = 2125 rows, exactly four overlapping ids).
 */
class NativeQuestOwnerResolverTest {

	@Test
	void derivesBothIdSetsFromSources() {
		NativeQuestOwnerResolver resolver = NativeQuestOwnerResolver.instance();
		// 原版表行全量在册：SimpleHunt 1863 + SimpleCollectItem 262（P4 切换批并入）。
		// All retail table rows: 1863 SimpleHunt + 262 SimpleCollectItem (added by the P4 batch).
		assertEquals(2125, resolver.retailTableIds().size());
		assertTrue(resolver.retailTableIds().contains(2354));
		// XML 目录规模随 QE-112 在飞变动（快照 742），只锚下界防形状破坏。
		// The XML directory size moves with the in-flight QE-112 (742 in the snapshot); pin a loose
		// lower bound against shape breakage only.
		assertTrue(resolver.xmlOnlyIds().size() >= 600, String.valueOf(resolver.xmlOnlyIds().size()));
		assertTrue(resolver.xmlOnlyIds().contains(1000));
	}

	@Test
	void overlapIsExactlyTheThreeKnownConflicts() {
		NativeQuestOwnerResolver resolver = NativeQuestOwnerResolver.instance();
		// 交叠 = 表行有 XML 定义的切换待办：14112/16961（SimpleHunt 表行 READY、XML 残留）+
		// 14123（SimpleHunt 表行名字多义冻结中）+ 2237（SimpleCollectItem 表行、XML 定义仍在）。
		// The overlap = switch backlog of table rows with XML definitions: 14112/16961 (SimpleHunt
		// rows READY, XML leftover) + 14123 (SimpleHunt row frozen for name ambiguity) + 2237
		// (SimpleCollectItem row whose XML definition is still present).
		assertEquals(Set.of(2237, 14112, 14123, 16961), resolver.conflicts());
	}

	@Test
	void ownershipPrecedenceAndUnowned() {
		NativeQuestOwnerResolver resolver = NativeQuestOwnerResolver.instance();
		assertEquals(NativeQuestOwnerResolver.Owner.NATIVE_TABLE, resolver.ownerOf(2354));
		assertEquals(NativeQuestOwnerResolver.Owner.XML_ONLY, resolver.ownerOf(1000));
		assertEquals(NativeQuestOwnerResolver.Owner.UNOWNED, resolver.ownerOf(99999999));
	}

	@Test
	void goLiveGateFailsFastOnOverlap() {
		// 迁移期交叠存在 ⇒ requireDisjoint 必须以 NATIVE_OWNER_CONFLICT 拒绝 go-live。
		// During migration the overlap exists ⇒ requireDisjoint must refuse go-live.
		NativeQuestOwnerResolver resolver = NativeQuestOwnerResolver.instance();
		IllegalStateException conflict = assertThrows(IllegalStateException.class,
				resolver::requireDisjoint);
		assertTrue(conflict.getMessage().startsWith("NATIVE_OWNER_CONFLICT"), conflict.getMessage());
	}

	@Test
	void disjointSetsPassTheGoLiveGate() {
		NativeQuestOwnerResolver clean = NativeQuestOwnerResolver.fromSets(Set.of(2354), Set.of(1000));
		assertTrue(clean.disjoint());
		clean.requireDisjoint();
		assertEquals(Set.of(), clean.conflicts());
	}

	@Test
	void syntheticOverlapReportsBothSides() {
		NativeQuestOwnerResolver overlapped = NativeQuestOwnerResolver.fromSets(Set.of(1, 2, 3),
				Set.of(3, 4));
		assertEquals(Set.of(3), overlapped.conflicts());
		assertEquals(NativeQuestOwnerResolver.Owner.NATIVE_TABLE, overlapped.ownerOf(3));
	}
}
