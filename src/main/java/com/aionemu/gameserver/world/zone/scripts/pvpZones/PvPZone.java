package com.aionemu.gameserver.world.zone.scripts.pvpZones;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.SiegeZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;
import com.aionemu.gameserver.world.zone.handler.AdvencedZoneHandler;

/**
 * PvP 区域脚本基类：处理玩家死亡播报、延迟复活与传送。
 * Base PvP zone script: handles player-death broadcast, delayed revive, and teleport.
 */
public abstract class PvPZone implements AdvencedZoneHandler {

	/**
	 * 进入 PvP 区：默认无操作。
	 * Enter PvP zone: no-op by default.
	 *
	 * @param player 进入区域的生物 / creature entering the zone
	 * @param zone 区域实例 / zone instance
	 */
	@Override
	public void onEnterZone(Creature player, ZoneInstance zone) {
	}

	/**
	 * 离开 PvP 区：默认无操作。
	 * Leave PvP zone: no-op by default.
	 *
	 * @param player 离开区域的生物 / creature leaving the zone
	 * @param zone 区域实例 / zone instance
	 */
	@Override
	public void onLeaveZone(Creature player, ZoneInstance zone) {
	}

	/**
	 * 玩家在 PvP 区内死亡：广播死亡、通知区内玩家，并在延迟后复活传送。
	 * Player dies inside PvP zone: broadcast death, notify zone players, then revive and teleport after delay.
	 *
	 * @param lastAttacker 最后攻击者 / last attacker
	 * @param target 死亡目标 / dead target
	 * @param zone 区域实例 / zone instance
	 * @return 是否已处理 / whether handled
	 */
	@Override
	public boolean onDie(final Creature lastAttacker, Creature target, final ZoneInstance zone) {
		if (!(target instanceof Player)) {
			return false;
		}
		final Player player = (Player) target;
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()), true);
		if (zone instanceof SiegeZoneInstance) {
			((SiegeZoneInstance) zone).doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player p) {
					PacketSendUtility.sendPacket(p, SM_SYSTEM_MESSAGE.STR_PvPZONE_OUT_MESSAGE(player.getName()));
				}
			});
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					PlayerReviveService.duelRevive(player);
					doTeleport(player, zone.getZoneTemplate().getName());
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_PvPZONE_MY_DEATH_TO_B(lastAttacker.getName()));
				}
			}, 5000);
		}
		return true;
	}

	/**
	 * 子类实现：将玩家传送到区域对应复活点。
	 * Implemented by subclasses: teleport the player to the zone-specific revive point.
	 *
	 * @param player 玩家 / player
	 * @param zoneName 区域名称 / zone name
	 */
	protected abstract void doTeleport(Player player, ZoneName zoneName);
}
