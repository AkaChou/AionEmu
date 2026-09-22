package com.aionemu.gameserver.services;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 物品强化规则查表：按物品 ID 判定强化失败规则与荣耀护盾技能授予。
 * Item enchant rule tables: item-id based failure rules and the Glory: Shield skill grant.
 * <p>该类型只服务 {@link EnchantService}：全部为静态纯查表/规则函数，不持有状态、不创建对象。
 * 对外仍通过 {@link EnchantService} 的原 public static 方法访问（门面签名不变）。
 * This type only serves {@link EnchantService}: every function is a static pure table lookup or
 * rule without state or allocation. External callers keep using the original public static
 * {@link EnchantService} facade methods with unchanged signatures.</p>
 */
final class EnchantItemRules {

	/**
	 * 仅静态规则，禁止实例化。
	 * Static rules only; not instantiable.
	 */
	private EnchantItemRules() {
	}

	/**
	 * 强化失败时物品不销毁，规则：0~10 降 1 级；11~14 降至 +10；15~19 降至 +15。 / - If Enchanting fails, the item is not destroyed and following rules apply. - 0~10: -1 Enchanting level - 11~14: Enchanting level drops to +10 - 15~19: Enchanting level drops to +15 - 20 and higher: Enchanting level drops to +20 - https://aionpowerbook.com/powerbook/KR_-_Update_September_20th_2017
	 */
	static boolean isEnhancedAncientFallusha(Item targetItem) {
		return switch (targetItem.getItemId()) {
			case 100002363, 100101767, 100201949, 100501729, 100601833, 100901781, 101501785, 101701768, 101801523,
			     101901420, 102001544, 102101359, 115002341, 115002342, 110102319, 110102320, 110302303, 110302304,
			     110302307, 110302308, 110551695, 110551697, 110551700, 110551701, 110551702, 110602055, 110602056,
			     110602060, 110602061, 111102118, 111102121, 111102122, 111302256, 111302257, 111302260, 111302261,
			     111502266, 111502267, 111502268, 111502271, 111502272, 111502273, 111602025, 111602026, 111602030,
			     112102055, 112102056, 112102059, 112102060, 112302185, 112302186, 112302189, 112302190, 112502200,
			     112502201, 112502202, 112502205, 112502206, 112502207, 112602008, 112602009, 112602012, 112602013,
			     113102113, 113102114, 113102116, 113102117, 113302268, 113302269, 113302271, 113302272, 113502271,
			     113502272, 113502273, 113502275, 113502276, 113502277, 113601998, 113601999, 113602001, 113602002,
			     114102158, 114102159, 114102162, 114102163, 114302318, 114302319, 114302322, 114302323, 114502284,
			     114502285, 114502286, 114502289, 114502290, 114502291, 114602008, 114602009, 114602013, 114602014,
			     120001720, 120001721, 121001670, 121001671, 122001954, 122001955, 123001704, 123001705, 125005554,
			     125005555, 125005556, 125005557, 125005558, 125005559, 125005560, 125005561, 125005562, 187000257 ->
				true;
			default -> false;
		};
	}

	/**
	 * 灰狼饰品强化失败不销毁，重置为 +0；不可交易，仅可包装一次。 / - Gray Wolf Accessories will not be destroyed when the upgrading process fails and will reset to +0. - Gray Wolf Accessories cannot be traded and can only be wrapped once when the Upgrade level reaches +10. - Upgrading Gray Wolf Accessories increases PvE abilities. - https://aionpowerbook.com/powerbook/KR_-_Update_September_20th_2017
	 */
	static boolean isGrayWolfAccessories(Item targetItem) {
		return switch (targetItem.getItemId()) {
			case 125005563, 125005564, 125005565, 125005566, 125005567, 125005568, 125005569, 125005570, 125005571,
			     120001718, 120001719, 121001668, 121001669, 122001952, 122001953, 123001702, 123001703 -> true;
			default -> false;
		};
	}

	/**
	 * 高阶守护者达努亚改型/重塑装备说明：强化销毁永不。 / - Archdaeva's Reformed Danuar - Archdaeva's Remodeled Danuar - Archdaeva's Restructured Danuar - Destroy Enchant: NEVER!!!
	 */
	static boolean isArchdaevaReformedDanuar(Item targetItem) {
		return switch (targetItem.getItemId()) {
			case 110551314, 111501874, 112501810, 113501893, 114501901, 125004548 -> true;
			default -> false;
		};
	}

	/**
	 * 是否为执政官改装达努亚系列。
	 * Whether the item is Archdaeva remodeled Danuar gear.
	 * @param targetItem 目标物品 / target item
	 * @return 是否匹配 / whether matched
	 */
	static boolean isArchdaevaRemodeledDanuar(Item targetItem) {
		return switch (targetItem.getItemId()) {
			case 100002007, 100101489, 100201670, 100501447, 100601565, 100901524, 101301408, 101501510, 101701505,
			     101801340, 101901245, 102001368, 102101183, 110101990, 110301967, 110551315, 110601755, 111101784,
			     111301906, 111501875, 111601719, 112101729, 112301843, 112501811, 112601700, 113101795, 113301937,
			     113501894, 113601702, 114101829, 114301974, 114501902, 114601708, 125004547, 125004552, 125004553,
			     115001961 -> true;
			default -> false;
		};
	}

	/**
	 * 是否为执政官重构达努亚系列。
	 * Whether the item is Archdaeva restructured Danuar gear.
	 * @param targetItem 目标物品 / target item
	 * @return 是否匹配 / whether matched
	 */
	static boolean isArchdaevaRestructuredDanuar(Item targetItem) {
        return switch (targetItem.getItemId()) {
            case 100002008, 100101490, 100201671, 100501448, 100601566, 100901525, 101301409, 101501511, 101701506,
                 101801341, 101901246, 102001369, 102101184, 110101991, 110301966, 110551316, 110601754, 111101785,
                 111301905, 111501876, 111601718, 112101730, 112301842, 112501812, 112601699, 113101796, 113301936,
                 113501895, 113601701, 114101830, 114301973, 114501903, 114601707, 125004546, 125004549, 125004550,
                 125004551, 125004554, 115001962 -> true;
            default -> false;
        };
    }

	/**
	 * 荣耀：护盾。 / 5.5 强化系统相关说明。 / http://aionpowerbook.com/powerbook/Glory:_Shield.
	 */
	static void GloryShieldSkill(Player player) {
		int Enchant = 0;
		Equipment equip = player.getEquipment();
		for (Item item : equip.getEquippedItemsWithoutStigmaOld()) {
			if (item.getItemTemplate().isWeapon() || item.getItemTemplate().isArmor() || item.getItemTemplate().getItemSlot() == 32768) {
				if (item.getEnchantLevel() >= 16) {
					Enchant++;
				}
			}
		}
		if (Enchant >= 6) {
			if (player.getSkillList().isSkillPresent(4694) || player.getSkillList().isSkillPresent(4695)) {
				return;
			}
			if (player.getRace() == Race.ELYOS) {
				player.getSkillList().addSkill(player, 4694, 1);
			} else if (player.getRace() == Race.ASMODIANS) {
				player.getSkillList().addSkill(player, 4695, 1);
			}
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));
		} else {
			if (player.getSkillList().isSkillPresent(4694)) {
				SkillLearnService.removeSkill(player, 4694);
			} else if (player.getSkillList().isSkillPresent(4695)) {
				SkillLearnService.removeSkill(player, 4695);
			}
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));
		}
	}

}
