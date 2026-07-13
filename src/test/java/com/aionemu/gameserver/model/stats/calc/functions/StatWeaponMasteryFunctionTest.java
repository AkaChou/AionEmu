package com.aionemu.gameserver.model.stats.calc.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.WeaponType;

class StatWeaponMasteryFunctionTest {

	@Test
	void appliesRateAndFixedDamageOnlyForMatchingWeapon() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setEquipment(new TestEquipment(player, WeaponType.BOOK_2H));
		StatOwner owner = new StatOwner() { };

		AdditionStat matching = new AdditionStat(StatEnum.MAGICAL_ATTACK, 100, player);
		new StatWeaponMasteryFunction(owner, WeaponType.BOOK_2H,
				new StatRateFunction(StatEnum.MAGICAL_ATTACK, 30, true), StatEnum.MAGICAL_ATTACK).apply(matching);
		new StatWeaponMasteryFunction(owner, WeaponType.BOOK_2H,
				new StatAddFunction(StatEnum.MAGICAL_ATTACK, 7, true), StatEnum.MAGICAL_ATTACK).apply(matching);
		assertEquals(137, matching.getCurrent());

		AdditionStat mismatching = new AdditionStat(StatEnum.MAGICAL_ATTACK, 100, player);
		new StatWeaponMasteryFunction(owner, WeaponType.ORB_2H,
				new StatRateFunction(StatEnum.MAGICAL_ATTACK, 30, true), StatEnum.MAGICAL_ATTACK).apply(mismatching);
		assertEquals(100, mismatching.getCurrent());
	}

	private static final class TestEquipment extends Equipment {

		private final WeaponType weaponType;

		private TestEquipment(Player player, WeaponType weaponType) {
			super(player);
			this.weaponType = weaponType;
		}

		@Override
		public WeaponType getMainHandWeaponType() {
			return weaponType;
		}
	}
}
