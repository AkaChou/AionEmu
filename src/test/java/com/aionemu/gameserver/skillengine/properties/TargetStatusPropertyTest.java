package com.aionemu.gameserver.skillengine.properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import jakarta.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class TargetStatusPropertyTest {

	@Test
	void acceptsMixedCaseStatusNamesFromSkillData() throws Exception {
		Properties properties = JAXBContext.newInstance(Properties.class).createUnmarshaller()
				.unmarshal(new StreamSource(new StringReader(
						"<properties first_target=\"ME\" first_target_range=\"0\" target_status=\"Stun\"/>")), Properties.class)
				.getValue();
		TestCreature target = new ObjenesisStd().newInstance(TestCreature.class);
		target.setEffectController(new TestEffectController(target, AbnormalState.STUN.getId()));
		Skill skill = new Skill(new SkillTemplate(), target, 1, target, null);
		skill.getEffectedList().add(target);

		assertTrue(TargetStatusProperty.set(skill, properties));
	}

	private static final class TestEffectController extends EffectController {
		private TestEffectController(Creature owner, int abnormals) {
			super(owner);
			this.abnormals = abnormals;
		}
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
