package com.aionemu.gameserver.services.player;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;
import com.aionemu.gameserver.model.templates.revive_start_points.InstanceReviveStartPoints;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.model.TeleportAnimation;

/**
 * 玩家复活服务，处理决斗/技能/绑点/基斯克等复活路径。
 * Player revive service handling duel/skill/bind/kisk and other revive paths.
 */
public class PlayerReviveService {
	/**
	 * 决斗复活。
	 * Duel revive.
	 *
	 * @param player 玩家 / player
	 */
	public static final void duelRevive(Player player) {
		revive(player, 25, 25, false, 0);
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		player.unsetResPosState();
	}

	/**
	 * 技能复活。
	 * Skill revive.
	 *
	 * @param player 玩家 / player
	 */
	public static final void skillRevive(Player player) {
		revive(player, 25, 25, true, player.getResurrectionSkill());
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		if (player.getIsFlyingBeforeDeath()) {
			player.setState(CreatureState.FLYING);
		}
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		}
		if (player.isInResPostState()) {
			TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getResPosX(),
					player.getResPosY(), player.getResPosZ());
		}
		player.unsetResPosState();
		player.setIsFlyingBeforeDeath(false);
	}

	/**
	 * 重生复活。
	 * Rebirth revive.
	 *
	 * @param player 玩家 / player
	 */
	public static final void rebirthRevive(Player player) {
		if (!player.canUseRebirthRevive()) {
			return;
		}
		if (player.getRebirthResurrectPercent() <= 0) {
			player.setRebirthResurrectPercent(5);
		}
		boolean soulSickness = true;
		int rebirthResurrectPercent = player.getRebirthResurrectPercent();
		if (player.getAccessLevel() >= AdminConfig.ADMIN_AUTO_RES) {
			rebirthResurrectPercent = 100;
			soulSickness = false;
		}
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		revive(player, rebirthResurrectPercent, rebirthResurrectPercent, soulSickness, player.getRebirthSkill());
		if (player.getIsFlyingBeforeDeath()) {
			player.setState(CreatureState.FLYING);
		}
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		}
		player.unsetResPosState();
		player.setIsFlyingBeforeDeath(false);
	}

	/**
	 * 绑点复活。
	 * Bind-point revive.
	 *
	 * @param player 玩家 / player
	 */
	public static final void bindRevive(Player player) {
		bindRevive(player, 0);
	}

	/**
	 * 绑点复活。
	 * Bind-point revive.
	 *
	 * 玩家 / player
	 * skillId
	 */
	public static final void bindRevive(Player player, int skillId) {
		revive(player, 25, 25, true, skillId);
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
		PacketSendUtility.sendPacket(player,
				new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		} else {
			boolean isInvadeActiveVortex = false;
			for (VortexLocation loc : GameLocationBootstrapServices.vortexService().getVortexLocations().values()) {
				isInvadeActiveVortex = loc.isInsideActiveVortex(player)
						&& player.getRace().equals(loc.getInvadersRace());
				if (isInvadeActiveVortex) {
					TeleportService2.teleportTo(player, loc.getResurrectionPoint());
				}
			}
			if (!isInvadeActiveVortex) {
				TeleportService2.moveToBindLocation(player, true);
			}
		}
		player.unsetResPosState();
	}

	/**
	 * 基斯克复活。
	 * Kisk revive.
	 *
	 * @param player 玩家 / player
	 */
	public static final void kiskRevive(Player player) {
		kiskRevive(player, 0);
	}

	/**
	 * 基斯克复活。
	 * Kisk revive.
	 *
	 * 玩家 / player
	 * skillId
	 */
	public static final void kiskRevive(Player player, int skillId) {
		Kisk kisk = player.getKisk();
		if (kisk == null) {
			bindRevive(player);
			return;
		}
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		} else if (kisk.isActive()) {
			WorldPosition bind = kisk.getPosition();
			kisk.resurrectionUsed();
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
			revive(player, 25, 25, false, skillId);
			player.getController().startProtectionActiveTask();
			player.setPortAnimation(4);
			if (player.getIsFlyingBeforeDeath()) {
				player.getFlyController().startFly();
			}
			player.getGameStats().updateStatsAndSpeedVisually();
			player.unsetResPosState();
			TeleportService2.moveToKiskLocation(player, bind);
		}
	}

	/**
	 * 副本复活。
	 * Instance revive.
	 *
	 * @param player 玩家 / player
	 */
	public static final void instanceRevive(Player player) {
		instanceRevive(player, 0);
	}

	/**
	 * 副本复活。
	 * Instance revive.
	 *
	 * 玩家 / player
	 * skillId
	 */
	public static final void instanceRevive(Player player, int skillId) {
		if (player.getPosition().getWorldMapInstance().getInstanceHandler().onReviveEvent(player)) {
			return;
		}
		WorldMap map = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(player.getWorldId());
		if (map == null) {
			bindRevive(player);
			return;
		}
		revive(player, 25, 25, true, skillId);
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
		PacketSendUtility.sendPacket(player,
				new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
		InstanceReviveStartPoints revivePoint = TeleportService2.getReviveInstanceStartPoints(map.getMapId());
		if (map.isInstanceType() && revivePoint != null) {
			TeleportService2.teleportTo(player, revivePoint.getReviveWorld(), revivePoint.getX(), revivePoint.getY(),
					revivePoint.getZ(), revivePoint.getH(), TeleportAnimation.NO_ANIMATION);
		} else {
			bindRevive(player);
		}
		player.unsetResPosState();
	}

	/**
	 * 执行复活。
	 * Performs revive.
	 *
	 * 玩家 / player
	 * @param hpPercent 生命百分比 / hpPercent
	 * @param mpPercent 魔法百分比 / mpPercent
	 * @param setSoulsickness 是否设置灵魂病 / setSoulsickness
	 * resurrectionSkill
	 */
	public static final void revive(final Player player, int hpPercent, int mpPercent, boolean setSoulsickness,
			int resurrectionSkill) {
		player.getKnownList().doOnAllPlayers(new Visitor<Player>() {
			@Override
			/**
			 * visit 方法。
			 * visit method.
			 *
			 * visitor
			 */
			public void visit(Player visitor) {
				VisibleObject target = visitor.getTarget();
				if (target != null && target.getObjectId() == player.getObjectId()
						&& (visitor.getRace() != player.getRace())) {
					visitor.setTarget(null);
					PacketSendUtility.sendPacket(visitor, new SM_TARGET_SELECTED(null));
				}
			}
		});
		boolean isNoResurrectPenalty = player.getController().isNoResurrectPenaltyInEffect();
		player.getMoveController().stopFalling();
		player.setPlayerResActivate(false);
		player.getLifeStats().setCurrentHpPercent(isNoResurrectPenalty ? 100 : hpPercent);
		player.getLifeStats().setCurrentMpPercent(isNoResurrectPenalty ? 100 : mpPercent);
		if (player.getCommonData().getDp() > 0 && !isNoResurrectPenalty) {
			player.getCommonData().setDp(0);
		}
		player.getLifeStats().triggerRestoreOnRevive();
		if (!isNoResurrectPenalty && setSoulsickness) {
			player.getController().updateSoulSickness(resurrectionSkill);
		}
		if (player.getResurrectionSkill() > 0) {
			player.setResurrectionSkill(0);
		}
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		player.getAggroList().clear();
		player.getController().onBeforeSpawn(false);
		if (player.isInGroup2()) {
			PlayerGroupService.updateGroup(player, GroupEvent.MOVEMENT);
		}
		if (player.isInAlliance2()) {
			PlayerAllianceService.updateAlliance(player, PlayerAllianceEvent.MOVEMENT);
		}
	}

	/**
	 * 道具自我复活。
	 * Item self-revive.
	 *
	 * @param player 玩家 / player
	 */
	public static final void itemSelfRevive(Player player) {
		Item item = player.getSelfRezStone();
		if (item == null && player.getAccessLevel() == 0) {
			cancelRes(player);
			return;
		}
		ItemUseLimits useLimits = item.getItemTemplate().getUseLimits();
		int useDelay = useLimits.getDelayTime();
		player.addItemCoolDown(useLimits.getDelayId(), System.currentTimeMillis() + useDelay, useDelay / 1000);
		player.getController().cancelUseItem();
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), item.getObjectId(),
				item.getItemTemplate().getTemplateId()), true);
		if (!player.getInventory().decreaseByObjectId(item.getObjectId(), 1)) {
			cancelRes(player);
			return;
		}
		revive(player, 25, 25, true, player.getResurrectionSkill());
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		if (player.getIsFlyingBeforeDeath()) {
			player.setState(CreatureState.FLYING);
		}
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		}
		player.unsetResPosState();
		player.setIsFlyingBeforeDeath(false);
	}

	/**
	 * banditRevive 方法。
	 * banditRevive method.
	 *
	 * @param player 玩家 / player
	 */
	public static final void banditRevive(Player player) {
		revive(player, 100, 100, false, 0);
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		if (player.getIsFlyingBeforeDeath()) {
			player.setState(CreatureState.FLYING);
		}
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, true));
		PacketSendUtility.sendPacket(player,
				new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		if (player.isInResPostState()) {
			TeleportService2.teleportTo(player, player.getWorldId(), player.getResPosX(), player.getResPosY(),
					player.getResPosZ());
		}
		player.unsetResPosState();
		player.setIsFlyingBeforeDeath(false);
	}

	/**
	 * ffaRevive 方法。
	 * ffaRevive method.
	 *
	 * @param player 玩家 / player
	 */
	public static final void ffaRevive(Player player) {
		revive(player, 100, 100, false, 0);
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		if (player.getIsFlyingBeforeDeath()) {
			player.setState(CreatureState.FLYING);
		}
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, true));
		PacketSendUtility.sendPacket(player,
				new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		if (player.isInResPostState()) {
			TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getResPosX(),
					player.getResPosY(), player.getResPosZ());
		}
		player.unsetResPosState();
		player.setIsFlyingBeforeDeath(false);
	}

	/**
	 * bgRevive 方法。
	 * bgRevive method.
	 *
	 * @param player 玩家 / player
	 */
	public static final void bgRevive(Player player) {
		revive(player, 100, 100, false, player.getResurrectionSkill());
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		if (player.getIsFlyingBeforeDeath()) {
			player.setState(CreatureState.FLYING);
		}
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, true));
		PacketSendUtility.sendPacket(player,
				new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		player.getGameStats().updateStatsAndSpeedVisually();
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		}
		if (player.isInResPostState()) {
			TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getResPosX(),
					player.getResPosY(), player.getResPosZ());
		}
		player.unsetResPosState();
		player.setIsFlyingBeforeDeath(false);
	}

	/**
	 * eventRevive 方法。
	 * eventRevive method.
	 *
	 * @param player 玩家 / player
	 */
	public static final void eventRevive(Player player) {
		revive(player, 25, 25, false, 0);
		player.getController().startProtectionActiveTask();
		player.setPortAnimation(4);
		if (player.getIsFlyingBeforeDeath()) {
			player.setState(CreatureState.FLYING);
		}
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.RESURRECT), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		player.getGameStats().updateStatsAndSpeedVisually();
		if (player.getIsFlyingBeforeDeath()) {
			player.getFlyController().startFly();
		}
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		}
		if (player.isInResPostState()) {
			TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getResPosX(),
					player.getResPosY(), player.getResPosZ());
		}
		player.unsetResPosState();
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, true));
		PacketSendUtility.sendPacket(player,
				new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
		player.setIsFlyingBeforeDeath(false);
	}

	/**
	 * startPositionRevive 方法。
	 * startPositionRevive method.
	 *
	 * @param player 玩家 / player
	 */
	public static final void startPositionRevive(Player player) {
		startPositionRevive(player, 0);
	}

	/**
	 * startPositionRevive 方法。
	 * startPositionRevive method.
	 *
	 * 玩家 / player
	 * skillId
	 */
	public static final void startPositionRevive(Player player, int skillId) {
		revive(player, 25, 25, true, skillId);
		player.setPortAnimation(4);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		player.getGameStats().updateStatsAndSpeedVisually();
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
		PacketSendUtility.sendPacket(player,
				new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
		if (player.isInPrison()) {
			TeleportService2.teleportToPrison(player);
		} else {
			TeleportService2.teleportWorldStartPoint(player, player.getWorldId());
		}
		player.unsetResPosState();
	}

	private static final void cancelRes(Player player) {
		AuditLogger.info(player, "Possible selfres hack.");
		player.getController().sendDie();
	}
}