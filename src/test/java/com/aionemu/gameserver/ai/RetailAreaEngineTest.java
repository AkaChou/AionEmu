package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.RetailAiData.LocationAliasPoint;
import com.aionemu.gameserver.dataholders.RetailAiData.LimitArea;
import com.aionemu.gameserver.dataholders.RetailAiData.QuestArea;
import com.aionemu.gameserver.dataholders.RetailAiData.ResurrectArea;
import com.aionemu.gameserver.model.geometry.PolyArea;
import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.world.zone.ZoneName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RetailAreaEngineTest {

	@Test
	void selectsEnabledMatchingRetailResurrectArea() {
		var destination = new LocationAliasPoint(7, 8, 9, 90);
		var area = new ResurrectArea("Boss_Area", "Boss_Return", 0, "none",
			new PolyArea(ZoneName.createOrGet("Boss_Area"), 123,
				List.of(new Point2D(0, 0), new Point2D(0, 10), new Point2D(10, 0)), 0, 10),
			List.of(destination));

		assertEquals(destination, RetailAreaEngine.findResurrectPoint(List.of(area), Map.of(), 0, "PC", 1, 1, 1));
		assertNull(RetailAreaEngine.findResurrectPoint(List.of(area), Map.of("boss_area", false), 0, "PC", 1, 1, 1));
		assertNull(RetailAreaEngine.findResurrectPoint(List.of(area), Map.of(), 1, "PC_DARK", 1, 1, 1));
		assertNull(RetailAreaEngine.findResurrectPoint(List.of(area), Map.of(), 0, "PC", 20, 20, 1));
	}

	@Test
	void reportsQuestAreaOnlyOnEntryAndHonorsDynamicState() {
		var area = new QuestArea("Quest_Area", List.of(18033, 28033),
			new PolyArea(ZoneName.createOrGet("Quest_Area"), 123,
				List.of(new Point2D(0, 0), new Point2D(0, 10), new Point2D(10, 0)), 0, 10));
		var active = new HashSet<QuestArea>();

		assertEquals(List.of(area), RetailAreaEngine.enteredQuestAreas(List.of(area), Map.of(), active, 1, 1, 1));
		assertEquals(List.of(), RetailAreaEngine.enteredQuestAreas(List.of(area), Map.of(), active, 1, 1, 1));
		assertEquals(List.of(), RetailAreaEngine.enteredQuestAreas(List.of(area), Map.of("quest_area", false), active, 1, 1, 1));
		assertEquals(List.of(area), RetailAreaEngine.enteredQuestAreas(List.of(area), Map.of(), active, 1, 1, 1));
	}

	@Test
	void honorsDynamicNoParkAndNoRecallState() {
		var area = new LimitArea("LimitArea", true, "None", true, "All", 30, false, false, 16,
			new PolyArea(ZoneName.createOrGet("LimitArea"), 123,
				List.of(new Point2D(0, 0), new Point2D(0, 10), new Point2D(10, 0)), 0, 10));

		assertEquals(false, RetailAreaEngine.isNoPark(List.of(area), Map.of(), 0, 30, 1, 1, 1));
		assertEquals(true, RetailAreaEngine.isNoPark(List.of(area), Map.of(), 0, 31, 1, 1, 1));
		assertEquals(false, RetailAreaEngine.isNoPark(List.of(area), Map.of("limitarea", false), 0, 31, 1, 1, 1));
		assertEquals(true, RetailAreaEngine.isNoPark(List.of(area), Map.of("limitarea", true), 1, 31, 1, 1, 1));
		var lightOnly = new LimitArea("LightOnly", true, "None", false, "Light", 0, false, false, 16, area.area());
		assertEquals(false, RetailAreaEngine.isNoPark(List.of(lightOnly), Map.of("lightonly", true), 1, 1, 1, 1, 1));
		assertEquals(true, RetailAreaEngine.isNoRecall(List.of(area), Map.of(), 1, 1, 1));
		assertEquals(false, RetailAreaEngine.isNoRecall(List.of(area), Map.of("limitarea", false), 1, 1, 1));
	}
}
