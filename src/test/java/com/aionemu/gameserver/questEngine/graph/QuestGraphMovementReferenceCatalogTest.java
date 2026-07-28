package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.FlyRingData;
import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.dataholders.loadingutils.WindstreamDefinitionLoader;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamRoute;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamTemplate;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler.FlyingRingReference;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler.WindstreamRouteReference;

import jakarta.xml.bind.JAXBContext;

/** 验证正式 movement 静态数据的复合引用闭包。 / Verifies composite reference closure for formal movement static data. */
class QuestGraphMovementReferenceCatalogTest {

	/** 验证当前生产静态数据包含任务实际使用的 route 与 flying-ring 引用。 / Verifies current production data contains quest-used route and flying-ring references. */
	@Test
	void buildsCurrentProductionStaticDataReferenceClosure() throws Exception {
		WindstreamData windstreams = WindstreamDefinitionLoader.load(
			new File("src/main/resources/aion/definitions/compact/world/fly_path.xml"),
			new File("src/main/resources/aion/definitions/compact/wind.xml"),
			new File("src/main/resources/aion/definitions/compact/id-mappings.xml"));
		FlyRingData rings = (FlyRingData) JAXBContext.newInstance(FlyRingData.class).createUnmarshaller()
			.unmarshal(new File("src/main/resources/aion/data/static_data/fly_rings/fly_rings.xml"));

		QuestGraphMovementReferenceCatalog.MovementReferences references = QuestGraphMovementReferenceCatalog.build(windstreams, rings);

		assertTrue(references.windstreamRoutes().contains(new WindstreamRouteReference(210130000, 405)));
		assertTrue(references.windstreamRoutes().contains(new WindstreamRouteReference(210130000, 406)));
		assertTrue(references.windstreamRoutes().contains(new WindstreamRouteReference(210130000, 407)));
		assertTrue(references.flyingRings().contains(new FlyingRingReference(210020000, "ELTNEN_AIR_BOOSTER_1")));
		assertTrue(references.flyingRings().contains(new FlyingRingReference(220020000, "MORHEIM_AIR_BOOSTER_6")));
		assertTrue(references.flyingRings().contains(new FlyingRingReference(210020000, "ERACUS_TEMPLE_AIR_BOOSTER_7")));
	}

	/** 验证 catalog 按 world 与规范静态键构造不可变复合引用。 / Verifies the catalog builds immutable composite keys by world and canonical static key. */
	@Test
	void buildsCompositeReferencesFromFormalStaticData() {
		WindstreamRoute route = new WindstreamRoute(210130000, 405, 1000,
			List.of(new com.aionemu.gameserver.model.geometry.Point3D(1, 2, 3)));
		WindstreamData windstreams = new WindstreamData(List.of(new WindstreamTemplate(210130000, List.of())), List.of(route));
		FlyRingData rings = new FlyRingData();
		rings.addAll(List.of(new FlyRingTemplate("ELTNEN_AIR_BOOSTER_1", 210020000,
			new com.aionemu.gameserver.model.utils3d.Point3D(1, 2, 3),
			new com.aionemu.gameserver.model.utils3d.Point3D(2, 2, 3),
			new com.aionemu.gameserver.model.utils3d.Point3D(0, 2, 3), 6)));

		QuestGraphMovementReferenceCatalog.MovementReferences references = QuestGraphMovementReferenceCatalog.build(windstreams, rings);

		assertEquals(Set.of(new WindstreamRouteReference(210130000, 405)), references.windstreamRoutes());
		assertEquals(Set.of(new FlyingRingReference(210020000, "ELTNEN_AIR_BOOSTER_1")), references.flyingRings());
		assertEquals(new WindstreamRouteReference(210130000, 405),
			WindstreamRouteReference.fromTeleportId(210130000, 405001));
	}

	/** 验证同 world 重复 route/ring 静态键会失败关闭。 / Verifies duplicate route or ring keys in one world fail closed. */
	@Test
	void rejectsDuplicateCompositeStaticKeys() {
		WindstreamRoute first = new WindstreamRoute(210130000, 405, 1000,
			List.of(new com.aionemu.gameserver.model.geometry.Point3D(1, 2, 3)));
		WindstreamRoute duplicate = new WindstreamRoute(210130000, 405, 1000,
			List.of(new com.aionemu.gameserver.model.geometry.Point3D(2, 3, 4)));
		WindstreamData windstreams = new WindstreamData(List.of(new WindstreamTemplate(210130000, List.of())), List.of(first, duplicate));

		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphMovementReferenceCatalog.build(windstreams, new FlyRingData()));
	}
}
