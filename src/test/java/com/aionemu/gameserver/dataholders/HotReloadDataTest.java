package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;

class HotReloadDataTest {

	@Test
	void eventDataKeepsActiveNamesAfterParsing() throws Exception {
		EventData data = unmarshal(EventData.class,
				"<events_config><active>Alpha;Beta</active><events/></events_config>");

		assertEquals("Alpha;Beta", data.getActiveText());
	}

	@Test
	void replacingSkillsRemovesOldGroupIndex() throws Exception {
		SkillData current = skillData(1, "SKILL_OLD");
		SkillData replacement = skillData(2, "SKILL_NEW");

		current.setSkillTemplates(replacement.getSkillTemplates());

		assertNull(current.getSkillTemplateByGroup("OLD"));
		assertNotNull(current.getSkillTemplateByGroup("NEW"));
	}

	private SkillData skillData(int id, String stack) throws Exception {
		return unmarshal(SkillData.class, "<skill_data><skill_template skill_id=\"" + id
				+ "\" name=\"test\" nameId=\"1\" stack=\"" + stack
				+ "\" skilltype=\"NONE\" activation=\"ACTIVE\" duration=\"0\"/></skill_data>");
	}

	private <T> T unmarshal(Class<T> type, String xml) throws Exception {
		return type.cast(JAXBContext.newInstance(type).createUnmarshaller().unmarshal(new StringReader(xml)));
	}
}
