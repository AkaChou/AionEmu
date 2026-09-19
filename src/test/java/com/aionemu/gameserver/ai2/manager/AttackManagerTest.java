package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

/**
 * {@code AttackManager#targetTooFar} 对不可移动 NPC 的契约闸门。
 * Contract gate for {@code AttackManager#targetTooFar} and immobile NPCs.
 *
 * <p>背景：卵、固定炮台这类 0 移速但有伤害的 NPC 一旦在这里放弃目标，就会清空仇恨；下一次受击或视野事件会立刻把它
 * 拉回战斗，客户端因此反复播放脱战表现（Taloc's Hollow 的 mosqua egg 就是该症状）。真端数据里没有这个驱动
 * （0 移速不会产生寻路失败、{@code max_chase_time=0} 不设追击超时、pattern 在进入战斗时 {@code do_nothing}），
 * 因此该分支必须保持不存在。
 * Background: an immobile but damaging NPC that gives up here clears hate only to be pulled back into combat by the next
 * hit or sight event, which makes the client replay the disengage animation (the mosqua egg symptom). Retail data defines
 * no such driver, so this branch must stay absent.</p>
 */
class AttackManagerTest {

	/** 被测源码路径 / Source under test. */
	private static final Path ATTACK_MANAGER = Path
			.of("src/main/java/com/aionemu/gameserver/ai2/manager/AttackManager.java");

	/**
	 * 不可移动的 NPC 不得因目标够不着而放弃目标。
	 * Immobile NPCs must not give up a target they cannot reach.
	 *
	 * @throws IOException 读取源码失败 / when the source cannot be read
	 */
	@Test
	void immobileNpcsKeepUnreachableTargets() throws IOException {
		String body = methodBody(Files.readString(ATTACK_MANAGER), "public static void targetTooFar(NpcAI2 npcAI)");
		assertTrue(body.contains("isMoveSupported()"),
				"targetTooFar 必须保留“能移动则继续追击”的判断。 / targetTooFar must keep the move-supported chase branch.");
		assertFalse(body.contains("TARGET_GIVEUP"),
				"不可移动的 NPC 不得在 targetTooFar 中放弃目标（会与受击重新仇恨形成脱战抖动）。"
						+ " / Immobile NPCs must not give up in targetTooFar; it oscillates with damage-driven re-aggro.");
	}

	/**
	 * 0 移速 NPC 够不着目标时必须继续重排攻击尝试，而不是让攻击链静默停摆。
	 * Immobile NPCs must keep retrying attacks instead of silently dropping the attack chain.
	 *
	 * <p>同一入口也服务于受击复位（{@code AttackEventHandler#onAttack}）：任何重排都必须离开受击调用栈再执行，
	 * 否则 {@code AggroList#addDamageInternal} → {@code onAttacked} → {@code scheduleNextAttack}
	 * → {@code attackAction} → {@code CreatureController#attackTarget} 会跨生物无限递归直到 {@code StackOverflowError}。
	 * The same entry point also serves the hit-driven resume: every reschedule must leave the hit stack first.</p>
	 *
	 * @throws IOException 读取源码失败 / when the source cannot be read
	 */
	@Test
	void immobileNpcsRetryAttacksInsteadOfDroppingTheChain() throws IOException {
		String source = Files.readString(ATTACK_MANAGER);
		String targetTooFar = methodBody(source, "public static void targetTooFar(NpcAI2 npcAI)");
		assertTrue(targetTooFar.contains("scheduleImmobileRetry(npcAI)"),
				"0 移速 NPC 够不着目标时不得直接结束攻击链（会停在 FIGHT 只保留仇恨、不还手）。"
						+ " / Immobile NPCs must reschedule a retry instead of ending the attack chain.");

		String resume = methodBody(source, "public static void resumeInterruptedAttack(NpcAI2 npcAI)");
		assertTrue(resume.contains("scheduleAttackRetry(npcAI, 0)"),
				"受击复位必须复用同一条异步重排路径，不得自行同步攻击。"
						+ " / The hit-driven resume must reuse the same asynchronous retry path.");

		String retry = methodBody(source, "private static void scheduleAttackRetry(NpcAI2 npcAI, int delay)");
		assertTrue(retry.contains("threadPoolManager().schedule("),
				"重排必须在 AI 线程池上延迟执行，不能在受击调用栈内同步攻击。"
						+ " / Retries must run on the AI thread pool, never synchronously inside the hit stack.");
		assertFalse(retry.contains("scheduleNextAttack(npcAI)"),
				"排入重试的方法自身不得直接攻击，只有线程池任务允许调用 scheduleNextAttack。"
						+ " / The queueing method itself must not attack; only the thread-pool task may call scheduleNextAttack.");
		assertTrue(methodBody(source, "private static void runAttackRetry(NpcAI2 npcAI, int objectId)")
				.contains("scheduleNextAttack(npcAI)"),
				"线程池任务必须真正复位攻击链。 / The thread-pool task must actually resume the attack chain.");
	}

	/**
	 * 攻击链复位按对象 ID 去重：在途任务未结束时不重复入队，任务结束后允许再次入队。
	 * Attack-chain retries are deduplicated per object id: in-flight tasks are not queued twice, finished ones may be.
	 */
	@Test
	void attackRetryDeduplicationOnlySkipsInFlightTasks() {
		assertTrue(AttackManager.shouldQueueAttackRetry(null));
		assertTrue(AttackManager.shouldQueueAttackRetry(CompletableFuture.completedFuture(null)));
		assertFalse(AttackManager.shouldQueueAttackRetry(new CompletableFuture<>()));
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
