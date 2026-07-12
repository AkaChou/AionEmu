package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CONQUEROR_PROTECTOR;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.conquerors.Conqueror;
import com.aionemu.gameserver.services.conquerors.ConquerorBuffs;
import com.aionemu.gameserver.services.protectors.Protector;
import com.aionemu.gameserver.services.protectors.ProtectorBuffs;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 守护者与征服者排行服务，管理跨图杀戮等级、增益与图标同步。
 * Protector and Conqueror ranking service managing cross-world kill ranks, buffs, and icon sync.
 */
@Slf4j
public class ProtectorConquerorService {
	private static volatile ObjectProvider<ProtectorConquerorService> instanceProvider;

	private Map<Integer, Protector> protectors = new ConcurrentHashMap<Integer, Protector>();
	private Map<Integer, Conqueror> conquerors = new ConcurrentHashMap<Integer, Conqueror>();

	private Map<Integer, Map<Integer, Player>> worldConqueror = new ConcurrentHashMap<Integer, Map<Integer, Player>>();
	private Map<Integer, Map<Integer, Player>> worldProtectors = new ConcurrentHashMap<Integer, Map<Integer, Player>>();

	private static final Map<Integer, WorldType> handledWorlds = new ConcurrentHashMap<Integer, WorldType>();
	private Future<?> refreshTask;
	private ProtectorBuffs protectorBuff;
	private ConquerorBuffs conquerorBuff;;

	/**
	 * 世界归属类型：魔族/天族/双方。
	 * Elyos / both.
	 */
	public enum WorldType {
		ASMODIANS, ELYOS, USEALL;
	}

	/**
	 * 初始化守护者/征服者系统与刷新任务。
	 * Initializes the protector/conqueror system and refresh task.
	 */
	public synchronized void initSystem() {
		log.info(I18n.get("log.efc341530358"));
		if (refreshTask != null) {
			refreshTask.cancel(false);
			refreshTask = null;
		}
		handledWorlds.clear();
		if (!CustomConfig.PROTECTOR_CONQUEROR_ENABLE) {
			return;
		}
		for (String world : CustomConfig.PROTECTOR_CONQUEROR_WORLDS.split(",")) {
			if ("".equals(world))
				break;
			int worldId = Integer.parseInt(world);
			int worldType = Integer.parseInt(String.valueOf(world.charAt(1)));
			protectorBuff = new ProtectorBuffs();
			conquerorBuff = new ConquerorBuffs();
			WorldType type = worldType > 0 ? worldType > 1 ? WorldType.ASMODIANS : WorldType.ELYOS : WorldType.USEALL;
			handledWorlds.put(worldId, type);
		}
			refreshTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					decayProtectorConquerorRanks();
				}
			}, CustomConfig.PROTECTOR_CONQUEROR_REFRESH * 60000, CustomConfig.PROTECTOR_CONQUEROR_REFRESH * 60000);
		}

	void decayProtectorConquerorRanks() {
		for (Iterator<Map.Entry<Integer, Protector>> iterator = protectors.entrySet().iterator(); iterator.hasNext();) {
			Protector info = iterator.next().getValue();
			if (info.victims > 0 && !isEnemyWorld(info.getOwner())) {
				info.victims -= CustomConfig.PROTECTOR_CONQUEROR_DECREASE;
				int newRank = getRanks(info.victims);
				if (info.getRank() != newRank) {
					info.setRank(newRank);
					PacketSendUtility.sendPacket(info.getOwner(), new SM_CONQUEROR_PROTECTOR(true, info.getRank()));
				}
				if (info.victims < 1) {
					info.victims = 0;
					iterator.remove();
				}
			}
		}
		for (Iterator<Map.Entry<Integer, Conqueror>> iterator = conquerors.entrySet().iterator(); iterator.hasNext();) {
			Conqueror info = iterator.next().getValue();
			if (info.victims > 0 && !isEnemyWorld(info.getOwner())) {
				info.victims -= CustomConfig.PROTECTOR_CONQUEROR_DECREASE;
				int newRank = getRanks(info.victims);
				if (info.getRank() != newRank) {
					info.setRank(newRank);
					PacketSendUtility.sendPacket(info.getOwner(), new SM_CONQUEROR_PROTECTOR(true, info.getRank()));
				}
				if (info.victims < 1) {
					info.victims = 0;
					iterator.remove();
				}
			}
		}
	}

	/**
	 * 获取指定世界中的守护者玩家映射。
	 * Returns the protector player map for the given world.
	 *
	 * 世界 ID / world id
	 * player map
	 */
	public Map<Integer, Player> getWorldProtector(int worldId) {
		return worldProtectors.computeIfAbsent(worldId, id -> new ConcurrentHashMap<Integer, Player>());
	}

	/**
	 * 获取指定世界中的征服者玩家映射。
	 * Returns the conqueror player map for the given world.
	 *
	 * 世界 ID / world id
	 * player map
	 */
	public Map<Integer, Player> getWorldConqueror(int worldId) {
		return worldConqueror.computeIfAbsent(worldId, id -> new ConcurrentHashMap<Integer, Player>());
	}

	private List<Player> playersSnapshot(Map<Integer, Player> players) {
		return new ArrayList<Player>(players.values());
	}

	/**
	 * 玩家登录时恢复守护者/征服者信息。
	 * Restores protector/conqueror info when a player logs in.
	 *
	 * 玩家 / player
	 */
	public void onProtectorConquerorLogin(Player player) {
		if (!CustomConfig.PROTECTOR_CONQUEROR_ENABLE) {
			return;
		}
		if (protectors.containsKey(player.getObjectId())) {
			player.setProtectorInfo(protectors.get(player.getObjectId()));
			player.getProtectorInfo().refreshOwner(player);
		}
		if (conquerors.containsKey(player.getObjectId())) {
			player.setConquerorInfo(conquerors.get(player.getObjectId()));
			player.getConquerorInfo().refreshOwner(player);
		}
	}

	/**
	 * 玩家登出时清理地图内状态。
	 * Clears map-related state when a player logs out.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogout(Player player) {
		if (!CustomConfig.PROTECTOR_CONQUEROR_ENABLE) {
			return;
		}
		onLeaveMap(player);
	}

	/**
	 * 进入受管地图时应用守护者/征服者状态与广播。
	 * Applies protector/conqueror state and broadcasts on entering a handled map.
	 *
	 * 玩家 / player
	 */
	public void onEnterMap(final Player player) {
		if (!CustomConfig.PROTECTOR_CONQUEROR_ENABLE) {
			return;
		}
		int worldId = player.getWorldId();
		Protector info = player.getProtectorInfo();
		Conqueror infoConqueror = player.getConquerorInfo();
		if (!isHandledWorld(worldId)) {
			return;
		}
		if (!isEnemyWorld(player)) { // Protector.
			int objId = player.getObjectId();
			info.setRank(1);
			if (info.getRank() >= 1) {
				// 你现已成为守护者。 / You are now a Protector.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GUARD_UP_1LEVEL);
			}
			if (info.getRank() >= 2) {
				// 你现已成为不屈守护者。 / You are now an Indomitable Protector.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GUARD_UP_2LEVEL);
			}
			if (info.getRank() >= 3) {
				// 你现已成为英勇守护者。 / You are now a Valiant Protector.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GUARD_UP_3LEVEL);
			}
			PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(false, info.getRank()));
			final Map<Integer, Player> world = getWorldProtector(worldId);
			if (!world.containsKey(objId)) {
				world.put(objId, player);
			}
			protectorBuff.applyRankEffect(player, info.getRank());
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).getWorldMapInstanceById(player.getInstanceId())
					.doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player victim) {
							if (!player.getRace().equals(victim.getRace())) {
								PacketSendUtility.sendPacket(victim, new SM_CONQUEROR_PROTECTOR(playersSnapshot(world)));
							}
						}
					});
		} else if (isEnemyWorld(player)) { // Conqueror.
			int objId = player.getObjectId();
			infoConqueror.setRank(1);
			if (infoConqueror.getRank() >= 1) {
				// 你现已成为征服者。 / You are now a Conqueror.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_SLAYER_UP_1LEVEL);
			}
			if (infoConqueror.getRank() >= 2) {
				// 你现已成为狂怒征服者。 / You are now an Furious Conqueror.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_SLAYER_UP_2LEVEL);
			}
			if (infoConqueror.getRank() >= 3) {
				// 你现已成为狂暴征服者。 / You are now a Berserk Conqueror.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_SLAYER_UP_3LEVEL);
			}
			PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(false, infoConqueror.getRank()));
			final Map<Integer, Player> world = getWorldConqueror(worldId);
			if (!world.containsKey(objId)) {
				world.put(objId, player);
			}
			conquerorBuff.applyEffect(player, infoConqueror.getRank());
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).getWorldMapInstanceById(player.getInstanceId())
					.doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player victim) {
							if (!player.getRace().equals(victim.getRace())) {
								PacketSendUtility.sendPacket(victim, new SM_CONQUEROR_PROTECTOR(playersSnapshot(world)));
							}
						}
					});
		} else {
			PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(playersSnapshot(getWorldProtector(worldId))));
			PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(playersSnapshot(getWorldConqueror(worldId))));
		}
		player.clearKnownlist();
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_PLAYER_INFO(player, false));
		player.updateKnownlist();
	}

	/**
	 * 离开受管地图时移除状态并通知敌对阵营。
	 * Removes state and notifies the enemy race when leaving a handled map.
	 *
	 * @param player 玩家 / player
	 */
	public void onLeaveMap(Player player) {
		int worldId = player.getWorldId();
		if (!isHandledWorld(worldId)) {
			return;
		}
		if (!isEnemyWorld(player)) { // Protector.
			Protector info = player.getProtectorInfo();
			List<Player> kill = new ArrayList<Player>();
			Map<Integer, Player> guards = getWorldProtector(worldId);
			kill.addAll(playersSnapshot(guards));
			guards.remove(player.getObjectId());
			if (info.getRank() > 0) {
				info.setRank(0);
				protectorBuff.endEffect(player);
				for (Player victim : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId)
						.getWorldMapInstanceById(player.getInstanceId()).getPlayersInside()) {
					if (!player.getRace().equals(victim.getRace())) {
						PacketSendUtility.sendPacket(victim, new SM_CONQUEROR_PROTECTOR(kill));
					}
				}
			}
		} else if (isEnemyWorld(player)) { // Conqueror.
			Conqueror info = player.getConquerorInfo();
			List<Player> kill = new ArrayList<Player>();
			Map<Integer, Player> killers = getWorldConqueror(worldId);
			kill.addAll(playersSnapshot(killers));
			killers.remove(player.getObjectId());
			if (info.getRank() > 0) {
				info.setRank(0);
				conquerorBuff.endEffect(player);
				for (Player victim : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId)
						.getWorldMapInstanceById(player.getInstanceId()).getPlayersInside()) {
					if (!player.getRace().equals(victim.getRace())) {
						PacketSendUtility.sendPacket(victim, new SM_CONQUEROR_PROTECTOR(kill));
					}
				}
			}
		}
	}

	/**
	 * 向玩家同步当前世界的守护者/征服者图标。
	 * Syncs protector/conqueror icons of the current world to the player.
	 *
	 * 玩家 / player
	 */
	public void updateIcons(Player player) {
		if (!isEnemyWorld(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_CONQUEROR_PROTECTOR(playersSnapshot(getWorldProtector(player.getWorldId()))));
		} else if (isEnemyWorld(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_CONQUEROR_PROTECTOR(playersSnapshot(getWorldConqueror(player.getWorldId()))));
		}
	}

	/**
	 * 根据击杀更新杀手的守护者/征服者等级。
	 * Updates the killer's protector/conqueror rank based on the kill.
	 *
	 * killer
	 * victim
	 */
	public void updateRanks(final Player killer, Player victim) {
		if (!isEnemyWorld(killer)) { // Protector.
			Protector info = killer.getProtectorInfo();
			if (killer.getLevel() >= victim.getLevel() + CustomConfig.PROTECTOR_CONQUEROR_LEVEL_DIFF) {
				int rank = getRanks(++info.victims);
				if (info.getRank() >= 1) {
					// 你现已成为守护者。 / You are now a Protector.
					PacketSendUtility.sendPacket(killer, SM_SYSTEM_MESSAGE.STR_MSG_GUARD_UP_1LEVEL);
				}
				if (info.getRank() >= 2) {
					// 你现已成为不屈守护者。 / You are now an Indomitable Protector.
					PacketSendUtility.sendPacket(killer, SM_SYSTEM_MESSAGE.STR_MSG_GUARD_UP_2LEVEL);
				}
				if (info.getRank() >= 3) {
					// 你现已成为英勇守护者。 / You are now a Valiant Protector.
					PacketSendUtility.sendPacket(killer, SM_SYSTEM_MESSAGE.STR_MSG_GUARD_UP_3LEVEL);
				}
				if (info.getRank() != rank) {
					info.setRank(rank);
					protectorBuff.applyRankEffect(killer, rank);
					final Map<Integer, Player> guards = getWorldProtector(killer.getWorldId());
					PacketSendUtility.sendPacket(killer, new SM_CONQUEROR_PROTECTOR(true, info.getRank()));
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(killer.getWorldId()).getWorldMapInstanceById(killer.getInstanceId())
							.doOnAllPlayers(new Visitor<Player>() {
								@Override
								public void visit(Player observed) {
									if (!killer.getRace().equals(observed.getRace())) {
										PacketSendUtility.sendPacket(observed,
												new SM_CONQUEROR_PROTECTOR(playersSnapshot(guards)));
									}
								}
							});
				}
				if (!protectors.containsKey(killer.getObjectId())) {
					protectors.put(killer.getObjectId(), info);
				}
			}
		} else if (isEnemyWorld(killer)) { // Conqueror.
			Conqueror info = killer.getConquerorInfo();
			if (killer.getLevel() >= victim.getLevel() + CustomConfig.PROTECTOR_CONQUEROR_LEVEL_DIFF) {
				int rank = getRanks(++info.victims);
				if (info.getRank() >= 1) {
					// 你现已成为征服者。 / You are now a Conqueror.
					PacketSendUtility.sendPacket(killer, SM_SYSTEM_MESSAGE.STR_MSG_SLAYER_UP_1LEVEL);
				}
				if (info.getRank() >= 2) {
					// 你现已成为狂怒征服者。 / You are now an Furious Conqueror.
					PacketSendUtility.sendPacket(killer, SM_SYSTEM_MESSAGE.STR_MSG_SLAYER_UP_2LEVEL);
				}
				if (info.getRank() >= 3) {
					// 你现已成为狂暴征服者。 / You are now a Berserk Conqueror.
					PacketSendUtility.sendPacket(killer, SM_SYSTEM_MESSAGE.STR_MSG_SLAYER_UP_3LEVEL);
				}
				if (info.getRank() != rank) {
					info.setRank(rank);
					conquerorBuff.applyEffect(killer, rank);
					final Map<Integer, Player> killers = getWorldConqueror(killer.getWorldId());
					PacketSendUtility.sendPacket(killer, new SM_CONQUEROR_PROTECTOR(true, info.getRank()));
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(killer.getWorldId()).getWorldMapInstanceById(killer.getInstanceId())
							.doOnAllPlayers(new Visitor<Player>() {
								@Override
								public void visit(Player observed) {
									if (!killer.getRace().equals(observed.getRace())) {
										PacketSendUtility.sendPacket(observed,
												new SM_CONQUEROR_PROTECTOR(playersSnapshot(killers)));
									}
								}
							});
				}
				if (!conquerors.containsKey(killer.getObjectId())) {
					conquerors.put(killer.getObjectId(), info);
				}
			}
		}
	}

	private int getRanks(int kills) {
		return kills > CustomConfig.PROTECTOR_CONQUEROR_2ND_RANK_KILLS ? 2
				: kills > CustomConfig.PROTECTOR_CONQUEROR_1ST_RANK_KILLS ? 1 : 0;
	}

	/**
	 * 击杀守护者/征服者时对附近同阵营施加增益。
	 * Applies a nearby same-race buff when a protector/conqueror is killed.
	 *
	 * killer
	 * victim
	 */
	public void onKillProtectorConqueror(final Player killer, final Player victim) {
		if (!isEnemyWorld(victim)) {
			final Protector info = victim.getProtectorInfo();
			victim.getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (killer.getRace().equals(player.getRace()) && MathUtil.isIn3dRange(victim, player, 30)) {
						GameEngineServices.skillEngine().applyEffectDirectly(buffId(killer, info), player, player, 0);
					}
				}
			});
		} else if (isEnemyWorld(victim)) {
			final Conqueror conqueror = victim.getConquerorInfo();
			victim.getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (killer.getRace().equals(player.getRace()) && MathUtil.isIn3dRange(victim, player, 30)) {
						GameEngineServices.skillEngine().applyEffectDirectly(buffId(killer, conqueror), player, player, 0);
					}
				}
			});
		}
	}

	/**
	 * 判断世界是否受守护者/征服者系统管理。
	 * Checks whether the world is handled by this system.
	 *
	 * 世界 ID / world id
	 * whether handled
	 */
	public boolean isHandledWorld(int worldId) {
		return handledWorlds.containsKey(worldId);
	}

	/**
	 * 判断玩家是否处于敌对世界（征服者侧）。
	 * Checks whether the player is in an enemy world (conqueror side).
	 *
	 * @param player 玩家 / player
	 * @return 是否敌对世界 / whether enemy world
	 */
	public boolean isEnemyWorld(Player player) {
		if (handledWorlds.containsKey(player.getWorldId())) {
			WorldType homeType = player.getRace().equals(Race.ASMODIANS) ? WorldType.ASMODIANS : WorldType.ELYOS;
			return !handledWorlds.get(player.getWorldId()).equals(homeType);
		}
		return false;
	}

	private int buffId(Player player, Protector info) {
		if (info.getRank() > 0) {
			return player.getRace() == Race.ELYOS ? 8610 : 8611;
		}
		return 0;
	}

	private int buffId(Player player, Conqueror info) {
		if (info.getRank() > 0) {
			return player.getRace() == Race.ELYOS ? 8610 : 8611;
		}
		return 0;
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static ProtectorConquerorService getInstance() {
		ObjectProvider<ProtectorConquerorService> provider = instanceProvider;
		if (provider == null) {
			return ProtectorConquerorService.SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> ProtectorConquerorService.SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<ProtectorConquerorService> instanceProvider) {
		ProtectorConquerorService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final ProtectorConquerorService instance = new ProtectorConquerorService();
	}
}
