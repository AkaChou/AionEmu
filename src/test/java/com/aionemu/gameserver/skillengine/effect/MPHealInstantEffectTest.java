package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;

class MPHealInstantEffectTest {

	@Test
	void retailMpHealBoostIsCappedAtThreeTimesBaseValue() {
		assertEquals(250, AbstractHealEffect.capMpHealBoost(100, 250));
		assertEquals(300, AbstractHealEffect.capMpHealBoost(100, 500));
	}

	@Test
	void notifiesNpcOnlyWhenPlayerActuallyRestoresHpOrMp() {
		ObjenesisStd objenesis = new ObjenesisStd();
		Player player = objenesis.newInstance(Player.class);
		Npc npc = objenesis.newInstance(Npc.class);
		RecordingAI ai = new RecordingAI();
		npc.setAi2(ai);
		SkillTemplate template = new SkillTemplate();
		setField(template, "activationAttribute", ActivationAttribute.ACTIVE);
		Effect effect = new Effect(player, npc, template, 1, 0);

		AbstractHealEffect.notifyHealedByUser(effect, HealType.HP, 10);
		AbstractHealEffect.notifyHealedByUser(effect, HealType.MP, 0);
		AbstractHealEffect.notifyHealedByUser(effect, HealType.FP, 10);

		assertEquals(1, ai.calls);
		assertSame(player, ai.player);
	}

	private static void setField(SkillTemplate target, String name, Object value) {
		try {
			Field field = SkillTemplate.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class RecordingAI extends AITemplate {
		private int calls;
		private Player player;

		@Override
		public void onHealedByUser(Player player) {
			calls++;
			this.player = player;
		}
	}
}
