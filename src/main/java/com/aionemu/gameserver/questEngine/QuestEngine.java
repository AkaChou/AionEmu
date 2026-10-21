package com.aionemu.gameserver.questEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestNpc;
import com.aionemu.gameserver.model.templates.rewards.BonusType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNpcAttackFacts;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
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
import com.aionemu.gameserver.questEngine.runtime.QuestDispatchContract;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRouteResult;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeComposition;
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
	private static volatile ObjectProvider<QuestEngine> instanceProvider;
	private static volatile QuestEngine resolvedInstance;
	/** 任务事件注册表 / Quest event registry */
	private final QuestEventRegistry registry = new QuestEventRegistry();
	/** 对话平面路由 / Dialog-plane router */
	private final QuestDialogRouter dialogRouter;
	/** 任务物品销毁守卫 / Quest-item destroy guard */
	private final QuestItemDestroyGuard destroyGuard;
	/** 生产目录预装载状态机 / Production catalog preload state machine */
	private final QuestCatalogBoot catalogBoot = new QuestCatalogBoot();
	/** 每日/周任务提醒广播 / Daily/weekly reminder broadcast */
	private final QuestDailyReminder dailyReminder = new QuestDailyReminder();
	/** Fully composed production ports used by typed quest execution. */
	private final QuestRuntimeComposition runtimeComposition = QuestRuntimeComposition.production();
	/** Live XML/retail runtime router; each child owns a disjoint catalog. */
	private volatile QuestRuntimeDispatcher productionDispatcher = QuestProductionDispatcher.disabled();

	/** Fully validated immutable typed runtime waiting to be published. */
	public record PreparedProductionDefinitions(QuestCatalogRegistry catalog,
			QuestRuntimeDispatcher dispatcher) {
		public PreparedProductionDefinitions {
			java.util.Objects.requireNonNull(catalog, "catalog");
			java.util.Objects.requireNonNull(dispatcher, "dispatcher");
		}
	}

	/**
	 * 创建任务引擎实例。
	 * Create a quest-engine instance.
	 */
	public QuestEngine() {
		this.dialogRouter = new QuestDialogRouter(this);
		this.destroyGuard = new QuestItemDestroyGuard(registry);
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

	/** 返回当前已发布的 typed 生产分发器（包内只读视图）。
	 * Returns the currently published typed production dispatcher (package read-only view). */
	QuestRuntimeDispatcher productionDispatcher() {
		return productionDispatcher;
	}

	/** Returns the immutable catalog snapshot used by the currently published typed dispatcher. */
	public QuestCatalogRegistry questCatalog() {
		return productionDispatcher.catalogRegistry();
	}

	/**
	 * 掉落查询 = 目录快照掉落 ∪ native 表车道掉落列（采集族 + Talk 族 + UseItem 族 + DataDriven 族）。
	 * <p>
	 * 各族已切 native，退役 XML 从 catalog 退场后其 {@code <drops>} 一并消失——native 行必须从
	 * 原版 {@code drop_*} 列接手（2026-10-04 修复：采集族此前对象交互与击杀装配都拿不到任务掉落，
	 * 真机 1103 采集不产出 {@code quest_1103a}；Talk 族 1031 行同型，真机 1105 击杀 210079 无道具。
	 * 2026-10-08 补齐 UseItem 族 2435 与 DataDriven 族 160 行：真机 10034 击杀 216494 无
	 * {@code quest_10034a}）。同一 questId 只信目录条目（单一 owner；XML 车道仍持有该行时 native
	 * 不重复供源；native 各族按 owner 族列互斥，重复即缺陷——由门禁报红，不在运行期静默吸收）。
	 * <p>
	 * Quest drops = the catalog snapshot ∪ the native table-lane drop columns (collect + talk +
	 * use-item + data-driven). After the XML retirement the families' {@code <drops>} left the catalog
	 * with the XML; the native rows serve them from the retail {@code drop_*} column (2026-10-04 for
	 * collect/talk, 2026-10-08 for use-item/data-driven). A quest id owned by the catalog keeps its
	 * catalog entries only (single-owner invariant).
	 */
	public List<QuestCatalogDrop> questDrops(int npcId) {
		List<QuestCatalogDrop> catalogDrops = productionDispatcher.questDrops(npcId);
		List<QuestCatalogDrop> collectDrops = SimpleCollectItemHandler.instance().questDropsFor(npcId);
		List<QuestCatalogDrop> talkDrops = SimpleTalkHandler.instance().questDropsFor(npcId);
		List<QuestCatalogDrop> useItemDrops = SimpleUseItemHandler.instance().questDropsFor(npcId);
		List<QuestCatalogDrop> dataDrivenDrops = DataDrivenNativeRuntime.instance().questDropsFor(npcId);
		if (collectDrops.isEmpty() && talkDrops.isEmpty() && useItemDrops.isEmpty() && dataDrivenDrops.isEmpty()) {
			return catalogDrops;
		}
		Set<Integer> catalogQuestIds = catalogDrops.stream().map(QuestCatalogDrop::questId)
			.collect(java.util.stream.Collectors.toSet());
		List<QuestCatalogDrop> combined = new java.util.ArrayList<>(catalogDrops);
		for (List<QuestCatalogDrop> source : List.of(collectDrops, talkDrops, useItemDrops, dataDrivenDrops)) {
			for (QuestCatalogDrop drop : source) {
				if (!catalogQuestIds.contains(drop.questId())) {
					combined.add(drop);
				}
			}
		}
		return List.copyOf(combined);
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
	public static QuestEngine getInstance() {
		QuestEngine resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<QuestEngine> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("QuestEngine 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<QuestEngine> instanceProvider) {
		QuestEngine.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/**
	 * 分发 NPC 对话事件；逐字委托对话平面路由（原版表车道先手、DD 对话平面、typed 派发与
	 * questId==0 兜底重放）。
	 * Dispatch an NPC dialog event; delegates verbatim to the dialog-plane router (retail table-lane
	 * first-hand matching, the DD dialog plane, typed dispatch and the questId==0 fallback replay).
	 * @param env 任务环境 / Quest environment
	 * @return 是否有处理器接管 / Whether a handler took over
	 */
	public boolean onDialog(QuestEnv env) {
		return dialogRouter.onDialog(env);
	}

	/** 返回 questId==0 对话的派发顺序（委托对话路由；包内测试入口签名不变）。 / Returns the questId==0 dialog dispatch order (dialog router; the package-visible test entry is unchanged). */
	List<Integer> npcDialogDispatchOwners(Player player, Npc npc, QuestEvent event) {
		return dialogRouter.npcDialogDispatchOwners(player, npc, event);
	}

	/** 对话路由（包内测试入口）。 / The dialog router (package-visible test entry). */
	QuestDialogRouter dialogRouterForTest() {
		return dialogRouter;
	}

	/**
	 * 分发已接取的服务端任务共享（不虚构 NPC 交互对象）。
	 * Dispatches an accepted server-issued quest share without inventing an NPC interaction object.
	 */
	public void onSharedQuestDialog(QuestEnv env) {
		dialogRouter.onSharedQuestDialog(env);
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
		// 原版表驱动车道：SimpleHunt 怪物击杀由原生相机直接推进，不走 IR 边分派
		if (SimpleHuntHandler.instance().onKill(env.getPlayer(), npc.getNpcId())) {
			return true;
		}
		if (SimpleSerialHuntHandler.instance().onKill(env.getPlayer(), npc.getNpcId())) {
			return true;
		}
		// 原版表驱动车道：SimpleCollectItem 采集怪击杀推进同一相机（掉落仍由掉落族发放）。
		if (SimpleCollectItemHandler.instance().onKill(env.getPlayer(), npc.getNpcId())) {
			return true;
		}
		// 原版表驱动车道：DataDriven Hunt 步组计数（带原版 50m 距离门——处理函数前奏 case 0，
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
		if (!attackFactIsAuthoritative(npc)) {
			return true;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			List<Integer> questIds = getQuestNpc(npc.getNpcId()).getOnAttackEvent();
			if (questIds.stream().anyMatch(typed::owns)) {
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

	/**
	 * 攻击事实是否权威。
	 * Whether an attack fact is authoritative.
	 * <p>已删除且仍存活的 NPC 不构成权威事实：诱导到点删除后的在途攻击会空寻址 after-commit 监视动作。
	 * 已死亡的 NPC 例外——伤害先于 {@code onAttack} 钩子结算，致命一击到达时已死亡（甚至刚被 0ms 尸体消散删除），
	 * 但事实仍权威，低于 50% 之类的血量条件必须由它成立。</p>
	 * <p>A deleted-but-alive NPC carries no authoritative fact: in-flight attacks after lure deletion would
	 * address no target in the after-commit watch action. A dead NPC is the exception: damage settles before
	 * the {@code onAttack} hook, so the lethal blow arrives already dead (possibly even decay-deleted) yet is
	 * still authoritative — HP conditions such as "below 50%" must be satisfied by it.</p>
	 */
	static boolean attackFactIsAuthoritative(Npc npc) {
		return npc.getLifeStats() != null
				&& (npc.isSpawned() || npc.getLifeStats().isAlreadyDead());
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
			try {
				// 原版表驱动车道：DD LevelUp 接取面（kind 8 与 10，等级等值；路由集为空 ⇒ 恒 false）。
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
		} catch (Exception ex) {
			// log.error(I18n.get("log.b844c9346335", ex));
		}
	}

	/**
	 * 分发登录事件（原版 DD LevelUpLogIn 接取遍历：等级等值、只服务 kind 10）。
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
		// native 车道链式发放面（DD `acquire=none` 后继）：本方法是 typed 车道完成同步的唯一出口
		// （PlayerQuestStateSyncPort 的 COMPLETION 分支），XML 前置（如 10032）完成后经此定向发放后继；
		// 方法也服务非完成同步 ⇒ 运行期以「该任务当前确为 COMPLETE」自守卫，非完成零动作。
		// native 完成侧（setFinishingState → onLvlUp → onLevelReached 重走）不在本方法内。
		// The native chain-acquire face: this is the typed lane's only completion-sync outlet, so XML
		// predecessors (e.g. 10032) reach the successor grant here; the runtime self-guards on the live
		// COMPLETE status because non-completion syncs share this method. Native completions arrive via
		// setFinishingState → onLvlUp → the onLevelReached re-walk instead.
		try {
			DataDrivenNativeRuntime.instance().onQuestCompleted(env.getPlayer(), env.getQuestId());
		} catch (RuntimeException ignored) {
			// Native chain acquire is best-effort, matching the other native notifications.
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
			Player player = env.getPlayer();
			if (registry.questOnDie.stream().anyMatch(typed::owns)) {
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
			// 分发到 typed owner：玩家登出（广播全部 log-out 路由）。
			// Dispatch to typed owners: player logged out (broadcast all log-out routes).
			try {
				typed.dispatch(new QuestEvent.LogOut(), player.getObjectId(), 0,
					QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed logout dispatch is best-effort.
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
			IntArrayList lists = registry.getOnPassFlyingRingsQuests(FlyRing);
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			boolean hasTypedOwner = false;
			for (int questId : lists) {
				if (typed.owns(questId)) {
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
			// 原生车道：链形行/单步行的旧存档任务书行自愈（原版编译边登记 P0c-28 的 native 等价物）。
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
				// 采集族中继步旧私编（bit16..17 → var0）归一，修复客户端任务书步骤显示为空。
				// CollectItem relay-step normalization (legacy bits 16..17 → var0).
				SimpleCollectItemHandler.instance().onEnterWorld(player);
			} catch (RuntimeException ignored) {
				// Native enter-world heal is best-effort.
			}
			try {
				// 用物族中继步旧私编（bit16..17 → var0）归一，修复客户端任务书步骤显示为空。
				// UseItem relay-step normalization (legacy bits 16..17 → var0).
				SimpleUseItemHandler.instance().onEnterWorld(player);
			} catch (RuntimeException ignored) {
				// Native enter-world heal is best-effort.
			}
			try {
				// 原版表驱动车道：DataDriven EnterWorld 步（本批路由集为空 ⇒ 恒 false）。
				// DataDriven EnterWorld steps: no-op until the atomic switch batch.
				DataDrivenNativeRuntime.instance().onEnterWorld(player, player.getWorldId());
			} catch (RuntimeException ignored) {
				// Native enter-world dispatch is best-effort.
			}
			try {
				typed.dispatch(new QuestEvent.EnterWorld(), player.getObjectId(), 0,
					QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed enter-world dispatch is best-effort.
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
			int itemId = item.getItemTemplate().getTemplateId();
			// P5 原版族用物事件：SimpleUseItem 用物开接取窗、SimpleItemPlay 用物推进入 REWARD。
			// 两个原生车道先手认领各自道具（单一 owner：已切换行的 typed 目录里没有它的路由）。
			// P5 native item-use events: SimpleUseItem opens the accept window, SimpleItemPlay
			// advances to REWARD. The native lanes claim their own items first (single owner).
			if (SimpleUseItemHandler.instance().onItemUse(player, itemId)
					|| SimpleItemPlayHandler.instance().onItemUse(player, itemId)) {
				return HandlerResult.SUCCESS;
			}
			// DD 原生车道：ItemPlay 步 = 物品使用事件（原版事件 5 在 `User__UseItem`/
			// `User_IdentifyItem` 内经 `mgr+0x268+5*0x10` walk 派发，ctx+8 = 被使用物品 id；
			// 接取 kind==3 的行由同一事件接取）。物品获得/发放不推进 ItemPlay 步。
			// DD lane: ItemPlay steps advance on the item-USE event (retail tag 5 fired from
			// User__UseItem with the used item's id); acquiring/granting the item does not.
			if (DataDrivenNativeRuntime.instance().onItemUsed(player, itemId)) {
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
	 */
	public void onItemPlayCompletedEvent(Player player, int itemId) {
		if (player == null || itemId <= 0) {
			return;
		}
		try {
			OptionalInt animationMillis = productionDispatcher.itemPlayAnimationMillis(itemId);
			if (animationMillis.isEmpty()) {
				return;
			}
			productionDispatcher.dispatch(
				new QuestEvent.ItemPlay(itemId, animationMillis.getAsInt()), player.getObjectId(), 0,
				QuestDispatchContract.FIRST_NON_UNKNOWN);
		} catch (Exception ex) {
			log.error(I18n.get("log.quest_engine.item_play_failed", player.getObjectId(), itemId), ex);
		}
	}

	/**
	 * 分发使用房屋物品事件。
	 * Dispatch a house-item use event.
	 * @param env 任务环境 / Quest environment
	 * @param itemId 物品模板 ID / Item template id
	 * @param itemObjectId 客户端提供的房屋物件标识 / Client-provided house object identity
	 */
	public void onHouseItemUseEvent(QuestEnv env, int itemId, int itemObjectId) {
		if (env == null || env.getPlayer() == null || itemId <= 0 || itemObjectId < 0) {
			return;
		}
		QuestRuntimeDispatcher typed = productionDispatcher;
		Player player = env.getPlayer();
		if (typed.hasRoutes(new QuestEvent.HouseItemUse(itemId))) {
			try {
				QuestEvent.HouseItemUse event = runtimeComposition.housingEventPort()
					.houseItemUse(env, itemId, itemObjectId);
				typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed house-item dispatch is best-effort.
			}
		}
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
		Player player = env.getPlayer();
		// DD 原生车道的 ItemPlay 步不走获得面：原版事件 5 = 物品使用（`User__UseItem` walk），
		// 接线在 onItemUseEvent（2026-10-06 实机 13403：发放动作曾在此级联跳过探测器使用步）。
		// The DD ItemPlay steps are not wired to the obtain face: retail event 5 is item USE,
		// hooked in onItemUseEvent (live 13403 round 4).
		List<Integer> questIds = registry.questItems.get(itemId);
		if (questIds != null && questIds.stream().anyMatch(typed::owns)) {
			// 物品进入玩家背包后先进入正式 typed owner；同一物品可被多个
			// 任务监听，因此使用广播契约，不因一个 owner 的状态而截断其他 owner。
			// Route the obtain fact through typed owners first. The same item may
			// belong to multiple quests, so use the broadcast contract.
			try {
				typed.dispatch(new QuestEvent.GetItem(itemId), player.getObjectId(), 0,
					QuestDispatchContract.BROADCAST);
				long inventoryCount = player.getInventory() == null
					? 1L : player.getInventory().getItemCountByItemId(itemId);
				int collectedCount = (int) Math.clamp(inventoryCount, 1L, Integer.MAX_VALUE);
				typed.dispatch(new QuestEvent.CollectItem(itemId, collectedCount),
					player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed obtain dispatch is best-effort.
			}
		}
	}

	/**
	 * 销毁任务物品前的确认与收口（原版 User_DestroyItem 确认流程镜像；逐字委托销毁守卫）。
	 * Quest-item destroy confirmation flow (the retail User_DestroyItem mirror; delegates verbatim to
	 * the destroy guard).
	 * @param player 玩家 / the player
	 * @param item 待销毁物品 / the item being destroyed
	 * @return true = 已认领销毁（弹窗或拒绝提示）；false = 无进行中任务引用，走普通销毁 /
	 *         true when claimed (window or refusal), false when unreferenced
	 */
	public boolean tryQuestItemDestroy(Player player, Item item) {
		return destroyGuard.tryQuestItemDestroy(player, item);
	}

	/**
	 * 玩家进行中且引用该物品的任务（typed 目录索引 ∪ DD 原生行引用面；委托销毁守卫）。
	 * In-progress quests referencing the item (typed index ∪ DD row references; delegates to the guard).
	 * @param player 玩家 / the player
	 * @param itemId 物品模板 id / the item template id
	 * @return 按发现顺序的任务 id 列表 / the quest ids in discovery order
	 */
	public List<Integer> activeQuestsReferencingItem(Player player, int itemId) {
		return destroyGuard.activeQuestsReferencingItem(player, itemId);
	}

	/**
	 * 分发 PvpService 授权的军衔击杀（含显式积分来源）。
	 * Dispatches a PvpService-authorized ranked kill with explicit credit source.
	 */
	public void onKillRanked(QuestEnv env, AbyssRankEnum playerRank, Player killer,
			QuestPvpCreditSource creditSource) {
		if (env == null || env.getPlayer() == null || playerRank == null || killer == null || creditSource == null) {
			return;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player recipient = env.getPlayer();
			// 原版表驱动车道：DataDriven PvP 步（军衔区间 + 等级差闸门；本批路由集为空 ⇒ 恒 false）。
			// DataDriven PvP steps: no-op until the atomic switch batch.
			try {
				if (env.getVisibleObject() instanceof Player victim) {
					DataDrivenNativeRuntime.instance().onKillRanked(killer, victim, playerRank);
				}
			} catch (RuntimeException ignored) {
				// Native PvP dispatch is best-effort.
			}
			if (typed.hasRoutes(new QuestEvent.KillRanked(playerRank.getId()))) {
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
		}
	}

	/**
	 * 分发世界内击杀事件。
	 * Dispatch a kill-in-world event.
	 * @param env 任务环境 / Quest environment
	 * @param worldId 世界 ID / World id
	 */
	public void onKillInWorld(QuestEnv env, int worldId) {
		onKillInWorld(env, worldId, env == null ? null : env.getPlayer(), QuestPvpCreditSource.SOLO);
	}

	/**
	 * 分发 PvpService 授权的世界内击杀（含显式积分来源）。
	 * Dispatches a PvpService-authorized world kill with explicit credit source.
	 */
	public void onKillInWorld(QuestEnv env, int worldId, Player killer,
			QuestPvpCreditSource creditSource) {
		if (env == null || env.getPlayer() == null || !(env.getVisibleObject() instanceof Player victim)
				|| worldId <= 0 || killer == null || creditSource == null) {
			return;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player recipient = env.getPlayer();
			if (victim.getAbyssRank() != null && victim.getAbyssRank().getRank() != null
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
		}
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
			// 原版表驱动车道：DataDriven EnterArea 步（本批路由集为空 ⇒ 恒 false）。
			// DataDriven EnterArea steps: no-op until the atomic switch batch.
			DataDrivenNativeRuntime.instance().onEnterZone(player, zoneName.name());
			try {
				typed.dispatch(new QuestEvent.EnterZone(zoneName.name()),
					player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed enter-zone dispatch is best-effort.
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
			try {
				typed.dispatch(new QuestEvent.LeaveZone(zoneName.name()),
					player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed leave-zone dispatch is best-effort.
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
	 * @param movieId 动画 ID / Movie id
	 */
	public void onMovieEnd(QuestEnv env, int movieId) {
		if (env == null || env.getPlayer() == null) {
			return;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			try {
				typed.dispatch(new QuestEvent.MovieEnd(movieId), player.getObjectId(), 0,
					QuestDispatchContract.EXCLUSIVE);
			} catch (RuntimeException ignored) {
				// Typed movie-end dispatch is best-effort.
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.e20bb13d3b6a", ex));
		}
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
		if (env.getQuestId() > 0) {
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
	 * @param skillId 技能 ID / Skill id
	 */
	public void onUseSkill(QuestEnv env, int skillId) {
		if (env == null || env.getPlayer() == null || skillId <= 0) {
			return;
		}
		try {
			QuestRuntimeDispatcher typed = productionDispatcher;
			Player player = env.getPlayer();
			if (typed.hasRoutes(new QuestEvent.UseSkill(skillId))) {
				try {
					QuestEvent.UseSkill event = runtimeComposition.skillEventPort().useSkill(env, skillId);
					typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
				} catch (RuntimeException ignored) {
					// Typed skill-use dispatch is best-effort.
				}
			}
		} catch (Exception ex) {
			// log.error(I18n.get("log.494055729bb4", ex));
		}
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
		Player player = env.getPlayer();
		QuestRuntimeDispatcher typed = productionDispatcher;
		if (player.getInventory() != null
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
		Player player = env.getPlayer();
		QuestRuntimeDispatcher typed = productionDispatcher;
		try {
			typed.dispatch(new QuestEvent.EquipItem(itemId),
				player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
		} catch (RuntimeException ignored) {
			// Typed equip-item dispatch is best-effort.
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
	public boolean onCanAct(final QuestEnv env, int templateId, final QuestActionType questActionType) {
		if (env == null || env.getPlayer() == null || questActionType == null) {
			return false;
		}
		QuestRuntimeDispatcher typed = productionDispatcher;
		QuestEvent event = new QuestEvent.CanAct(templateId, questActionType.name());
		try {
			// 原版表驱动车道：采集对象（QUEST_USE_ITEM 交互物）的可交互性由 native 侧判定——采集族按
			// START 态 + 中继链完成度；DD 族按 START + 当前步命中（CollectItem 步的追加 FOBJ 列对象，
			// 2026-10-08 实机 15011 右击 702730）。族切换后两者都没有 typed 路由可查。
			// Native lane: a collect object's usability is adjudicated natively (the collect family by
			// START plus relay-chain completion, the DD family by START plus the current-step hit on its
			// appended-FOBJ objects); after the family switch there is no typed route to consult.
			if (questActionType == QuestActionType.ACTION_ITEM_USE
					&& (SimpleCollectItemHandler.instance().allowsItemUse(env.getPlayer(), templateId)
						|| DataDrivenNativeRuntime.instance().allowsItemUse(env.getPlayer(), templateId))) {
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
		Player player = env.getPlayer();
		if (typed.hasRoutes(new QuestEvent.DredgionReward())) {
			try {
				QuestEvent.DredgionReward event = runtimeComposition.pvpInstanceEventPort().dredgionReward(env);
				typed.dispatch(event, player.getObjectId(), 0, QuestDispatchContract.BROADCAST);
			} catch (RuntimeException ignored) {
				// Typed dredgion-reward dispatch is best-effort.
			}
		}
	}

	/**
	 * 分发奖励加成应用事件。
	 * Dispatch a bonus-apply event.
	 * @param env 任务环境 / Quest environment
	 * @param bonusType 奖励类型 / Bonus type
	 * @return 处理结果 / Handler result
	 */
	public HandlerResult onBonusApplyEvent(QuestEnv env, BonusType bonusType) {
		if (env == null || env.getPlayer() == null || bonusType == null) {
			return HandlerResult.FAILED;
		}
		try {
			Player player = env.getPlayer();
			QuestRuntimeDispatcher typed = productionDispatcher;
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
			return HandlerResult.UNKNOWN;
		} catch (Exception ex) {
			// log.error(I18n.get("log.fc7e13ab7975", ex));
			return HandlerResult.FAILED;
		}
	}

	/**
	 * 分发靠近目标距离事件（20 单位内）。
	 * Dispatch an at-distance event (within 20 units).
	 * @param env 任务环境 / Quest environment
	 */
	public void onAtDistance(QuestEnv env) {
		if (env == null || !(env.getVisibleObject() instanceof Npc npc) || env.getPlayer() == null) {
			return;
		}
		if (!registry.questNpcs.containsKey(npc.getNpcId())) {
			return;
		}
		QuestNpc questNpc = getQuestNpc(npc.getNpcId());
		if (questNpc.getOnDistanceEvent().isEmpty()) {
			return;
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
		}
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
			Player player = env.getPlayer();
			if (typed.hasRoutes(new QuestEvent.EnterWindStream(teleportId))) {
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

	/** 注册（或获取）NPC 的任务关联对象。 / Register (or obtain) the quest association for an NPC. */
	public QuestNpc registerQuestNpc(int npcId) {
		return registry.registerQuestNpc(npcId);
	}

	/** 注册物品使用关联任务。 / Register a quest for item-use events. */
	public void registerQuestItem(int itemId, int questId) {
		registry.registerQuestItem(itemId, questId);
	}

	/** 注册获得物品关联任务。 / Register a quest for item-obtain events. */
	public void registerGetingItem(int itemId, int questId) {
		registry.registerGetingItem(itemId, questId);
	}

	/** 注册区域任务结束监听。 / Register a zone-mission-end listener. */
	public void registerOnEnterZoneMissionEnd(int questId) {
		registry.registerOnEnterZoneMissionEnd(questId);
	}

	/** 注册死亡监听。 / Register a death listener. */
	public void registerOnDie(int questId) {
		registry.registerOnDie(questId);
	}

	/** 注册登出监听。 / Register a logout listener. */
	public void registerOnLogOut(int questId) {
		registry.registerOnLogOut(questId);
	}

	/** 注册穿过飞行环监听。 / Register a pass-flying-ring listener. */
	public void registerOnPassFlyingRings(String flyingRing, int questId) {
		registry.registerOnPassFlyingRings(flyingRing, questId);
	}

	/** 注册动画结束监听。 / Register a movie-end listener. */
	public void registerOnMovieEndQuest(int moveId, int questId) {
		registry.registerOnMovieEndQuest(moveId, questId);
	}

	/** 注册隐形计时器结束监听。 / Register an invisible-timer-end listener. */
	public void registerOnInvisibleTimerEnd(int questId) {
		registry.registerOnInvisibleTimerEnd(questId);
	}

	/** 注册装备物品监听。 / Register an equip-item listener. */
	public void registerOnEquipItem(int itemId, int questId) {
		registry.registerOnEquipItem(itemId, questId);
	}

	/** 注册可行动作监听。 / Register a can-act listener for a template. */
	public void registerCanAct(int questId, int templateId) {
		registry.registerCanAct(questId, templateId);
	}

	/** 注册进入风道监听。 / Register an enter-windstream listener. */
	public void registerOnEnterWindStream(int questId) {
		registry.registerOnEnterWindStream(questId);
	}

	/** 注册跟随到达目标监听。 / Register a reach-target listener. */
	public void registerAddOnReachTargetEvent(int questId) {
		registry.registerAddOnReachTargetEvent(questId);
	}

	/** 注册跟随丢失目标监听。 / Register a lost-target listener. */
	public void registerAddOnLostTargetEvent(int questId) {
		registry.registerAddOnLostTargetEvent(questId);
	}

	/** 获取 NPC 的任务关联对象；未注册时返回空壳。
	 * Return the quest association for an NPC, or an empty shell if unregistered. */
	public QuestNpc getQuestNpc(int npcId) {
		return registry.getQuestNpc(npcId);
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
	 * 原生行的原版 {@code quest.xml} 元数据（与生产目录同一条 {@code RetailQuestMetadataCompiler} 装载链）。
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
				.filter(RetailQuestMetadataCompiler.Outcome::clean)
				.map(RetailQuestMetadataCompiler.Outcome::metadata);
		} catch (RuntimeException | java.io.IOException e) {
			return Optional.empty();
		}
	}

	/**
	 * 原生行的区域任务清单结论（原版 opcode 127 三值：平条目 / {@code 0x20000} 软标记 / 不入列表）。
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
	 * 原生行的接取资格硬判定（原版 {@code CanAcquireQuest} = 2）：NPC 对话、传送门与势力任务列表面。
	 * 「只差 1 级」是清单面的软结论，在本判定下仍是拒绝。
	 * <p>
	 * The hard acquisition verdict of a native-owned row (retail {@code CanAcquireQuest} == 2), used by
	 * the NPC/portal/faction faces; the one-level-short soft verdict stays a rejection here.
	 */
	public boolean nativeAcquireAllowed(Player player, int questId) {
		return nativeZoneVerdict(player, questId) == NativeQuestStartPort.ZoneVerdict.ACQUIRABLE;
	}

	/**
	 * 该行是否仍在指定 NPC 的接取清单里（原版 {@code OnHyperlinkAction} 对页 4/1011 动作的受理轴：
	 * 该 NPC 服务器给玩家标记了这条可接任务）。判定与附近任务列表同一函数（原版
	 * {@code User::CheckQuestAcquireCondition} 与清单共用 {@code Quest::CanAcquireQuest}）。
	 * <p>
	 * Whether the row is still offered at the npc (the retail intake axis of accept-page acts: the
	 * npc server flagged the row acquirable for this player). Same verdict as the nearby list.
	 */
	public boolean isQuestReceivableAt(Player player, int npcId, int questId) {
		if (player == null || npcId <= 0 || questId <= 0) {
			return false;
		}
		if (!isQuestRowAtNpc(npcId, questId)) {
			return false;
		}
		return nativeAcquireAllowed(player, questId);
	}

	/**
	 * 该行是否由指定 NPC 发放（各 native 族的接取节点表）：原版页 4/1011 的动作只在「该 NPC 的对话树」
	 * 里出现，别的 NPC 上的同 id 行不受理。
	 * Whether the row is offered at the npc (each native family's acquire node); accept-page acts only
	 * exist inside the npc's own dialog tree.
	 */
	public boolean isQuestRowAtNpc(int npcId, int questId) {
		if (npcId <= 0 || questId <= 0) {
			return false;
		}
		return SimpleTalkHandler.instance().acquireNpcs(questId).contains(npcId)
			|| SimpleHuntHandler.instance().acquireNpcs(questId).contains(npcId)
			|| SimpleSerialHuntHandler.instance().acquireNpc(questId) != null
				&& SimpleSerialHuntHandler.instance().acquireNpc(questId) == npcId
			|| SimpleCollectItemHandler.instance().acquireNpcs(questId).contains(npcId)
			|| SimpleItemPlayHandler.instance().acquireNpcs(questId).contains(npcId);
	}

	/**
	 * 该 NPC 上仍在发放的接取行（各族接取节点 ∪，按附近任务清单同一判定过滤）。
	 * The rows this npc still offers (every native family's acquire nodes, same verdict as the list).
	 */
	public List<Integer> receivableQuestIdsAtNpc(Player player, int npcId) {
		if (player == null || npcId <= 0) {
			return List.of();
		}
		List<Integer> receivable = new java.util.ArrayList<>();
		for (int questId : SimpleTalkHandler.instance().acquireQuestIdsForNpc(npcId)) {
			if (SimpleTalkHandler.instance().routes(questId) && nativeAcquireAllowed(player, questId)) {
				receivable.add(questId);
			}
		}
		for (int questId : SimpleHuntHandler.instance().questsForNpc(npcId)) {
			if (SimpleHuntHandler.instance().acquireNpcs(questId).contains(npcId)
					&& nativeAcquireAllowed(player, questId)) {
				receivable.add(questId);
			}
		}
		for (int questId : SimpleSerialHuntHandler.instance().questsForNpc(npcId)) {
			Integer acquire = SimpleSerialHuntHandler.instance().acquireNpc(questId);
			if (acquire != null && acquire == npcId && nativeAcquireAllowed(player, questId)) {
				receivable.add(questId);
			}
		}
		for (int questId : SimpleCollectItemHandler.instance().ownedQuestIds()) {
			if (SimpleCollectItemHandler.instance().acquireNpcs(questId).contains(npcId)
					&& nativeAcquireAllowed(player, questId)) {
				receivable.add(questId);
			}
		}
		for (int questId : SimpleItemPlayHandler.instance().ownedQuestIds()) {
			if (SimpleItemPlayHandler.instance().acquireNpcs(questId).contains(npcId)
					&& nativeAcquireAllowed(player, questId)) {
				receivable.add(questId);
			}
		}
		return receivable;
	}

	/**
	 * 原生行是否可放弃：owner 命中即由 {@link #onNativeAbandon(Player, int)} + 共用清理段收尾；
	 * 「能否放弃」由原版 {@code cannot_giveup} 元数据轴在 {@code QuestService} 侧判定。
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
	 * 原版驱动开关（{@code aion.quest.retailDriver}，默认开启）开启时，保留清单判定为
	 * RETAIL_TABLE 的任务用原版定义替换/补入（retail-package overlay）；关闭开关时
	 * 只有 XML 目录仍覆盖全部生产任务才允许启动，已退役的 XML 不会被开关恢复。
	 * Loads the typed catalog; the retail-first overlay replaces retail-owned definitions when
	 * the switch is on; the off state requires a complete legacy XML catalog.
	 */
	private QuestCatalog loadProductionCatalog() {
		QuestDialogContract.invalidateDefault();
		QuestCatalog xmlCatalog = QuestDefinitionCatalogManifest.compile(
			Config.dataFile("./data/static_data/quest/definitions").toPath());
		return RetailQuestDriver.overlayProduction(xmlCatalog);
	}

	/**
	 * 在配置就绪后尽早编译 raw catalog，使其解析时间与静态数据加载重叠。
	 * 编译结果进入生产目录的共享缓存，静态数据阶段与后续读取直接复用。
	 * Compile the raw catalog as soon as configuration is ready so parsing overlaps static-data loading.
	 * The result enters the shared production catalog cache and is reused by the static-data phase
	 * and all later readers.
	 */
	public void preloadProductionCatalog() {
		ProductionQuestDefinitions.preload(this::loadProductionCatalog);
	}

	/**
	 * 返回启动期生产目录：优先消费本引擎的后台预编译结果，未预编译时复用共享缓存。
	 * Returns the startup production catalog: consumes this engine's preload when armed,
	 * otherwise reuses the shared cache.
	 *
	 * @return 已编译并校验的生产目录 / compiled and verified production catalog
	 */
	QuestCatalog productionCatalogForStartup() {
		if (catalogBoot.isArmed()) {
			try {
				return awaitProductionCatalogPreload();
			} catch (Exception e) {
				throw new IllegalStateException("quest catalog preload failed", e);
			}
		}
		return ProductionQuestDefinitions.catalog(this::loadProductionCatalog);
	}

	/**
	 * 可注入预编译入口，仅用于包内测试；生产启动走共享目录预编译。
	 * Injectable preload entry used by package-level tests; production startup uses the shared
	 * catalog preload.
	 */
	synchronized void preloadProductionCatalog(Supplier<QuestCatalog> catalogSupplier) {
		catalogBoot.preload(catalogSupplier);
	}

	/**
	 * 等待预编译结果；未启用预加载时保持原有同步编译路径。
	 * Await the package-level injectable preload result; production startup reads the shared
	 * catalog instead.
	 */
	synchronized QuestCatalog awaitProductionCatalogPreload() throws Exception {
		return catalogBoot.awaitPreload(this::loadProductionCatalog);
	}

	PreparedProductionDefinitions prepareProductionDefinitions(QuestCatalog catalog) {
		return QuestProductionPreparer.prepare(catalog);
	}

	/**
	 * 生产准备的可注入重载（准备器承载完整启动期合同）。
	 * Injectable production preparation overload (the preparer carries the full startup contract:
	 * interaction objects, event wiring, item-play index).
	 */
	PreparedProductionDefinitions prepareProductionDefinitions(QuestCatalog catalog,
			IntFunction<String> aiNameByTemplate) {
		return QuestProductionPreparer.prepare(catalog, aiNameByTemplate);
	}

	/**
	 * 在 legacy Handler 注册前校验并安装全部正式 typed owner。
	 * Validate and install all production typed owners before legacy handlers register.
	 * <p>所有 owner 和事件接线会先完成校验，dispatcher 只在 NPC 索引注册完成后发布。
	 * All owners and event wiring are validated first; the dispatcher is published only after
	 * NPC indexes have been registered.</p>
	 */
	void installProductionDefinitions(QuestCatalog catalog) {
		installProductionDefinitions(prepareProductionDefinitions(catalog));
	}

	private void installProductionDefinitions(PreparedProductionDefinitions prepared) {
		productionDispatcher = QuestProductionInstaller.install(this, prepared);
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
	 * 启动时装载：注册掉落、加载脚本处理器与 XML 任务，并启动每日提醒。
	 * Bootstrap load: register drops, load script handlers and XML quests, start daily reminders.
	 * @param progressLatch 进度闩锁（可空） / Progress latch (nullable)
	 */
	public void load(CountDownLatch progressLatch) {
		load(progressLatch, null);
	}

	/** Loads the typed production catalog using an already compiled and validated typed snapshot. */
	public void load(CountDownLatch progressLatch, PreparedProductionDefinitions prepared) {
		log.debug(I18n.get("log.5359e35f8f99"));
		try {
			// P0b 数据基础：启动期装载原版页注册表；缺失/损坏页表必须让启动失败（fail-fast）。
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
					? prepareProductionDefinitions(productionCatalogForStartup()) : prepared);
			log.info(I18n.get("log.quest_engine.typed_owners_loaded", productionDispatcher.owners().size()));
		} catch (Exception e) {
			throw new GameServerError("Can't initialize typed quest engine.", e);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
		dailyReminder.schedule(this);
	}

	/**
	 * 关闭引擎：清空全部注册数据。
	 * Shut down the engine: clear all registered data.
	 */
	public void shutdown() {
		clear();
		log.debug(I18n.get("log.dd61afc44888"));
	}

	/**
	 * 清空所有事件注册表与处理器映射，取消定时任务。
	 * Clear every event registry and handler map; cancel the reminder task.
	 */
	public void clear() {
		runtimeComposition.cleanupAll();
		productionDispatcher = QuestProductionDispatcher.disabled();
		catalogBoot.reset();
		dailyReminder.cancel();
		registry.clear();
	}
}
