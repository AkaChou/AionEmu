package com.aionemu.gameserver.services.events.bg;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import lombok.Getter;
import lombok.Setter;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.persistence.dao.LadderPlayerDao;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 战场基类，封装匹配、对局生命周期、天梯评分、观战与公共工具逻辑。
 * Base battleground class encapsulating matchmaking, match lifecycle, ladder rating, spectating and shared helpers.
 *
 * @author Rinzler (Encom)
 */
@Getter
@Setter
public abstract class Battleground {

	/**
	 * 设置活动战场标记（历史方法名，保留以兼容既有调用）。
	 * Marks this battleground as an event match (legacy setter name kept for existing callers).
	 *
	 * @param isEvent 是否活动战场 / whether event mode
	 */
	public void setIsEvent(boolean isEvent) {
		this.isEvent = isEvent;
	}

	/**
	 * 默认传送延迟（毫秒）。 / Default teleport delay in ms.
	 */
	protected static final int TELEPORT_DEFAULT_DELAY = 0;
	/**
	 * Elo 评分 K 值 / Elo rating K-value.
	 */
	protected final int K_VALUE = 20;

	/**
	 * 战场显示名 → 实现类。 / Display name → implementation class.
	 * -- GETTER --
	 * 获取显示名到实现类的别名表。
	 * Returns display-name to implementation-class aliases.
	 */
	@Getter
	protected static final Map<String, Class<?>> aliases = Map.of(
		"[DeathMatch]", DeathmatchBg.class,
		"[1 VS 1]", SoloSurvivorBg.class,
		"[Team VS Team]", TwoTeamSmallBg.class
	);
	/**
	 * 战场名称。 / Battleground name.
	 */
	protected String name = "";
	/**
	 * 战场描述。 / Battleground description.
	 */
	protected String description = "";
	/**
	 * 单队/单局最小人数。 / Minimum size per team/match.
	 */
	protected int minSize = 0;
	/**
	 * 单队/单局最大人数。 / Maximum size per team/match.
	 */
	protected int maxSize = 0;
	/**
	 * 队伍数量。 / Number of teams.
	 */
	protected int teamCount = 0;
	/**
	 * 对局时长（秒）。 / Match length in seconds.
	 */
	protected int matchLength = 0;
	/**
	 * 可选战场地图列表。 / Available battleground maps.
	 */
	protected List<BattlegroundMap> maps = new ArrayList<>();
	/**
	 * 当前副本 ID / Current instance id
	 */
	protected int instanceId = -1;
	/**
	 * 战场注册 ID / Registered battleground id.
	 */
	protected Integer bgId = -1;
	/**
	 * 对局开始时间戳。 / Match start timestamp.
	 */
	protected long startStamp = 0;
	/**
	 * 是否锦标赛模式。 / Whether tournament mode.
	 * -- GETTER --
	 * 是否锦标赛模式。
	 * Whether tournament mode.
	 * 若 tournament 则为 true / true if tournament
	 */
	protected boolean isTournament = false;
	/**
	 * 是否活动模式。 / Whether event mode.
	 * -- GETTER --
	 * 是否活动模式。
	 * Whether event mode.
	 * 若 event 则为 true / true if event
	 */
	protected boolean isEvent = false;
	/**
	 * 是否 1v1 模式 / Whether 1v1 mode.
	 * -- GETTER --
	 * 是否 1v1 模式。
	 * Whether 1v1 mode.
	 * 若 1v1 则为 true / true if 1v1
	 */
	protected boolean is1v1 = false;
	/**
	 * 当前世界地图实例。 / Current world map instance.
	 */
	protected WorldMapInstance instance = null;
	/**
	 * 过期/结束任务。 / Expire/end task.
	 */
	protected ScheduledFuture<?> expireTask = null;
	/**
	 * 后台周期任务。 / Background periodic task.
	 * -- GETTER --
	 * 获取后台周期任务。
	 * Returns the background periodic task.
	 */
	protected ScheduledFuture<?> backgroundTask = null;
	/**
	 * 后台任务计数。 / Background task counter.
	 */
	protected int backgroundCounter = 0;
	/**
	 * 当前选用地图。 / Currently selected map.
	 */
	protected BattlegroundMap map = null;
	/**
	 * 当前映射 ID / Current map id
	 */
	protected int mapId = 0;
	/**
	 * 对局是否已结束。 / Whether match is finished.
	 */
	protected boolean isDone = false;
	/**
	 * 结束时是否解散队伍。 / Whether to disband teams on end.
	 */
	protected boolean shouldDisband = true;
	/**
	 * 是否基于队伍。 / Whether team-based.
	 * -- GETTER --
	 * 是否基于队伍。
	 * Whether team-based.
	 * 若 team-based 则为 true / true if team-based
	 */
	protected boolean teamBased = false;
	/**
	 * 玩家进场前坐标。 / Player locations before entry.
	 */
	protected Map<Integer, WorldPosition> previousLocations = new HashMap<>();
	/**
	 * 单人参赛者列表。 / Solo participants.
	 */
	protected List<Player> _players = new CopyOnWriteArrayList<>();
	/**
	 * 小队参赛者列表。 / Group participants.
	 */
	protected List<PlayerGroup> _groups = new CopyOnWriteArrayList<>();
	/**
	 * 联盟参赛者列表。 / Alliance participants.
	 */
	protected List<PlayerAlliance> _alliances = new CopyOnWriteArrayList<>();
	/**
	 * 观战者列表。 / Spectators.
	 */
	protected List<Player> _spectators = new CopyOnWriteArrayList<>();
	/**
	 * 中途离开者（用于重连）。 / Leavers (for reconnect).
	 */
	protected Map<Integer, AionObject> _leavers = Collections.synchronizedMap(new LinkedHashMap<>());

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
	 * @param player       死亡玩家 / dead player
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	public abstract void onDie(final Player player, Creature lastAttacker);

	/**
	 * 处理玩家离开战场。
	 * Handles a player leaving the battleground.
	 *
	 * @param player   离开的玩家 / leaving player
	 * @param isLogout 是否登出 / whether logout
	 * @param isAfk    是否挂机 / whether AFK
	 */
	public abstract void onLeave(Player player, boolean isLogout, boolean isAfk);

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
	 * 获取当前地图的出生点列表。
	 * Returns spawn positions of the current map.
	 *
	 * @return 出生点列表 / spawn positions
	 */
	public List<SpawnPosition> getSpawnPositions() {
		if (map == null) {
			return new ArrayList<>();
		} else {
			return map.getSpawnPoints();
		}
	}

	/**
	 * 创建锦标赛对局；默认不支持。
	 * Creates a tournament match; unsupported by default.
	 *
	 * @param teams 各队玩家列表 / teams of players
	 * @return 成功则 true / true on success
	 */
	public boolean createTournament(List<List<Player>> teams) {
		return false;
	}

	/**
	 * 按排队人数与模式计算本局抽取规模。
	 * Computes match draw size from queue size and mode.
	 *
	 * @param playerCount 排队人数 / queue size
	 * @return 本局抽取规模 / draw size
	 */
	protected int getRandomSize(int playerCount) {
		int avgCount = playerCount / getTeamCount();
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
	 * @param players 排队玩家对象 ID / queued player ids
	 * @return 成功则 true / true on success
	 */
	protected boolean handleQueueSolo(List<Integer> players) {
		int size = getRandomSize(players.size());
		if (players.size() < size) {
			return false;
		}
		int playerIndex = 0;
		while (!players.isEmpty()) {
			int objId = players.removeFirst();
			Player pl = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
			if (pl == null) {
				continue;
			}
			pl.setBgIndex(playerIndex);
			pl.setBattleground(this);
			addPlayer(pl);
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
	 * @param players 排队玩家对象 ID / queued player ids
	 * @return 成功则 true / true on success
	 */
	protected boolean handleQueueGroup(List<Integer> players) {
		int groupSize = getRandomSize(players.size());
		if (players.size() < groupSize * getTeamCount()) {
			return false;
		}
		int groupIndex = 0;
		while (players.size() >= groupSize) {
			List<Player> groupPlayers = new ArrayList<>();
			while (groupPlayers.size() < groupSize && !players.isEmpty()) {
				int objId = players.removeFirst();
				Player pl = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objId);
				if (pl != null) {
					removePlayerFromTeam(pl);
					groupPlayers.add(pl);
					pl.setBattleground(this);
				}
			}
			PlayerGroup group = PlayerGroupService.createGroup(groupPlayers.getFirst());
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
	 * 为各队创建小队并登记。
	 * Creates groups for each team and registers them.
	 *
	 * @param teams 各队玩家 / teams of players
	 * @return 成功则 true / true on success
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
			List<Player> players = teams.removeFirst();
			while (players.size() > groupSize) {
				players.removeLast();
			}
			for (Player pl : players) {
				pl.setBattleground(this);
				removePlayerFromTeam(pl);
			}
			PlayerGroup group = PlayerGroupService.createGroup(players.getFirst());
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
	 * 登记单人参赛者列表。
	 * Registers solo participants.
	 *
	 * @param players 各方玩家列表 / sides of players
	 * @return 成功则 true / true on success
	 */
	protected boolean createPlayers(List<List<Player>> players) {
		List<Integer> playerList = new ArrayList<>();
		for (List<Player> plList : players) {
			for (Player pl : plList) {
				playerList.add(pl.getObjectId());
			}
		}
		while (playerList.size() > getMaxSize()) {
			playerList.removeLast();
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
	 * @param group 小队 / group
	 */
	protected void addGroup(PlayerGroup group) {
		if (!getGroups().contains(group)) {
			getGroups().add(group);
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
	 * @param player   玩家 / player
	 * @param duration 时长毫秒 / duration in ms
	 */
	protected void freezePlayer(final Player player, int duration) {
		player.getEffectController().setAbnormal(AbnormalState.PARALYZE.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			player.getEffectController().unsetAbnormal(AbnormalState.PARALYZE.getId());
			player.getEffectController().updatePlayerEffectIcons();
			player.getEffectController().broadCastEffects();
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
	 * @param player  玩家 / player
	 * @param resetDp 是否重置 DP / whether to reset DP
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
	 * @param x      X 坐标 / x
	 * @param y      Y 坐标 / y
	 * @param z      Z 坐标 / z
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
		List<Integer> delayIds = new ArrayList<>();
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
	 * @param player 玩家 / player
	 * @param sender 发送者名称 / sender name
	 * @param msg    消息 / message
	 * @param delay  延迟毫秒 / delay ms
	 */
	protected void scheduleAnnouncement(final Player player, final String sender, final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(() -> PacketSendUtility.sendSys3Message(player, sender, msg), delay);
		} else {
			PacketSendUtility.sendSys3Message(player, sender, msg);
		}
	}

	/**
	 * 延迟向玩家发送默认发送者公告。
	 * Schedules a default-sender announcement to the player.
	 *
	 * @param player 玩家 / player
	 * @param msg    消息 / message
	 * @param delay  延迟毫秒 / delay ms
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
	 * @param msg 消息 / message
	 */
	protected void specAnnounce(String msg) {
		for (Player spectator : getSpectators()) {
			PacketSendUtility.sendSys3Message(spectator, "BG", msg);
		}
	}

	/**
	 * 向玩家安排倒计时公告。
	 * Schedules countdown announcements for the player.
	 *
	 * @param player    玩家 / player
	 * @param startTime 起始延迟毫秒 / start delay ms
	 */
	protected void scheduleCountdown(Player player, int startTime) {
		for (int i = 5; i > 0; i--) {
			scheduleAnnouncement(player, "The match start in " + i + " seconds!", startTime - i * 1000);
		}
	}

	/**
	 * 延迟解散小队。
	 * Schedules group disband.
	 *
	 * @param group 小队 / group
	 */
	protected void scheduleGroupDisband(final PlayerGroup group) {
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			while (!group.isEmpty()) {
				PlayerGroupService.removePlayer((Player) group.getMembers().toArray()[0]);
			}
		}, 2000);
	}

	/**
	 * 延迟解散联盟。
	 * Schedules alliance disband.
	 *
	 * @param alliance 联盟 / alliance
	 */
	protected void scheduleAllianceDisband(final PlayerAlliance alliance) {
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			while (!alliance.isEmpty()) {
				PlayerAllianceService.removePlayer(((Player) alliance.getMembers().toArray()[0]));
			}
		}, 2000);
	}

	/**
	 * 准备玩家（默认发送公告）。
	 * Prepares the player (announce by default).
	 *
	 * @param pl   玩家 / player
	 * @param time 准备时长毫秒 / prepare time ms
	 */
	protected void preparePlayer(final Player pl, int time) {
		this.preparePlayer(pl, time, true);
	}

	/**
	 * 准备玩家：冻结、治疗、清 CD，可选公告。
	 * Prepares the player: freeze, heal, clear CDs; optional announce.
	 *
	 * @param pl       玩家 / player
	 * @param time     准备时长毫秒 / prepare time ms
	 * @param announce 是否公告 / whether to announce
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
			scheduleCountdown(pl, time);
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
			GameThreadPoolServices.threadPoolManager().schedule(() -> pl.getEffectController().removeAllEffects(), 2500);
			GameThreadPoolServices.threadPoolManager().schedule(() -> createTimer(pl, getSecondsLeft()), time - 5000);
		} else {
			pl.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
			pl.getEffectController().removeEffectByDispelCat(DispelCategoryType.ALL, SkillTargetSlot.DEBUFF, 100, 2,
				100);
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
		List<Integer> delay = new ArrayList<>();
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
	 * 计算对局剩余秒数。
	 * Remaining match seconds.
	 *
	 * @return 剩余秒数 / seconds left
	 */
	public int getSecondsLeft() {
		return (getMatchLength() - Math.round((float) (System.currentTimeMillis() - getStartStamp()) / 1000));
	}

	/**
	 * 向玩家创建任务式倒计时 UI。
	 * Creates quest-style timer UI for the player.
	 *
	 * @param player  玩家 / player
	 * @param seconds 秒数 / seconds
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
	 * @param player       玩家 / player
	 * @param ratingChange 评分变化量 / rating delta
	 */
	protected void playerWinMatch(Player player, int ratingChange) {
		if (is1v1()) {
			return;
		}
		BattlegroundLadder.playerWinMatch(player, ratingChange);
	}

	/**
	 * 记录玩家负场与评分变化。
	 * Records a player loss and rating change.
	 *
	 * @param player       玩家 / player
	 * @param ratingChange 评分变化量 / rating delta
	 */
	protected void playerLoseMatch(Player player, int ratingChange) {
		if (is1v1()) {
			return;
		}
		BattlegroundLadder.playerLoseMatch(player, ratingChange);
	}

	/**
	 * 读取天梯 DAO。
	 * Resolves the ladder DAO.
	 *
	 * @return 天梯 DAO / ladder DAO
	 */
	protected static LadderPlayerDao getLadderDAO() {
		return BattlegroundLadder.dao();
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
		setBackgroundTask(GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			backgroundCounter++;
			zCheck();
			if ((backgroundCounter % 5) == 0) {
				backgroundCounter = 0;
			}
		}, 30 * 1000, 1000));
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (getBackgroundTask() != null) {
				getBackgroundTask().cancel(true);
			}
		}, 10L * getMatchLength() * 1000);
	}

	/**
	 * 检测玩家是否低于击杀高度并处理坠落死亡。
	 * Checks whether players fell below kill Z and handles fall deaths.
	 */
	protected void zCheck() {
		if (!getGroups().isEmpty()) {
			for (PlayerGroup group : getGroups()) {
				for (Player pl : group.getMembers()) {
					checkKillZ(pl);
				}
			}
		}
		if (!getAlliances().isEmpty()) {
			for (PlayerAlliance alliance : getAlliances()) {
				for (Player pl : alliance.getMembers()) {
					checkKillZ(pl);
				}
			}
		}
		if (!getPlayers().isEmpty()) {
			for (Player pl : getPlayers()) {
				checkKillZ(pl);
			}
		}
	}

	private void checkKillZ(Player pl) {
		if (pl != null && !pl.getLifeStats().isAlreadyDead() && map.getMapId() == pl.getWorldId() && pl.getBattleground() != null) {
			if (pl.getZ() < map.getKillZ()) {
				pl.getLifeStats().reduceHp(100000, pl);
			}
		}
	}

	/**
	 * 默认离场处理：登记离开者、惩罚与清理。
	 * Default leave handling: register leaver, penalize and cleanup.
	 *
	 * @param player   玩家 / player
	 * @param isLogout 是否登出 / whether logout
	 * @param isAfk    是否挂机 / whether AFK
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
			List<Player> players = getPlayers();
			synchronized (players) {
				players.remove(player);
			}
			player.setBattleground(null);
			if (!this.isDone && !player.getController().isInShutdownProgress()) {
				getLadderDAO().addLeave(player);
				getLadderDAO().addRating(player, -K_VALUE);
			}
			removePlayerFromTeam(player);
			endTimer(player);
		}
	}

	/**
	 * 默认死亡处理：击杀统计与公告。
	 * Default death handling: kill stats and announcements.
	 *
	 * @param player       死亡玩家 / dead player
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
			100);
		player.setTarget(null);
		PacketSendUtility.sendPacket(player, new SM_TARGET_SELECTED(player));
		if (lastAttacker instanceof Player killer && !killer.getObjectId().equals(player.getObjectId())) {
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
		if (!getPlayers().isEmpty()) {
			for (Player pl : getPlayers()) {
				if (!pl.getLifeStats().isAlreadyDead()) {
					healPlayer(pl, false);
				}
			}
		}
		if (!getGroups().isEmpty()) {
			for (PlayerGroup group : getGroups()) {
				for (Player pl : group.getMembers()) {
					if (!pl.getLifeStats().isAlreadyDead()) {
						healPlayer(pl, false);
					}
				}
			}
		}
		if (!getAlliances().isEmpty()) {
			for (PlayerAlliance alliance : getAlliances()) {
				for (Player pl : alliance.getMembers()) {
					if (pl != null && !pl.getLifeStats().isAlreadyDead()) {
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
		if (!getPlayers().isEmpty()) {
			for (Player pl : getPlayers()) {
				freezePlayer(pl, 7500);
			}
			GameThreadPoolServices.threadPoolManager().schedule(() -> {
				for (Player pl : getPlayers()) {
					returnToPreviousLocation(pl);
				}
			}, 5000);
		}
		if (!getGroups().isEmpty()) {
			for (PlayerGroup group : getGroups()) {
				for (Player pl : group.getMembers()) {
					freezePlayer(pl, 7500);
				}
			}
			GameThreadPoolServices.threadPoolManager().schedule(() -> {
				for (PlayerGroup group : getGroups()) {
					for (Player pl : group.getMembers()) {
						returnToPreviousLocation(pl);
					}
					if (!isTournament() && shouldDisband()) {
						scheduleGroupDisband(group);
					}
				}
			}, 5000);
		}
		if (!getAlliances().isEmpty()) {
			for (PlayerAlliance alliance : getAlliances()) {
				for (Player pl : alliance.getMembers()) {
					if (pl != null) {
						freezePlayer(pl, 7500);
					}
				}
			}
			GameThreadPoolServices.threadPoolManager().schedule(() -> {
				for (PlayerAlliance alliance : getAlliances()) {
					for (Player pl : alliance.getMembers()) {
						if (pl != null) {
							returnToPreviousLocation(pl);
						}
					}
					if (!isTournament() && shouldDisband()) {
						scheduleAllianceDisband(alliance);
					}
				}
			}, 5000);
		}
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			List<Player> spectators = new ArrayList<>(getSpectators());
			for (Player pl : spectators) {
				onSpectatorLeave(pl, true);
			}
			getSpectators().removeAll(spectators);
		}, 5000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			for (Player pl : getInstance().getPlayersInside()) {
				returnToPreviousLocation(pl);
			}
		}, 15000);

		this.isDone = true;
	}

	/**
	 * 观战者离开：还原状态并传回原坐标。
	 * Spectator leaves: restore state and return to previous location.
	 *
	 * @param spectator   观战者 / spectator
	 * @param isIterating 是否在批量遍历中（避免并发修改） / whether iterating (avoid CME)
	 */
	public void onSpectatorLeave(final Player spectator, boolean isIterating) {
		endTimer(spectator);
		returnToPreviousLocation(spectator);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			spectator.getEffectController().unsetAbnormal(AbnormalState.HIDE.getId());
			spectator.unsetVisualState(CreatureVisualState.HIDE3);
			spectator.setInvul(false);
			spectator.unsetSeeState(CreatureSeeState.SEARCH2);
			spectator.setSpectating(false);
			PacketSendUtility.broadcastPacket(spectator, new SM_PLAYER_STATE(spectator), true);
		}, TELEPORT_DEFAULT_DELAY);
		if (!isIterating) {
			List<Player> spectators = getSpectators();
			synchronized (spectators) {
				spectators.remove(spectator);
			}
		}
	}

	/**
	 * 随机选取地图并创建副本实例。
	 * Picks a random map and creates a world instance.
	 */
	protected void createInstance() {
		if (maps == null || maps.isEmpty()) {
			return;
		}
		this.map = maps.get(Rnd.get(maps.size()));
		this.mapId = map.getMapId();
		WorldMapInstance instance = InstanceService.getNextBgInstance(getMapId());
		if (instance != null) {
			setInstanceId(instance.getInstanceId());
		}
		setInstance(instance);
		setStartStamp(System.currentTimeMillis());
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
	 * 设置副本实例 ID。
	 * Sets instance id.
	 *
	 * @param instanceId 实例 ID / instance id
	 */
	protected void setInstanceId(int instanceId) {
		this.instanceId = instanceId;
	}

	/**
	 * 设置对局开始时间戳。
	 * Sets match start timestamp.
	 *
	 * @param startStamp 时间戳 / timestamp
	 */
	protected void setStartStamp(long startStamp) {
		this.startStamp = startStamp;
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
	 * 获取单人参赛者列表。
	 * Returns solo participants.
	 *
	 * @return 单人参赛者列表 / players
	 */
	protected List<Player> getPlayers() {
		return _players;
	}

	/**
	 * 获取小队参赛者列表。
	 * Returns group participants.
	 *
	 * @return 小队参赛者列表 / groups
	 */
	protected List<PlayerGroup> getGroups() {
		return _groups;
	}

	/**
	 * 获取联盟参赛者列表。
	 * Returns alliance participants.
	 *
	 * @return 联盟参赛者列表 / alliances
	 */
	protected List<PlayerAlliance> getAlliances() {
		return _alliances;
	}

	/**
	 * 获取观战者列表。
	 * Returns spectators.
	 *
	 * @return 观战者列表 / spectators
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
		return (!getPlayers().isEmpty() || !getGroups().isEmpty() || !getAlliances().isEmpty());
	}

	/**
	 * 设置过期/结束任务。
	 * Sets the expire/end task.
	 *
	 * @param expireTask 过期/结束任务 / the task
	 */
	protected void setExpireTask(ScheduledFuture<?> expireTask) {
		this.expireTask = expireTask;
	}

	/**
	 * 获取过期/结束任务。
	 * Returns the expire/end task.
	 *
	 * @return 过期/结束任务 / the task
	 */
	protected ScheduledFuture<?> getExpireTask() {
		return expireTask;
	}

	/**
	 * 出生点坐标。
	 * Spawn position coordinates.
	 */
	@Setter
	@Getter
	public static class SpawnPosition {
		/**
		 * 地图 ID / Map id
		 * -- GETTER --
		 * 获取地图 ID。
		 * Returns map id.
		 * -- SETTER --
		 * 设置地图 ID。
		 * Sets map id.
		 */
		private int mapId = 0;
		/**
		 * X coordinate / X coordinate
		 * -- GETTER --
		 * 获取 X 坐标。
		 * Returns X.
		 * -- SETTER --
		 * 设置 X 坐标。
		 * Sets X.
		 */
		private float x;
		/**
		 * Y coordinate / Y coordinate
		 * -- GETTER --
		 * 获取 Y 坐标。
		 * Returns Y.
		 * -- SETTER --
		 * 设置 Y 坐标。
		 * Sets Y.
		 */
		private float y;
		/**
		 * Z coordinate / Z coordinate
		 * -- GETTER --
		 * 获取 Z 坐标。
		 * Returns Z.
		 * -- SETTER --
		 * 设置 Z 坐标。
		 * Sets Z.
		 */
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

	}

	/**
	 * 战场地图配置：出生点、静态门、击杀高度与飞行限制。
	 * Battleground map config: spawns, static doors, kill Z and flight restriction.
	 */
	@Getter
	public static class BattlegroundMap {
		/**
		 * 地图 ID / Map id
		 * -- GETTER --
		 * 获取地图 ID。
		 * Returns map id.
		 * -- SETTER --
		 * 设置地图 ID。
		 * Sets map id.
		 */
		@Setter
		private int mapId = 0;
		/**
		 * 出生点列表。 / Spawn points.
		 * -- GETTER --
		 * 获取出生点列表。
		 * Returns spawn points.
		 * -- SETTER --
		 * 设置出生点列表。
		 * Sets spawn points.
		 */
		@Setter
		private List<SpawnPosition> spawnPoints = null;
		/**
		 * 静态门 ID / Static door ids
		 * -- GETTER --
		 * 获取静态门 ID 列表。
		 * Returns static door ids.
		 */
		private final List<Integer> staticDoors = null;
		/**
		 * Kill Z threshold / Kill Z threshold
		 * -- GETTER --
		 * 获取坠落击杀高度。
		 * Returns kill Z.
		 * -- SETTER --
		 * 设置坠落击杀高度。
		 * Sets kill Z.
		 */
		@Setter
		private float killZ = 0f;

		/**
		 * 创建指定地图的配置。
		 * Creates map config for the given map id.
		 *
		 * @param mapId 地图 ID / map id
		 */
		public BattlegroundMap(int mapId) {
			this.setMapId(mapId);
		}

		/**
		 * 添加出生点。
		 * Adds a spawn position.
		 *
		 * @param pos 出生点 / spawn position
		 */
		public void addSpawn(SpawnPosition pos) {
			if (spawnPoints == null) {
				spawnPoints = new ArrayList<>();
			}
			spawnPoints.add(pos);
		}

	}
}
