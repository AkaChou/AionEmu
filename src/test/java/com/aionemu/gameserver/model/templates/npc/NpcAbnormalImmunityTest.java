package com.aionemu.gameserver.model.templates.npc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.stats.container.StatEnum;

import jakarta.xml.bind.JAXBContext;

class NpcAbnormalImmunityTest {

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
		return (NpcTemplate) JAXBContext.newInstance(NpcTemplate.class).createUnmarshaller()
				.unmarshal(new StringReader(xml));
	}
}
