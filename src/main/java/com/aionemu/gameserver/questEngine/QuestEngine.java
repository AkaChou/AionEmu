package com.aionemu.gameserver.questEngine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.quest.QuestNpc;
import com.aionemu.gameserver.model.templates.rewards.BonusType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestDropScope;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNpcAttackFacts;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestBroadcastPort;
import com.aionemu.gameserver.questEngine.runtime.QuestDispatchContract;
import com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectValidator;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRouteResult;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeComposition;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeResources;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneName;

import lombok.extern.slf4j.Slf4j;

/**
 * 任务引擎单例：维护事件注册表，并向已注册处理器分发各类游戏事件。
 * Central quest-engine singleton that maintains event registries and dispatches
 * game events to registered processors.
 *
 */
@Slf4j
public class QuestEngine implements GameEngine {
	/** Spring ObjectProvider 覆盖钩子 / Spring ObjectProvider override hook */
	private static volatile ObjectProvider<QuestEngine> instanceProvider;
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
	/** Fully composed production ports used by typed quest execution. */
	private final QuestRuntimeComposition runtimeComposition = QuestRuntimeComposition.production();
	/** Live typed owner catalog and central Router/Coordinator execution chain. */
	private volatile QuestProductionDispatcher productionDispatcher = QuestProductionDispatcher.disabled();
	/** Raw catalog compiled in parallel with other startup work; cleared once consumed. */
	private volatile Future<QuestCatalog> productionCatalogPreload;

	/** Fully validated immutable typed runtime waiting to be published. */
	public record PreparedProductionDefinitions(QuestCatalogRegistry catalog,
			QuestProductionDispatcher dispatcher) {
		public PreparedProductionDefinitions {
			java.util.Objects.requireNonNull(catalog, "catalog");
			java.util.Objects.requireNonNull(dispatcher, "dispatcher");
		}
	}
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

	/** Returns the immutable catalog snapshot used by the currently published typed dispatcher. */
	public QuestCatalogRegistry questCatalog() {
		return productionDispatcher.catalogRegistry();
	}

	/** Returns quest drops from the exact catalog snapshot used by live event routing. */
	public List<QuestCatalogDrop> questDrops(int npcId) {
		return productionDispatcher.questDrops(npcId);
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
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		Player player = env.getPlayer();
		try {
			Npc npc = env.getVisibleObject() instanceof Npc target ? target : null;
			int requestedOwner = env.getQuestId();
			int npcId = npc == null ? 0 : npc.getNpcId();
			QuestProductionDispatcher typed = productionDispatcher;
			if (requestedOwner != 0 && typed.owns(requestedOwner)) {
				QuestEvent event = npcId == 0
					? new QuestEvent.QuestDialog(env.getDialogId())
					: new QuestEvent.TalkToNpc(npcId, env.getDialogId(), npc.getObjectId());
				var result = typed.dispatch(event, player.getObjectId(), requestedOwner,
					QuestDispatchContract.EXCLUSIVE);
				if (result.handled()) {
					env.setQuestId(requestedOwner);
					return true;
				}
				// A failed typed owner still owns this exclusive route, but the interaction failed.
				// Never report it as successful and never replay the retired owner.
				return false;
			}

			if (requestedOwner == 0 && npcId != 0) {
				QuestEvent event = new QuestEvent.TalkToNpc(npcId, env.getDialogId(), npc.getObjectId());
				var result = typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.EXCLUSIVE);
				if (result.handled()) {
					result.handledOwners().stream().findFirst().ifPresent(env::setQuestId);
					return true;
				}
				if (result.claimed()) {
					return false;
				}
				// Most quest interaction objects use a pure ACTION_ITEM_USE eligibility route and no
				// separate TALK transition. Re-run that side-effect-free route at use completion so
				// QuestItemNpcAI2 receives the actual owner id for group/alliance drop filtering.
				QuestEvent.CanAct actionObject = new QuestEvent.CanAct(npcId, QuestActionType.ACTION_ITEM_USE.name());
				if (env.getDialogId() == -1 && npc.getAi2() != null
						&& "quest_use_item".equals(npc.getAi2().getName()) && typed.hasRoutes(actionObject)) {
					var actionResult = typed.dispatch(actionObject, player.getObjectId(), 0,
						QuestDispatchContract.EXCLUSIVE);
					if (actionResult.handled()) {
						actionResult.handledOwners().stream().findFirst().ifPresent(env::setQuestId);
						return true;
					}
					if (actionResult.claimed()) {
						return false;
					}
				}
			}

			if (requestedOwner != 0) {
				var metadata = questCatalog().findMetadata(requestedOwner).orElse(null);
				if (metadata != null && "CHALLENGE_TASK".equals(metadata.category())
						&& player.getAccessLevel() > 0) {
					PacketSendUtility.sendMessage(player,
						"You're GM! So system won't apply countNextRepeatTime()");
					return true;
				} else if (metadata != null && "CHALLENGE_TASK".equals(metadata.category())
						&& player.getAccessLevel() == 0) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400855, 9));
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.dd0b8ceead0c", ex));
			return false;
		}
		return false;
	}

	/** Dispatches an accepted server-issued quest share without inventing an NPC interaction object. */
	public boolean onSharedQuestDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null || env.getQuestId() <= 0) {
			return false;
		}
		QuestProductionDispatcher typed = productionDispatcher;
		if (!typed.owns(env.getQuestId())) {
			return false;
		}
		return typed.dispatchSharedQuestAccept(env.getPlayer().getObjectId(), env.getQuestId(), env.getDialogId());
	}

	/**
	 * 分发击杀事件。
	 * Dispatch a kill event.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功（异常时 false） / Whether successful ({@code false} on error)
	 */
	public boolean onKill(QuestEnv env) {
		if (env == null || env.getPlayer() == null || !(env.getVisibleObject() instanceof Npc)) {
			return false;
		}
		try {
			Npc npc = (Npc) env.getVisibleObject();
			QuestEvent event = new QuestEvent.KillNpc(npc.getNpcId());
			QuestProductionDispatcher typed = productionDispatcher;
			List<Integer> questIds = getQuestNpc(npc.getNpcId()).getOnKillEvent();
			if (questIds.stream().anyMatch(typed::owns)) {
				try {
					typed.dispatch(event, env.getPlayer().getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed kill dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || !(env.getVisibleObject() instanceof Npc)) {
			return false;
		}
		try {
			Npc npc = (Npc) env.getVisibleObject();
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			List<Integer> questIds = getQuestNpc(npc.getNpcId()).getOnAttackEvent();
			if (player != null && questIds.stream().anyMatch(typed::owns)) {
				// 攻击事实广播给所有匹配 typed owner；同一 NPC 可服务多个任务。
				// Broadcast the authoritative attack fact to every matching typed owner.
				try {
					QuestNpcAttackFacts facts = attackFacts(npc, player);
					typed.dispatch(new QuestEvent.AttackNpc(npc.getNpcId(), facts),
						player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed attack dispatch is best-effort.
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.105680305f00", ex));
			return false;
		}
		return true;
	}

	private static QuestNpcAttackFacts attackFacts(Npc npc, Player player) {
		if (npc == null || player == null || npc.getLifeStats() == null
				|| player.getPosition() == null || player.getWorldId() <= 0 || player.getInstanceId() <= 0) {
			return null;
		}
		return new QuestNpcAttackFacts(player.getObjectId(), npc.getObjectId(), npc.getNpcId(),
			npc.getLifeStats().getCurrentHp(), npc.getLifeStats().getMaxHp(),
			player.getWorldId(), player.getInstanceId());
	}

	/**
	 * 分发升级事件（仅对未完成任务调用处理器）。
	 * Dispatch a level-up event (handlers only for incomplete quests).
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onLvlUp(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestProductionDispatcher typed = productionDispatcher;
			if (player != null) {
				try {
					typed.dispatch(new QuestEvent.LevelUp(), player.getObjectId(), 0,
						QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed level-up dispatch is best-effort.
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.b844c9346335", ex));
		}
	}

	/**
	 * 任务状态提交后只重新评估目录中显式依赖该任务的自动获取路由。
	 * Re-evaluates only catalog owners explicitly affected by a committed quest-state change.
	 */
	public void onQuestStateChanged(QuestEnv env) {
		if (env == null || env.getPlayer() == null || env.getQuestId() <= 0) {
			return;
		}
		try {
			productionDispatcher.dispatchQuestStateChanged(env.getPlayer().getObjectId(), env.getQuestId());
		} catch (RuntimeException ignored) {
			// Typed dependency refresh is best-effort, matching level-up refresh behavior.
		}
	}

	/**
	 * 分发区域任务结束事件。
	 * Dispatch a zone-mission-end event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onEnterZoneMissionEnd(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			if (player != null) {
				// Zone-mission completion has no authoritative quest owner: a single
				// preceding mission may unlock several typed follow-up owners. Broadcast
				// the fact so each definition can evaluate its own prerequisites.
				try {
					productionDispatcher.dispatch(new QuestEvent.ZoneMissionEnd(),
						player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed zone-mission-end dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env == null ? null : env.getPlayer();
			if (player != null && questOnDie.stream().anyMatch(typed::owns)) {
				// 死亡事实由正式 owner 广播消费；同一玩家可同时拥有多个死亡回退任务。
				// Broadcast the authoritative death fact to every matching typed owner.
				try {
					typed.dispatch(new QuestEvent.Die(), player.getObjectId(), 0,
						QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed death dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestProductionDispatcher typed = productionDispatcher;
			if (player != null) {
				// 分发到 typed owner：玩家登出（广播全部 log-out 路由）。
				// Dispatch to typed owners: player logged out (broadcast all log-out routes).
				try {
					typed.dispatch(new QuestEvent.LogOut(), player.getObjectId(), 0,
						QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed logout dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestProductionDispatcher typed = productionDispatcher;
			if (player != null) {
				// 分发到 typed owner：护送 NPC 到达目标（有 owner 则独占，否则广播）。
				// Dispatch to typed owners: escort NPC reached target (exclusive if owner is set, else broadcast).
				int owner = env.getQuestId();
				try {
					QuestDispatchContract contract = owner > 0
						? QuestDispatchContract.EXCLUSIVE
						: QuestDispatchContract.BROADCAST;
					typed.dispatch(new QuestEvent.NpcReachTarget(), player.getObjectId(), owner, contract);
				} catch (RuntimeException ignored) {
					// Typed escort dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestProductionDispatcher typed = productionDispatcher;
			if (player != null) {
				// 分发到 typed owner：护送 NPC 丢失目标（有 owner 则独占，否则广播）。
				// Dispatch to typed owners: escort NPC lost target (exclusive if owner is set, else broadcast).
				int owner = env.getQuestId();
				try {
					QuestDispatchContract contract = owner > 0
						? QuestDispatchContract.EXCLUSIVE
						: QuestDispatchContract.BROADCAST;
					typed.dispatch(new QuestEvent.NpcLostTarget(), player.getObjectId(), owner, contract);
				} catch (RuntimeException ignored) {
					// Typed escort dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			IntArrayList lists = getOnPassFlyingRingsQuests(FlyRing);
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env == null ? null : env.getPlayer();
			if (player != null) {
				boolean hasTypedOwner = false;
				for (int index = 0; index < lists.size(); index++) {
					if (typed.owns(lists.get(index))) {
						hasTypedOwner = true;
						break;
					}
				}
				if (hasTypedOwner) {
					// Movement facts are captured only after the server-side ring
					// handshake has succeeded; the typed dispatcher is authoritative
					// for every matching owner.
					try {
						typed.dispatch(runtimeComposition.movementEventPort()
							.passFlyingRing(env, FlyRing), player.getObjectId(), 0,
							QuestDispatchContract.BROADCAST);
					} catch (RuntimeException ignored) {
						// Typed flying-ring dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestProductionDispatcher typed = productionDispatcher;
			if (player != null) {
				try {
					typed.dispatch(new QuestEvent.EnterWorld(), player.getObjectId(), 0,
						QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed enter-world dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || item == null || item.getItemTemplate() == null) {
			return HandlerResult.FAILED;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			if (player != null) {
				int itemId = item.getItemTemplate().getTemplateId();
				OptionalInt itemPlayDuration = typed.itemPlayAnimationMillis(itemId);
				if (itemPlayDuration.isPresent()) {
					scheduleTypedItemPlay(player, item, itemPlayDuration.getAsInt());
					return HandlerResult.SUCCESS;
				}
				QuestEvent.UseItem event = new QuestEvent.UseItem(itemId,
					item.getObjectId());
				var typedResult = typed.dispatch(event, player.getObjectId(), 0,
					QuestDispatchContract.FIRST_NON_UNKNOWN);
				if (typedResult.owners().stream().anyMatch(owner -> owner.result() == QuestRouteResult.FAILED
						|| owner.result() == QuestRouteResult.BLOCKED)) {
					return HandlerResult.FAILED;
				}
				if (typedResult.consumed()) {
					return HandlerResult.SUCCESS;
				}
			}
			return HandlerResult.UNKNOWN;
		} catch (Exception ex) {
			// log.error(I18n.get("log.882dbd53a6cc", ex));
			return HandlerResult.FAILED;
		}
	}

	/**
	 * 调度带客户端使用动画的 typed item-play 事件。
	 * Schedules a typed item-play event with the client-side use animation.
	 *
	 * @param player 使用物品的玩家 / player using the item
	 * @param item 使用的物品 / used item
	 * @param animationMillis 动画时长 / animation duration
	 */
	private void scheduleTypedItemPlay(Player player, Item item, int animationMillis) {
		int playerId = player.getObjectId();
		int itemId = item.getItemTemplate().getTemplateId();
		int itemObjectId = item.getObjectId();
		PacketSendUtility.broadcastPacket(player,
			new SM_ITEM_USAGE_ANIMATION(playerId, itemObjectId, itemId, animationMillis, 0, 0), true);
		player.getController().scheduleTask(TaskId.ITEM_USE, () -> {
			if (player.isOnline()) {
				PacketSendUtility.broadcastPacket(player,
					new SM_ITEM_USAGE_ANIMATION(playerId, itemObjectId, itemId, 0, 1, 0), true);
			}
			if (!player.isOnline() || player.getInventory() == null) {
				return;
			}
			Item current = player.getInventory().getItemByObjId(itemObjectId);
			if (current == null || current.getItemTemplate().getTemplateId() != itemId
					|| current.getItemCount() <= 0) {
				return;
			}
			onItemPlayCompletedEvent(player, itemId);
		}, animationMillis);
	}

	/**
	 * 分发已由调用方成功完成动画和物品效果的 typed item-play 事件。
	 * Dispatches a typed item-play event after the caller has successfully completed the animation and item effect.
	 *
	 * @param player 使用物品的玩家 / player using the item
	 * @param itemId 物品模板 ID / item template ID
	 * @return 任务处理结果 / quest handling result
	 */
	public HandlerResult onItemPlayCompletedEvent(Player player, int itemId) {
		if (player == null || itemId <= 0) {
			return HandlerResult.FAILED;
		}
		try {
			OptionalInt animationMillis = productionDispatcher.itemPlayAnimationMillis(itemId);
			if (animationMillis.isEmpty()) {
				return HandlerResult.UNKNOWN;
			}
			var typedResult = productionDispatcher.dispatch(
				new QuestEvent.ItemPlay(itemId, animationMillis.getAsInt()), player.getObjectId(), 0,
				QuestDispatchContract.FIRST_NON_UNKNOWN);
			if (typedResult.owners().stream().anyMatch(owner -> owner.result() == QuestRouteResult.FAILED
					|| owner.result() == QuestRouteResult.BLOCKED)) {
				return HandlerResult.FAILED;
			}
			return typedResult.consumed() ? HandlerResult.SUCCESS : HandlerResult.UNKNOWN;
		} catch (Exception ex) {
			log.error(I18n.get("log.quest_engine.item_play_failed", player.getObjectId(), itemId), ex);
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
		if (env == null || env.getPlayer() == null || itemId <= 0 || itemObjectId < 0) {
			return false;
		}
		QuestProductionDispatcher typed = productionDispatcher;
		Player player = env == null ? null : env.getPlayer();
		if (player != null && itemId > 0 && typed.hasRoutes(new QuestEvent.HouseItemUse(itemId))) {
			try {
				QuestEvent.HouseItemUse event = runtimeComposition.housingEventPort()
					.houseItemUse(env, itemId, itemObjectId);
				typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed house-item dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || itemId <= 0) {
			return;
		}
		QuestProductionDispatcher typed = productionDispatcher;
		Player player = env == null ? null : env.getPlayer();
		List<Integer> questIds = questItems.get(itemId);
		if (player != null && itemId > 0 && questIds != null && questIds.stream().anyMatch(typed::owns)) {
			// 物品进入玩家背包后先进入正式 typed owner；同一物品可被多个
			// 任务监听，因此使用广播契约，不因一个 owner 的状态而截断其他 owner。
			// Route the obtain fact through typed owners first. The same item may
			// belong to multiple quests, so use the broadcast contract.
			try {
				typed.dispatch(new QuestEvent.GetItem(itemId), player.getObjectId(), 0,
					QuestDispatchContract.BROADCAST);
				long inventoryCount = player.getInventory() == null
					? 1L : player.getInventory().getItemCountByItemId(itemId);
				int collectedCount = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, inventoryCount));
				typed.dispatch(new QuestEvent.CollectItem(itemId, collectedCount),
					player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed obtain dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || playerRank == null || killer == null || creditSource == null) {
			return false;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player recipient = env == null ? null : env.getPlayer();
			if (recipient != null && playerRank != null
				&& typed.hasRoutes(new QuestEvent.KillRanked(playerRank.getId()))) {
				try {
					QuestEvent.KillRanked event = runtimeComposition.pvpEventPort()
						.killRanked(env, killer, playerRank.getId(), creditSource);
					typed.dispatch(event, recipient.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed kill-ranked dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || !(env.getVisibleObject() instanceof Player)
				|| worldId <= 0 || killer == null || creditSource == null) {
			return false;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player recipient = env == null ? null : env.getPlayer();
			Player victim = env != null && env.getVisibleObject() instanceof Player player ? player : null;
			if (recipient != null && victim != null && victim.getAbyssRank() != null
				&& victim.getAbyssRank().getRank() != null && worldId > 0
				&& typed.hasRoutes(new QuestEvent.KillInWorld(worldId))) {
				try {
					int victimRankId = victim.getAbyssRank().getRank().getId();
					QuestEvent.KillInWorld event = runtimeComposition.pvpEventPort()
						.killInWorld(env, killer, victimRankId, worldId, creditSource);
					typed.dispatch(event, recipient.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed kill-in-world dispatch is best-effort.
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.2fecf5cac390", ex));
			return false;
		}
		return true;
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
		if (env == null || env.getPlayer() == null || zoneName == null) {
			return false;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			if (player != null) {
				try {
					typed.dispatch(new QuestEvent.EnterZone(zoneName.name()),
						player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed enter-zone dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || zoneName == null) {
			return false;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			if (player != null) {
				try {
					typed.dispatch(new QuestEvent.LeaveZone(zoneName.name()),
						player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed leave-zone dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			Set<Integer> typedClaimedOwners = Set.of();
			if (player != null) {
				try {
					typedClaimedOwners = typed.dispatch(new QuestEvent.MovieEnd(movieId), player.getObjectId(), 0,
						QuestDispatchContract.EXCLUSIVE).claimedOwners();
				} catch (RuntimeException ignored) {
					// Typed movie-end dispatch is best-effort.
				}
			}
			return !typedClaimedOwners.isEmpty();
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		Player player = env.getPlayer();
		QuestProductionDispatcher typed = productionDispatcher;
		if (player != null && env.getQuestId() > 0) {
			// 分发到 typed owner：任务计时器结束（独占指定 owner）。
			// Dispatch to typed owners: quest timer ended (exclusive to the named owner).
			try {
				typed.dispatch(new QuestEvent.QuestTimerEnd(), player.getObjectId(), env.getQuestId(),
					QuestDispatchContract.EXCLUSIVE);
			} catch (RuntimeException ignored) {
				// Typed timer-end dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null) {
			return;
		}
		Player player = env.getPlayer();
		try {
			productionDispatcher.dispatch(new QuestEvent.InvisibleTimerEnd(), player.getObjectId(), 0,
				QuestDispatchContract.BROADCAST);
		} catch (RuntimeException ignored) {
			// Typed invisible-timer-end dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || skillId <= 0) {
			return false;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env == null ? null : env.getPlayer();
			if (player != null && skillId > 0 && typed.hasRoutes(new QuestEvent.UseSkill(skillId))) {
				try {
					QuestEvent.UseSkill event = runtimeComposition.skillEventPort().useSkill(env, skillId);
					typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed skill-use dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || itemId <= 0) {
			return;
		}
		Player player = env == null ? null : env.getPlayer();
		QuestProductionDispatcher typed = productionDispatcher;
		if (player != null && player.getInventory() != null
				&& player.getInventory().getItemCountByItemId(itemId) == 0) {
			try {
				typed.dispatch(new QuestEvent.FailCraft(itemId), player.getObjectId(), 0,
					QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed craft-fail dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || itemId <= 0) {
			return;
		}
		Player player = env == null ? null : env.getPlayer();
		QuestProductionDispatcher typed = productionDispatcher;
		if (player != null) {
			try {
				typed.dispatch(new QuestEvent.EquipItem(itemId),
					player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed equip-item dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || questActionType == null) {
			return false;
		}
		QuestProductionDispatcher typed = productionDispatcher;
		QuestEvent event = new QuestEvent.CanAct(templateId, questActionType.name());
		try {
			var result = typed.dispatch(event, env.getPlayer().getObjectId(), 0,
				QuestDispatchContract.EXCLUSIVE);
			if (result.claimed() && !result.handled()) {
				return false;
			}
			return !result.handledOwners().isEmpty();
		} catch (RuntimeException ignored) {
			// Typed can-act dispatch is best-effort.
			return false;
		}
	}

	/**
	 * 分发挖掘号奖励事件。
	 * Dispatch a Dredgion reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onDredgionReward(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		QuestProductionDispatcher typed = productionDispatcher;
		Player player = env == null ? null : env.getPlayer();
		if (player != null && typed.hasRoutes(new QuestEvent.DredgionReward())) {
			try {
				QuestEvent.DredgionReward event = runtimeComposition.pvpInstanceEventPort().dredgionReward(env);
				typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed dredgion-reward dispatch is best-effort.
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
	}

	/**
	 * 分发欧菲丹奖励事件。
	 * Dispatch an Ophidan reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onOphidanReward(QuestEnv env) {
	}

	/**
	 * 分发堡垒奖励事件。
	 * Dispatch a Bastion reward event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onBastionReward(QuestEnv env) {
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
		if (env == null || env.getPlayer() == null || bonusType == null) {
			return HandlerResult.FAILED;
		}
		try {
			Player player = env.getPlayer();
			QuestProductionDispatcher typed = productionDispatcher;
			if (player != null) {
				// 分发到 typed owner：按 bonus-type 广播，声明了该类型的 owner 自行以条件决定是否匹配。
				// Dispatch to typed owners: broadcast by bonus type; owners decide with their own conditions.
				try {
					Integer questId = env.getQuestId();
					typed.dispatch(new QuestEvent.BonusApply(bonusType.name()),
						player.getObjectId(), questId == null ? 0 : questId,
						QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed bonus-apply dispatch is best-effort.
				}
			}
			return HandlerResult.UNKNOWN;
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
		if (env == null || !(env.getVisibleObject() instanceof Npc npc) || env.getPlayer() == null) {
			return false;
		}
		if (!questNpcs.containsKey(npc.getNpcId())) {
			return false;
		}
		QuestNpc questNpc = getQuestNpc(npc.getNpcId());
		if (questNpc.getOnDistanceEvent().size() == 0) {
			return false;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			if (questNpc.getOnDistanceEvent().stream().anyMatch(typed::owns)) {
				try {
					typed.dispatch(runtimeComposition.proximityEventPort()
						.atDistance(env, npc.getNpcId()), env.getPlayer().getObjectId(), 0,
						QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed proximity dispatch is best-effort.
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
		if (env == null || env.getPlayer() == null || teleportId <= 0) {
			return;
		}
		try {
			QuestProductionDispatcher typed = productionDispatcher;
			Player player = env == null ? null : env.getPlayer();
			if (player != null && typed.hasRoutes(new QuestEvent.EnterWindStream(teleportId))) {
				try {
					typed.dispatch(runtimeComposition.movementEventPort()
						.enterWindStream(env, teleportId), player.getObjectId(), 0,
						QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed wind-stream dispatch is best-effort.
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
	}

	/**
	 * 分发创造力点数事件。
	 * Dispatch a creativity-point event.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public void onCreativityPoint(QuestEnv env) {
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
		} else if (!questOnKillInWorld.get(worldId).contains(questId)) {
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
	 * 是否已有该任务的处理器。
	 * Whether a handler is registered for the quest.
	 *
	 * Quest id
	 * Whether present
	 */
	public boolean isHaveHandler(int questId) {
		return productionDispatcher.owns(questId);
	}

	/** 返回正式 typed catalog 是否为该任务的权威 owner。 Returns whether the live typed catalog is authoritative. */
	public boolean isProductionOwner(int questId) {
		return productionDispatcher.owns(questId);
	}

	/** 返回指定 typed owner 是否声明了放弃过渡。 Returns whether the typed owner declares abandon routing. */
	public boolean hasProductionAbandonRoute(int questId) {
		return productionDispatcher.hasRoutes(new QuestEvent.Abandon(), questId);
	}

	/** 将放弃清理分发给指定 typed owner，禁止回退 legacy。 Dispatches abandon cleanup without legacy fallback. */
	public boolean onAbandon(Player player, int questId) {
		if (player == null || questId <= 0 || !productionDispatcher.owns(questId)) {
			return false;
		}
		return productionDispatcher.dispatch(new QuestEvent.Abandon(), player.getObjectId(), questId,
			QuestDispatchContract.EXCLUSIVE).handled();
	}

	/** 从显式 production catalog 加载已通过 owner 审核的 typed 定义。 */
	private QuestCatalog loadProductionCatalog() throws Exception {
		return QuestDefinitionCatalogManifest.compile(
			Config.dataFile("./data/static_data/quest_definition").toPath());
	}

	/**
	 * 在配置就绪后尽早编译 raw catalog，使其解析时间与静态数据加载重叠。
	 * Compile the raw catalog as soon as configuration is ready so parsing overlaps static-data loading.
	 */
	public synchronized void preloadProductionCatalog() {
		preloadProductionCatalog(this::loadProductionCatalogForPreload);
	}

	synchronized void preloadProductionCatalog(Supplier<QuestCatalog> catalogSupplier) {
		if (productionCatalogPreload != null) {
			return;
		}
		CompletableFuture<QuestCatalog> future = new CompletableFuture<>();
		productionCatalogPreload = future;
		CompletableFuture.runAsync(() -> {
			try {
				future.complete(catalogSupplier.get());
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
	}

	private QuestCatalog loadProductionCatalogForPreload() {
		try {
			return loadProductionCatalog();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Can't compile typed quest catalog.", e);
		}
	}

	/**
	 * 等待预编译结果；未启用预加载时保持原有同步编译路径。
	 * Await the precompiled result; fall back to synchronous compilation when preload was not armed.
	 */
	synchronized QuestCatalog awaitProductionCatalogPreload() throws Exception {
		Future<QuestCatalog> future = productionCatalogPreload;
		productionCatalogPreload = null;
		if (future == null) {
			return loadProductionCatalog();
		}
		try {
			return future.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception exception) {
				throw exception;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw e;
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
	PreparedProductionDefinitions prepareProductionDefinitions(QuestCatalog catalog) {
		QuestCatalogRegistry registry = catalog instanceof QuestCatalogRegistry existing
			? existing : new QuestCatalogRegistry(catalog);
		QuestRuntimeComposition snapshotComposition = QuestRuntimeComposition.production(registry);
		QuestProductionDispatcher dispatcher = QuestProductionDispatcher.production(registry, snapshotComposition);
		QuestInteractionObjectValidator.validate(dispatcher, templateId -> {
			if (DataManager.NPC_DATA == null) {
				return null;
			}
			var template = DataManager.NPC_DATA.getNpcTemplate(templateId);
			return template == null ? null : template.getAi();
		});
		snapshotComposition.installBroadcastPort(new PlayerQuestBroadcastPort(
			playerId -> com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId),
			(player, questIds) -> dispatcher.dispatchOwners(new QuestEvent.ZoneMissionEnd(), player.getObjectId(),
				questIds, QuestDispatchContract.EXCLUSIVE),
			(player, questIds) -> dispatcher.dispatchOwners(new QuestEvent.EventQuestRefresh(), player.getObjectId(),
				questIds, QuestDispatchContract.EXCLUSIVE)));
		for (CompiledQuestDefinition definition : registry.executables()) {
			for (var transition : definition.definition().transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc)
						&& !(transition.event() instanceof QuestEvent.KillNpc)
						&& !(transition.event() instanceof QuestEvent.KillNpcSet)
						&& !(transition.event() instanceof QuestEvent.AttackNpc)
						&& !(transition.event() instanceof QuestEvent.CanAct)
						&& !(transition.event() instanceof QuestEvent.EnterZone)
						&& !(transition.event() instanceof QuestEvent.LevelUp)
						&& !(transition.event() instanceof QuestEvent.EnterWorld)
						&& !(transition.event() instanceof QuestEvent.UseItem)
						&& !(transition.event() instanceof QuestEvent.ItemPlay)
						&& !(transition.event() instanceof QuestEvent.GetItem)
						&& !(transition.event() instanceof QuestEvent.CollectItem)
						&& !(transition.event() instanceof QuestEvent.PassFlyingRing)
						&& !(transition.event() instanceof QuestEvent.EnterWindStream)
						&& !(transition.event() instanceof QuestEvent.AtDistance)
						&& !(transition.event() instanceof QuestEvent.Die)
						&& !(transition.event() instanceof QuestEvent.LogOut)
						&& !(transition.event() instanceof QuestEvent.MovieEnd)
						&& !(transition.event() instanceof QuestEvent.NpcReachTarget)
						&& !(transition.event() instanceof QuestEvent.NpcLostTarget)
						&& !(transition.event() instanceof QuestEvent.ZoneMissionEnd)
						&& !(transition.event() instanceof QuestEvent.EventQuestRefresh)
							&& !(transition.event() instanceof QuestEvent.InvisibleTimerEnd)
							&& !(transition.event() instanceof QuestEvent.FailCraft)
							&& !(transition.event() instanceof QuestEvent.EquipItem)
							&& !(transition.event() instanceof QuestEvent.Abandon)
							&& !(transition.event() instanceof QuestEvent.DredgionReward)
							&& !(transition.event() instanceof QuestEvent.HouseItemUse)
							&& !(transition.event() instanceof QuestEvent.KillInWorld)
							&& !(transition.event() instanceof QuestEvent.KillRanked)
							&& !(transition.event() instanceof QuestEvent.LeaveZone)
							&& !(transition.event() instanceof QuestEvent.QuestTimerEnd)
							&& !(transition.event() instanceof QuestEvent.UseSkill)
							&& !(transition.event() instanceof QuestEvent.QuestDialog)
							&& !(transition.event() instanceof QuestEvent.BonusApply)) {
					throw new IllegalStateException("typed production event is not wired into QuestEngine: "
						+ transition.event().type());
				}
			}
			if (definition.definition().transitions().stream()
					.map(com.aionemu.gameserver.questEngine.definition.QuestTransition::event)
					.filter(QuestEvent.ItemPlay.class::isInstance)
					.map(QuestEvent.ItemPlay.class::cast)
					.mapToInt(QuestEvent.ItemPlay::itemId)
					.distinct()
					.anyMatch(itemId -> dispatcher.itemPlayAnimationMillis(itemId).isEmpty())) {
				throw new IllegalStateException("typed item-play event has no indexed route");
			}
		}
		return new PreparedProductionDefinitions(registry, dispatcher);
	}

	void installProductionDefinitions(QuestCatalog catalog) {
		installProductionDefinitions(prepareProductionDefinitions(catalog));
	}

	private void installProductionDefinitions(PreparedProductionDefinitions prepared) {
		QuestCatalogRegistry catalog = prepared.catalog();
		QuestProductionDispatcher dispatcher = prepared.dispatcher();
		for (CompiledQuestDefinition definition : catalog.executables()) {
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
					} else if (transition.event() instanceof QuestEvent.KillNpcSet kills) {
						for (int npcId : kills.npcIds()) {
							registerQuestNpc(npcId).addOnKillEvent(definition.id());
						}
				} else if (transition.event() instanceof QuestEvent.AttackNpc attack) {
					registerQuestNpc(attack.npcId()).addOnAttackEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.CanAct canAct) {
					registerCanAct(definition.id(), canAct.templateId());
				} else if (transition.event() instanceof QuestEvent.UseItem use) {
					registerQuestItem(use.itemId(), definition.id());
				} else if (transition.event() instanceof QuestEvent.GetItem get) {
					// Keep the obtain-event index in sync with the typed catalog. The
					// runtime dispatcher remains authoritative; this index only lets
					// the legacy loop skip typed owners safely.
					registerGetingItem(get.itemId(), definition.id());
				} else if (transition.event() instanceof QuestEvent.CollectItem collect) {
					// Collection routes share the item-obtain ingress while retaining
					// their own event type for count matching.
					registerGetingItem(collect.itemId(), definition.id());
				} else if (transition.event() instanceof QuestEvent.PassFlyingRing ring) {
					registerOnPassFlyingRings(ring.ring(), definition.id());
				} else if (transition.event() instanceof QuestEvent.EnterWindStream) {
					registerOnEnterWindStream(definition.id());
				} else if (transition.event() instanceof QuestEvent.AtDistance atDistance) {
					registerQuestNpc(atDistance.npcId()).addOnAtDistanceEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.Die) {
					registerOnDie(definition.id());
				} else if (transition.event() instanceof QuestEvent.LogOut) {
					registerOnLogOut(definition.id());
				} else if (transition.event() instanceof QuestEvent.NpcReachTarget) {
					registerAddOnReachTargetEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.NpcLostTarget) {
					registerAddOnLostTargetEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.MovieEnd movie) {
					registerOnMovieEndQuest(movie.movieId(), definition.id());
				} else if (transition.event() instanceof QuestEvent.ZoneMissionEnd) {
					registerOnEnterZoneMissionEnd(definition.id());
				} else if (transition.event() instanceof QuestEvent.InvisibleTimerEnd) {
					registerOnInvisibleTimerEnd(definition.id());
				} else if (transition.event() instanceof QuestEvent.EquipItem equip) {
					registerOnEquipItem(equip.itemId(), definition.id());
				}
			}
		}
		productionDispatcher = dispatcher;
	}

	/** Compiles and validates the complete canonical catalog without changing the live dispatcher. */
	public PreparedProductionDefinitions prepareProductionDefinitions() {
		try {
			return prepareProductionDefinitions(loadProductionCatalog());
		} catch (Exception e) {
			throw new GameServerError("Can't prepare typed quest catalog.", e);
		}
	}

	/** Returns the exact catalog/dispatcher pair currently published. */
	public PreparedProductionDefinitions currentProductionDefinitions() {
		QuestProductionDispatcher dispatcher = productionDispatcher;
		return new PreparedProductionDefinitions(dispatcher.catalogRegistry(), dispatcher);
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
		QuestService.addHandlerSideQuestDrop(handlerSideDrop(questId, npcId, itemId, amount, chance, 0));
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
		QuestService.addHandlerSideQuestDrop(handlerSideDrop(questId, npcId, itemId, amount, chance, step));
	}

	private QuestCatalogDrop handlerSideDrop(int questId, int npcId, int itemId, int amount, int chance, int step) {
		var metadata = questCatalog().findMetadata(questId);
		QuestDropScope scope = metadata.stream().flatMap(value -> value.drops().stream())
			.filter(drop -> drop.npcId() == npcId && drop.itemId() == itemId)
			.map(com.aionemu.gameserver.questEngine.definition.QuestDrop::scope)
			.findFirst().orElse(QuestDropScope.NONE);
		return new QuestCatalogDrop(questId, npcId, itemId, chance, scope, step, amount, metadata);
	}

	/**
	 * 启动时装载：注册掉落、加载脚本处理器与 XML 任务，并启动每日提醒。
	 * Bootstrap load: register drops, load script handlers and XML quests, start daily reminders.
	 *
	 * @param progressLatch 进度闩锁（可空） / Progress latch (nullable)
	 */
	public void load(CountDownLatch progressLatch) {
		load(progressLatch, null);
	}

	/** Loads the typed production catalog using an already compiled and validated typed snapshot. */
	public void load(CountDownLatch progressLatch, PreparedProductionDefinitions prepared) {
		log.info(I18n.get("log.5359e35f8f99"));
		try {
			installProductionDefinitions(prepared == null
					? prepareProductionDefinitions(awaitProductionCatalogPreload()) : prepared);
			log.info(I18n.get("log.quest_engine.typed_owners_loaded", productionDispatcher.owners().size()));
		} catch (Exception e) {
			throw new GameServerError("Can't initialize typed quest engine.", e);
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
						var metadata = qs == null ? null : questCatalog().findMetadata(qs.getQuestId()).orElse(null);
						if (qs != null && qs.canRepeat(metadata)) {
							if (metadata.repeatPolicy().daily()) {
								player.getController().updateZone();
								player.getController().updateNearbyQuests();
								PacketSendUtility.sendPacket(player, dailyMessage);
							} else if (metadata.repeatPolicy().weekly()) {
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
		productionCatalogPreload = null;
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
		questOnKillInWorld.clear();
		onInvisibleTimerEnd.clear();
		questOnUseSkill.clear();
		reachTarget.clear();
		lostTarget.clear();
		questOnEnterWindStream.clear();
		questRideAction.clear();
		questOnCreativityPoint.clear();
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
