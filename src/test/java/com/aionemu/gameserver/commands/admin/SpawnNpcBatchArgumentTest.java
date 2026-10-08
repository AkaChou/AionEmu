package com.aionemu.gameserver.commands.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `//spawn <npcid>*<num>` 与 `//spawn *<num>` 批量语法的解析必须严格：写错一个字符应被拒绝，而不是刷出错误的量。
 * The {@code //spawn <npcid>*<num>} and {@code //spawn *<num>} batch syntax must be parsed strictly:
 * a mistyped token has to be rejected instead of spawning the wrong amount.
 */
class SpawnNpcBatchArgumentTest {

	@Test
	void singleIdKeepsLegacyShape() {
		SpawnNpc.SpawnTarget target = SpawnNpc.parseTarget("210000");
		assertEquals(210000, target.templateId());
		assertEquals(1, target.count());
		assertFalse(target.batch());
	}

	@Test
	void starFormParsesTemplateAndCount() {
		SpawnNpc.SpawnTarget target = SpawnNpc.parseTarget("210000*10");
		assertEquals(210000, target.templateId());
		assertEquals(10, target.count());
		assertTrue(target.batch());
	}

	@Test
	void whitespaceAroundBothPartsIsTolerated() {
		SpawnNpc.SpawnTarget target = SpawnNpc.parseTarget(" 210000 * 10 ");
		assertEquals(210000, target.templateId());
		assertEquals(10, target.count());
		assertTrue(target.batch());
	}

	@Test
	void countBoundsAreEnforced() {
		assertNull(SpawnNpc.parseTarget("210000*0"));
		assertNull(SpawnNpc.parseTarget("210000*-1"));
		assertNull(SpawnNpc.parseTarget("210000*101"));

		assertEquals(100, SpawnNpc.parseTarget("210000*100").count());
	}

	@Test
	void targetRelativeFormDefersTemplateIdToTheSelectedTarget() {
		SpawnNpc.SpawnTarget target = SpawnNpc.parseTarget("*10");
		assertNull(target.templateId());
		assertEquals(10, target.count());
		assertTrue(target.batch());
	}

	@Test
	void targetRelativeFormToleratesWhitespaceAndHonoursBounds() {
		SpawnNpc.SpawnTarget target = SpawnNpc.parseTarget(" * 5 ");
		assertNull(target.templateId());
		assertEquals(5, target.count());
		assertTrue(target.batch());

		assertNull(SpawnNpc.parseTarget("*0"));
		assertNull(SpawnNpc.parseTarget("*101"));
		assertNull(SpawnNpc.parseTarget("*"));
		assertNull(SpawnNpc.parseTarget("*x"));
	}

	@Test
	void malformedTokensAreRejected() {
		assertNull(SpawnNpc.parseTarget(null));
		assertNull(SpawnNpc.parseTarget(""));
		assertNull(SpawnNpc.parseTarget("   "));
		assertNull(SpawnNpc.parseTarget("abc"));
		assertNull(SpawnNpc.parseTarget("210000*"));
		assertNull(SpawnNpc.parseTarget("210000*x"));
		assertNull(SpawnNpc.parseTarget("210000*10*2"));
	}
}
