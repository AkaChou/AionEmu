package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** 验证影片引用来自真实客户端 CutScenes ID 集，而不是协议整数范围。 / Verifies movie references use real client CutScenes ids, not the protocol range. */
class QuestGraphMovieReferenceCatalogTest {

	@Test
	void buildsExactClientCutsceneClosure() {
		Set<Integer> references = QuestGraphMovieReferenceCatalog.build();

		assertEquals(989, references.size());
		assertTrue(references.containsAll(Set.of(1, 300, 351, 913, 1008, 10001, 11012, 12012, 13002)));
		assertFalse(references.contains(0));
		assertFalse(references.contains(301));
		assertFalse(references.contains(1009));
		assertFalse(references.contains(0xFFFF));
		assertThrows(UnsupportedOperationException.class, () -> references.add(914));
	}
}
