package com.aionemu.gameserver.world.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.ZoneData;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;

/**
 * 常设门：全库 usearea 注册覆盖审计（2026-10-08，QE-043 同类）补入的使用区必须保留原版几何，
 * 且 Aturam 使用区必须使用 item 模板声明的区名。
 * 几何权威：球体 = 原版 {@code Map/XML/Subzones/source_sphere.csv} 的 itemUseArea 行；
 * 多边形 = 原版 {@code Map/Worlds/<world>/world.xml} 的 {@code <item_use_area>}（本次并已逐点核对
 * 与既有旧名区完全一致）。断言区名/所属地图/区类/球心半径与多边形首点，防止静默漂移回旧值。
 * <p>
 * Standing gate: the item-use zones added by the 2026-10-08 catalog-wide usearea registry audit
 * (QE-043 family) must keep their retail geometry, and the Aturam area must carry the zone name the
 * item templates declare. Sphere authority = retail {@code source_sphere.csv} itemUseArea rows;
 * polygon authority = retail {@code Map/Worlds/<world>/world.xml} {@code <item_use_area>} entries
 * (point-identical to the legacy-named zones this repair renamed). The assertions lock zone name,
 * map id, zone class, sphere centre/radius, and the polygon's first point so the values cannot
 * silently drift back.
 */
class ItemUseAreaZoneRegistrationTest {

	private static final Path ZONES_DIR = Path.of("src/main/resources/aion/data/static_data/zones");
	private static final Path ZONES_XSD = ZONES_DIR.resolve("zones.xsd");

	/** 球体使用区：区名 → 原版 source_sphere.csv 的球心/半径（mapid 锁定在其所在文件断言内）。 */
	@Test
	void sphereItemUseAreasKeepRetailSourceSphereGeometry() throws Exception {
		Map<Integer, Map<String, ZoneInfo>> quest = zonesOf("zones_quest.xml");
		assertSphere(quest, 400010000, "AB1_ITEMUSEAREA_Q2060", 1524.98f, 1590.73f, 1599.15f, 77.80f);
		assertSphere(quest, 300610000, "IDRAKSHA_ITEMUSEAREA_Q28703", 676.68f, 667.03f, 527.06f, 19.57f);
	}

	/** 多边形使用区（原版 world.xml，无球值源）：两地图同形登记，首点/底顶锁定。 */
	@Test
	void polygonItemUseAreasKeepRetailWorldXmlGeometry() throws Exception {
		Map<Integer, Map<String, ZoneInfo>> quest = zonesOf("zones_quest.xml");
		for (int mapId : new int[] {301400000, 301590000}) {
			ZoneInfo info = zone(quest, mapId, "IDSWEEP_ITEMAREA_SUMMON");
			assertEquals(ZoneClassName.ITEM_USE, info.getZoneTemplate().getZoneType(), "IDSWEEP_ITEMAREA_SUMMON 区类");
			assertEquals(1, info.getZoneTemplate().getPoints().size(), "IDSWEEP_ITEMAREA_SUMMON 环数");
			assertEquals(4, info.getZoneTemplate().getPoints().getFirst().getPoint().size(), "IDSWEEP_ITEMAREA_SUMMON 顶点数");
			assertEquals(391.304840f, info.getZoneTemplate().getPoints().getFirst().getBottom(), 0.001f);
			assertEquals(441.304840f, info.getZoneTemplate().getPoints().getFirst().getTop(), 0.001f);
			assertEquals(526.073364f, info.getZoneTemplate().getPoints().getFirst().getPoint().getFirst().getX(), 0.001f);
			assertEquals(278.304413f, info.getZoneTemplate().getPoints().getFirst().getPoint().getFirst().getY(), 0.001f);
		}
	}

	/** Aturam 使用区：item 模板声明 IDStation_ItemUseArea_3F；旧名（_1/_2）不得回归，几何维持原版多边形。 */
	@Test
	void aturamItemUseAreaUsesTheItemDeclaredName() throws Exception {
		assertAturam("zones_300240000.xml", 300240000, "IDSTATION_ITEM_USE_AREA_1");
		assertAturam("zones_300241000.xml", 300241000, "IDSTATION_ITEM_USE_AREA_2");
	}

	private static void assertAturam(String fileName, int mapId, String legacyName) throws Exception {
		Map<Integer, Map<String, ZoneInfo>> zones = zonesOf(fileName);
		ZoneInfo info = zone(zones, mapId, "IDSTATION_ITEMUSEAREA_3F");
		assertEquals(ZoneClassName.ITEM_USE, info.getZoneTemplate().getZoneType(), "IDStation_ItemUseArea_3F 区类");
		assertEquals(890.0f, info.getZoneTemplate().getPoints().getFirst().getBottom(), 0.001f);
		assertEquals(940.0f, info.getZoneTemplate().getPoints().getFirst().getTop(), 0.001f);
		assertEquals(92.283775f, info.getZoneTemplate().getPoints().getFirst().getPoint().getFirst().getX(), 0.001f);
		assertEquals(619.9553f, info.getZoneTemplate().getPoints().getFirst().getPoint().getFirst().getY(), 0.001f);
		assertNull(zones.get(mapId).get(legacyName), "旧名 " + legacyName + " 不得回归");
	}

	/** 生产 zones XML 必须符合自身 schema。 / Production zones XML must validate against its own schema. */
	@Test
	void changedZoneFilesValidateAgainstTheirSchema() throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(ZONES_XSD.toFile());
		for (String fileName : new String[] {"zones_quest.xml", "zones_300240000.xml", "zones_300241000.xml"}) {
			schema.newValidator().validate(new StreamSource(ZONES_DIR.resolve(fileName).toFile()));
		}
	}

	private static void assertSphere(Map<Integer, Map<String, ZoneInfo>> zones, int mapId, String name, float x, float y,
			float z, float r) {
		ZoneInfo info = zone(zones, mapId, name);
		assertEquals(ZoneClassName.ITEM_USE, info.getZoneTemplate().getZoneType(), name + " 区类");
		assertNotNull(info.getZoneTemplate().getSphere(), name + " 应为球体区");
		assertEquals(x, info.getZoneTemplate().getSphere().getX(), 0.001f, name + " 球心 X");
		assertEquals(y, info.getZoneTemplate().getSphere().getY(), 0.001f, name + " 球心 Y");
		assertEquals(z, info.getZoneTemplate().getSphere().getZ(), 0.001f, name + " 球心 Z");
		assertEquals(r, info.getZoneTemplate().getSphere().getR(), 0.001f, name + " 半径");
	}

	private static ZoneInfo zone(Map<Integer, Map<String, ZoneInfo>> zones, int mapId, String name) {
		Map<String, ZoneInfo> byName = zones.get(mapId);
		assertNotNull(byName, "mapid " + mapId + " 无区文件登记");
		ZoneInfo info = byName.get(name);
		assertNotNull(info, "mapid " + mapId + " 缺区 " + name);
		return info;
	}

	/** 按文件名反序列化 zones 文件（走真实 afterUnmarshal 建面逻辑）。 */
	private static Map<Integer, Map<String, ZoneInfo>> zonesOf(String fileName) throws Exception {
		Unmarshaller unmarshaller = JAXBContext.newInstance(ZoneData.class).createUnmarshaller();
		ZoneData data = (ZoneData) unmarshaller.unmarshal(ZONES_DIR.resolve(fileName).toFile());
		Map<Integer, Map<String, ZoneInfo>> byMap = new HashMap<>();
		data.getZones().forEach((mapId, infos) -> {
			Map<String, ZoneInfo> byName = new HashMap<>();
			for (ZoneInfo info : infos) {
				byName.put(info.getZoneTemplate().getName().name(), info);
			}
			byMap.put(mapId, byName);
		});
		return byMap;
	}
}
