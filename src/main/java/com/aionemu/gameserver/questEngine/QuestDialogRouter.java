package com.aionemu.gameserver.questEngine;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
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
					log.info(I18n.get("log.quest_trace.typed_accept_refused", player.getName(),
						requestedOwner, env.getDialogId()));
					// 资格投影只在生产 dispatcher 上可得；测试替身没有投影时退回 native 判定。
					// The projection exists only on the production dispatcher; test doubles fall back.
					QuestStartEligibility eligibility = typed instanceof QuestProductionDispatcher production
						? production.startEligibility(player.getObjectId(), requestedOwner).orElse(null)
						: null;
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
				// 右键开门（TalkEventHandler.onTalk → dialogId=-1, questId=0）。客户端任务对话树把
				// NPC 的入口分成两相：先「可接取任务」相（客户端页 1011 的入口按钮 → 页 4「领取」），
				// 全部领完后才回落「进行中任务」相（页 4 上客户端本地切页）；原版对话平面
				// （FUN_180c474b0）只在页 0/10 的事件里切阶段页，页 4/1011 上的动作由
				// User::OnHyperlinkAction 按「领取中任务」与 NPC 可接清单受理。服务端开门因此按同一
				// 结构编排：可交付（REWARD）任务 → 本 NPC 仍在发放的接取行 → 进行中（START）任务。
				// 未接取的行原样重放进接取面（接取资格与入口页全在那个面里判定）；重放全落空才进
				// 进行中、可交付任务的阶段页。
				// The right-click open: the client dialog tree offers the npc's quests in two phases —
				// acquirable rows first (client page 1011 → page 4), the live rows only after they are
				// all taken. The retail dialog plane switches stage pages only on page 0/10 events, and
				// accept-page acts are gated by the acquiring slot and the npc's acquirable list, so the
				// server opens in the same structure: deliverable rows, then the rows this npc still
				// offers, then live rows.
				// 任务行（31）已由客户端选中具体任务、自带任务上下文，不属于开门动作；只把 -1/26 与
				// 页内导航动作当开门相位处理。
				// A row selection (31) carries its own quest context; only -1/26 and page acts are opens.
				boolean npcQuestDraft = env.getDialogId() == -1 || env.getDialogId() == 26;
				if (npcQuestDraft) {
					String acquireKind = acquireFaceVerdict(player, npc, event, env.getDialogId());
					if (acquireKind != null) {
						log.debug(I18n.get("log.quest_engine.open_door_acquire_face", player.getName(), npcId,
							env.getDialogId(), acquireKind));
						return true;
					}
				}
				List<QuestState> openDoorReplay = new ArrayList<>();
				for (QuestState state : player.getQuestStateList().getAllQuestState()) {
					if (state.getStatus() == QuestStatus.REWARD) {
						openDoorReplay.add(state);
					}
				}
				boolean liveQuestOnNpc = false;
				for (QuestState state : player.getQuestStateList().getAllQuestState()) {
					if (state.getStatus() == QuestStatus.START) {
						openDoorReplay.add(state);
						liveQuestOnNpc |= ownsQuestAtNpc(state.getQuestId(), npcId);
					}
				}
				if (npcQuestDraft && liveQuestOnNpc) {
					// 进行中任务占着 NPC 对话 ⇒ 本 NPC 的可接取相已在客户端本地翻过后被再次请求：
					// 这是原版「非领取中任务、也不在可接清单」的同一形态，只留日志不推进（客户端停留
					// 在当前页，不会 load fail）。
					// The npc's live quest already claimed the dialog tree: a second ask for the acquire
					// phase on this npc is the retail "neither acquiring nor offered" shape.
					log.debug(I18n.get("log.quest_engine.open_door_acquire_face", player.getName(), npcId,
						env.getDialogId(), "ALREADY_CLAIMED"));
					return true;
				}
				for (QuestState state : openDoorReplay) {
					if (engine.onDialog(new QuestEnv(npc, player, state.getQuestId(), env.getDialogId()))) {
						env.setQuestId(state.getQuestId());
						return true;
					}
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
				// 客户端任务对话树：入口页的动作（1011，或全局页 4）是<b>页内导航</b>——客户端本地翻到
				// 页 4「领取」/「正在领取」，服务端回发同页只会把客户端重新放到同一页。原版
				// User::OnHyperlinkAction 对页 4/1011 的再接取页动作按「领取中任务」受理，「打开」页不是
				// 接取页本身，因此这里只留日志不推进（未接取行出现第二支路时同样零发页）。上面已开过
				// 接取入口页时，此支路即「关闭领取对话」的收尾。
				// The client task dialog tree: the entry-page act (1011 or the global page 4) is page-local —
				// the client turns to page 4 itself, and echoing the same page just re-renders it (the dead
				// click on multi-quest npcs). Only a page-4 act with the acquiring slot is accepted.
				if (QuestDialogAction.isSelectionContinuationAction(env.getDialogId())) {
					log.debug(I18n.get("log.quest_engine.open_door_acquire_face", player.getName(), npcId,
						env.getDialogId(), "PAGE_LOCAL"));
					return true;
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
	 * 开门相位 1：NPC 的可接取相。本 NPC 上仍在发放（原版 {@code CanAcquireQuest == 2}，与附近任务
	 * 列表同一判定）的接取行按客户端入口原样重放进接取面，由该面下发携带任务上下文的入口页
	 * （页 1011 信页 / 页 4 接取窗），并在页 4 打开时登记「领取中任务」槽位。
	 * <p>
	 * 返回非 null 表示开门已由接取相处理（值为判定类型，供调试日志）；null = 该 NPC 没有可接取的
	 * 路由行，开门交给后续相位（可交付 / 进行中任务）。
	 * <p>
	 * Open phase 1 (the npc's acquirable phase): rows this npc still offers (the same verdict as the
	 * nearby list, retail {@code CanAcquireQuest == 2}) are replayed into their acquire faces, which
	 * answer with the task-carrying entry page and record the acquiring slot on page 4.
	 *
	 * @return 判定类型，或 null（无接取行）/ a verdict label, or null when the npc offers nothing
	 */
	private String acquireFaceVerdict(Player player, Npc npc, QuestEvent event, int dialogId) {
		List<Integer> candidates = new ArrayList<>(engine.receivableQuestIdsAtNpc(player, npc.getNpcId()));
		for (int questId : candidates) {
			// 各族接取面只认「本族接取 NPC」的问询动作（Hunt 族的 31/26 即入口页）；打开（-1）在
			// Talk 族是物件自环，非物件 NPC 由路由在开门相位另行处理。
			// Each family's acquire face serves the row's own acquire npc; the open (-1) belongs to the
			// object self-loop and is handled by the open phase for plain npcs.
			int faceAct = dialogId == -1 ? QuestDialogAction.QUEST_SELECT.id() : dialogId;
			if (engine.onDialog(new QuestEnv(npc, player, questId, faceAct))) {
				return "ACQUIRE_FACE:" + questId;
			}
		}
		return candidates.isEmpty() ? null : "UNCLAIMED:" + candidates;
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
