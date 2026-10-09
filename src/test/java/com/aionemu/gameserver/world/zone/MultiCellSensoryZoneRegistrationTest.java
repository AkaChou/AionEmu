package com.aionemu.gameserver.world.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.ZoneData;
import com.aionemu.gameserver.model.geometry.Area;
import com.aionemu.gameserver.model.geometry.MultiPolyArea;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;

/**
 * 常设门：原版**多胞感官区**（一个区名由多个互不相连的多边形胞组成）必须被 zones XML 正确登记，
 * 且运行时按「进入任一胞即算进入」判定；每胞保留自己的 top/bottom，不得把 A 胞的 XY 与 B 胞的 Z 拼起来。
 * <p>
 * 背景：原版世界文件的同名感官区 NPC 常出现多个胞（`IDEternity_War_ShugoSeller` 6 胞、
 * `DF5_SensoryArea_65_Deva_Q15322b` 3 胞）。只注册其中一个胞会让其余胞变成静默死边；把多胞压成
 * 一个全局 Z 区间则会放行错胞的高度。本门锁定「多胞 = 多环 `<points>` 一条 `<zone>`」的登记形与几何语义。
 * <p>
 * Standing gate: retail multi-cell sensory areas (one zone name made of several disjoint polygons) must be
 * registered in the zones XML and must behave as "inside any cell"; every cell keeps its own top/bottom, so a
 * cell's XY must never combine with another cell's Z. Registering only one cell would leave the others as
 * silent dead edges, and flattening the cells into one global Z range would accept wrong heights.
 */
class MultiCellSensoryZoneRegistrationTest {

	private static final Path ZONES_DIR = Path.of("src/main/resources/aion/data/static_data/zones");
	private static final Path ZONES_QUEST = ZONES_DIR.resolve("zones_quest.xml");
	private static final Path ZONES_XSD = ZONES_DIR.resolve("zones.xsd");

	/** 原版采样点的顶点平均值（逐胞几何中心，已用射线法复核在多边形内）。 / Vertex-average cell centers. */
	private static final float[][] SHUGOSELLER_CELLS = {
		{992.990021f, 1015.205872f},
		{1062.609833f, 956.725647f},
		{787.437836f, 925.410644f},
		{696.513886f, 414.104736f},
		{739.607636f, 719.307678f},
		{741.600494f, 974.536499f},
	};
	private static final float[][] Q15322B_CELLS = {
		{1387.676618f, 2652.883615f},
		{953.225016f, 2547.818115f},
		{1614.502040f, 2893.115165f},
	};

	/** 生产 zones XML 必须符合自身 schema（多环 `<points>` 是合法声明形）。 / Schema validity. */
	@Test
	void zonesQuestValidatesAgainstItsSchema() throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
			.newSchema(ZONES_XSD.toFile());
		schema.newValidator().validate(new StreamSource(ZONES_QUEST.toFile()));
	}

	/** 6 胞感官区：进入任一胞都算进入；区外点不算。 / Every cell counts as inside; outside stays outside. */
	@Test
	void multiCellSensoryZoneCountsEveryCellAsInside() throws Exception {
		Area area = questZones().get("IDETERNITY_WAR_SHUGOSELLER_302350000");
		MultiPolyArea multi = assertInstanceOf(MultiPolyArea.class, area);
		assertEquals(6, multi.getCells().size(), "原版 IDEternity_War_ShugoSeller 有 6 胞");
		for (int cell = 0; cell < SHUGOSELLER_CELLS.length; cell++) {
			float x = SHUGOSELLER_CELLS[cell][0];
			float y = SHUGOSELLER_CELLS[cell][1];
			assertTrue(multi.isInside3D(x, y, 300f), "cell " + cell + " 中心必须算在区内");
		}
		assertFalse(multi.isInside3D(1000f, 2000f, 300f), "区外点不算在区内");
	}

	/** 每胞独立 Z：错胞的 XY 不得借用别胞的 Z 区间。 / Per-cell Z: no borrowing another cell's Z range. */
	@Test
	void multiCellSensoryZoneKeepsPerCellZRange() throws Exception {
		Area area = questZones().get("DF5_SENSORYAREA_65_DEVA_Q15322B_220080000");
		MultiPolyArea multi = assertInstanceOf(MultiPolyArea.class, area);
		assertEquals(3, multi.getCells().size(), "原版 DF5_SensoryArea_65_Deva_Q15322b 有 3 胞");

		float x = Q15322B_CELLS[0][0];
		float y = Q15322B_CELLS[0][1];
		assertTrue(multi.isInside3D(x, y, 305f), "cell 0 自己的 Z 区间（295.5–315.5）内必须算在区");
		assertFalse(multi.isInside3D(x, y, 240f), "cell 0 的 XY 不得借用 cell 2 的 Z 区间（228.4–248.4）");
		assertTrue(multi.isInside3D(Q15322B_CELLS[2][0], Q15322B_CELLS[2][1], 240f), "cell 2 自己的 Z 区间内必须算在区");
	}

	/**
	 * 早期用「出生点 + r=10 球体」近似注册的 4 个感官区必须回到原版多边形：区名不变，几何换成
	 * `<sensory_area>` 环，因此**原版 Z 窗口**（例如 Q30722 = 281.9–331.9）才是判定区间，而不是球体的 ±10。
	 * <p>
	 * The four sensory zones approximated earlier as "spawn point + r=10 sphere" must use the retail polygon:
	 * same zone name, but the geometry is the `<sensory_area>` ring, so the retail Z window (e.g. Q30722 =
	 * 281.9-331.9) is what decides entry instead of the sphere's ±10 around its center.
	 */
	@Test
	void legacySphereSensoryZonesUseTheRetailPolygon() throws Exception {
		Map<String, Area> zones = questZones();
		float[][] retail = {
			{1706.478333f, 543.938543f, 298.366882f},   // DF5_SENSORYAREA_Q16987_220080000
			{2095.936977f, 2276.398089f, 307.641968f},  // LF5_SENSORYAREA_Q26987_210070000
			{2861.048889f, 1675.143097f, 329.921478f},  // DF5_SENSORYAREA_Q30722_220080000
			{106.218785f, 1458.214798f, 506.414307f},   // LF5_SENSORYAREA_Q30772_210070000
		};
		List<String> names = List.of(
			"DF5_SENSORYAREA_Q16987_220080000",
			"LF5_SENSORYAREA_Q26987_210070000",
			"DF5_SENSORYAREA_Q30722_220080000",
			"LF5_SENSORYAREA_Q30772_210070000");
		for (int index = 0; index < names.size(); index++) {
			String name = names.get(index);
			Area area = assertInstanceOf(com.aionemu.gameserver.model.geometry.PolyArea.class, zones.get(name), name);
			float[] sample = retail[index];
			assertTrue(area.isInside3D(sample[0], sample[1], sample[2]),
				name + " 原版多边形（含顶沿附近的 Z）必须算在区内——旧 r=10 球体会漏判");
		}
	}

	/** 本片登记的 5 别名 × 2 阵营区名齐备（每区 3 胞）。 / Both factions' five sensory zones (3 cells each). */
	@Test
	void q15322SensoryZonesRegisterAllFiveCellsPerAlias() throws Exception {
		Map<String, Area> zones = questZones();
		for (String prefix : List.of("DF5_SENSORYAREA_65_DEVA_Q15322", "LF5_SENSORYAREA_65_DEVA_Q25322")) {
			for (char step : "bdfhj".toCharArray()) {
				String name = prefix + Character.toUpperCase(step) + "_" + (prefix.startsWith("DF5") ? "220080000" : "210070000");
				MultiPolyArea multi = assertInstanceOf(MultiPolyArea.class, zones.get(name), name);
				assertEquals(3, multi.getCells().size(), name + " 胞数");
			}
		}
	}

	/** 按名字取出生产 quest 区（从 zones XML 反序列化，走真实的 afterUnmarshal 建面逻辑）。 /
	 * Loads the production quest zones through JAXB so the real area-building code runs. */
	private static Map<String, Area> questZones() throws Exception {
		Unmarshaller unmarshaller = JAXBContext.newInstance(ZoneData.class).createUnmarshaller();
		ZoneData data = (ZoneData) unmarshaller.unmarshal(ZONES_QUEST.toFile());
		Map<String, Area> byName = new HashMap<>();
		data.getZones().forEach((mapId, infos) -> {
			for (ZoneInfo info : infos) {
				byName.put(info.getZoneTemplate().getName().name(), info.getArea());
			}
		});
		return byName;
	}
}
