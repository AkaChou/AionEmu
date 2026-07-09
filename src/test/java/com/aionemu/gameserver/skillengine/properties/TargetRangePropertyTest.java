package com.aionemu.gameserver.skillengine.properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.model.templates.BoundRadius;
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

	@Test
	void pointTargetSelectionIncludesCandidateCollisionRadius() {
		boolean originalGeoEnabled = GeoDataConfig.GEO_ENABLE;
		try {
			GeoDataConfig.GEO_ENABLE = false;
			TestCreature effector = new TestCreature(1);
			TestCreature edgeTarget = new TestCreature(2, 6.5f, 0f, 0f, 2f);
			effector.getKnownList().getKnownObjects().put(edgeTarget.getObjectId(), edgeTarget);
			Skill skill = new Skill(new SkillTemplate(), effector, 1, effector, null);
			skill.setTargetPosition(0f, 0f, 0f, (byte) 0);
			Properties properties = new Properties();
			properties.targetType = TargetRangeAttribute.POINT;
			properties.targetDistance = 5;

			TargetRangeProperty.set(skill, properties);

			assertTrue(skill.getEffectedList().contains(edgeTarget));
		} finally {
			GeoDataConfig.GEO_ENABLE = originalGeoEnabled;
		}
	}

	@Test
	void areaPointSkillSelectionIncludesCandidateCollisionRadius() {
		boolean originalGeoEnabled = GeoDataConfig.GEO_ENABLE;
		try {
			GeoDataConfig.GEO_ENABLE = false;
			TestCreature effector = new TestCreature(1);
			TestCreature edgeTarget = new TestCreature(2, 6.5f, 0f, 0f, 2f);
			effector.getKnownList().getKnownObjects().put(edgeTarget.getObjectId(), edgeTarget);
			Skill skill = new Skill(new SkillTemplate(), effector, 1, effector, null);
			skill.setFirstTargetAttribute(FirstTargetAttribute.POINT);
			skill.setFirstTarget(effector);
			skill.setTargetPosition(0f, 0f, 0f, (byte) 0);
			Properties properties = new Properties();
			properties.targetType = TargetRangeAttribute.AREA;
			properties.targetDistance = 5;

			TargetRangeProperty.set(skill, properties);

			assertTrue(skill.getEffectedList().contains(edgeTarget));
		} finally {
			GeoDataConfig.GEO_ENABLE = originalGeoEnabled;
		}
	}

	private static class TestCreature extends Creature {

		private TestCreature(int objectId) {
			this(objectId, 0f, 0f, 0f, 0f);
		}

		private TestCreature(int objectId, float x, float y, float z, float collision) {
			super(objectId, (CreatureController<? extends Creature>) null, null,
					new TestVisibleObjectTemplate(collision), position(x, y, z));
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

		private static WorldPosition position(float x, float y, float z) {
			WorldPosition position = new WorldPosition(1);
			position.setXYZH(x, y, z, (byte) 0);
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

		private final BoundRadius boundRadius;

		private TestVisibleObjectTemplate(float collision) {
			boundRadius = new BoundRadius(collision, collision, 0f);
		}

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

		@Override
		public BoundRadius getBoundRadius() {
			return boundRadius;
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
