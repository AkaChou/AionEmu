package com.aionemu.gameserver.services.events.bg;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dao.LadderDAO;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceMember;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.services.StaticDoorService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 战场基类，封装匹配、对局生命周期、天梯评分、观战与公共工具逻辑。
 * Base battleground class encapsulating matchmaking, match lifecycle, ladder rating, spectating and shared helpers.
 *
 * @Author Rinzler (Encom)
 */
public abstract class Battleground {
	/** 默认传送延迟（毫秒）。 / Default teleport delay in ms. */
	protected static final int TELEPORT_DEFAULT_DELAY = 0;
	/** Elo rating K-value / Elo rating K-value */
	protected final int K_VALUE = 20;

	/** 战场显示名 → 实现类。 / Display name → implementation class. */
	@SuppressWarnings("serial")
	protected static final Map<String, Class<?>> aliases = new HashMap<String, Class<?>>() {
		{
			put("[DeathMatch]", DeathmatchBg.class);
			put("[1 VS 1]", SoloSurvivorBg.class);
			put("[Team VS Team]", TwoTeamBg.class);
			put("[Team VS Team]", TwoTeamSmallBg.class);
		}
	};
	/** 战场名称。 / Battleground name. */
	protected String name = "";
	/** 战场描述。 / Battleground description. */
	protected String description = "";
	/** 单队/单局最小人数。 / Minimum size per team/match. */
	protected int minSize = 0;
	/** 单队/单局最大人数。 / Maximum size per team/match. */
	protected int maxSize = 0;
	/** 队伍数量。 / Number of teams. */
	protected int teamCount = 0;
	/** 对局时长（秒）。 / Match length in seconds. */
	protected int matchLength = 0;
	/** 可选战场地图列表。 / Available battleground maps. */
	protected List<BattlegroundMap> maps = new ArrayList<BattlegroundMap>();
	/** 当前副本 ID / Current instance id */
	protected int instanceId = -1;
	/** Registered battleground id / Registered battleground id */
	protected Integer bgId = -1;
	/** 对局开始时间戳。 / Match start timestamp. */
	protected long startStamp = 0;
	/** 是否锦标赛模式。 / Whether tournament mode. */
	protected boolean isTournament = false;
	/** 是否活动模式。 / Whether event mode. */
	protected boolean isEvent = false;
	/**
	 * Whether 1v1 mode
	 */
	protected boolean is1v1 = false;
	/** 当前世界地图实例。 / Current world map instance. */
	protected WorldMapInstance instance = null;
	/** 过期/结束任务。 / Expire/end task. */
	protected ScheduledFuture<?> expireTask = null;
	/** 后台周期任务。 / Background periodic task. */
	protected ScheduledFuture<?> backgroundTask = null;
	/** 后台任务计数。 / Background task counter. */
	protected int backgroundCounter = 0;
	/** 当前选用地图。 / Currently selected map. */
	protected BattlegroundMap map = null;
	/** 当前映射 ID / Current map id */
	protected int mapId = 0;
	/** 对局是否已结束。 / Whether match is finished. */
	protected boolean isDone = false;
	/** 结束时是否解散队伍。 / Whether to disband teams on end. */
	protected boolean shouldDisband = true;
	/** 是否基于队伍。 / Whether team-based. */
	protected boolean teamBased = false;
	/** 玩家进场前坐标。 / Player locations before entry. */
	protected Map<Integer, WorldPosition> previousLocations = new HashMap<Integer, WorldPosition>();
	/** 单人参赛者列表。 / Solo participants. */
	protected List<Player> _players = new CopyOnWriteArrayList<Player>();
	/** 小队参赛者列表。 / Group participants. */
	protected List<PlayerGroup> _groups = new CopyOnWriteArrayList<PlayerGroup>();
	/** 联盟参赛者列表。 / Alliance participants. */
	protected List<PlayerAlliance> _alliances = new CopyOnWriteArrayList<PlayerAlliance>();
	/** 观战者列表。 / Spectators. */
	protected List<Player> _spectators = new CopyOnWriteArrayList<Player>();
	/** 中途离开者（用于重连）。 / Leavers (for reconnect). */
	protected Map<Integer, AionObject> _leavers = Collections.synchronizedMap(new LinkedHashMap<Integer, AionObject>());

	/**
	 * 根据排队玩家创建并准备对局。
	 * Creates and prepares a match from queued player object ids.
	 *
	 * @param players 排队玩家对象 ID 列表 / queued player object ids
	 */
	public abstract void createMatch(List<Integer> players);

	/**
	 * 开始对局（解冻、倒计时、开启计时）。
	 * Starts the match (unfreeze, countdown, start timer).
	 */
	public abstract void startMatch();

	/**
	 * 处理玩家死亡事件。
	 * Handles a player death event.
	 *
	 * dead player
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	public abstract void onDie(final Player player, Creature lastAttacker);

	/**
	 * 处理玩家离开战场。
	 * Handles a player leaving the battleground.
	 *
	 * @param player 离开的玩家 / leaving player
	 * whether logout
	 * @param isAfk 是否挂机 / whether AFK
	 */
	public abstract void onLeave(Player player, boolean isLogout, boolean isAfk);

	/**
	 * 神器被摧毁时回调（子类可覆盖）。
	 * Callback when an artifact is destroyed (override in subclasses).
	 *
	 * team index
	 */
	public void onArtifactDie(int teamIndex) {
	}

	/**
	 * 资源被采集时回调（子类可覆盖）。
	 * Callback when a resource is gathered (override in subclasses).
	 *
	 * gatherable
	 * team index
	 */
	public void onResourceGathered(Gatherable resource, int teamIndex) {
	}

	/**
	 * 是否限制隐身技能。
	 * Whether stealth skills are restricted.
	 *
	 * @return 若 restricted 则为 true / true if restricted
	 */
	public boolean isStealthRestricted() {
		return false;
	}

	/**
	 * 判断效果模板是否允许使用。
	 * Whether the given effect template is allowed.
	 *
	 * @param et 效果模板 / effect template
	 * @return 若 allowed 则为 true / true if allowed
	 */
	public boolean isEffectAllowed(EffectTemplate et) {
		return true;
	}

	/**
	 * 是否限制飞行。
	 * Whether flight is restricted on the current map.
	 *
	 * @return 若 restricted 则为 true / true if restricted
	 */
	public boolean isFlightRestricted() {
		if (map != null && map.isRestrictFlight()) {
			return true;
		}
		return false;
	}

	/**
	 * 创建锦标赛对局；默认不支持。
	 * Creates a tournament match; unsupported by default.
	 *
	 * @param teams 各队玩家列表 / teams of players
	 * true on success
	 */
	public boolean createTournament(List<List<Player>> teams) {
		return false;
	}

	/**
	 * 获取当前地图的出生点列表。
	 * Returns spawn positions of the current map.
	 *
	 * @return 出生点列表 / spawn positions
	 */
	public List<SpawnPosition> getSpawnPositions() {
		if (map == null) {
			return new ArrayList<SpawnPosition>();
		} else {
			return map.getSpawnPoints();
		}
	}

	/**
	 * 判断玩家是否位于本战场可用地图中。
	 * Whether the player is on one of this battleground maps.
	 *
	 * @param player 玩家 / player
	 * @return 在战场地图内则 true / true if on a BG map
	 */
	public boolean isInBg(Player player) {
		for (BattlegroundMap bgMap : maps) {
			if (bgMap.getMapId() == player.getWorldId()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 按排队人数与模式计算本局抽取规模。
	 * Computes match draw size from queue size and mode.
	 *
	 * queue size
	 * draw size
	 */
	protected int getRandomSize(int playerCount) {
		int avgCount = (int) Math.floor(playerCount / getTeamCount());
		if (isTeamBased()) {
			return avgCount;
		} else if (avgCount <= getMaxSize() && avgCount >= getMinSize()) {
			return avgCount;
		} else if (avgCount < getMinSize()) {
			return getMinSize();
		} else if (isEvent() || isTournament()) {
			return getMaxSize();
		} else {
			return Rnd.get(getMinSize(), getMaxSize());
		}
	}

	/**
	 * 处理单人队列匹配。
	 * Handles solo queue matchmaking.
	 *
	 * queued player ids
	 * true on success
	 */
	protected boolean handleQueueSolo(List<Integer> players) {
		int size = getRandomSize(players.size());
		if (players.size() < size) {
			return false;
		}
		int playerIndex = 0;
		while (players.size() > 0) {
			int objId = players.remove(0);
			Player pl = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
			if (pl == null) {
				continue;
			}
			pl.setBgIndex(playerIndex);
			pl.setBattleground(this);
			addPlayer(pl);
			players.remove((Integer) objId);
			removePlayerFromTeam(pl);
			playerIndex++;
			if (getPlayers().size() >= size) {
				break;
			}
		}
		return true;
	}

	/**
	 * 处理小队队列匹配。
	 * Handles group queue matchmaking.
	 *
	 * queued player ids
	 * true on success
	 */
	protected boolean handleQueueGroup(List<Integer> players) {
		int groupSize = getRandomSize(players.size());
		if (players.size() < groupSize * getTeamCount()) {
			return false;
		}
		int groupIndex = 0;
		while (players.size() >= groupSize) {
			List<Player> groupPlayers = new ArrayList<Player>();
			while (groupPlayers.size() < groupSize && players.size() > 0) {
				int objId = players.remove(0);
				Player pl = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
				if (pl != null) {
					removePlayerFromTeam(pl);
					groupPlayers.add(pl);
					pl.setBattleground(this);
					players.remove((Integer) objId);
				}
			}
			PlayerGroup group = PlayerGroupService.createGroup(groupPlayers.get(0));
			for (int i = 1; i < groupPlayers.size(); i++) {
				PlayerGroupService.addPlayer(group, groupPlayers.get(i));
			}
			group.setBgIndex(groupIndex);
			addGroup(group);
			groupIndex++;
			if (getGroups().size() >= getTeamCount()) {
				break;
			}
		}
		return true;
	}

	/**
	 * 处理联盟队列匹配。
	 * Handles alliance queue matchmaking.
	 *
	 * queued player ids
	 * true on success
	 */
	protected boolean handleQueueAlliance(List<Integer> players) {
		int allianceSize = getRandomSize(players.size());
		if (players.size() < allianceSize * getTeamCount()) {
			return false;
		}
		int allianceIndex = 0;
		while (players.size() >= allianceSize) {
			List<Player> alliancePlayers = new ArrayList<Player>();
			while (alliancePlayers.size() < allianceSize && players.size() > 0) {
				int objId = players.remove(0);
				Player pl = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
				if (pl != null) {
					alliancePlayers.add(pl);
					pl.setBattleground(this);
					removePlayerFromTeam(pl);
				}
			}
			PlayerAlliance alliance = new PlayerAlliance(new PlayerAllianceMember(alliancePlayers.get(0)),
					TeamType.ALLIANCE);
			for (int i = 0; i < alliancePlayers.size(); i++) {
				PlayerAllianceService.addPlayerToAlliance(alliance, alliancePlayers.get(i));
			}
			alliance.setBgIndex(allianceIndex);
			addAlliance(alliance);
			allianceIndex++;
			if (getAlliances().size() >= getTeamCount()) {
				break;
			}
		}
		return true;
	}

	/**
	 * 为各队创建小队并登记。
	 * Creates groups for each team and registers them.
	 *
	 * @param teams 各队玩家 / teams of players
	 * true on success
	 */
	protected boolean createGroups(List<List<Player>> teams) {
		if (teams.size() < getTeamCount()) {
			return false;
		}
		int groupSize = 100;
		for (List<Player> team : teams) {
			if (team.size() < groupSize) {
				groupSize = team.size();
			}
		}
		if (groupSize < 1) {
			return false;
		}
		int groupIndex = 0;
		while (getGroups().size() < getTeamCount()) {
			List<Player> players = teams.remove(0);
			while (players.size() > groupSize) {
				players.remove(players.size());
			}
			for (Player pl : players) {
				pl.setBattleground(this);
				removePlayerFromTeam(pl);
			}
			PlayerGroup group = PlayerGroupService.createGroup(players.get(0));
			for (int i = 1; i < players.size(); i++) {
				PlayerGroupService.addPlayer(group, players.get(i));
			}
			group.setBgIndex(groupIndex);
			addGroup(group);
			groupIndex++;
		}
		return true;
	}

	/**
	 * 为各队创建联盟并登记。
	 * Creates alliances for each team and registers them.
	 *
	 * @param teams 各队玩家 / teams of players
	 * true on success
	 */
	protected boolean createAlliances(List<List<Player>> teams) {
		if (teams.size() < getTeamCount()) {
			return false;
		}
		int allianceSize = 100;
		for (List<Player> team : teams) {
			if (team.size() < allianceSize) {
				allianceSize = team.size();
			}
		}
		if (allianceSize < 1) {
			return false;
		}
		int allianceIndex = 0;
		while (getAlliances().size() < getTeamCount()) {
			List<Player> players = teams.remove(0);
			while (players.size() > allianceSize) {
				players.remove(players.size());
			}
			for (Player pl : players) {
				pl.setBattleground(this);
				removePlayerFromTeam(pl);
			}
			PlayerAlliance alliance = new PlayerAlliance(new PlayerAllianceMember(players.get(0)), TeamType.ALLIANCE);
			for (int i = 0; i < players.size(); i++) {
				PlayerAllianceService.addPlayerToAlliance(alliance, players.get(i));
			}
			alliance.setBgIndex(allianceIndex);
			addAlliance(alliance);
			allianceIndex++;
		}
		return true;
	}

	/**
	 * 登记单人参赛者列表。
	 * Registers solo participants.
	 *
	 * sides of players
	 * true on success
	 */
	protected boolean createPlayers(List<List<Player>> players) {
		List<Integer> playerList = new ArrayList<Integer>();
		for (List<Player> plList : players) {
			for (Player pl : plList) {
				playerList.add(pl.getObjectId());
			}
		}
		while (playerList.size() > getMaxSize()) {
			playerList.remove(playerList.size() - 1);
		}
		return handleQueueSolo(playerList);
	}

	/**
	 * 添加单人参赛者。
	 * Adds a solo participant.
	 *
	 * @param player 玩家 / player
	 */
	protected void addPlayer(Player player) {
		if (!getPlayers().contains(player)) {
			getPlayers().add(player);
		}
	}

	/**
	 * 添加小队参赛者。
	 * Adds a group participant.
	 *
	 * group
	 */
	protected void addGroup(PlayerGroup group) {
		if (!getGroups().contains(group)) {
			getGroups().add(group);
		}
	}

	/**
	 * 添加联盟参赛者。
	 * Adds an alliance participant.
	 *
	 * alliance
	 */
	protected void addAlliance(PlayerAlliance alliance) {
		if (!getAlliances().contains(alliance)) {
			getAlliances().add(alliance);
		}
	}

	/**
	 * 冻结玩家且不自动解冻。
	 * Freezes the player without auto-unfreeze.
	 *
	 * @param player 玩家 / player
	 */
	protected void freezeNoEnd(Player player) {
		player.getEffectController().setAbnormal(AbnormalState.PARALYZE.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
	}

	/**
	 * 冻结玩家并在指定时长后解冻。
	 * Freezes the player and unfreezes after the given duration.
	 *
	 * 玩家 / player
	 * duration in ms
	 */
	protected void freezePlayer(final Player player, int duration) {
		player.getEffectController().setAbnormal(AbnormalState.PARALYZE.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				player.getEffectController().unsetAbnormal(AbnormalState.PARALYZE.getId());
				player.getEffectController().updatePlayerEffectIcons();
				player.getEffectController().broadCastEffects();
			}
		}, duration);
	}

	/**
	 * 治疗玩家（默认重置 DP）。
	 * Heals the player (resets DP by default).
	 *
	 * @param player 玩家 / player
	 */
	protected void healPlayer(Player player) {
		this.healPlayer(player, true);
	}

	/**
	 * 治疗玩家并可选择是否重置 DP。
	 * Heals the player, optionally resetting DP.
	 *
	 * 玩家 / player
	 * whether to reset DP
	 */
	protected void healPlayer(Player player, boolean resetDp) {
		player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, player.getLifeStats().getMaxHp() + 1);
		player.getLifeStats().increaseMp(SM_ATTACK_STATUS.TYPE.MP, player.getLifeStats().getMaxMp() + 1);
		if (resetDp) {
			player.getCommonData().setDp(0);
		}
	}

	/**
	 * 传送玩家到当前副本坐标。
	 * Teleports the player to coordinates in the current instance.
	 *
	 * @param player 玩家 / player
	 * @param x X 坐标 / x
	 * @param y Y 坐标 / y
	 * @param z Z 坐标 / z
	 */
	protected void performTeleport(Player player, float x, float y, float z) {
		player.getController().abortCast();
		previousLocations.put(player.getObjectId(), player.getPosition().clone());
		TeleportService2.teleportTo(player, getMapId(), getInstanceId(), x, y, z);
	}

	/**
	 * 重置玩家技能与物品冷却。
	 * Resets player skill and item cooldowns.
	 *
	 * @param player 玩家 / player
	 */
	protected void performCdReset(Player player) {
		List<Integer> delayIds = new ArrayList<Integer>();
		if (player.getSkillCoolDowns() != null) {
			long currentTime = System.currentTimeMillis();
			for (Map.Entry<Integer, Long> en : player.getSkillCoolDowns().entrySet()) {
				delayIds.add(en.getKey());
			}
			for (Integer delayId : delayIds) {
				player.setSkillCoolDown(delayId, currentTime);
			}
			delayIds.clear();
			PacketSendUtility.sendPacket(player, new SM_SKILL_COOLDOWN(player.getSkillCoolDowns()));
		}
	}

	/**
	 * 将玩家传回进场前坐标。
	 * Returns the player to the pre-entry location.
	 *
	 * @param player 玩家 / player
	 */
	protected void returnToPreviousLocation(Player player) {
		player.setBattleground(null);
		if (player.getLifeStats().isAlreadyDead()) {
			PlayerReviveService.bgRevive(player);
		}
		healPlayer(player, false);
		endTimer(player);
		WorldPosition previousPos = previousLocations.get(player.getObjectId());
		if (previousPos == null) {
			TeleportService2.moveToBindLocation(player, true);
			return;
		}
		previousLocations.remove(player.getObjectId());
		TeleportService2.teleportTo(player, previousPos.getMapId(), previousPos.getX(), previousPos.getY(),
				previousPos.getZ() + 1);
	}

	/**
	 * 延迟向玩家发送带发送者的公告。
	 * Schedules an announcement with sender to the player.
	 *
	 * 玩家 / player
	 * sender name
	 * message
	 * @param delay 延迟毫秒 / delay ms
	 */
	protected void scheduleAnnouncement(final Player player, final String sender, final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					PacketSendUtility.sendSys3Message(player, sender, msg);
				}
			}, delay);
		} else {
			PacketSendUtility.sendSys3Message(player, sender, msg);
		}
	}

	/**
	 * 延迟向玩家发送默认发送者公告。
	 * Schedules a default-sender announcement to the player.
	 *
	 * 玩家 / player
	 * message
	 * @param delay 延迟毫秒 / delay ms
	 */
	protected void scheduleAnnouncement(Player player, String msg, int delay) {
		if (player.getBattleground() instanceof SoloSurvivorBg) {
			this.scheduleAnnouncement(player, "1vs1", msg, delay);
		} else if (player.getBattleground() instanceof DeathmatchBg) {
			this.scheduleAnnouncement(player, "DM", msg, delay);
		} else {
			this.scheduleAnnouncement(player, "BG", msg, delay);
		}
	}

	/**
	 * 向全部观战者即时公告。
	 * Announces immediately to all spectators.
	 *
	 * message
	 */
	protected void specAnnounce(String msg) {
		for (Player spectator : getSpectators()) {
			PacketSendUtility.sendSys3Message(spectator, "BG", msg);
		}
	}

	/**
	 * 延迟向全部观战者公告。
	 * Schedules an announcement to all spectators.
	 *
	 * message
	 * @param delay 延迟毫秒 / delay ms
	 */
	protected void specAnnounce(final String msg, int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				for (Player spectator : getSpectators()) {
					PacketSendUtility.sendSys3Message(spectator, "BG", msg);
				}
			}
		}, delay);
	}

	/**
	 * 向玩家安排倒计时公告。
	 * Schedules countdown announcements for the player.
	 *
	 * @param player 玩家 / player
	 * @param length 倒计时秒数 / countdown length
	 * @param startTime 起始延迟毫秒 / start delay ms
	 */
	protected void scheduleCountdown(Player player, int length, int startTime) {
		for (int i = length; i > 0; i--) {
			scheduleAnnouncement(player, "The match start in " + i + " seconds!", startTime - i * 1000);
		}
	}

	/**
	 * 延迟解散小队。
	 * Schedules group disband.
	 *
	 * group
	 * @param delay 延迟毫秒 / delay ms
	 */
	protected void scheduleGroupDisband(final PlayerGroup group, int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				while (group.size() > 0) {
					PlayerGroupService.removePlayer((Player) group.getMembers().toArray()[0]);
				}
			}
		}, delay);
	}

	/**
	 * 延迟解散联盟。
	 * Schedules alliance disband.
	 *
	 * alliance
	 * @param delay 延迟毫秒 / delay ms
	 */
	protected void scheduleAllianceDisband(final PlayerAlliance alliance, int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				while (alliance.size() > 0) {
					PlayerAllianceService.removePlayer(((Player) alliance.getMembers().toArray()[0]));
				}
			}
		}, delay);
	}

	/**
	 * 准备玩家（默认发送公告）。
	 * Prepares the player (announce by default).
	 *
	 * @param pl 玩家 / player
	 * @param time 准备时长毫秒 / prepare time ms
	 */
	protected void preparePlayer(final Player pl, int time) {
		this.preparePlayer(pl, time, true);
	}

	/**
	 * 准备玩家：冻结、治疗、清 CD，可选公告。
	 * Prepares the player: freeze, heal, clear CDs; optional announce.
	 *
	 * @param pl 玩家 / player
	 * @param time 准备时长毫秒 / prepare time ms
	 * whether to announce
	 */
	protected void preparePlayer(final Player pl, int time, boolean announce) {
		GameGameplayServices.duelService().loseDuel(pl);
		pl.setKillStreak(0);
		pl.setLastAction();
		pl.getFlyController().endFly(true);
		if (pl.getLifeStats().isAlreadyDead()) {
			PlayerReviveService.skillRevive(pl);
		}
		healPlayer(pl);
		InstanceService.registerPlayerWithInstance(getInstance(), pl);
		if (time > 0) {
			freezePlayer(pl, time);
			scheduleCountdown(pl, 5, time);
		}
		if (announce) {
			pl.setTotalKills(0);
			removecd(pl);
			if (time > 0) {
				scheduleAnnouncement(pl, "You have join " + getName() + " battleground!", 0);
				scheduleAnnouncement(pl, "Description: " + getDescription(), 10000);
				scheduleAnnouncement(pl, "The match begin's!!!", time);
				// sendEventPacket(StageType.PVP_STAGE_1, 0);
			}
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					pl.getEffectController().removeAllEffects();
				}
			}, 2500);
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					createTimer(pl, getSecondsLeft());
				}
			}, time - 5000);
		} else {
			pl.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
			pl.getEffectController().removeEffectByDispelCat(DispelCategoryType.ALL, SkillTargetSlot.DEBUFF, 100, 2,
					100, false);
			pl.setTarget(null);
			PacketSendUtility.sendPacket(pl, new SM_TARGET_SELECTED(pl));
			scheduleAnnouncement(pl, "The match begin's!!!", time);
			createTimer(pl, getSecondsLeft());
		}
	}

	/**
	 * 清除玩家技能冷却并同步客户端。
	 * Clears the player skill cooldowns and syncs the client.
	 *
	 * @param player 玩家 / player
	 */
	public void removecd(Player player) {
		List<Integer> delay = new ArrayList<Integer>();
		if (player.getSkillCoolDowns() != null) {
			for (Map.Entry<Integer, Long> en : player.getSkillCoolDowns().entrySet()) {
				delay.add(en.getKey());
			}
			for (Integer delayId : delay) {
				player.setSkillCoolDown(delayId, 0);
			}
			delay.clear();
			PacketSendUtility.sendPacket(player, new SM_SKILL_COOLDOWN(player.getSkillCoolDowns()));
		}
	}

	/**
	 * 重置玩家已知列表与外观包。
	 * Resets the player known list and appearance packets.
	 *
	 * @param player 玩家 / player
	 * @param delay 延迟毫秒 / delay ms
	 */
	protected void resetPlayerKnownlist(final Player player, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					player.clearKnownlist();
					PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
					PacketSendUtility.sendPacket(player, new SM_MOTION(player.getMotions().getMotions().values()));
					player.getEffectController().updatePlayerEffectIcons();
					player.getKnownList().doUpdate();
				}
			}, delay);
		} else {
			player.clearKnownlist();
			PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
			PacketSendUtility.sendPacket(player, new SM_MOTION(player.getMotions().getMotions().values()));
			player.getEffectController().updatePlayerEffectIcons();
			player.getKnownList().doUpdate();
		}
	}

	/**
	 * 计算对局剩余秒数。
	 * Remaining match seconds.
	 *
	 * seconds left
	 */
	public int getSecondsLeft() {
		return (getMatchLength() - Math.round((float) (System.currentTimeMillis() - getStartStamp()) / 1000));
	}

	/**
	 * 向玩家创建任务式倒计时 UI。
	 * Creates quest-style timer UI for the player.
	 *
	 * 玩家 / player
	 * seconds
	 */
	protected void createTimer(Player player, int seconds) {
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, seconds));
	}

	/**
	 * 结束玩家倒计时 UI。
	 * Ends the player timer UI.
	 *
	 * @param player 玩家 / player
	 */
	protected void endTimer(Player player) {
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
	}

	/**
	 * 记录玩家胜场与评分变化。
	 * Records a player win and rating change.
	 *
	 * 玩家 / player
	 * rating delta
	 */
	protected void playerWinMatch(Player player, int ratingChange) {
		if (is1v1()) {
			return;
		}
		getLadderDAO().addWin(player);
		getLadderDAO().addRating(player, Math.round(ratingChange / (getLadderDAO().getRating(player) * 0.0015f)));
	}

	/**
	 * 记录玩家负场与评分变化。
	 * Records a player loss and rating change.
	 *
	 * 玩家 / player
	 * rating delta
	 */
	protected void playerLoseMatch(Player player, int ratingChange) {
		if (is1v1()) {
			return;
		}
		getLadderDAO().addLoss(player);
		getLadderDAO().addRating(player, Math.round(ratingChange * (getLadderDAO().getRating(player) * 0.0015f)));
	}

	/**
	 * 按胜负双方批量更新天梯。
	 * Batch ladder update for winners and losers.
	 *
	 * winners
	 * losers
	 */
	protected void performLadderUpdate(Collection<Player> winner, Collection<Player> loser) {
		int avgWinnerRating = 0;
		int avgLoserRating = 0;
		for (Player pl : winner) {
			getLadderDAO().addWin(pl);
			avgWinnerRating += getLadderDAO().getRating(pl);
		}
		for (Player pl : winner) {
			getLadderDAO().addLoss(pl);
			avgWinnerRating += getLadderDAO().getRating(pl);
		}
		if (winner.size() > 0) {
			avgWinnerRating = avgWinnerRating / winner.size();
		}
		if (loser.size() > 0) {
			avgLoserRating = avgLoserRating / loser.size();
		}
		int ratingChange = calcRatingChange(avgWinnerRating, avgLoserRating);
		for (Player pl : winner) {
			getLadderDAO().addRating(pl, +ratingChange);
		}
		for (Player pl : loser) {
			getLadderDAO().addRating(pl, -ratingChange);
		}
	}

	/**
	 * 计算 Elo 评分变化量。
	 * Calculates Elo rating change.
	 *
	 * rating A
	 * rating B
	 * delta
	 */
	protected int calcRatingChange(int ratingA, int ratingB) {
		return (int) Math.round(K_VALUE * (1 / (1 + Math.pow(10, ((float) ratingB - (float) ratingA) / 400))));
	}

	/**
	 * 将玩家从当前队伍结构中移除。
	 * Removes the player from current team structures.
	 *
	 * @param player 玩家 / player
	 */
	protected void removePlayerFromTeam(Player player) {
		if (player.isInGroup2()) {
			PlayerGroupService.removePlayer(player);
		}
		if (player.isInAlliance2()) {
			PlayerAllianceService.removePlayer(player);
		}
	}

	/**
	 * 启动后台周期任务（坠落检测等）。
	 * Starts the background periodic task (e.g. fall checks).
	 */
	protected void startBackgroundTask() {
		setBackgroundTask(GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				backgroundCounter++;
				zCheck();
				if ((backgroundCounter % 5) == 0) {
					backgroundCounter = 0;
				}
			}
		}, 30 * 1000, 1 * 1000));
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				if (getBackgroundTask() != null) {
					getBackgroundTask().cancel(true);
				}
			}
		}, 10 * getMatchLength() * 1000);
	}

	/**
	 * 检测玩家是否低于击杀高度并处理坠落死亡。
	 * Checks whether players fell below kill Z and handles fall deaths.
	 */
	protected void zCheck()
	{
		if (getGroups().size() > 0)
		{
			for (PlayerGroup group : getGroups())
			{
				for (Player pl : group.getMembers())
				{
					if ((pl == null) || pl.getLifeStats().isAlreadyDead() || (map.getMapId() != pl.getWorldId()) || (pl.getBattleground() == null))
					{
						continue;
					}
					if (pl.getZ() < map.getKillZ())
					{
						pl.getLifeStats().reduceHp(100000, pl);
						//	new SM_ATTACK_STATUS(pl, SM_ATTACK_STATUS.TYPE.FALL_DAMAGE, 0, -100000));
					}
				}
			}
		}
		if (getAlliances().size() > 0)
		{
			for (PlayerAlliance alliance : getAlliances())
			{
				for (Player pl : alliance.getMembers())
				{
					if ((pl == null) || pl.getLifeStats().isAlreadyDead() || (map.getMapId() != pl.getWorldId()) || (pl.getBattleground() == null))
					{
						continue;
					}
					if (pl.getZ() < map.getKillZ())
					{
						pl.getLifeStats().reduceHp(100000, pl);
						//	new SM_ATTACK_STATUS(pl, SM_ATTACK_STATUS.TYPE.FALL_DAMAGE, 0, -100000));
					}
				}
			}
		}
		if (getPlayers().size() > 0)
		{
			for (Player pl : getPlayers())
			{
				if ((pl == null) || pl.getLifeStats().isAlreadyDead() || (map.getMapId() != pl.getWorldId()) || (pl.getBattleground() == null))
				{
					continue;
				}
				if (pl.getZ() < map.getKillZ())
				{
					pl.getLifeStats().reduceHp(100000, pl);
					//	new SM_ATTACK_STATUS(pl, SM_ATTACK_STATUS.TYPE.FALL_DAMAGE, 0, -100000));
				}
			}
		}
	}

	/**
	 * 默认离场处理：登记离开者、惩罚与清理。
	 * Default leave handling: register leaver, penalize and cleanup.
	 *
	 * 玩家 / player
	 * logout
	 * @param isAfk 是否挂机 / AFK
	 */
	protected void onLeaveDefault(Player player, boolean isLogout, boolean isAfk) {
		if (player.isSpectating()) {
			onSpectatorLeave(player, false);
			return;
		}
		if (player.isInAlliance2()) {
			getLeavers().put(player.getObjectId(), player.getPlayerAlliance2());
		} else if (player.isInGroup2()) {
			getLeavers().put(player.getObjectId(), player.getPlayerGroup2());
		} else {
			getLeavers().put(player.getObjectId(), null);
		}
		if (isLogout) {
			TeleportService2.moveToBindLocation(player, true);
		} else if (!isAfk) {
			if (!this.isDone && !player.getController().isInShutdownProgress()) {
				scheduleAnnouncement(player, "You are penalized for leaving the battleground!", 10000);
			} else {
				returnToPreviousLocation(player);
				scheduleAnnouncement(player, "You are penalized for being absent too long.", 10000);
			}
			if (!isAfk) {
				List<Player> players = getPlayers();
				synchronized (players) {
					players.remove(player);
				}
			}
			player.setBattleground(null);
			if (!this.isDone && !player.getController().isInShutdownProgress()) {
				getLadderDAO().addLeave(player);
				getLadderDAO().addRating(player, -K_VALUE);
			}
			if (isLogout || isAfk) {
				player.setAfk(true);
			}
			removePlayerFromTeam(player);
			endTimer(player);
		}
	}

	/**
	 * 通用生物死亡入口（默认空实现，子类可覆盖）。
	 * Generic creature-death entry (no-op by default; override as needed).
	 *
	 * dead creature
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	public void onDie(Creature creature, Creature lastAttacker) {
	}

	/**
	 * 默认死亡处理：击杀统计与公告。
	 * Default death handling: kill stats and announcements.
	 *
	 * dead player
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	protected void onDieDefault(Player player, Creature lastAttacker) {
		Summon summon = player.getSummon();
		if (summon != null) {
			summon.getController().release(UnsummonType.UNSPECIFIED);
		}
		PacketSendUtility.broadcastPacket(player,
				new SM_EMOTION(player, EmotionType.DIE, 0, lastAttacker == null ? 0 : lastAttacker.getObjectId()),
				true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DEATH_MESSAGE_ME);
		player.getMoveController().abortMove();
		player.setState(CreatureState.DEAD);
		player.getObserveController().notifyDeathObservers(player);
		player.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
		player.getEffectController().removeEffectByDispelCat(DispelCategoryType.ALL, SkillTargetSlot.DEBUFF, 100, 3,
				100, false);
		player.setTarget(null);
		PacketSendUtility.sendPacket(player, new SM_TARGET_SELECTED(player));
		if (lastAttacker instanceof Player && lastAttacker.getObjectId() != player.getObjectId()) {
			Player killer = (Player) lastAttacker;
			killer.setTotalKills(killer.getTotalKills() + 1);
			if (killer.getPlayerGroup2() != null) {
				killer.getPlayerGroup2().setKillCount(killer.getPlayerGroup2().getKillCount() + 1);
			} else if (killer.getPlayerAlliance2() != null) {
				killer.getPlayerAlliance2().setKillCount(killer.getPlayerAlliance2().getKillCount() + 1);
			}
		}
	}

	/**
	 * 对局结束第一阶段默认处理（公告与冻结）。
	 * First-phase default end handling (announce and freeze).
	 */
	protected void onEndFirstDefault() {
		if (getExpireTask() != null) {
			getExpireTask().cancel(true);
		}
		if (getBackgroundTask() != null) {
			getBackgroundTask().cancel(true);
		}
		if (getPlayers().size() > 0) {
			for (Player pl : getPlayers()) {
				if (!pl.getLifeStats().isAlreadyDead()) {
					healPlayer(pl, false);
				}
			}
		}
		if (getGroups().size() > 0) {
			for (PlayerGroup group : getGroups()) {
				for (Player pl : group.getMembers()) {
					if (!pl.getLifeStats().isAlreadyDead()) {
						healPlayer(pl, false);
					}
				}
			}
		}
		if (getAlliances().size() > 0) {
			for (PlayerAlliance alliance : getAlliances()) {
				for (Player pl : alliance.getMembers()) {
					if (pl == null) {
						continue;
					}
					if (!pl.getLifeStats().isAlreadyDead()) {
						healPlayer(pl, false);
					}
				}
			}
		}
	}

	/**
	 * 对局结束默认清理：解散、回城、销毁实例。
	 * Default end cleanup: disband, return, destroy instance.
	 */
	protected void onEndDefault() {
		GameFeatureServices.ladderService().onBgEnd(this);
		if (getPlayers().size() > 0) {
			for (Player pl : getPlayers()) {
				freezePlayer(pl, 7500);
			}
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					for (Player pl : getPlayers()) {
						returnToPreviousLocation(pl);
					}
				}
			}, 5000);
		}
		if (getGroups().size() > 0) {
			for (PlayerGroup group : getGroups()) {
				for (Player pl : group.getMembers()) {
					freezePlayer(pl, 7500);
				}
			}
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					for (PlayerGroup group : getGroups()) {
						for (Player pl : group.getMembers()) {
							returnToPreviousLocation(pl);
						}
						if (!isTournament() && shouldDisband()) {
							scheduleGroupDisband(group, 2000);
						}
					}
				}
			}, 5000);
		}
		if (getAlliances().size() > 0) {
			for (PlayerAlliance alliance : getAlliances()) {
				for (Player pl : alliance.getMembers()) {
					if (pl == null) {
						continue;
					}
					freezePlayer(pl, 7500);
				}
			}
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					for (PlayerAlliance alliance : getAlliances()) {
						for (Player pl : alliance.getMembers()) {
							if (pl == null) {
								continue;
							}
							returnToPreviousLocation(pl);
						}
						if (!isTournament() && shouldDisband()) {
							scheduleAllianceDisband(alliance, 2000);
						}
					}
				}
			}, 5000);
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				List<Player> spectators = new ArrayList<Player>(getSpectators());
				for (Player pl : spectators) {
					onSpectatorLeave(pl, true);
				}
				getSpectators().removeAll(spectators);
			}
		}, 5000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				for (Player pl : getInstance().getPlayersInside()) {
					returnToPreviousLocation(pl);
				}
			}
		}, 15000);

		this.isDone = true;
	}

	/**
	 * 观战者加入：隐身传送进场并启动计时。
	 * Spectator joins: hide, teleport in and start timer.
	 *
	 * spectator
	 */
	public void onSpectatorJoin(Player spectator) {
		spectator.setBattleground(this);
		spectator.setSpectating(true);
		getSpectators().add(spectator);
		InstanceService.registerPlayerWithInstance(getInstance(), spectator);
		previousLocations.put(spectator.getObjectId(), spectator.getPosition().clone());
		SpawnPosition pos = getSpawnPositions().get(Rnd.get(getSpawnPositions().size()));
		TeleportService2.teleportTo(spectator, getMapId(), getInstanceId(), pos.getX(), pos.getY(), pos.getZ());
		spectator.getEffectController().setAbnormal(AbnormalState.HIDE.getId());
		spectator.setVisualState(CreatureVisualState.HIDE3);
		spectator.setInvul(true);
		spectator.setSeeState(CreatureSeeState.SEARCH2);
		PacketSendUtility.broadcastPacket(spectator, new SM_PLAYER_STATE(spectator), true);
		createTimer(spectator, getSecondsLeft());
		scheduleAnnouncement(spectator, "You have join " + getName() + " <Spectator> battleground!", 0);
	}

	/**
	 * 观战者离开：还原状态并传回原坐标。
	 * Spectator leaves: restore state and return to previous location.
	 *
	 * spectator
	 * @param isIterating 是否在批量遍历中（避免并发修改） / whether iterating (avoid CME)
	 */
	public void onSpectatorLeave(final Player spectator, boolean isIterating) {
		endTimer(spectator);
		returnToPreviousLocation(spectator);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				spectator.getEffectController().unsetAbnormal(AbnormalState.HIDE.getId());
				spectator.unsetVisualState(CreatureVisualState.HIDE3);
				spectator.setInvul(false);
				spectator.unsetSeeState(CreatureSeeState.SEARCH2);
				spectator.setSpectating(false);
				PacketSendUtility.broadcastPacket(spectator, new SM_PLAYER_STATE(spectator), true);
			}
		}, TELEPORT_DEFAULT_DELAY);
		if (!isIterating) {
			List<Player> spectators = getSpectators();
			synchronized (spectators) {
				spectators.remove(spectator);
			}
		}
	}

	/**
	 * 重连已离开的参赛者，恢复队伍/位置与天梯扣分。
	 * Reconnects a leaver, restoring team/position and undoing leave penalty.
	 *
	 * reconnecting player
	 */
	public void reconnectPlayer(Player player) {
		if (player.getBattleground() != null) {
			return;
		}
		if (player.isAfk()) {
			player.setAfk(false);
		}
		AionObject obj = null;
		synchronized (getLeavers()) {
			obj = getLeavers().remove(player.getObjectId());
		}
		String msg = player.getName() + " is back in <Battleground>!";
		boolean success = false;
		SpawnPosition pos = null;
		if (obj == null) {
			success = true;
			addPlayer(player);
			int bgIndex = getPlayers().get(getPlayers().size() - 1).getBgIndex() + 1;
			player.setBgIndex(bgIndex);
			pos = getSpawnPositions().get(Rnd.get(getSpawnPositions().size()));
			for (Player pl : getPlayers()) {
				scheduleAnnouncement(pl, msg, 0);
			}
		} else if (obj instanceof PlayerAlliance) {
			success = true;
			PlayerAlliance alliance = (PlayerAlliance) obj;
			PlayerAllianceService.onPlayerLogin(player);
			pos = getSpawnPositions().get(alliance.getBgIndex());
			for (PlayerAlliance ally : getAlliances()) {
				for (Player pl : ally.getMembers()) {
					if (pl == null) {
						continue;
					}
					scheduleAnnouncement(pl, msg, 0);
				}
			}
		} else if (obj instanceof PlayerGroup) {
			success = true;
			PlayerGroup group = (PlayerGroup) obj;
			PlayerGroupService.onPlayerLogin(player);
			pos = getSpawnPositions().get(group.getBgIndex());
			for (PlayerGroup grp : getGroups()) {
				for (Player pl : grp.getMembers()) {
					scheduleAnnouncement(pl, msg, 0);
				}
			}
		}
		if (success) {
			player.setBattleground(this);
			preparePlayer(player, 0, false);
			performTeleport(player, pos.getX(), pos.getY(), pos.getZ());
			getLadderDAO().setLeaves(player, getLadderDAO().getLeaves(player) - 1);
			getLadderDAO().addRating(player, K_VALUE);
		}
	}

	/**
	 * 获取天梯 DAO。
	 * Returns the ladder DAO.
	 *
	 * DAO
	 */
	protected static LadderDAO getLadderDAO() {
		return DAOManager.getDAO(LadderDAO.class);
	}

	/**
	 * 随机选取地图并创建副本实例。
	 * Picks a random map and creates a world instance.
	 *
	 * @return 实例，失败为 null / instance or null
	 */
	protected WorldMapInstance createInstance() {
		if (maps == null || maps.size() == 0) {
			return null;
		}
		this.map = maps.get(Rnd.get(maps.size()));
		this.mapId = map.getMapId();
		WorldMapInstance instance = InstanceService.getNextBgInstance(getMapId());
		if (instance != null) {
			setInstanceId(instance.getInstanceId());
		}
		setInstance(instance);
		setStartStamp(System.currentTimeMillis());
		return instance;
	}

	/**
	 * 为场内玩家打开配置的静态门。
	 * Opens configured static doors for players inside.
	 */
	protected void openStaticDoors() {
		if (getMap().getStaticDoors() == null || getMap().getStaticDoors().isEmpty()) {
			return;
		}
		for (Player pl : getPlayers()) {
			for (Integer doorId : getMap().getStaticDoors()) {
				GameFeatureServices.staticDoorService().openStaticDoor(pl, doorId);
			}
		}
	}

	/**
	 * 获取战场名称。
	 * Returns the battleground name.
	 *
	 * name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取战场描述。
	 * Returns the battleground description.
	 *
	 * description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 获取最小人数。
	 * Returns minimum size.
	 *
	 * min size
	 */
	public int getMinSize() {
		return minSize;
	}

	/**
	 * 获取最大人数。
	 * Returns maximum size.
	 *
	 * max size
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * 获取队伍数量。
	 * Returns team count.
	 *
	 * team count
	 */
	public int getTeamCount() {
		return teamCount;
	}

	/**
	 * 获取当前地图 ID。
	 * Returns current map id.
	 *
	 * map id
	 */
	public int getMapId() {
		return mapId;
	}

	/**
	 * 设置对局时长（秒）。
	 * Sets match length in seconds.
	 *
	 * length in seconds
	 */
	public void setMatchLength(int matchLength) {
		this.matchLength = matchLength;
	}

	/**
	 * 获取对局时长（秒）。
	 * Returns match length in seconds.
	 *
	 * length in seconds
	 */
	public int getMatchLength() {
		return matchLength;
	}

	/**
	 * 获取当前战场地图配置。
	 * Returns the current battleground map config.
	 *
	 * map config
	 */
	public BattlegroundMap getMap() {
		return map;
	}

	/**
	 * 设置副本实例 ID。
	 * Sets instance id.
	 *
	 * instance id
	 */
	protected void setInstanceId(int instanceId) {
		this.instanceId = instanceId;
	}

	/**
	 * 获取副本实例 ID。
	 * Returns instance id.
	 *
	 * instance id
	 */
	public int getInstanceId() {
		return instanceId;
	}

	/**
	 * 设置战场注册 ID。
	 * Sets registered battleground id.
	 *
	 * battleground id
	 */
	public void setBgId(Integer bgId) {
		this.bgId = bgId;
	}

	/**
	 * 获取战场注册 ID。
	 * Returns registered battleground id.
	 *
	 * battleground id
	 */
	public Integer getBgId() {
		return bgId;
	}

	/**
	 * 设置对局开始时间戳。
	 * Sets match start timestamp.
	 *
	 * timestamp
	 */
	protected void setStartStamp(long startStamp) {
		this.startStamp = startStamp;
	}

	/**
	 * 获取对局开始时间戳。
	 * Returns match start timestamp.
	 *
	 * timestamp
	 */
	public long getStartStamp() {
		return startStamp;
	}

	/**
	 * 设置是否锦标赛模式。
	 * Sets tournament mode flag.
	 *
	 * @param isTournament 是否锦标赛 / tournament flag
	 */
	public void setIsTournament(boolean isTournament) {
		this.isTournament = isTournament;
	}

	/**
	 * 是否锦标赛模式。
	 * Whether tournament mode.
	 *
	 * @return 若 tournament 则为 true / true if tournament
	 */
	public boolean isTournament() {
		return isTournament;
	}

	/**
	 * 设置是否活动模式。
	 * Sets event mode flag.
	 *
	 * event flag
	 */
	public void setIsEvent(boolean isEvent) {
		this.isEvent = isEvent;
	}

	/**
	 * 是否活动模式。
	 * Whether event mode.
	 *
	 * @return 若 event 则为 true / true if event
	 */
	public boolean isEvent() {
		return isEvent;
	}

	/**
	 * 是否 1v1 模式。
	 * Whether 1v1 mode.
	 *
	 * @return 若 1v1 则为 true / true if 1v1
	 */
	public boolean is1v1() {
		return is1v1;
	}

	/**
	 * 设置是否 1v1 模式。
	 * Sets 1v1 mode flag.
	 *
	 * @param is1v1 是否 1v1 / 1v1 flag
	 */
	public void setIs1v1(boolean is1v1) {
		this.is1v1 = is1v1;
	}

	/**
	 * 结束时是否解散队伍。
	 * Whether teams should be disbanded on end.
	 *
	 * @return 若 should disband 则为 true / true if should disband
	 */
	public boolean shouldDisband() {
		return shouldDisband;
	}

	/**
	 * 设置结束时是否解散队伍。
	 * Sets whether teams should disband on end.
	 *
	 * disband flag
	 */
	public void setShouldDisband(boolean shouldDisband) {
		this.shouldDisband = shouldDisband;
	}

	/**
	 * 是否基于队伍。
	 * Whether team-based.
	 *
	 * @return 若 team-based 则为 true / true if team-based
	 */
	public boolean isTeamBased() {
		return teamBased;
	}

	/**
	 * 设置是否基于队伍。
	 * Sets team-based flag.
	 *
	 * @param teamBased 是否基于队伍 / team-based flag
	 */
	public void setTeamBased(boolean teamBased) {
		this.teamBased = teamBased;
	}

	/**
	 * 获取世界地图实例。
	 * Returns the world map instance.
	 *
	 * instance
	 */
	public WorldMapInstance getInstance() {
		return instance;
	}

	/**
	 * 设置世界地图实例。
	 * Sets the world map instance.
	 *
	 * instance
	 */
	public void setInstance(WorldMapInstance instance) {
		this.instance = instance;
	}

	/**
	 * 获取单人参赛者列表。
	 * Returns solo participants.
	 *
	 * players
	 */
	protected List<Player> getPlayers() {
		return _players;
	}

	/**
	 * 获取小队参赛者列表。
	 * Returns group participants.
	 *
	 * groups
	 */
	protected List<PlayerGroup> getGroups() {
		return _groups;
	}

	/**
	 * 获取联盟参赛者列表。
	 * Returns alliance participants.
	 *
	 * alliances
	 */
	protected List<PlayerAlliance> getAlliances() {
		return _alliances;
	}

	/**
	 * 获取观战者列表。
	 * Returns spectators.
	 *
	 * spectators
	 */
	protected List<Player> getSpectators() {
		return _spectators;
	}

	/**
	 * 获取中途离开者映射。
	 * Returns leaver map for reconnect.
	 *
	 * @return 离开者映射 / leavers
	 */
	public Map<Integer, AionObject> getLeavers() {
		return _leavers;
	}

	/**
	 * 是否仍有参赛者。
	 * Whether any participants remain.
	 *
	 * @return 若 has players 则为 true / true if has players
	 */
	public boolean hasPlayers() {
		return (getPlayers().size() > 0 || getGroups().size() > 0 || getAlliances().size() > 0);
	}

	/**
	 * 设置过期/结束任务。
	 * Sets the expire/end task.
	 *
	 * task
	 */
	protected void setExpireTask(ScheduledFuture<?> expireTask) {
		this.expireTask = expireTask;
	}

	/**
	 * 获取过期/结束任务。
	 * Returns the expire/end task.
	 *
	 * task
	 */
	protected ScheduledFuture<?> getExpireTask() {
		return expireTask;
	}

	/**
	 * 获取后台周期任务。
	 * Returns the background periodic task.
	 *
	 * task
	 */
	public ScheduledFuture<?> getBackgroundTask() {
		return backgroundTask;
	}

	/**
	 * 设置后台周期任务。
	 * Sets the background periodic task.
	 *
	 * task
	 */
	public void setBackgroundTask(ScheduledFuture<?> backgroundTask) {
		this.backgroundTask = backgroundTask;
	}

	/**
	 * 获取显示名到实现类的别名表。
	 * Returns display-name to implementation-class aliases.
	 *
	 * aliases
	 */
	public static Map<String, Class<?>> getAliases() {
		return aliases;
	}

	/**
	 * 出生点坐标。
	 * Spawn position coordinates.
	 */
	public static class SpawnPosition {
		/** 映射 ID / Map id */
		private int mapId = 0;
		/** X coordinate / X coordinate */
		private float x;
		/** Y coordinate / Y coordinate */
		private float y;
		/** Z coordinate / Z coordinate */
		private float z;

		/**
		 * 使用 XYZ 构造出生点。
		 * Creates a spawn position from XYZ.
		 *
		 * @param x X 坐标 / x
		 * @param y Y 坐标 / y
		 * @param z Z 坐标 / z
		 */
		public SpawnPosition(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		/**
		 * 使用地图 ID 与 XYZ 构造出生点。
		 * Creates a spawn position with map id and XYZ.
		 *
		 * map id
		 * @param x X 坐标 / x
		 * @param y Y 坐标 / y
		 * @param z Z 坐标 / z
		 */
		public SpawnPosition(int mapId, float x, float y, float z) {
			this.mapId = mapId;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		/**
		 * 获取 X 坐标。
		 * Returns X.
		 *
		 * x
		 */
		public float getX() {
			return x;
		}

		/**
		 * 设置 X 坐标。
		 * Sets X.
		 *
		 * @param x X / x
		 */
		public void setX(float x) {
			this.x = x;
		}

		/**
		 * 获取 Y 坐标。
		 * Returns Y.
		 *
		 * y
		 */
		public float getY() {
			return y;
		}

		/**
		 * 设置 Y 坐标。
		 * Sets Y.
		 *
		 * @param y Y / y
		 */
		public void setY(float y) {
			this.y = y;
		}

		/**
		 * 获取 Z 坐标。
		 * Returns Z.
		 *
		 * z
		 */
		public float getZ() {
			return z;
		}

		/**
		 * 设置 Z 坐标。
		 * Sets Z.
		 *
		 * @param z Z / z
		 */
		public void setZ(float z) {
			this.z = z;
		}

		/**
		 * 获取地图 ID。
		 * Returns map id.
		 *
		 * map id
		 */
		public int getMapId() {
			return mapId;
		}

		/**
		 * 设置地图 ID。
		 * Sets map id.
		 *
		 * map id
		 */
		public void setMapId(int mapId) {
			this.mapId = mapId;
		}
	}

	/**
	 * 战场地图配置：出生点、静态门、击杀高度与飞行限制。
	 * Battleground map config: spawns, static doors, kill Z and flight restriction.
	 */
	public static class BattlegroundMap {
		/** 映射 ID / Map id */
		private int mapId = 0;
		/** 出生点列表。 / Spawn points. */
		private List<SpawnPosition> spawnPoints = null;
		/** 静态门 ID / Static door ids */
		private List<Integer> staticDoors = null;
		/** Kill Z threshold / Kill Z threshold */
		private float killZ = 0f;
		/** 是否禁止飞行。 / Whether flight is restricted. */
		private boolean restrictFlight = false;

		/**
		 * 创建指定地图的配置。
		 * Creates map config for the given map id.
		 *
		 * map id
		 */
		public BattlegroundMap(int mapId) {
			this.setMapId(mapId);
		}

		/**
		 * 添加出生点。
		 * Adds a spawn position.
		 *
		 * spawn position
		 */
		public void addSpawn(SpawnPosition pos) {
			if (spawnPoints == null) {
				spawnPoints = new ArrayList<SpawnPosition>();
			}
			spawnPoints.add(pos);
		}

		/**
		 * 添加静态门 ID。
		 * Adds a static door id.
		 *
		 * door id
		 */
		public void addStaticDoor(Integer doorId) {
			if (staticDoors == null) {
				staticDoors = new ArrayList<Integer>();
			}
			staticDoors.add(doorId);
		}

		/**
		 * 设置地图 ID。
		 * Sets map id.
		 *
		 * map id
		 */
		public void setMapId(int mapId) {
			this.mapId = mapId;
		}

		/**
		 * 获取地图 ID。
		 * Returns map id.
		 *
		 * map id
		 */
		public int getMapId() {
			return mapId;
		}

		/**
		 * 获取出生点列表。
		 * Returns spawn points.
		 *
		 * spawn points
		 */
		public List<SpawnPosition> getSpawnPoints() {
			return spawnPoints;
		}

		/**
		 * 设置出生点列表。
		 * Sets spawn points.
		 *
		 * spawn points
		 */
		public void setSpawnPoints(List<SpawnPosition> spawnPoints) {
			this.spawnPoints = spawnPoints;
		}

		/**
		 * 获取静态门 ID 列表。
		 * Returns static door ids.
		 *
		 * door ids
		 */
		public List<Integer> getStaticDoors() {
			return staticDoors;
		}

		/**
		 * 设置坠落击杀高度。
		 * Sets kill Z.
		 *
		 * kill Z
		 */
		public void setKillZ(float killZ) {
			this.killZ = killZ;
		}

		/**
		 * 获取坠落击杀高度。
		 * Returns kill Z.
		 *
		 * kill Z
		 */
		public float getKillZ() {
			return killZ;
		}

		/**
		 * 是否限制飞行。
		 * Whether flight is restricted.
		 *
		 * @return 若 restricted 则为 true / true if restricted
		 */
		public boolean isRestrictFlight() {
			return restrictFlight;
		}

		/**
		 * 设置是否限制飞行。
		 * Sets flight restriction.
		 *
		 * restriction flag
		 */
		public void setRestrictFlight(boolean restrictFlight) {
			this.restrictFlight = restrictFlight;
		}
	}
}
