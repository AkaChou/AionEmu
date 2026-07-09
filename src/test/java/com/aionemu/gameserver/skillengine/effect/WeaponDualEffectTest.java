package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class WeaponDualEffectTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void startEffectSetsDualStatsWithoutChangeModifiers() {
		Player player = objenesis.newInstance(Player.class);
		player.setGameStats(new PlayerGameStats(player));
		WeaponDualEffect weaponDualEffect = new WeaponDualEffect();
		setField(weaponDualEffect, EffectTemplate.class, "value", 63);
		setField(weaponDualEffect, EffectTemplate.class, "delta", 2);
		setField(weaponDualEffect, WeaponDualEffect.class, "skillEfficiency", 50);
		setField(weaponDualEffect, WeaponDualEffect.class, "maxDamageChance", 20);
		setField(weaponDualEffect, WeaponDualEffect.class, "maxDamageDelta", 80);

		SkillTemplate skillTemplate = new SkillTemplate();
		setField(skillTemplate, SkillTemplate.class, "activationAttribute", ActivationAttribute.PASSIVE);

		weaponDualEffect.startEffect(new Effect(player, player, skillTemplate, 2, 0));

		assertEquals(0.50f, player.getGameStats().getSkillEfficiency());
		assertEquals(180, player.getGameStats().getMaxDamageChance());
		assertEquals(0.67f, player.getGameStats().getMinDamageRatio(), 0.001f);
	}

	private static void setField(Object target, Class<?> owner, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
