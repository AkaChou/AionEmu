package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.ObserveController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;

/**
 * NPC 直接改血的血量同步回归。
 * Regression for HP synchronization on direct NPC HP assignment.
 * <p>覆盖 {@link NpcLifeStats#setCurrentHp(int)} 与 {@link NpcLifeStats#setCurrentHpPercent(int)} 的收敛出口：
 * 只有「已进入世界」且「客户端可见的百分比真的变了」才补发血量同步包，攻击路径的广播不受影响。
 * 夹具不触发任何真实广播：记录子类覆写了包出口，因此测试不依赖连接、KnownList 或 mock 框架。</p>
 * <p>Covers the sync outlet of setCurrentHp and setCurrentHpPercent: a sync packet is emitted only when the
 * creature is spawned and the client-visible percentage actually changed. The fixture triggers no real
 * broadcast — the recording subclass overrides the packet outlet — so no connection, known list or mock
 * framework is involved.</p>
 */
class NpcDirectHpSyncTest {

	@Test
	void unspawnedAssignmentUpdatesStateWithoutAnyPacket() {
		TestNpc npc = npc(false);
		RecordingLifeStats stats = stats(npc);

		stats.setCurrentHpPercent(5);

		// 未进入世界：客户端还没见过该生物，出生包会自带正确百分比。
		// Unspawned: the client has never seen the creature, and the spawn packet carries the correct value.
		assertEquals(5, stats.getCurrentHp(), "state must still change");
		assertTrue(stats.packets.isEmpty(), "no packet may be sent before the creature is in the world");
	}

	@Test
	void spawnedPercentDropSynchronizesNegativeDelta() {
		TestNpc npc = npc(true);
		RecordingLifeStats stats = stats(npc);

		stats.setCurrentHpPercent(5);

		assertEquals(1, stats.packets.size(), "a real percentage change must be synchronized");
		assertEquals(new Packet(TYPE.HP, -95, 0, LOG.REGULAR), stats.packets.getFirst());
	}

	@Test
	void spawnedRestoreSynchronizesPositiveDelta() {
		TestNpc npc = npc(true);
		RecordingLifeStats stats = stats(npc);
		stats.setCurrentHpPercent(40);
		stats.packets.clear();

		stats.setCurrentHp(100);

		assertEquals(1, stats.packets.size());
		assertEquals(new Packet(TYPE.HP, 60, 0, LOG.REGULAR), stats.packets.getFirst());
	}

	@Test
	void identicalPercentageStaysSilent() {
		TestNpc npc = npc(true);
		RecordingLifeStats stats = stats(npc);
		stats.setCurrentHpPercent(50);
		stats.packets.clear();

		stats.setCurrentHpPercent(50);

		assertEquals(0, stats.packets.size(), "an idempotent assignment must not reach the client");
	}

	@Test
	void proportionalRescaleWithUnchangedPercentageStaysSilent() {
		TestNpc npc = npc(true);
		TestNpcGameStats gameStats = (TestNpcGameStats) npc.getGameStats();
		RecordingLifeStats stats = stats(npc);
		stats.setCurrentHpPercent(40);
		stats.packets.clear();

		// 等价于 CreatureGameStats#checkHPStats：最大生命翻倍后走静默重算路径，客户端可见百分比不变。
		// Mirrors CreatureGameStats#checkHPStats: max HP doubles and the rescale takes the silent path, so
		// the client-visible percentage stays the same.
		gameStats.maxHp = 200;
		stats.rescaleCurrentHp(80);

		assertEquals(80, stats.getCurrentHp());
		assertEquals(0, stats.packets.size(), "a proportional rescale must never reach the client");
	}

	@Test
	void subPercentagePointChangeStaysSilent() {
		TestNpc npc = npc(true);
		TestNpcGameStats gameStats = (TestNpcGameStats) npc.getGameStats();
		RecordingLifeStats stats = stats(npc);
		gameStats.maxHp = 1000;
		stats.setCurrentHpPercent(4);
		stats.packets.clear();

		// 40 → 45 截断后仍是同一个整数百分比，血条原样，补包只会平白多一次客户端表现。
		// 40 → 45 truncates to the same integer percentage: the bar does not move, so a packet would only
		// add client-side noise.
		stats.setCurrentHp(45);

		assertEquals(45, stats.getCurrentHp());
		assertEquals(0, stats.packets.size(), "a change below one percentage point is invisible to the client");
	}

	@Test
	void spawnedAssignmentWithoutAnyChangeStaysSilent() {
		TestNpc npc = npc(true);
		RecordingLifeStats stats = stats(npc);

		stats.setCurrentHp(100);

		assertEquals(0, stats.packets.size(), "assigning the current value is not a change");
	}

	@Test
	void effectHpReductionSynchronizesToNearbyPlayers() {
		TestNpc npc = npc(true);
		RecordingLifeStats stats = stats(npc);

		stats.reduceHpFromEffect(30, npc);

		assertEquals(70, stats.getCurrentHp());
		assertEquals(1, stats.packets.size());
		assertEquals(new Packet(TYPE.HP, -30, 0, LOG.REGULAR), stats.packets.getFirst());
	}

	@Test
	void effectHpReductionBeforeSpawnStaysSilent() {
		TestNpc npc = npc(false);
		RecordingLifeStats stats = stats(npc);

		stats.reduceHpFromEffect(30, npc);

		assertEquals(70, stats.getCurrentHp(), "state must still change");
		assertEquals(0, stats.packets.size(), "no packet before the creature is in the world");
	}

	@Test
	void subPercentagePointEffectReductionStaysSilent() {
		TestNpc npc = npc(true);
		TestNpcGameStats gameStats = (TestNpcGameStats) npc.getGameStats();
		RecordingLifeStats stats = stats(npc);
		gameStats.maxHp = 1000;
		stats.setCurrentHp(505);
		stats.packets.clear();

		// 505 → 501 截断后仍是 50%：周期扣血尤其需要这道闸门，否则每个 tick 都会发包。
		// 505 → 501 truncates to the same 50%: a periodic drain especially needs this gate.
		stats.reduceHpFromEffect(4, npc);

		assertEquals(501, stats.getCurrentHp());
		assertEquals(0, stats.packets.size(), "a drain below one percentage point is invisible to the client");
	}

	@Test
	void lethalEffectHpReductionStillSynchronizes() {
		TestNpc npc = npc(true);
		RecordingLifeStats stats = stats(npc);

		// 请求值超过当前生命：实际损失量才是该下发的差值。
		// The request exceeds the current HP: the actual loss is the delta that must be sent.
		stats.reduceHpFromEffect(150, npc);

		assertTrue(stats.isAlreadyDead());
		assertEquals(0, stats.getCurrentHp());
		assertEquals(1, stats.packets.size(), "the lethal frame still synchronizes, exactly like the attack path");
		assertEquals(new Packet(TYPE.HP, -100, 0, LOG.REGULAR), stats.packets.getFirst());
		assertEquals(1, ((RecordingNpcController) npc.getController()).deaths);
	}

	@Test
	void effectHpReductionAfterDeathStaysSilent() {
		TestNpc npc = npc(true);
		RecordingLifeStats stats = stats(npc);
		stats.reduceHpFromEffect(150, npc);
		stats.packets.clear();

		stats.reduceHpFromEffect(10, npc);

		assertEquals(0, stats.packets.size(), "an already dead creature has nothing to synchronize");
	}

	private static TestNpc npc(boolean spawned) {
		TestNpc npc = new ObjenesisStd().newInstance(TestNpc.class);
		npc.spawned = spawned;
		npc.setGameStats(new TestNpcGameStats(npc, 100));
		npc.setLifeStats(new RecordingLifeStats(npc));
		npc.observeController = new ObserveController();
		npc.controller = new RecordingNpcController();
		return npc;
	}

	private static RecordingLifeStats stats(TestNpc npc) {
		return (RecordingLifeStats) npc.getLifeStats();
	}

	/** 记录了包出口的生命属性，不触发任何真实广播。 / Life stats recording the packet outlet instead of broadcasting. */
	private static final class RecordingLifeStats extends NpcLifeStats {

		private final List<Packet> packets = new ArrayList<>();

		private RecordingLifeStats(Npc owner) {
			super(owner);
		}

		@Override
		protected void sendAttackStatusPacketUpdate(TYPE type, int value, int skillId, LOG log) {
			packets.add(new Packet(type, value, skillId, log));
		}
	}

	private record Packet(TYPE type, int value, int skillId, LOG log) {}

	/** 可切换出生态与最大生命的 NPC 桩。 / NPC stub with a switchable spawn state and max HP. */
	private static final class TestNpc extends Npc {

		private boolean spawned;
		private ObserveController observeController;
		private NpcController controller;

		private TestNpc() {
			super(0, null, null, null);
		}

		@Override
		public boolean isSpawned() {
			return spawned;
		}

		@Override
		public ObserveController getObserveController() {
			return observeController;
		}

		@Override
		public NpcController getController() {
			return controller;
		}
	}

	/** 只记录死亡回调的控制器。 / Controller recording death callbacks only. */
	private static final class RecordingNpcController extends NpcController {

		private int deaths;

		@Override
		public void onDie(Creature lastAttacker) {
			deaths++;
		}
	}

	private static final class TestNpcGameStats extends NpcGameStats {

		private final Npc owner;
		private int maxHp;

		private TestNpcGameStats(Npc owner, int maxHp) {
			super(owner);
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
