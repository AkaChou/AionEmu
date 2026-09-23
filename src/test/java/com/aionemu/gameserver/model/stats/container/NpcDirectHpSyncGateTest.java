package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * NPC 与召唤物的血量同步出口结构门禁。
 * Structural gate for the NPC and summon HP sync outlets.
 * <p>锁住四条不变式：NPC 的四个覆写（两个直接改血、一个静默重算、一个非攻击扣血）都声明在子类；
 * 召唤物有自己的出口并同时下发血条与主人面板；攻击与死亡路径不得进入同步入口；AI 不得再自行拼血量包。</p>
 * <p>Locks four invariants: the NPC overrides live in the subclass, summons own an outlet covering both the bar
 * and the master panel, the attack and death paths never enter the sync entry, and no AI hand-builds packets.</p>
 */
class NpcDirectHpSyncGateTest {

	private static final Path STATS_ROOT = Path.of("src/main/java/com/aionemu/gameserver/model/stats/container");

	private static final Path RETURNING_EVENT_HANDLER =
		Path.of("src/main/java/com/aionemu/gameserver/ai2/handler/ReturningEventHandler.java");

	@Test
	void npcLifeStatsOwnsEveryHpSyncOutlet() {
		// 四个覆写共同构成 NPC 的血量同步出口：两个收敛直接改血，一个让等比重算保持静默，
		// 一个收敛非攻击扣血。一旦被删，血条会退回「停在旧值直到下一次攻击」。
		// The four overrides form the NPC HP sync outlet: two funnel direct assignments, one keeps a proportional
		// rescale silent, and one funnels non-attack HP reduction. Removing either brings back a stale bar.
		assertDoesNotThrow(() -> NpcLifeStats.class.getDeclaredMethod("setCurrentHp", int.class));
		assertDoesNotThrow(() -> NpcLifeStats.class.getDeclaredMethod("setCurrentHpPercent", int.class));
		assertDoesNotThrow(() -> NpcLifeStats.class.getDeclaredMethod("rescaleCurrentHp", int.class));
		assertDoesNotThrow(() -> NpcLifeStats.class.getDeclaredMethod("reduceHpFromEffect", int.class, Creature.class));
	}

	@Test
	void maxHpRescaleGoesThroughTheSilentPath() throws IOException {
		String source = Files.readString(STATS_ROOT.resolve("CreatureGameStats.java"));

		// 最大生命变化后的等比重算必须走静默出口；改回 setCurrentHp 会让全实例重算重新产生假飘字。
		// A max-HP rescale must take the silent outlet; reverting to setCurrentHp would bring back the fake
		// floats of a whole-instance rescale.
		assertTrue(methodBody(source, "private void checkHPStats()").contains("rescaleCurrentHp("),
			"the max-HP rescale must stay on the silent path");
	}

	@Test
	void effectHpReductionFlowsThroughTheSyncEntry() throws IOException {
		// 三个技能侧调用点必须走同步入口；"reduceHpFromEffect(" 与 ".reduceHp(" 的前缀差异让两者可精确区分。
		// The three skill-side call sites must use the sync entry; the prefix difference between
		// "reduceHpFromEffect(" and ".reduceHp(" keeps the two distinguishable.
		for (String file : List.of("skillengine/effect/AbstractHealEffect.java", "skillengine/action/HpUseAction.java",
			"skillengine/periodicaction/HpUsePeriodicAction.java")) {
			String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/" + file));
			assertTrue(source.contains("reduceHpFromEffect("), file + " must use the sync entry");
			assertFalse(source.contains(".reduceHp("), file + " must not bypass it with a raw reduceHp");
		}

		String controller = Files.readString(
			Path.of("src/main/java/com/aionemu/gameserver/controllers/CreatureController.java"));
		assertFalse(controller.contains("reduceHpFromEffect"),
			"the attack and death paths must keep broadcasting through their own channels");
	}

	@Test
	void summonsHaveTheirOwnOutletWithBothChannels() throws IOException {
		String source = Files.readString(STATS_ROOT.resolve("SummonLifeStats.java"));

		// 召唤物有两条客户端通道：血条走广播，主人面板消费的是绝对生命值，必须各自下发。
		// A summon has two client channels: the bar is broadcast while the master panel consumes absolute HP.
		assertDoesNotThrow(() -> SummonLifeStats.class.getDeclaredMethod("reduceHpFromEffect", int.class, Creature.class));
		assertTrue(source.contains("sendSummonPanelUpdate()"), "the master panel needs its own update");
	}

	@Test
	void blastRadiusNeverReachesPlayersOrSummons() {
		// 爆炸半径收口：玩家与召唤物直接继承 CreatureLifeStats；召唤物后来有了自己的出口，
		// 但任何层级都不得继承 NPC 的出口（判据与客户端通道不同）。整条继承链检查而非只看直接父类。
		// Blast-radius containment: players and summons extend CreatureLifeStats directly. Summons have gained
		// their own outlet, but must never inherit the NPC one at any level of the hierarchy.
		assertDoesNotExtendNpcOutlet(PlayerLifeStats.class);
		assertDoesNotExtendNpcOutlet(SummonLifeStats.class);
	}

	@Test
	void reduceHpStaysSilentSoTheAttackPathNeverDoubleBroadcasts() throws IOException {
		String source = Files.readString(STATS_ROOT.resolve("NpcLifeStats.java"));

		// 攻击路径已由 NpcController#onAttack 广播，reduceHp 回调里再发会形成双包双飘字。
		// The attack path already broadcasts via NpcController#onAttack; emitting here would double it.
		assertFalse(methodBody(source, "protected void onReduceHp()").contains("sendAttackStatusPacketUpdate"),
			"onReduceHp must stay silent to keep a single broadcast source on the attack path");
	}

	@Test
	void returningEventHandlerDelegatesHpSyncToTheOutlet() throws IOException {
		String source = Files.readString(RETURNING_EVENT_HANDLER);

		assertFalse(source.contains("new SM_ATTACK_STATUS"),
			"the AI must delegate HP sync to the outlet instead of hand-building packets");
		assertTrue(source.contains("notifyLifeChangedObservers"),
			"the observer notice is outside the sync outlet and must be kept");
	}

	/** 沿继承链断言该类型不是 NPC 血量出口的子类。 / Asserts the type is not an NPC HP-outlet subtype at any level. */
	private static void assertDoesNotExtendNpcOutlet(Class<?> type) {
		for (Class<?> ancestor = type.getSuperclass(); ancestor != null; ancestor = ancestor.getSuperclass()) {
			assertNotEquals(NpcLifeStats.class, ancestor, type.getSimpleName() + " must not extend the NPC HP outlet");
		}
	}

	private static String methodBody(String source, String signature) {
		int start = source.indexOf(signature);
		assertTrue(start >= 0, signature + " must exist");
		int brace = source.indexOf('{', start);
		assertTrue(brace >= 0, signature + " must have a body");
		int depth = 0;
		for (int i = brace; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(brace + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " was not closed");
	}
}
