package com.aionemu.gameserver.services.events;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ArmorType;

/**
 * 外观展示服务，管理玩家展示用外观状态。
 * Display service managing player cosmetic/display appearance state.
 *
 * @author Rinzler (Encom)
 */

public class DisplayService {
	/**
	 * getDisplayTemplate 方法。
	 * getDisplayTemplate method.
	 *
	 * 玩家 / player
	 * item
	 * result
	 */
	public static int getDisplayTemplate(Player player, Item item) {
		if (player.isBandit() || player.isFFA()) {
			if (item.getItemTemplate().isWeapon()) {
				switch (item.getItemTemplate().getWeaponType()) {
				case SWORD_1H: // 荣耀无界长剑 / Boundless Long Sword Of Glory.
					return 100002013;
				case MACE_1H: // 荣耀无界锤 / Boundless Mace Of Glory.
					return 100101495;
				case DAGGER_1H: // 荣耀无界匕首 / Boundless Dagger Of Glory.
					return 100201676;
				case ORB_2H: // 荣耀无界宝珠 / Boundless Orb Of Glory.
					return 100501453;
				case BOOK_2H: // 荣耀无界魔法书 / Boundless Spellbook Of Glory.
					return 100601571;
				case SWORD_2H: // 荣耀无界巨剑 / Boundless Great Sword Of Glory.
					return 100901530;
				case POLEARM_2H: // 荣耀无界长柄武器 / Boundless Polearm Of Glory.
					return 101301414;
				case STAFF_2H: // 荣耀无界法杖 / Boundless Staff Of Glory.
					return 101501516;
				case BOW: // 荣耀无界弓 / Boundless Bow Of Glory.
					return 101701511;
				case GUN_1H: // 荣耀无界魔法枪 / Boundless Magic Gun Of Glory.
					return 101801346;
				case CANNON_2H: // 荣耀无界魔法炮 / Boundless Magic Cannon Of Glory.
					return 101901251;
				case HARP_2H: // 荣耀无界弦乐器 / Boundless String Instrument Of Glory.
					return 102001374;
				case KEYBLADE_2H: // 荣耀无界钥匙剑 / Boundless Keyblade Of Glory.
					return 102101189;
				default:
					return 100002013; // 默认值 / is by default.
				}
			} else if (player.isFFA() && item.getEquipmentSlot() == 8) { // 行刑者服装 / Executioner's Outfit.
				return 110901014;
			} else if (player.isFFA() && item.getEquipmentSlot() == 4) { // 行刑者面具 / Executioner's Mask.
				return 125045594;
			} else if (item.getItemTemplate().getArmorType() == ArmorType.SHIELD) {
				return 115001971; // 荣耀无界盾牌 / Boundless Shield Of Glory.
			} else if (item.getEquipmentSlot() == 8 && player.getBattleground() != null) {
				if (player.getRace() == Race.ELYOS) {
					return 110101255; // 精英军团制服 / Elite Legion Uniform.
				} else {
					return 110101257; // 精英军团制服 / Elite Legion Uniform.
				}
			} else {
				return item.getItemSkinTemplate().getTemplateId();
			}
		} else {
			return item.getItemSkinTemplate().getTemplateId();
		}
	}

	/**
	 * getDisplayName 方法。
	 * getDisplayName method.
	 *
	 * 玩家 / player
	 * result
	 */
	public static String getDisplayName(Player player) {
		if (player.isBandit()) {
			return "[PK] Bandit";
		} else if (player.isFFA()) {
			return "Opponent";
		} else if (player.getBattleground() != null) {
			return player.getPlayerClass().name();
		} else {
			return player.getName();
		}
	}

	/**
	 * getDisplayLegionName 方法。
	 * getDisplayLegionName method.
	 *
	 * 玩家 / player
	 * result
	 */
	public static String getDisplayLegionName(Player player) {
		if (player.isBandit()) {
			return "Wanted";
		} else if (player.isFFA()) {
			return "Free For All";
		} else {
			return player.getLegion().getLegionName();
		}
	}
}