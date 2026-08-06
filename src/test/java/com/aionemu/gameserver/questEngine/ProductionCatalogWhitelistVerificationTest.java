package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 临时验证：逐个编译生产 catalog 条目，报告编译失败与白名单违规。
 * Temporary verification: compile each production catalog entry, report compile failures and whitelist violations.
 */
public class ProductionCatalogWhitelistVerificationTest {

	@Test
	public void verifyProductionCatalogWhitelist() throws Exception {
		ClassLoader loader = getClass().getClassLoader();
		try (InputStream input = loader.getResourceAsStream("aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing production catalog");
			}
			QuestDefinitionCatalogManifest manifest = QuestDefinitionCatalogManifest.load(input);
			List<String> compileFailures = new ArrayList<>();
			List<String> violations = new ArrayList<>();
			int ok = 0;
			for (var entry : manifest.entries()) {
				String expectedResource = "aion/data/static_data/quest_definition/quests/"
					+ entry.id() + ".xml";
				if (!expectedResource.equals(entry.resource())) {
					violations.add(entry.id() + ":RESOURCE_NOT_CANONICAL:" + entry.resource());
				}
				byte[] xml;
				try (InputStream ris = loader.getResourceAsStream(entry.resource())) {
					if (ris == null) {
						compileFailures.add(entry.id() + ":RESOURCE_MISSING");
						continue;
					}
					xml = ris.readAllBytes();
				}
				CompiledQuestDefinition d;
				try {
					d = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml));
				} catch (Exception e) {
					compileFailures.add(entry.id() + ":" + String.valueOf(e.getMessage()).lines().findFirst().orElse(e.getClass().getSimpleName()));
					continue;
				}
				ok++;
				if (d.id() != entry.id()) {
					violations.add(entry.id() + ":ID_MISMATCH:" + d.id());
				}
				for (var t : d.definition().transitions()) {
					if (!(t.event() instanceof QuestEvent.TalkToNpc)
							&& !(t.event() instanceof QuestEvent.KillNpc)
							&& !(t.event() instanceof QuestEvent.KillNpcSet)
							&& !(t.event() instanceof QuestEvent.AttackNpc)
							&& !(t.event() instanceof QuestEvent.CanAct)
							&& !(t.event() instanceof QuestEvent.EnterZone)
							&& !(t.event() instanceof QuestEvent.LevelUp)
							&& !(t.event() instanceof QuestEvent.EnterWorld)
							&& !(t.event() instanceof QuestEvent.UseItem)
							&& !(t.event() instanceof QuestEvent.ItemPlay)
							&& !(t.event() instanceof QuestEvent.GetItem)
							&& !(t.event() instanceof QuestEvent.PassFlyingRing)
							&& !(t.event() instanceof QuestEvent.EnterWindStream)
							&& !(t.event() instanceof QuestEvent.AtDistance)
							&& !(t.event() instanceof QuestEvent.Die)
							&& !(t.event() instanceof QuestEvent.LogOut)
							&& !(t.event() instanceof QuestEvent.MovieEnd)
							&& !(t.event() instanceof QuestEvent.NpcReachTarget)
							&& !(t.event() instanceof QuestEvent.NpcLostTarget)
							&& !(t.event() instanceof QuestEvent.ZoneMissionEnd)
							&& !(t.event() instanceof QuestEvent.InvisibleTimerEnd)
							&& !(t.event() instanceof QuestEvent.FailCraft)
							&& !(t.event() instanceof QuestEvent.EquipItem)
							&& !(t.event() instanceof QuestEvent.Abandon)
							&& !(t.event() instanceof QuestEvent.DredgionReward)
							&& !(t.event() instanceof QuestEvent.HouseItemUse)
							&& !(t.event() instanceof QuestEvent.KillInWorld)
							&& !(t.event() instanceof QuestEvent.KillRanked)
							&& !(t.event() instanceof QuestEvent.LeaveZone)
							&& !(t.event() instanceof QuestEvent.QuestTimerEnd)
							&& !(t.event() instanceof QuestEvent.UseSkill)
							&& !(t.event() instanceof QuestEvent.QuestDialog)
							&& !(t.event() instanceof QuestEvent.BonusApply)) {
						violations.add(d.id() + ":" + t.event().type());
					}
				}
			}
			System.out.println("PRODUCTION_COMPILE_OK=" + ok);
			System.out.println("PRODUCTION_COMPILE_FAILURES=" + compileFailures.size());
			compileFailures.stream().limit(30).forEach(System.out::println);
			System.out.println("PRODUCTION_WHITELIST_VIOLATIONS=" + violations.size());
			violations.stream().limit(60).forEach(System.out::println);
			org.junit.jupiter.api.Assertions.assertEquals(manifest.entries().size(), ok,
				() -> "production catalog compile failures: " + compileFailures);
			org.junit.jupiter.api.Assertions.assertTrue(compileFailures.isEmpty(),
				() -> "production catalog compile failures: " + compileFailures);
			org.junit.jupiter.api.Assertions.assertTrue(violations.isEmpty(),
				() -> "production catalog whitelist violations: " + violations);
		}
	}
}
