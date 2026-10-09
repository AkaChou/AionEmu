package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.ProgressCamera.Outcome;
import com.aionemu.gameserver.questEngine.tablelane.ProgressCamera.Result;
import com.aionemu.gameserver.questEngine.tablelane.ProgressCamera.Status;

/**
 * {@link ProgressCamera} 门测试：三重守卫、一次事件只加一、超杀零副作用、双通道裁决。
 * Gate tests for {@link ProgressCamera}: the three guards, one-event-plus-one, overkill no-ops and the
 * dual-channel verdict — anchored on P0a retail evidence (quests 1143 / 1842 / 2354, camera-params.tsv).
 */
class ProgressCameraTest {

	private static final RawQuestVarsCodec.Width SIX = RawQuestVarsCodec.Width.SIX;
	private static final RawQuestVarsCodec.Width TEN = RawQuestVarsCodec.Width.TEN;

	@Test
	void normalWriteIncrementsExactlyOne() {
		// 1143：slot1 required=10，fullValue 0xa（fun_773.cpp:13）。首杀 → 1，普通写入。
		// Quest 1143: slot1 required=10, fullValue 0xa (fun_773.cpp:13). First kill -> 1, normal write.
		Result result = ProgressCamera.advance(Status.START, 0, SIX, 1, 10, 0xa, true);
		assertEquals(Outcome.NORMAL_WRITE, result.outcome());
		assertEquals(1, result.newVars());
		assertEquals(1, RawQuestVarsCodec.slotValue(SIX, result.newVars(), 1));
	}

	@Test
	void advanceChannelOnlyOnFullValue() {
		// 2354：{slot1:6, slot2:4}，fullValue 0x106（fun_773.cpp:1003/949）。
		// Quest 2354: {slot1:6, slot2:4}, fullValue 0x106 (fun_773.cpp:1003/949).
		int vars = 0;
		for (int i = 0; i < 5; i++) {
			Result result = ProgressCamera.advance(Status.START, vars, SIX, 1, 6, 0x106, true);
			assertEquals(Outcome.NORMAL_WRITE, result.outcome());
			vars = result.newVars();
		}
		Result slot1Complete = ProgressCamera.advance(Status.START, vars, SIX, 1, 6, 0x106, true);
		assertEquals(Outcome.NORMAL_WRITE, slot1Complete.outcome());
		vars = slot1Complete.newVars();
		for (int i = 0; i < 3; i++) {
			Result slot2 = ProgressCamera.advance(Status.START, vars, SIX, 2, 4, 0x106, true);
			assertEquals(Outcome.NORMAL_WRITE, slot2.outcome());
			vars = slot2.newVars();
		}
		Result finalKill = ProgressCamera.advance(Status.START, vars, SIX, 2, 4, 0x106, true);
		assertEquals(Outcome.ADVANCE_WRITE, finalKill.outcome());
		assertEquals(0x106, finalKill.newVars());
	}

	@Test
	void overkillIsNoAction() {
		// 槽已满：再杀零副作用（原版 (count&mask)<required 守卫）。
		// A full slot: further kills are side-effect free (retail (count&mask)<required guard).
		int full = RawQuestVarsCodec.withSlotValue(SIX, 0, 1, 6);
		Result result = ProgressCamera.advance(Status.START, full, SIX, 1, 6, 0x106, true);
		assertEquals(Outcome.NO_ACTION, result.outcome());
		assertEquals(full, result.newVars());
	}

	@Test
	void nonStartStatusIsNoAction() {
		for (Status status : new Status[] {Status.NONE, Status.SUCCESS, Status.REWARDED, Status.WAITING}) {
			Result result = ProgressCamera.advance(status, 0, SIX, 1, 6, 0x106, true);
			assertEquals(Outcome.NO_ACTION, result.outcome());
			assertEquals(0, result.newVars());
		}
	}

	@Test
	void guardedVarsIsNoAction() {
		Result result = ProgressCamera.advance(Status.START, 0x40000000, SIX, 1, 6, 0x106, true);
		assertEquals(Outcome.NO_ACTION, result.outcome());
	}

	@Test
	void flagFalseForcesNormalChannel() {
		// 原版 2463 个调用点 flag 全为 1；语义上 flag=0 时即使到满值也走普通写入。
		// All 2463 retail call sites pass flag=1; semantically flag=0 keeps full values on the normal channel.
		// 起点 vars=0xc6（槽1:6 已满、槽2:3），最后一杀到 0x106。
		// Start vars=0xc6 (slot1:6 full, slot2:3); the final kill lands on 0x106.
		Result result = ProgressCamera.advance(Status.START, 0xc6, SIX, 2, 4, 0x106, false);
		assertEquals(Outcome.NORMAL_WRITE, result.outcome());
		assertEquals(0x106, result.newVars());
	}

	@Test
	void tenBitMultiSlotRowMatchesRetailEvidence() {
		// 1842（10 位 {80,1}）：槽 1 未满时普通写入；到 80 后槽 2 的一杀即满 → 推进。
		// Quest 1842 (10-bit {80,1}): slot 1 fills via normal writes; the first slot-2 kill at full completes.
		int vars = 0x450 - (1 << 10);
		Result result = ProgressCamera.advance(Status.START, vars, TEN, 2, 1, 0x450, true);
		assertEquals(Outcome.ADVANCE_WRITE, result.outcome());
		assertEquals(0x450, result.newVars());
	}
}
