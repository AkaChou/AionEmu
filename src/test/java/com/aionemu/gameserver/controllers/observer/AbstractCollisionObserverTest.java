package com.aionemu.gameserver.controllers.observer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.geo.GeoData;
import com.aionemu.gameserver.world.geo.GeoService;

class AbstractCollisionObserverTest {

	@Test
	void touchRayEndsBelowFeetWhenNoPhysicalGroundIsFound() throws Exception {
		GeoService geoService = new GeoService();
		GeoMap emptyMap = new GeoMap("test", 1);
		setGeoData(geoService, new GeoData() {
			@Override
			public void loadGeoMaps() {
			}

			@Override
			public GeoMap getMap(int worldId) {
				return emptyMap;
			}
		});
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(GeoService.class.getName(), geoService);
		GeoService.setInstanceProvider(beanFactory.getBeanProvider(GeoService.class));

		AtomicReference<Ray> capturedRay = new AtomicReference<>();
		CountDownLatch collisionChecked = new CountDownLatch(1);
		Node geometry = new Node("test-material") {
			@Override
			public int collideWith(Collidable other, CollisionResults results) {
				capturedRay.set((Ray) other);
				return 0;
			}
		};
		TestCreature creature = new TestCreature(100f);
		AbstractCollisionObserver observer = new AbstractCollisionObserver(creature, geometry,
				CollisionIntention.MATERIAL.getId(), AbstractCollisionObserver.CheckType.TOUCH) {
			@Override
			public void onMoved(CollisionResults result) {
				collisionChecked.countDown();
			}
		};

		try {
			observer.moved();
			assertTrue(collisionChecked.await(5, TimeUnit.SECONDS), "材质碰撞检测未执行");
			Ray ray = capturedRay.get();
			assertNotNull(ray);
			float endZ = ray.origin.z + ray.direction.z * ray.limit;
			assertTrue(endZ < creature.getZ(), () -> "射线终点应低于角色脚面，实际为 " + endZ);
		} finally {
			GeoService.setInstanceProvider(null);
		}
	}

	private static void setGeoData(GeoService geoService, GeoData geoData) throws Exception {
		Field field = GeoService.class.getDeclaredField("geoData");
		field.setAccessible(true);
		field.set(geoService, geoData);
	}

	private static final class TestCreature extends Creature {

		private TestCreature(float z) {
			super(1, new TestCreatureController(), null, new TestVisibleObjectTemplate(), position(z));
		}

		@Override
		public int getInstanceId() {
			return 1;
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

	private static final class TestCreatureController extends CreatureController<Creature> {
	}

	private static WorldPosition position(float z) {
		WorldPosition position = new WorldPosition(1);
		position.setXYZH(10f, 20f, z, (byte) 0);
		return position;
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
