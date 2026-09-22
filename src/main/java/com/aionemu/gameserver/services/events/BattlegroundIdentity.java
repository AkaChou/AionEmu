package com.aionemu.gameserver.services.events;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;

/**
 * 战场身份与外观展示域：按队伍索引与种族解析披风/徽章模板，并处理战场内目标名称脱敏。
 * Battleground identity and cosmetics domain: resolves cloak/emblem templates by team index and
 * race, and handles in-BG target name masking.
 *
 * <p>该类型只服务 {@link LadderService}：全部为静态纯查表/展示函数，不持有状态、不创建对象；
 * 对外仍通过 {@link LadderService} 的原公开方法访问（门面签名不变）。
 * This type only serves {@link LadderService}: every function is a static pure table lookup or
 * display helper without state or allocation. External callers keep using the original
 * {@link LadderService} facade methods with unchanged signatures.</p>
 */
final class BattlegroundIdentity {

	/**
	 * 仅静态展示逻辑，禁止实例化。
	 * Static display logic only; not instantiable.
	 */
	private BattlegroundIdentity() {
	}

	/**
	 * 按战场索引与种族返回伪装斗篷模板 ID。
	 * Returns the disguise cloak template id by BG index and race.
	 *
	 * 玩家 / player
	 * @param bgIndex 战场队伍索引 / battleground team index
	 * cloak template id
	 */
	static int getBgCloak(Player player, int bgIndex) {
		int template;
		switch (bgIndex + 1) {
		case 1:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202617;
			} else {
				template = 202618;
			}
			break;
		case 2:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202603;
			} else {
				template = 202604;
			}
			break;
		case 3:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202539;
			} else {
				template = 202540;
			}
			break;
		case 4:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202583;
			} else {
				template = 202584;
			}
			break;
		case 5:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202529;
			} else {
				template = 202530;
			}
			break;
		case 6:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202531;
			} else {
				template = 202532;
			}
			break;
		case 7:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202537;
			} else {
				template = 202538;
			}
			break;
		case 8:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202526;
			} else {
				template = 202527;
			}
			break;
		case 9:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202541;
			} else {
				template = 202542;
			}
			break;
		case 10:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202543;
			} else {
				template = 202544;
			}
			break;
		case 11:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202551;
			} else {
				template = 202552;
			}
			break;
		case 12:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202561;
			} else {
				template = 202562;
			}
			break;
		case 13:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202571;
			} else {
				template = 202572;
			}
			break;
		case 14:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202573;
			} else {
				template = 202574;
			}
			break;
		case 15:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202575;
			} else {
				template = 202576;
			}
			break;
		case 16:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202579;
			} else {
				template = 202580;
			}
			break;
		case 17:
			if (player.getCommonData().getRace() == Race.ELYOS) {
				template = 202524;
			} else {
				template = 202525;
			}
			break;
		default:
			template = 0;
			break;
		}
		return template;
	}

	/**
	 * 返回战场内目标显示名（可能脱敏为 Contestant）。
	 * Returns the in-BG display name for a target (may be masked as Contestant).
	 *
	 * viewer
	 * target
	 * display name
	 */
	static String getName(Player player, Player target) {
		if (player.isSpectating() || (player.getBattleground() != null && player.getBattleground().isTournament())) {
			return target.getName();
		}
		if (player.getAccessLevel() > 0 || (player.getBattleground() != null && player.getBattleground().is1v1())) {
			return target.getName();
		}
		if (player.isInGroup2() && target.isInGroup2() && player.getPlayerGroup2().getMembers().contains(target)) {
			return target.getName();
		}
		String playerName = "Contestant";
		if (!player.isInGroup2() && !player.isInAlliance2()) {
			playerName += " " + (target.getBgIndex() + 1);
		}
		return playerName;
	}

	/**
	 * 按战场队伍索引返回队伍名称。
	 * Returns the team name for a battleground team index.
	 *
	 * team index
	 * team name
	 */
	static String getNameByIndex(int bgIndex) {
		String name = switch (bgIndex + 1) {
            case 1 -> "Daeva Of Chaos";
            case 2 -> "Until Death";
            case 3 -> "Happy Tiger's";
            case 4 -> "Abyssal Inquin's";
            case 5 -> "Puffy Bear's";
            case 6 -> "Mossy Treant's";
            case 7 -> "Cursed Pirate's";
            case 8 -> "Naughty Kerub's";
            case 9 -> "Tundra Tiger's";
            case 10 -> "Tiger Lover's";
            case 11 -> "Screamer's";
            case 12 -> "Ninja Balaur's";
            case 13 -> "Summer Inquin's";
            case 14 -> "Volcano Inquin's";
            case 15 -> "Savana Inquin's";
            case 16 -> "Tropical Inquin's";
            case 17 -> "Walking Dead";
            default -> "Invalid";
        };
        name += "Team";
		return name;
	}

	/**
	 * 按战场队伍索引返回披风军团徽章样式。
	 * Returns the cape legion-emblem style for a battleground team index.
	 *
	 * team index
	 * emblem
	 */
	static LegionEmblem getCapeEmblemByIndex(int bgIndex) {
		LegionEmblem emblem = new LegionEmblem();
		byte[] uploadData = { 0, 0 };
		switch (bgIndex + 1) {
		case 1:
			emblem.setEmblem(22, 0, 0, 255, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 2:
			emblem.setEmblem(22, 255, 0, 0, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 3:
			emblem.setEmblem(22, 255, 255, 0, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 4:
			emblem.setEmblem(22, 0, 255, 0, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 5:
			emblem.setEmblem(22, 255, 0, 255, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 6:
			emblem.setEmblem(22, 0, 255, 255, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 7:
			emblem.setEmblem(22, 255, 128, 0, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 8:
			emblem.setEmblem(22, 255, 100, 180, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 9:
			emblem.setEmblem(22, 115, 255, 0, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 10:
			emblem.setEmblem(22, 0, 255, 212, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 11:
			emblem.setEmblem(22, 255, 255, 255, LegionEmblemType.DEFAULT, uploadData);
			break;
		case 12:
			emblem.setEmblem(22, 0, 0, 0, LegionEmblemType.DEFAULT, uploadData);
			break;
		default:
			emblem.setEmblem(22, 0, 0, 0, LegionEmblemType.DEFAULT, uploadData);
			break;
		}
		return emblem;
	}

}
