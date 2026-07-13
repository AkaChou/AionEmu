package com.aionemu.gameserver.skillengine.properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import jakarta.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class PropertiesTest {

	@Test
	void emptyTargetStatusDoesNotRejectSkill() throws Exception {
		String xml = """
				<properties first_target="ME" first_target_range="0" target_status=""/>
				""";
		Properties properties = JAXBContext.newInstance(Properties.class).createUnmarshaller()
				.unmarshal(new StreamSource(new StringReader(xml)), Properties.class).getValue();
		TestCreature effector = new ObjenesisStd().newInstance(TestCreature.class);
		Skill skill = new Skill(new SkillTemplate(), effector, 1, effector, null);

		assertTrue(properties.validate(skill));
	}

	private static final class TestCreature extends Creature {

		@SuppressWarnings("unused")
		private TestCreature() {
			super(1, null, null, null, null);
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}
}
