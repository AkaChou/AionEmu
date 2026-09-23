package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.ObserveController;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;

/**
 * 召唤物非攻击扣血的双通道同步回归。
 * Regression for the dual-channel synchronization of non-attack HP reduction on summons.
 * <p>召唤物有两条客户端通道：血条走 {@code SM_ATTACK_STATUS} 广播给能看到它的玩家，主人的召唤面板消费的是
 * 绝对生命值、必须单独下发 {@code SM_SUMMON_UPDATE}。夹具覆写了这两个出口，因此测试不依赖连接或 mock 框架。</p>
 * <p>A summon has two client channels: the bar travels through SM_ATTACK_STATUS to every player that can see it,
 * while the master's summon panel consumes absolute HP and needs its own SM_SUMMON_UPDATE. The fixture overrides
 * both outlets, so no connection or mock framework is involved.</p>
 */
class SummonLifeStatsTest {

	@Test
	void effectHpReductionSynchronizesBarAndPanel() {
		TestSummon summon = summon();
		RecordingSummonLifeStats stats = stats(summon);

		stats.reduceHpFromEffect(30, summon);

		assertEquals(70, stats.getCurrentHp());
		assertEquals(1, stats.packets.size());
		assertEquals(new Packet(TYPE.HP, -30, 0, LOG.REGULAR), stats.packets.getFirst());
		assertEquals(1, stats.panelUpdates, "the panel consumes absolute HP and needs its own update");
	}

	@Test
	void subPercentagePointChangeStaysSilentOnBothChannels() {
		TestSummon summon = summon(1000);
		RecordingSummonLifeStats stats = stats(summon);
		stats.setCurrentHp(505);
		stats.packets.clear();

		// 505 → 501 截断后仍是 50%，血条与面板都看不出变化。
		// 505 → 501 truncates to the same 50%, invisible on both the bar and the panel.
		stats.reduceHpFromEffect(4, summon);

		assertEquals(501, stats.getCurrentHp());
		assertEquals(0, stats.packets.size());
		assertEquals(0, stats.panelUpdates);
	}

	@Test
	void panelUpdateIsSkippedWhenTheMasterIsGone() {
		// 主人引用会被召唤释放流程置空：真实实现必须判空返回，不得 NPE 也不得触网。
		// The master reference is cleared by the summon-release flow: the real seam must return safely.
		TestSummon summon = summon();
		RecordingSummonLifeStats stats = stats(summon);
		stats.delegatePanelUpdate = true;

		assertDoesNotThrow(() -> stats.reduceHpFromEffect(30, summon));
	}

	private static TestSummon summon() {
		return summon(100);
	}

	private static TestSummon summon(int maxHp) {
		TestSummon summon = new ObjenesisStd().newInstance(TestSummon.class);
		summon.setGameStats(new TestSummonGameStats(summon, maxHp));
		summon.setLifeStats(new RecordingSummonLifeStats(summon));
		summon.observeController = new ObserveController();
		return summon;
	}

	private static RecordingSummonLifeStats stats(TestSummon summon) {
		return (RecordingSummonLifeStats) summon.getLifeStats();
	}

	/** 记录两条出口的召唤物生命属性，不触发任何真实广播。 / Records both outlets without broadcasting. */
	private static final class RecordingSummonLifeStats extends SummonLifeStats {

		private final List<Packet> packets = new ArrayList<>();
		private int panelUpdates;
		private boolean delegatePanelUpdate;

		private RecordingSummonLifeStats(Summon owner) {
			super(owner);
		}

		@Override
		protected void sendAttackStatusPacketUpdate(TYPE type, int value, int skillId, LOG log) {
			packets.add(new Packet(type, value, skillId, log));
		}

		@Override
		protected void sendSummonPanelUpdate() {
			if (delegatePanelUpdate) {
				super.sendSummonPanelUpdate();
				return;
			}
			panelUpdates++;
		}
	}

	private record Packet(TYPE type, int value, int skillId, LOG log) {}

	/** 主人缺席、观察控制器可注入的召唤物桩。 / Summon stub with an absent master and an injectable observe controller. */
	private static final class TestSummon extends Summon {

		private ObserveController observeController;

		/**
		 * 仅为满足超类构造契约而存在；Summon 的真实构造器会解引用模板与静态数据，Objenesis 从执行它。
		 * Exists only to satisfy the superclass contract; the real constructor dereferences templates and static
		 * data, which Objenesis never runs.
		 */
		private TestSummon() {
			super(0, null, null, null, (byte) 0, 0);
		}

		@Override
		public Player getMaster() {
			// 召唤释放流程会把主人引用置空，此处固定覆盖该情形。
			// The summon-release flow clears the master reference; this stub pins that case.
			return null;
		}

		@Override
		public ObserveController getObserveController() {
			return observeController;
		}
	}

	private static final class TestSummonGameStats extends SummonGameStats {

		private final Summon owner;
		private final int maxHp;

		private TestSummonGameStats(Summon owner, int maxHp) {
			super(owner, null);
			this.owner = owner;
			this.maxHp = maxHp;
		}

		@Override
		public Stat2 getMaxHp() {
			return new AdditionStat(StatEnum.MAXHP, maxHp, owner);
		}

		@Override
		public Stat2 getMaxMp() {
			return new AdditionStat(StatEnum.MAXMP, maxHp, owner);
		}
	}
}
