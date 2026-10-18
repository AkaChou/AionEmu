package com.aionemu.gameserver.controllers.observer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.lifecycle.TestServiceProviders;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.materials.MaterialSkill;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldPosition;

class TerrainZoneCollisionMaterialActorTest {

	private static ThreadPoolManager threadPoolManager;

	@BeforeAll
	static void installThreadPoolProvider() {
		// act() 会注册地形材质周期任务，静态线程池门面必须由测试提供。
		// act() registers the terrain-material periodic task, so the static thread-pool facade must be provided.
		threadPoolManager = new ThreadPoolManager();
		TestServiceProviders.install(ThreadPoolManager.class, threadPoolManager);
	}

	@AfterAll
	static void clearThreadPoolProvider() {
		TestServiceProviders.clear(ThreadPoolManager.class);
		threadPoolManager.shutdown();
	}

	@Test
	void zoneAndTerrainMaterialTasksCoexist() throws Exception {
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new TestCreature(controller);
		TerrainZoneCollisionMaterialActor actor = actorWithSkill(creature);
		Future<?> zoneTask = new CompletableFuture<Void>();
		controller.addTask(TaskId.ZONE_MATERIAL_ACTION, zoneTask);

		try {
			actor.act();

			assertSame(zoneTask, controller.getTask(TaskId.ZONE_MATERIAL_ACTION));
			assertNotNull(controller.getTask(TaskId.TERRAIN_MATERIAL_ACTION));

			actor.abort();

			assertSame(zoneTask, controller.getTask(TaskId.ZONE_MATERIAL_ACTION));
		} finally {
			actor.abort();
			controller.cancelTask(TaskId.ZONE_MATERIAL_ACTION);
		}
	}

	@Test
	void restartsAfterTerrainTaskWasCancelled() throws Exception {
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new TestCreature(controller);
		TerrainZoneCollisionMaterialActor actor = actorWithSkill(creature);

		try {
			actor.act();
			Future<?> firstTask = controller.getTask(TaskId.TERRAIN_MATERIAL_ACTION);
			controller.cancelTask(TaskId.TERRAIN_MATERIAL_ACTION);

			actor.act();

			Future<?> restartedTask = controller.getTask(TaskId.TERRAIN_MATERIAL_ACTION);
			assertNotSame(firstTask, restartedTask);
		} finally {
			actor.abort();
		}
	}

	@Test
	void leavingOldActorDoesNotCancelReplacementTask() throws Exception {
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new TestCreature(controller);
		TerrainZoneCollisionMaterialActor oldActor = actorWithSkill(creature);
		TerrainZoneCollisionMaterialActor currentActor = actorWithSkill(creature);

		try {
			oldActor.act();
			Future<?> oldTask = controller.getTask(TaskId.TERRAIN_MATERIAL_ACTION);
			currentActor.act();
			Future<?> replacementTask = controller.getTask(TaskId.TERRAIN_MATERIAL_ACTION);
			assertNotSame(oldTask, replacementTask);

			oldActor.abort();

			assertSame(replacementTask, controller.getTask(TaskId.TERRAIN_MATERIAL_ACTION));
		} finally {
			oldActor.abort();
			currentActor.abort();
		}
	}

	@SuppressWarnings("unchecked")
	private static TerrainZoneCollisionMaterialActor actorWithSkill(Creature creature) throws Exception {
		MaterialSkill skill = new MaterialSkill();
		setField(skill, "frequency", Float.NaN);
		TerrainZoneCollisionMaterialActor actor = new TerrainZoneCollisionMaterialActor(creature);
		Field currentSkillsField = TerrainZoneCollisionMaterialActor.class.getDeclaredField("currentSkills");
		currentSkillsField.setAccessible(true);
		AtomicReference<List<MaterialSkill>> currentSkills = (AtomicReference<List<MaterialSkill>>) currentSkillsField.get(actor);
		currentSkills.set(List.of(skill));
		return actor;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestCreatureController extends CreatureController<Creature> {
	}

	private static final class TestCreature extends Creature {
		private TestCreature(TestCreatureController controller) {
			super(1, controller, null, new TestVisibleObjectTemplate(), new WorldPosition(1));
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
}
