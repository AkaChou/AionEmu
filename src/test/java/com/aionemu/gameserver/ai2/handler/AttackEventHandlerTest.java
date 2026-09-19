package com.aionemu.gameserver.ai2.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import org.junit.jupiter.api.Test;

class AttackEventHandlerTest {

	/** 被测源码路径 / Source under test. */
	private static final Path ATTACK_EVENT_HANDLER = Path
			.of("src/main/java/com/aionemu/gameserver/ai2/handler/AttackEventHandler.java");

	@Test
	void onlyFirstAttackEventEntersFight() {
		NpcAI2 ai = new NpcAI2();

		assertTrue(AttackEventHandler.tryEnterFight(ai));
		assertFalse(AttackEventHandler.tryEnterFight(ai));
	}

	@Test
	void attackDoesNotRestartAnActiveReturnState() {
		NpcAI2 ai = new NpcAI2();
		ai.setStateIfNot(AIState.RETURNING);

		assertFalse(AttackEventHandler.tryEnterFight(ai));
		assertTrue(ai.isInState(AIState.RETURNING));
	}

	@Test
	void attackActivityCancelsPendingLostTargetGiveup() {
		NpcAI2 ai = new NpcAI2();
		ai.setStateIfNot(AIState.FIGHT);
		ai.setSubStateIfNot(AISubState.TARGET_LOST);

		assertFalse(AttackEventHandler.tryEnterFight(ai));
		assertTrue(ai.isInSubState(AISubState.NONE));
	}

	/**
	 * 已在 FIGHT 时再次受击必须重排攻击链，而不是把这次受击吞掉。
	 * A hit that lands while already fighting must reschedule the attack chain instead of being swallowed.
	 *
	 * <p>背景：0 移速 NPC 在 {@code AttackManager#targetTooFar} 里既不能追击、也不会重排下一次攻击，攻击链
	 * 就此静默停摆；此后每次受击都会因为“已经是 FIGHT”被 {@code tryEnterFight} 拒绝，NPC 只剩仇恨站桩。
	 * Background: a zero-speed NPC can neither chase nor reschedule in {@code AttackManager#targetTooFar}, so its
	 * attack chain stops silently; afterwards {@code tryEnterFight} rejects every new hit because the AI is already
	 * in FIGHT, leaving the NPC holding hate without retaliating.</p>
	 *
	 * <p>回归约束：这里的复位必须异步。受击调用栈是 {@code AggroList#addDamageInternal} → {@code AbstractAI#onAttacked}
	 * → 本方法，若同步调用 {@code AttackManager#scheduleNextAttack}，普攻会顺着
	 * {@code SimpleAttackManager#attackAction} → {@code CreatureController#attackTarget} 打回对方并再次进入本方法；
	 * 双方普攻间隔为 0 时会来回无限递归，最终以 {@code StackOverflowError} 打死线程池线程（2026-09-19 线上日志复现）。
	 * Regression constraint: the resume must be asynchronous. The hit stack arrives from
	 * {@code AggroList#addDamageInternal} via {@code AbstractAI#onAttacked}; a synchronous
	 * {@code scheduleNextAttack} attacks back through {@code CreatureController#attackTarget} and re-enters this
	 * handler, so two creatures with a zero attack interval recurse until {@code StackOverflowError}.</p>
	 *
	 * @throws IOException 读取源码失败 / when the source cannot be read
	 */
	@Test
	void alreadyFightingHitResumesInterruptedAttackChain() throws IOException {
		String body = methodBody(Files.readString(ATTACK_EVENT_HANDLER),
				"public static void onAttack(NpcAI2 npcAI, Creature creature)");

		assertTrue(body.contains("else if (npcAI.isInState(AIState.FIGHT))"),
				"受击时若已在 FIGHT，必须走重排攻击链的分支。 / A hit while already fighting must take the resume branch.");
		assertTrue(body.contains("AttackManager.resumeInterruptedAttack(npcAI)"),
				"已在 FIGHT 的受击分支必须通过异步去重入口复位攻击链，否则 NPC 停摆后永远不还手。"
						+ " / The resume branch must go through the asynchronous, deduplicated resume entry point.");
		assertFalse(body.contains("AttackManager.scheduleNextAttack("),
				"受击处理严禁同步重排攻击：onAttacked 调用栈内同步执行普攻会跨生物递归直到 StackOverflowError。"
						+ " / The hit handler must never reschedule synchronously; it recurses across creatures until StackOverflowError.");
	}

	/**
	 * 按大括号配对提取指定方法体的源码片段。
	 * Extracts a method body by brace matching.
	 *
	 * @param source 源码 / source
	 * @param signature 方法签名 / method signature
	 * @return 方法体源码；找不到时返回空串（使闸门失败） / the method body, or an empty string when missing
	 */
	private static String methodBody(String source, String signature) {
		int start = source.indexOf(signature);
		if (start < 0) {
			return "";
		}
		int open = source.indexOf('{', start);
		if (open < 0) {
			return "";
		}
		int depth = 0;
		for (int i = open; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}' && --depth == 0) {
				return source.substring(open, i + 1);
			}
		}
		return "";
	}
}
