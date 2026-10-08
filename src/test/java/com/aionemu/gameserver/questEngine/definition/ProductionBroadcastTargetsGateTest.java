package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生产目录全量广播目标门禁：目录内每一条 {@code broadcast-zone-mission-end} 动作，目标都必须是
 * 具备 {@code zone-mission-end} 路由的 typed 可执行定义，且绝不含完成方自身（QE-015 口径）。
 * <p>
 * 为什么需要它：2026-09-15 修复 10031 自含目标时只做了一次性人工目录审计，未固化为门禁；
 * 2026-09-29 批量物理退役（9321e7663）把 10526/20526/20032/20033/20034 翻成 native 车道后，
 * 10520/20520/20031 的广播清单仍引用无路由目标，每次领奖记 AFTER_COMMIT 失败，直到实机踩中。
 * 本门禁把「广播目标必须真实可路由」变成常驻断言：任何 owner 翻转/退役批次漏改广播清单，这里即红。
 * <p>
 * Production-directory broadcast-target gate: every {@code broadcast-zone-mission-end} action in the
 * catalog must target only executable typed definitions that own a {@code zone-mission-end} route and
 * must never include the completing quest itself (the QE-015 contract).
 * <p>
 * Why it exists: the 2026-09-15 10031 self-target fix ran a one-off manual directory audit that was
 * never hardened into a gate; the 2026-09-29 physical retirement batch (9321e7663) flipped
 * 10526/20526/20032/20033/20034 to the native lanes, leaving 10520/20520/20031 broadcasting to
 * unroutable targets — every claim logged AFTER_COMMIT failures until a player hit it live. This gate
 * turns "broadcast targets must be truly routable" into a standing assertion: any owner-flip or
 * retirement batch that misses a broadcast list goes red here.
 */
class ProductionBroadcastTargetsGateTest {

	/**
	 * 广播动作总量锚：目录当前实测 14 条（天/魔英吉森链 + 亚斯莫 20031 + 深渊 140xx/240xx 族）。
	 * 低于下限即拦「清点面塌空壳」；目录广播结构变化后随实测收紧。
	 * The broadcast-action floor: 14 observed today (Inggison chains, Asmodian 20031, Abyss 140xx/240xx
	 * families). A lower count means the sweep went hollow; re-tighten with the measured value.
	 */
	private static final int MIN_BROADCAST_ACTIONS = 10;

	@Test
	void everyBroadcastZoneMissionEndTargetIsAnExecutableRoutedTypedDefinition() {
		Map<Integer, QuestCatalogEntry> entriesById = ProductionQuestDefinitions.catalog().entries().stream()
			.collect(Collectors.toMap(QuestCatalogEntry::id, Function.identity()));
		int[] broadcastCount = {0};
		List<String> violations = new ArrayList<>();
		for (QuestCatalogEntry entry : ProductionQuestDefinitions.catalog().entries()) {
			if (entry.executable().isEmpty()) {
				continue;
			}
			int sourceQuestId = entry.id();
			entry.executable().get().definition().transitions().stream()
				.flatMap(transition -> transition.afterCommit().stream())
				.filter(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance)
				.map(AfterCommitAction.BroadcastZoneMissionEnd.class::cast)
				.forEach(broadcast -> {
					broadcastCount[0]++;
					for (int targetId : broadcast.questIds()) {
						// 自含目标：完成方自己没有 zone-mission-end 路由时必然整次广播硬失败（QE-015 代表案例）。
						// Self-target: the completing owner has no route of its own, so the whole broadcast fails.
						if (targetId == sourceQuestId) {
							violations.add("quest " + sourceQuestId + " broadcasts to itself");
							continue;
						}
						QuestCatalogEntry target = entriesById.get(targetId);
						// 目标必须是 typed 可执行定义；已退役/native 行的命中由链式发放面承担，不得入列。
						// Targets must be executable typed definitions; retired/native rows are carried by
						// the chain-acquire grant face and must never be listed.
						if (target == null || target.executable().isEmpty()) {
							violations.add("quest " + sourceQuestId + " broadcasts to non-executable target "
								+ targetId);
							continue;
						}
						boolean routed = target.executable().get().definition().transitions().stream()
							.anyMatch(transition -> transition.event() instanceof QuestEvent.ZoneMissionEnd);
						if (!routed) {
							violations.add("quest " + sourceQuestId + " broadcasts to unroutable target "
								+ targetId + " (no zone-mission-end transition)");
						}
					}
				});
		}
		assertTrue(violations.isEmpty(), () -> "广播目标门禁违规：" + String.join("; ", violations));
		assertFalse(broadcastCount[0] < MIN_BROADCAST_ACTIONS,
			() -> "生产目录广播动作数量异常：" + broadcastCount[0] + "（下限 " + MIN_BROADCAST_ACTIONS
				+ "），清点面塌空壳");
	}
}
