package com.aionemu.gameserver.world.geo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.Field;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.MaterialData;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.IgnoreProperties;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.Geometry;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.world.WorldPosition;
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeoServiceSkillObstacleTest {

	private final boolean oldCanSeeEnable = GeoDataConfig.CANSEE_ENABLE;
	private final MaterialData oldMaterialData = DataManager.MATERIAL_DATA;

	@BeforeEach
	void setUp() throws Exception {
		GeoDataConfig.CANSEE_ENABLE = true;
		DataManager.MATERIAL_DATA = (MaterialData) JAXBContext.newInstance(MaterialData.class)
				.createUnmarshaller().unmarshal(new StringReader(
						"<material_templates><material id=\"123\" skill_obstacle=\"3\"/></material_templates>"));
	}

	@AfterEach
	void tearDown() {
		GeoDataConfig.CANSEE_ENABLE = oldCanSeeEnable;
		DataManager.MATERIAL_DATA = oldMaterialData;
	}

	@Test
	void filtersSkillObstacleCollisionsByRetailLevel() throws Exception {
		TestGeoMap map = new TestGeoMap();
		GeoService geoService = new GeoService();
		setGeoData(geoService, map);
		TestCreature source = new TestCreature(1, 0);
		TestCreature target = new TestCreature(2, 10);

		assertFalse(geoService.canSeeSkill(source, target, 3));
		assertTrue(geoService.canSeeSkill(source, target, 4));
		assertTrue(geoService.canSeeSkill(source, target, 5));

		map.baseVisible = false;
		assertFalse(geoService.canSeeSkill(source, target, 5));
	}

	private static void setGeoData(GeoService geoService, GeoMap map) throws Exception {
		Field field = GeoService.class.getDeclaredField("geoData");
		field.setAccessible(true);
		field.set(geoService, new GeoData() {
			@Override
			public void loadGeoMaps() {
			}

			@Override
			public GeoMap getMap(int worldId) {
				return map;
			}
		});
	}

	private static final class TestGeoMap extends GeoMap {

		private boolean baseVisible = true;
		private final CollisionResults skillCollisions = new CollisionResults(CollisionIntention.SKILL.getId(), 1);

		private TestGeoMap() {
			super("1", 100);
			Mesh mesh = new Mesh();
			mesh.setCollisionFlags((short) (CollisionIntention.SKILL.getId() << 8 | 123));
			CollisionResult result = new CollisionResult(new Vector3f(5, 0, 0), 5);
			result.setGeometry(new Geometry("skill-obstacle", mesh));
			skillCollisions.addCollision(result);
		}

		@Override
		public boolean canSee(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
				int instanceId, IgnoreProperties ignoreProperties) {
			return baseVisible;
		}

		@Override
		public CollisionResults getCollisions(Vector3f origin, float targetX, float targetY, float targetZ, int instanceId,
				byte intentions, IgnoreProperties ignoreProperties) {
			return skillCollisions;
		}
	}

	private static final class TestCreature extends Creature {

		private TestCreature(int objectId, float x) {
			super(objectId, new CreatureController<>() {}, null, new TestTemplate(), position(x));
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

		@Override
		public boolean isSpawned() {
			return true;
		}

		private static WorldPosition position(float x) {
			WorldPosition position = new WorldPosition(1);
			position.setXYZH(x, 0f, 0f, (byte) 0);
			return position;
		}
	}

	private static final class TestTemplate extends VisibleObjectTemplate {

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
}
