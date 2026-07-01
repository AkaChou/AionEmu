package com.aionemu.gameserver.skillengine.properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.KnownList;

class TargetRangePropertyTest {

	@Test
	void areaTargetSelectionToleratesKnownListMutationDuringScan() {
		TestCreature effector = new TestCreature(1);
		TestCreature target = new MutatingCreature(2, effector.getKnownList().getKnownObjects());
		TestCreature otherTarget = new TestCreature(3);
		effector.getKnownList().getKnownObjects().put(target.getObjectId(), target);
		effector.getKnownList().getKnownObjects().put(otherTarget.getObjectId(), otherTarget);
		Skill skill = new Skill(new SkillTemplate(), effector, 1, effector, null);
		Properties properties = new Properties();
		properties.targetType = TargetRangeAttribute.AREA;
		properties.targetDistance = 18;

		assertDoesNotThrow(() -> TargetRangeProperty.set(skill, properties));
	}

	private static class TestCreature extends Creature {

		private TestCreature(int objectId) {
			super(objectId, (CreatureController<? extends Creature>) null, null, new TestVisibleObjectTemplate(), position());
			setKnownlist(new KnownList(this));
			setLifeStats(new TestLifeStats(this));
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public int getInstanceId() {
			return 1;
		}

		@Override
		public byte getLevel() {
			return 1;
		}

		private static WorldPosition position() {
			WorldPosition position = new WorldPosition(1);
			position.setXYZH(0f, 0f, 0f, (byte) 0);
			return position;
		}
	}

	private static final class MutatingCreature extends TestCreature {

		private final Map<Integer, VisibleObject> knownObjects;
		private boolean mutated;

		private MutatingCreature(int objectId, Map<Integer, VisibleObject> knownObjects) {
			super(objectId);
			this.knownObjects = knownObjects;
		}

		@Override
		public CreatureLifeStats<? extends Creature> getLifeStats() {
			if (!mutated) {
				mutated = true;
				TestCreature lateArrival = new TestCreature(99);
				knownObjects.put(lateArrival.getObjectId(), lateArrival);
			}
			return super.getLifeStats();
		}
	}

	private static final class TestVisibleObjectTemplate extends VisibleObjectTemplate {

		@Override
		public int getTemplateId() {
			return 1;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}

	private static final class TestLifeStats extends CreatureLifeStats<TestCreature> {

		private TestLifeStats(TestCreature owner) {
			super(owner, 1, 1);
		}

		@Override
		protected void onIncreaseMp(TYPE type, int value, int skillId, LOG log) {
		}

		@Override
		protected void onReduceMp() {
		}

		@Override
		protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		}

		@Override
		protected void onReduceHp() {
		}
	}
}
