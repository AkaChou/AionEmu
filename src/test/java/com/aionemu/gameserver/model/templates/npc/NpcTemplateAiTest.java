package com.aionemu.gameserver.model.templates.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.AIConfig;

import jakarta.xml.bind.JAXBContext;

class NpcTemplateAiTest {

	@Test
	void fearfulBeastOverrideFollowsSwitchAndTemplateConditions() throws Exception {
		boolean previous = AIConfig.ENABLE_FEARFUL_BEAST_AI;
		try {
			NpcTemplate beast = template("ATTACKABLE", 2, "BEAST", 9);
			AIConfig.ENABLE_FEARFUL_BEAST_AI = false;
			assertEquals("general", beast.getAi());

			AIConfig.ENABLE_FEARFUL_BEAST_AI = true;
			assertEquals("fearful_beast", beast.getAi());
			assertEquals("fearful_beast", template("ATTACKABLE", 1, "BEAST", 9).getAi());
			assertEquals("general", template("PEACE", 1, "BEAST", 9).getAi());
			assertEquals("general", template("ATTACKABLE", 3, "BEAST", 9).getAi());
			assertEquals("general", template("ATTACKABLE", 1, "ELYOS", 9).getAi());
			assertEquals("general", template("ATTACKABLE", 1, "BEAST", 10).getAi());
			assertEquals("aggressive", template("ATTACKABLE", 1, "BEAST", 9, "aggressive").getAi());
		} finally {
			AIConfig.ENABLE_FEARFUL_BEAST_AI = previous;
		}
	}

	private static NpcTemplate template(String npcType, int level, String race, int maxHp) throws Exception {
		return template(npcType, level, race, maxHp, "general");
	}

	private static NpcTemplate template(String npcType, int level, String race, int maxHp, String ai) throws Exception {
		String xml = "<npc_template npc_id=\"1\" level=\"" + level + "\" name_id=\"1\" "
				+ "npc_type=\"" + npcType + "\" race=\"" + race + "\" ai=\"" + ai + "\">"
				+ "<stats maxHp=\"" + maxHp + "\"/></npc_template>";
		return (NpcTemplate) JAXBContext.newInstance(NpcTemplate.class).createUnmarshaller()
				.unmarshal(new StringReader(xml));
	}
}
