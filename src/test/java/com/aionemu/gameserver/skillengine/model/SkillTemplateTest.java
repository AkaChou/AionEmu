package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import com.aionemu.gameserver.dataholders.SkillData;
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

class SkillTemplateTest {

	@Test
	void applyMcritDefaultsToTrue() {
		assertTrue(new SkillTemplate().isMcritApplied());
	}

	@Test
	void unmarshalsApplyMcritAttribute() throws Exception {
		SkillTemplate template = unmarshal("""
			<skill_data>
				<skill_template skill_id="1" name="test" nameId="1" skilltype="MAGICAL" skillsubtype="ATTACK"
					activation="ACTIVE" duration="0" applymcrit="false"/>
			</skill_data>
			""");

		assertFalse(template.isMcritApplied());
	}

	private static SkillTemplate unmarshal(String xml) throws Exception {
		return ((SkillData) JAXBContext.newInstance(SkillData.class)
				.createUnmarshaller()
				.unmarshal(new StringReader(xml)))
				.getSkillTemplate(1);
	}
}
