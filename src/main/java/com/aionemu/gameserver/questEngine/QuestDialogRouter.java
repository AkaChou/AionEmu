package com.aionemu.gameserver.questEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestDispatchContract;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRouteResult;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeDispatcher;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.NativeUnpinnedRewardWindow;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 任务对话平面路由：承接 {@link QuestEngine#onDialog(QuestEnv)} 的原版表车道先手、
 * DD 对话平面、typed owner 派发与 questId==0 兜底重放。
 * Quest dialog-plane router: hosts the retail table-lane first-hand matching, the DD dialog
 * plane, the typed-owner dispatch and the questId==0 fallback replay of
 * {@link QuestEngine#onDialog(QuestEnv)}.
 * <p>本类承载原 {@code QuestEngine#onDialog} 的逐字行为
 * （含日志类别：logger 绑定 {@link QuestEngine}）。</p>
 * <p>This router reproduces the former {@code QuestEngine#onDialog} behavior verbatim, including the
 * logging category (the logger stays bound to {@link QuestEngine}).</p>
 */
final class QuestDialogRouter {

	private static final Logger log = LoggerFactory.getLogger(QuestEngine.class);

	private final QuestEngine engine;

	QuestDialogRouter(QuestEngine engine) {
		this.engine = engine;
	}

	/**
	 * 分发 NPC 对话事件；questId 为 0 时按 NPC 上注册的谈话任务依次尝试。
	 * Dispatch an NPC dialog event; when questId is 0, try talk-quests registered on the NPC.
	 * @param env 任务环境 / Quest environment
	 * @return 是否有处理器接管 / Whether a handler took over
	 */
	boolean onDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		Player player = env.getPlayer();
		try {
			Npc npc = env.getVisibleObject() instanceof Npc target ? target : null;
			int requestedOwner = env.getQuestId();
			int npcId = npc == null ? 0 : npc.getNpcId();
			QuestRuntimeDispatcher typed = engine.productionDispatcher();
			// 原版表驱动车道：SimpleHunt 任务直接由原生处理器驱动，不走 IR 节点状态机
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
			// 原版表驱动车道：SimpleTalk 任务由原生处理器直驱（cab520 接取 / cabb10 中继与报告）
			if (requestedOwner != 0 && SimpleTalkHandler.instance().routes(requestedOwner)) {
				if (SimpleTalkHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 原版表驱动车道：SimpleCollectItem 采集对象/交付 NPC 由原生处理器直驱（P4 切换批）
			if (requestedOwner != 0 && SimpleCollectItemHandler.instance().routes(requestedOwner)) {
				if (SimpleCollectItemHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 原版表驱动车道：SimpleUseItem 用物接取后的无主对话/中继/交付由原生处理器直驱（P5 切换批）
			if (requestedOwner != 0 && SimpleUseItemHandler.instance().routes(requestedOwner)) {
				if (SimpleUseItemHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 原版表驱动车道：SimpleItemPlay 接取/交付预览/领奖由原生处理器直驱（P5 切换批）
			if (requestedOwner != 0 && SimpleItemPlayHandler.instance().routes(requestedOwner)) {
				if (SimpleItemPlayHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 原版表驱动车道：CombineTask 接取（发分量 + 学配方）/ 交付（产物门 + 回收分量）/ 领奖由
			// 原生处理器直驱（P6 切换批）。
			// The CombineTask lane (accept with component grants and recipe learn, product-gated
			// hand-in with component recycling, reward-window settlement) is driven natively.
			if (requestedOwner != 0 && SimpleCombineTaskHandler.instance().routes(requestedOwner)) {
				if (SimpleCombineTaskHandler.instance().onDialog(env)) {
					return true;
				}
			}
			// 原版表驱动车道：DataDriven Talk / CollectItem 步的共享对话平面（开页 + 页动作/1009/1008，
			// `FUN_180c474b0`）与 TalkFOBJ 组计数（本批路由集为空 ⇒ 恒 false）。
			// DataDriven dialog plane and TalkFOBJ groups: no-op until the atomic switch batch.
			// 无目标领奖（npcId == 0）也进入 DD 面：原版 QuestDialog 无主键协议由本面按 questId 结算
			// （存在性/状态/动作门全在 DD 内部；未认领返回 false 继续下方链路）。
			// Targetless reward claims (npcId == 0) reach the DD face too: the ownerless QuestDialog
			// protocol is settled by quest id inside the runtime; an unclaimed id falls through.
			if (DataDrivenNativeRuntime.instance().onDialog(player, npcId, env.getDialogId(),
				npc != null ? npc.getObjectId() : 0, requestedOwner)) {
				return true;
			}
			// native 车道奖励窗确认动作的按 questId 恢复面（原版 QuestDialog 无主键协议；与 typed 车道
			// QuestRuntimeDispatcher#dispatchRewardWindowAction 的恢复同裁定）：奖励窗由全局 UI 打开，
			// 客户端可能携带上一个交互对象，不能要求交互对象等于完成路由的交付 NPC。八族严格绑定全部
			// 未命中后按 questId + action 结算——传送门类 AI 的开门页轴会在「接取对象 ≠ 交付对象」的行
			// 的接取对象上开出奖励窗（2026-10-08 实机 19640：洛塔斯 799022 上点奖励窗 → 无人认领 →
			// 动作 id 被 AI 回显为页 8 → 客户端 load fail）。门 = 奖励窗动作段 + REWARD 态 + native owner；
			// 结算体继续验原版元数据/档位/按钮声明，任何一门不过零副作用继续既有链路。
			// Native-lane quest-id recovery for reward-window confirmations (the retail ownerless
			// QuestDialog protocol; same adjudication as the typed lane's recovery). The reward window is
			// opened by global UI, so the carried interaction object must not be required to equal the
			// completion route's delivery NPC. After all eight families miss on strict binding, settle by
			// quest id + action; every retail gate still runs inside the settlement and any failure keeps
			// zero side effects (live 2026-10-08, quest 19640: the portal-type AI's open-door page axis
			// pops the reward window on the acquire npc, the action was unclaimed and echoed back as
			// page 8, failing the client load).
			if (requestedOwner != 0 && npc != null && engine.isNativeOwner(requestedOwner)
					&& NativeUnpinnedRewardWindow.claim(player, requestedOwner, env.getDialogId(),
						npc.getObjectId())) {
				return true;
			}
			// 原版对话平面的统一兜底：子页动作（SELECT⟨n⟩_…，动作 id 即目标页 id，如 1694 = select3_1）
			// 在 native 各族与 DD 都未认领时，按客户端契约原样回发该页——**必须携带 questId**（客户端按
			// 任务 html 渲染该页；零上下文时去 NPC 对话 html 找该页 ⇒ load fail：2026-10-06 实机 13403
			// 步推进后动作 1694 落三参/两参分叉，两参形态即此症状；9/28 基线与 1001-1007 的三参回发为
			// 正确形）。与 Talk/ItemPlay 既有子页分支（QE-141）同语义，本兜底统一覆盖 DD 与其余缺该分支
			// 的族；typed 车道保持原链路。
			// Universal fallback of the retail dialog plane: a selection sub-page action (the action id is
			// the target page id, e.g. 1694 = select3_1) echoes the declared page back WITH the quest
			// context when no lane claimed it (live 13403: after an advance the step guard no longer
			// matched and the echo dropped the context, so the client looked the page up in the NPC
			// dialog html and failed to load; the 9/28 baseline sent it with the context). Same semantics
			// as the existing Talk/ItemPlay sub-page branches (QE-141); the typed lane keeps its own path.
			if (requestedOwner != 0 && npcId != 0 && !typed.owns(requestedOwner)
					&& QuestDialogPage.isSelectionSubPage(env.getDialogId())
					&& QuestDialogContract.loadDefault().hasButtonPage(requestedOwner, env.getDialogId())) {
				QuestState subPageState = player.getQuestStateList().getQuestState(requestedOwner);
				if (subPageState != null && (subPageState.getStatus() == QuestStatus.START
						|| subPageState.getStatus() == QuestStatus.REWARD)) {
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(npc.getObjectId(), env.getDialogId(), requestedOwner));
					return true;
				}
			}
			// 携带任务上下文的接取页动作（页 4/1011，动作 id = 页 id）：原版 OnHyperlinkAction 在
			// 「领取中任务 / 仍在可接清单」时受理并向接取窗答页；未受理时零副作用。
			// A quest-carrying accept-page act (page 4/1011): retail admits it through the acquiring slot
			// or the offered list and answers with the ask window; otherwise it stays inert.
			if (requestedOwner != 0 && npc != null
					&& QuestDialogAction.isSelectionContinuationAction(env.getDialogId())
					&& engine.isQuestReceivableAt(player, npcId, requestedOwner)) {
				int askWindow = QuestDialogContract.loadDefault().askWindowPage(requestedOwner);
				if (askWindow > 0) {
					player.setNpcQuestDialogAcquiring(npc.getObjectId(), requestedOwner);
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(npc.getObjectId(), askWindow, requestedOwner));
					return true;
				}
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
				// 拒绝面不得静默：接取转换命中但状态/条件/资格不满足时，位置形如「点了接受，窗一关，
				// 任务没接上」——2026-10-19 实机 19679（缺成长证物）无从区分「条件不够」与「服务器坏了」。
				// 解析唯一拒绝轴并下发对应原版提示（见 QuestStartRefusal），同时留下 QUEST-TRACE。
				// A refused accept must never stay silent: accepting, watching the window close and getting
				// no quest was indistinguishable from a broken server (live 2026-10-19, quest 19679). Only a
				// refused START transition is reported; turn-in and page refusals keep their retail silence.
				if (result.owners().stream().anyMatch(owner -> owner.result() == QuestRouteResult.UNKNOWN
						&& isAcceptAction(player, owner.questId(), env.getDialogId()))) {
					// 资格投影只在生产 dispatcher 上可得；测试替身没有投影时退回 native 判定。
					// The projection exists only on the production dispatcher; test doubles fall back.
					QuestStartEligibility eligibility = typed instanceof QuestProductionDispatcher production
						? production.startEligibility(player.getObjectId(), requestedOwner).orElse(null)
						: null;
					// 拒绝轴进 trace：只记「被拒」无法区分等级/种族/前置/重复冷却，排查时只能猜。
					// The rejection axis goes into the trace; "refused" alone cannot tell level, race,
					// prerequisite or repeat-cooldown apart.
					log.info(I18n.get("log.quest_trace.typed_accept_refused", player.getName(),
						requestedOwner, env.getDialogId(),
						eligibility == null ? "native" : eligibility.reason()));
					try {
						QuestStartRefusal.notifyTypedRefused(player, requestedOwner, eligibility);
					} catch (RuntimeException hintFailure) {
						log.warn(I18n.get("log.quest_dialog.typed_accept_refusal_hint_failed",
							player.getName(), requestedOwner, hintFailure.getMessage()));
					}
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
				// 该行已切 native，typed 目录里没有它的路由）。2026-10-03 修复：先手原样 return true
				// 吞掉交互且不下发任何包（真机 1103：右键谷物袋子零响应）——改为重放进 handler，
				// 由 START 段 onObjectUse 推进收集并回页。
				// Direct client interaction with a collect object (questId==0 entry): the native collect
				// family matches first. Fixed 2026-10-03: the bare return swallowed the interaction with
				// no packet (live quest 1103); it now replays into the handler so onObjectUse advances.
				boolean collectObjectClaimed = false;
				for (SimpleCollectItemHandler.CollectTargetRef ref
						: SimpleCollectItemHandler.instance().targetsForNpc(npcId)) {
					if (!SimpleCollectItemHandler.instance().routes(ref.questId())) {
						continue;
					}
					collectObjectClaimed = true;
					if (engine.onDialog(new QuestEnv(npc, player, ref.questId(), env.getDialogId()))) {
						env.setQuestId(ref.questId());
						return true;
					}
				}
				if (collectObjectClaimed) {
					// 物件交互已被 native 采集族认领（推进成功或原版零副作用：超杀/条件不满足）。
					// 原版从不在采集物件上开对话窗——不得落到 TalkEventHandler 的通用页 10
					// （2026-10-04 真机 1103：对谷物袋子开窗即客户端 load fail）。
					// The interaction is claimed by the native collect family (advance or retail zero
					// side effect). The retail server never opens a dialog window on a collect object;
					// falling through to the generic page 10 makes the client fail to load it (live 1103).
					return true;
				}
				// DD 采集物件（CollectItem 步的追加 FOBJ 列对象，2026-10-08 实机 15011 右击 702730）同形先手：
				// 按候选重放回 DD 面按 questId 认领（认领零状态写零发包；携带 owner 供组队每人一枚的
				// 掉落过滤）。DD 行无 typed 路由，认领只能由 DD 面自己做——此先手与上面采集族同责。
				// DD collect objects (CollectItem appended-FOBJ columns): the same first-hand replay so the
				// claim carries the owner id (used by the group each-member drop filter).
				boolean dataDrivenObjectClaimed = false;
				for (int objectQuestId : DataDrivenNativeRuntime.instance().collectObjectQuestIds(npcId)) {
					dataDrivenObjectClaimed = true;
					if (engine.onDialog(new QuestEnv(npc, player, objectQuestId, env.getDialogId()))) {
						env.setQuestId(objectQuestId);
						return true;
					}
				}
				if (dataDrivenObjectClaimed) {
					// 物件已被 DD 采集面接管（认领成功或原版零副作用：步不符/未接取）——同采集族口径，
					// 不得落到通用页 10（物件开窗即客户端 load fail）。
					// Claimed by the DD collect face (or the retail zero-side-effect shape); a dialog
					// window aimed at a collect object makes the client fail to load it.
					return true;
				}
				// 领取中任务的接取页动作（页 4/1011，动作 id = 页 id）：原版 User::OnHyperlinkAction 先按
				// 「questId == questIdAcquiring」受理，再做可接清单判定。用领取中槽位补上任务上下文后
				// 按该任务重派发同一动作（入口页/接取窗由各族的接取面答页）。
				// An accept-page act (page 4/1011, act id = page id) for the npc's acquiring slot: retail
				// admits it through questIdAcquiring first, then re-checks the offered list.
				if (requestedOwner == 0 && npc != null
						&& QuestDialogAction.isSelectionContinuationAction(env.getDialogId())) {
					int acquiringQuestId = player.getNpcQuestDialogAcquiringQuestId(npc.getObjectId());
					if (acquiringQuestId > 0
							&& engine.isQuestReceivableAt(player, npcId, acquiringQuestId)) {
						if (engine.onDialog(new QuestEnv(npc, player, acquiringQuestId, env.getDialogId()))) {
							env.setQuestId(acquiringQuestId);
							return true;
						}
					}
				}
				// 客户端任务对话树的页内导航动作（页 4/1011 及其主线/子页）：客户端本地翻页，
				// 服务端不回发同页（回发即把客户端重新放到同一页，多任务 NPC 上表现为「点了没反应」）。
				// Page-local navigation acts (page 4/1011 and its line/sub-pages): the client turns the
				// page itself; echoing it re-renders the same page (the dead click on multi-quest npcs).
				if (QuestDialogAction.isSelectionContinuationAction(env.getDialogId())) {
					log.debug(I18n.get("log.quest_engine.open_door_acquire_face", player.getName(), npcId,
						env.getDialogId(), "PAGE_LOCAL"));
					return true;
				}
				// 普通 NPC 的点选/打开（-1/26）：客户端任务列表不渲染待交付行（19683 教训），所以
				// **可交付（REWARD）任务必须先被重放**——原版「和头上有交付标记的 NPC 对话 → 交付窗」；
				// 没有可交付任务时交回客户端任务列表（页 10）。可接取/进行中的任务一律不参与开门：
				// 它们只由玩家点列表里的任务行（31）进入（旧实现重放进行中行，点 NPC 就直接进了某个任务）。
				// 任务物件（quest_use_item 等）例外，走下面的物件重放。
				// A plain npc click/open (-1/26): the client task list never renders hand-in rows (the
				// 19683 lesson), so deliverable (REWARD) quests must be replayed first — the retail
				// "talk to the npc with the hand-in marker → completion window". With nothing to hand in
				// the click falls back to the client-rendered list (page 10). Acquirable and live rows
				// never claim the open; they are entered from the list row (31) only. Quest objects are
				// exempt and keep the object replay below.
				if (env.getDialogId() == QuestDialogAction.USE_OBJECT.id()
						|| env.getDialogId() == 26) {
					// AI 引擎未装配（单测/早期启动）时按普通 NPC 处理：getAi2 会抛异常。
					// Without an AI engine (unit tests, early boot) treat the npc as plain: getAi2 throws.
					boolean questObject;
					try {
						questObject = npc.getAi2() instanceof com.aionemu.gameserver.ai.QuestItemNpcAI2
							|| npc.getAi2() instanceof com.aionemu.gameserver.ai.quests.QuestStartItemNpcAi2;
					} catch (RuntimeException noAiEngine) {
						questObject = false;
					}
					if (!questObject) {
						boolean replayed = false;
						for (QuestState state : player.getQuestStateList().getAllQuestState()) {
							// 可交付行＝native 行，或 typed owner 自带到本 NPC 的开门路由（npc-complete 的
							// preview 被编译成 REWARD 自环 USE_OBJECT → 奖励窗）。旧写法只认 native，typed
							// 任务进入 REWARD 后没有交付入口（19678/19679 实机 806698）。
							// Deliverable = a native row, or a typed owner carrying an open route at this npc
							// (npc-complete's preview compiles to a REWARD USE_OBJECT self-loop showing the
							// reward window). Native-only matching left typed REWARD rows with no entry.
							if (state.getStatus() != QuestStatus.REWARD
									|| (!ownsQuestAtNpc(state.getQuestId(), npcId)
										&& !typed.hasMatchingRoutes(event, state.getQuestId()))) {
								continue;
							}
							replayed = true;
							if (engine.onDialog(new QuestEnv(npc, player, state.getQuestId(),
									env.getDialogId()))) {
								env.setQuestId(state.getQuestId());
								return true;
							}
						}
						if (replayed) {
							return false;
						}
						// 客户端任务列表只会渲染**客户端 data_driven_quest.xml 里挂在该 NPC 名下的行**；
						// 少数任务（19678/19679 这对外服成长行）原版客户端数据里根本没有 DD 行，列表
						// 永远列不出来，玩家既看不到行、也没有可点的确认按钮（SET_SUCCEED 只能由阶段页发出）。
						// 于是这类行的交付条件一旦满足就永远停在 START：头顶没有可交付标记、没有奖励窗
						// （2026-10-20 实机：19679 推了页 1011 但无标记）。兜底＝在开门时替玩家走完那条
						// 「本 NPC 的 TALK 转换 → REWARD」，把任务送进可交付态；此后开门由上面的 REWARD
						// 重放（npc-complete 的 preview）打开奖励窗。
						// 门：①不在原版 DD 表 ⇒ 客户端清单必然列不出（客户端表 ⊂ 服务端表，已实测
						// 2155/2510）；②该转换的目标节点投影为 REWARD、且玩家当前节点投影正好是它的来源
						// （按状态 + 打包变量逐位比对，与规划器同口径），兄弟任务互不遮挡；③一次开门只推进
						// 一条，取 id 最小者；④交付按钮在更后面一页上的步不走兜底（客户端页链：入口页 →
						// 续页 → 交付按钮），那种步只能由玩家点完（实机 2026-10-22：点 Jucleas 无弹窗无影片，
						// 任务直接落到 Reward 30）。扣物/给物等动作由引擎按真实快照复核，不满足即零副作用。
						// The client task list only renders rows the client's data_driven_quest.xml binds to
						// this npc; a few rows (the 19678/19679 field-growth pair) have no DD row at all, so
						// they can never be listed and the player also has no confirm button to press
						// (SET_SUCCEED only comes from the stage page). Such a row therefore sits in START
						// forever once its delivery condition is met: no hand-in marker and no reward window
						// (live 2026-10-20: page 1011 was pushed but no marker appeared). The fallback walks
						// the npc's TALK transition into REWARD on the npc open, putting the quest into the
						// deliverable state; the REWARD replay above (npc-complete's preview) then opens the
						// reward window. Gates: the row must be absent from the retail DD table (the client
						// table is a subset, measured 2155/2510), the transition must project to REWARD with
						// the player's current node projecting exactly onto its source (status plus packed
						// variables, the planner's own rule, so siblings never block each other), one open
						// advances one row (lowest id), and a step whose delivery button sits on a later page
						// (a client page chain: entry page → sub-page → delivery button) never takes the
						// fallback — the player has to walk it (live 2026-10-22: no dialog window and no movie
						// at Jucleas, the quest jumped straight to Reward 30). Item actions are revalidated by
						// the engine against the real snapshot, so an unmet one has zero side effects.
						UnlistedDelivery delivery = unlistedDeliveryTransition(player, npcId);
						if (delivery != null
								&& engine.onDialog(new QuestEnv(npc, player, delivery.questId(),
									((QuestEvent.TalkToNpc) delivery.transition().event()).dialogId()))) {
							env.setQuestId(delivery.questId());
							// 推进成功后直接重放开门：REWARD 的 preview 路由会开出奖励窗，玩家一次点击
							// 就到达交付面（2026-10-20 实机：只推进会 close-dialog，要再点一次才见奖励窗）。
							// 重放失败不影响已提交的推进，交回调用方走常规链路。
							// Replay the open right after the advance: the REWARD preview route opens the
							// reward window, so one click reaches the hand-in face (live 2026-10-20: advancing
							// alone closes the dialog and needs a second click). A failed replay leaves the
							// committed advance intact and falls back to the regular path.
							if (engine.onDialog(new QuestEnv(npc, player, delivery.questId(), env.getDialogId()))) {
								return true;
							}
							return false;
						}
						return false;
					}
				}
				// 无任务上下文的**物件**打开（-1 = USE_OBJECT / 1001 = QUEST_SELECT）：按物件反查
				// SimpleTalk 接取行重放——原版物件接取 = USE_OBJECT 自环 → 携带 questId 的接取入口页。
				// 缺此重放时打开只落 AI 的两参兜底页，客户端无任务上下文渲染不出接取对话
				// （2026-10-07 实机 18645/730777：点"卡利加的竖琴架"无可接取任务）。仅限物件族：
				// 普通 NPC 的打开由客户端本地对话框驱动（页 10 任务列表 → 31），保持原行为（QE-093）。
				// Context-less **object** open (-1 / QUEST_SELECT): replay the object's SimpleTalk acquire
				// rows (the retail object accept self-loop grants the quest-carrying entry page). Without
				// it the open only reaches the context-less AI fallback page (live 2026-10-07, quest
				// 18645 / object 730777). Objects only: a plain npc's open is client-driven (page-10
				// list → 31) and stays untouched.
				boolean questObjectOpen = env.getDialogId() == QuestDialogAction.USE_OBJECT.id()
						|| env.getDialogId() == QuestDialogAction.QUEST_SELECT.id();
				if (questObjectOpen && (npc.getAi2() instanceof com.aionemu.gameserver.ai.QuestItemNpcAI2
						|| npc.getAi2() instanceof com.aionemu.gameserver.ai.quests.QuestStartItemNpcAi2)) {
					for (int acquireQuestId : SimpleTalkHandler.instance().acquireQuestIdsForNpc(npcId)) {
						if (SimpleTalkHandler.instance().routes(acquireQuestId)
								&& engine.onDialog(new QuestEnv(npc, player, acquireQuestId, env.getDialogId()))) {
							env.setQuestId(acquireQuestId);
							return true;
						}
					}
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
				var metadata = engine.questCatalog().findMetadata(requestedOwner).orElse(null);
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
	 * 一条「替玩家推进到交付态」的候选：owner + 它的交付转换。
	 * One delivery candidate: the owner plus its transition into the deliverable state.
	 */
	record UnlistedDelivery(int questId, QuestTransition transition) {
	}

	/**
	 * 返回需要在开门时替玩家推进到交付态的候选：本 NPC 上「不在原版 DD 表、且当前节点的本 NPC TALK
	 * 转换目标投影为 REWARD」的进行中任务里 id 最小的一条；没有则返回 null。
	 * <p>
	 * 一次开门只推进一条（取 id 最小者）；下一条由玩家下一次交互推进，避免同一下点击连开多个奖励窗。
	 * 交付按钮在更后面一页上的步（客户端页链）不参与：那种步的交付按钮玩家还没看到，替按等于跳过
	 * 整条对话（见 {@link #walksClientPageChain}）。
	 * <p>
	 * The delivery candidate the open should walk for the player: the lowest-id live typed quest on this npc
	 * that is absent from the retail DD table and whose current node carries a TALK transition on this npc
	 * targeting a REWARD projection; null when there is none. One open advances one row (lowest id); the next
	 * one is advanced by the player's next interaction. A step whose delivery button sits on a later page (a
	 * client page chain) never qualifies — the player has not seen that button yet, so pressing it for them
	 * skips the whole dialog (see {@link #walksClientPageChain}).
	 */
	UnlistedDelivery unlistedDeliveryTransition(Player player, int npcId) {
		QuestRuntimeDispatcher typed = engine.productionDispatcher();
		UnlistedDelivery best = null;
		for (QuestState state : player.getQuestStateList().getAllQuestState()) {
			int questId = state.getQuestId();
			if (state.getStatus() != QuestStatus.START || !typed.owns(questId)
					|| DataDrivenNativeRuntime.instance().inRetailTable(questId)
					|| (best != null && questId >= best.questId())) {
				continue;
			}
			QuestTransition delivery = deliveryTransitionOf(typed, player, questId, npcId);
			if (delivery != null) {
				best = new UnlistedDelivery(questId, delivery);
			}
		}
		return best;
	}

	/**
	 * 本 NPC 上「来源节点等于玩家当前节点、目标节点投影为 REWARD」的 TALK 转换；没有则返回 null。
	 * 节点匹配用状态 + 打包变量逐位比对（与 {@code QuestMutationPlanner.matchesSourceNode} 同口径），
	 * 所以同在栏的兄弟任务不会互相挡住。交付按钮在更后面一页上的步（客户端页链）不参与——
	 * 那种步只能由玩家点行走完，见 {@link #walksClientPageChain}。
	 * <p>
	 * The TALK transition on this npc whose source node projects exactly onto the player's current state and
	 * whose target projects to REWARD; null when there is none. The node check compares status and packed
	 * variables exactly like {@code QuestMutationPlanner.matchesSourceNode}, so siblings never block. A step
	 * whose delivery button sits on a later page (a client page chain) never qualifies — the player has to
	 * walk it; see {@link #walksClientPageChain}.
	 */
	private QuestTransition deliveryTransitionOf(QuestRuntimeDispatcher typed, Player player, int questId,
			int npcId) {
		CompiledQuestDefinition definition = typed.catalogRegistry().findExecutable(questId).orElse(null);
		QuestState state = player == null ? null : player.getQuestStateList().getQuestState(questId);
		if (definition == null || state == null) {
			return null;
		}
		Map<String, Integer> current = definition.definition().progressLayout()
			.unpack(state.getQuestVars().getQuestVars());
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.definition().nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		for (QuestTransition transition : definition.transitionsFor("TALK_TO_NPC")) {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
					|| talk.npcId() != npcId || talk.dialogId() == null
					|| transition.sourceNode() == null
					|| statuses.get(transition.targetNode()) != QuestStatus.REWARD) {
				continue;
			}
			QuestNode source = definition.definition().nodes().stream()
				.filter(node -> node.label().equals(transition.sourceNode()))
				.findFirst().orElse(null);
			if (source != null && source.projection().status() == state.getStatus()
					&& source.projection().variables().entrySet().stream()
						.allMatch(entry -> entry.getValue().equals(current.get(entry.getKey())))
					&& !walksClientPageChain(definition, transition, npcId)) {
				return transition;
			}
		}
		return null;
	}

	/**
	 * 该步在客户端是否要走页链才能到交付按钮：同一来源节点、同一 NPC 上另有一条「子页续页」路由
	 * （动作 id = 客户端续页动作，如 {@code SELECT3_1}，且回发该子页），说明交付按钮在更后面一页上
	 * （1007 的 Jucleas：select3 → select3_1 → SETPRO3）。那种步必须由玩家点完对话页与影片，
	 * 服务端替按最后的按钮等于替玩家跳过整条对话（实机 2026-10-22：点 Jucleas 无弹窗无影片，
	 * 任务直接落到 Reward 30）。
	 * <p>
	 * Whether the step walks a client page chain before the delivery button: another route on the same source
	 * node and npc carries a selection sub-page action (the client's "keep talking" act, e.g. {@code SELECT3_1})
	 * and answers with that sub-page, so the delivery button sits on a later page (quest 1007 at Jucleas:
	 * select3 → select3_1 → SETPRO3). Such a step must be walked by the player; pressing the final button for
	 * them skips the whole dialog (live 2026-10-22: no dialog window and no movie at Jucleas, the quest jumped
	 * straight to Reward 30).
	 */
	private boolean walksClientPageChain(CompiledQuestDefinition definition, QuestTransition delivery, int npcId) {
		String source = delivery.sourceNode();
		for (QuestTransition candidate : definition.transitionsFor("TALK_TO_NPC")) {
			if (candidate == delivery || !source.equals(candidate.sourceNode())
					|| !(candidate.event() instanceof QuestEvent.TalkToNpc talk)
					|| talk.npcId() != npcId || talk.dialogId() == null
					|| !QuestDialogPage.isSelectionSubPage(talk.dialogId())) {
				continue;
			}
			boolean answersWithSubPage = candidate.afterCommit().stream()
				.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog);
			if (answersWithSubPage) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 该任务是否以当前 NPC 为接取 / 交付 / 中继节点（开门相位里的「本 NPC 的任务」）。
	 * Whether the quest is served at this npc as an acquire, hand-in or relay node.
	 */
	private static boolean ownsQuestAtNpc(int questId, int npcId) {
		SimpleTalkHandler talk = SimpleTalkHandler.instance();
		if (talk.routes(questId) && (talk.acquireNpcs(questId).contains(npcId)
				|| talk.rewardNpcs(questId).contains(npcId))) {
			return true;
		}
		SimpleHuntHandler hunt = SimpleHuntHandler.instance();
		if (hunt.routes(questId) && (hunt.acquireNpcs(questId).contains(npcId)
				|| hunt.rewardNpcs(questId).contains(npcId))) {
			return true;
		}
		Integer serialAcquire = SimpleSerialHuntHandler.instance().acquireNpc(questId);
		Integer serialReward = SimpleSerialHuntHandler.instance().rewardNpc(questId);
		if (SimpleSerialHuntHandler.instance().routes(questId)
				&& ((serialAcquire != null && serialAcquire == npcId)
					|| (serialReward != null && serialReward == npcId))) {
			return true;
		}
		return talk.routes(questId) && talk.relaysForNpc(npcId).stream()
			.anyMatch(relay -> relay.questId() == questId);
	}

	/**
	 * 分发已接取的服务端任务共享（不虚构 NPC 交互对象）。
	 * Dispatches an accepted server-issued quest share without inventing an NPC interaction object.
	 */
	void onSharedQuestDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null || env.getQuestId() <= 0) {
			return;
		}
		QuestRuntimeDispatcher typed = engine.productionDispatcher();
		if (!typed.owns(env.getQuestId())) {
			return;
		}
		typed.dispatchSharedQuestAccept(env.getPlayer().getObjectId(), env.getQuestId(), env.getDialogId());
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
		QuestRuntimeDispatcher typed = engine.productionDispatcher();
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
		QuestRuntimeDispatcher typed = engine.productionDispatcher();
		if (!typed.owns(questId)) {
			return false;
		}
		if (player.hasNpcQuestDialogSelection(npc.getObjectId(), questId)) {
			return true;
		}
		QuestMetadata metadata = typed.catalogRegistry()
			.findMetadata(questId).orElse(null);
		if (metadata != null && !"QUEST".equals(metadata.category())) {
			return true;
		}
		QuestState questState = player.getQuestStateList().getQuestState(questId);
		return questState != null && (questState.getStatus() == QuestStatus.START
			|| questState.getStatus() == QuestStatus.REWARD);
	}

	/**
	 * 判断动作是否为该 owner 的接取动作（1002 / 20000），且接取后状态仍停在接取前。
	 * Returns whether the action is this owner's accept action (1002 / 20000) and the quest stayed
	 * unaccepted, i.e. the accept itself was refused rather than some later page step.
	 */
	private static boolean isAcceptAction(Player player, int questId, Integer dialogId) {
		if (dialogId == null || !(dialogId == QuestDialogAction.QUEST_ACCEPT_1.id()
				|| dialogId == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())) {
			return false;
		}
		// 接取成功时状态已推进到 START/REWARD/COMPLETE；仍是 NONE/LOCKED 说明接取被拒。
		// A committed accept advances the state; still NONE/LOCKED means the accept was refused.
		QuestState state = player.getQuestStateList().getQuestState(questId);
		return state == null || state.getStatus() == QuestStatus.NONE
			|| state.getStatus() == QuestStatus.LOCKED;
	}
}
