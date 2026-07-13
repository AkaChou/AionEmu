package com.aionemu.gameserver.model.stats.calc.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ArmorType;

class StatArmorMasteryFunctionTest {

	@Test
	void preservesRateAndFixedBonusForMatchingArmor() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setEquipment(new TestEquipment(player, ArmorType.CLOTHES));
		StatOwner owner = new StatOwner() { };

		AdditionStat matching = new AdditionStat(StatEnum.PHYSICAL_DEFENSE, 100, player);
		new StatArmorMasteryFunction(owner, ArmorType.CLOTHES,
				new StatRateFunction(StatEnum.PHYSICAL_DEFENSE, 30, true)).apply(matching);
		new StatArmorMasteryFunction(owner, ArmorType.CLOTHES,
				new StatAddFunction(StatEnum.PHYSICAL_DEFENSE, 7, true)).apply(matching);
		assertEquals(137, matching.getCurrent());

		AdditionStat mismatching = new AdditionStat(StatEnum.PHYSICAL_DEFENSE, 100, player);
		new StatArmorMasteryFunction(owner, ArmorType.PLATE,
				new StatRateFunction(StatEnum.PHYSICAL_DEFENSE, 30, true)).apply(mismatching);
		assertEquals(100, mismatching.getCurrent());
	}

	private static final class TestEquipment extends Equipment {

		private final ArmorType armorType;

		private TestEquipment(Player player, ArmorType armorType) {
			super(player);
			this.armorType = armorType;
		}

		@Override
		public boolean isArmorEquipped(ArmorType armorType) {
			return this.armorType == armorType;
		}
	}
}
