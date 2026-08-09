package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.EventType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.event.EventTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestStartEligibilityPort;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;

/**
 * 活动服务，管理限时活动启停及活动任务的发放与维护。
 * Event service managing timed event start/stop and event quest start/maintain.
 *
 * @author Rolandas
 */
@Slf4j
public class EventService {

	private static volatile ObjectProvider<EventService> instanceProvider;

	/** 活动状态检查周期（毫秒）。 / Event status check period in ms. */
	private final int CHECK_TIME_PERIOD = 1000 * 60 * 5;

	/** 服务是否已启动。 / Whether the service is started. */
	private boolean isStarted = false;

	/** 周期检查任务。 / Periodic check task. */
	private Future<?> checkTask = null;

	/** 当前活跃活动列表。 / Currently active events. */
	private List<EventTemplate> activeEvents;

	/** 可开启任务 ID → 活动模板。 / Startable quest id → event templates. */
	Map<Integer, List<EventTemplate>> eventsForStartQuest = new HashMap<Integer, List<EventTemplate>>();

	/** 可维护任务 ID → 活动模板。 / Maintainable quest id → event templates. */
	Map<Integer, List<EventTemplate>> eventsForMaintainQuest = new HashMap<Integer, List<EventTemplate>>();

	private static class SingletonHolder {

		protected static final EventService instance = new EventService();
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final EventService getInstance() {
		ObjectProvider<EventService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<EventService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 构造服务并加载当前活跃活动。
	 * Constructs the service and loads currently active events.
	 */
	public EventService() {
		activeEvents = Collections.synchronizedList(DataManager.EVENT_DATA.getActiveEvents());
		updateQuestMap();
	}

	/**
	 * 玩家登录后发放/维护活跃活动任务。
	 * Starts or maintains active event quests after player login.
	 * 仅应在登录流程中调用。
	 * Must not be called from anywhere else.
	 *
	 * @param player 玩家 / player
	 */
	public void onPlayerLogin(Player player) {
		List<Integer> activeStartQuests = new ArrayList<Integer>();
		List<Integer> activeMaintainQuests = new ArrayList<Integer>();
		Map<Integer, List<EventTemplate>> map1 = null;
		Map<Integer, List<EventTemplate>> map2 = null;

		synchronized (activeEvents) {
			for (EventTemplate et : activeEvents) {
				if (et.isActive()) {
					activeStartQuests.addAll(et.getStartableQuests());
					activeMaintainQuests.addAll(et.getMaintainableQuests());
				}
			}
			map1 = new HashMap<Integer, List<EventTemplate>>(eventsForStartQuest);
			map2 = new HashMap<Integer, List<EventTemplate>>(eventsForMaintainQuest);
		}

		StartOrMaintainQuests(player, activeStartQuests.listIterator(), map1, true);
		StartOrMaintainQuests(player, activeMaintainQuests.listIterator(), map2, false);

		activeStartQuests.clear();
		activeMaintainQuests.clear();
		map1.clear();
		map2.clear();
	}

	/**
	 * 按条件为玩家开启或重置/维护活动任务。
	 * Starts or resets/maintains event quests for the player by conditions.
	 *
	 * 玩家 / player
	 * @param questList 任务 ID 迭代器 / quest id iterator
	 * @param templateMap 任务 → 活动模板映射 / quest → event template map
	 * @param start true 表示可新开任务 / true to start new quests
	 */
	void StartOrMaintainQuests(Player player, ListIterator<Integer> questList, Map<Integer, List<EventTemplate>> templateMap, boolean start) {
		var catalog = GameEngineServices.questEngine().questCatalog();
		PlayerQuestStartEligibilityPort eligibility = new PlayerQuestStartEligibilityPort(playerId -> player,
			id -> catalog.findMetadata(id).orElse(null));
		while (questList.hasNext()) {
			int questId = questList.next();
			QuestState qs = player.getQuestStateList().getQuestState(questId);
			QuestEnv cookie = new QuestEnv(null, player, questId, 0);
			QuestStatus status = qs == null ? QuestStatus.START : qs.getStatus();

			QuestMetadata metadata = catalog.findMetadata(questId).orElse(null);
			if (matchesEventQuestMetadata(player, metadata, eligibility)) {
				if (qs != null) {
					if (qs.getCompleteTime() != null || status == QuestStatus.COMPLETE) {
						ZonedDateTime completed = null;
						if (qs.getCompleteTime() == null) {
							completed = ZonedDateTime.now().withYear(1970).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0);
						} else {
							completed = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(qs.getCompleteTime().getTime()), java.time.ZoneId.systemDefault());
						}
						if (templateMap.containsKey(questId)) {
							for (EventTemplate et : templateMap.get(questId)) {
								// 循环事件，重置它 / recurring event, reset it
								if (et.getStartDate().isAfter(completed)) {
									if (start) {
										status = QuestStatus.START;
										qs.setQuestVar(0);
										qs.setCompleteCount(0);
										qs.setStatus(status);
									}
									break;
								}
							}
						}
					}
					// 重新登记任务 / re-register quests
					if (status == QuestStatus.COMPLETE) {
						PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, status, qs.getQuestVars().getQuestVars()));
					} else {
						QuestService.startEventQuest(cookie, status);
					}
				} else if (start) {
					QuestService.startEventQuest(cookie, status);
				}
			}
		}
	}

	static boolean matchesEventQuestMetadata(Player player, QuestMetadata metadata,
			PlayerQuestStartEligibilityPort eligibility) {
		if (metadata == null || player.getLevel() < metadata.minLevel()
				|| player.getLevel() > metadata.maxLevel()) {
			return false;
		}
		if (!metadata.permitsRace(player.getRace() == null ? null : player.getRace().name())) {
			return false;
		}
		if (!metadata.permittedClasses().isEmpty()
				&& (player.getCommonData() == null || player.getCommonData().getPlayerClass() == null
					|| !metadata.permittedClasses().contains(player.getCommonData().getPlayerClass().name()))) {
			return false;
		}
		if (!metadata.permittedGender().isEmpty()
				&& (player.getGender() == null || !metadata.permittedGender().equals(player.getGender().name()))) {
			return false;
		}
		return eligibility.matchesCanonicalStartConditions(player, metadata);
	}

	/**
	 * 判断活动检查服务是否已启动。
	 * Returns whether the event check service is started.
	 *
	 * @return 已启动返回 true / true if started
	 */
	public boolean isStarted() {
		return isStarted;
	}

	/**
	 * 启动周期性活动状态检查。
	 * Starts the periodic event status check.
	 */
	public void start() {
		if (isStarted) {
			checkTask.cancel(false);
		}
		isStarted = true;

		checkTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				checkEvents();
			}
		}, 0, CHECK_TIME_PERIOD);
	}

	/**
	 * 停止周期性活动状态检查。
	 * Stops the periodic event status check.
	 */
	public void stop() {
		if (isStarted) {
			checkTask.cancel(false);
		}
		checkTask = null;
		isStarted = false;
	}

	/**
	 * 刷新活跃活动列表，启停过期/新增活动。
	 * Refreshes the active event list, starting new and stopping expired events.
	 */
	private void checkEvents() {
		List<EventTemplate> newEvents = new ArrayList<EventTemplate>();
		List<EventTemplate> allEvents = DataManager.EVENT_DATA.getAllEvents();

		for (EventTemplate et : allEvents) {
			if (et.isActive()) {
				newEvents.add(et);
				et.Start();
			}
		}

		synchronized (activeEvents) {
			for (EventTemplate et : activeEvents) {
				if (et.isExpired() || !DataManager.EVENT_DATA.Contains(et.getName())) {
					et.Stop();
				}
			}
			activeEvents.clear();
			eventsForStartQuest.clear();
			eventsForMaintainQuest.clear();
			activeEvents.addAll(newEvents);
			updateQuestMap();
		}

		newEvents.clear();
		allEvents.clear();
	}

	/**
	 * 根据活跃活动重建任务映射。
	 * Rebuilds quest maps from active events.
	 */
	private void updateQuestMap() {
		for (EventTemplate et : activeEvents) {
			for (int qId : et.getStartableQuests()) {
				if (!eventsForStartQuest.containsKey(qId)) {
					eventsForStartQuest.put(qId, new ArrayList<EventTemplate>());
				}
				eventsForStartQuest.get(qId).add(et);
			}
			for (int qId : et.getMaintainableQuests()) {
				if (!eventsForMaintainQuest.containsKey(qId)) {
					eventsForMaintainQuest.put(qId, new ArrayList<EventTemplate>());
				}
				eventsForMaintainQuest.get(qId).add(et);
			}
		}
	}

	/**
	 * 判断任务是否属于当前活跃活动。
	 * Returns whether the quest is part of a currently active event.
	 *
	 * quest id
	 *
	 * @param questId 若 active 则为 true / true if active
	 */
	public boolean checkQuestIsActive(int questId) {
		synchronized (activeEvents) {
			if (eventsForStartQuest.containsKey(questId) || eventsForMaintainQuest.containsKey(questId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取当前活动主题类型；无活动时返回 NONE。
	 * Returns the current event theme type, or NONE when no event is active.
	 *
	 * event type
	 */
	public EventType getEventType() {
		if (EventsConfig.ENABLE_EVENT_SERVICE) {
			for (EventTemplate et : activeEventsSnapshot()) {
				String theme = et.getTheme();
				if (theme != null) {
					EventType type = EventType.getEventType(theme);
					if (et.isActive() && !type.equals(EventType.NONE)) {
						return type;
					}
				}
			}
		}
		return EventType.NONE;
	}

	/**
	 * 获取当前活跃活动快照列表。
	 * Returns a snapshot list of currently active events.
	 *
	 * event list
	 */
	public List<EventTemplate> getActiveEvents() {
		return activeEventsSnapshot();
	}

	/**
	 * 线程安全地复制活跃活动列表。
	 * Thread-safely copies the active event list.
	 *
	 * event snapshot
	 */
	private List<EventTemplate> activeEventsSnapshot() {
		synchronized (activeEvents) {
			return new ArrayList<EventTemplate>(activeEvents);
		}
	}
}
