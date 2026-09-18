package com.aionemu.gameserver.model.templates.npc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.aionemu.gameserver.model.stats.container.StatEnum;

import jakarta.xml.bind.JAXBContext;

class NpcAbnormalImmunityTest {

	@ParameterizedTest
	@CsvSource({
		"sleep, SLEEP_RESISTANCE",
		"pulled, PULLED_RESISTANCE",
		"Stat_ArAll, SLEEP_RESISTANCE",
		"Stat_ArStunLike, STUMBLE_RESISTANCE"
	})
	void unmarshalsNamedImmunityInsteadOfTreatingItAsAnInteger(String immunity, StatEnum stat) throws Exception {
		assertTrue(template(immunity).isImmuneTo(stat));
	}

	@Test
	void retailAbnormalGroupsPreserveStunLikeBoundary() throws Exception {
		NpcTemplate all = template("Stat_ArAll");
		assertTrue(all.isImmuneTo(StatEnum.SLEEP_RESISTANCE));
		assertTrue(all.isImmuneTo(StatEnum.PULLED_RESISTANCE));
		assertFalse(all.isImmuneTo(StatEnum.STUMBLE_RESISTANCE));
		assertFalse(all.isImmuneTo(StatEnum.OPENAREIAL_RESISTANCE));

		NpcTemplate stunLike = template("Stat_ArStunLike");
		assertTrue(stunLike.isImmuneTo(StatEnum.CHARM_RESISTANCE));
		assertTrue(stunLike.isImmuneTo(StatEnum.STUMBLE_RESISTANCE));
		assertTrue(stunLike.isImmuneTo(StatEnum.OPENAREIAL_RESISTANCE));
		assertFalse(stunLike.isImmuneTo(StatEnum.SLEEP_RESISTANCE));
		assertFalse(stunLike.isImmuneTo(StatEnum.PARALYZE_RESISTANCE));

		NpcTemplate boss = template("Stat_ArAll,Stat_ArStunLike");
		assertTrue(boss.isImmuneTo(StatEnum.SLEEP_RESISTANCE));
		assertTrue(boss.isImmuneTo(StatEnum.STUMBLE_RESISTANCE));
		assertTrue(boss.isImmuneTo(StatEnum.OPENAREIAL_RESISTANCE));
	}

	private static NpcTemplate template(String immunity) throws Exception {
		String xml = "<npc_template npc_id=\"216520\" level=\"57\" name_id=\"1\" "
				+ "npc_type=\"ATTACKABLE\" abnormal_immunity=\"" + immunity + "\"/>";
		var unmarshaller = JAXBContext.newInstance(NpcTemplate.class).createUnmarshaller();
		// 绑定异常应直接失败；上方断言另行捕获无异常但属性未生效的情况。
		// Fail on binding errors; the assertions also catch silently ignored attributes.
		unmarshaller.setEventHandler(event -> false);
		return (NpcTemplate) unmarshaller.unmarshal(new StringReader(xml));
	}
}
