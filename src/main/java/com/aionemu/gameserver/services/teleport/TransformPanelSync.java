package com.aionemu.gameserver.services.teleport;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerTransformDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送后变身面板同步：恢复大天使/副本/活动变形的技能面板关联。
 * Post-teleport transform panel sync: restores archdaeva/instance/event transform skill panels.
 * <p>该类型只服务 {@link TeleportService2}：玩家使用传送/飞行/热点/回程卷轴或管理员传送命令后，
 * 若仍处于变形效果中，必须重发效果关联的技能面板。全部为静态方法，不持有状态；
 * 对外仍通过 {@link TeleportService2} 的原 public static 方法访问（门面签名不变）。
 * This type only serves {@link TeleportService2}: after a teleport/fly/hotspot/return scroll or an
 * admin goto/movetoplayer/movetonpc, the skill panel linked to a still-active transform effect must
 * be re-sent. All static, stateless; external callers keep using the original public static
 * {@link TeleportService2} facade methods with unchanged signatures.</p>
 */
final class TransformPanelSync {

	/**
	 * 仅静态同步逻辑，禁止实例化。
	 * Static sync logic only; not instantiable.
	 */
	private TransformPanelSync() {
	}

	/**
	 * 从数据库加载并同步玩家变形状态到客户端。
	 * Loads and syncs player transformation state to the client.
	 * @param player 玩家 / Player
	 */
	static void loadAndSyncTransformation(Player player) {
		DAOManager.getDAO(PlayerTransformDAO.class).loadPlTransfo(player);
		PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, player.getTransformModel().getPanelId(), true, player.getTransformModel().getItemId()));
	}

	/**
	 * 同步大天使变形技能面板（传送后保持效果关联面板）。
	 * Syncs Archdaeva transform skill panels (keeps effect-linked panels after teleport).
	 * <p>
	 * Archdaeva Transformation 5.1: If a player is under one of the following effects, and uses a
	 * "Teleport/Fly/Hotspot/Return Scroll" or admin command "goto/movetoplayer/movetonpc",
	 * the skill panel linked to this effect must not disappear.
	 * @param player 玩家 / Player
	 */
	static void syncArchdaevaPanels(Player player) {
		if (!player.isInGroup2() || player != null) {
			if (player.getEffectController().hasAbnormalEffect(4752)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(76);
					player.getTransformModel().setItemId(102301000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 76, true, 102301000));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4757)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(77);
					player.getTransformModel().setItemId(102303000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 77, true, 102303000));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4762)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(78);
					player.getTransformModel().setItemId(102302000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 78, true, 102302000));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4768)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(79);
					player.getTransformModel().setItemId(102304000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 79, true, 102304000));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4804)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(76);
					player.getTransformModel().setItemId(102301000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 76, true, 102301000));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4805)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(77);
					player.getTransformModel().setItemId(102303000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 77, true, 102303000));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4806)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(78);
					player.getTransformModel().setItemId(102302000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 78, true, 102302000));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4807)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(79);
					player.getTransformModel().setItemId(102304000);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 79, true, 102304000));
				}
			}
		}
	}

	/**
	 * 同步副本/活动变形技能面板（传送后保持效果关联面板）。
	 * Syncs instance/event transform skill panels (keeps effect-linked panels after teleport).
	 * <p>
	 * Instance + Event Transformation: If a player is under one of the following effects, and uses a
	 * "Teleport/Fly/Hotspot/Return Scroll" or admin command "goto/movetoplayer/movetonpc",
	 * the skill panel linked to this effect must not disappear.
	 * @param player 玩家 / Player
	 */
	static void syncInstanceEventPanels(Player player) {
		if (!player.isInGroup2() || player != null) {
			// 【PvP】竞技场 / [PvP] Arena
			if (player.getEffectController().hasAbnormalEffect(10405)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(15);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 15, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(10406)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(15);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 15, true, 0));
				}
			}
			// 遗忘裂隙 5.1 / Fissure Of Oblivion 5.1
			if (player.getEffectController().hasAbnormalEffect(4829) || player.getEffectController().hasAbnormalEffect(4831) || player.getEffectController().hasAbnormalEffect(4834) || player.getEffectController().hasAbnormalEffect(4835) || player.getEffectController().hasAbnormalEffect(4836)) {
				player.getTransformModel().setPanelId(81);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 81, true, 0));
			}

			if (player.getEffectController().hasAbnormalEffect(4808)) {
				player.getTransformModel().setPanelId(82);
				player.getTransformModel().setItemId(102301000);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 82, true, 102301000));
			}

			if (player.getEffectController().hasAbnormalEffect(4813)) {
				player.getTransformModel().setPanelId(83);
				player.getTransformModel().setItemId(102303000);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 83, true, 102303000));
			}

			if (player.getEffectController().hasAbnormalEffect(4818)) {
				player.getTransformModel().setPanelId(84);
				player.getTransformModel().setItemId(102302000);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 84, true, 102302000));
			}

			if (player.getEffectController().hasAbnormalEffect(4824)) {
				player.getTransformModel().setPanelId(85);
				player.getTransformModel().setItemId(102304000);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 85, true, 102304000));
			}
			// 阿图拉姆天空要塞 4.8 / Aturam Sky Fortress 4.8
			if (player.getEffectController().hasAbnormalEffect(21807)) {
				player.getTransformModel().setPanelId(61);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 61, true, 0));
			}

			if (player.getEffectController().hasAbnormalEffect(21808)) {
				player.getTransformModel().setPanelId(62);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 62, true, 0));
			}
			// 永恒摇篮 5.1 / Cradle Of Eternity 5.1
			if (player.getEffectController().hasAbnormalEffect(21340)) {
				player.getTransformModel().setPanelId(71);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 71, true, 0));
			}
			// 术古皇陵 4.3 / Shugo Imperial Tomb 4.3
			if (player.getEffectController().hasAbnormalEffect(21096)) {
				player.getTransformModel().setPanelId(27);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 27, true, 0));
			}
			// 提亚马特要塞 3.5 / Tiamat Stronghold 3.5
			if (player.getEffectController().hasAbnormalEffect(20865)) {
				player.getTransformModel().setPanelId(17);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 17, true, 0));
			}
			// 污染地下通道 5.1 / Contaminated Underpath 5.1
			if (player.getEffectController().hasAbnormalEffect(21345)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(68);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 68, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21346)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(68);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 68, true, 0));
				}
			}
			// [活动] 污染地下通道 5.6 / [Event] Contaminated Underpath 5.6
			if (player.getEffectController().hasAbnormalEffect(4935)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(120);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 120, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4936)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(121);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 121, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4937)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(122);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 122, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4938)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(123);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 123, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4939)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(124);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 124, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4940)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(120);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 120, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4941)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(121);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 121, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4942)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(122);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 122, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4943)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(123);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 123, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(4944)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(124);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 124, true, 0));
				}
			}
			// 秘密军需工厂 5.1 / Secret Munitions Factory 5.1
			if (player.getEffectController().hasAbnormalEffect(21347)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(69);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 69, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21348)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(69);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 69, true, 0));
				}
			}
			// 被占领的伦图斯基地 4.8 与陨落波埃塔 5.1 / Occupied Rentus Base 4.8 & Fallen Poeta 5.1
			if (player.getEffectController().hasAbnormalEffect(21805)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(63);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 63, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21806)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(63);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 63, true, 0));
				}
			}
			// 闷燃火神殿 5.1 / Smoldering Fire Temple 5.1
			if (player.getEffectController().hasAbnormalEffect(21375)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(72);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 72, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21376)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(73);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 73, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21377)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(74);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 74, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21378)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(72);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 72, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21379)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(73);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 73, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21380)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(74);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 74, true, 0));
				}
			}
			// 奥菲丹战道 5.1 / Ophidan Warpath 5.1
			if (player.getEffectController().hasAbnormalEffect(21336)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(70);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 70, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21337)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(70);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 70, true, 0));
				}
			}
			// 光明方尖碑与【炼狱】光明方尖碑 4.7 / Illuminary Obelisk & [Infernal] Illuminary Obelisk 4.7
			if (player.getEffectController().hasAbnormalEffect(21511)) {
				player.getTransformModel().setPanelId(51);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 51, true, 0));
			}
			// 永恒堡垒 4.3 / The Eternal Bastion 4.3
			if (player.getEffectController().hasAbnormalEffect(21065)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(20);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 20, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21066)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(20);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 20, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21141)) {
				player.getTransformModel().setPanelId(31);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 31, true, 0));
			}
			// 梦魇马戏团 4.3 / Nightmare Circus 4.3
			if (player.getEffectController().hasAbnormalEffect(21469)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(38);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 38, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21470)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(39);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 39, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21471)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(38);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 38, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21472)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(39);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 39, true, 0));
				}
			}
			// 特兰西迪姆附楼 4.7.5 / Transidium Annex 4.7.5
			if (player.getEffectController().hasAbnormalEffect(21728) || player.getEffectController().hasAbnormalEffect(21729) || player.getEffectController().hasAbnormalEffect(21730) || player.getEffectController().hasAbnormalEffect(21731)) {
				player.getTransformModel().setPanelId(55);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 55, true, 0));
			}

			if (player.getEffectController().hasAbnormalEffect(21579) || player.getEffectController().hasAbnormalEffect(21586) || player.getEffectController().hasAbnormalEffect(21587) || player.getEffectController().hasAbnormalEffect(21588)) {
				player.getTransformModel().setPanelId(56);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 56, true, 0));
			}

			if (player.getEffectController().hasAbnormalEffect(21582) || player.getEffectController().hasAbnormalEffect(21589) || player.getEffectController().hasAbnormalEffect(21590) || player.getEffectController().hasAbnormalEffect(21591)) {
				player.getTransformModel().setPanelId(57);
				PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 57, true, 0));
			}
			// 术古皇帝宝库 4.7.5 / The Shugo Emperor Vault 4.7.5
			// 皇帝特里利伦克保险箱 4.9.1 / Emperor Trillirunerk Safe 4.9.1
			if (player.getEffectController().hasAbnormalEffect(21829)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(64);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 64, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21830)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(65);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 65, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21831)) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					player.getTransformModel().setPanelId(66);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 66, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21832)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(64);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 64, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21833)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(65);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 65, true, 0));
				}
			}

			if (player.getEffectController().hasAbnormalEffect(21834)) {
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
					player.getTransformModel().setPanelId(66);
					PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 66, true, 0));
				}
			}
		}
	}

}
