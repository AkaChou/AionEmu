package com.aionemu.gameserver.ai2.manager;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.handler.FollowEventHandler;
import com.aionemu.gameserver.controllers.movement.NpcMoveController;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.NpcGameStats;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.KnownList;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowManagerTest {
	@Test
	void startsMovingAsSoonAsFollowingBegins() {
		TestNpc owner = new ObjenesisStd().newInstance(TestNpc.class);
		owner.setKnownlist(new KnownList(owner));
		owner.setGameStats(new TestNpcGameStats(owner));
		owner.setLifeStats(new TestNpcLifeStats(owner));
		owner.setPosition(position(0));
		RecordingMoveController movement = new RecordingMoveController(owner);
		owner.movement = movement;
		TestNpcAI ai = new TestNpcAI(owner, true);
		TestNpc target = new ObjenesisStd().newInstance(TestNpc.class);
		target.setPosition(position(1));

		FollowEventHandler.follow(ai, target);

		assertTrue(ai.isInState(AIState.FOLLOWING));
		assertSame(target, owner.target);
		assertTrue(movement.started);
	}

	@Test
	void doesNotStartAnImmobileFollower() {
		TestNpc owner = new ObjenesisStd().newInstance(TestNpc.class);
		RecordingMoveController movement = new RecordingMoveController(owner);
		owner.movement = movement;

		assertFalse(FollowManager.startMoving(new TestNpcAI(owner, false)));
		assertFalse(movement.started);
	}

	@Test
	void followingStopsAtTheCloseFollowDistance() {
		TestNpc owner = new ObjenesisStd().newInstance(TestNpc.class);
		owner.setKnownlist(new KnownList(owner));
		owner.setGameStats(new TestNpcGameStats(owner));
		owner.setLifeStats(new TestNpcLifeStats(owner));
		owner.setPosition(position(0));
		TestNpc target = new ObjenesisStd().newInstance(TestNpc.class);
		target.setPosition(position(2));
		owner.setTarget(target);
		TestNpcAI ai = new TestNpcAI(owner, true);
		ai.setStateIfNot(AIState.FOLLOWING);

		assertTrue(ai.poll(AIQuestion.DESTINATION_REACHED));

		target.setPosition(position(4));
		assertFalse(ai.poll(AIQuestion.DESTINATION_REACHED));
	}

	private static WorldPosition position(float x) {
		WorldPosition position = new WorldPosition(110010000);
		position.setXYZH(x, 0f, 0f, (byte) 0);
		return position;
	}

	private static final class TestNpcAI extends NpcAI2 {
		private final Npc owner;
		private final boolean moveSupported;

		private TestNpcAI(Npc owner, boolean moveSupported) {
			this.owner = owner;
			this.moveSupported = moveSupported;
		}

		@Override
		public Npc getOwner() {
			return owner;
		}

		@Override
		public boolean isMoveSupported() {
			return moveSupported;
		}
	}

	private static final class TestNpc extends Npc {
		private NpcMoveController movement;
		private VisibleObject target;

		private TestNpc() {
			super(0, null, null, null);
		}

		@Override
		public NpcMoveController getMoveController() {
			return movement;
		}

		@Override
		public Integer getObjectId() {
			return 1;
		}

		@Override
		public boolean isInInstance() {
			return false;
		}

		@Override
		public int getInstanceId() {
			return 0;
		}

		@Override
		public void setTarget(VisibleObject target) {
			this.target = target;
			super.setTarget(target);
		}
	}

	private static final class RecordingMoveController extends NpcMoveController {
		private boolean started;

		private RecordingMoveController(Npc owner) {
			super(owner);
		}

		@Override
		public synchronized void moveToTargetObject() {
			started = true;
		}
	}

	private static final class TestNpcGameStats extends NpcGameStats {
		private final Npc owner;

		private TestNpcGameStats(Npc owner) {
			super(owner);
			this.owner = owner;
		}

		@Override
		public Stat2 getMaxHp() {
			return new AdditionStat(StatEnum.MAXHP, 100, owner);
		}

		@Override
		public Stat2 getMaxMp() {
			return new AdditionStat(StatEnum.MAXMP, 100, owner);
		}

		@Override
		public Stat2 getAttackSpeed() {
			return new AdditionStat(StatEnum.ATTACK_SPEED, 0, owner);
		}

		@Override
		public float getMovementSpeedFloat() {
			return 0;
		}
	}

	private static final class TestNpcLifeStats extends NpcLifeStats {
		private TestNpcLifeStats(Npc owner) {
			super(owner);
		}
	}
}
