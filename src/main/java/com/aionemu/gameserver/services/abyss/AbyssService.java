package com.aionemu.gameserver.services.abyss;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 欧比斯通用工具：PvP 地图判定与军阶击杀/技能全服广播。
 * skill world announcements.
 */
public class AbyssService {
	private static final int[] abyssMapList = {
			//// ***////
			210020000, // Elten.
			210040000, // Heiron.
			210130000, // Inggison [Master Server].
			210060000, // Theobomos.
			210070000, // Cygnea.
			210090000, // Idian Depths.
			210100000, // Iluma.
			//// ***////
			220020000, // Morheim.
			220040000, // Beluslan.
			220050000, // Brusthonin.
			220140000, // Gelkmaros [Master Server].
			220080000, // Enshar.
			220100000, // Idian Depths.
			220110000, // Norvsvold.
			//// ***////
			400010000, // Reshanta.
			// 帕内斯特拉// / Panesterra//
			400020000, // Belus.
			400040000, // Aspida.
			400050000, // Atanatos.
			400060000, // Disillon.
			//// ***////
			600110000, // Silentera Canyon [Master Server].
			//// 4.7////
			600090000, // Kaldor.
			600100000, // Levinshor.
			//// 5.8////
			600040000, // Tiamaranta's Eye.
			600041000 }; // Tiamaranta's Eye [Master Server].

	/**
	 * 判断玩家是否位于欧比斯/PvP 地图列表中。
	 * Whether the player is on a listed abyss/PvP map.
	 *
	 * 玩家 / Player
	 * @return 在列表内则为 {@code true} / {@code true} if on a listed map
	 */
	public static final boolean isOnPvpMap(Player player) {
		for (int i : abyssMapList) {
			if (i == player.getWorldId()) {
				return true;
			} else {
				continue;
			}
		}
		return false;
	}

	/**
	 * 向同地图其他玩家广播高军阶玩家被击杀。
	 * Announce a high-rank player's death to others on the same map.
	 *
	 * @param victim 被击杀玩家 / Victim player
	 */
	public static final void rankedKillAnnounce(final Player victim) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player p) {
				if (p != victim && victim.getWorldId() == p.getWorldId()) {
					PacketSendUtility.sendPacket(p, SM_SYSTEM_MESSAGE.STR_ABYSS_ORDER_RANKER_DIE(victim,
							AbyssRankEnum.getRankDescriptionId(victim)));
				}
			}
		});
	}

	/**
	 * 向同世界类型非副本玩家广播欧比斯技能释放。
	 * Announce abyss-skill cast to non-instance players of the same world type.
	 *
	 * Caster
	 * @param nameId 技能名称描述 ID / Skill name description id
	 */
	public static final void rankerSkillAnnounce(final Player player, final int nameId) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player p) {
				if (p != player && player.getWorldType() == p.getWorldType() && !p.isInInstance()) {
					PacketSendUtility.sendPacket(p,
							SM_SYSTEM_MESSAGE.STR_SKILL_ABYSS_SKILL_IS_FIRED(player, new DescriptionId(nameId)));
				}
			}
		});
	}
}
