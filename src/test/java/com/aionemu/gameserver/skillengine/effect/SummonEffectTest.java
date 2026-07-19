package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SummonEffectTest {

	@Test
	void targetlessPointEffectUsesCasterAsPositionReference() {
		Player caster = new ObjenesisStd().newInstance(Player.class);

		assertSame(caster, SummonEffect.getPositionReference(new Effect(caster, null, new SkillTemplate(), 1, 0)));
	}
}
