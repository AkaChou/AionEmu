package com.aionemu.gameserver.questEngine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.scripting.CompiledScriptLoader;
import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.commons.utils.collections.IntProcedure;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.XMLQuests;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.HandlerSideDrop;
import com.aionemu.gameserver.model.templates.quest.QuestCategory;
import com.aionemu.gameserver.model.templates.quest.QuestDrop;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.quest.QuestNpc;
import com.aionemu.gameserver.model.templates.rewards.BonusType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.QuestHandlerLoader;
import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestDispatchContract;
import com.aionemu.gameserver.questEngine.runtime.QuestLegacyInvocationBridge;
import com.aionemu.gameserver.questEngine.runtime.QuestLegacyObservationRecorder;
import com.aionemu.gameserver.questEngine.runtime.QuestLegacyObservationSink;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRouteResult;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeComposition;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeResources;
import com.aionemu.gameserver.questEngine.runtime.QuestShadowCapture;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneName;

import lombok.extern.slf4j.Slf4j;

/**
 * 任务引擎单例：加载脚本处理器、维护事件注册表，并向已注册处理器分发各类游戏事件。
 * Central quest-engine singleton that loads script handlers, maintains event
 * registries, and dispatches game events to registered {@link QuestHandler}s.
 *
 * @author MrPoke, Hilgert
 * @modified vlog
 */
@Slf4j
public class QuestEngine implements GameEngine {
	private static final String PRODUCTION_DEFINITION_CATALOG =
		"aion/data/static_data/quest_definition/quest_definition_catalog.xml";

	/** Spring ObjectProvider 覆盖钩子 / Spring ObjectProvider override hook */
	private static volatile ObjectProvider<QuestEngine> instanceProvider;
	/** 任务 ID → 处理器映射 / questId → handler map */
	private static final Map<Integer, QuestHandler> questHandlers = new LinkedHashMap<Integer, QuestHandler>();
	/** NPC 关联任务索引 / NPC-related quest index */
	private IntObjectHashMap<QuestNpc> questNpcs = new IntObjectHashMap<QuestNpc>();
	/** 物品使用关联任务 / Item-use related quests */
	private IntObjectHashMap<IntArrayList> questItemRelated = new IntObjectHashMap<IntArrayList>();
	/** 房屋物品关联任务 / House-item related quests */
	private IntObjectHashMap<IntArrayList> questHouseItems = new IntObjectHashMap<IntArrayList>();
	/** 获得物品关联任务 / Item-obtain related quests */
	private IntObjectHashMap<IntArrayList> questItems = new IntObjectHashMap<IntArrayList>();
	/** 区域任务结束监听列表 / Zone-mission-end listeners */
	private IntArrayList questOnEnterZoneMissionEnd = new IntArrayList();
	/** 升级监听列表 / Level-up listeners */
	private IntArrayList questOnLevelUp = new IntArrayList();
	/** 死亡监听列表 / Death listeners */
	private IntArrayList questOnDie = new IntArrayList();
	/** 登出监听列表 / Logout listeners */
	private IntArrayList questOnLogOut = new IntArrayList();
	/** 进入世界监听列表 / Enter-world listeners */
	private IntArrayList questOnEnterWorld = new IntArrayList();
	/** 进入区域监听 / Enter-zone listeners */
	private Map<ZoneName, IntArrayList> questOnEnterZone = new LinkedHashMap<ZoneName, IntArrayList>();
	/** 离开区域监听 / Leave-zone listeners */
	private Map<ZoneName, IntArrayList> questOnLeaveZone = new LinkedHashMap<ZoneName, IntArrayList>();
	/** 穿过飞行环监听 / Pass-flying-ring listeners */
	private Map<String, IntArrayList> questOnPassFlyingRings = new LinkedHashMap<String, IntArrayList>();
	/** 动画结束监听 / Movie-end listeners */
	private IntObjectHashMap<IntArrayList> questOnMovieEnd = new IntObjectHashMap<IntArrayList>();
	/** 计时器结束监听 / Timer-end listeners */
	private List<Integer> questOnTimerEnd = new ArrayList<Integer>();
	/** 隐形计时器结束监听 / Invisible-timer-end listeners */
	private List<Integer> onInvisibleTimerEnd = new ArrayList<Integer>();
	/** 击杀军衔玩家监听 / Kill-ranked listeners */
	private Map<AbyssRankEnum, IntArrayList> questOnKillRanked = new LinkedHashMap<AbyssRankEnum, IntArrayList>();
	/** 世界内击杀监听 / Kill-in-world listeners */
	private Map<Integer, IntArrayList> questOnKillInWorld = new LinkedHashMap<Integer, IntArrayList>();
	/** 使用技能监听 / Skill-use listeners */
	private IntObjectHashMap<IntArrayList> questOnUseSkill = new IntObjectHashMap<IntArrayList>();
	/** 对话框 ID → 枚举映射 / dialogId → enum map */
	private Map<Integer, QuestDialog> dialogMap = new LinkedHashMap<>();
	/** 制作失败监听 / Fail-craft listeners */
	private Map<Integer, Integer> questOnFailCraft = new HashMap<Integer, Integer>();
	/** 装备物品监听 / Equip-item listeners */
	private Map<Integer, Set<Integer>> questOnEquipItem = new HashMap<Integer, Set<Integer>>();
	/** 每日/周任务提醒定时任务 / Daily/weekly reminder scheduled task */
	private ScheduledFuture<?> messageSendingTask;
	/**
	 * Atomic production assembly: the shadow capture scope provider and the
	 * legacy observation bridge are swapped together, never one without the
	 * other. A null capture detaches back to the no-op bridge with zero
	 * capture overhead; a failing scope, sink or report never changes the
	 * legacy route or result.
	 */
	private volatile ShadowAssembly shadowAssembly = ShadowAssembly.detached();
	/** Fully composed production ports used by typed quest execution. */
	private final QuestRuntimeComposition runtimeComposition = QuestRuntimeComposition.production();
	/** Live typed owner catalog and central Router/Coordinator execution chain. */
	private volatile QuestProductionDispatcher productionDispatcher = QuestProductionDispatcher.disabled();
	/** 可行动作监听 / Can-act listeners */
	private IntObjectHashMap<IntArrayList> questCanAct = new IntObjectHashMap<IntArrayList>();
	/** 挖掘号奖励监听 / Dredgion reward listeners */
	private List<Integer> questOnDredgionReward = new ArrayList<Integer>();
	/** 卡玛尔奖励监听 / Kamar reward listeners */
	private List<Integer> questOnKamarReward = new ArrayList<Integer>();
	/** 欧菲丹奖励监听 / Ophidan reward listeners */
	private List<Integer> questOnOphidanReward = new ArrayList<Integer>();
	/** 堡垒奖励监听 / Bastion reward listeners */
	private List<Integer> questOnBastionReward = new ArrayList<Integer>();
	/** 奖励加成监听 / Bonus-apply listeners */
	private Map<BonusType, IntArrayList> questOnBonusApply = new LinkedHashMap<BonusType, IntArrayList>();
	/** 跟随到达目标监听 / Reach-target listeners */
	private IntArrayList reachTarget = new IntArrayList();
	/** 跟随丢失目标监听 / Lost-target listeners */
	private IntArrayList lostTarget = new IntArrayList();
	/** 进入风道监听 / Enter-windstream listeners */
	private IntArrayList questOnEnterWindStream = new IntArrayList();
	/** 骑乘动作监听 / Ride-action listeners */
	private IntArrayList questRideAction = new IntArrayList();
	/** 创造力点数监听 / Creativity-point listeners */
	private IntArrayList questOnCreativityPoint = new IntArrayList();

	/**
	 * 创建任务引擎实例。
	 * Create a quest-engine instance.
	 */
	public QuestEngine() {
	}

	QuestRuntimeComposition runtimeComposition() {
		return runtimeComposition;
	}

	/**
	 * Installs (or detaches) the production shadow capture assembly atomically.
	 * Capture is both the scope provider (physical events open a capture scope
	 * that freezes pre-event snapshots) and the bridge sink (legacy owner
	 * observations produced inside the scope are recorded into it). Passing
	 * {@code null} revokes the whole assembly: the engine returns to the no-op
	 * bridge with zero capture overhead. Capture is diagnostic only and never
	 * changes the old route or result.
	 */
	public void setShadowCapture(QuestShadowCapture capture) {
		shadowAssembly = capture == null
			? ShadowAssembly.detached()
			: new ShadowAssembly(capture, new QuestLegacyInvocationBridge(capture));
	}

	/**
	 * Installs a passive observation sink without a capture scope provider.
	 * The sink receives immutable observations after legacy execution and is
	 * never consulted for routing or fallback decisions.
	 */
	public void setLegacyObservationSink(QuestLegacyObservationSink sink) {
		shadowAssembly = new ShadowAssembly(shadowAssembly.capture, new QuestLegacyInvocationBridge(sink));
	}

	/**
	 * Opens a capture scope for one physical event when capture is installed.
	 * A failing event construction or capture setup degrades to no capture and
	 * never changes the legacy route.
	 */
	private QuestShadowCapture.Scope shadowScope(Player player, Supplier<QuestEvent> eventFactory,
			Collection<Integer> questIds) {
		QuestShadowCapture capture = shadowAssembly.capture;
		if (capture == null) {
			return null;
		}
		try {
			return capture.open(player, eventFactory.get(), questIds);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private <T> T invokeObserved(QuestEnv env, int questId, String eventType,
		QuestDispatchContract contract, QuestLegacyInvocationBridge.Invocation<T> invocation,
		QuestLegacyInvocationBridge.ResultClassifier<T> classifier) {
		return shadowAssembly.bridge.invoke(env.getPlayer(), questId, eventType, contract, invocation, classifier);
	}

	/** Immutable capture+bridge pair swapped atomically via one volatile reference. */
	private static final class ShadowAssembly {
		private final QuestShadowCapture capture;
		private final QuestLegacyInvocationBridge bridge;

		private ShadowAssembly(QuestShadowCapture capture, QuestLegacyInvocationBridge bridge) {
			this.capture = capture;
			this.bridge = bridge;
		}

		private static ShadowAssembly detached() {
			return new ShadowAssembly(null, new QuestLegacyInvocationBridge());
		}
	}

	private static QuestRouteResult booleanResult(Boolean result, boolean stateChanged,
		QuestLegacyObservationRecorder recorder) {
		return Boolean.TRUE.equals(result) ? QuestRouteResult.HANDLED : QuestRouteResult.NOT_HANDLED;
	}

	private static QuestRouteResult voidResult(int questId, Void ignored, boolean stateChanged,
		QuestLegacyObservationRecorder recorder) {
		return stateChanged || recorder.hasEffects(questId) ? QuestRouteResult.HANDLED : QuestRouteResult.UNKNOWN;
	}

	private static QuestRouteResult handlerResult(HandlerResult result, boolean stateChanged,
		QuestLegacyObservationRecorder recorder) {
		if (result == null) {
			return QuestRouteResult.UNKNOWN;
		}
		return switch (result) {
			case SUCCESS -> QuestRouteResult.HANDLED;
			case FAILED -> QuestRouteResult.FAILED;
			case UNKNOWN -> QuestRouteResult.UNKNOWN;
		};
	}

	/**
	 * 返回任务引擎单例（优先走 Spring ObjectProvider）。
	 * Return the quest-engine singleton (prefer Spring ObjectProvider when set).
	 *
	 * Engine instance
	 */
	public static final QuestEngine getInstance() {
		ObjectProvider<QuestEngine> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring ObjectProvider 覆盖点。
	 * Install a Spring ObjectProvider override.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<QuestEngine> provider) {
		instanceProvider = provider;
	}

	/**
	 * 分发 NPC 对话事件；questId 为 0 时按 NPC 上注册的谈话任务依次尝试。
	 * Dispatch an NPC dialog event; when questId is 0, try talk-quests registered on the NPC.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否有处理器接管 / Whether a handler took over
	 */
	public boolean onDialog(QuestEnv env) {
		Player player = env.getPlayer();
		try {
			Npc npc = env.getVisibleObject() instanceof Npc target ? target : null;
			int requestedOwner = env.getQuestId();
			int npcId = npc == null ? 0 : npc.getNpcId();
			QuestProductionDispatcher typed = productionDispatcher;
			if (requestedOwner != 0 && typed.owns(requestedOwner)) {
				if (npcId == 0) {
					return false;
				}
				QuestEvent event = new QuestEvent.TalkToNpc(npcId, env.getDialogId(), npc.getObjectId());
				return typed.dispatch(event, player.getObjectId(), requestedOwner,
					QuestDispatchContract.EXCLUSIVE).claimed();
			}

			if (requestedOwner == 0 && npcId != 0) {
				QuestEvent event = new QuestEvent.TalkToNpc(npcId, env.getDialogId(), npc.getObjectId());
				if (typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.EXCLUSIVE).claimed()) {
					return true;
				}
			}

			List<Integer> legacyOwners = requestedOwner != 0
				? List.of(requestedOwner)
				: (npc == null ? List.of() : getQuestNpc(npcId).getOnTalkEvent());
			try (QuestShadowCapture.Scope scope = shadowScope(player,
					() -> new QuestEvent.TalkToNpc(env.getTargetId(), env.getDialogId(),
						npc == null ? 0 : npc.getObjectId()), legacyOwners)) {
				QuestHandler questHandler = null;
				if (requestedOwner != 0) {
					questHandler = getQuestHandlerByQuestId(requestedOwner);
					if (questHandler != null) {
						QuestHandler handler = questHandler;
						if (invokeObserved(env, requestedOwner, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
								() -> handler.onDialogEvent(env), QuestEngine::booleanResult)) {
							return true;
						}
						QuestTemplate qt = DataManager.QUEST_DATA.getQuestById(requestedOwner);
						if (qt != null && qt.getCategory() == QuestCategory.CHALLENGE_TASK
								&& player.getAccessLevel() > 0) {
							PacketSendUtility.sendMessage(player,
								"You're GM! So system won't apply countNextRepeatTime()");
							return true;
						} else if (qt != null && qt.getCategory() == QuestCategory.CHALLENGE_TASK
								&& player.getAccessLevel() == 0) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400855, 9));
						}
					}
				} else {
					List<Integer> onTalkEvents = getQuestNpc(npcId).getOnTalkEvent();
					if (onTalkEvents == null || onTalkEvents.isEmpty()) {
						log.debug(I18n.get("log.quest_engine.no_quests_for_npc", npcId));
						return false;
					}
					for (int questId : onTalkEvents) {
						QuestTemplate qt = DataManager.QUEST_DATA.getQuestById(questId);
						if (qt == null) {
							log.warn(I18n.get("log.2e7b7247f488", questId));
							continue;
						}
						QuestState qs = player.getQuestStateList().getQuestState(questId);
						if (qs == null || qs.getStatus() == QuestStatus.NONE) {
							QuestEnv checkEnv = new QuestEnv(env.getVisibleObject(), player, questId, env.getDialogId());
							if (!QuestService.checkStartConditions(checkEnv, false)) {
								continue;
							}
						}
						questHandler = getQuestHandlerByQuestId(questId);
						if (questHandler != null) {
							env.setQuestId(questId);
							QuestHandler handler = questHandler;
							if (invokeObserved(env, questId, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
									() -> handler.onDialogEvent(env), QuestEngine::booleanResult)) {
								return true;
							}
						}
					}
					env.setQuestId(0);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.dd0b8ceead0c", ex));
			return false;
		}
		return false;
	}

	/**
	 * 分发击杀事件。
	 * Dispatch a kill event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功（异常时 false） / Whether successful ({@code false} on error)
	 */
	public boolean onKill(QuestEnv env) {
		try {
			Npc npc = (Npc) env.getVisibleObject();
			QuestEvent event = new QuestEvent.KillNpc(npc.getNpcId());
			QuestProductionDispatcher typed = productionDispatcher;
			typed.dispatch(event, env.getPlayer().getObjectId(), 0, QuestDispatchContract.BROADCAST);
			List<Integer> legacyOwners = getQuestNpc(npc.getNpcId()).getOnKillEvent().stream()
				.filter(questId -> !typed.owns(questId))
				.toList();
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> event, legacyOwners)) {
				for (int questId : legacyOwners) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questId);
					if (questHandler != null) {
						env.setQuestId(questId);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "KILL_NPC", QuestDispatchContract.BROADCAST,
							() -> {
								handler.onKillEvent(env);
								return (Void) null;
							}, (ignored, stateChanged, recorder) -> voidResult(questId, ignored, stateChanged, recorder));
					}
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.59f50c2b1e29", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发攻击事件。
	 * Dispatch an attack event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onAttack(QuestEnv env) {
		try {
			Npc npc = (Npc) env.getVisibleObject();
			List<Integer> questIds = getQuestNpc(npc.getNpcId()).getOnAttackEvent();
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.AttackNpc(npc.getNpcId()), questIds)) {
				for (int questId : questIds) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questId);
					if (questHandler != null) {
						env.setQuestId(questId);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "ATTACK_NPC", QuestDispatchContract.BROADCAST,
							() -> handler.onAttackEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.105680305f00", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发升级事件（仅对未完成任务调用处理器）。
	 * Dispatch a level-up event (handlers only for incomplete quests).
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onLvlUp(QuestEnv env) {
		try {
			Player player = env.getPlayer();
			try (QuestShadowCapture.Scope scope = shadowScope(player, QuestEvent.LevelUp::new, questOnLevelUp)) {
				for (int index = 0; index < questOnLevelUp.size(); index++) {
					QuestHandler questHandler = null;
					QuestState qs = player.getQuestStateList().getQuestState(questOnLevelUp.get(index));
					if (qs == null || qs.getStatus() != QuestStatus.COMPLETE) {
						questHandler = getQuestHandlerByQuestId(questOnLevelUp.get(index));
					}
					if (questHandler != null) {
						env.setQuestId(questOnLevelUp.get(index));
						int questId = questOnLevelUp.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "LEVEL_UP", QuestDispatchContract.BROADCAST,
							() -> handler.onLvlUpEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.b844c9346335", ex));
		}
	}

	/**
	 * 分发区域任务结束事件。
	 * Dispatch a zone-mission-end event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onEnterZoneMissionEnd(QuestEnv env) {
		try {
			List<Integer> legacyOwners = List.of(env.getQuestId());
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					QuestEvent.ZoneMissionEnd::new, legacyOwners)) {
				int result = questOnEnterZoneMissionEnd.indexOf(env.getQuestId());
				QuestHandler questHandler = null;
				if (result != -1) {
					questHandler = getQuestHandlerByQuestId(questOnEnterZoneMissionEnd.get(result));
				}
				if (questHandler != null) {
					env.setQuestId(questOnEnterZoneMissionEnd.get(result));
					int questId = questOnEnterZoneMissionEnd.get(result);
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "ZONE_MISSION_END", QuestDispatchContract.EXCLUSIVE,
						() -> handler.onZoneMissionEndEvent(env), QuestEngine::booleanResult);
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.b844c9346335", ex));
		}
	}

	/**
	 * 分发玩家死亡事件。
	 * Dispatch a player-death event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onDie(QuestEnv env) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					QuestEvent.Die::new, questOnDie)) {
				for (int index = 0; index < questOnDie.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questOnDie.get(index));
					if (questHandler != null) {
						env.setQuestId(questOnDie.get(index));
						int questId = questOnDie.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "DIE", QuestDispatchContract.BROADCAST,
							() -> handler.onDieEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.86c5c656aa98", ex));
		}
	}

	/**
	 * 分发玩家登出事件。
	 * Dispatch a player-logout event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onLogOut(QuestEnv env) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> runtimeComposition.recoveryEventPort().logOut(env), questOnLogOut)) {
				for (int index = 0; index < questOnLogOut.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questOnLogOut.get(index));
					if (questHandler != null) {
						env.setQuestId(questOnLogOut.get(index));
						int questId = questOnLogOut.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "LOG_OUT", QuestDispatchContract.BROADCAST,
							() -> handler.onLogOutEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.223296a535b7", ex));
		} finally {
			runtimeComposition.recoveryEventPort().recover(env);
		}
	}

	/**
	 * 分发跟随 NPC 到达目标事件。
	 * Dispatch an escort NPC reach-target event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onNpcReachTarget(QuestEnv env) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					QuestEvent.NpcReachTarget::new, reachTarget)) {
				for (int index = 0; index < reachTarget.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(reachTarget.get(index));
					if (questHandler != null && env.getQuestId() == reachTarget.get(index)) {
						env.setQuestId(reachTarget.get(index));
						int questId = reachTarget.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "NPC_REACH_TARGET", QuestDispatchContract.EXCLUSIVE,
							() -> handler.onNpcReachTargetEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.c805bdea58a2", ex));
		}
	}

	/**
	 * 分发跟随 NPC 丢失目标事件。
	 * Dispatch an escort NPC lost-target event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onNpcLostTarget(QuestEnv env) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					QuestEvent.NpcLostTarget::new, lostTarget)) {
				for (int index = 0; index < lostTarget.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(lostTarget.get(index));
					if (questHandler != null && env.getQuestId() == lostTarget.get(index)) {
						env.setQuestId(lostTarget.get(index));
						int questId = lostTarget.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "NPC_LOST_TARGET", QuestDispatchContract.EXCLUSIVE,
							() -> handler.onNpcLostTargetEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.9e0a2243c5b5", ex));
		}
	}

	/**
	 * 分发穿过飞行环事件。
	 * Dispatch a pass-flying-ring event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param FlyRing 飞行环标识 / Flying-ring key
	 */
	public void onPassFlyingRing(QuestEnv env, String FlyRing) {
		try {
			IntArrayList lists = getOnPassFlyingRingsQuests(FlyRing);
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> runtimeComposition.movementEventPort().passFlyingRing(env, FlyRing), lists)) {
				for (int index = 0; index < lists.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(lists.get(index));
					if (questHandler != null) {
						env.setQuestId(lists.get(index));
						int questId = lists.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "PASS_FLYING_RING", QuestDispatchContract.BROADCAST,
							() -> handler.onPassFlyingRingEvent(env, FlyRing), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.3acccd87e595", ex));
		}
	}

	/**
	 * 分发进入世界事件。
	 * Dispatch an enter-world event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onEnterWorld(QuestEnv env) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					QuestEvent.EnterWorld::new, questOnEnterWorld)) {
				for (int index = 0; index < questOnEnterWorld.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questOnEnterWorld.get(index));
					if (questHandler != null) {
						env.setQuestId(questOnEnterWorld.get(index));
						int questId = questOnEnterWorld.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "ENTER_WORLD", QuestDispatchContract.BROADCAST,
							() -> handler.onEnterWorldEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.d48896f83594", ex));
		}
	}

	/**
	 * 分发使用物品事件；首个非 UNKNOWN 结果即返回。
	 * Dispatch an item-use event; return the first non-UNKNOWN result.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param item 使用的物品 / Used item
	 * Handler result
	 */
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		try {
			IntArrayList lists = getItemRelatedQuests(item.getItemTemplate().getTemplateId());
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.UseItem(item.getItemTemplate().getTemplateId()), lists)) {
				for (int index = 0; index < lists.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(lists.get(index));
					if (questHandler != null) {
						env.setQuestId(lists.get(index));
						int questId = lists.get(index);
						QuestHandler handler = questHandler;
						HandlerResult result = invokeObserved(env, questId, "USE_ITEM", QuestDispatchContract.FIRST_NON_UNKNOWN,
							() -> handler.onItemUseEvent(env, item), QuestEngine::handlerResult);
						// 允许其他任务处理；同一物品可不只用于一个任务。 / allow other quests to process, the same item can be used not in one quest
						if (result != HandlerResult.UNKNOWN) {
							return result;
						}
					}
				}
			}
			return HandlerResult.UNKNOWN;
		} catch (Exception ex) {
			// log.error(I18n.get("log.882dbd53a6cc", ex));
			return HandlerResult.FAILED;
		}
	}

	/**
	 * 分发使用房屋物品事件。
	 * Dispatch a house-item use event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item template id
	 * Always {@code false}。
	 */
	public boolean onHouseItemUseEvent(QuestEnv env, int itemId) {
		return onHouseItemUseEvent(env, itemId, 0);
	}

	/** Dispatches a house item event with the client-provided house object identity when available. */
	public boolean onHouseItemUseEvent(QuestEnv env, int itemId, int itemObjectId) {
		IntArrayList lists = getHouseItemQuests(itemId);
		try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
				() -> runtimeComposition.housingEventPort().houseItemUse(env, itemId, itemObjectId), lists)) {
			for (int index = 0; index < lists.size(); index++) {
				QuestHandler questHandler = getQuestHandlerByQuestId(lists.get(index));
				if (questHandler != null) {
					env.setQuestId(lists.get(index));
					int questId = lists.get(index);
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "HOUSE_ITEM_USE", QuestDispatchContract.BROADCAST,
						() -> handler.onHouseItemUseEvent(env, itemId), QuestEngine::booleanResult);
				}
			}
		}
		return false;
	}

	/**
	 * 分发获得物品事件。
	 * Dispatch an item-obtained event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 */
	public void onItemGet(QuestEnv env, int itemId) {
		if (questItems.containsKey(itemId)) {
			List<Integer> questIds = questItems.get(itemId);
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.GetItem(itemId), questIds)) {
				for (int i = 0; i < questIds.size(); i++) {
					int questId = questIds.get(i);
					QuestHandler questHandler = getQuestHandlerByQuestId(questId);
					if (questHandler != null) {
						env.setQuestId(questId);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "GET_ITEM", QuestDispatchContract.BROADCAST,
							() -> handler.onGetItemEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		}
	}

	/**
	 * 分发击杀指定军衔玩家事件。
	 * Dispatch a kill-ranked-player event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param playerRank 被杀玩家军衔 / Victim rank
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onKillRanked(QuestEnv env, AbyssRankEnum playerRank) {
		return onKillRanked(env, playerRank, env == null ? null : env.getPlayer(), QuestPvpCreditSource.SOLO);
	}

	/** Dispatches a PvpService-authorized ranked kill with explicit credit source. */
	public boolean onKillRanked(QuestEnv env, AbyssRankEnum playerRank, Player killer,
		QuestPvpCreditSource creditSource) {
		try {
			if (playerRank != null) {
				IntArrayList questList = getOnKillRankedQuests(playerRank);
				try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> runtimeComposition.pvpEventPort().killRanked(env, killer, playerRank.getId(), creditSource), questList)) {
					for (int index = 0; index < questList.size(); index++) {
						int id = questList.get(index);
						QuestHandler questHandler = getQuestHandlerByQuestId(id);
						if (questHandler != null) {
							env.setQuestId(id);
							QuestHandler handler = questHandler;
							invokeObserved(env, id, "KILL_RANKED", QuestDispatchContract.BROADCAST,
								() -> handler.onKillRankedEvent(env), QuestEngine::booleanResult);
						}
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.1227af0919fd", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发世界内击杀事件。
	 * Dispatch a kill-in-world event.
	 *
	 * @param env 任务环境 / Quest environment
	 * 世界 ID / World id
	 *
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onKillInWorld(QuestEnv env, int worldId) {
		return onKillInWorld(env, worldId, env == null ? null : env.getPlayer(),
			QuestPvpCreditSource.SOLO);
	}

	/** Dispatches a PvpService-authorized world kill with explicit credit source. */
	public boolean onKillInWorld(QuestEnv env, int worldId, Player killer,
		QuestPvpCreditSource creditSource) {
		try {
			if (questOnKillInWorld.containsKey(worldId)) {
				IntArrayList killInWorldQuests = questOnKillInWorld.get(worldId);
				try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> runtimeComposition.pvpEventPort().killInWorld(env, killer,
						getVictimRankId(env), worldId, creditSource), killInWorldQuests)) {
					for (int i = 0; i < killInWorldQuests.size(); i++) {
						QuestHandler questHandler = getQuestHandlerByQuestId(killInWorldQuests.get(i));
						if (questHandler != null) {
							env.setQuestId(killInWorldQuests.get(i));
							int questId = killInWorldQuests.get(i);
							QuestHandler handler = questHandler;
							invokeObserved(env, questId, "KILL_IN_WORLD", QuestDispatchContract.BROADCAST,
								() -> handler.onKillInWorldEvent(env), QuestEngine::booleanResult);
						}
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.2fecf5cac390", ex));
			return false;
		}
		return true;
	}

	private static int getVictimRankId(QuestEnv env) {
		if (env == null || !(env.getVisibleObject() instanceof Player victim)
				|| victim.getAbyssRank() == null || victim.getAbyssRank().getRank() == null) {
			throw new IllegalArgumentException("PvP victim rank is unavailable");
		}
		return victim.getAbyssRank().getRank().getId();
	}

	/**
	 * 分发进入区域事件。
	 * Dispatch an enter-zone event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Zone name
	 *
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onEnterZone(QuestEnv env, ZoneName zoneName) {
		try {
			IntArrayList lists = getOnEnterZoneQuests(zoneName);
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.EnterZone(zoneName.name()), lists)) {
				for (int index = 0; index < lists.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(lists.get(index));
					if (questHandler != null) {
						env.setQuestId(lists.get(index));
						int questId = lists.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "ENTER_ZONE", QuestDispatchContract.BROADCAST,
							() -> handler.onEnterZoneEvent(env, zoneName), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.ba0cd9d466bc", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发离开区域事件。
	 * Dispatch a leave-zone event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Zone name
	 *
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onLeaveZone(QuestEnv env, ZoneName zoneName) {
		try {
			if (questOnLeaveZone.containsKey(zoneName)) {
				IntArrayList leaveZoneList = questOnLeaveZone.get(zoneName);
				try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
						() -> new QuestEvent.LeaveZone(zoneName.name()), leaveZoneList)) {
					for (int i = 0; i < leaveZoneList.size(); i++) {
						QuestHandler questHandler = getQuestHandlerByQuestId(leaveZoneList.get(i));
						if (questHandler != null) {
							env.setQuestId(leaveZoneList.get(i));
							int questId = leaveZoneList.get(i);
							QuestHandler handler = questHandler;
							invokeObserved(env, questId, "LEAVE_ZONE", QuestDispatchContract.BROADCAST,
								() -> handler.onLeaveZoneEvent(env, zoneName), QuestEngine::booleanResult);
						}
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.43775bfcbccf", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发动画结束事件。
	 * Dispatch a movie-end event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Movie id
	 *
	 * @return 是否有处理器接管 / Whether a handler took over
	 */
	public boolean onMovieEnd(QuestEnv env, int movieId) {
		try {
			IntArrayList onMovieEndQuests = getOnMovieEndQuests(movieId);
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.MovieEnd(movieId), onMovieEndQuests)) {
				for (int index = 0; index < onMovieEndQuests.size(); index++) {
					env.setQuestId(onMovieEndQuests.get(index));
					QuestHandler questHandler = getQuestHandlerByQuestId(env.getQuestId());
					if (questHandler != null) {
						QuestHandler handler = questHandler;
						if (invokeObserved(env, env.getQuestId(), "MOVIE_END", QuestDispatchContract.EXCLUSIVE,
								() -> handler.onMovieEndEvent(env, movieId), QuestEngine::booleanResult)) {
							return true;
						}
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.e20bb13d3b6a", ex));
		}
		return false;
	}

	/**
	 * 分发任务计时器结束事件。
	 * Dispatch a quest-timer-end event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onQuestTimerEnd(QuestEnv env) {
		try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
				QuestEvent.QuestTimerEnd::new, questOnTimerEnd)) {
			for (int questId : questOnTimerEnd) {
				QuestHandler questHandler = getQuestHandlerByQuestId(questId);
				if (questHandler != null) {
					env.setQuestId(questId);
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "QUEST_TIMER_END", QuestDispatchContract.BROADCAST,
						() -> {
							handler.onQuestTimerEndEvent(env);
							return (Void) null;
						}, (ignored, stateChanged, recorder) -> voidResult(questId, ignored, stateChanged, recorder));
				}
			}
		}
	}

	/**
	 * 分发隐形计时器结束事件。
	 * Dispatch an invisible-timer-end event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onInvisibleTimerEnd(QuestEnv env) {
		try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
				QuestEvent.InvisibleTimerEnd::new, onInvisibleTimerEnd)) {
			for (int questId : onInvisibleTimerEnd) {
				QuestHandler questHandler = getQuestHandlerByQuestId(questId);
				if (questHandler != null) {
					env.setQuestId(Integer.valueOf(questId));
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "INVISIBLE_TIMER_END", QuestDispatchContract.BROADCAST,
						() -> {
							handler.onQuestTimerEndEvent(env);
							return (Void) null;
						}, (ignored, stateChanged, recorder) -> voidResult(questId, ignored, stateChanged, recorder));
				}
			}
		}
	}

	/**
	 * 分发使用技能事件。
	 * Dispatch a skill-use event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Skill id
	 *
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onUseSkill(QuestEnv env, int skillId) {
		try {
			if (questOnUseSkill.containsKey(skillId)) {
				IntArrayList quests = questOnUseSkill.get(skillId);
				try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
						() -> runtimeComposition.skillEventPort().useSkill(env, skillId), quests)) {
					for (int i = 0; i < quests.size(); i++) {
						QuestHandler questHandler = getQuestHandlerByQuestId(quests.get(i));
						if (questHandler != null) {
							env.setQuestId(quests.get(i));
							int questId = quests.get(i);
							QuestHandler handler = questHandler;
							invokeObserved(env, questId, "USE_SKILL", QuestDispatchContract.BROADCAST,
								() -> handler.onUseSkillEvent(env, skillId), QuestEngine::booleanResult);
						}
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.494055729bb4", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发制作失败事件（背包中该物品数量为 0 时触发）。
	 * Dispatch a craft-fail event (fires when the inventory has zero of the item).
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 */
	public void onFailCraft(QuestEnv env, int itemId) {
		if (questOnFailCraft.containsKey(itemId)) {
			int questId = questOnFailCraft.get(itemId);
			QuestHandler questHandler = getQuestHandlerByQuestId(questId);
			if (questHandler != null) {
				try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
						() -> new QuestEvent.FailCraft(itemId), List.of(questId))) {
					if (env.getPlayer().getInventory().getItemCountByItemId(itemId) == 0) {
						env.setQuestId(questId);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "FAIL_CRAFT", QuestDispatchContract.EXCLUSIVE,
							() -> handler.onFailCraftEvent(env, itemId), QuestEngine::booleanResult);
					}
				}
			}
		}
	}

	/**
	 * 分发装备物品事件。
	 * Dispatch an equip-item event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Item id
	 */
	public void onEquipItem(QuestEnv env, int itemId) {
		if (questOnEquipItem.containsKey(itemId)) {
			Set<Integer> questIds = questOnEquipItem.get(itemId);
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.EquipItem(itemId), questIds)) {
				for (int questId : questIds) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questId);
					if (questHandler != null) {
						env.setQuestId(questId);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "EQUIP_ITEM", QuestDispatchContract.BROADCAST,
							() -> handler.onEquipItemEvent(env, itemId), QuestEngine::booleanResult);
					}
				}
			}
		}
	}

	/**
	 * 查询模板是否允许执行指定任务动作。
	 * Whether any registered handler allows the given action on the template.
	 *
	 * @param env 任务环境 / Quest environment
	 * Template id
	 * Action type
	 * Extra arguments
	 * Whether allowed
	 */
	public boolean onCanAct(final QuestEnv env, int templateId, final QuestActionType questActionType,
			final Object... objects) {
		if (questCanAct.containsKey(templateId)) {
			IntArrayList questIds = questCanAct.get(templateId);
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.CanAct(templateId, questActionType.name()), questIds)) {
				return !questIds.forEach(new IntProcedure() {
					@Override
					public boolean execute(int value) {
						QuestHandler questHandler = getQuestHandlerByQuestId(value);
						if (questHandler != null) {
							env.setQuestId(value);
							QuestHandler handler = questHandler;
							if (invokeObserved(env, value, "CAN_ACT", QuestDispatchContract.EXCLUSIVE,
								() -> handler.onCanAct(env, questActionType, objects), QuestEngine::booleanResult)) {
								return false;
							}
						}
						return true;
					}
				});
			}
		}
		return false;
	}

	/**
	 * 分发挖掘号奖励事件。
	 * Dispatch a Dredgion reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onDredgionReward(QuestEnv env) {
		try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
				() -> runtimeComposition.pvpInstanceEventPort().dredgionReward(env), questOnDredgionReward)) {
			for (int questId : questOnDredgionReward) {
				QuestHandler questHandler = getQuestHandlerByQuestId(questId);
				if (questHandler != null) {
					env.setQuestId(questId);
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "DREDGION_REWARD", QuestDispatchContract.BROADCAST,
						() -> handler.onDredgionRewardEvent(env), QuestEngine::booleanResult);
				}
			}
		}
	}

	/**
	 * 分发卡玛尔奖励事件。
	 * Dispatch a Kamar reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onKamarReward(QuestEnv env) {
		try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
				() -> runtimeComposition.pvpInstanceEventPort().kamarReward(env), questOnKamarReward)) {
			for (int questId : questOnKamarReward) {
				QuestHandler questHandler = getQuestHandlerByQuestId(questId);
				if (questHandler != null) {
					env.setQuestId(questId);
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "KAMAR_REWARD", QuestDispatchContract.BROADCAST,
						() -> handler.onKamarRewardEvent(env), QuestEngine::booleanResult);
				}
			}
		}
	}

	/**
	 * 分发欧菲丹奖励事件。
	 * Dispatch an Ophidan reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onOphidanReward(QuestEnv env) {
		try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
				() -> runtimeComposition.pvpInstanceEventPort().ophidanReward(env), questOnOphidanReward)) {
			for (int questId : questOnOphidanReward) {
				QuestHandler questHandler = getQuestHandlerByQuestId(questId);
				if (questHandler != null) {
					env.setQuestId(questId);
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "OPHIDAN_REWARD", QuestDispatchContract.BROADCAST,
						() -> handler.onOphidanRewardEvent(env), QuestEngine::booleanResult);
				}
			}
		}
	}

	/**
	 * 分发堡垒奖励事件。
	 * Dispatch a Bastion reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onBastionReward(QuestEnv env) {
		try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
				() -> runtimeComposition.pvpInstanceEventPort().bastionReward(env), questOnBastionReward)) {
			for (int questId : questOnBastionReward) {
				QuestHandler questHandler = getQuestHandlerByQuestId(questId);
				if (questHandler != null) {
					env.setQuestId(questId);
					QuestHandler handler = questHandler;
					invokeObserved(env, questId, "BASTION_REWARD", QuestDispatchContract.BROADCAST,
						() -> handler.onBastionRewardEvent(env), QuestEngine::booleanResult);
				}
			}
		}
	}

	/**
	 * 分发奖励加成应用事件。
	 * Dispatch a bonus-apply event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Bonus type
	 * @param rewardItems 奖励物品列表 / Reward items
	 * Handler result
	 */
	public HandlerResult onBonusApplyEvent(QuestEnv env, BonusType bonusType, List<QuestItems> rewardItems) {
		try {
			IntArrayList lists = this.getOnBonusApplyQuests(bonusType);
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.BonusApply(bonusType.name()), lists)) {
				for (int index = 0; index < lists.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(lists.get(index));
					if (questHandler != null) {
						env.setQuestId(lists.get(index));
						int questId = lists.get(index);
						QuestHandler handler = questHandler;
						return invokeObserved(env, questId, "BONUS_APPLY", QuestDispatchContract.FIRST_REGISTERED,
							() -> handler.onBonusApplyEvent(env, bonusType, rewardItems), QuestEngine::handlerResult);
					}
				}
				return HandlerResult.UNKNOWN;
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.fc7e13ab7975", ex));
			return HandlerResult.FAILED;
		}
	}

	/**
	 * 分发被加入仇恨列表事件。
	 * Dispatch an add-to-aggro-list event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onAddAggroList(QuestEnv env) {
		return onAddAggroList(env, null);
	}

	/** Dispatches an aggro observation with the actual hostile source supplied by AggroList. */
	public boolean onAddAggroList(QuestEnv env, Creature aggroSource) {
		try {
			Npc npc = (Npc) env.getVisibleObject();
			List<Integer> questIds = getQuestNpc(npc.getNpcId()).getOnAddAggroListEvent();
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> runtimeComposition.aiPerceptionEventPort().addAggroList(env, npc.getNpcId(), aggroSource), questIds)) {
				for (int questId : questIds) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questId);
					if (questHandler != null) {
						env.setQuestId(questId);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "ADD_AGGRO_LIST", QuestDispatchContract.BROADCAST,
							() -> handler.onAddAggroListEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.5772cf372416", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发靠近目标距离事件（20 单位内）。
	 * Dispatch an at-distance event (within 20 units).
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onAtDistance(QuestEnv env) {
		QuestNpc questNpc = null;
		if (env == null || !(env.getVisibleObject() instanceof Npc npc) || env.getPlayer() == null) {
			return false;
		}
		if (!questNpcs.containsKey(npc.getNpcId())) {
			return false;
		}
		questNpc = getQuestNpc(npc.getNpcId());
		if (getQuestNpc(npc.getNpcId()).getOnDistanceEvent().size() == 0) {
			return false;
		}
		try {
			QuestEvent.AtDistance proximityEvent = runtimeComposition.proximityEventPort()
				.atDistance(env, npc.getNpcId());
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> proximityEvent, questNpc.getOnDistanceEvent())) {
				for (int questId : questNpc.getOnDistanceEvent()) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questId);
					if (questHandler != null) {
						env.setQuestId(questId);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "AT_DISTANCE", QuestDispatchContract.BROADCAST,
							() -> handler.onAtDistanceEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.873dcad16db3", ex));
			return false;
		}
		return true;
	}

	/**
	 * 分发进入风道事件。
	 * Dispatch an enter-windstream event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param loc 位置 / 世界标识 / Location or world id
	 */
	public void onEnterWindStream(QuestEnv env, int teleportId) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> runtimeComposition.movementEventPort().enterWindStream(env, teleportId), questOnEnterWindStream)) {
				for (int index = 0; index < questOnEnterWindStream.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questOnEnterWindStream.get(index));
					if (questHandler != null) {
						env.setQuestId(questOnEnterWindStream.get(index));
						int questId = questOnEnterWindStream.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "ENTER_WIND_STREAM", QuestDispatchContract.BROADCAST,
							() -> handler.onEnterWindStreamEvent(env, teleportId), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.2f404bb783fc", ex));
		}
	}

	/**
	 * 分发骑乘动作事件。
	 * Dispatch a ride-action event.
	 *
	 * @param env 任务环境 / Quest environment
	 * Ride item id
	 */
	public void rideAction(QuestEnv env, int itemId) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					() -> new QuestEvent.RideAction(itemId), questRideAction)) {
				for (int index = 0; index < questRideAction.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questRideAction.get(index));
					if (questHandler != null) {
						env.setQuestId(questRideAction.get(index));
						int questId = questRideAction.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "RIDE_ACTION", QuestDispatchContract.BROADCAST,
							() -> handler.rideAction(env, itemId), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.32f0e831856e", ex));
		}
	}

	/**
	 * 分发创造力点数事件。
	 * Dispatch a creativity-point event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onCreativityPoint(QuestEnv env) {
		try {
			try (QuestShadowCapture.Scope scope = shadowScope(env.getPlayer(),
					QuestEvent.CreativityPoint::new, questOnCreativityPoint)) {
				for (int index = 0; index < questOnCreativityPoint.size(); index++) {
					QuestHandler questHandler = getQuestHandlerByQuestId(questOnCreativityPoint.get(index));
					if (questHandler != null) {
						env.setQuestId(questOnCreativityPoint.get(index));
						int questId = questOnCreativityPoint.get(index);
						QuestHandler handler = questHandler;
						invokeObserved(env, questId, "CREATIVITY_POINT", QuestDispatchContract.BROADCAST,
							() -> handler.onCreativityPointEvent(env), QuestEngine::booleanResult);
					}
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.18e75e1aea8d", ex));
		}
	}

	/**
	 * 注册（或获取）NPC 的任务关联对象。
	 * Register (or obtain) the quest association for an NPC.
	 *
	 * NPC 模板 ID / NPC template id
	 * QuestNpc association
	 */
	public QuestNpc registerQuestNpc(int npcId) {
		if (!questNpcs.containsKey(npcId)) {
			questNpcs.put(npcId, new QuestNpc(npcId));
		}
		return questNpcs.get(npcId);
	}

	/**
	 * 注册物品使用关联任务。
	 * Register a quest for item-use events.
	 *
	 * Item id
	 * Quest id
	 */
	public void registerQuestItem(int itemId, int questId) {
		if (!questItemRelated.containsKey(itemId)) {
			IntArrayList itemRelatedQuests = new IntArrayList();
			itemRelatedQuests.add(questId);
			questItemRelated.put(itemId, itemRelatedQuests);
		} else {
			questItemRelated.get(itemId).add(questId);
		}
	}

	/**
	 * 注册房屋物品关联任务。
	 * Register a quest for house-item use events.
	 *
	 * Item id
	 * Quest id
	 */
	public void registerQuestHouseItem(int itemId, int questId) {
		if (!questHouseItems.containsKey(itemId)) {
			IntArrayList itemRelatedQuests = new IntArrayList();
			itemRelatedQuests.add(questId);
			questHouseItems.put(itemId, itemRelatedQuests);
		} else {
			((IntArrayList) questHouseItems.get(itemId)).add(questId);
		}
	}

	/**
	 * 注册获得物品关联任务。
	 * Register a quest for item-obtain events.
	 *
	 * Item id
	 * Quest id
	 */
	public void registerGetingItem(int itemId, int questId) {
		if (!questItems.containsKey(itemId)) {
			IntArrayList questItemsToReg = new IntArrayList();
			questItemsToReg.add(questId);
			questItems.put(itemId, questItemsToReg);
		} else {
			questItems.get(itemId).add(questId);
		}
	}

	/**
	 * 注册升级监听。
	 * Register a level-up listener.
	 *
	 * Quest id
	 */
	public void registerOnLevelUp(int questId) {
		if (!questOnLevelUp.contains(questId)) {
			questOnLevelUp.add(questId);
		}
	}

	/**
	 * 注册区域任务结束监听。
	 * Register a zone-mission-end listener.
	 *
	 * Quest id
	 */
	public void registerOnEnterZoneMissionEnd(int questId) {
		if (!questOnEnterZoneMissionEnd.contains(questId)) {
			questOnEnterZoneMissionEnd.add(questId);
		}
	}

	/**
	 * 注册进入世界监听。
	 * Register an enter-world listener.
	 *
	 * Quest id
	 */
	public void registerOnEnterWorld(int questId) {
		if (!questOnEnterWorld.contains(questId)) {
			questOnEnterWorld.add(questId);
		}
	}

	/**
	 * 注册死亡监听。
	 * Register a death listener.
	 *
	 * Quest id
	 */
	public void registerOnDie(int questId) {
		if (!questOnDie.contains(questId)) {
			questOnDie.add(questId);
		}
	}

	/**
	 * 注册登出监听。
	 * Register a logout listener.
	 *
	 * Quest id
	 */
	public void registerOnLogOut(int questId) {
		if (!questOnLogOut.contains(questId)) {
			questOnLogOut.add(questId);
		}
	}

	/**
	 * 注册进入区域监听。
	 * Register an enter-zone listener.
	 *
	 * Zone name
	 * Quest id
	 */
	public void registerOnEnterZone(ZoneName zoneName, int questId) {
		if (!questOnEnterZone.containsKey(zoneName)) {
			IntArrayList onEnterZoneQuests = new IntArrayList();
			onEnterZoneQuests.add(questId);
			questOnEnterZone.put(zoneName, onEnterZoneQuests);
		} else {
			questOnEnterZone.get(zoneName).add(questId);
		}
	}

	/**
	 * 注册离开区域监听。
	 * Register a leave-zone listener.
	 *
	 * Zone name
	 * Quest id
	 */
	public void registerOnLeaveZone(ZoneName zoneName, int questId) {
		if (!questOnLeaveZone.containsKey(zoneName)) {
			IntArrayList onLeaveZoneQuests = new IntArrayList();
			onLeaveZoneQuests.add(questId);
			questOnLeaveZone.put(zoneName, onLeaveZoneQuests);
		} else {
			questOnLeaveZone.get(zoneName).add(questId);
		}
	}

	/**
	 * 注册击杀军衔玩家监听（覆盖自该军衔及以上）。
	 * Register a kill-ranked listener (covers the given rank and above).
	 *
	 * Starting rank
	 * Quest id
	 */
	public void registerOnKillRanked(AbyssRankEnum playerRank, int questId) {
		for (int rank = playerRank.getId(); rank < 19; rank++) {
			if (!questOnKillRanked.containsKey(AbyssRankEnum.getRankById(rank))) {
				IntArrayList onKillRankedQuests = new IntArrayList();
				onKillRankedQuests.add(questId);
				questOnKillRanked.put(AbyssRankEnum.getRankById(rank), onKillRankedQuests);
			} else {
				questOnKillRanked.get(AbyssRankEnum.getRankById(rank)).add(questId);
			}
		}
	}

	/**
	 * 注册世界内击杀监听。
	 * Register a kill-in-world listener.
	 *
	 * 世界 ID / World id
	 * Quest id
	 */
	public void registerOnKillInWorld(int worldId, int questId) {
		if (!questOnKillInWorld.containsKey(worldId)) {
			IntArrayList killInWorldQuests = new IntArrayList();
			killInWorldQuests.add(questId);
			questOnKillInWorld.put(worldId, killInWorldQuests);
		} else {
			questOnKillInWorld.get(worldId).add(questId);
		}
	}

	/**
	 * 注册穿过飞行环监听。
	 * Register a pass-flying-ring listener.
	 *
	 * @param flyingRing 飞行环标识 / Flying-ring key
	 * Quest id
	 */
	public void registerOnPassFlyingRings(String flyingRing, int questId) {
		if (!questOnPassFlyingRings.containsKey(flyingRing)) {
			IntArrayList onPassFlyingRingsQuests = new IntArrayList();
			onPassFlyingRingsQuests.add(questId);
			questOnPassFlyingRings.put(flyingRing, onPassFlyingRingsQuests);
		} else {
			questOnPassFlyingRings.get(flyingRing).add(questId);
		}
	}

	/**
	 * 注册动画结束监听。
	 * Register a movie-end listener.
	 *
	 * Movie id
	 * Quest id
	 */
	public void registerOnMovieEndQuest(int moveId, int questId) {
		if (!questOnMovieEnd.containsKey(moveId)) {
			IntArrayList onMovieEndQuests = new IntArrayList();
			onMovieEndQuests.add(questId);
			questOnMovieEnd.put(moveId, onMovieEndQuests);
		} else {
			questOnMovieEnd.get(moveId).add(questId);
		}
	}

	/**
	 * 注册计时器结束监听。
	 * Register a quest-timer-end listener.
	 *
	 * Quest id
	 */
	public void registerOnQuestTimerEnd(int questId) {
		if (!questOnTimerEnd.contains(questId)) {
			questOnTimerEnd.add(questId);
		}
	}

	/**
	 * 注册隐形计时器结束监听。
	 * Register an invisible-timer-end listener.
	 *
	 * Quest id
	 */
	public void registerOnInvisibleTimerEnd(int questId) {
		if (!onInvisibleTimerEnd.contains(Integer.valueOf(questId))) {
			onInvisibleTimerEnd.add(Integer.valueOf(questId));
		}
	}

	/**
	 * 注册使用技能监听。
	 * Register a skill-use listener.
	 *
	 * Skill id
	 * Quest id
	 */
	public void registerQuestSkill(int skillId, int questId) {
		if (!questOnUseSkill.containsKey(skillId)) {
			IntArrayList questSkills = new IntArrayList();
			questSkills.add(questId);
			questOnUseSkill.put(skillId, questSkills);
		} else {
			questOnUseSkill.get(skillId).add(questId);
		}
	}

	/**
	 * 注册制作失败监听。
	 * Register a craft-fail listener.
	 *
	 * Item id
	 * Quest id
	 */
	public void registerOnFailCraft(int itemId, int questId) {
		if (!questOnFailCraft.containsKey(itemId)) {
			questOnFailCraft.put(itemId, questId);
		}
	}

	/**
	 * 注册装备物品监听。
	 * Register an equip-item listener.
	 *
	 * Item id
	 * Quest id
	 */
	public void registerOnEquipItem(int itemId, int questId) {
		if (!questOnEquipItem.containsKey(itemId)) {
			Set<Integer> questIds = new HashSet<Integer>();
			questIds.add(questId);
			questOnEquipItem.put(itemId, questIds);
		} else {
			questOnEquipItem.get(itemId).add(questId);
		}
	}

	/**
	 * 注册可行动作监听。
	 * Register a can-act listener for a template.
	 *
	 * Quest id
	 * Template id
	 */
	public void registerCanAct(int questId, int templateId) {
		if (!questCanAct.containsKey(templateId)) {
			IntArrayList questSkills = new IntArrayList();
			questSkills.add(questId);
			questCanAct.put(templateId, questSkills);
		} else {
			questCanAct.get(templateId).add(questId);
		}
	}

	/**
	 * 注册挖掘号奖励监听。
	 * Register a Dredgion reward listener.
	 *
	 * Quest id
	 */
	public void registerOnDredgionReward(int questId) {
		if (!questOnDredgionReward.contains(questId)) {
			questOnDredgionReward.add(questId);
		}
	}

	/**
	 * 注册卡玛尔奖励监听。
	 * Register a Kamar reward listener.
	 *
	 * Quest id
	 */
	public void registerOnKamarReward(int questId) {
		if (!questOnKamarReward.contains(questId)) {
			questOnKamarReward.add(questId);
		}
	}

	/**
	 * 注册欧菲丹奖励监听。
	 * Register an Ophidan reward listener.
	 *
	 * Quest id
	 */
	public void registerOnOphidanReward(int questId) {
		if (!questOnOphidanReward.contains(questId)) {
			questOnOphidanReward.add(questId);
		}
	}

	/**
	 * 注册堡垒奖励监听。
	 * Register a Bastion reward listener.
	 *
	 * Quest id
	 */
	public void registerOnBastionReward(int questId) {
		if (!questOnBastionReward.contains(questId)) {
			questOnBastionReward.add(questId);
		}
	}

	/**
	 * 注册奖励加成应用监听。
	 * Register a bonus-apply listener.
	 *
	 * Quest id
	 * Bonus type
	 */
	public void registerOnBonusApply(int questId, BonusType bonusType) {
		if (!questOnBonusApply.containsKey(bonusType)) {
			IntArrayList onBonusApplyQuests = new IntArrayList();
			onBonusApplyQuests.add(questId);
			questOnBonusApply.put(bonusType, onBonusApplyQuests);
		} else {
			questOnBonusApply.get(bonusType).add(questId);
		}
	}

	/**
	 * 按加成类型查询已注册任务列表。
	 * Look up quests registered for a bonus type.
	 *
	 * Bonus type
	 * Quest id list
	 */
	private IntArrayList getOnBonusApplyQuests(BonusType bonusType) {
		if (questOnBonusApply.containsKey(bonusType)) {
			return questOnBonusApply.get(bonusType);
		}
		return new IntArrayList();
	}

	/**
	 * 注册进入风道监听。
	 * Register an enter-windstream listener.
	 *
	 * Quest id
	 */
	public void registerOnEnterWindStream(int questId) {
		if (!questOnEnterWindStream.contains(questId))
			questOnEnterWindStream.add(questId);
	}

	/**
	 * 注册骑乘动作监听。
	 * Register a ride-action listener.
	 *
	 * Quest id
	 */
	public void registerOnRide(int questId) {
		if (!questRideAction.contains(questId))
			questRideAction.add(questId);
	}

	/**
	 * 注册创造力点数监听。
	 * Register a creativity-point listener.
	 *
	 * Quest id
	 */
	public void registerOnCreativityPoint(int questId) {
		if (!questOnCreativityPoint.contains(questId))
			questOnCreativityPoint.add(questId);
	}

	/**
	 * 注册跟随到达目标监听。
	 * Register a reach-target listener.
	 *
	 * Quest id
	 */
	public void registerAddOnReachTargetEvent(int questId) {
		if (!reachTarget.contains(questId))
			reachTarget.add(questId);
	}

	/**
	 * 注册跟随丢失目标监听。
	 * Register a lost-target listener.
	 *
	 * Quest id
	 */
	public void registerAddOnLostTargetEvent(int questId) {
		if (!lostTarget.contains(questId))
			lostTarget.add(questId);
	}

	/**
	 * 获取 NPC 的任务关联对象；未注册时返回空壳。
	 * Return the quest association for an NPC, or an empty shell if unregistered.
	 *
	 * NPC 模板 ID / NPC template id
	 * QuestNpc association
	 */
	public QuestNpc getQuestNpc(int npcId) {
		if (questNpcs.containsKey(npcId)) {
			return questNpcs.get(npcId);
		}
		return new QuestNpc(npcId);
	}

	/**
	 * 按对话框 ID 查询枚举。
	 * Look up a {@link QuestDialog} by dialog id.
	 *
	 * Dialog id
	 *
	 * @param dialogId
	 * @return 枚举值；不存在时 null / Enum value, or {@code null}
	 */
	public QuestDialog getDialog(int dialogId) {
		if (dialogMap.containsKey(dialogId)) {
			return dialogMap.get(dialogId);
		}
		return null;
	}

	/**
	 * 查询物品使用关联任务。
	 * Look up quests related to item use.
	 *
	 * Item id
	 * Quest id list
	 */
	private IntArrayList getItemRelatedQuests(int itemId) {
		if (questItemRelated.containsKey(itemId)) {
			return questItemRelated.get(itemId);
		}
		return new IntArrayList();
	}

	/**
	 * 查询房屋物品关联任务。
	 * Look up quests related to house items.
	 *
	 * Item id
	 * Quest id list
	 */
	private IntArrayList getHouseItemQuests(int itemId) {
		if (questHouseItems.containsKey(itemId)) {
			return (IntArrayList) questHouseItems.get(itemId);
		}
		return new IntArrayList();
	}

	/**
	 * 查询进入区域关联任务。
	 * Look up quests related to entering a zone.
	 *
	 * Zone name
	 * Quest id list
	 */
	private IntArrayList getOnEnterZoneQuests(ZoneName zoneName) {
		if (questOnEnterZone.containsKey(zoneName)) {
			return questOnEnterZone.get(zoneName);
		}
		return new IntArrayList();
	}

	/**
	 * 查询击杀军衔关联任务。
	 * Look up quests related to killing a ranked player.
	 *
	 * Rank
	 * Quest id list
	 */
	private IntArrayList getOnKillRankedQuests(AbyssRankEnum playerRank) {
		if (questOnKillRanked.containsKey(playerRank)) {
			return questOnKillRanked.get(playerRank);
		}
		return new IntArrayList();
	}

	/**
	 * 查询穿过飞行环关联任务。
	 * Look up quests related to passing a flying ring.
	 *
	 * @param flyingRing 飞行环标识 / Flying-ring key
	 * Quest id list
	 */
	private IntArrayList getOnPassFlyingRingsQuests(String flyingRing) {
		if (questOnPassFlyingRings.containsKey(flyingRing)) {
			return questOnPassFlyingRings.get(flyingRing);
		}
		return new IntArrayList();
	}

	/**
	 * 查询动画结束关联任务。
	 * Look up quests related to a movie end.
	 *
	 * Movie id
	 * Quest id list
	 */
	private IntArrayList getOnMovieEndQuests(int moveId) {
		if (questOnMovieEnd.containsKey(moveId)) {
			return questOnMovieEnd.get(moveId);
		}
		return new IntArrayList();
	}

	/**
	 * 按任务 ID 获取处理器。
	 * Look up a handler by quest id.
	 *
	 * Quest id
	 *
	 * @param questId
	 * @return 处理器；不存在时 null / Handler, or {@code null}
	 */
	private QuestHandler getQuestHandlerByQuestId(int questId) {
		return questHandlers.get(questId);
	}

	/**
	 * 是否已有该任务的处理器。
	 * Whether a handler is registered for the quest.
	 *
	 * Quest id
	 * Whether present
	 */
	public boolean isHaveHandler(int questId) {
		return productionDispatcher.owns(questId) || questHandlers.containsKey(questId);
	}

	/** 加载并编译正式 typed 任务 catalog。 Load and compile the production typed quest catalog. */
	private QuestCatalog loadProductionCatalog() throws Exception {
		ClassLoader loader = QuestEngine.class.getClassLoader();
		try (InputStream input = loader.getResourceAsStream(PRODUCTION_DEFINITION_CATALOG)) {
			if (input == null) {
				throw new IllegalStateException("missing typed production catalog: "
					+ PRODUCTION_DEFINITION_CATALOG);
			}
			return QuestDefinitionCatalogManifest.compile(input, loader);
		}
	}

	/**
	 * 在 legacy Handler 注册前校验并安装全部正式 typed owner。
	 * Validate and install all production typed owners before legacy handlers register.
	 *
	 * <p>所有 owner 和事件接线会先完成校验，dispatcher 只在 NPC 索引注册完成后发布。
	 * All owners and event wiring are validated first; the dispatcher is published only after
	 * NPC indexes have been registered.</p>
	 */
	void installProductionDefinitions(QuestCatalog catalog) {
		QuestProductionDispatcher dispatcher = QuestProductionDispatcher.production(catalog, runtimeComposition);
		for (CompiledQuestDefinition definition : catalog.all()) {
			if (questHandlers.containsKey(definition.id())) {
				throw new IllegalStateException("quest " + definition.id() + " already has a legacy handler");
			}
			for (var transition : definition.definition().transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc)
						&& !(transition.event() instanceof QuestEvent.KillNpc)) {
					throw new IllegalStateException("typed production event is not wired into QuestEngine: "
						+ transition.event().type());
				}
			}
		}
		for (CompiledQuestDefinition definition : catalog.all()) {
			for (var transition : definition.definition().transitions()) {
				if (transition.event() instanceof QuestEvent.TalkToNpc talk) {
					QuestNpc questNpc = registerQuestNpc(talk.npcId());
					questNpc.addOnTalkEvent(definition.id());
					if (transition.sourceNode() != null && definition.definition().nodes().stream()
							.filter(node -> node.label().equals(transition.sourceNode()))
							.map(QuestNode::projection)
							.anyMatch(projection -> projection.status() == QuestStatus.NONE)) {
						questNpc.addOnQuestStart(definition.id());
					}
				} else if (transition.event() instanceof QuestEvent.KillNpc kill) {
					registerQuestNpc(kill.npcId()).addOnKillEvent(definition.id());
				}
			}
		}
		productionDispatcher = dispatcher;
	}

	/**
	 * 注册处理器：调用其 {@link QuestHandler#register()} 并放入映射。
	 * Register a handler: invoke {@link QuestHandler#register()} and store it.
	 *
	 * @param questHandler 任务处理器 / Quest handler
	 */
	public void addQuestHandler(QuestHandler questHandler) {
		int questId = questHandler.getQuestId();
		if (productionDispatcher.owns(questId)) {
			throw new IllegalStateException("quest " + questId + " is already owned by the typed production catalog");
		}
		questHandler.register();
		if (questHandlers.containsKey(questId)) {
			log.warn(I18n.get("log.6928c2152c98", questId));
		}
		questHandlers.put(questId, questHandler);
	}

	/**
	 * 添加处理器侧掉落（XML 未声明时由脚本侧补充）。
	 * Add a handler-side drop (supplemental when not declared in XML).
	 *
	 * Quest id
	 * NPC id
	 * Item id
	 * Amount
	 * Chance
	 */
	public void addHandlerSideQuestDrop(int questId, int npcId, int itemId, int amount, int chance) {
		HandlerSideDrop hsd = new HandlerSideDrop(questId, npcId, itemId, amount, chance);
		QuestService.addQuestDrop(hsd.getNpcId(), hsd);
	}

	/**
	 * 添加带步骤条件的处理器侧掉落。
	 * Add a handler-side drop gated by quest step.
	 *
	 * Quest id
	 * NPC id
	 * Item id
	 * Amount
	 * Chance
	 * @param step 所需步骤 / Required step
	 */
	public void addHandlerSideQuestDrop(int questId, int npcId, int itemId, int amount, int chance, int step) {
		HandlerSideDrop hsd = new HandlerSideDrop(questId, npcId, itemId, amount, chance, step);
		QuestService.addQuestDrop(hsd.getNpcId(), hsd);
	}

	/**
	 * 启动时装载：注册掉落、加载脚本处理器与 XML 任务，并启动每日提醒。
	 * Bootstrap load: register drops, load script handlers and XML quests, start daily reminders.
	 *
	 * @param progressLatch 进度闩锁（可空） / Progress latch (nullable)
	 */
	public void load(CountDownLatch progressLatch) {
		log.info(I18n.get("log.5359e35f8f99"));
		QuestsData questData = DataManager.QUEST_DATA;
		for (QuestTemplate data : questData.getQuestsData()) {
			for (QuestDrop drop : data.getQuestDrop()) {
				drop.setQuestId(data.getId());
				QuestService.addQuestDrop(drop.getNpcId(), drop);
			}
		}
		AggregatedClassListener acl = new AggregatedClassListener();
		acl.addClassListener(new OnClassLoadUnloadListener());
		acl.addClassListener(new ScheduledTaskClassListener());
		acl.addClassListener(new QuestHandlerLoader());

		try {
			installProductionDefinitions(loadProductionCatalog());
			acl.postLoad(CompiledScriptLoader.load("com.aionemu.gameserver.quest.handlers"));
			XMLQuests xmlQuests = DataManager.XML_QUESTS;
			for (XMLQuest xmlQuest : xmlQuests.getQuest()) {
				xmlQuest.register(this);
			}
			log.info(I18n.get("log.490b5f534bb2", questHandlers.size()));
			log.info(I18n.get("log.quest_engine.typed_owners_loaded", productionDispatcher.owners()));
		} catch (Exception e) {
			throw new GameServerError("Can't initialize quest handlers.", e);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
		addMessageSendingTask();
		for (QuestDialog d : QuestDialog.values()) {
			dialogMap.put(d.id(), d);
		}
	}

	/**
	 * 安排每日 9:00 的可重复任务提醒广播。
	 * Schedule the 09:00 daily broadcast for repeatable quest reminders.
	 */
	private void addMessageSendingTask() {
		Calendar sendingDate = Calendar.getInstance();
		sendingDate.set(Calendar.AM_PM, Calendar.AM);
		sendingDate.set(Calendar.HOUR, 9);
		sendingDate.set(Calendar.MINUTE, 0);
		sendingDate.set(Calendar.SECOND, 0);
		if (sendingDate.getTime().getTime() < System.currentTimeMillis()) {
			sendingDate.add(Calendar.HOUR, 24);
		}
		messageSendingTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				SM_SYSTEM_MESSAGE dailyMessage = new SM_SYSTEM_MESSAGE(1400854);
				SM_SYSTEM_MESSAGE weeklyMessage = new SM_SYSTEM_MESSAGE(1400856);
				for (Player player : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
					for (QuestState qs : player.getQuestStateList().getAllQuestState()) {
						if (qs != null && qs.canRepeat()) {
							QuestTemplate template = DataManager.QUEST_DATA.getQuestById(qs.getQuestId());
							if (template.isDaily()) {
								player.getController().updateZone();
								player.getController().updateNearbyQuests();
								PacketSendUtility.sendPacket(player, dailyMessage);
							} else if (template.isWeekly()) {
								player.getController().updateZone();
								player.getController().updateNearbyQuests();
								PacketSendUtility.sendPacket(player, weeklyMessage);
							}
						}
					}
					player.getNpcFactions().sendDailyQuest();
				}
			}
		}, sendingDate.getTimeInMillis() - System.currentTimeMillis(), 1000 * 60 * 60 * 24);
	}

	/**
	 * 关闭引擎：清空全部注册数据。
	 * Shut down the engine: clear all registered data.
	 */
	public void shutdown() {
		setShadowCapture(null);
		clear();
		log.info(I18n.get("log.dd61afc44888"));
	}

	/**
	 * 清空所有事件注册表与处理器映射，取消定时任务。
	 * Clear every event registry and handler map; cancel the reminder task.
	 */
	public void clear() {
		runtimeComposition.cleanupAll();
		productionDispatcher = QuestProductionDispatcher.disabled();
		if (messageSendingTask != null) {
			messageSendingTask.cancel(false);
			messageSendingTask = null;
		}
		QuestService.clearQuestDrops();
		questNpcs.clear();
		questItemRelated.clear();
		questItems.clear();
		questHouseItems.clear();
		questOnLevelUp.clear();
		questOnEnterZoneMissionEnd.clear();
		questOnEnterWorld.clear();
		questOnDie.clear();
		questOnLogOut.clear();
		questOnEnterZone.clear();
		questOnLeaveZone.clear();
		questOnMovieEnd.clear();
		questOnTimerEnd.clear();
		questOnPassFlyingRings.clear();
		questOnKillRanked.clear();
		questOnUseSkill.clear();
		reachTarget.clear();
		lostTarget.clear();
		questOnEnterWindStream.clear();
		questRideAction.clear();
		questOnCreativityPoint.clear();
		questHandlers.clear();
	}

	/**
	 * 延迟初始化单例持有者。
	 * Lazy singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final QuestEngine instance = new QuestEngine();
	}
}
