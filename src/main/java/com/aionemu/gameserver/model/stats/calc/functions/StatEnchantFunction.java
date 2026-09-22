package com.aionemu.gameserver.model.stats.calc.functions;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ArmorType;

/**
 * 属性 Enchant 函数模型。
 * Stat Enchant Function model.
 */
@Slf4j

public class StatEnchantFunction extends StatAddFunction {

	private final Item item;
	private final int point;

	public StatEnchantFunction(Item owner, StatEnum stat, int point) {
		this.stat = stat;
		this.item = owner;
		this.point = point;
	}

	/** 返回 priority / Returns the priority */
	@Override
	public final int getPriority() {
		return 30;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		if (!item.isEquipped()) {
			return;
		}
		int enchantLvl = this.item.getEnchantLevel();
		if ((this.item.getItemTemplate().isAccessory())) {
			enchantLvl = this.item.getAuthorize();
		}
		if (enchantLvl == 0) {
			return;
		}
		if ((item.getEquipmentSlot() & ItemSlot.MAIN_OFF_HAND.getSlotIdMask()) != 0
				|| (item.getEquipmentSlot() & ItemSlot.SUB_OFF_HAND.getSlotIdMask()) != 0) {
			return;
		}
		if (item.getItemTemplate().getArmorType() == ArmorType.PLUME) {
			stat.addToBonus(getEnchantAdditionModifier(enchantLvl, stat));
		} else {
			stat.addToBase(getEnchantAdditionModifier(enchantLvl, stat));
		}
	}

	private int getEnchantAdditionModifier(int enchantLvl, Stat2 stat) {
		int enchantAdvLvl=0;
		if (enchantLvl>15){
			enchantAdvLvl=enchantLvl-15;
			enchantLvl=15;
		}
		if (item.getItemTemplate().isWeapon()) {
			return getWeaponModifiers(enchantLvl, enchantAdvLvl);
		}
		if (this.item.getItemTemplate().isAccessory() && !this.item.getItemTemplate().isPlume()
				&& !this.item.getItemTemplate().isBracelet()) {
			if (this.point == 0) {
				return getAccessoryModifiers(enchantLvl);
			}
			return this.point;
		}
		if (this.item.getItemTemplate().isArmor()
				|| this.item.getItemTemplate().isPlume() && !this.item.getItemTemplate().isBracelet()) {
			return getArmorModifiers(enchantLvl, enchantAdvLvl, stat);
		}
		if (this.item.getItemTemplate().isArmor()
				|| this.item.getItemTemplate().isBracelet() && !this.item.getItemTemplate().isPlume()) {
			return getBraceleteModifiers(enchantLvl);
		}
		return 0;
	}

	private int getWeaponModifiers(int enchantLvl, int enchantAdvLvl) {
		return switch (stat) {
			case MAIN_HAND_POWER, OFF_HAND_POWER, PHYSICAL_ATTACK -> switch (item.getItemTemplate().getWeaponType()) {
				case GUN_1H, SWORD_1H, DAGGER_1H -> 2 * enchantLvl + 4 * enchantAdvLvl;
				case BOW, SWORD_2H, POLEARM_2H -> 4 * enchantLvl + 8 * enchantAdvLvl;
				case MACE_1H, STAFF_2H -> 3 * enchantLvl + 6 * enchantAdvLvl;
				default -> 0;
			};
			case BOOST_MAGICAL_SKILL -> switch (item.getItemTemplate().getWeaponType()) {
				case ORB_2H, GUN_1H, HARP_2H, BOOK_2H, MACE_1H, STAFF_2H, CANNON_2H, KEYBLADE_2H ->
					20 * enchantLvl + 40 * enchantAdvLvl;
				default -> 0;
			};
			case MAGICAL_ATTACK -> switch (item.getItemTemplate().getWeaponType()) {
				case GUN_1H -> 2 * enchantLvl + 4 * enchantAdvLvl;
				case BOOK_2H -> 3 * enchantLvl + 6 * enchantAdvLvl;
				case ORB_2H, HARP_2H, CANNON_2H, KEYBLADE_2H -> 4 * enchantLvl + 8 * enchantAdvLvl;
				default -> 0;
			};
			default -> 0;
		};
	}

	private int getAccessoryModifiers(int autorizeLvl) {
		return switch (this.stat) {
			case PVP_ATTACK_RATIO -> switch (autorizeLvl) {
				case 1 -> 2;
				case 2 -> 7;
				case 3 -> 12;
				case 4 -> 17;
				case 5 -> 25;
				case 6 -> 60;
				case 7 -> 75;
				default -> 0;
			};
			case PVP_DEFEND_RATIO -> switch (autorizeLvl) {
				case 1 -> 3;
				case 2 -> 9;
				case 3 -> 15;
				case 4 -> 21;
				case 5 -> 31;
				case 6 -> 41;
				case 7 -> 55;
				default -> 0;
			};
			default -> 0;
		};
	}

	private int getBraceleteModifiers(int autorizeLvl) {
		return switch (this.stat) {
			case PVP_ATTACK_RATIO -> switch (autorizeLvl) {
				case 1, 2, 3, 4, 5 -> 0;
				case 6 -> 5;
				case 7 -> 5;
				case 8 -> 10;
				case 9 -> 15;
				case 10 -> 20;
				default -> 0;
			};
			case PVP_DEFEND_RATIO -> switch (autorizeLvl) {
				case 1 -> 3;
				case 2 -> 7;
				case 3 -> 11;
				case 4 -> 16;
				case 5 -> 21;
				case 6 -> 27;
				case 7 -> 33;
				case 8 -> 40;
				case 9 -> 48;
				case 10 -> 57;
				default -> 0;
			};
			default -> 0;
		};
	}

	private int getArmorModifiers(int enchantLvl, int enchantAdvLvl, Stat2 applyStat) {
		ArmorType armorType = item.getItemTemplate().getArmorType();
		if (armorType == null) {
			return 0;
		}
		long slot = item.getEquipmentSlot();
		int equipmentSlot = (int) slot;
		switch (item.getItemTemplate().getArmorType()) {
		// 4.9 附魔属性。 / 4.9 Enchant Stats.
		case ROBE:
			return switch (equipmentSlot) {
				case 1 << 5, 1 << 11, 1 << 4 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> enchantLvl + 2 * enchantAdvLvl;
					case MAXHP -> 20 * enchantLvl + 40 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 2 * enchantLvl + 4 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 12 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 2 * enchantLvl + 4 * enchantAdvLvl;
					case MAXHP -> 22 * enchantLvl + 44 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 3 * enchantLvl + 6 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 3 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 3 * enchantLvl + 6 * enchantAdvLvl;
					case MAXHP -> 24 * enchantLvl + 48 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 3 * enchantLvl + 6 * enchantAdvLvl;
					default -> 0;
				};
				default -> 0;
			};
			case LEATHER:
			return switch (equipmentSlot) {
				case 1 << 5, 1 << 11, 1 << 4 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 2 * enchantLvl + 4 * enchantAdvLvl;
					case MAXHP -> 18 * enchantLvl + 36 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 2 * enchantLvl + 4 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 12 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 3 * enchantLvl + 6 * enchantAdvLvl;
					case MAXHP -> 20 * enchantLvl + 40 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 3 * enchantLvl + 6 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 3 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case MAXHP -> 22 * enchantLvl + 44 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 3 * enchantLvl + 6 * enchantAdvLvl;
					default -> 0;
				};
				default -> 0;
			};
			case CHAIN:
			return switch (equipmentSlot) {
				case 1 << 5, 1 << 11, 1 << 4 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 3 * enchantLvl + 6 * enchantAdvLvl;
					case MAXHP -> 16 * enchantLvl + 32 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 2 * enchantLvl + 4 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 12 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case MAXHP -> 18 * enchantLvl + 36 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 3 * enchantLvl + 6 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 3 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 5 * enchantLvl + 10 * enchantAdvLvl;
					case MAXHP -> 20 * enchantLvl + 40 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 3 * enchantLvl + 6 * enchantAdvLvl;
					default -> 0;
				};
				default -> 0;
			};
			case PLATE:
			return switch (equipmentSlot) {
				case 1 << 5, 1 << 11, 1 << 4 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case MAXHP -> 14 * enchantLvl + 28 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 2 * enchantLvl + 4 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 12 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 5 * enchantLvl + 10 * enchantAdvLvl;
					case MAXHP -> 16 * enchantLvl + 32 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 3 * enchantLvl + 6 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 2 * enchantLvl + 4 * enchantAdvLvl;
					default -> 0;
				};
				case 1 << 3 -> switch (stat) {
					case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
					case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case PHYSICAL_DEFENSE -> 6 * enchantLvl + 12 * enchantAdvLvl;
					case MAXHP -> 18 * enchantLvl + 36 * enchantAdvLvl;
					case PHYSICAL_CRITICAL_RESIST -> 4 * enchantLvl + 8 * enchantAdvLvl;
					case MAGICAL_DEFEND -> 3 * enchantLvl + 6 * enchantAdvLvl;
					default -> 0;
				};
				default -> 0;
			};
			case SHIELD:
			switch (stat) {
			case DAMAGE_REDUCE:
				float reduceRate = enchantLvl > 10 ? 0.2f : enchantLvl * 0.02f;
				return Math.round(reduceRate * applyStat.getBase());
			case BLOCK:
				if (enchantLvl > 10) {
					int blocktemp = 30 * (enchantLvl - 10 + enchantAdvLvl);
					if (blocktemp>300)
						blocktemp=300;
				return blocktemp;
				}
			case MAXHP:
				return 100 * enchantAdvLvl;
			case PHYSICAL_DEFENSE:
				if (enchantAdvLvl>5)
					return 50 * (enchantAdvLvl - 5);
			case MAGIC_SKILL_BOOST_RESIST:
				if (enchantAdvLvl>5)
					return 20 * (enchantAdvLvl - 5);
			}
			return 0;
		case PLUME:
			int plumeench = enchantLvl + enchantAdvLvl;
			return switch (this.stat) {
				case MAXHP -> 150 * plumeench;
				case PHYSICAL_ATTACK -> 4 * plumeench;
				case BOOST_MAGICAL_SKILL -> 20 * plumeench;
				case PHYSICAL_CRITICAL -> 12 * plumeench;
				case PHYSICAL_ACCURACY -> 16 * plumeench;
				case MAGICAL_ACCURACY -> 8 * plumeench;
				case MAGICAL_CRITICAL -> 8 * plumeench;
				default -> 0;
			};
			// 5.0 翅膀强化 / 5.0 Wings Enchant
		 // 呵呵 / lol
		case WING:
			if (enchantLvl+enchantAdvLvl<20)
				enchantLvl = enchantLvl+enchantAdvLvl;
			else
				enchantLvl = 20;
			if (enchantAdvLvl - 5 < 0)
				enchantAdvLvl = 0;
			else
				enchantAdvLvl -= 5;
			return switch (this.stat) {
				case PHYSICAL_ATTACK -> enchantLvl + 2 * enchantAdvLvl;
				case BOOST_MAGICAL_SKILL -> 4 * enchantLvl + 8 * enchantAdvLvl;
				case MAXHP -> 40 * enchantLvl + 80 * enchantAdvLvl;
				case PHYSICAL_CRITICAL_RESIST -> 2 * enchantLvl + 8 * enchantAdvLvl;
				case FLY_TIME -> 10 * enchantLvl + 20 * enchantAdvLvl;
				case MAGICAL_CRITICAL_RESIST -> enchantLvl + 4 * enchantAdvLvl;
				default -> 0;
			};
		}
		return 0;
	}
}
