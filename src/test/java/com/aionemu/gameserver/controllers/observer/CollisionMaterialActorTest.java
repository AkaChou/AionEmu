package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.persistence.dao.impl.ServerVariableDaoImpl;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.observer.AbstractCollisionObserver.CheckType;
import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.materials.MaterialSkill;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldPosition;

class CollisionMaterialActorTest {

	@Test
	void restartsUnchangedMaterialAfterSharedTaskWasCancelled() throws Exception {
		initializeGameTimeDao();
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new TestCreature(controller, emptyRegion());
		CollisionMaterialActor actor = new CollisionMaterialActor(creature, new Node("test-material"), materialTemplate());

		try {
			actor.act();
			Future<?> firstTask = controller.getTask(TaskId.ZONE_MATERIAL_ACTION);
			assertNotNull(firstTask);
			controller.cancelTask(TaskId.ZONE_MATERIAL_ACTION);

			actor.act();

			Future<?> restartedTask = controller.getTask(TaskId.ZONE_MATERIAL_ACTION);
			assertNotNull(restartedTask);
			assertNotSame(firstTask, restartedTask);
		} finally {
			actor.abort();
		}
	}

	@Test
	void leavingOldActorDoesNotCancelReplacementTask() throws Exception {
		initializeGameTimeDao();
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new TestCreature(controller, emptyRegion());
		CollisionMaterialActor oldActor = new CollisionMaterialActor(creature, new Node("old-material"), materialTemplate());
		CollisionMaterialActor currentActor = new CollisionMaterialActor(creature, new Node("current-material"), materialTemplate());

		try {
			oldActor.act();
			Future<?> oldTask = controller.getTask(TaskId.ZONE_MATERIAL_ACTION);
			currentActor.act();
			Future<?> replacementTask = controller.getTask(TaskId.ZONE_MATERIAL_ACTION);
			assertNotSame(oldTask, replacementTask);

			oldActor.abort();

			assertSame(replacementTask, controller.getTask(TaskId.ZONE_MATERIAL_ACTION));
		} finally {
			oldActor.abort();
			currentActor.abort();
		}
	}

	@Test
	void stopsTouchMaterialAfterCollisionMiss() throws Exception {
		initializeGameTimeDao();
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new TestCreature(controller, emptyRegion());
		CollisionMaterialActor actor = new CollisionMaterialActor(creature, new Node("touch-material"), materialTemplate(), CheckType.TOUCH);

		try {
			actor.act();
			assertNotNull(controller.getTask(TaskId.ZONE_MATERIAL_ACTION));

			actor.onMoved(new CollisionResults(CollisionIntention.MATERIAL.getId(), true, 1));

			assertNull(controller.getTask(TaskId.ZONE_MATERIAL_ACTION));
		} finally {
			actor.abort();
		}
	}

	@Test
	void keepsEnterTriggeredFireMaterialActiveAfterCollisionMiss() throws Exception {
		initializeGameTimeDao();
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new TestCreature(controller, emptyRegion());
		CollisionMaterialActor actor = new CollisionMaterialActor(creature, new Node("FIREPOT"), materialTemplate(), CheckType.TOUCH);

		try {
			actor.act();
			Future<?> task = controller.getTask(TaskId.ZONE_MATERIAL_ACTION);
			assertNotNull(task);

			actor.onMoved(new CollisionResults(CollisionIntention.MATERIAL.getId(), true, 1));

			assertSame(task, controller.getTask(TaskId.ZONE_MATERIAL_ACTION));
		} finally {
			actor.abort();
		}
	}

	private static void initializeGameTimeDao() {
		GameTimeManager.init(new TestServerVariablesDAO());
	}

	private static MaterialTemplate materialTemplate() throws Exception {
		MaterialSkill skill = new MaterialSkill();
		setField(skill, "frequency", Float.NaN);
		MaterialTemplate template = new MaterialTemplate();
		setField(template, "skills", List.of(skill));
		return template;
	}

	private static MapRegion emptyRegion() throws Exception {
		MapRegion region = new ObjenesisStd().newInstance(MapRegion.class);
		setField(region, "zoneMap", Map.of());
		return region;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestCreatureController extends CreatureController<Creature> {
	}

	private static final class TestCreature extends Creature {
		private final MapRegion activeRegion;

		private TestCreature(TestCreatureController controller, MapRegion activeRegion) {
			super(1, controller, null, new TestVisibleObjectTemplate(), new WorldPosition(1));
			this.activeRegion = activeRegion;
		}

		@Override
		public MapRegion getActiveRegion() {
			return activeRegion;
		}

		@Override
		public String getName() {
			return "test-creature";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

	private static final class TestVisibleObjectTemplate extends VisibleObjectTemplate {
		@Override
		public int getTemplateId() {
			return 1;
		}

		@Override
		public String getName() {
			return "test-template";
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}

	private static final class TestServerVariablesDAO extends ServerVariableDaoImpl {

		@Override
		public int load(String var) {
			return 0;
		}

		@Override
		public boolean store(String var, int value) {
			return true;
		}
	}
}
