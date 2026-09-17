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
 *
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
		switch (targetItem.getItemId()) {
		case 100002363:
		case 100101767:
		case 100201949:
		case 100501729:
		case 100601833:
		case 100901781:
		case 101501785:
		case 101701768:
		case 101801523:
		case 101901420:
		case 102001544:
		case 102101359:
		case 115002341:
		case 115002342:
		case 110102319:
		case 110102320:
		case 110302303:
		case 110302304:
		case 110302307:
		case 110302308:
		case 110551695:
		case 110551697:
		case 110551700:
		case 110551701:
		case 110551702:
		case 110602055:
		case 110602056:
		case 110602060:
		case 110602061:
		case 111102118:
		case 111102121:
		case 111102122:
		case 111302256:
		case 111302257:
		case 111302260:
		case 111302261:
		case 111502266:
		case 111502267:
		case 111502268:
		case 111502271:
		case 111502272:
		case 111502273:
		case 111602025:
		case 111602026:
		case 111602030:
		case 112102055:
		case 112102056:
		case 112102059:
		case 112102060:
		case 112302185:
		case 112302186:
		case 112302189:
		case 112302190:
		case 112502200:
		case 112502201:
		case 112502202:
		case 112502205:
		case 112502206:
		case 112502207:
		case 112602008:
		case 112602009:
		case 112602012:
		case 112602013:
		case 113102113:
		case 113102114:
		case 113102116:
		case 113102117:
		case 113302268:
		case 113302269:
		case 113302271:
		case 113302272:
		case 113502271:
		case 113502272:
		case 113502273:
		case 113502275:
		case 113502276:
		case 113502277:
		case 113601998:
		case 113601999:
		case 113602001:
		case 113602002:
		case 114102158:
		case 114102159:
		case 114102162:
		case 114102163:
		case 114302318:
		case 114302319:
		case 114302322:
		case 114302323:
		case 114502284:
		case 114502285:
		case 114502286:
		case 114502289:
		case 114502290:
		case 114502291:
		case 114602008:
		case 114602009:
		case 114602013:
		case 114602014:
		case 120001720:
		case 120001721:
		case 121001670:
		case 121001671:
		case 122001954:
		case 122001955:
		case 123001704:
		case 123001705:
		case 125005554:
		case 125005555:
		case 125005556:
		case 125005557:
		case 125005558:
		case 125005559:
		case 125005560:
		case 125005561:
		case 125005562:
		case 187000257:
			return true;
		}
		return false;
	}

	/**
	 * 灰狼饰品强化失败不销毁，重置为 +0；不可交易，仅可包装一次。 / - Gray Wolf Accessories will not be destroyed when the upgrading process fails and will reset to +0. - Gray Wolf Accessories cannot be traded and can only be wrapped once when the Upgrade level reaches +10. - Upgrading Gray Wolf Accessories increases PvE abilities. - https://aionpowerbook.com/powerbook/KR_-_Update_September_20th_2017
	 */
	static boolean isGrayWolfAccessories(Item targetItem) {
		switch (targetItem.getItemId()) {
		case 125005563:
		case 125005564:
		case 125005565:
		case 125005566:
		case 125005567:
		case 125005568:
		case 125005569:
		case 125005570:
		case 125005571:
		case 120001718:
		case 120001719:
		case 121001668:
		case 121001669:
		case 122001952:
		case 122001953:
		case 123001702:
		case 123001703:
			return true;
		}
		return false;
	}

	/**
	 * 高阶守护者达努亚改型/重塑装备说明：强化销毁永不。 / - Archdaeva's Reformed Danuar - Archdaeva's Remodeled Danuar - Archdaeva's Restructured Danuar - Destroy Enchant: NEVER!!!
	 */
	static boolean isArchdaevaReformedDanuar(Item targetItem) {
		switch (targetItem.getItemId()) {
		case 110551314:
		case 111501874:
		case 112501810:
		case 113501893:
		case 114501901:
		case 125004548:
			return true;
		}
		return false;
	}

	/**
	 * 是否为执政官改装达努亚系列。
	 * Whether the item is Archdaeva remodeled Danuar gear.
	 *
	 * @param targetItem 目标物品 / target item
	 * @return 是否匹配 / whether matched
	 */
	static boolean isArchdaevaRemodeledDanuar(Item targetItem) {
		switch (targetItem.getItemId()) {
		case 100002007:
		case 100101489:
		case 100201670:
		case 100501447:
		case 100601565:
		case 100901524:
		case 101301408:
		case 101501510:
		case 101701505:
		case 101801340:
		case 101901245:
		case 102001368:
		case 102101183:
		case 110101990:
		case 110301967:
		case 110551315:
		case 110601755:
		case 111101784:
		case 111301906:
		case 111501875:
		case 111601719:
		case 112101729:
		case 112301843:
		case 112501811:
		case 112601700:
		case 113101795:
		case 113301937:
		case 113501894:
		case 113601702:
		case 114101829:
		case 114301974:
		case 114501902:
		case 114601708:
		case 125004547:
		case 125004552:
		case 125004553:
		case 115001961:
			return true;
		}
		return false;
	}

	/**
	 * 是否为执政官重构达努亚系列。
	 * Whether the item is Archdaeva restructured Danuar gear.
	 *
	 * @param targetItem 目标物品 / target item
	 * @return 是否匹配 / whether matched
	 */
	static boolean isArchdaevaRestructuredDanuar(Item targetItem) {
		switch (targetItem.getItemId()) {
		case 100002008:
		case 100101490:
		case 100201671:
		case 100501448:
		case 100601566:
		case 100901525:
		case 101301409:
		case 101501511:
		case 101701506:
		case 101801341:
		case 101901246:
		case 102001369:
		case 102101184:
		case 110101991:
		case 110301966:
		case 110551316:
		case 110601754:
		case 111101785:
		case 111301905:
		case 111501876:
		case 111601718:
		case 112101730:
		case 112301842:
		case 112501812:
		case 112601699:
		case 113101796:
		case 113301936:
		case 113501895:
		case 113601701:
		case 114101830:
		case 114301973:
		case 114501903:
		case 114601707:
		case 125004546:
		case 125004549:
		case 125004550:
		case 125004551:
		case 125004554:
		case 115001962:
			return true;
		}
		return false;
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
