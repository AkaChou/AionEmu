package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * {@link CameraRegistry} 门测试：原版锚点行 + 四类 fail-fast 负例（计划 §6.3）。
 * Gate tests for {@link CameraRegistry}: retail-evidence rows plus the four fail-fast negatives (plan §6.3).
 * <p>
 * 锚点（P0a camera-params.tsv，带文件:行号）：1143 = 6位{1:10}/0xa；1842 = 10位{1:80,2:1}/0x450；
 * 2354 = 6位{1:6,2:4}/0x106。
 */
class CameraRegistryTest {

	private static CameraRegistry.RowSpec spec(int questId, RawQuestVarsCodec.Width width, int fullValue,
			Map<Integer, Integer> slots) {
		return new CameraRegistry.RowSpec(questId, width, fullValue, slots);
	}

	@Test
	void buildsAndQueriesRetailEvidenceRows() {
		CameraRegistry registry = CameraRegistry.fromSpecs(java.util.List.of(
				spec(1143, RawQuestVarsCodec.Width.SIX, 0xa, Map.of(1, 10)),
				spec(1842, RawQuestVarsCodec.Width.TEN, 0x450, Map.of(1, 80, 2, 1)),
				spec(2354, RawQuestVarsCodec.Width.SIX, 0x106, Map.of(1, 6, 2, 4))));
		assertEquals(3, registry.size());
		assertEquals(10, registry.require(1143).required(1));
		assertEquals(80, registry.require(1842).required(1));
		assertEquals(1, registry.require(1842).required(2));
		assertEquals(0, registry.require(1842).required(3));
		assertTrue(registry.find(999999).isEmpty());
		assertEquals(RawQuestVarsCodec.Width.TEN, registry.require(1842).width());
	}

	@Test
	void rejectsDuplicateQuestRegistration() {
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> CameraRegistry.fromSpecs(
				java.util.List.of(spec(1143, RawQuestVarsCodec.Width.SIX, 0xa, Map.of(1, 10)),
						spec(1143, RawQuestVarsCodec.Width.SIX, 0xa, Map.of(1, 10)))));
		assertTrue(e.getMessage().startsWith("NATIVE_CAMERA_WIDTH_MIXED"));
	}

	@Test
	void rejectsRequiredOverMask() {
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> CameraRegistry.fromSpecs(java.util.List.of(spec(1, RawQuestVarsCodec.Width.SIX, 100, Map.of(1, 100)))));
		assertTrue(e.getMessage().startsWith("NATIVE_CAMERA_REQUIRED_EXCEEDS_MASK"));
	}

	@Test
	void rejectsFullValueMismatch() {
		// 原版自身分歧形态（13912/23912：fullValue 含未声明槽）必须由数据层显式登记，本类直接拒绝。
		// The retail-side divergence shape (13912/23912: fullValue claims an undeclared slot) must be
		// registered explicitly by the data layer; the registry refuses it.
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> CameraRegistry
			.fromSpecs(java.util.List.of(spec(13912, RawQuestVarsCodec.Width.SIX, 0x1001, Map.of(1, 1)))));
		assertTrue(e.getMessage().startsWith("NATIVE_CAMERA_FULL_VALUE_INVALID"));
	}

	@Test
	void rejectsEmptyAndNonPositiveSpecs() {
		assertThrows(IllegalStateException.class,
				() -> CameraRegistry.fromSpecs(java.util.List.of(spec(1, RawQuestVarsCodec.Width.SIX, 0, Map.of()))));
		assertThrows(IllegalStateException.class, () -> CameraRegistry
			.fromSpecs(java.util.List.of(spec(1, RawQuestVarsCodec.Width.SIX, 0, Map.of(1, 0)))));
		assertThrows(IllegalStateException.class, () -> CameraRegistry
			.fromSpecs(java.util.List.of(new CameraRegistry.RowSpec(1, null, 0, Map.of(1, 1)))));
	}
}
