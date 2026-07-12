package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dao.LadderDAO;
import com.aionemu.gameserver.eventEngine.Event;
import com.aionemu.gameserver.eventEngine.events.BattlegroundEvent;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.services.events.bg.Battleground;
import com.aionemu.gameserver.services.events.bg.DeathmatchBg;
import com.aionemu.gameserver.services.events.bg.SoloSurvivorBg;
import com.aionemu.gameserver.services.events.bg.TwoTeamBg;
import com.aionemu.gameserver.services.events.bg.TwoTeamSmallBg;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 天梯 / 战场匹配服务，管理普通与活动队列、战场创建、排名与名称伪装。
 * battleground matchmaking service managing normal and event queues, BG creation, ranks, and name masking.
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class LadderService {
	/** Spring 实例提供者 / Spring instance provider */
	private static volatile ObjectProvider<LadderService> instanceProvider;
	/** 活动战场排队列表。 / Event battleground queue list. */
	private List<AionObject> eventQueueList = Collections.synchronizedList(new ArrayList<AionObject>());
	/** 普通战场排队列表。 / Normal battleground queue list. */
	private List<AionObject> normalQueueList = Collections.synchronizedList(new ArrayList<AionObject>());
	/** 当前 battleground 映射 bgIdbattleground / Active battleground map (bgId → battleground) */
	private Map<Integer, Battleground> bgMap = Collections.synchronizedMap(new LinkedHashMap<Integer, Battleground>());
	/** 普通战场与事件引擎关联映射。 / Map linking normal BGs to event engine instances. */
	private Map<Integer, Event> normalBgMap = Collections.synchronizedMap(new LinkedHashMap<Integer, Event>());
	/** 当前活动战场模板。 / Current event battleground template. */
	private Battleground eventBg = null;
	/** 活动报名截止任务。 / Event registration deadline task. */
	private ScheduledFuture<?> eventTask = null;
	/** 普通报名截止任务。 / Normal registration deadline task. */
	private ScheduledFuture<?> normalTask = null;
	/** 普通战场是否开放报名。 / Whether normal BG registration is open. */
	boolean normalReady = false;
	/** 活动战场是否开放报名。 / Whether event BG registration is open. */
	boolean eventReady = false;
	/** 普通队列是否按队伍匹配。 / Whether normal queue uses team-based matchmaking. */
	boolean normalTeamBased = false;
	/** 活动队列是否按队伍匹配。 / Whether event queue uses team-based matchmaking. */
	boolean eventTeamBased = false;
	/** 排名刷新间隔（分钟）。 / Rank refresh interval in minutes. */
	private int rankUpdateInterval = 2;

	/**
	 * 构造服务并启动周期排名刷新。
	 * Constructs the service and starts periodic rank updates.
	 */
	public LadderService() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				UpdateRanks();
			}
		}, rankUpdateInterval * 60 * 1000, rankUpdateInterval * 60 * 1000);
		log.info(I18n.get("log.feb60173643c"));
	}

	/**
	 * 通过 DAO 刷新天梯排名。
	 * Refreshes ladder ranks via the DAO.
	 */
	public void UpdateRanks() {
		getLadderDAO().updateRanks();
	}

	/**
	 * 将玩家注册到普通战场队列。
	 * Registers a player into the normal battleground queue.
	 *
	 * @param player 玩家 / player
	 * @return 是否注册成功 / whether registration succeeded
	 */
	public boolean registerForNormal(Player player) {
		if (!isNormalReady() || player.getBattleground() != null) {
			return false;
		}
		if (player.isInGroup2() && isInQueue(player.getPlayerGroup2())) {
			return false;
		}
		if (isInQueue(player)) {
			unregisterFromQueue(player);
		}
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(1, 301550000, 0));
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(301550000, false));
		return normalQueueList.add(player);
	}

	/**
	 * 将玩家注册到活动战场队列。
	 * Registers a player into the event battleground queue.
	 *
	 * @param player 玩家 / player
	 * @return 是否注册成功 / whether registration succeeded
	 */
	public boolean registerForEvent(Player player) {
		if (!isEventReady() || player.getBattleground() != null) {
			return false;
		}
		if (player.isInGroup2() && isInQueue(player.getPlayerGroup2())) {
			return false;
		}
		if (isInQueue(player)) {
			unregisterFromQueue(player);
		}
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(1, 300350000, 0));
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(300350000, false));
		return eventQueueList.add(player);
	}

	/**
	 * 将玩家从普通战场队列移除。
	 * Removes a player from the normal battleground queue.
	 *
	 * @param player 玩家 / player
	 */
	public void unregisterForNormal(Player player) {
		normalQueueList.remove(player);
	}

	/**
	 * 将玩家从活动战场队列移除。
	 * Removes a player from the event battleground queue.
	 *
	 * @param player 玩家 / player
	 */
	public void unregisterForEvent(Player player) {
		eventQueueList.remove(player);
	}

	/**
	 * 判断玩家是否已在任一队列中。
	 * Returns whether the player is already in any queue.
	 *
	 * @param player 玩家 / player
	 * @return 是否在队列中 / whether queued
	 */
	public boolean isInQueue(Player player) {
		if (normalQueueList.contains(player) || eventQueueList.contains(player)) {
			return true;
		}
		return false;
	}

	/**
	 * 取消玩家在普通 / 活动队列中的全部报名并同步 UI。
	 * Cancels all of a player's normal/event queue registrations and syncs the UI.
	 *
	 * 玩家 / player
	 */
	public void unregisterFromQueue(Player player) {
		if (normalQueueList.contains(player)) {
			unregisterForNormal(player);
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(2, 301550000, 0));
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(301550000, true));
		}
		if (eventQueueList.contains(player)) {
			unregisterForEvent(player);
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(2, 300350000, 0));
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(300350000, true));
		}
	}

	/**
	 * 将队伍注册到普通战场队列。
	 * Registers a party into the normal battleground queue.
	 *
	 * party
	 *
	 * @param group
	 * @return 是否注册成功 / whether registration succeeded
	 */
	public boolean registerForNormal(PlayerGroup group) {
		if (!isNormalReady()) {
			return false;
		}
		for (Player pl : group.getMembers()) {
			if (pl.getBattleground() != null) {
				return false;
			} else if (isInQueue(pl)) {
				unregisterFromQueue(pl);
			}
		}
		if (isInQueue(group)) {
			unregisterFromQueue(group);
		}
		for (Player pl : group.getMembers()) {
			PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(1, 301550000, 2));
			PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(301550000, false));
		}
		return normalQueueList.add(group);
	}

	/**
	 * 将队伍注册到活动战场队列。
	 * Registers a party into the event battleground queue.
	 *
	 * party
	 *
	 * @param group
	 * @return 是否注册成功 / whether registration succeeded
	 */
	public boolean registerForEvent(PlayerGroup group) {
		if (!isEventReady()) {
			return false;
		}
		for (Player pl : group.getMembers()) {
			if (pl.getBattleground() != null) {
				return false;
			}
		}
		if (isInQueue(group)) {
			unregisterFromQueue(group);
		}
		for (Player pl : group.getMembers()) {
			PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(1, 300350000, 2));
			PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(300350000, false));
		}
		return eventQueueList.add(group);
	}

	/**
	 * 将队伍从普通战场队列移除。
	 * Removes a party from the normal battleground queue.
	 *
	 * party
	 */
	public void unregisterForNormal(PlayerGroup group) {
		normalQueueList.remove(group);
	}

	/**
	 * 将队伍从活动战场队列移除。
	 * Removes a party from the event battleground queue.
	 *
	 * party
	 */
	public void unregisterForEvent(PlayerGroup group) {
		eventQueueList.remove(group);
	}

	/**
	 * 判断队伍是否已在任一队列中。
	 * Returns whether the party is already in any queue.
	 *
	 * party
	 *
	 * @param group
	 * @return 是否在队列中 / whether queued
	 */
	public boolean isInQueue(PlayerGroup group) {
		if (normalQueueList.contains(group) || eventQueueList.contains(group)) {
			return true;
		}
		return false;
	}

	/**
	 * 取消队伍在普通 / 活动队列中的全部报名并同步 UI。
	 * Cancels all of a party's normal/event queue registrations and syncs the UI.
	 *
	 * party
	 */
	public void unregisterFromQueue(PlayerGroup group) {
		if (normalQueueList.contains(group)) {
			unregisterForNormal(group);
			for (Player pl : group.getMembers()) {
				PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(2, 301550000, 0));
				PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(301550000, true));
			}
		}
		if (eventQueueList.contains(group)) {
			unregisterForEvent(group);
			for (Player pl : group.getMembers()) {
				PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(2, 301550000, 0));
				PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(301550000, true));
			}
		}
	}

	/**
	 * 返回活动队列条目数（玩家或队伍条目）。
	 * Returns the number of entries in the event queue (players or parties).
	 *
	 * @return 队列条目数 / queue entry count
	 */
	public int getEventQueueSize() {
		return eventQueueList.size();
	}

	/**
	 * 统计活动队列中的实际玩家人数。
	 * Counts the actual number of players in the event queue.
	 *
	 * player count
	 */
	public int getEventQueuePlayers() {
		int players = 0;
		for (AionObject ao : queueSnapshot(eventQueueList)) {
			if (ao instanceof Player) {
				players++;
			} else if (ao instanceof PlayerGroup) {
				players += ((PlayerGroup) ao).getMembers().size();
			}
		}
		return players;
	}

	/**
	 * 按是否仅队伍模式随机实例化一个战场类型。
	 * Instantiates a random battleground type based on team-only mode.
	 *
	 * @param teamOnly 是否仅队伍模式 / whether team-only
	 * battleground instance
	 */
	private Battleground getRandomBg(boolean teamOnly) {
		Battleground bg = null;
		Class<?>[] bgs = new Class<?>[] { TwoTeamBg.class, TwoTeamSmallBg.class };
		if (!teamOnly) {
			bgs = new Class<?>[] { SoloSurvivorBg.class, SoloSurvivorBg.class, SoloSurvivorBg.class,
					SoloSurvivorBg.class, DeathmatchBg.class, TwoTeamSmallBg.class };
		}
		try {
			bg = (Battleground) bgs[Rnd.get(bgs.length)].getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			// log.error(I18n.get("log.8015e21ea7c3", e));
		}
		return bg;
	}

	/**
	 * 开启一轮普通战场报名，并在截止后处理匹配。
	 * Opens one normal battleground registration window and processes matchmaking after the deadline.
	 *
	 * @param event 战场事件 / battleground event
	 * @return 是否成功开启 / whether opened successfully
	 */
	public boolean createNormalBgs(final BattlegroundEvent event) {
		if (normalTask != null) {
			normalTask.cancel(false);
		}
		normalReady = true;
		normalTeamBased = Rnd.get(3) != 0;
		normalQueueList.clear();
		announceAll(
				"[BG Open] Register with the button located on the right of your skill bar. You have <2 Minutes> to register!!!");
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player pl) {
				if (pl.getBattleground() == null && !isInQueue(pl) && !GameFeatureServices.ffaService().isInArena(pl)) {
					PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(301550000, true));
				}
			}
		});
		normalTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				HandleNormalQueue(event);
				normalReady = false;
				normalTask = null;
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(301550000, false));
					}
				});
			}
		}, (normalTeamBased ? 60 : 30) * 1000);
		return true;
	}

	/**
	 * 开启一轮指定类型的活动战场报名。
	 * Opens one event battleground registration window of the given type.
	 *
	 * @param bg 活动战场模板 / event battleground template
	 * @param teamBased 是否按队伍匹配 / whether team-based
	 * @return 是否成功开启 / whether opened successfully
	 */
	public boolean createEventBg(Battleground bg, boolean teamBased) {
		if (eventBg != null) {
			return false;
		}
		if (eventTask != null) {
			eventTask.cancel(true);
		}
		eventReady = true;
		eventBg = bg;
		eventTeamBased = teamBased;
		eventQueueList.clear();
		if (eventTeamBased) {
			announceAll("WARNING!!! " + bg.getName()
					+ "The event start in 1 minute!!! Register you by using the right button on your skill bars!!!");
		} else {
			announceAll("WARNING!!! " + bg.getName()
					+ "The event start in 30 seconds ! Register you by using the right button on your skill bars!!!");
		}
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player pl) {
				if (pl.getBattleground() == null && !isInQueue(pl)) {
					PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(300350000, true));
				}
			}
		});
		eventTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(300350000, false));
					}
				});
				HandleEventQueue();
				eventTask.cancel(false);
				eventTask = null;
			}
		}, (eventTeamBased ? 60 : 30) * 1000);
		return true;
	}

	/**
	 * 处理普通队列匹配并创建战场。
	 * Processes normal-queue matchmaking and creates battlegrounds.
	 *
	 * @param event 战场事件 / battleground event
	 */
	private void HandleNormalQueue(BattlegroundEvent event) {
		List<List<Player>> validGroups = new ArrayList<List<Player>>();
		List<Integer> validParticipants = new ArrayList<Integer>();
		for (AionObject ao : queueSnapshot(normalQueueList)) {
			if (ao == null) {
				continue;
			}
			if (ao instanceof Player) {
				Player pl = (Player) ao;
				PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(2, 301550000, 0));
				if (!pl.isOnline() || pl.getBattleground() != null) {
					continue;
				}
				validGroups.add(Arrays.asList(pl));
				validParticipants.add(pl.getObjectId());
			} else if (ao instanceof PlayerGroup) {
				final PlayerGroup group = (PlayerGroup) ao;
				boolean add = true;
				for (Player pl : group.getMembers()) {
					PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(2, 301550000, 0));
					if (!pl.isOnline() || pl.getBattleground() != null) {
						add = false;
					}
				}
				if (add && normalTeamBased) {
					validGroups.add(new ArrayList<Player>() {
						{
							addAll(group.getMembers());
						}
					});
				}
			}
		}
		if (normalTeamBased) {
			Collections.shuffle(validGroups);
			Collections.sort(validGroups, new Comparator<List<Player>>() {
				@Override
				public int compare(List<Player> o1, List<Player> o2) {
					return -Integer.valueOf(o1.size()).compareTo(Integer.valueOf(o2.size()));
				}
			});
		} else {
			Collections.shuffle(validParticipants);
			SortParticipantList(validParticipants);
		}
		normalQueueList.clear();
		int iterations = 0;
		while (iterations++ < 40) {
			Battleground bg = getRandomBg(normalTeamBased);
			bg.setTeamBased(normalTeamBased);
			if (normalTeamBased) {
				List<List<Player>> usedGroups = new ArrayList<List<Player>>();
				List<List<Player>> groups = new ArrayList<List<Player>>();
				List<Integer> participants = new ArrayList<Integer>();
				if (validGroups.size() < bg.getTeamCount()) {
					continue;
				}
				for (int i = 0; i < bg.getTeamCount(); i++) {
					groups.add(new ArrayList<Player>());
					for (List<Player> group : validGroups) {
						if (!usedGroups.contains(group) && group.size() <= bg.getMaxSize()) {
							groups.set(i, cloneGroup(group));
							usedGroups.add(group);
							break;
						}
					}
				}
				if (groups.get(0).size() < bg.getMinSize()) {
					for (List<Player> group : validGroups) {
						if (!usedGroups.contains(group) && group.size() + groups.get(0).size() <= bg.getMaxSize()) {
							groups.get(0).addAll(group);
							usedGroups.add(group);
							if (groups.get(0).size() >= bg.getMinSize()) {
								break;
							}
						}
					}
				}
				if (groups.get(0).size() < bg.getMinSize() || groups.get(0).size() > bg.getMaxSize()) {
					continue;
				}
				boolean satisfied = true;
				for (int i = 1; i < bg.getTeamCount(); i++) {
					if (groups.get(i).size() < groups.get(0).size()) {
						for (List<Player> group : validGroups) {
							if (!usedGroups.contains(group)
									&& group.size() + groups.get(i).size() <= groups.get(0).size()) {
								groups.get(i).addAll(group);
								usedGroups.add(group);
							}
						}
					}
					if (groups.get(i).size() < groups.get(0).size()) {
						satisfied = false;
					}
				}
				if (!satisfied) {
					continue;
				}
				for (List<Player> group : groups) {
					for (Player pl : group) {
						participants.add(pl.getObjectId());
					}
				}
				validGroups.removeAll(usedGroups);
				bg.createMatch(participants);
			} else {
				bg.createMatch(validParticipants);
			}
			if (bg.hasPlayers()) {
				Integer bgId = registerBg(bg);
				normalBgMap.put(bgId, event);
				event.onCreate(bgId);
			}
		}
		if (event.getBgCount() == 0) {
			event.onEnd();
		}
		if (normalTeamBased) {
			for (List<Player> group : validGroups) {
				for (Player pl : group) {
					scheduleAnnouncement(pl, "No opponents found!!! Please wait for the next registration.", 0);
				}
			}
		} else {
			for (Integer objectId : validParticipants) {
				AionObject ao = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(objectId);
				if (ao != null && ao instanceof Player) {
					scheduleAnnouncement((Player) ao, "No opponents found!!! Please wait for the next registration.",
							0);
				}
			}
		}
	}

	/**
	 * 处理活动队列匹配并创建战场。
	 * Processes event-queue matchmaking and creates battlegrounds.
	 */
	private void HandleEventQueue() {
		List<List<Player>> validGroups = new ArrayList<List<Player>>();
		List<Integer> validParticipants = new ArrayList<Integer>();
		for (AionObject ao : queueSnapshot(eventQueueList)) {
			if (ao != null && ao instanceof Player) {
				Player pl = (Player) ao;
				PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(2, 300350000, 0));
				if (!pl.isOnline() || pl.getBattleground() != null) {
					continue;
				}
				validGroups.add(Arrays.asList(pl));
				validParticipants.add(ao.getObjectId());
			} else if (ao instanceof PlayerGroup) {
				final PlayerGroup group = (PlayerGroup) ao;
				boolean add = true;
				for (Player pl : group.getMembers()) {
					PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(2, 300350000, 0));
					if (!pl.isOnline() || pl.getBattleground() != null) {
						add = false;
					}
				}
				if (add && eventTeamBased) {
					validGroups.add(new ArrayList<Player>() {
						{
							addAll(group.getMembers());
						}
					});
				}
			}
		}
		if (eventTeamBased) {
			Collections.shuffle(validGroups);
			Collections.sort(validGroups, new Comparator<List<Player>>() {
				@Override
				public int compare(List<Player> o1, List<Player> o2) {
					return -Integer.valueOf(o1.size()).compareTo(Integer.valueOf(o2.size()));
				}
			});
		} else {
			Collections.shuffle(validParticipants);
			SortParticipantList(validParticipants);
		}
		Class<?> eventBgClass = eventBg.getClass();
		eventQueueList.clear();
		int iterations = 0;
		while (iterations++ < 40) {
			Battleground bg;
			try {
				bg = (Battleground) eventBgClass.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				continue;
			}
			bg.setTeamBased(eventTeamBased);
			bg.setIsEvent(true);
			if (eventTeamBased) {
				List<List<Player>> usedGroups = new ArrayList<List<Player>>();
				List<List<Player>> groups = new ArrayList<List<Player>>();
				List<Integer> participants = new ArrayList<Integer>();
				if (validGroups.size() < bg.getTeamCount()) {
					continue;
				}
				for (int i = 0; i < bg.getTeamCount(); i++) {
					groups.add(new ArrayList<Player>());
					for (List<Player> group : validGroups) {
						if (!usedGroups.contains(group) && group.size() <= bg.getMaxSize()) {
							groups.set(i, cloneGroup(group));
							usedGroups.add(group);
							break;
						}
					}
				}
				if (groups.get(0).size() < bg.getMinSize()) {
					for (List<Player> group : validGroups) {
						if (!usedGroups.contains(group) && group.size() + groups.get(0).size() <= bg.getMaxSize()) {
							groups.get(0).addAll(group);
							usedGroups.add(group);
							if (groups.get(0).size() >= bg.getMinSize()) {
								break;
							}
						}
					}
				}
				if (groups.get(0).size() < bg.getMinSize() || groups.get(0).size() > bg.getMaxSize()) {
					continue;
				}
				boolean satisfied = true;
				for (int i = 1; i < bg.getTeamCount(); i++) {
					if (groups.get(i).size() < groups.get(0).size()) {
						for (List<Player> group : validGroups) {
							if (!usedGroups.contains(group)
									&& group.size() + groups.get(i).size() <= groups.get(0).size()) {
								groups.get(i).addAll(group);
								usedGroups.add(group);
							}
						}
					}
					if (groups.get(i).size() < groups.get(0).size()) {
						satisfied = false;
					}
				}
				if (!satisfied) {
					continue;
				}
				for (List<Player> group : groups) {
					for (Player pl : group) {
						participants.add(pl.getObjectId());
					}
				}
				validGroups.removeAll(usedGroups);
				bg.createMatch(participants);
			} else {
				bg.createMatch(validParticipants);
			}
			if (bg.hasPlayers()) {
				registerBg(bg);
			}
		}
		if (eventTeamBased) {
			for (List<Player> group : validGroups) {
				for (Player pl : group) {
					scheduleAnnouncement(pl, "There are no more place!!!, You will more luck next time!", 0);
				}
			}
		} else {
			for (Integer objectId : validParticipants) {
				AionObject ao = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(objectId);
				if (ao != null && ao instanceof Player) {
					scheduleAnnouncement((Player) ao, "\uE05C",
							"There are no more place!!!, You will more luck next time!", 0);
				}
			}
		}
		eventReady = false;
		eventBg = null;
		eventTeamBased = false;
	}

	/**
	 * 返回普通战场是否处于报名中。
	 * Returns whether normal battleground registration is open.
	 *
	 * @return 是否报名中 / whether ready
	 */
	public boolean isNormalReady() {
		return normalReady;
	}

	/**
	 * 返回活动战场是否处于报名中。
	 * Returns whether event battleground registration is open.
	 *
	 * @return 是否报名中 / whether ready
	 */
	public boolean isEventReady() {
		return eventReady;
	}

	/**
	 * 取消进行中的活动报名并通知全服。
	 * Cancels an in-progress event registration and notifies the world.
	 */
	public void cancelEvent() {
		if (eventTask != null) {
			eventTask.cancel(false);
			eventBg = null;
			eventQueueList.clear();
			eventTask = null;
			eventReady = false;
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player pl) {
					PacketSendUtility.sendPacket(pl, new SM_AUTO_GROUP(300350000, false));
				}
			});
			announceAll("The event was canceled!!!");
		}
	}

	/**
	 * 向未在战场 / FFA 中的玩家广播系统消息。
	 * Broadcasts a system message to players not currently in a BG/FFA.
	 *
	 * message
	 */
	private void announceAll(final String msg) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.getBattleground() == null && !GameFeatureServices.ffaService().isInArena(player)) {
					PacketSendUtility.sendSys3Message(player, "\uE05C", msg);
				}
			}
		});
	}

	/**
	 * 分配下一个可用战场 ID。
	 * Allocates the next available battleground id.
	 *
	 * @return 战场 ID，失败返回 -1 / bg id, or -1 on failure
	 */
	private Integer getNextAvailableBgId() {
		for (Integer i = 1; i < 10000; i++) {
			if (!bgMap.containsKey(i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 战场结束时清理映射并回调事件引擎。
	 * Cleans mappings when a battleground ends and notifies the event engine.
	 *
	 * @param bg 结束的战场 / finished battleground
	 */
	public void onBgEnd(Battleground bg) {
		bgMap.remove(bg.getBgId());
		if (normalBgMap.containsKey(bg.getBgId())) {
			Event event = normalBgMap.remove(bg.getBgId());
			if (event instanceof BattlegroundEvent) {
				((BattlegroundEvent) event).onEnd(bg.getBgId());
			}
		}
	}

	/**
	 * 注册并启动一个战场实例。
	 * Registers and starts a battleground instance.
	 *
	 * @param bg 战场 / battleground
	 * @return 分配的战场 ID / allocated bg id
	 */
	public Integer registerBg(Battleground bg) {
		Integer bgId = getNextAvailableBgId();
		bg.setBgId(bgId);
		bgMap.put(bgId, bg);
		return bgId;
	}

	/**
	 * 返回活跃战场的不可变快照。
	 * Returns an immutable snapshot of active battlegrounds.
	 *
	 * @return 战场映射快照 / battleground map snapshot
	 */
	public Map<Integer, Battleground> getBattlegrounds() {
		synchronized (bgMap) {
			return Collections.unmodifiableMap(new LinkedHashMap<Integer, Battleground>(bgMap));
		}
	}

	/**
	 * 复制当前活跃战场列表快照。
	 * Copies a snapshot of the active battleground list.
	 *
	 * battleground list
	 */
	private List<Battleground> battlegroundsSnapshot() {
		synchronized (bgMap) {
			return new ArrayList<Battleground>(bgMap.values());
		}
	}

	/**
	 * 复制队列快照以便安全遍历。
	 * Copies a queue snapshot for safe iteration.
	 *
	 * source queue
	 * snapshot list
	 */
	private List<AionObject> queueSnapshot(List<AionObject> queue) {
		synchronized (queue) {
			return new ArrayList<AionObject>(queue);
		}
	}

	/**
	 * 查找玩家可重连的未结束战场。
	 * Finds an unfinished battleground the player can rejoin.
	 *
	 * @param player 玩家 / player
	 * @return 可重连战场，或 null / rejoinable battleground, or null
	 */
	public Battleground getActiveBattleground(Player player) {
		for (Battleground bg : battlegroundsSnapshot()) {
			if (bg.getSecondsLeft() > 1) {
				if (bg.getLeavers().containsKey(player.getObjectId())) {
					return bg;
				}
			}
		}
		return null;
	}

	/**
	 * 按战场索引与种族返回伪装斗篷模板 ID。
	 * Returns the disguise cloak template id by BG index and race.
	 *
	 * 玩家 / player
	 * @param bgIndex 战场队伍索引 / battleground team index
	 * cloak template id
	 */
	public int getBgCloak(Player player, int bgIndex) {
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
	public String getName(Player player, Player target) {
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
	public String getNameByIndex(int bgIndex) {
		String name;
		switch (bgIndex + 1) {
		case 1:
			name = "Daeva Of Chaos";
			break;
		case 2:
			name = "Until Death";
			break;
		case 3:
			name = "Happy Tiger's";
			break;
		case 4:
			name = "Abyssal Inquin's";
			break;
		case 5:
			name = "Puffy Bear's";
			break;
		case 6:
			name = "Mossy Treant's";
			break;
		case 7:
			name = "Cursed Pirate's";
			break;
		case 8:
			name = "Naughty Kerub's";
			break;
		case 9:
			name = "Tundra Tiger's";
			break;
		case 10:
			name = "Tiger Lover's";
			break;
		case 11:
			name = "Screamer's";
			break;
		case 12:
			name = "Ninja Balaur's";
			break;
		case 13:
			name = "Summer Inquin's";
			break;
		case 14:
			name = "Volcano Inquin's";
			break;
		case 15:
			name = "Savana Inquin's";
			break;
		case 16:
			name = "Tropical Inquin's";
			break;
		case 17:
			name = "Walking Dead";
			break;
		default:
			name = "Invalid";
			break;
		}
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
	public LegionEmblem getCapeEmblemByIndex(int bgIndex) {
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

	/**
	 * 处理自动组队 / 战场报名窗口交互。
	 * battleground registration window interactions.
	 *
	 * 玩家 / player
	 * window id
	 * dialog id
	 */
	public void handleWindow(Player player, int windowId, int dialogId) {
		if (isEventReady()) {
			switch (windowId) {
			case 100:
				switch (dialogId) {
				case 0:
				case 1:
					if (registerForEvent(player)) {
						PacketSendUtility.sendSys3Message(player, "\uE05C", "You are now registered for event!!!");
					} else {
						PacketSendUtility.sendSys3Message(player, "\uE05C",
								"Failed to register!!! If you are in a group, you have already registered.");
					}
					break;
				case 2:
					if (eventTeamBased && player.isInGroup2()) {
						if (player.getPlayerGroup2().getLeaderObject() == player) {
							if (registerForEvent(player.getPlayerGroup2())) {
								for (Player pl : player.getPlayerGroup2().getMembers()) {
									PacketSendUtility.sendSys3Message(pl, "\uE05C",
											"Your group is now registered for event.");
								}
							} else {
								PacketSendUtility.sendSys3Message(player, "\uE05C", "Failed to register your group!!!");
							}
						} else {
							PacketSendUtility.sendSys3Message(player, "\uE05C",
									"You must be the leader of your group for registration!!!");
						}
					}
					break;
				}
				break;
			case 101:
				if (isInQueue(player)) {
					unregisterFromQueue(player);
					PacketSendUtility.sendSys3Message(player, "\uE05C",
							"You must cancel your registration for the event!!!");
				} else if (player.isInGroup2() && isInQueue(player.getPlayerGroup2())) {
					unregisterFromQueue(player.getPlayerGroup2());
					for (Player pl : player.getPlayerGroup2().getMembers()) {
						PacketSendUtility.sendSys3Message(pl, "\uE05C",
								"Your group to withdraw its registration for the event!!!");
					}
				}
				break;
			case 104:
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(300350000, eventTeamBased, 903479));
				break;
			}
		} else if (isNormalReady()) {
			switch (windowId) {
			case 100:
				switch (dialogId) {
				case 0:
				case 1:
					if (registerForNormal(player)) {
						PacketSendUtility.sendSys3Message(player, "\uE05C",
								"You are now registered for the next battlefield!!!");
					} else {
						PacketSendUtility.sendSys3Message(player, "\uE05C",
								"Failed to register!!! If you are in a group, you have already registered.");
					}
					break;
				case 2:
					if (normalTeamBased && player.isInGroup2()) {
						if (player.getPlayerGroup2().getLeaderObject() == player) {
							if (registerForNormal(player.getPlayerGroup2())) {
								for (Player pl : player.getPlayerGroup2().getMembers()) {
									PacketSendUtility.sendSys3Message(pl, "\uE05C",
											"Your group is now registered for battlefield.");
								}
							} else {
								PacketSendUtility.sendSys3Message(player, "\uE05C", "Failed to register your group!!!");
							}
						} else {
							PacketSendUtility.sendSys3Message(player, "\uE05C",
									"You must be the leader of your group for registration!!!");
						}
					}
					break;
				}
				break;
			case 101:
				if (isInQueue(player)) {
					unregisterFromQueue(player);
					PacketSendUtility.sendSys3Message(player, "\uE05C",
							"You must cancel your registration for the battlefield!");
				} else if (player.isInGroup2() && isInQueue(player.getPlayerGroup2())) {
					unregisterFromQueue(player.getPlayerGroup2());
					for (Player pl : player.getPlayerGroup2().getMembers()) {
						PacketSendUtility.sendSys3Message(pl, "\uE05C",
								"Your group to withdraw its registration for the battlefield!!!");
					}
				}
				break;
			case 104:
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(301550000, normalTeamBased, 903479));
				break;
			}
		}
	}

	/**
	 * 玩家登录时同步普通战场报名 UI 状态。
	 * Syncs normal battleground registration UI state on player login.
	 *
	 * @param player 玩家 / player
	 */
	public void onPlayerLogin(Player player) {
		if (isNormalReady() && player.getBattleground() != null && !isInQueue(player)) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(301550000, true));
		}
	}

	/**
	 * 按起始职业交叉排序参与者，尽量均衡职业分布。
	 * Interleaves participants by starting class to balance class distribution.
	 *
	 * @param participants 参与者 objectId 列表 / participant objectId list
	 */
	private void SortParticipantList(List<Integer> participants) {
		List<Integer> warrior = new ArrayList<Integer>();
		List<Integer> scout = new ArrayList<Integer>();
		List<Integer> mage = new ArrayList<Integer>();
		List<Integer> cleric = new ArrayList<Integer>();
		List<Integer> technist = new ArrayList<Integer>();
		List<Integer> muse = new ArrayList<Integer>();
		for (Integer objectId : participants) {
			Player pl = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objectId);
			if (pl == null) {
				continue;
			}
			switch (PlayerClass.getStartingClassFor(pl.getPlayerClass())) {
			case WARRIOR:
				warrior.add(objectId);
				break;
			case SCOUT:
				scout.add(objectId);
				break;
			case MAGE:
				mage.add(objectId);
				break;
			case PRIEST:
				cleric.add(objectId);
				break;
			case TECHNIST:
				technist.add(objectId);
				break;
			case MUSE:
				muse.add(objectId);
				break;
			}
		}
		participants = new ArrayList<Integer>();
		while (!warrior.isEmpty() && !scout.isEmpty() && !mage.isEmpty() && !cleric.isEmpty() && !technist.isEmpty()
				&& !muse.isEmpty()) {
			int total = warrior.size() + scout.size() + mage.size() + cleric.size() + technist.size() + muse.size();
			float ratioW = (warrior.size() * 100) / total;
			float ratioS = (scout.size() * 100) / total;
			float ratioM = (mage.size() * 100) / total;
			float ratioP = (cleric.size() * 100) / total;
			float ratioT = (technist.size() * 100) / total;
			float ratioMu = (muse.size() * 100) / total;
			if (!warrior.isEmpty()) {
				participants.add(warrior.remove(0));
			}
			if (!scout.isEmpty()) {
				participants.add(scout.remove(0));
			}
			if (!mage.isEmpty()) {
				participants.add(mage.remove(0));
			}
			if (!cleric.isEmpty()) {
				participants.add(cleric.remove(0));
			}
			if (!technist.isEmpty()) {
				participants.add(technist.remove(0));
			}
			if (!muse.isEmpty()) {
				participants.add(muse.remove(0));
			}
			if (!warrior.isEmpty() && ratioW > 30) {
				participants.add(warrior.remove(0));
			}
			if (!scout.isEmpty() && ratioS > 30) {
				participants.add(scout.remove(0));
			}
			if (!mage.isEmpty() && ratioM > 30) {
				participants.add(mage.remove(0));
			}
			if (!cleric.isEmpty() && ratioP > 30) {
				participants.add(cleric.remove(0));
			}
			if (!technist.isEmpty() && ratioT > 30) {
				participants.add(technist.remove(0));
			}
			if (!muse.isEmpty() && ratioMu > 30) {
				participants.add(muse.remove(0));
			}
		}
	}

	/**
	 * 浅拷贝玩家列表。
	 * Shallow-copies a player list.
	 *
	 * source list
	 * clone
	 */
	private List<Player> cloneGroup(List<Player> group) {
		List<Player> clone = new ArrayList<Player>();
		clone.addAll(group);
		return clone;
	}

	/**
	 * 向玩家发送（可延迟）系统公告，使用默认发送者图标。
	 * Sends a (optionally delayed) system announcement to a player with the default sender icon.
	 *
	 * 玩家 / player
	 * message
	 * @param delay 延迟毫秒 / delay in ms
	 */
	private void scheduleAnnouncement(final Player player, final String msg, int delay) {
		this.scheduleAnnouncement(player, "\uE05C", msg, delay);
	}

	/**
	 * 向玩家发送（可延迟）系统公告。
	 * Sends a (optionally delayed) system announcement to a player.
	 *
	 * 玩家 / player
	 * @param sender 发送者图标 / sender icon
	 * message
	 * @param delay 延迟毫秒 / delay in ms
	 */
	private void scheduleAnnouncement(final Player player, final String sender, final String msg, int delay) {
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
	 * 获取天梯 DAO。
	 * Returns the ladder DAO.
	 *
	 * DAO instance
	 */
	private LadderDAO getLadderDAO() {
		return DAOManager.getDAO(LadderDAO.class);
	}

	/**
	 * 懒加载单例持有者。
	 * Lazy singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final LadderService instance = new LadderService();
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers the Spring provider).
	 *
	 * service instance
	 */
	public static final LadderService getInstance() {
		ObjectProvider<LadderService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<LadderService> instanceProvider) {
		LadderService.instanceProvider = instanceProvider;
	}
}
