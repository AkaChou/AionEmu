package com.aionemu.gameserver.questEngine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import lombok.Setter;
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
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestDropScope;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNpcAttackFacts;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.tablelane.HtmlPagesRegistry;
import com.aionemu.gameserver.questEngine.tablelane.CameraRegistry;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestBroadcastPort;
import com.aionemu.gameserver.questEngine.runtime.PlayerSerialExecutor;
import com.aionemu.gameserver.questEngine.runtime.QuestDispatchContract;
import com.aionemu.gameserver.questEngine.runtime.QuestExecutionCoordinator;
import com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectValidator;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionEventWiring;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeRouter;
import com.aionemu.gameserver.questEngine.runtime.QuestRouteResult;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeComposition;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.zone.ZoneName;

import lombok.extern.slf4j.Slf4j;

/**
 * 任务引擎单例：维护事件注册表，并向已注册处理器分发各类游戏事件。
 * Central quest-engine singleton that maintains event registries and dispatches
 * game events to registered processors.
 */
@Slf4j
public class QuestEngine implements GameEngine {
	/** Spring ObjectProvider 覆盖钩子 / Spring ObjectProvider override hook
	 * -- SETTER --
	 *  设置 Spring ObjectProvider 覆盖点。
	 *  Install a Spring ObjectProvider override.
	 *  Spring provider
	 */
	@Setter
	private static volatile ObjectProvider<QuestEngine> instanceProvider;
	/** NPC 关联任务索引 / NPC-related quest index */
	private final IntObjectHashMap<QuestNpc> questNpcs = new IntObjectHashMap<>();
	/** 物品使用关联任务 / Item-use related quests */
	private final IntObjectHashMap<IntArrayList> questItemRelated = new IntObjectHashMap<>();
	/** 房屋物品关联任务 / House-item related quests */
	private final IntObjectHashMap<IntArrayList> questHouseItems = new IntObjectHashMap<>();
	/** 获得物品关联任务 / Item-obtain related quests */
	private final IntObjectHashMap<IntArrayList> questItems = new IntObjectHashMap<>();
	/** 区域任务结束监听列表 / Zone-mission-end listeners */
	private final IntArrayList questOnEnterZoneMissionEnd = new IntArrayList();
	/** 升级监听列表 / Level-up listeners */
	private final IntArrayList questOnLevelUp = new IntArrayList();
	/** 死亡监听列表 / Death listeners */
	private final IntArrayList questOnDie = new IntArrayList();
	/** 登出监听列表 / Logout listeners */
	private final IntArrayList questOnLogOut = new IntArrayList();
	/** 进入世界监听列表 / Enter-world listeners */
	private final IntArrayList questOnEnterWorld = new IntArrayList();
	/** 进入区域监听 / Enter-zone listeners */
	private final Map<ZoneName, IntArrayList> questOnEnterZone = new LinkedHashMap<>();
	/** 离开区域监听 / Leave-zone listeners */
	private final Map<ZoneName, IntArrayList> questOnLeaveZone = new LinkedHashMap<>();
	/** 穿过飞行环监听 / Pass-flying-ring listeners */
	private final Map<String, IntArrayList> questOnPassFlyingRings = new LinkedHashMap<>();
	/** 动画结束监听 / Movie-end listeners */
	private final IntObjectHashMap<IntArrayList> questOnMovieEnd = new IntObjectHashMap<>();
	/** 计时器结束监听 / Timer-end listeners */
	private final List<Integer> questOnTimerEnd = new ArrayList<>();
	/** 隐形计时器结束监听 / Invisible-timer-end listeners */
	private final List<Integer> onInvisibleTimerEnd = new ArrayList<>();
	/** 击杀军衔玩家监听 / Kill-ranked listeners */
	private final Map<AbyssRankEnum, IntArrayList> questOnKillRanked = new LinkedHashMap<>();
	/** 世界内击杀监听 / Kill-in-world listeners */
	private final Map<Integer, IntArrayList> questOnKillInWorld = new LinkedHashMap<>();
	/** 使用技能监听 / Skill-use listeners */
	private final IntObjectHashMap<IntArrayList> questOnUseSkill = new IntObjectHashMap<>();
	/** 制作失败监听 / Fail-craft listeners */
	private final Map<Integer, Integer> questOnFailCraft = new HashMap<>();
	/** 装备物品监听 / Equip-item listeners */
	private final Map<Integer, Set<Integer>> questOnEquipItem = new HashMap<>();
	/** 每日/周任务提醒定时任务 / Daily/weekly reminder scheduled task */
	private ScheduledFuture<?> messageSendingTask;
	/** Fully composed production ports used by typed quest execution. */
	private final QuestRuntimeComposition runtimeComposition = QuestRuntimeComposition.production();
	/** Live XML/retail runtime router; each child owns a disjoint catalog. */
	private volatile QuestRuntimeDispatcher productionDispatcher = QuestProductionDispatcher.disabled();
	/** Raw catalog compiled in parallel with other startup work; cleared once consumed. */
	private volatile Future<QuestCatalog> productionCatalogPreload;

	/** Fully validated immutable typed runtime waiting to be published. */
	public record PreparedProductionDefinitions(QuestCatalogRegistry catalog,
			QuestRuntimeDispatcher dispatcher) {
		public PreparedProductionDefinitions {
			java.util.Objects.requireNonNull(catalog, "catalog");
			java.util.Objects.requireNonNull(dispatcher, "dispatcher");
		}
	}
	/** 可行动作监听 / Can-act listeners */
	private final IntObjectHashMap<IntArrayList> questCanAct = new IntObjectHashMap<>();
	/** 挖掘号奖励监听 / Dredgion reward listeners */
	private final List<Integer> questOnDredgionReward = new ArrayList<>();
	/** 卡玛尔奖励监听 / Kamar reward listeners */
	private final List<Integer> questOnKamarReward = new ArrayList<>();
	/** 欧菲丹奖励监听 / Ophidan reward listeners */
	private final List<Integer> questOnOphidanReward = new ArrayList<>();
	/** 堡垒奖励监听 / Bastion reward listeners */
	private final List<Integer> questOnBastionReward = new ArrayList<>();
	/** 奖励加成监听 / Bonus-apply listeners */
	private final Map<BonusType, IntArrayList> questOnBonusApply = new LinkedHashMap<>();
	/** 跟随到达目标监听 / Reach-target listeners */
	private final IntArrayList reachTarget = new IntArrayList();
	/** 跟随丢失目标监听 / Lost-target listeners */
	private final IntArrayList lostTarget = new IntArrayList();
	/** 进入风道监听 / Enter-windstream listeners */
	private final IntArrayList questOnEnterWindStream = new IntArrayList();
	/** 骑乘动作监听 / Ride-action listeners */
	private final IntArrayList questRideAction = new IntArrayList();
	/** 创造力点数监听 / Creativity-point listeners */
	private final IntArrayList questOnCreativityPoint = new IntArrayList();

	/**
	 * 创建任务引擎实例。
	 * Create a quest-engine instance.
	 */
	public QuestEngine() {
	}

	/** 返回指定 owner 是否拥有实际匹配事件的路由。 / Return whether the owner has a route matching the event. */
	public boolean hasMatchingRoutes(QuestEvent event, int questId) {
		if (isNativeOwner(questId)) {
			return true;
		}
		return productionDispatcher.hasMatchingRoutes(event, questId);
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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final QuestEngine getInstance() {
		ObjectProvider<QuestEngine> provider = instanceProvider;
		QuestEngine provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("QuestEngine 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 分发 NPC 对话事件；questId 为 0 时按 NPC 上注册的谈话任务依次尝试。
	 * 客户端 NPC 对话选择没有任务上下文时，由 CM_DIALOG_SELECT 先按普通对话分流；
	 * 这里的 questId==0 派发保留给任务交互物 AI 等仍需要 owner 路由的入口。
	 * Dispatch an NPC dialog event; when questId is 0, try talk-quests registered on the NPC.
	 * CM_DIALOG_SELECT routes client NPC selections without quest context to a plain dialog first;
	 * this questId==0 dispatch remains for interaction-object AI and other callers that still need owner routing.
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
			QuestRuntimeDispatcher typed = productionDispatcher;
			// 真端表驱动车道：SimpleHunt 任务直接由原生处理器驱动，不走 IR 节点状态机
			if (requestedOwner != 0 && SimpleHuntHandler.instance().routes(requestedOwner)) {
				if (SimpleHuntHandler.instance().onDialog(env)) {
					return true;
				}
			}
			if (requestedOwner != 0 && SimpleSerialHuntHandler.instance().routes(requestedOwner)) {
				if (SimpleSerialHuntHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 真端表驱动车道：SimpleTalk 任务由原生处理器直驱（cab520 接取 / cabb10 中继与报告）
			if (requestedOwner != 0 && SimpleTalkHandler.instance().routes(requestedOwner)) {
				if (SimpleTalkHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 真端表驱动车道：SimpleCollectItem 采集对象/交付 NPC 由原生处理器直驱（P4 切换批）
			if (requestedOwner != 0 && SimpleCollectItemHandler.instance().routes(requestedOwner)) {
				if (SimpleCollectItemHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 真端表驱动车道：SimpleUseItem 用物接取后的无主对话/中继/交付由原生处理器直驱（P5 切换批）
			if (requestedOwner != 0 && SimpleUseItemHandler.instance().routes(requestedOwner)) {
				if (SimpleUseItemHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 真端表驱动车道：SimpleItemPlay 接取/交付预览/领奖由原生处理器直驱（P5 切换批）
			if (requestedOwner != 0 && SimpleItemPlayHandler.instance().routes(requestedOwner)) {
				if (SimpleItemPlayHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 真端表驱动车道：CombineTask 接取（发分量 + 学配方）/ 交付（产物门 + 回收分量）/ 领奖由
			// 原生处理器直驱（P6 切换批）。
			// The CombineTask lane (accept with component grants and recipe learn, product-gated
			// hand-in with component recycling, reward-window settlement) is driven natively.
			if (requestedOwner != 0 && SimpleCombineTaskHandler.instance().routes(requestedOwner)) {
				if (SimpleCombineTaskHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 真端表驱动车道：DataDriven Talk / CollectItem 步的共享对话平面（开页 + 页动作/1009/1008，
			// `FUN_180c474b0`）与 TalkFOBJ 组计数（本批路由集为空 ⇒ 恒 false）。
			// DataDriven dialog plane and TalkFOBJ groups: no-op until the atomic switch batch.
			if (npcId != 0 && DataDrivenNativeRuntime.instance().onDialog(player, npcId, env.getDialogId(),
				npc.getObjectId(), requestedOwner)) {
				return true;
			}
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
				// 奖励窗口的确认动作（SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD）由全局 UI 发出，
				// 客户端携带的交互对象可能不是完成路由绑定的报告 NPC；严格绑定未命中时按 questId + action
				// 恢复 owner 内唯一的 REWARD -> COMPLETE 路由，避免动作 ID 被当作页面 ID 回显而 load fail。
				// Reward-window confirmations come from global UI and may carry an interaction object that is
				// not the completion route's report NPC; on a strict miss, recover the owner's unique
				// REWARD -> COMPLETE route by questId + action instead of echoing the action id as a page id.
				if (event instanceof QuestEvent.TalkToNpc talk
						&& QuestDialogAction.isRewardWindowAction(env.getDialogId())
						&& typed.dispatchRewardWindowAction(talk, player.getObjectId(), requestedOwner)) {
					env.setQuestId(requestedOwner);
					log.debug(I18n.get("log.quest_engine.reward_window_unpinned_recovery",
						player.getName(), npc.getNpcId(), requestedOwner, env.getDialogId()));
					return true;
				}
				// A failed typed owner still owns this exclusive route, but the interaction failed.
				// Never report it as successful and never replay the retired owner.
				return false;
			}

			if (requestedOwner == 0 && npcId != 0) {
				QuestEvent event = new QuestEvent.TalkToNpc(npcId, env.getDialogId(), npc.getObjectId());
				// 客户端直接交互采集对象（questId==0 入口）：native 采集族先手匹配（单一 owner 不变量：
				// 该行已切 native，typed 目录里没有它的路由）。
				// Direct client interaction with a collect object (questId==0 entry): the native collect
				// family matches first (single-owner invariant: switched rows carry no typed route).
				for (SimpleCollectItemHandler.CollectTargetRef ref
						: SimpleCollectItemHandler.instance().targetsForNpc(npcId)) {
					if (!SimpleCollectItemHandler.instance().routes(ref.questId())) {
						continue;
					}
					env.setQuestId(ref.questId());
					return true;
				}
				// 参考 legacy 引擎：当调用方确实提供了 questId==0 的任务对话入口（交互物 AI 等）时，
				// 按 NPC 任务顺序逐个尝试，让第一个真正处理该动作的 owner 胜出。
				// 客户端 NPC 对话选择没有任务上下文时已在 CM_DIALOG_SELECT 按普通对话处理，不会走到这里。
				// Legacy-engine parity: when the caller really provides a questId==0 quest-dialog entry
				// (interaction-object AI and similar), try the NPC's quests in order and let the first
				// owner that actually handles the action win. Client NPC selections without quest context
				// are already handled as plain dialogs by CM_DIALOG_SELECT and never reach this path.
				for (int candidateId : npcDialogDispatchOwners(player, npc, event)) {
					var result = typed.dispatch(event, player.getObjectId(), candidateId,
						QuestDispatchContract.EXCLUSIVE);
					if (result.handled()) {
						env.setQuestId(candidateId);
						return true;
					}
					if (result.claimed()) {
						return false;
					}
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
			log.error(I18n.get("log.dd0b8ceead0c"), ex);
			return false;
		}
		return false;
	}

	/**
	 * 返回 questId==0 对话的派发顺序：先客户端可见/进行中/已授权的 owner，再其余匹配 owner。
	 * Returns the questId==0 dialog dispatch order: client-visible, live, or authorized owners first,
	 * then every remaining match.
	 * <p>与 legacy 引擎一致，逐个尝试候选，第一个真正处理该动作的 owner 胜出；未接取的普通任务
	 * 只是排在可见 owner 之后，仍会被尝试，因此不会出现“点了没反应”。</p>
	 * <p>Legacy-engine parity: candidates are tried in order and the first owner that actually handles
	 * the action wins. Unaccepted normal quests are only ordered after the visible owners; they are
	 * still tried, so a dialog click can never dead-end.</p>
	 * @param player 玩家 / player
	 * @param npc 对话 NPC / dialog NPC
	 * @param event 客户端动作事件 / client action event
	 * @return 派发顺序 / dispatch order
	 */
	List<Integer> npcDialogDispatchOwners(Player player, Npc npc, QuestEvent event) {
		QuestRuntimeDispatcher typed = productionDispatcher;
		List<Integer> preferred = new ArrayList<>();
		List<Integer> remaining = new ArrayList<>();
		// 候选来自正式 catalog 并通过事件键过滤，因此不依赖 questNpcs 索引是否已装载。
		// Candidates come from the production catalog and are filtered by the event key, so this does
		// not depend on the questNpcs index being installed.
		for (int candidateId : typed.owners()) {
			if (!typed.hasMatchingRoutes(event, candidateId)) {
				continue;
			}
			(isClientVisibleNpcDialogOwner(player, npc, candidateId) ? preferred : remaining)
				.add(candidateId);
		}
		preferred.addAll(remaining);
		return preferred;
	}

	/**
	 * 判断 typed owner 在客户端关闭普通任务标记时是否可见/可进入，用于决定派发优先级。
	 * Determines whether a typed owner is visible/enterable while the normal-quest marker is disabled;
	 * this decides the dispatch preference only.
	 * <p>非普通任务类别（IMPORTANT/MISSION 等）客户端始终可见；进行中或已由任务列表行选择的任务
	 * 同样优先。未接取的普通任务排在它们之后，但依然会被尝试。</p>
	 * <p>Other categories (IMPORTANT/MISSION and so on) stay visible in the client, and a live or
	 * row-selected quest is preferred as well. Unaccepted normal quests are only ordered after them and
	 * are still tried.</p>
	 * @param player 玩家 / player
	 * @param npc 对话 NPC / dialog NPC
	 * @param questId 候选任务 ID / candidate quest id
	 * @return 是否可见/可进入 / whether the owner stays visible or enterable
	 */
	private boolean isClientVisibleNpcDialogOwner(Player player, Npc npc, int questId) {
		QuestRuntimeDispatcher typed = productionDispatcher;
		if (!typed.owns(questId)) {
			return false;
		}
		if (player.hasNpcQuestDialogSelection(npc.getObjectId(), questId)) {
			return true;
		}
		QuestMetadata metadata = typed.catalogRegistry().findMetadata(questId).orElse(null);
		if (metadata != null && !"QUEST".equals(metadata.category())) {
			return true;
		}
		QuestState questState = player.getQuestStateList().getQuestState(questId);
		return questState != null && (questState.getStatus() == QuestStatus.START
			|| questState.getStatus() == QuestStatus.REWARD);
	}

	/** Dispatches an accepted server-issued quest share without inventing an NPC interaction object. */
	public boolean onSharedQuestDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null || env.getQuestId() <= 0) {
			return false;
		}
		QuestRuntimeDispatcher typed = productionDispatcher;
		if (!typed.owns(env.getQuestId())) {
			return false;
		}
		return typed.dispatchSharedQuestAccept(env.getPlayer().getObjectId(), env.getQuestId(), env.getDialogId());
	}

	/**
	 * 分发击杀事件。
	 * Dispatch a kill event.
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功（异常时 false） / Whether successful ({@code false} on error)
	 */
	public boolean onKill(QuestEnv env) {
		if (env == null || env.getPlayer() == null || !(env.getVisibleObject() instanceof Npc npc)) {
			return false;
		}
		// 真端表驱动车道：SimpleHunt 怪物击杀由原生相机直接推进，不走 IR 边分派
		if (SimpleHuntHandler.instance().onKill(env.getPlayer(), npc.getNpcId())) {
			return true;
		}
		if (SimpleSerialHuntHandler.instance().onKill(env.getPlayer(), npc.getNpcId())) {
			return true;
		}
		// 真端表驱动车道：SimpleCollectItem 采集怪击杀推进同一相机（掉落仍由掉落族发放）。
		if (SimpleCollectItemHandler.instance().onKill(env.getPlayer(), npc.getNpcId())) {
			return true;
		}
		// 真端表驱动车道：DataDriven Hunt 步组计数（带真端 50m 距离门——处理函数前奏 case 0，
		// def+0x70 恒 0，见 DataDrivenNativeRuntime.RETAIL_KILL_DISTANCE_SQ 取证链）。
		// DataDriven hunt steps with the retail 50 m distance gate (handler-preamble case 0).
		if (DataDrivenNativeRuntime.instance().onKill(env.getPlayer(), npc)) {
			return true;
		}
		try {
			QuestEvent event = new QuestEvent.KillNpc(npc.getNpcId());
			QuestRuntimeDispatcher typed = productionDispatcher;
			List<Integer> questIds = getQuestNpc(npc.getNpcId()).getOnKillEvent();
			if (questIds.stream().anyMatch(typed::owns)) {
				try {
					typed.dispatch(event, env.getPlayer().getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed kill dispatch is best-effort.
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.59f50c2b1e29"), ex);
			return false;
		}
		return true;
	}

	/**
	 * 分发攻击事件。
	 * Dispatch an attack event.
	 * @param env 任务环境 / Quest environment
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onAttack(QuestEnv env) {
		if (env == null || env.getPlayer() == null || !(env.getVisibleObject() instanceof Npc npc)) {
			return false;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onLvlUp(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestRuntimeDispatcher typed = productionDispatcher;
			if (player != null) {
				try {
					// 真端表驱动车道：DD LevelUp 接取面（kind 8 与 10，等级等值；路由集为空 ⇒ 恒 false）。
					// DataDriven LevelUp acquire face (kinds 8 and 10, exact level equality).
					DataDrivenNativeRuntime.instance().onLevelReached(player, player.getLevel(), false);
				} catch (RuntimeException ignored) {
					// Native level-up acquire is best-effort.
				}
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
	 * 分发登录事件（真端 DD LevelUpLogIn 接取遍历：等级等值、只服务 kind 10）。
	 * Dispatch the login event (the retail DD LevelUpLogIn acquire walk: exact level equality,
	 * kind 10 only).
	 * @param player 登录玩家 / the logged-in player
	 */
	public void onLoggedIn(Player player) {
		if (player == null) {
			return;
		}
		try {
			DataDrivenNativeRuntime.instance().onLevelReached(player, player.getLevel(), true);
		} catch (RuntimeException ignored) {
			// Native login acquire is best-effort.
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onDie(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onLogOut(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onNpcReachTarget(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onNpcLostTarget(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * @param FlyRing 飞行环标识 / Flying-ring key
	 */
	public void onPassFlyingRing(QuestEnv env, String FlyRing) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			IntArrayList lists = getOnPassFlyingRingsQuests(FlyRing);
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onEnterWorld(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			Player player = env.getPlayer();
			QuestRuntimeDispatcher typed = productionDispatcher;
			// 原生车道：链形行/单步行的旧存档任务书行自愈（真端编译边登记 P0c-28 的 native 等价物）。
			// Native lane: enter-world save heal for chain/single-step rows (the P0c-28 heal-edge equivalent).
			try {
				SimpleTalkHandler.instance().onEnterWorld(player);
			} catch (RuntimeException ignored) {
				// Native enter-world heal is best-effort.
			}
			try {
				SimpleItemPlayHandler.instance().onEnterWorld(player);
			} catch (RuntimeException ignored) {
				// Native enter-world heal is best-effort.
			}
			try {
				// 真端表驱动车道：DataDriven EnterWorld 步（本批路由集为空 ⇒ 恒 false）。
				// DataDriven EnterWorld steps: no-op until the atomic switch batch.
				DataDrivenNativeRuntime.instance().onEnterWorld(player, player.getWorldId());
			} catch (RuntimeException ignored) {
				// Native enter-world dispatch is best-effort.
			}
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
	 * 重新评估玩家持久化的 LOCKED 任务，只执行对应 owner 的自动升级路由。
	 * Re-evaluates the player's persisted LOCKED quests through each owner's automatic level-up routes.
	 * <p>LOCKED 是旧版任务列表中的持久化占位状态；规划器仍要求任务定义显式声明
	 * {@code start-eligible} 及全部元数据前置条件，避免把普通 NPC 对话路由变成自动接取。
	 * LOCKED is a persisted placeholder from the legacy quest list; the planner still requires
	 * an explicit {@code start-eligible} condition and all metadata prerequisites, so ordinary
	 * NPC dialog routes never become automatic starts.</p>
	 * @param player 玩家 / player
	 */
	public void recheckLockedQuestStates(Player player) {
		if (player == null || player.getQuestStateList() == null) {
			return;
		}
		List<Integer> lockedQuestIds = new ArrayList<>();
		for (QuestState state : player.getQuestStateList().getAllQuestState()) {
			if (state.getStatus() == QuestStatus.LOCKED) {
				lockedQuestIds.add(state.getQuestId());
			}
		}
		for (int questId : lockedQuestIds) {
			try {
				productionDispatcher.dispatch(new QuestEvent.LevelUp(), player.getObjectId(), questId,
					QuestDispatchContract.EXCLUSIVE);
			} catch (RuntimeException ignored) {
				// 锁定任务恢复为尽力而为，与常规升级分发边界一致。
				// Locked-state recovery is best-effort, matching the normal level-up dispatch boundary.
			}
		}
	}

	/**
	 * 分发使用物品事件；首个非 UNKNOWN 结果即返回。
	 * Dispatch an item-use event; return the first non-UNKNOWN result.
	 * @param env 任务环境 / Quest environment
	 * @param item 使用的物品 / Used item
	 * Handler result
	 */
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		if (env == null || env.getPlayer() == null || item == null || item.getItemTemplate() == null) {
			return HandlerResult.FAILED;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			if (player != null) {
				int itemId = item.getItemTemplate().getTemplateId();
				// P5 真端族用物事件：SimpleUseItem 用物开接取窗、SimpleItemPlay 用物推进入 REWARD。
				// 两个原生车道先手认领各自道具（单一 owner：已切换行的 typed 目录里没有它的路由）。
				// P5 native item-use events: SimpleUseItem opens the accept window, SimpleItemPlay
				// advances to REWARD. The native lanes claim their own items first (single owner).
				if (SimpleUseItemHandler.instance().onItemUse(player, itemId)
						|| SimpleItemPlayHandler.instance().onItemUse(player, itemId)) {
					return HandlerResult.SUCCESS;
				}
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
		QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * Item id
	 */
	public void onItemGet(QuestEnv env, int itemId) {
		if (env == null || env.getPlayer() == null || itemId <= 0) {
			return;
		}
		QuestRuntimeDispatcher typed = productionDispatcher;
		Player player = env == null ? null : env.getPlayer();
		// 真端表驱动车道：DataDriven ItemPlay 步由物品获得事件推进（真端事件 5，`FUN_180c46e90`；
		// 本批路由集为空 ⇒ 恒 false）。
		// Table lane: DataDriven ItemPlay steps advance on the item-acquire event (no-op until step 2f).
		DataDrivenNativeRuntime.instance().onItemAcquired(player, itemId);
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
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player recipient = env == null ? null : env.getPlayer();
			// 真端表驱动车道：DataDriven PvP 步（军衔区间 + 等级差闸门；本批路由集为空 ⇒ 恒 false）。
			// DataDriven PvP steps: no-op until the atomic switch batch.
			try {
				if (recipient != null && env.getVisibleObject() instanceof Player victim) {
					DataDrivenNativeRuntime.instance().onKillRanked(killer, victim, playerRank);
				}
			} catch (RuntimeException ignored) {
				// Native PvP dispatch is best-effort.
			}
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
	 * @param env 任务环境 / Quest environment
	 * 世界 ID / World id
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
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * Zone name
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onEnterZone(QuestEnv env, ZoneName zoneName) {
		if (env == null || env.getPlayer() == null || zoneName == null) {
			return false;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			// 真端表驱动车道：DataDriven EnterArea 步（本批路由集为空 ⇒ 恒 false）。
			// DataDriven EnterArea steps: no-op until the atomic switch batch.
			DataDrivenNativeRuntime.instance().onEnterZone(player, zoneName.name());
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
	 * @param env 任务环境 / Quest environment
	 * Zone name
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onLeaveZone(QuestEnv env, ZoneName zoneName) {
		if (env == null || env.getPlayer() == null || zoneName == null) {
			return false;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * Movie id
	 * @return 是否有处理器接管 / Whether a handler took over
	 */
	public boolean onMovieEnd(QuestEnv env, int movieId) {
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onQuestTimerEnd(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		Player player = env.getPlayer();
		QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * Skill id
	 * @return 是否处理成功 / Whether successful
	 */
	public boolean onUseSkill(QuestEnv env, int skillId) {
		if (env == null || env.getPlayer() == null || skillId <= 0) {
			return false;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * Item id
	 */
	public void onFailCraft(QuestEnv env, int itemId) {
		if (env == null || env.getPlayer() == null || itemId <= 0) {
			return;
		}
		Player player = env == null ? null : env.getPlayer();
		QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * Item id
	 */
	public void onEquipItem(QuestEnv env, int itemId) {
		if (env == null || env.getPlayer() == null || itemId <= 0) {
			return;
		}
		Player player = env == null ? null : env.getPlayer();
		QuestRuntimeDispatcher typed = productionDispatcher;
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
		QuestRuntimeDispatcher typed = productionDispatcher;
		QuestEvent event = new QuestEvent.CanAct(templateId, questActionType.name());
		try {
			// 真端表驱动车道：采集对象（QUEST_USE_ITEM 交互物）的可交互性由 native 侧按
			// START 态 + 中继链完成度判定（族切换后没有 typed 路由可查）。
			// Native lane: a collect object's usability is adjudicated natively (START state plus
			// relay-chain completion); after the family switch there is no typed route to consult.
			if (questActionType == QuestActionType.ACTION_ITEM_USE
					&& SimpleCollectItemHandler.instance().allowsItemUse(env.getPlayer(), templateId)) {
				return true;
			}
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onDredgionReward(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 */
	public void onKamarReward(QuestEnv env) {
	}

	/**
	 * 分发欧菲丹奖励事件。
	 * Dispatch an Ophidan reward event.
	 * @param env 任务环境 / Quest environment
	 */
	public void onOphidanReward(QuestEnv env) {
	}

	/**
	 * 分发堡垒奖励事件。
	 * Dispatch a Bastion reward event.
	 * @param env 任务环境 / Quest environment
	 */
	public void onBastionReward(QuestEnv env) {
	}

	/**
	 * 分发奖励加成应用事件。
	 * Dispatch a bonus-apply event.
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
			QuestRuntimeDispatcher typed = productionDispatcher;
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
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * @param teleportId 传送点 ID / teleport id
	 */
	public void onEnterWindStream(QuestEnv env, int teleportId) {
		if (env == null || env.getPlayer() == null || teleportId <= 0) {
			return;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
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
	 * @param env 任务环境 / Quest environment
	 * Ride item id
	 */
	public void rideAction(QuestEnv env, int itemId) {
	}

	/**
	 * 分发创造力点数事件。
	 * Dispatch a creativity-point event.
	 * @param env 任务环境 / Quest environment
	 */
	public void onCreativityPoint(QuestEnv env) {
	}

	/**
	 * 注册（或获取）NPC 的任务关联对象。
	 * Register (or obtain) the quest association for an NPC.
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
	 * Item id
	 * Quest id
	 */
	public void registerQuestHouseItem(int itemId, int questId) {
		if (!questHouseItems.containsKey(itemId)) {
			IntArrayList itemRelatedQuests = new IntArrayList();
			itemRelatedQuests.add(questId);
			questHouseItems.put(itemId, itemRelatedQuests);
		} else {
			questHouseItems.get(itemId).add(questId);
		}
	}

	/**
	 * 注册获得物品关联任务。
	 * Register a quest for item-obtain events.
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
	 * Item id
	 * Quest id
	 */
	public void registerOnEquipItem(int itemId, int questId) {
		if (!questOnEquipItem.containsKey(itemId)) {
			Set<Integer> questIds = new HashSet<>();
			questIds.add(questId);
			questOnEquipItem.put(itemId, questIds);
		} else {
			questOnEquipItem.get(itemId).add(questId);
		}
	}

	/**
	 * 注册可行动作监听。
	 * Register a can-act listener for a template.
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
	 * Quest id
	 */
	public void registerOnEnterWindStream(int questId) {
		if (!questOnEnterWindStream.contains(questId))
			questOnEnterWindStream.add(questId);
	}

	/**
	 * 注册骑乘动作监听。
	 * Register a ride-action listener.
	 * Quest id
	 */
	public void registerOnRide(int questId) {
		if (!questRideAction.contains(questId))
			questRideAction.add(questId);
	}

	/**
	 * 注册创造力点数监听。
	 * Register a creativity-point listener.
	 * Quest id
	 */
	public void registerOnCreativityPoint(int questId) {
		if (!questOnCreativityPoint.contains(questId))
			questOnCreativityPoint.add(questId);
	}

	/**
	 * 注册跟随到达目标监听。
	 * Register a reach-target listener.
	 * Quest id
	 */
	public void registerAddOnReachTargetEvent(int questId) {
		if (!reachTarget.contains(questId))
			reachTarget.add(questId);
	}

	/**
	 * 注册跟随丢失目标监听。
	 * Register a lost-target listener.
	 * Quest id
	 */
	public void registerAddOnLostTargetEvent(int questId) {
		if (!lostTarget.contains(questId))
			lostTarget.add(questId);
	}

	/**
	 * 获取 NPC 的任务关联对象；未注册时返回空壳。
	 * Return the quest association for an NPC, or an empty shell if unregistered.
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
	 * 查询穿过飞行环关联任务。
	 * Look up quests related to passing a flying ring.
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

	/**
	 * 原生表车道是否为该行的 owner（已被原生处理器路由的行）。
	 * <p>
	 * 与 {@link #isProductionOwner(int)}（typed IR 目录）互斥：已切换的行不在 typed 目录里，因此
	 * 放弃/元数据这类"目录面"入口必须按 owner 分流，不能只看 typed catalog。
	 * Whether the retail-table lane owns the row (a row routed by a native handler). Mutually exclusive
	 * with the typed IR catalog, so catalog-facing entries (abandon, metadata) must branch by owner.
	 */
	public boolean isNativeOwner(int questId) {
		return questId > 0 && (SimpleHuntHandler.instance().routes(questId)
			|| SimpleSerialHuntHandler.instance().routes(questId)
			|| SimpleTalkHandler.instance().routes(questId)
			|| SimpleCollectItemHandler.instance().routes(questId)
			|| SimpleUseItemHandler.instance().routes(questId)
			|| SimpleItemPlayHandler.instance().routes(questId)
			|| SimpleCombineTaskHandler.instance().routes(questId)
			|| com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.instance().routes(questId));
	}

	/**
	 * 原生行的真端 {@code quest.xml} 元数据（与生产目录同一条 {@code RetailQuestMetadataCompiler} 装载链）。
	 * 缺行或元数据不干净一律 empty（fail-closed）。
	 * The retail {@code quest.xml} metadata of a native row, from the same compiler chain as the
	 * production catalog; missing rows and unclean metadata return empty (fail-closed).
	 */
	public Optional<QuestMetadata> nativeMetadata(int questId) {
		if (!isNativeOwner(questId)) {
			return Optional.empty();
		}
		try {
			return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId)
				.filter(compiled -> compiled.clean())
				.map(compiled -> compiled.metadata());
		} catch (RuntimeException | java.io.IOException e) {
			return Optional.empty();
		}
	}

	/**
	 * 原生行的区域任务清单结论（真端 opcode 127 三值：平条目 / {@code 0x20000} 软标记 / 不入列表）。
	 * 非 owner 行返回 {@code OMITTED}，调用方按 typed 车道自有规则处理。
	 * <p>
	 * The retail zone-quest verdict of a native-owned row (opcode 127). Non-owned rows answer
	 * {@code OMITTED} so the caller can fall back to the typed lane's own rule.
	 */
	public NativeQuestStartPort.ZoneVerdict nativeZoneVerdict(Player player, int questId) {
		if (player == null || questId <= 0 || !isNativeOwner(questId)) {
			return NativeQuestStartPort.ZoneVerdict.OMITTED;
		}
		return NativeQuestStartPort.instance().zoneVerdict(player, questId);
	}

	/**
	 * 原生行的接取资格硬判定（真端 {@code CanAcquireQuest} = 2）：NPC 对话、传送门与势力任务列表面。
	 * 「只差 1 级」是清单面的软结论，在本判定下仍是拒绝。
	 * <p>
	 * The hard acquisition verdict of a native-owned row (retail {@code CanAcquireQuest} == 2), used by
	 * the NPC/portal/faction faces; the one-level-short soft verdict stays a rejection here.
	 */
	public boolean nativeAcquireAllowed(Player player, int questId) {
		return nativeZoneVerdict(player, questId) == NativeQuestStartPort.ZoneVerdict.ACQUIRABLE;
	}

	/**
	 * 原生行是否可放弃：owner 命中即由 {@link #onNativeAbandon(Player, int)} + 共用清理段收尾；
	 * 「能否放弃」由真端 {@code cannot_giveup} 元数据轴在 {@code QuestService} 侧判定。
	 * Whether the native lane declares the abandon path for the row; the retail {@code cannot_giveup}
	 * axis stays with {@code QuestService}.
	 */
	public boolean hasNativeAbandonRoute(int questId) {
		return isNativeOwner(questId);
	}

	/**
	 * 原生行的族级放弃动作（如 CombineTask 忘配方）；无族级动作的族返回 {@code true}。
	 * 共用的状态复位与工作物品回收不在本方法内。
	 * The family-level abandon actions of a native row (CombineTask forgets its recipe); families
	 * without family-level actions answer {@code true}. Shared state/inventory cleanup is not here.
	 */
	public boolean onNativeAbandon(Player player, int questId) {
		if (player == null || questId <= 0 || !isNativeOwner(questId)) {
			return false;
		}
		if (SimpleCombineTaskHandler.instance().routes(questId)) {
			return SimpleCombineTaskHandler.instance().onAbandon(player, questId);
		}
		return true;
	}

	/**
	 * 从显式 production catalog 加载已通过 owner 审核的 typed 定义。
	 * 真端驱动开关（{@code aion.quest.retailDriver}，默认开启）开启时，保留清单判定为
	 * RETAIL_TABLE 的任务用真端定义替换/补入（retail-package overlay）；关闭开关时
	 * 只有 XML 目录仍覆盖全部生产任务才允许启动，已退役的 XML 不会被开关恢复。
	 * Loads the typed catalog; the retail-first overlay replaces retail-owned definitions when
	 * the switch is on; the off state requires a complete legacy XML catalog.
	 */
	private QuestCatalog loadProductionCatalog() throws Exception {
		QuestDialogContract.invalidateDefault();
		QuestCatalog xmlCatalog = QuestDefinitionCatalogManifest.compile(
			Config.dataFile("./data/static_data/quest/definitions").toPath());
		return RetailQuestDriver.overlayProduction(xmlCatalog);
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
		// 使用专用守护线程而非 commonPool：预加载与静态数据阶段并行，commonPool 调度是
		// 启动期静默卡住的疑点之一；独立线程不占静态数据加载池，也不依赖 commonPool。
		// Uses a dedicated daemon thread instead of the commonPool: the preload overlaps the
		// static-data phase and commonPool scheduling was a suspect in silent startup stalls;
		// a dedicated thread neither occupies the static-data pool nor depends on commonPool.
		Thread preloadThread = new Thread(() -> {
			try {
				future.complete(catalogSupplier.get());
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		}, "quest-catalog-preload");
		preloadThread.setDaemon(true);
		preloadThread.start();
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
	 * <p>所有 owner 和事件接线会先完成校验，dispatcher 只在 NPC 索引注册完成后发布。
	 * All owners and event wiring are validated first; the dispatcher is published only after
	 * NPC indexes have been registered.</p>
	 */
	/**
	 * 生产 NPC AI 查询（启动期固定使用）：{@code DataManager.NPC_DATA} 未就绪时返回 null。
	 * Production NPC AI lookup; returns null while the runtime {@code DataManager} is not ready.
	 */
	private static String productionNpcAi(int templateId) {
		if (DataManager.NPC_DATA == null) {
			return null;
		}
		var template = DataManager.NPC_DATA.getNpcTemplate(templateId);
		return template == null ? null : template.getAi();
	}

	PreparedProductionDefinitions prepareProductionDefinitions(QuestCatalog catalog) {
		return prepareProductionDefinitions(catalog, QuestEngine::productionNpcAi);
	}

	/**
	 * 生产准备的可注入重载：NPC AI 索引由调用方给出，其余启动期合同完全一致。
	 * <p>
	 * 生产调用固定注入 {@link #productionNpcAi(int)}（{@code DataManager.NPC_DATA}）；启动期门禁注入测试侧
	 * AI 索引，从而在不起静态数据的情况下跑完整准备路径（交互对象合同 + 事件接线合同 + item-play 索引）。
	 * Production preparation with an injectable NPC AI index; the gates use it to exercise the whole
	 * startup path without the runtime {@code DataManager} bootstrap.
	 */
	PreparedProductionDefinitions prepareProductionDefinitions(QuestCatalog catalog,
			IntFunction<String> aiNameByTemplate) {
		java.util.Objects.requireNonNull(aiNameByTemplate, "aiNameByTemplate");
		RuntimeCatalogs catalogs = splitRuntimeCatalogs(catalog);
		QuestRuntimeComposition snapshotComposition = QuestRuntimeComposition.production(catalogs.combined());
		QuestExecutionCoordinator coordinator = new QuestExecutionCoordinator(new PlayerSerialExecutor());
		QuestProductionDispatcher xmlDispatcher = QuestProductionDispatcher.production(catalogs.xml(),
			snapshotComposition, coordinator);
		QuestProductionDispatcher retailDispatcher = catalogs.retail().entries().isEmpty()
			? QuestProductionDispatcher.disabled()
			: QuestProductionDispatcher.production(catalogs.retail(), snapshotComposition, coordinator);
		QuestRuntimeRouter dispatcher = new QuestRuntimeRouter(catalogs.combined(), xmlDispatcher, retailDispatcher);
		QuestInteractionObjectValidator.validate(xmlDispatcher, aiNameByTemplate);
		QuestInteractionObjectValidator.validate(retailDispatcher, aiNameByTemplate);
		snapshotComposition.installBroadcastPort(new PlayerQuestBroadcastPort(
			playerId -> com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId),
			(player, questIds) -> dispatcher.dispatchOwners(new QuestEvent.ZoneMissionEnd(), player.getObjectId(),
				questIds, QuestDispatchContract.EXCLUSIVE),
			(player, questIds) -> dispatcher.dispatchOwners(new QuestEvent.EventQuestRefresh(), player.getObjectId(),
				questIds, QuestDispatchContract.EXCLUSIVE)));
		for (CompiledQuestDefinition definition : catalogs.combined().executables()) {
			QuestProductionEventWiring.validateDefinition(definition);
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
		return new PreparedProductionDefinitions(catalogs.combined(), dispatcher);
	}

	/**
	 * 将生产目录按真端保留清单拆成 XML 与真端两个互斥子目录。
	 * Splits the production catalog into disjoint XML and retail child catalogs according to the retail owner manifest.
	 * <p>
	 * P7 步 f 起 retail 编译产物为零（七族 + DataDriven 1467 行全部原生直驱），retail 子目录恒空，
	 * 生产与测试目录都走 combined 单目录兼容形；归属核验仍由
	 * {@link RetailQuestDriver#overlayProduction(QuestCatalog)} 在装载前完成。
	 * Since P7 step f the retail compile products are zero (all families and the 1467 DataDriven rows
	 * are native-driven), so the retail child catalog is always empty and both production and test
	 * catalogs use the combined form; ownership is still validated by overlayProduction before load.
	 */
	private static RuntimeCatalogs splitRuntimeCatalogs(QuestCatalog catalog) {
		QuestCatalogRegistry combined = catalog instanceof QuestCatalogRegistry existing
			? existing : new QuestCatalogRegistry(catalog);
		return new RuntimeCatalogs(combined, combined, new QuestCatalogRegistry(new ImmutableQuestCatalog(List.of())));
	}

	/** 已校验的组合目录与两个互斥子目录。 / Validated combined catalog and its two disjoint child catalogs. */
	private record RuntimeCatalogs(QuestCatalogRegistry combined, QuestCatalogRegistry xml, QuestCatalogRegistry retail) {
	}

	void installProductionDefinitions(QuestCatalog catalog) {
		installProductionDefinitions(prepareProductionDefinitions(catalog));
	}

	private void installProductionDefinitions(PreparedProductionDefinitions prepared) {
		QuestCatalogRegistry catalog = prepared.catalog();
		QuestRuntimeDispatcher dispatcher = prepared.dispatcher();
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
				} else if (transition.event() instanceof QuestEvent.KillNpc(int npcId1)) {
					registerQuestNpc(npcId1).addOnKillEvent(definition.id());
					} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
						for (int npcId : npcIds) {
							registerQuestNpc(npcId).addOnKillEvent(definition.id());
						}
				} else if (transition.event() instanceof QuestEvent.AttackNpc attack) {
					registerQuestNpc(attack.npcId()).addOnAttackEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.CanAct canAct) {
					registerCanAct(definition.id(), canAct.templateId());
				} else if (transition.event() instanceof QuestEvent.UseItem use) {
					registerQuestItem(use.itemId(), definition.id());
				} else if (transition.event() instanceof QuestEvent.GetItem(int id)) {
					// Keep the obtain-event index in sync with the typed catalog. The
					// runtime dispatcher remains authoritative; this index only lets
					// the legacy loop skip typed owners safely.
					registerGetingItem(id, definition.id());
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
				} else if (transition.event() instanceof QuestEvent.MovieEnd(int movieId)) {
					registerOnMovieEndQuest(movieId, definition.id());
				} else if (transition.event() instanceof QuestEvent.ZoneMissionEnd) {
					registerOnEnterZoneMissionEnd(definition.id());
				} else if (transition.event() instanceof QuestEvent.InvisibleTimerEnd) {
					registerOnInvisibleTimerEnd(definition.id());
				} else if (transition.event() instanceof QuestEvent.EquipItem(int itemId)) {
					registerOnEquipItem(itemId, definition.id());
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
		QuestRuntimeDispatcher dispatcher = productionDispatcher;
		return new PreparedProductionDefinitions(dispatcher.catalogRegistry(), dispatcher);
	}

	/**
	 * 添加处理器侧掉落（XML 未声明时由脚本侧补充）。
	 * Add a handler-side drop (supplemental when not declared in XML).
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
	 * @param progressLatch 进度闩锁（可空） / Progress latch (nullable)
	 */
	public void load(CountDownLatch progressLatch) {
		load(progressLatch, null);
	}

	/** Loads the typed production catalog using an already compiled and validated typed snapshot. */
	public void load(CountDownLatch progressLatch, PreparedProductionDefinitions prepared) {
		log.info(I18n.get("log.5359e35f8f99"));
		try {
			// P0b 数据基础：启动期装载真端页注册表；缺失/损坏页表必须让启动失败（fail-fast）。
			// P0b data foundation: eagerly load the retail page registry at startup; a missing or
			// corrupt page table must fail the boot (fail-fast).
			HtmlPagesRegistry.ensureLoaded();
			CameraRegistry.ensureLoaded();
			SimpleHuntHandler.instance().installInterest(this);
			SimpleSerialHuntHandler.instance().installInterest(this);
			SimpleTalkHandler.instance().installInterest(this);
			SimpleCollectItemHandler.instance().installInterest(this);
			SimpleUseItemHandler.instance().installInterest(this);
			SimpleItemPlayHandler.instance().installInterest(this);
			SimpleCombineTaskHandler.instance().installInterest(this);
			// DataDriven 原生运行时：本批路由集为空 ⇒ 兴趣面零注册（切换随 P7 步 2 步 f）。
			// DataDriven native runtime: empty routing set in this batch, so no interests are installed.
			DataDrivenNativeRuntime.instance().installInterest(this);
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
		messageSendingTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
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
}
