package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.ProgressScope;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.questEngine.definition.QuestRewardGroup;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 真端 SimpleTalk 表行 → 完整任务定义（无 shell）的合成器（M3 第一批：单步形态）。
 * <p>
 * 单步形态 = 表行只有接取/报告两个 NPC（无 {@code talk_npcN} 链、无 give/remove 物品轴、无过场），
 * 对应 ScriptDLL64 的 {@code FUN_180cab520}（状态 0↔10 推进）与 {@code FUN_180caca90}（报告步）：
 * 接取 → 对话（中间 NPC 链为空）→ 报告 → 领奖；进度字段 var0 只做行号投影，
 * 领奖态取客户端任务书末行行号（QE-051）。
 * <p>
 * 合成结果与生产 DSL 的三个规范块同构：{@code npc-start}（selection-sources = 接取态与进行态）、
 * {@code npc-report}（SimpleTalk 的报告页是 SELECT5）、{@code npc-complete}。
 * <p>
 * 接取名是类别哨兵（{@code _faction_} / {@code _area_}）时走<b>系统发放形状</b>：真端没有 NPC 接取
 * （客户端只有委托书页），接取由发放子系统负责，定义只保留一条 {@code SystemGrant} 边 + 报告 + 领奖；
 * 报告名是 {@code <地图>_<势力名>} 复合引用时，交付 NPC 集取客户端任务书 dic 链登记
 * （{@code quest_client_reward_npcs.tsv}）。其它形态（对话链 / 物品轴 / 过场 / 无发放入口的哨兵）
 * 返回稳定拒绝码，继续走 XML 降级。
 * Synthesizes the single-step retail SimpleTalk shape plus the system-grant shape; other shapes get
 * stable rejection codes.
 */
public final class RetailSimpleTalkDefinitionCompiler {

	private RetailSimpleTalkDefinitionCompiler() {
	}

	/** 编译结果。 / Compilation outcome. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 编译一行：{@code exits} 提供真端模板表没有列出的客户端对话续页（{@code SELECT1_1} 等），
	 * {@code summaryRows} 提供真端表同样没有的任务书行数（REWARD 投影 = 末行行号，QE-051）。
	 * Compiles one row; {@code exits} supplies the client dialog continuations and {@code summaryRows}
	 * the client journal row counts the retail table cannot express.
	 */
	public static Outcome compile(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			RetailQuestMetadataCompiler.Outcome metadata, RetailClientDialogExits exits,
			RetailClientSummaryRows summaryRows, RetailClientRewardNpcs clientRewardNpcs,
			RetailClientTalkChainSteps chainSteps) {
		return compile(entry, index, metadata, exits, summaryRows, clientRewardNpcs,
			RetailQuestUseItemNpcs.empty(), chainSteps);
	}

	/**
	 * 带交互物登记的编译入口（P0c-22）：真端 drop_monster 解析为交互物（useitem npc）时，
	 * 定义必须携带 START 态 ACTION_ITEM_USE 门（启动期交互对象合同，判例 18509/28509）。
	 * Compiles one row with the interaction-object registry (P0c-22): retail drops that resolve
	 * to useitem objects require the START-state ACTION_ITEM_USE gates.
	 */
	public static Outcome compile(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			RetailQuestMetadataCompiler.Outcome metadata, RetailClientDialogExits exits,
			RetailClientSummaryRows summaryRows, RetailClientRewardNpcs clientRewardNpcs,
			RetailQuestUseItemNpcs interactionObjects, RetailClientTalkChainSteps chainSteps) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(index, "index");
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(exits, "exits");
		Objects.requireNonNull(summaryRows, "summaryRows");
		Objects.requireNonNull(clientRewardNpcs, "clientRewardNpcs");
		Objects.requireNonNull(chainSteps, "chainSteps");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		Outcome blocked = precheck(entry, index, metadata.metadata(), clientRewardNpcs, chainSteps);
		if (blocked != null) {
			return blocked;
		}
		try {
			QuestDefinition definition = !entry.singleStep()
				? buildChain(entry, index, chainSteps, metadata.metadata(), exits, interactionObjects,
				clientRewardNpcs)
				: build(entry, index, metadata.metadata(), summaryRows, clientRewardNpcs,
					interactionObjects);
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 迁移判据。拒绝码（稳定，供保留清单消费）：
	 * <ul>
	 * <li>{@code RETAIL_ACQUIRE_NPC_*} / {@code RETAIL_REWARD_NPC_*}：接取/报告 NPC 名字解析不到或不唯一；</li>
	 * <li>{@code RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH}：类别哨兵已知但本服没有对应发放入口；</li>
	 * <li>{@code RETAIL_REWARD_NPC_FACTION_COMPOSITE}：复合势力报告名在客户端登记表里没有交付 NPC 集；</li>
	 * <li>{@code RETAIL_TALK_CHAIN}：行带 {@code talk_npcN} 对话链（{@code FUN_180cabb10} 状态链，后续切片）；</li>
	 * <li>{@code RETAIL_TALK_ITEM}：行带 give/remove 物品轴；</li>
	 * <li>{@code RETAIL_TALK_CUTSCENE}：行带过场。</li>
	 * </ul>
	 * Migration pre-check with stable rejection codes.
	 */
	private static Outcome precheck(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			QuestMetadata metadata, RetailClientRewardNpcs clientRewardNpcs,
			RetailClientTalkChainSteps chainSteps) {
		Outcome acquired = requireAcquire(index, entry);
		if (acquired != null) {
			return acquired;
		}
		Outcome reward = requireNpc(index, entry.rewardNpc(), "REWARD");
		if (reward != null) {
			// 系统发放行的报告名可以是 {@code <地图>_<势力名>} 复合引用（不是 NPC 名）：交付 NPC 集
			// 取客户端任务书 dic 链登记；登记缺失才算拒绝。
			// Composite reward references resolve through the client quest-letter dic chain registry.
			if (RetailQuestMetadataCompiler.isFactionComposite(entry.rewardNpc())) {
				if (clientRewardNpcs.rewardNpcs(entry.questId()).isEmpty()) {
					return new Outcome(null, "RETAIL_REWARD_NPC_FACTION_COMPOSITE", entry.rewardNpc());
				}
			} else {
				return reward;
			}
		}
		if (!entry.singleStep()) {
			// wave A 链式路径（P0c-10f）：链 NPC 逐个判定；复合轴留 wave B；登记表缺路由 = 残组。
			// Wave-A chain path: per-NPC gate; compound axes stay deferred; missing routes = residual.
			for (String talkNpc : entry.talkNpcs()) {
				Outcome talkGate = requireNpc(index, talkNpc, "TALK");
				if (talkGate != null) {
					return talkGate;
				}
			}
			if (entry.givesItem() || entry.removesItem() || entry.cutscene() || entry.itemCheck()) {
				// wave B（P0c-10h）：物品轴复合行若已被登记表逐字转写（路由在案）则放行——
				// 登记表是转写事实源；cutscene / con_quest 行不在册，维持拒绝留独立裁定波。
				// Wave-B: item-axis rows covered by the registry proceed; cutscene/con_quest stay out.
				boolean registryCovered = chainSteps.has(entry.questId())
					&& !chainSteps.routes(entry.questId()).isEmpty();
				if (!registryCovered) {
					return new Outcome(null, "RETAIL_TALK_CHAIN_COMPOUND", "talk=" + entry.talkNpcs());
				}
			}
			// P0c-32：接取入口两形——规范 NPC_START 块（块合成接取流）或**逐字接取路由**
			// （登记表 R/Q 记录里 unaccepted→started 的入口，如 QUEST_ACCEPT_1/ACCEPT_SIMPLE/
			// SETPRO1；buildChain 的逐字回放已覆盖后者，无需合成）。1479 形（纯规范块、零 R 记录）
			// 由 NPC_START 背书放行 NO_ROUTES；19070/19071 形（零接取入口）维持拒绝。
			// P0c-32: two acceptance-entrance shapes — a canonical NPC_START block (block
			// synthesis) or verbatim accept routes (R/Q records entering unaccepted->started,
			// already covered by buildChain's replay). Rows with neither stay rejected.
			boolean canonicalStart = chainSteps.block(entry.questId(), "NPC_START").isPresent();
			boolean acceptEntrance = chainSteps.routes(entry.questId()).stream()
				.anyMatch(route -> "unaccepted".equals(route.source()) && "started".equals(route.target()));
			if (entry.grantKind().systemGrant()
					&& chainSteps.block(entry.questId(), "NPC_START").isPresent()) {
				// P0c-10h B-7：系统发放链行合法（npc-start 块不再合成接取流，unaccepted 源路由
				// 在 buildChain 剔除——P0c-2 形状合同）。
			} else if (!entry.grantKind().systemGrant() && !canonicalStart && !acceptEntrance) {
				return new Outcome(null, "RETAIL_TALK_CHAIN_NO_START", "no NPC_START block recorded");
			}
			if (!chainSteps.has(entry.questId()) && !canonicalStart) {
				return new Outcome(null, "RETAIL_TALK_CHAIN_NO_ROUTES",
					String.join(",", entry.talkNpcs()));
			}
			return null;
		}
		if (entry.givesItem() || entry.removesItem()) {
			// P0c-10m：单步行仅 give_item（无编号接取发物，无 remove/无 talk 链）→ 接取时发物，
			// 走 work_items 通道（判例 1131）；带 remove_itemN 或 talk_npcN 的行仍留链式波。
			// Single-step rows with only the unnumbered give_item grant at accept time via work-items.
			boolean singleStepGrantOnly = entry.singleStep() && !entry.removesItem()
				&& entry.giveItemSymbol() != null;
			if (!singleStepGrantOnly) {
				return new Outcome(null, "RETAIL_TALK_ITEM", "give=" + entry.givesItem() + " remove=" + entry.removesItem());
			}
		}
		if (entry.cutscene()) {
			// P0c-10n：单步发物行的过场轴（10k 判例扩展）——movie id = 真端表 cutsceneid1，
			// 触发动作限于 canonical 路由（1009=SELECT_QUEST_REWARD 报告页 / 1007=ASK_QUEST_ACCEPT
			// 接取页，判例 3020 同轴）；1009 与 item_check 组合未见于真端行集，保持拒绝。
			// 老 XML 从未实现过场（1941 系 28 行的 craft 语义与 13 行纯过场皆无 play-movie 元素），
			// 合成是补真端语义（非等价路线）；craft 行经裁定层保留 XML（真端无 craft 轴来源）。
			// Chain/portal rows (13800: no trigger) and other triggers stay rejected.
			boolean grantable = entry.singleStep() && !entry.removesItem() && entry.giveItemSymbol() != null;
			int trigger = entry.cutsceneTrigger();
			boolean supported = grantable && (trigger == QuestDialogAction.SELECT_QUEST_REWARD.id()
				? !entry.itemCheck()
				: trigger == QuestDialogAction.ASK_QUEST_ACCEPT.id());
			if (!supported) {
				return new Outcome(null, "RETAIL_TALK_CUTSCENE",
					"movie=" + entry.cutsceneMovieId() + " trigger=" + trigger);
			}
		}
		if (entry.itemCheck() && metadata.itemRequirements().isEmpty()) {
			// P0c-10o：item_check=1 且无 collect_item 时，校验目标 = 接取发放的工作物品
			// （quest_work_item1 与 give_item 同物，判例 3204/80269）；客户端 select5 页无检查按钮
			// （页链无 select6）→ 门落在 SELECT_QUEST_REWARD 路由上而非 CHECK 按钮。
			// 两通道皆不可解才拒绝。
			boolean workItemResolvable = entry.singleStep() && !entry.removesItem()
				&& entry.giveItemSymbol() != null
				&& RetailQuestWorkItems.first(entry.giveItemSymbol(), entry.questId()) != null;
			if (!workItemResolvable) {
				return new Outcome(null, "RETAIL_ITEM_CHECK_UNRESOLVED",
					"item_check=1 但真端 quest.xml 未声明 collect_item 且工作物品通道不可解");
			}
		}
		return null;
	}

	/**
	 * 接取名判定：普通名走唯一性门；类别哨兵放行 {@code _faction_}（阵营日常轮换）与 {@code _area_}
	 * （世界 quest_area）——两者在本服都有发放入口；{@code _challengetask_}（挑战任务）自 P0c-12
	 * 起按真端形状受理：老 XML 实证接取 NPC = 交付 shugo 本人（60 行 accept==reward，判例 17100：
	 * accept 831222 == reward Town_shugo_Worker_05），因此哨兵行落到交付 NPC 的对话接取
	 * （非系统发放边）。未知哨兵仍拒绝。
	 * Acquire-name gate: plain names must resolve uniquely; the two sentinels with a live grant entry
	 * (faction rotation, quest area) pass; challenge-task rows take the retail shape evidenced by the
	 * legacy XML — the accepting NPC is the hand-in shugo itself (accept==reward across all 60 rows) —
	 * so the sentinel resolves to the reward NPC's dialog accept, not a system-grant edge. Unknown
	 * sentinels stay rejected.
	 */
	private static Outcome requireAcquire(RetailNpcNameIndex index, RetailSimpleTalkTable.Entry entry) {
		RetailGrantKind kind = entry.grantKind();
		if (kind == RetailGrantKind.CHALLENGE_TASK) {
			return requireNpc(index, entry.rewardNpc(), "ACQUIRE");
		}
		if (!kind.systemGrant()) {
			return requireNpc(index, entry.acquiredNpc(), "ACQUIRE");
		}
		if (kind.grantable()) {
			return null;
		}
		String raw = entry.acquiredNpc() == null ? "" : entry.acquiredNpc().trim();
		return new Outcome(null, kind == RetailGrantKind.CHALLENGE_TASK
			? "RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH" : "RETAIL_ACQUIRE_NPC_SENTINEL", raw + " -> " + kind);
	}

	/** 单个 NPC 名判定：唯一 → 放行；空集/多解按名字形态归类。 / Single-NPC gate. */
	private static Outcome requireNpc(RetailNpcNameIndex index, String name, String role) {
		Set<Integer> ids = index.resolveAll(List.of(name == null ? "" : name)).npcIds();
		if (ids.size() == 1) {
			return null;
		}
		String raw = name == null ? "" : name.trim();
		if (ids.size() > 1) {
			return new Outcome(null, "RETAIL_" + role + "_NPC_AMBIGUOUS", raw + " -> " + ids);
		}
		String kind = raw.length() > 2 && raw.startsWith("_") && raw.endsWith("_")
			? "_NPC_SENTINEL" : "_NPC_UNRESOLVED";
		return new Outcome(null, "RETAIL_" + role + kind, raw);
	}

	private static QuestDefinition build(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			QuestMetadata metadata, RetailClientSummaryRows summaryRows,
			RetailClientRewardNpcs clientRewardNpcs, RetailQuestUseItemNpcs interactionObjects) {
		// 系统发放形状（P0c-2）：接取名是类别哨兵（_faction_ / _area_）时真端没有 NPC 接取，
		// 定义不生成接取路由，只留一条 SystemGrant 边供发放子系统分发（阵营日常轮换 / 进区域）。
		// System-grant shape: sentinel acquire names have no NPC accept in retail; the definition keeps a
		// single SystemGrant edge and lets the granting subsystem start the quest.
		// P0c-12 例外：挑战任务哨兵走对话接取（受理 = 交付 shugo 本人），非系统发放边。
		boolean systemGrant = entry.grantKind().systemGrant()
			&& entry.grantKind() != RetailGrantKind.CHALLENGE_TASK;
		// P0c-12：挑战任务哨兵行受理 NPC = 交付 shugo 本人（老 XML 判例 17100 同体实证）——
		// 非系统发放边，走对话接取。
		int acquiredNpc = systemGrant ? -1
			: index.resolveAll(List.of(entry.grantKind() == RetailGrantKind.CHALLENGE_TASK
				? entry.rewardNpc() : entry.acquiredNpc())).npcIds().iterator().next();
		// 交付 NPC 集：唯一名解析优先；复合势力引用（系统发放行）走客户端任务书 dic 链登记。
		// Hand-in NPCs: unique name resolution first; composite faction references use the client registry.
		Set<Integer> resolvedReward = index.resolveAll(List.of(entry.rewardNpc())).npcIds();
		List<Integer> rewardNpcs = resolvedReward.size() == 1
			? List.of(resolvedReward.iterator().next())
			: clientRewardNpcs.rewardNpcs(entry.questId());
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		// 领奖投影 = 客户端任务书末行行号（QE-051）：单行任务为 0，两行任务（如 80316 交付型）为 1。
		// Reward projection = the last client journal row index; the retail table carries no row counts.
		Map<String, Integer> rewardRow = Map.of("var0", summaryRows.lastRowIndex(entry.questId()));
		List<QuestNode> nodes = new ArrayList<>(4);
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)));
		nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, zero)));
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));

		List<QuestTransition> transitions = new ArrayList<>();
		if (!systemGrant) {
			// S1 规范形（quest-native-dispatch）：接取段换 canonicalAcceptFlow——QUEST_SELECT 直发接取窗
			// （页 4）、1002/20000 两形提交（带单步 give_item 接取发物，P0c-10m 通道不变）、拒绝族、
			// FINISH_DIALOG→任务列表页；select1 入口页、ASK_QUEST_ACCEPT(1007) 中转与 SELECT1_1 续页梯
			// 随页链整体退场。过场轴重挂（cs1_haction=1007，判例 3020/4056）：canonical 形没有 1007 路由，
			// PlayMovie 改挂到"下发接取窗的那条 QUEST_SELECT 边"，匹配键带 source 节点。
			// The S1 canonical accept flow; the 1007-triggered cutscene re-attaches to the ask-window
			// QUEST_SELECT edge with a source-node-scoped key.
			transitions.addAll(attachMovieToRoute(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(
				acquiredNpc, "started", acceptGiveItemActions(entry)), "unaccepted", acquiredNpc,
				QuestDialogAction.QUEST_SELECT.id(),
				entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
					? entry.cutsceneMovieId() : -1));
		} else {
			// 系统发放边：定义图在无接取路由时仍从 NONE 连通到 START；发放服务（阵营日常轮换等）
			// 后续显式分发 {@link QuestEvent.SystemGrant}，或按 RetailAreaEngine 先例直启。
			// The system-grant edge keeps the definition graph connected without accept routes.
			transitions.add(new QuestTransition(new QuestEvent.SystemGrant(),
				List.of(new QuestCondition.StartEligible()), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		}
		// P0c-22：交互物门（启动期交互对象合同，判例 18509/28509）——真端 drop_monster 解析为
		// 交互物（useitem npc，如 IDNovice_WoodenBox=700853）时，与 CollectItem 规范形同构发射
		// USE_OBJECT + CanAct(ACTION_ITEM_USE) started 自环（空载荷）；命名怪掉落不受影响。
		// P0c-22: interaction-object gates (startup interaction contract) — retail drops that
		// resolve to useitem objects (e.g. IDNovice_WoodenBox=700853) emit the USE_OBJECT +
		// CanAct(ACTION_ITEM_USE) started self-loop pair, isomorphic to the CollectItem shape;
		// named-monster drops are unaffected.
		for (int objectId : new TreeSet<>(metadata.drops().stream()
				.map(QuestDrop::npcId)
				.filter(interactionObjects::isInteractionObject)
				.toList())) {
			transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(objectId), List.of(), List.of(),
				"started", List.of(), null, "started"));
			transitions.add(new QuestTransition(new QuestEvent.CanAct(objectId, "ACTION_ITEM_USE"), List.of(),
				List.of(), "started", List.of(), null, "started"));
		}
		for (int rewardNpc : rewardNpcs) {
			// S1 规范形交付（quest-native-dispatch）：QUEST_SELECT 带整组 HasItem 门直翻 REWARD 并下发
			// 档位奖励窗（QE-028 查表，零奖励组兜底固定窗 1）；报告页 SELECT5、39/20002 双按钮对与
			// select6 失败页随页链退场——未集齐时零路由，关窗兜底交 DialogService。
			// 三子形按真端轴分派（P0c-10o 语义保持）：item_check 且真端声明 collect_item = 整组门；
			// item_check 且无 collect_item = 工作物品门（门 = 接取发放的 give_item 同物）；无 item_check = 空门。
			// 过场轴重挂（cs1_haction=1009）：PlayMovie 插到"下发交付窗的那条 QUEST_SELECT 边"的开窗之前
			// （after 序与老 craft 行一致：Sync → PlayMovie → 开窗）。
			// The S1 canonical delivery keeps the three P0c-10o gate shapes and re-attaches the
			// 1009-triggered cutscene to the delivery-window QUEST_SELECT edge.
			List<QuestItemRequirement> handIn = List.of();
			if (entry.itemCheck()) {
				handIn = metadata.itemRequirements().isEmpty()
					? List.of(workItemRequirement(entry)) : metadata.itemRequirements();
			}
			List<QuestCondition> hasItems = handIn.stream()
				.<QuestCondition>map(item -> new QuestCondition.HasItem(item.itemId(), item.count())).toList();
			List<QuestAction> removeItems = handIn.stream()
				.<QuestAction>map(item -> new QuestAction.RemoveItem(item.itemId(), item.count())).toList();
			transitions.addAll(attachMovieToRoute(List.of(RetailSimpleCollectItemDefinitionCompiler
				.canonicalDelivery(rewardNpc, hasItems, removeItems,
					RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, entry.questId()),
					"started")), "started", rewardNpc, QuestDialogAction.QUEST_SELECT.id(),
				entry.cutsceneTrigger() == QuestDialogAction.SELECT_QUEST_REWARD.id()
					? entry.cutsceneMovieId() : -1));
			transitions.addAll(reportNpcExit(systemGrant, acquiredNpc, rewardNpc));
		}
		transitions.addAll(completeFlow(metadata, rewardNpcs));
		// P0c-28：旧存档自愈边（armour 方向，登记驱动）——单步行族 XML 时代存档停在 1 基行号、
		// 真端投影在 0 基末行，不修复则领奖书页落在不存在的行（判例 80290/80294；链路径 weapon
		// 方向同源通道在 buildChain journalRowRepair）。登记与投影不一致即拒绝编译（fail-closed）。
		// P0c-28: registry-driven legacy-save heal edge (armour direction) — XML-era saves stop on
		// 1-based rows while the retail projection sits on the 0-based last row; the chain path's
		// weapon-direction sibling lives in buildChain. Registry/projection mismatch rejects the row.
		RetailLegacySaveHealRows.HealEdge heal = RetailLegacySaveHealRows.forQuest(entry.questId());
		if (heal != null) {
			if (heal.rewardRow() != rewardRow.get("var0")) {
				throw new IllegalStateException("legacy heal registry row " + entry.questId()
					+ " disagrees with the journal projection " + rewardRow);
			}
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", heal.staleRow())),
				List.of(new QuestAction.SetVariable("var0", heal.rewardRow())), "reward",
				List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), null, null));
		}
		return new QuestDefinition(entry.questId(), 1, metadata, layout, List.copyOf(nodes),
			List.copyOf(transitions));
	}

	/**
	 * S2 策略 A（quest-native-dispatch）：双块规范段行（同时具备 NPC_START 与 NPC_REPORT 块）在块 NPC 上
	 * 已由规范段接管的登记动作（符号口径，登记表 action 列）——接取段词表与交付段词表。
	 * S2 strategy A: registry actions retired on canonical-segment NPCs (symbol caliber).
	 */
	private static final Set<String> CHAIN_ACCEPT_RETIRED = Set.of(
		"ASK_QUEST_ACCEPT", "SELECT1", "SELECT1_1", "SELECT1_1_1",
		"QUEST_ACCEPT_1", "QUEST_ACCEPT_SIMPLE",
		"QUEST_REFUSE_1", "QUEST_REFUSE_2", "QUEST_REFUSE_SIMPLE");

	private static final Set<String> CHAIN_DELIVERY_RETIRED = Set.of(
		"SELECT_QUEST_REWARD", "CHECK_USER_HAS_QUEST_ITEM", "CHECK_USER_HAS_QUEST_ITEM_SIMPLE",
		"SELECT5", "SELECT6");

	/** S3c-D 的 R-REP 退场对象页集（裁定 ①(iii)：**只认页下发**，不认动作词表——动作词表会打掉 reward 态
	 * 重开载体，判例 4970/35025）。 / R-REP retirement pages (page criterion only). */
	private static final Set<String> R_REP_REPORT_PAGES = Set.of("SELECT5", "SELECT6");

	/** 交付段退场页：任何记录下发这些页即旧页链残留（与家族门 pageChain 判据同口径）。 / Retired delivery pages. */
	private static final Set<String> CHAIN_RETIRED_PAGES = Set.of(
		"SELECT1", "SELECT1_1", "SELECT1_1_1", "SELECT5", "SELECT6");

	/**
	 * S2 策略 A：该登记记录是否随规范段退场——接取词表只在接取 NPC 上退场、交付词表与退场页只在交付 NPC 上
	 * 退场；中段 NPC 的记录（简报页、SETPRO 阶梯、交互物）逐字保留，仍参与显式路由覆盖与同键去重。
	 * Whether the registry route retires with the canonical segments (scoped per segment NPC).
	 */
	private static boolean retiredChainRoute(RetailClientTalkChainSteps.RouteRecord route,
			Set<Integer> acceptNpcs, Set<Integer> reportNpcs) {
		if (acceptNpcs.contains(route.npcId())
				&& (CHAIN_ACCEPT_RETIRED.contains(route.action())
					// 判例 1914/1915/1916：接取段由登记记录驱动（无 1002/20000）时，
					// `SETPRO1 unaccepted→started` 与 canonical 的 QUEST_ACCEPT_SIMPLE(20000) 边逐字同形
					// （StartEligible + Sync(VISIBILITY_REFRESH) + Close），且其按钮页（select1_1）随页梯
					// 退场 ⇒ 随段退场；其余源的 SETPRO1 是阶段推进，保留。
					// The accept-segment SETPRO1 commit is isomorphic to the canonical 20000 edge and retires.
					|| ("SETPRO1".equals(route.action()) && "unaccepted".equals(route.source())))) {
			return true;
		}
		return reportNpcs.contains(route.npcId())
			&& (CHAIN_DELIVERY_RETIRED.contains(route.action())
				|| !Collections.disjoint(chainPushedPages(route.afterCommits()), CHAIN_RETIRED_PAGES));
	}

	/**
	 * S3b 接取段载荷覆盖守卫：退场记录的动作侧载荷（`GIVE_ITEM`/`REMOVE_ITEM`）必须被**规范接取边**
	 * （source = `unaccepted`）的动作覆盖，否则静默丢物（判例 21136：提交记录上的
	 * `GIVE_ITEM:182207919:1` 由 canonicalAcceptFlow 的 acceptActions 承接；不被覆盖即拒绝编译）。
	 * Accept-side payload coverage: retired action payloads must be carried by the canonical accept edges.
	 */
	private static void assertAcceptPayloadCovered(int questId,
			List<RetailClientTalkChainSteps.RouteRecord> retiredRoutes, List<QuestTransition> acceptEdges) {
		Map<String, Integer> canonical = new java.util.TreeMap<>();
		for (QuestTransition edge : acceptEdges) {
			if (!"unaccepted".equals(edge.sourceNode())) {
				continue;
			}
			for (QuestAction action : edge.actions()) {
				if (action instanceof QuestAction.GiveItem give) {
					canonical.merge("GIVE:" + give.itemId(), give.count(), Math::max);
				} else if (action instanceof QuestAction.RemoveItem remove) {
					canonical.merge("REMOVE:" + remove.itemId(), remove.count(), Math::max);
				}
			}
		}
		for (RetailClientTalkChainSteps.RouteRecord route : retiredRoutes) {
			for (String token : route.actions().split(";")) {
				String[] parts = token.trim().split(":");
				if (parts.length != 3 || !("GIVE_ITEM".equals(parts[0]) || "REMOVE_ITEM".equals(parts[0]))) {
					continue;
				}
				int count = Integer.parseInt(parts[2]);
				Integer covered = canonical.get(parts[0].startsWith("GIVE") ? "GIVE:" + parts[1] : "REMOVE:" + parts[1]);
				if (covered == null || covered < count) {
					throw new IllegalStateException("canonical accept loses the retired registry payload: quest "
						+ questId + " action " + token);
				}
			}
		}
	}

	/** 层 A（S3b）：该登记记录的键（source, npc, dialogId）是否属于规范边键集。 / Whether the registry route
	 * shares a canonical edge key. */
	private static boolean canonicalKeyRoute(RetailClientTalkChainSteps.RouteRecord route, Set<String> canonicalKeys) {
		Integer dialogId = tryParseDialogAction(route.action());
		return dialogId != null
			&& canonicalKeys.contains(route.source() + ":" + route.npcId() + ":" + dialogId);
	}

	/** 宽松解析：非枚举且非数字的令牌（区间 `A..B`、`ACTION_ITEM_USE` 等）返回 null。 /
	 * Lenient variant for range and channel tokens. */
	private static Integer tryParseDialogAction(String token) {
		try {
			return parseDialogAction(token);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** 记录下发的任务页常量集（只认 {@code DIALOG:SHOW_QUEST_PAGE:} 通道）。 / Quest page pushes of a route. */
	private static Set<String> chainPushedPages(String afterCommits) {
		Set<String> pages = new java.util.HashSet<>();
		for (String token : afterCommits.split(";")) {
			String trimmed = token.trim();
			if (trimmed.startsWith("DIALOG:SHOW_QUEST_PAGE:")) {
				pages.add(trimmed.substring("DIALOG:SHOW_QUEST_PAGE:".length()));
			}
		}
		return pages;
	}

	/**
	 * 退场记录的 fail-closed 载荷守卫（S2）：只允许物品门载荷（{@code HAS_ITEM} 条件 / {@code REMOVE_ITEM}
	 * 动作，由规范交付边承接）与过场令牌（{@code MOVIE}，必须与真端 cutsceneid1 一致且触发轴属重挂支持集）；
	 * 其余条件/动作一律拒绝编译——防止静默丢门或丢片。
	 * Fail-closed payload guard for retired routes: only item-gate payload and the re-attachable cutscene token.
	 */
	private static void assertRetiredChainRouteQuiet(RetailSimpleTalkTable.Entry entry,
			RetailClientTalkChainSteps.RouteRecord route, Set<String> canonicalSources,
			Map<String, QuestNode> nodeByLabel, Set<String> carriedDeliveryGates,
			Set<String> carriedDeliveryActions) {
		for (String token : route.conditions().split(";")) {
			String trimmed = token.trim();
			if (trimmed.isEmpty() || trimmed.equals("-") || trimmed.startsWith("HAS_ITEM:")
					|| trimmed.equals("START_ELIGIBLE")) {
				continue;
			}
			// 守卫加固片 G-4：阶段门（VAR_IS/VAR_AT_LEAST）可随段退场，但必须被**某条规范边的源节点投影**
			// 蕴含（节点投影 == 快照变量，见 QuestMutationPlanner.matchesSourceNode）——否则该门在规范形下
			// 消失（"简报前即可领奖"的 premature），且 QuestPrematureRewardRouteAudit **看不见**：
			// 它的候选要求客户端页含该 dialogId，而规范边动作是 31、在客户端页动作集 0 命中。
			// 不被蕴含即拒绝编译，交裁定（判例 35010/35018/35024/45011/45025 的 `VAR_IS:var0=1 @ started`）。
			// Guard hardening G-4: retired stage gates must be anchored by a canonical edge's source node.
			if (trimmed.startsWith("VAR_IS:") || trimmed.startsWith("VAR_AT_LEAST:")) {
				// S3c 被承担分支：门被规范交付边的 conditions **逐字承接**（A 形记录自身即承接载体）——
				// 判据是集合成员（token 逐字相等），不是"存在某条边"的模糊允许。
				// Carried branch: the gate token is verbatim carried by a canonical delivery edge.
				if (carriedDeliveryGates.contains(trimmed)) {
					continue;
				}
				if (!isImpliedByAnyCanonicalSource(trimmed, canonicalSources, nodeByLabel)) {
					throw new IllegalStateException("chain retired stage gate is not anchored by a canonical source: quest "
						+ entry.questId() + " condition " + trimmed + " sources " + canonicalSources);
				}
				continue;
			}
			throw new IllegalStateException("chain canonical segment retires a gated route: quest "
				+ entry.questId() + " condition " + trimmed);
		}
		for (String token : route.actions().split(";")) {
			String trimmed = token.trim();
			if (!trimmed.isEmpty() && !trimmed.equals("-") && !trimmed.startsWith("REMOVE_ITEM:")
					&& !trimmed.startsWith("GIVE_ITEM:")) {
				// S3c 被承担分支（动作侧，G-1 同轴）：阶段写（`SET_VAR:var0=k`）等动作由规范交付边的
				// actions 逐字承接（判例 28809/35011/35026/80752）——token 逐字相等才放行。
				// Carried branch on the action side, symmetric to the condition side.
				if (carriedDeliveryActions.contains(trimmed)) {
					continue;
				}
				throw new IllegalStateException("chain canonical segment retires an acted route: quest "
					+ entry.questId() + " action " + trimmed);
			}
		}
		for (String token : route.afterCommits().split(";")) {
			String trimmed = token.trim();
			if (trimmed.isEmpty() || trimmed.equals("-") || trimmed.equals("CLOSE")
					|| trimmed.startsWith("DIALOG:") || trimmed.startsWith("SYNC:")) {
				continue;
			}
			if (trimmed.startsWith("MOVIE:")) {
				String[] parts = trimmed.split(":");
				boolean resupported = entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
					|| entry.cutsceneTrigger() == QuestDialogAction.SELECT_QUEST_REWARD.id();
				if (!resupported || parts.length < 2
						|| Integer.parseInt(parts[1]) != entry.cutsceneMovieId()) {
					throw new IllegalStateException("chain cutscene carrier retires without a canonical landing: quest "
						+ entry.questId() + " token " + trimmed);
				}
				continue;
			}
			throw new IllegalStateException("chain canonical segment retires a payload route: quest "
				+ entry.questId() + " after " + trimmed);
		}
	}

	/** 物品需求合并（逐物品取最大需求；`HAS_ITEM` 与 `REMOVE_ITEM` 同轴口径）。 /
	 * Item requirements merged per item id, keeping the strictest count. */
	private static List<QuestItemRequirement> mergeRequirements(List<QuestItemRequirement> left,
			List<QuestItemRequirement> right) {
		if (right.isEmpty()) {
			// 左表逐字返回（S2 行的既有门序不得被本片扰动）。
			return left;
		}
		List<QuestItemRequirement> merged = new ArrayList<>(left);
		for (QuestItemRequirement item : right) {
			boolean present = false;
			for (int index = 0; index < merged.size(); index++) {
				QuestItemRequirement existing = merged.get(index);
				if (existing.itemId() == item.itemId()) {
					present = true;
					if (existing.count() < item.count()) {
						merged.set(index, item);
					}
				}
			}
			if (!present) {
				merged.add(item);
			}
		}
		return merged;
	}



	/** S3c：把承接门合到规范交付边的条件侧/动作侧（同物同侧已声明且计数不减则不再重复）。 /
	 * S3c: merge the carried gate into the canonical edge's conditions and actions. */
	private static void mergeRequirementsIntoEdge(List<QuestCondition> conditions, List<QuestAction> actions,
			List<QuestItemRequirement> handIn) {
		for (QuestItemRequirement item : handIn) {
			boolean hasGate = conditions.stream().anyMatch(condition ->
				condition instanceof QuestCondition.HasItem gate && gate.itemId() == item.itemId()
					&& gate.count() >= item.count());
			if (!hasGate) {
				conditions.add(new QuestCondition.HasItem(item.itemId(), item.count()));
			}
			boolean hasRemove = actions.stream().anyMatch(action ->
				action instanceof QuestAction.RemoveItem remove && remove.itemId() == item.itemId()
					&& remove.count() >= item.count());
			if (!hasRemove) {
				actions.add(new QuestAction.RemoveItem(item.itemId(), item.count()));
			}
		}
	}

	/** 退场记录承接的门（`HAS_ITEM` 条件 + `REMOVE_ITEM` 动作，逐物品取最大需求）。 /
	 * Item gate carried by the retired routes (condition side and action side).
	 *
	 * <p>守卫加固片 G-1（quest-native-dispatch）：动作侧同轴收集——`REMOVE_ITEM` 是交付时扣物，
	 * 若只收条件侧，**"有扣物动作但无条件门"**的退场记录会让扣物**静默消失**（玩家永久持有任务物品，
	 * 且 `canonicalChainHandIn` 的覆盖断言因载荷集为空而空过）。收集后由既有覆盖断言兜底：
	 * 载荷不被规范门覆盖（含 `itemCheck=false ⇒ 空门`）即拒绝编译，交裁定。</p>
	 */
	private static List<QuestItemRequirement> retiredChainGate(
			List<RetailClientTalkChainSteps.RouteRecord> retiredRoutes,
			List<RetailClientTalkChainSteps.ItemReportRecord> itemReports) {
		Map<Integer, Integer> required = new java.util.TreeMap<>();
		// S3c：I 记录（item_check 门，39/20002 对的真端来源）与退场 R 记录同轴收集——交付段接管后
		// I 记录不再展开成旧页链，其门必须由规范交付边承接（判例 24202：唯一门源即 I 记录）。
		// I-record gates merge into the same payload collection.
		for (RetailClientTalkChainSteps.ItemReportRecord record : itemReports) {
			required.merge(record.itemId(), record.required(), Math::max);
		}
		for (RetailClientTalkChainSteps.RouteRecord route : retiredRoutes) {
			for (String token : route.conditions().split(";")) {
				String[] parts = token.trim().split(":");
				if (parts.length == 3 && "HAS_ITEM".equals(parts[0])) {
					required.merge(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Math::max);
				}
			}
			for (String token : route.actions().split(";")) {
				String[] parts = token.trim().split(":");
				if (parts.length == 3 && "REMOVE_ITEM".equals(parts[0])) {
					required.merge(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Math::max);
				}
			}
		}
		return required.entrySet().stream()
			.map(item -> new QuestItemRequirement(item.getKey(), item.getValue())).toList();
	}

	/**
	 * 链式交付门（S2）：三形规则与单步面同源（{@code item_check} 决定有无门，门源取真端元数据 /
	 * carried work-items）；元数据无门但退场记录带 HAS_ITEM 载荷时由载荷兜底承接，并逐条断言载荷不超出
	 * 规范门（否则拒绝编译）。 / Chain hand-in gate: the single-step three-shape rule, with the retired
	 * registry payload as the fallback source and a fail-closed coverage assertion.
	 */
	private static List<QuestItemRequirement> canonicalChainHandIn(RetailSimpleTalkTable.Entry entry,
			List<QuestItemRequirement> reportItems,
			List<RetailClientTalkChainSteps.RouteRecord> retiredRoutes,
			List<RetailClientTalkChainSteps.ItemReportRecord> itemReports) {
		return canonicalChainHandIn(entry, reportItems, retiredRoutes, itemReports, List.of());
	}

	/**
	 * S3c 版：增加「被承担」通道——A 形行把**退场翻转记录自身的载荷**（条件侧 `HAS_ITEM` + 动作侧
	 * `REMOVE_ITEM`）作为门源，与真端三形门同轴合并（`itemCheck=false` 的 A 行门即由此而来，判例
	 * 1118/1323/3218/3966/4218 的检查对与 80752 的真端扣物）；载荷不入门即由覆盖断言拒绝编译。
	 * S3c: the carried channel — the retired flip record's own payload is a gate source.
	 */
	private static List<QuestItemRequirement> canonicalChainHandIn(RetailSimpleTalkTable.Entry entry,
			List<QuestItemRequirement> reportItems,
			List<RetailClientTalkChainSteps.RouteRecord> retiredRoutes,
			List<RetailClientTalkChainSteps.ItemReportRecord> itemReports,
			List<QuestItemRequirement> carried) {
		List<QuestItemRequirement> payload = retiredChainGate(retiredRoutes, itemReports);
		List<QuestItemRequirement> gate = !entry.itemCheck() ? carried
			: reportItems.isEmpty() ? mergeRequirements(payload, carried) : mergeRequirements(reportItems, carried);
		for (QuestItemRequirement item : payload) {
			boolean covered = gate.stream().anyMatch(existing -> existing.itemId() == item.itemId()
				&& existing.count() >= item.count());
			if (!covered) {
				throw new IllegalStateException("chain delivery gate loses the retired registry payload: quest "
					+ entry.questId() + " item " + item.itemId() + ":" + item.count());
			}
		}
		return gate;
	}

	/**
	 * S3a：接取块的 selectionSources 是否全在段内（{@code unaccepted} ∪ 块 target）。越界行**延期接管**：
	 * 登记表把交付/阶段状态的关窗出口泄进了接取块（判例 28809——接取 NPC 与交付 NPC 同体、交付段仍是
	 * 纯 R 驱动），canonical 接取只登记段内关窗出口 ⇒ 现在接管会把在服务页（SELECT3/SELECT5）上的
	 * 关窗按钮变成死端（契约门 BUTTON_WITHOUT_ROUTE）。该行的页链出口必须与交付段一并裁定（S3c），
	 * 本片逐字保留（漂移 0），谓词与 {@link #assertCanonicalSelectionSources} 同源。
	 * S3a: whether the accept block declares its close exits within the segment; out-of-segment rows
	 * defer to the delivery slice instead of silently dropping in-service close exits.
	 */
	private static boolean acceptSourcesWithinSegment(int questId, RetailClientTalkChainSteps.BlockRecord start,
			RetailClientTalkChainSteps steps) {
		String encoded = start.extra().split("\\|", -1)[0];
		for (String source : encoded.trim().split("\\s+")) {
			if (source.isEmpty() || source.equals("-")) {
				continue;
			}
			if (!source.equals("unaccepted") && !source.equals(start.target())
					&& servesFinishDialog(questId, start.npcId(), source, steps)) {
				return false;
			}
		}
		return true;
	}

	/** S3c：A 形交付翻面的动作集（真端侧交付入边；`SET_SUCCEED` 是报告成功形的提交动作）。 /
	 * Delivery-flip actions of the A-shaped family. */
	private static final Set<String> A_SHAPED_DELIVERY_ACTIONS = Set.of("SELECT_QUEST_REWARD", "SET_SUCCEED",
		"SETPRO1", "SETPRO2", "SETPRO3");

	/** S3b 裁定：该源上登记表是否有 FINISH_DIALOG 记录（有 ⇒ 段外关窗出口有载体，不得静默丢；无 ⇒ 放行，
	 * 判例 28809 的 s1/s2）。 / Whether the registry serves a close exit at that state. */
	private static boolean servesFinishDialog(int questId, int npcId, String source,
			RetailClientTalkChainSteps steps) {
		return steps.routes(questId).stream().anyMatch(route -> route.npcId() == npcId
			&& "FINISH_DIALOG".equals(route.action()) && source.equals(route.source()));
	}

	/**
	 * 规范接取段的关窗出口范围守卫（S2）：canonicalAcceptFlow 只在 {@code unaccepted} 与接取目标节点登记
	 * FINISH_DIALOG；登记表声明了其它 selection-source 时拒绝编译（该行需单独裁定，不得静默丢出口）。
	 * Canonical accept keeps its close exits within the segment; out-of-segment selection sources reject.
	 */
	private static void assertCanonicalSelectionSources(int questId,
			RetailClientTalkChainSteps.BlockRecord start, String encoded,
			RetailClientTalkChainSteps steps) {
		for (String source : encoded.trim().split("\\s+")) {
			if (source.isEmpty() || source.equals("-")) {
				continue;
			}
			if (!source.equals("unaccepted") && !source.equals(start.target())
					&& servesFinishDialog(questId, start.npcId(), source, steps)) {
				throw new IllegalStateException("canonical accept finish sources must stay within the segment: quest "
					+ questId + " source " + source + " target " + start.target());
			}
		}
	}

	/**
	 * G-4：该变量条件是否被集合中某个节点的投影蕴含（{@code VAR_IS:var0=k} ⇒ 投影 var0==k；
	 * {@code VAR_AT_LEAST:var0=k} ⇒ 投影 var0≥k）。 / Whether some canonical source node's projection
	 * implies the retired variable gate.
	 */
	private static boolean isImpliedByAnyCanonicalSource(String token, Set<String> canonicalSources,
			Map<String, QuestNode> nodeByLabel) {
		boolean atLeast = token.startsWith("VAR_AT_LEAST:");
		String[] kv = token.substring(token.indexOf(':') + 1).split("=");
		if (kv.length != 2) {
			return false;
		}
		int bound = Integer.parseInt(kv[1]);
		for (String source : canonicalSources) {
			QuestNode node = nodeByLabel.get(source);
			if (node == null) {
				continue;
			}
			Integer actual = node.projection().variables().get(kv[0]);
			if (actual != null && (atLeast ? actual >= bound : actual == bound)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 接取段零残留守卫（S3a）：接管接取段的行过滤后，块 NPC 上不得再留下旧接取页梯——select1 族页下发
	 * 或 1007/1011/1012/1013 动作路由（canonical 接取只下发接取窗页 4 并登记 31/1002/20000/1008/拒绝族）。
	 * 交付段页（SELECT5/SELECT6）与报告动作不在此守卫范围内：无报告块的行其交付段逐字保留。
	 * Accept-segment zero-residue guard (S3a): no select1-ladder page pushes or 1007/1011..1013 routes
	 * survive on the accept NPCs once the canonical accept segment takes over.
	 */
	private static void assertCanonicalAcceptResidueFree(int questId, List<QuestTransition> transitions,
			Set<Integer> acceptNpcs) {
		for (QuestTransition transition : transitions) {
			boolean retiredPage = transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT1.id()
						|| page.dialogId() == QuestDialogPage.SELECT1_1.id()
						|| page.dialogId() == QuestDialogPage.SELECT1_1_1.id()));
			if (retiredPage) {
				throw new IllegalStateException("canonical accept segment still pushes a retired page: quest " + questId
					+ " " + transition.sourceNode() + "→" + transition.targetNode() + " " + transition.event()
					+ " after=" + transition.afterCommit());
			}
			if (transition.event() instanceof QuestEvent.TalkToNpc talk
					&& acceptNpcs.contains(talk.npcId()) && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT1.id()
						|| talk.dialogId() == QuestDialogAction.SELECT1_1.id()
						|| talk.dialogId() == QuestDialogAction.SELECT1_1_1.id())) {
				throw new IllegalStateException("canonical accept segment still carries a legacy ladder route: quest "
					+ questId + " npc " + talk.npcId() + " dialog " + talk.dialogId());
			}
		}
	}

	/**
	 * 规范段零残留守卫（S2 + 守卫加固片 G-2/G-3）：过滤后定义内不得再下发 SELECT5/SELECT6 页；
	 * 交付 NPC 上不得有**领奖前置态**（源节点投影 START）的 1009/39/20002 交付中转；且 reward 态必须
	 * 保留奖励窗重开载体。
	 * <p><b>G-2</b>：判据按**源节点状态**而非字面 {@code "started"}——REWARD 态的同名路由是 QE-083 的
	 * 重开预览语义（必须保留），而 I 记录展开的 39/20002 腿落在 {@code s2} 等中间态时（判例 24202 的
	 * {@code failure_page='CLOSE'}）旧判据整条空过：门边存活、无页承载、**任务不可交付而全部门禁绿**。
	 * <p><b>G-3</b>：领奖后关窗必须能靠 reward 态路由重开奖励窗，否则死档（82 行口径实测 11 行无
	 * {@code NPC_COMPLETE} 块；本片 canonicalDelivery 的 209 行全部有预览腿 ⇒ 零误伤）。
	 * Zero-residue guard for the canonical chain segments, with the pre-reward relay caliber fixed to the
	 * source node's status and the reward-state reopen carrier asserted.
	 */
	private static void assertCanonicalChainResidueFree(int questId, List<QuestTransition> transitions,
			Set<Integer> reportNpcs, Map<String, QuestNode> nodeByLabel) {
		boolean reopenCarrier = false;
		for (QuestTransition transition : transitions) {
			boolean retiredPage = transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT5.id()
						|| page.dialogId() == QuestDialogPage.SELECT6.id()));
			if (retiredPage) {
				throw new IllegalStateException("canonical chain segment still pushes a retired page: quest " + questId
					+ " " + transition.sourceNode() + "→" + transition.targetNode() + " " + transition.event()
					+ " after=" + transition.afterCommit());
			}
			if (transition.event() instanceof QuestEvent.TalkToNpc talk
					&& reportNpcs.contains(talk.npcId())
					&& isPreRewardSource(transition.sourceNode(), nodeByLabel)
					&& (talk.dialogId() != null && (talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()
						|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
						|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()))) {
				throw new IllegalStateException("canonical chain delivery still carries a pre-reward relay: quest "
					+ questId + " " + transition.sourceNode() + " npc " + talk.npcId()
					+ " dialog " + talk.dialogId());
			}
			if (isRewardState(transition.sourceNode(), nodeByLabel)
					&& transition.afterCommit().stream().anyMatch(action ->
						action instanceof AfterCommitAction.ShowQuestDialog page
							&& isRewardWindowPage(page.dialogId()))) {
				reopenCarrier = true;
			}
		}
		if (!reopenCarrier) {
			throw new IllegalStateException("canonical chain delivery loses the reward-state reopen carrier: quest "
				+ questId);
		}
	}

	/** G-2：源节点是否为**领奖前置态**（START）；无源（enter-world）与非 START 源不算交付中转。 /
	 * Whether the source node is a pre-reward START state. */
	private static boolean isPreRewardSource(String sourceNode, Map<String, QuestNode> nodeByLabel) {
		if (sourceNode == null) {
			return false;
		}
		QuestNode node = nodeByLabel.get(sourceNode);
		return node != null && node.projection().status() == QuestStatus.START;
	}

	/** G-3：源节点是否为 reward 态（重开载体必须挂在它上面）。 / Whether the source node projects REWARD. */
	private static boolean isRewardState(String sourceNode, Map<String, QuestNode> nodeByLabel) {
		if (sourceNode == null) {
			return false;
		}
		QuestNode node = nodeByLabel.get(sourceNode);
		return node != null && node.projection().status() == QuestStatus.REWARD;
	}

	/** G-3：该页是否六档奖励窗之一（查表判据，与交付窗选用同源）。 / Whether the page is a tiered reward window. */
	private static boolean isRewardWindowPage(int pageId) {
		for (int tier = 0; tier < 6; tier++) {
			var page = QuestDialogPage.rewardWindowForTier(tier);
			if (page.isPresent() && page.get().id() == pageId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * wave A 链式路径：登记表逐字回放（节点 + TalkToNpc 路由）+ 规范块合成（npc-start /
	 * npc-report 参数化复用；npc-complete 按元数据规范合成，存在才合成）。
	 * <p>
	 * S2（quest-native-dispatch）：同时具备 NPC_START 与 NPC_REPORT 块的行（G1，203 行）两个规范段整体
	 * 接管——接取换 {@code canonicalAcceptFlow}、交付换 {@code canonicalDelivery}，块 NPC 上构成旧页链的
	 * 登记记录按策略 A 退场（过滤面 = 388 条；中段简报/SETPRO 阶梯/交互物记录逐字保留）。无报告块或
	 * 无接取块的纯 R 驱动行（82 行）保持登记表逐字形状（γ 面，另行切片）。
	 * Chain path: wave-A replay plus canonical blocks; S2 gives rows with both NPC_START and NPC_REPORT
	 * blocks the canonical accept/delivery segments and retires their page-chain registry routes.
	 */
	private static QuestDefinition buildChain(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			RetailClientTalkChainSteps steps, QuestMetadata metadata, RetailClientDialogExits exits,
			RetailQuestUseItemNpcs interactionObjects, RetailClientRewardNpcs clientRewardNpcs) {
		List<QuestNode> nodes = steps.nodes(entry.questId()).stream()
			.map(node -> new QuestNode(node.label(), new NodeProjection(
				QuestStatus.valueOf(node.status()),
				node.var0() == null ? Map.of() : Map.of("var0", node.var0()))))
			.toList();
		List<QuestTransition> transitions = new ArrayList<>();
		List<QuestTransition> blockTransitions = new ArrayList<>();
		Map<String, QuestNode> nodeByLabel = new java.util.HashMap<>();
		for (QuestNode node : nodes) {
			nodeByLabel.put(node.label(), node);
		}
		// S2（quest-native-dispatch）：双块规范段行——接取块与报告块同时存在（且非系统发放）时，两个
		// 规范段整体接管；块 NPC 上的旧页链登记记录退场。其余行（系统发放/无报告块/无接取块）保持
		// 登记表逐字形状（γ 面）。
		// S3a（quest-native-dispatch）：段旗标按段独立——有接取块但无报告块的 60 行只接管接取段
		// （G2/G3 的交付段仍是纯 R 驱动，另行切片），报告块缺席时交付段逐字保留。
		// S2/S3a: the segment flags are per-segment — rows with an accept block but no report block take
		// only the canonical accept segment; their R-driven delivery stays verbatim for a later slice.
		boolean canonicalAccept = !entry.grantKind().systemGrant()
			&& !steps.blocks(entry.questId(), "NPC_START").isEmpty()
			&& steps.blocks(entry.questId(), "NPC_START").stream()
				.allMatch(block -> acceptSourcesWithinSegment(entry.questId(), block, steps));
		// S3b：R 驱动接取段——无 NPC_START 块但登记表已声明完整接取提交形（`unaccepted→started` 的
		// QUEST_ACCEPT_1/SIMPLE 记录）的行，从登记表的接取动作合成规范接取流（判例 2611/3001/3023/
		// 21136/24202/80320）。物件入口行（`acquired` 解析到交互物）延期到 USE_OBJECT 变体（判例 1323，
		// 其入口是宝箱、GIVE_ITEM 挂在入口自环上）。
		// S3b: R-driven accept segments (no NPC_START block) synthesize the canonical accept flow.
		Set<Integer> acquiredIds = entry.grantKind().systemGrant() ? Set.of()
			: index.resolveAll(List.of(entry.acquiredNpc() == null ? "" : entry.acquiredNpc())).npcIds();
		int rDrivenNpc = acquiredIds.size() == 1 ? acquiredIds.iterator().next() : -1;
		// S3b：接取入口必须是客户端可点的 `QUEST_SELECT` 记录——物件入口行（判例 1323 的宝箱 730032：
		// 入口是 `USE_OBJECT` 自环、`GIVE_ITEM` 挂在自环上、无 `QUEST_SELECT` 入口）留待 USE_OBJECT 变体。
		boolean rDrivenAccept = !entry.grantKind().systemGrant()
			&& steps.blocks(entry.questId(), "NPC_START").isEmpty()
			&& rDrivenNpc > 0
			&& !interactionObjects.isInteractionObject(rDrivenNpc)
			&& steps.routes(entry.questId()).stream().anyMatch(route -> route.npcId() == rDrivenNpc
				&& "unaccepted".equals(route.source()) && "QUEST_SELECT".equals(route.action()))
			&& steps.routes(entry.questId()).stream().anyMatch(route ->
				"unaccepted".equals(route.source()) && "started".equals(route.target())
					&& (route.action().startsWith("QUEST_ACCEPT")));
		// S3c-obj：**物件哨兵接取者**（判例 1323；真端 `acquired_npc_name=LF2_Lost_JewelBox` = 宝箱
		// 730032）——入口是 `USE_OBJECT` 自环（且**自带入口页下发**，与 S3b 的 `QUEST_SELECT` 入口互斥），
		// 同 owner 另有 `ASK_QUEST_ACCEPT` 中转（下发接取窗页）与 `QUEST_ACCEPT*` 提交。判据五轴全派生
		// （非系统 / 无 START 块 / acquired 唯一 / 三形记录齐备），全量普查实测恰 1 行
		// （`.agents/summary/quest-native-dispatch/w3_object_entry_census.py`）。
		// S3c-obj: the object-sentinel accept variant — entry is the page-pushing USE_OBJECT self loop.
		boolean objectAccept = !entry.grantKind().systemGrant()
			&& steps.blocks(entry.questId(), "NPC_START").isEmpty()
			&& rDrivenNpc > 0
			&& steps.routes(entry.questId()).stream().anyMatch(route -> route.npcId() == rDrivenNpc
				&& "unaccepted".equals(route.source()) && "USE_OBJECT".equals(route.action())
				&& !chainPushedPages(route.afterCommits()).isEmpty())
			&& steps.routes(entry.questId()).stream().anyMatch(route -> route.npcId() == rDrivenNpc
				&& "unaccepted".equals(route.source()) && "ASK_QUEST_ACCEPT".equals(route.action())
				&& !chainPushedPages(route.afterCommits()).isEmpty())
			&& steps.routes(entry.questId()).stream().anyMatch(route -> route.npcId() == rDrivenNpc
				&& "unaccepted".equals(route.source()) && "started".equals(route.target())
				&& route.action().startsWith("QUEST_ACCEPT"));
		if (objectAccept && rDrivenAccept) {
			// 两形互斥：物件入口与 `QUEST_SELECT` 入口同时成立时交裁定（不得静默择一）。
			throw new IllegalStateException("chain accept variant is ambiguous (object entry vs QUEST_SELECT entry): quest "
				+ entry.questId());
		}
		canonicalAccept = canonicalAccept || rDrivenAccept || objectAccept;
		// S3b：R 驱动接取动作 = 登记表提交记录（`QUEST_ACCEPT_1/SIMPLE`，`unaccepted→started`）上的动作
		// （判例 21136 的 `GIVE_ITEM:182207919:1` 落在提交边上）；无动作则为空（判例 3001/3023/24202/80320）。
		List<QuestAction> rDrivenAcceptActions = rDrivenAccept
			? steps.routes(entry.questId()).stream()
				.filter(route -> "unaccepted".equals(route.source()) && "started".equals(route.target()))
				.filter(route -> route.action().startsWith("QUEST_ACCEPT"))
				.map(route -> decodeActions(route.actions()))
				.filter(actions -> !actions.isEmpty())
				.findFirst().orElse(List.of())
			: List.of();
		// S3c：交付段旗标与接取段**解耦**（S3b 只为接取段收紧过它），两类形：
		//   ①块驱动：有 NPC_REPORT 块（S2 起沿用，含系统发放行）；
		//   ②A 形翻面：真端 reward NPC 上存在"源节点投影 START 的 reward 入边"，**且无竞争翻面**——
		//     存在指向 reward、源节点同为 START 态却落在别的 NPC 上的翻转记录即 D 类（中间人 premature 形），
		//     按 (iii) 裁定留待 W2，本片不接管（判例 4209/21033/21455/30711/30761 的混合形）。
		// S3c decouples the delivery flag: block-driven, or an A-shaped flip with no competing flip.
		Set<Integer> resolvedRewardNpcs = index.resolveAll(List.of(
			entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
		// 真端名解析**空集**时回落客户端交付 NPC 集（与单步路径 `clientRewardNpcs` 同源）——判例 35024/
		// 35025/35026（LF4_GuardianOfDivine）、45024/45025/45026（DF4_GuardianOfTower）的真端名在
		// npc 注册表里不存在，交付 owner 由客户端登记表声明（799800/799801、799842/799843）。
		// Fallback to the client-declared reward NPCs when the retail name resolves to nothing.
		Set<Integer> rewardNpcScope = resolvedRewardNpcs.isEmpty()
			? new TreeSet<>(clientRewardNpcs.rewardNpcs(entry.questId())) : resolvedRewardNpcs;
		Set<String> startStates = steps.nodes(entry.questId()).stream()
			.filter(node -> "START".equals(node.status()))
			.map(RetailClientTalkChainSteps.NodeRecord::label)
			.collect(java.util.stream.Collectors.toCollection(TreeSet::new));
		// 真端 reward_npc_name 可解析到**多个同族 id**（判例 35024/35025/35026 的两个交付变体
		// 799800/799801、80752 的 event_warewolf 族）——判据是**成员关系**而非单元素集。
		// The reward NPC may resolve to a family, so membership is the criterion, not a singleton.
		boolean aShapedFlip = steps.routes(entry.questId()).stream().anyMatch(route ->
			"reward".equals(route.target()) && startStates.contains(route.source())
				&& rewardNpcScope.contains(route.npcId())
				&& A_SHAPED_DELIVERY_ACTIONS.contains(route.action()));
		boolean competingFlip = steps.routes(entry.questId()).stream().anyMatch(route ->
			"reward".equals(route.target()) && startStates.contains(route.source())
				&& !rewardNpcScope.contains(route.npcId())
				&& A_SHAPED_DELIVERY_ACTIONS.contains(route.action()));
		// ①块驱动（有报告块**且**接取段已接管，S2 起沿用）②A 形翻面（无竞争翻面）。系统发放行与段未接管行
		// 的交付段不在本片——保持 legacy 逐字形，使交付面切片边界与 A 类行集逐行相等。
		// Delivery takes over only when the report-block path already applies, or an A-shaped flip exists.
		// 块路径：接取段已接管（S2 双块行）或**系统发放行**（无接取段可接管，交付段独立成片——判例
		// 35017：systemGrant + REPORT 块 + 无翻面记录，接取段恒不翻）。实测 REPORT 块行 205 行里仅有
		// 2611 与 35017 两块无 START 块，故系统发放放行的新增面恰为 35017 一行（切片边界不变）。
		// Block path: an already-taken-over accept segment, or a system-grant row (no accept segment to
		// take over) — exactly one row (35017) in the measured population.
		boolean canonicalDelivery = ((canonicalAccept || entry.grantKind().systemGrant())
				&& !steps.blocks(entry.questId(), "NPC_REPORT").isEmpty())
			|| (aShapedFlip && !competingFlip);
		// S3c：A 形翻面记录（源节点投影 START、落在真端 reward NPC、动作属交付翻转族）——每条合成一条规范
		// 交付边，记录本身随交付段退场；带报告块的行走块路径，本列表为空。
		// A-shaped flips: one canonical delivery edge per record; empty for block-driven rows.
		List<RetailClientTalkChainSteps.RouteRecord> aShapedFlips = canonicalDelivery
			&& steps.blocks(entry.questId(), "NPC_REPORT").isEmpty()
			? steps.routes(entry.questId()).stream()
				.filter(route -> "reward".equals(route.target())
					&& startStates.contains(route.source())
					&& rewardNpcScope.contains(route.npcId())
					&& A_SHAPED_DELIVERY_ACTIONS.contains(route.action()))
				.toList()
			: List.of();
		// 守卫加固片 G-4 的"被承担"分支（S3c）：退场的阶段门若被某条规范交付边**逐字承接**（A 形记录自身的
		// conditions 会原样落在合成交付边上），则无需源节点投影蕴含——A2 行的投影 `var0=-`，锚定不蕴含。
		// G-4 carried branch: stage gates carried verbatim by a canonical delivery edge are allowed.
		Set<String> carriedDeliveryGates = new TreeSet<>();
		Set<String> carriedDeliveryActions = new TreeSet<>();
		for (RetailClientTalkChainSteps.RouteRecord flip : aShapedFlips) {
			for (String token : flip.conditions().split(";")) {
				String trimmed = token.trim();
				if (!trimmed.isEmpty() && !trimmed.equals("-")) {
					carriedDeliveryGates.add(trimmed);
				}
			}
			for (String token : flip.actions().split(";")) {
				String trimmed = token.trim();
				if (!trimmed.isEmpty() && !trimmed.equals("-")) {
					carriedDeliveryActions.add(trimmed);
				}
			}
		}
		// S3c-D（裁定 ①(iii) 的**可证安全子集**）：交付 NPC 侧**下发报告页**（`SELECT5`/`SELECT6`）的记录按
		// R-REP 页下发判据退场（39 条 / 39 行；载荷实测全空）。**「中间人翻面就地加窗」被否决**——e2e 契约门
		// 实测中间人 owner 没有领奖选择腿（判例 3100：`native reward window has no completion route`），
		// 加窗会造**可见死按钮**（致命类 `BUTTON_WITHOUT_ROUTE`）⇒ 退回独立裁定（镜像领奖腿 / 落 reward NPC /
		// 维持现状），本片只做可证安全部分。
		// S3c-D takes only the provably safe subset (R-REP report-page retirement); the "window the mid-NPC
		// flip in place" part is rejected on e2e evidence of dead reward-selection buttons.
		boolean anyRewardFlip = steps.routes(entry.questId()).stream().anyMatch(route ->
			"reward".equals(route.target()) && startStates.contains(route.source())
				&& A_SHAPED_DELIVERY_ACTIONS.contains(route.action()));
		boolean dClassReportRetire = anyRewardFlip
			&& steps.blocks(entry.questId(), "NPC_REPORT").isEmpty();
		Set<Integer> acceptNpcs = new TreeSet<>();
		Set<Integer> reportNpcs = new TreeSet<>();
		if (canonicalAccept) {
			if (steps.blocks(entry.questId(), "NPC_START").isEmpty()) {
				acceptNpcs.add(acquiredIds.iterator().next());
			} else {
				steps.blocks(entry.questId(), "NPC_START").forEach(block -> acceptNpcs.add(block.npcId()));
			}
		}
		if (canonicalDelivery) {
			steps.blocks(entry.questId(), "NPC_REPORT").forEach(block -> reportNpcs.add(block.npcId()));
			reportNpcs.addAll(resolvedRewardNpcs);
			// S3c：A 形翻面 NPC 一律进交付段退场作用域（真端名空集行由客户端集声明，判例 35025/45024）。
			// A-shaped flip NPCs always join the delivery retirement scope.
			aShapedFlips.forEach(flip -> reportNpcs.add(flip.npcId()));
		}
		// S3c 层 A 交付对偶：A 形合成边占据 (源节点, 交付 NPC, 31) 键——同键登记记录必须随交付段退场，
		// 否则 `explicitRoutes` 覆盖会把刚合成的交付边**反向删除**（判例 80752 的 SELECT2 页记录，
		// 交付段接管后逐字静默丢失、指纹零漂移而形状回退）。
		// S3c layer A (delivery twin): registry records sharing the synthesized delivery edge key retire.
		Set<String> deliveryKeys = new TreeSet<>();
		for (RetailClientTalkChainSteps.RouteRecord flip : aShapedFlips) {
			deliveryKeys.add(flip.source() + ":" + flip.npcId() + ":" + QuestDialogAction.QUEST_SELECT.id());
		}
		// 层 A（S3b）：与规范边**同键**的登记记录必须退场——否则 `explicitRoutes` 会用同键 R 记录反向删除
		// 合成的规范边（R 记录的 `(unaccepted, npc, 31)` 入口页下发会删掉 canonical 接取窗边）⇒ 形状回退
		// 旧形而门禁全绿。键集 = 接取段的规范边键（31/1002/20000/拒绝族/关窗出口 × 接取 NPC）。
		// Layer A: registry routes sharing a canonical edge key must retire, or they delete the
		// synthesized canonical edges through the explicit-route override.
		Set<String> canonicalKeys = new TreeSet<>();
		Set<String> rDrivenAcceptedTargets = new TreeSet<>();
		if (canonicalAccept) {
			for (RetailClientTalkChainSteps.BlockRecord start : steps.blocks(entry.questId(), "NPC_START")) {
				rDrivenAcceptedTargets.add(start.target());
			}
			if (rDrivenAcceptedTargets.isEmpty()) {
				rDrivenAcceptedTargets.add("started");
			}
			for (int npc : acceptNpcs) {
				canonicalKeys.add("unaccepted:" + npc + ":" + QuestDialogAction.QUEST_SELECT.id());
				canonicalKeys.add("unaccepted:" + npc + ":" + QuestDialogAction.QUEST_ACCEPT_1.id());
				canonicalKeys.add("unaccepted:" + npc + ":" + QuestDialogAction.QUEST_ACCEPT_SIMPLE.id());
				for (QuestDialogAction refuse : List.of(QuestDialogAction.QUEST_REFUSE_1,
						QuestDialogAction.QUEST_REFUSE_2, QuestDialogAction.QUEST_REFUSE_SIMPLE)) {
					canonicalKeys.add("unaccepted:" + npc + ":" + refuse.id());
				}
				for (String target : rDrivenAcceptedTargets) {
					canonicalKeys.add("unaccepted:" + npc + ":" + QuestDialogAction.FINISH_DIALOG.id());
					canonicalKeys.add(target + ":" + npc + ":" + QuestDialogAction.FINISH_DIALOG.id());
				}
			}
			if (objectAccept) {
				// S3c-obj 层 A 对偶：物件梯的入口（`USE_OBJECT`）与中转（`ASK_QUEST_ACCEPT`）键——不同键
				// 退场，合成的入口/中转边会被 `explicitRoutes` 反向删除（形状静默回退旧形而指纹零漂移）。
				canonicalKeys.add("unaccepted:" + rDrivenNpc + ":" + QuestDialogAction.USE_OBJECT.id());
				canonicalKeys.add("unaccepted:" + rDrivenNpc + ":" + QuestDialogAction.ASK_QUEST_ACCEPT.id());
			}
		}
		// G-4：规范边的源节点集（接取边恒在 `unaccepted`；交付边在报告块的 source）——退场的阶段门
		// 必须被其中某个节点的投影蕴含，否则拒绝编译。
		Set<String> canonicalSources = new TreeSet<>();
		if (canonicalAccept) {
			canonicalSources.add("unaccepted");
		}
		if (canonicalDelivery) {
			steps.blocks(entry.questId(), "NPC_REPORT").forEach(block -> canonicalSources.add(block.source()));
			aShapedFlips.forEach(flip -> canonicalSources.add(flip.source()));
		}
		List<RetailClientTalkChainSteps.RouteRecord> retiredRoutes = new ArrayList<>();
		List<RetailClientTalkChainSteps.RouteRecord> retiredAcceptRoutes = new ArrayList<>();
		List<QuestTransition> replayAll = new ArrayList<>();
		if (entry.grantKind().systemGrant()) {
			transitions.add(new QuestTransition(new QuestEvent.SystemGrant(),
				List.of(new QuestCondition.StartEligible()), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		} else if (rDrivenAccept) {
			// S3b：R 驱动接取段（无 NPC_START 块）——按登记表的提交形合成 canonical 接取流：31 直发接取窗
			// （页 4）、1002/20000 两形提交（带登记表提交记录上的接取动作）、拒绝族、FINISH_DIALOG 关窗出口；
			// select1 入口页、ASK_QUEST_ACCEPT(1007) 中转与 SELECT1_1/阶段页梯由策略 A 词表 + **层 A** 键退场。
			// 交付段不受影响（canonicalDelivery 显式排除本形，留 S3c）。
			// S3b: R-driven accept segment synthesized from the registry's submission shape.
			blockTransitions.addAll(attachMovieToRoute(
				RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(acquiredIds.iterator().next(),
					"started", rDrivenAcceptActions),
				"unaccepted", acquiredIds.iterator().next(), QuestDialogAction.QUEST_SELECT.id(),
				entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
					? entry.cutsceneMovieId() : -1));
		} else if (objectAccept) {
			// S3c-obj：物件入口接取段合成——**保留客户端页梯**（入口页 select1 与中转页 4 是客户端为该
			// 物件 authored 的合同：`QuestStartItemNpcAi2` 在物件无任务路由时亦回落 `SELECT1`；随段退场会
			// 把入口页打成 `CLIENT_PAGE_UNREACHED`，判例 1323 的 P0c-42 三页合同）；真端 `give_item` 落
			// **提交边**（入口自环无门且可重复 ⇒ 重复用物件叠加任务物品、拒接后残留；其余族 give 均在
			// 提交边，判例 21136）。过场轴（若有）按 S1 判据重挂到入口边。
			// S3c-obj: the object accept ladder keeps the client page contract; the retail give_item
			// moves from the repeatable entry self loop onto the commit edge.
			List<QuestTransition> objectFlow = canonicalObjectAcceptFlow(rDrivenNpc, "started",
				acceptGiveItemActions(entry));
			blockTransitions.addAll(attachMovieToRoute(objectFlow, "unaccepted", rDrivenNpc,
				QuestDialogAction.USE_OBJECT.id(),
				entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
					? entry.cutsceneMovieId() : -1));
		} else if (!steps.blocks(entry.questId(), "NPC_START").isEmpty()) {
			// 多变体任务可有多个 NPC_START（镜像/职业变体，判例 1484 五块）——每块各合成接取流。
			for (RetailClientTalkChainSteps.BlockRecord start : steps.blocks(entry.questId(), "NPC_START")) {
				// wave B：extra 第三段 = 接取发物（give_item → accept-actions 的 GiveItem，'-' = 无）。
				String[] startExtra = start.extra().split("\\|");
				List<QuestAction> startActions = startExtra.length > 2
					? decodeActions(startExtra[2]) : List.of();
				if (canonicalAccept) {
					// S2：接取段整段换 canonicalAcceptFlow——QUEST_SELECT 直发接取窗（页 4）、1002/20000
					// 两形提交（带接取发物）、拒绝族、FINISH_DIALOG→任务列表页；select1 页梯与
					// ASK_QUEST_ACCEPT(1007) 中转随页链退场（其登记记录已被策略 A 过滤）。过场轴按 S1 判例
					// 重挂到下发接取窗的那条 QUEST_SELECT 边（cs1_haction=1007，判例 3020/4056）。
					// S2 canonical accept: ask window straight from QUEST_SELECT; the select1 ladder and the
					// 1007 relay retire, the 1007-triggered cutscene re-attaches to the ask-window edge.
					blockTransitions.addAll(attachMovieToRoute(
						RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(start.npcId(), start.target(),
							startActions),
						"unaccepted", start.npcId(), QuestDialogAction.QUEST_SELECT.id(),
						entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
							? entry.cutsceneMovieId() : -1));
					assertCanonicalSelectionSources(entry.questId(), start, startExtra[0], steps);
				} else {
					blockTransitions.addAll(acceptFlowChain(start.npcId(),
						List.of(startExtra[0].split(" ")),
						startExtra[1], start.target(), startActions));
					// P0c-40：接取页梯（select1 → select1_1 → [select1_1_1] → 接取窗）与单步路径同形，
					// 续页出口同样只认客户端登记（真端模板表没有页链列）。缺这一段则 select1 页的
					// 「继续听」按钮在链式行上无路由（客户端死按钮；判例 21460/29070/29071）。
					// P0c-40: the accept page ladder (select1 → select1_1 → [select1_1_1]) is shared with
					// the single-step path and likewise driven by the client exit registry.
					if (exits.requires(entry.questId(), RetailClientDialogExits.SELECT1_1)) {
						blockTransitions.addAll(acceptContinuation(start.npcId(),
							exits.requires(entry.questId(), RetailClientDialogExits.SELECT1_1_1)));
					}
				}
			}
		}
		for (RetailClientTalkChainSteps.RouteRecord route : steps.routes(entry.questId())) {
			// EnterWorld 无源（XML 无 source 属性即 null，与 QuestDefinitionXmlCompiler 同口径）；
			// priority 透传（重叠路由的显式消歧，XML 同口径）。
			String routeSource = route.eventType().equals("ENTER_WORLD") ? null : route.source();
			Integer routePriority = "-".equals(route.priority()) ? null : Integer.parseInt(route.priority());
			// TALK 路由的区间动作 token（A..B，dialogActions 同语义）展开为逐 id 事件
			// （判例 4970：SELECTED_QUEST_REWARD1..NOREWARD 区间此前被转写成 null 通配）。
			QuestEvent nonTalkEvent = switch (route.eventType()) {
				case "CAN_ACT" -> new QuestEvent.CanAct(route.npcId(), route.action());
				case "QUEST_ACTION" -> new QuestEvent.QuestDialog(parseDialogAction(route.action()));
				case "ENTER_WORLD" -> new QuestEvent.EnterWorld();
				default -> null;
			};
			List<Integer> dialogIds = nonTalkEvent != null
				? List.of(0) : expandDialogActionTokens(route.action());
			// S2 策略 A：规范段接管的登记记录退场——不进回放、不参与显式路由覆盖；载荷 fail-closed。
			// 两个作用域集合各自为空时不匹配，故无段接管的行（系统发放/无块）行为不变。
			// S2 strategy A: routes owned by the canonical segments retire (never replayed, never
			// overriding the canonical block edges); their payload is fail-closed audited.
			// 层 A（S3b）：同键（source, npc, dialogId）∈ 规范边键集 ⇒ 退场——否则 `explicitRoutes`
			// 会用该 R 记录反向删除合成的规范边（判例 2611/3001/3023/21136/24202/80320 的
			// `(unaccepted, npc, 31)` 入口页下发记录会删掉 canonical 接取窗边）⇒ 形状回退旧形而门禁全绿。
			// Layer A: registry routes sharing a canonical edge key retire before the override pass.
			boolean acceptSide = acceptNpcs.contains(route.npcId())
				&& (CHAIN_ACCEPT_RETIRED.contains(route.action())
					|| ("SETPRO1".equals(route.action()) && "unaccepted".equals(route.source()))
					|| canonicalKeyRoute(route, canonicalKeys));
			boolean deliverySide = reportNpcs.contains(route.npcId()) && canonicalKeyRoute(route, deliveryKeys);
			// R-REP：交付 NPC 上下发报告页的记录退场（**页下发判据**，不认动作词表）。
			boolean reportPageSide = dClassReportRetire && rewardNpcScope.contains(route.npcId())
				&& !Collections.disjoint(chainPushedPages(route.afterCommits()), R_REP_REPORT_PAGES);
			if (reportPageSide && !canonicalDelivery) {
				// 仅对**无规范承接边**的行收紧：canonical 行的退场载荷由 QE-089 四轴守卫（carried/蕴含）兜底。
				// The stricter check applies only to rows with no canonical delivery edge (D-class).
				assertReportPageRetirementQuiet(entry, route);
			}
			boolean retired = acceptSide || deliverySide || reportPageSide
				|| retiredChainRoute(route, acceptNpcs, reportNpcs);
			if (retired) {
				assertRetiredChainRouteQuiet(entry, route, canonicalSources, nodeByLabel, carriedDeliveryGates,
					carriedDeliveryActions);
				retiredRoutes.add(route);
				if (acceptSide) {
					retiredAcceptRoutes.add(route);
				}
			}
			for (int dialogId : dialogIds) {
				QuestEvent event = nonTalkEvent != null
					? nonTalkEvent : new QuestEvent.TalkToNpc(route.npcId(), dialogId, 0);
				QuestTransition replayed = new QuestTransition(event,
					decodeConditions(route.conditions()), decodeActions(route.actions()), route.target(),
					decodeAfters(route.afterCommits()), routePriority, routeSource);
				replayAll.add(replayed);
				if (!retired) {
					transitions.add(replayed);
				}
			}
		}
		// P0c-34：item_check 门（I 记录）——编译器级糖元素 npc-item-report 的逐字展开，并入
		// blockTransitions 以复用「显式路由覆盖块路由」与同键去重（与 XML 展开器同口径）。
		// Item-check gates expand here and join the block transitions, so the explicit-route
		// override and the same-key dedup apply exactly as in the XML expander.
		// S3c：交付段接管后 I 记录（item_check 的 39/20002 对）不再展开为旧页链——其门改由
		// `retiredChainGate` 并入规范交付边（判例 24202 的唯一门源即 I 记录）；未接管行逐字保留。
		// S3c: canonicalized rows fold the I-record gate into the canonical delivery edge instead of
		// expanding the legacy check pair.
		if (!canonicalDelivery) {
			for (RetailClientTalkChainSteps.ItemReportRecord report : steps.itemReports(entry.questId())) {
				blockTransitions.addAll(itemReportGate(entry.questId(), report, nodeByLabel));
			}
		}
		for (RetailClientTalkChainSteps.BlockRecord report : steps.blocks(entry.questId(), "NPC_REPORT")) {
			// 真端 reward_npc_name 指定交付 owner；旧登记块的 NPC 可停在前一位 talk_npc。 /
			// Retail reward_npc_name owns turn-in; older block snapshots can point to the preceding talk NPC.
			Set<Integer> rewardIds = index.resolveAll(List.of(
				entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
			int reportNpc = rewardIds.size() == 1 ? rewardIds.iterator().next() : report.npcId();
			List<QuestItemRequirement> reportItems = metadata.itemRequirements().isEmpty()
				? carriedWorkItems(metadata, replayAll, blockTransitions) : metadata.itemRequirements();
			if (canonicalDelivery) {
				// S2：交付段整段换 canonicalDelivery——QUEST_SELECT 带整组 HasItem 门直翻 REWARD 并下发
				// 档位奖励窗（零奖励组回落窗 1）；报告页 SELECT5、39/20002 双按钮对、SELECT6 失败页与
				// reward 态 1009 中转随页链退场（未集齐零路由，关窗兜底交 DialogService）。门的来源按
				// 单步面三形规则；退场记录带的 HAS_ITEM 载荷兜底承接并逐条校验不超出规范门。
				// S2 canonical delivery: the gated QUEST_SELECT straight to REWARD plus the tiered
				// window; the report page and its check pairs retire (an incomplete hand-in has no route).
				if (!"reward".equals(report.target())) {
					throw new IllegalStateException("canonical chain delivery expects the reward node: quest "
						+ entry.questId() + " target " + report.target());
				}
				List<QuestItemRequirement> handIn = canonicalChainHandIn(entry, reportItems, retiredRoutes,
					steps.itemReports(entry.questId()));
				List<QuestCondition> hasItems = handIn.stream()
					.<QuestCondition>map(item -> new QuestCondition.HasItem(item.itemId(), item.count())).toList();
				List<QuestAction> removeItems = handIn.stream()
					.<QuestAction>map(item -> new QuestAction.RemoveItem(item.itemId(), item.count())).toList();
				blockTransitions.addAll(attachMovieToRoute(List.of(
					RetailSimpleCollectItemDefinitionCompiler.canonicalDelivery(reportNpc, hasItems, removeItems,
						RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, entry.questId()),
						report.source())),
					report.source(), reportNpc, QuestDialogAction.QUEST_SELECT.id(),
					entry.cutsceneTrigger() == QuestDialogAction.SELECT_QUEST_REWARD.id()
						? entry.cutsceneMovieId() : -1));
			} else {
				blockTransitions.addAll(reportFlowChain(reportNpc, report.source(), report.target(),
					report.extra(), nodeByLabel, reportItems, entry.itemCheck(),
					exits, entry.questId()));
			}
		}
		// S3c：A 形交付边合成——无报告块的行，逐条翻面记录合成规范交付边（源节点 = 记录源、落 reward、
		// 门 = 元数据/carried/I 记录兜底 + 记录自身 conditions **逐字**、动作 = 扣物、下发档位奖励窗），
		// 过场按 S1 判据重挂；翻面记录本身已随交付段退场（报告 NPC 作用域 + 交付词表）。
		// S3c: A-shaped delivery edges, one per flip record; the record's own conditions are carried
		// verbatim (the G-4 carrier) and the cutscene re-attaches per the S1 ruling.
		for (RetailClientTalkChainSteps.RouteRecord flip : aShapedFlips) {
			List<QuestItemRequirement> reportItems = metadata.itemRequirements().isEmpty()
				? carriedWorkItems(metadata, replayAll, blockTransitions) : metadata.itemRequirements();
			// 退场翻转记录自身的载荷是"被承担"门源（三形规则的 carried 通道）：条件侧 HAS_ITEM 与动作侧
			// REMOVE_ITEM 一并逐字承接（判例 1118/1323/3218/3966/4218 的检查对、80752 的真端扣物、
			// 35011/35026/28809 的 SET_VAR 阶段写）——同构替换只换页面骨架，载荷既不丢门也不丢物。
			// The retired flip's own payload is the carried gate source; the edge keeps it verbatim.
			List<QuestItemRequirement> carriedGate = retiredChainGate(List.of(flip), List.of());
			List<QuestItemRequirement> handIn = canonicalChainHandIn(entry, reportItems, retiredRoutes,
				steps.itemReports(entry.questId()), carriedGate);
			List<QuestCondition> conditions = new ArrayList<>(decodeConditions(flip.conditions()));
			List<QuestAction> actions = new ArrayList<>(decodeActions(flip.actions()));
			mergeRequirementsIntoEdge(conditions, actions, handIn);
			QuestTransition edge = RetailSimpleCollectItemDefinitionCompiler.canonicalDelivery(flip.npcId(),
				conditions, actions,
				RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, entry.questId()),
				flip.source());
			blockTransitions.addAll(attachMovieToRoute(List.of(edge), flip.source(), flip.npcId(),
				QuestDialogAction.QUEST_SELECT.id(),
				entry.cutsceneTrigger() == QuestDialogAction.SELECT_QUEST_REWARD.id()
					? entry.cutsceneMovieId() : -1));
		}
		if (entry.grantKind().systemGrant()) {
			// P0c-2 形状合同：系统发放行不得有 NPC 接取/拒绝路由——unaccepted 源的
			// TalkToNpc / 无目标 QuestDialog 路由都是 XML 时代接取机制的残留
			// （真端经 SystemGrant 发放，链首推进由 started 起的路由承担）。
			transitions.removeIf(transition -> "unaccepted".equals(transition.sourceNode())
				&& (transition.event() instanceof QuestEvent.TalkToNpc
					|| transition.event() instanceof QuestEvent.QuestDialog));
		}
		// 多变体任务可有多个 NPC_COMPLETE（判例 1484 十块）——每块各回放完成流。
		for (RetailClientTalkChainSteps.BlockRecord block : steps.blocks(entry.questId(), "NPC_COMPLETE")) {
			blockTransitions.addAll(completeFlowFromBlock(metadata, block));
		}
		// 与 QuestXmlBlockExpander 同口径：块展开路由被显式原始路由覆盖（(source, npc, dialogId) 键）。
		java.util.Set<String> explicitRoutes = new java.util.HashSet<>();
		for (QuestTransition transition : transitions) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null) {
				explicitRoutes.add(transition.sourceNode() + ":" + talk.npcId() + ":" + talk.dialogId());
			}
		}
		blockTransitions.removeIf(transition -> {
			QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
			return talk.dialogId() != null
				&& explicitRoutes.contains(transition.sourceNode() + ":" + talk.npcId() + ":" + talk.dialogId());
		});
		// 块间同键去重：npc-report 与 npc-complete 都在 reward 节点合成 SELECT_QUEST_REWARD 时，
		// 保留先合成者（report 推进路由胜 complete 预览路由）——否则同事件无优先级冲突。
		// ADOPT 冻结集不受影响（有冲突的行在对拍即 DIFF，进不了冻结集）。
		java.util.Set<String> blockKeys = new java.util.HashSet<>();
		blockTransitions.removeIf(transition -> {
			QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
			if (talk.dialogId() == null) {
				return false;
			}
			String key = transition.sourceNode() + ":" + talk.npcId() + ":" + talk.dialogId()
				+ ":" + transition.priority();
			return !blockKeys.add(key);
		});
		transitions.addAll(blockTransitions);
		// S2/S3a 零残留守卫：按段作用域——接取段过滤后不得再留在册页梯（select1 族页下发、1007/1011 动作），
		// 交付段过滤后不得再含旧页链（SELECT5/SELECT6 页下发、started 源交付中转）。
		// S2/S3a zero-residue guards, scoped per canonicalized segment.
		if (canonicalAccept) {
			if (objectAccept) {
				// 物件梯的"零残留"判据 = 与登记表的物件未接取态对话面**逐键相等**（页梯本身在客户端合同内，
				// 故不套 SELECT1 族零残留守卫）；多一条 = 发明客户端不会发的按钮，少一条 = 静默丢出口。
				assertObjectAcceptContract(entry.questId(), rDrivenNpc, transitions, steps);
			} else {
				assertCanonicalAcceptResidueFree(entry.questId(), transitions, acceptNpcs);
			}
			assertAcceptPayloadCovered(entry.questId(), retiredAcceptRoutes, blockTransitions);
		}
		if (canonicalDelivery) {
			assertCanonicalChainResidueFree(entry.questId(), transitions, reportNpcs, nodeByLabel);
		}
		// SELECT2 的 1353 按钮没有同 NPC、同状态的路由时，给非当前步骤 NPC 显示无按钮页。 /
		// Show a terminal page for a non-stage NPC when its SELECT2 continuation has no matching route.
		if (exits.requires(entry.questId(), RetailClientDialogExits.SELECT2_CONTINUE)) {
			List<QuestTransition> closed = new ArrayList<>(transitions.size());
			List<QuestTransition> available = transitions;
			for (QuestTransition transition : transitions) {
				if (transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.dialogId() != null
						&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT2.id()))
						&& available.stream().noneMatch(candidate ->
							Objects.equals(candidate.sourceNode(), transition.sourceNode())
								&& candidate.event() instanceof QuestEvent.TalkToNpc continuation
								&& continuation.npcId() == talk.npcId()
								&& continuation.dialogId() != null
								&& continuation.dialogId() == QuestDialogAction.SELECT2_1.id())) {
					closed.add(new QuestTransition(transition.event(), transition.conditions(),
						transition.actions(), transition.targetNode(),
						List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
						transition.priority(), transition.sourceNode()));
				} else {
					closed.add(transition);
				}
			}
			transitions = closed;
		}
		// 检查按钮只在交付态可用；领奖态直接重开奖励窗，避免再次显示不可处理的 39/20002。 /
		// Check buttons belong to turn-in; reopen the reward window directly in REWARD state.
		if (exits.requires(entry.questId(), RetailClientDialogExits.SELECT5_CHECK)
				|| exits.requires(entry.questId(), RetailClientDialogExits.SELECT5_CHECK_SIMPLE)) {
			List<QuestTransition> reopened = new ArrayList<>(transitions.size());
			for (QuestTransition transition : transitions) {
				if ("reward".equals(transition.sourceNode())
						&& transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.dialogId() != null
						&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT5.id()))) {
					reopened.add(new QuestTransition(transition.event(), transition.conditions(),
						transition.actions(), transition.targetNode(),
						List.of(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
						transition.priority(), transition.sourceNode()));
				} else {
					reopened.add(transition);
				}
			}
			transitions = reopened;
		}
		// P0c-41：select6 失败页的关闭出口闭包。客户端 select6 页（2716）的唯一按钮是 1008「结束对话」，
		// 所以任何下发该页的节点都必须同节点、同 NPC 登记 FINISH_DIALOG。登记块的关闭出口锚在块 source
		// （s{K}），而显式路由（R 记录）可把失败页锚在块 target（判例 1932/3092：reward 态 CHECK 失败页
		// 无关闭路由 → 契约门 BUTTON_WITHOUT_ROUTE）。缺口形状逐字取自同任务既有关闭出口，不新造页与动作。
		// Close-exit closure for the select6 failure page: its only client button is 1008, so every node
		// pushing the page must serve FINISH_DIALOG for the same npc, anchored the way the pusher is.
		if (exits.requires(entry.questId(), RetailClientDialogExits.SELECT6)) {
			java.util.Set<String> servedClose = new java.util.HashSet<>();
			for (QuestTransition transition : transitions) {
				if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
						&& talk.dialogId() == QuestDialogAction.FINISH_DIALOG.id()) {
					servedClose.add(closeKey(transition.sourceNode(), talk.npcId()));
				}
			}
			List<QuestTransition> closed = new ArrayList<>(transitions.size());
			for (QuestTransition transition : transitions) {
				closed.add(transition);
				if (transition.sourceNode() == null || !(transition.event() instanceof QuestEvent.TalkToNpc talk)
						|| !pushesSelect6(transition)
						|| !servedClose.add(closeKey(transition.sourceNode(), talk.npcId()))) {
					continue;
				}
				closed.add(talk(talk.npcId(), QuestDialogAction.FINISH_DIALOG, transition.sourceNode(),
					transition.sourceNode(), null, List.of(new AfterCommitAction.ShowQuestSelectionDialog(
						QuestDialogPage.SELECT_QUEST.id()))));
			}
			transitions = closed;
		}
		// 领奖行投影为非零时，修复迁移前保存的 REWARD/var0=0。已有 REWARD 修复边（登记表 E 记录，
		// 含 stale≠0 的变体）时不再发射——否则同任务出现两条无源修复边（P0c-33 判据 6：24202/19004）。
		// Repair pre-migration REWARD/var0=0 saves when the client reward row is nonzero; skip when a
		// REWARD repair edge (E record, including the stale≠0 variant) is already present.
		QuestNode rewardNode = nodeByLabel.get("reward");
		Integer rewardRow = rewardNode == null ? null : rewardNode.projection().variables().get("var0");
		if (rewardRow != null && rewardRow > 0 && transitions.stream().noneMatch(transition ->
			transition.event() instanceof QuestEvent.EnterWorld
				&& transition.conditions().contains(new QuestCondition.StatusIs(QuestStatus.REWARD)))) {
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", 0)),
				List.of(new QuestAction.SetVariable("var0", rewardRow)), "reward",
				List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), null, null));
		}
		ProgressLayout chainLayout = steps.layout(entry.questId())
			.map(layout -> (ProgressLayout) new ProgressLayout.Builder()
				.add(new BitField("var0", layout.offset(), layout.width(), layout.min(),
					layout.max(), PersistenceMode.valueOf(layout.persistence()),
					ProgressScope.valueOf(layout.scope())))
				.build())
			.orElseGet(RetailSimpleTalkDefinitionCompiler::layout);
		return new QuestDefinition(entry.questId(), 1, metadata, chainLayout, List.copyOf(nodes),
			List.copyOf(transitions));
	}

	/** 链式布局与单步同宽（6 位 SECTION 网格）。 / The chain layout uses the same 6-bit grid. */
	private static ProgressLayout layout() {
		return new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
	}

	/** 动作 token 解析：枚举名优先，数字 id 兜底，'-'=无动作（null dialogId）。 */
	private static Integer parseDialogAction(String token) {
		if ("-".equals(token)) {
			return null;
		}
		try {
			return QuestDialogAction.valueOf(token).id();
		} catch (IllegalArgumentException e) {
			return Integer.parseInt(token);
		}
	}

	/** TALK 动作 token 展开（dialogActions 同语义）：单 token / {@code A..B} 区间。 */
	private static List<Integer> expandDialogActionTokens(String token) {
		if (token.contains("..")) {
			int delimiter = token.indexOf("..");
			int first = parseDialogAction(token.substring(0, delimiter));
			int last = parseDialogAction(token.substring(delimiter + 2));
			if (first > last || last - first >= 256) {
				throw new IllegalArgumentException("invalid dialog action range: " + token);
			}
			List<Integer> ids = new ArrayList<>();
			for (int id = first; id <= last; id++) {
				ids.add(id);
			}
			return ids;
		}
		return List.of(parseDialogAction(token));
	}

	/** 条件解码（fail-closed）。 / Condition token decoding. */	private static List<QuestCondition> decodeConditions(String encoded) {
		if ("-".equals(encoded)) {
			return List.of();
		}
		List<QuestCondition> out = new ArrayList<>();
		for (String token : encoded.split(";")) {
			if (token.equals("START_ELIGIBLE")) {
				out.add(new QuestCondition.StartEligible());
			} else if (token.startsWith("STATUS_IS:")) {
				out.add(new QuestCondition.StatusIs(
					QuestStatus.valueOf(token.substring("STATUS_IS:".length()))));
			} else if (token.startsWith("HAS_ITEM:")) {
				// wave B 物品轴：登记表 HAS_ITEM:id:count（expected 恒 true——XML 口径
				// booleanOrDefault(expected, true)，builder 未见 false 变体，越界即 fail-closed）。
				String[] v = token.substring("HAS_ITEM:".length()).split(":");
				out.add(new QuestCondition.HasItem(Integer.parseInt(v[0]), Integer.parseInt(v[1]), true));
			} else if (token.startsWith("VAR_IS:")) {
				String[] kv = token.substring("VAR_IS:".length()).split("=");
				out.add(new QuestCondition.QuestVariableIs(kv[0], Integer.parseInt(kv[1])));
			} else if (token.startsWith("VAR_AT_LEAST:")) {
				String[] kv = token.substring("VAR_AT_LEAST:".length()).split("=");
				out.add(new QuestCondition.VariableAtLeast(kv[0], Integer.parseInt(kv[1])));
			} else if (token.startsWith("VAR_BELOW:")) {
				String[] kv = token.substring("VAR_BELOW:".length()).split("=");
				out.add(new QuestCondition.VariableBelow(kv[0], Integer.parseInt(kv[1])));
			} else {
				throw new IllegalStateException("unknown condition token: " + token);
			}
		}
		return List.copyOf(out);
	}

	/** 动作解码（fail-closed）。 / Action token decoding. */
	private static List<QuestAction> decodeActions(String encoded) {
		if ("-".equals(encoded) || encoded.isEmpty()) {
			return List.of();
		}
		List<QuestAction> out = new ArrayList<>();
		for (String token : encoded.split(";")) {
			if (token.startsWith("SET_VAR:")) {
				String[] kv = token.substring("SET_VAR:".length()).split("=");
				out.add(new QuestAction.SetVariable(kv[0], Integer.parseInt(kv[1])));
			} else if (token.startsWith("GRANT:")) {
				String[] v = token.substring("GRANT:".length()).split(":");
				out.add(new QuestAction.GrantReward(v[0], Integer.parseInt(v[1]),
					Integer.parseInt(v[2]), QuestRewardAmountMode.valueOf(v[3])));
			} else if (token.startsWith("REMOVE_ITEM:")) {
				String[] v = token.substring("REMOVE_ITEM:".length()).split(":");
				out.add(new QuestAction.RemoveItem(Integer.parseInt(v[0]), Integer.parseInt(v[1])));
			} else if (token.startsWith("GIVE_ITEM:")) {
				// wave B 物品轴：阶段发物（give_itemN）与接取发物（give_item → npc-start accept-actions）。
				String[] v = token.substring("GIVE_ITEM:".length()).split(":");
				out.add(new QuestAction.GiveItem(Integer.parseInt(v[0]), Integer.parseInt(v[1])));
			} else if (token.startsWith("COMPLETE_QUEST:")) {
				out.add(new QuestAction.CompleteQuest(
					Integer.parseInt(token.substring("COMPLETE_QUEST:".length()))));
			} else {
				throw new IllegalStateException("unknown action token: " + token);
			}
		}
		return List.copyOf(out);
	}

	/** after-commit 解码（fail-closed；dialog type 决定 ShowQuestDialog/ShowQuestSelectionDialog）。 */
	/** S3c-D / R-REP 退场守卫（fail-closed）：报告页退场对象**不得携带物品或阶段载荷**——本路径**没有承接边**
	 * （不合成新交付边、翻面也不退场），载荷一旦出现即会静默消失（实测 39 条对象全空）。 /
	 * R-REP retirement must be payload-free: this path has no carrying edge. */
	private static void assertReportPageRetirementQuiet(RetailSimpleTalkTable.Entry entry,
			RetailClientTalkChainSteps.RouteRecord route) {
		for (String token : (route.conditions() + ";" + route.actions()).split(";")) {
			String trimmed = token.trim();
			if (trimmed.isEmpty() || trimmed.equals("-") || "START_ELIGIBLE".equals(trimmed)) {
				continue;
			}
			throw new IllegalStateException("R-REP report-page retirement carries a payload without a carrier:"
				+ " quest " + entry.questId() + " token " + trimmed);
		}
	}

	private static List<AfterCommitAction> decodeAfters(String encoded) {
		if ("-".equals(encoded)) {
			return List.of();
		}
		List<AfterCommitAction> out = new ArrayList<>();
		for (String token : encoded.split(";")) {
			if (token.startsWith("SYNC:")) {
				out.add(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.valueOf(token.substring("SYNC:".length()))));
			} else if (token.startsWith("DIALOG:SHOW_QUEST_PAGE:")) {
				out.add(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.valueOf(token.substring("DIALOG:SHOW_QUEST_PAGE:".length())).id()));
			} else if (token.startsWith("DIALOG:SHOW_SELECTION_PAGE:")) {
				out.add(new AfterCommitAction.ShowQuestSelectionDialog(
					QuestDialogPage.valueOf(token.substring("DIALOG:SHOW_SELECTION_PAGE:".length())).id()));
			} else if (token.equals("CLOSE")) {
				out.add(new AfterCommitAction.CloseDialog());
			} else if (token.equals("REFRESH_PLAYER_STATS")) {
				out.add(new AfterCommitAction.RefreshPlayerStats());
			} else if (token.startsWith("MOVIE:")) {
				// P0c-10k 过场轴：MOVIE:<movieId>:<type>（cs1_haction 触发，真端表 cutsceneid1）。
				String[] v = token.substring("MOVIE:".length()).split(":");
				out.add(new AfterCommitAction.PlayMovie(Integer.parseInt(v[0]),
					QuestMovieType.valueOf(v[1])));
			} else if (token.startsWith("TELEPORT:")) {
				String[] v = token.substring("TELEPORT:".length()).split(":");
				out.add(new AfterCommitAction.TeleportPlayer(Integer.parseInt(v[0]),
					Float.parseFloat(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3]),
					Byte.parseByte(v[4])));
			} else {
				throw new IllegalStateException("unknown after-commit token: " + token);
			}
		}
		return List.copyOf(out);
	}

	/**
	 * 链式接取流：npc-start 块参数化——selection-sources 与 start-page 逐字取登记表，
	 * 其余与单步 {@link #acceptFlow(int)} 同构。
	 * Chain accept flow: npc-start block parameterized by the registry's selection sources and page.
	 */
	private static List<QuestTransition> acceptFlowChain(int acquiredNpc, List<String> selectionSources,
			String startPage, String target, List<QuestAction> acceptActions) {
		// 与 QuestXmlBlockExpander.expandNpcStart 同口径：finish 源 = {source, target} ∪ selection-sources；
		// 块展开路由若与显式原始路由同 (source, npc, action) 则被覆盖（expander 的 explicitDialogRoutes 过滤）。
		List<QuestTransition> flow = new ArrayList<>(acceptFlow(acquiredNpc, target, acceptActions));
		flow.removeIf(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.QUEST_SELECT.id())));
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_SELECT, "unaccepted", "unaccepted", null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.valueOf(startPage).id()))));
		Set<String> finishSources = new TreeSet<>(List.of("unaccepted", target));
		selectionSources.stream().filter(source -> !source.equals("-")).forEach(finishSources::add);
		flow.removeIf(transition -> transition.event().equals(
			new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.FINISH_DIALOG.id())));
		for (String source : finishSources) {
			flow.add(talk(acquiredNpc, QuestDialogAction.FINISH_DIALOG, source, source, null,
				List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()))));
		}
		return flow;
	}

	/** 链式报告流：交付按钮取客户端，门物品取真端元数据。 / Client button and retail item gate. */
	private static List<QuestTransition> reportFlowChain(int rewardNpc, String source, String target,
			String page, Map<String, QuestNode> nodeByLabel, List<QuestItemRequirement> items,
			boolean itemCheck, RetailClientDialogExits exits, int questId) {
		QuestStatus targetStatus = nodeByLabel.containsKey(target)
			? nodeByLabel.get(target).projection().status() : QuestStatus.REWARD;
		QuestStateSyncMode mode = targetStatus == QuestStatus.REWARD
			|| targetStatus == QuestStatus.COMPLETE
			? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
		boolean checkButton = exits.requires(questId, RetailClientDialogExits.SELECT5_CHECK)
			|| exits.requires(questId, RetailClientDialogExits.SELECT5_CHECK_SIMPLE);
		boolean requiresItems = checkButton || itemCheck && !items.isEmpty();
		if (requiresItems && items.isEmpty()) {
			throw new IllegalArgumentException("retail report item gate missing: " + questId);
		}
		List<QuestTransition> flow = new ArrayList<>();
		flow.add(talk(rewardNpc, QuestDialogAction.QUEST_SELECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.valueOf(page).id()))));
		List<QuestCondition> conditions = new ArrayList<>();
		List<QuestAction> actions = new ArrayList<>();
		if (requiresItems) {
			for (QuestItemRequirement item : items) {
				conditions.add(new QuestCondition.HasItem(item.itemId(), item.count()));
				actions.add(new QuestAction.RemoveItem(item.itemId(), item.count()));
			}
		}
		List<AfterCommitAction> success = List.of(new AfterCommitAction.SyncQuestState(mode),
			new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
		if (checkButton) {
			for (QuestDialogAction button : List.of(QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE)) {
				flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, button.id()),
					List.copyOf(conditions), List.copyOf(actions), target, success, 0, source));
				AfterCommitAction failure = button == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM
					&& exits.requires(questId, RetailClientDialogExits.SELECT6)
					? new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())
					: new AfterCommitAction.CloseDialog();
				flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, button.id()),
					List.of(), List.of(), source, List.of(failure), 1, source));
			}
			if (exits.requires(questId, RetailClientDialogExits.SELECT6)) {
				flow.add(talk(rewardNpc, QuestDialogAction.FINISH_DIALOG, source, source, null,
					List.of(new AfterCommitAction.ShowQuestSelectionDialog(
						QuestDialogPage.SELECT_QUEST.id()))));
			}
		} else {
			QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(rewardNpc,
				QuestDialogAction.SELECT_QUEST_REWARD.id());
			flow.add(new QuestTransition(event, List.copyOf(conditions), List.copyOf(actions), target,
				success, requiresItems ? 0 : null, source));
			if (requiresItems) {
				flow.add(new QuestTransition(event, List.of(), List.of(), source,
					List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.valueOf(page).id())),
					1, source));
			}
		}
		return List.copyOf(flow);
	}

	/**
	 * item_check 门路由（编译器级糖元素 {@code npc-item-report}，P0c-34）：两个检查按钮
	 * ({@code CHECK_USER_HAS_QUEST_ITEM} / {@code ..._SIMPLE}) 各出一对成功/失败路由——成功扣物
	 * 并推进到 target、失败留在 source（缺省 SELECT6，{@code CLOSE} 则关窗）。形状合同与
	 * {@code QuestXmlBlockExpander.expandNpcItemReport} 同口径：source 必须投影 START、
	 * target 必须投影 REWARD，remove-count 只能是 required 或 ALL。
	 * Item-check gate routes for the compiler-level {@code npc-item-report} sugar (P0c-34).
	 */
	private static List<QuestTransition> itemReportGate(int questId,
			RetailClientTalkChainSteps.ItemReportRecord report, Map<String, QuestNode> nodeByLabel) {
		QuestNode sourceNode = nodeByLabel.get(report.source());
		QuestNode targetNode = nodeByLabel.get(report.target());
		if (sourceNode == null || targetNode == null) {
			throw new IllegalArgumentException("item-report node missing: " + questId);
		}
		if (sourceNode.projection().status() != QuestStatus.START) {
			throw new IllegalArgumentException("item-report source must project START: " + questId);
		}
		if (targetNode.projection().status() != QuestStatus.REWARD) {
			throw new IllegalArgumentException("item-report target must project REWARD: " + questId);
		}
		int removeCount = switch (report.removeCount()) {
			case "-" -> report.required();
			case "ALL" -> QuestAction.RemoveItem.ALL;
			default -> {
				int parsed = Integer.parseInt(report.removeCount());
				if (parsed != report.required()) {
					throw new IllegalArgumentException(
						"item-report remove-count must equal required or be ALL: " + questId);
				}
				yield parsed;
			}
		};
		List<QuestCondition> hasItem = List.of(
			new QuestCondition.HasItem(report.itemId(), report.required()));
		List<QuestAction> removeItem = List.of(
			new QuestAction.RemoveItem(report.itemId(), removeCount));
		List<AfterCommitAction> success = List.of(syncQuestState(targetNode),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
		AfterCommitAction failure = switch (report.failurePage()) {
			case "-" -> new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id());
			case "CLOSE" -> new AfterCommitAction.CloseDialog();
			default -> new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.valueOf(report.failurePage()).id());
		};
		QuestEvent checkButton = new QuestEvent.TalkToNpc(report.npcId(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		QuestEvent simpleButton = new QuestEvent.TalkToNpc(report.npcId(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id());
		return List.of(
			new QuestTransition(checkButton, hasItem, removeItem, report.target(), success, 0,
				report.source()),
			new QuestTransition(checkButton, List.of(), List.of(), report.source(), List.of(failure),
				1, report.source()),
			new QuestTransition(simpleButton, hasItem, removeItem, report.target(), success, 0,
				report.source()),
			new QuestTransition(simpleButton, List.of(), List.of(), report.source(),
				List.of(new AfterCommitAction.CloseDialog()), 1, report.source()));
	}

	/** 目标节点状态到同步模式的映射（与 {@code QuestXmlBlockExpander.syncQuestState} 同口径）。 /
	 * Target-node status to sync-mode mapping, same shape as the XML expander. */
	private static AfterCommitAction syncQuestState(QuestNode targetNode) {
		QuestStatus status = targetNode.projection().status();
		QuestStateSyncMode mode = status == QuestStatus.REWARD || status == QuestStatus.COMPLETE
			? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
		return new AfterCommitAction.SyncQuestState(mode);
	}

	/** 从真端工作物品和此前阶段的发放/扣除计算报告时仍应携带的物品。 /
	 * Derives carried work items from retail grants and earlier stage consumption. */
	private static List<QuestItemRequirement> carriedWorkItems(QuestMetadata metadata,
			List<QuestTransition> routes, List<QuestTransition> blocks) {
		Map<Integer, Integer> granted = new HashMap<>();
		Map<Integer, Integer> removed = new HashMap<>();
		for (QuestTransition route : java.util.stream.Stream.concat(routes.stream(), blocks.stream()).toList()) {
			if ("reward".equals(route.sourceNode()) || "complete".equals(route.targetNode())
					|| "reward".equals(route.targetNode())) {
				continue;
			}
			for (QuestAction action : route.actions()) {
				if (action instanceof QuestAction.GiveItem give) {
					granted.merge(give.itemId(), give.count(), Math::max);
				} else if (action instanceof QuestAction.RemoveItem remove) {
					removed.merge(remove.itemId(), remove.count(), Math::max);
				}
			}
		}
		List<QuestItemRequirement> carried = new ArrayList<>();
		for (QuestItemRequirement item : metadata.questWorkItems()) {
			int count = Math.min(item.count(), granted.getOrDefault(item.itemId(), 0))
				- removed.getOrDefault(item.itemId(), 0);
			if (count > 0) {
				carried.add(new QuestItemRequirement(item.itemId(), count));
			}
		}
		return List.copyOf(carried);
	}

	/**
	 * 接取流：与 {@code npc-start} 展开同构（selection-sources = unaccepted + started），
	 * 但不含 SimpleHunt 的"报告 NPC 未接任务出口"（SimpleTalk 的任务 XML 没有这两条）。
	 * The canonical accept flow, isomorphic to the npc-start expansion without the reward-NPC exit.
	 */
	/**
	 * 单步接取发物解析（P0c-10m）：give_item 无编号符号 → quest_data.xml quest_work_items
	 * 首项（单步行只声明 1 个符号；字母序通道判例 2515）。符号不在 work_items 命名域 = null（无发物）。
	 * Resolves the single-step accept grant via the quest_data work-items channel; null when absent.
	 */
	private static List<QuestAction> acceptGiveItemActions(RetailSimpleTalkTable.Entry entry) {
		String symbol = entry.giveItemSymbol();
		if (symbol == null || symbol.isBlank()) {
			return List.of();
		}
		Integer itemId = RetailQuestWorkItems.first(symbol, entry.questId());
		if (itemId == null) {
			return List.of();
		}
		String[] parts = symbol.trim().split("\\s+");
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		return List.of(new QuestAction.GiveItem(itemId, count));
	}

	/**
	 * S1 规范形过场重挂：把 {@link AfterCommitAction.PlayMovie}（{@code CUTSCENE} 型）插到"下发目标窗页的
	 * 那条 {@code QUEST_SELECT} 边"的开窗动作之前——after 序与老 craft 行编码一致（Sync → PlayMovie → 开窗）。
	 * <p>
	 * 匹配键**必须带 source 节点**：canonical 形里同一 NPC 可以同时拥有"接取开窗"（{@code unaccepted}
	 * 自环）与"交付开窗"（{@code started→reward}）两条 {@code QUEST_SELECT} 边（判例 4056：接取与交付
	 * 同体 NPC Jackspaner），只按 {@code (npcId, actionId)} 会双命中并把电影复制到另一条边上。
	 * match 数必须**恰为 1**（fail-closed）：0 = 电影会静默丢失（旧形"零匹配原样返回"的坑），
	 * &gt;1 = 作用域键失效，两者都当场炸出来。movieId &lt; 0（无过场轴）原样返回。
	 * Scope-aware canonical cutscene attachment: the key carries the source node, and the match count must
	 * be exactly one so a silently dropped or duplicated cutscene fails loudly.
	 */
	private static List<QuestTransition> attachMovieToRoute(List<QuestTransition> transitions, String sourceNode,
			int npcId, int actionId, int movieId) {
		if (movieId < 0) {
			return transitions;
		}
		List<QuestTransition> out = new ArrayList<>(transitions.size());
		int matches = 0;
		for (QuestTransition transition : transitions) {
			if (sourceNode.equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId && talk.dialogId() != null && talk.dialogId() == actionId) {
				matches++;
				List<AfterCommitAction> afters = new ArrayList<>(transition.afterCommit().size() + 1);
				for (AfterCommitAction action : transition.afterCommit()) {
					if (action instanceof AfterCommitAction.ShowQuestDialog
							|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
							|| action instanceof AfterCommitAction.CloseDialog) {
						afters.add(new AfterCommitAction.PlayMovie(movieId, QuestMovieType.CUTSCENE));
					}
					afters.add(action);
				}
				out.add(new QuestTransition(transition.event(), transition.conditions(), transition.actions(),
					transition.targetNode(), List.copyOf(afters), transition.priority(), transition.sourceNode()));
			} else {
				out.add(transition);
			}
		}
		if (matches != 1) {
			throw new IllegalStateException("cutscene route attachment must match exactly one transition: matches="
				+ matches + " source=" + sourceNode + " npc=" + npcId + " action=" + actionId);
		}
		return List.copyOf(out);
	}

	private static List<QuestTransition> acceptFlow(int acquiredNpc) {
		return acceptFlow(acquiredNpc, "started");
	}

	private static List<QuestTransition> acceptFlow(int acquiredNpc, String target) {
		return acceptFlow(acquiredNpc, target, List.of());
	}

	/** 接取流（acceptActions = wave B 接取发物，落到 QUEST_ACCEPT_1/SIMPLE 两条路由）。 */
	private static List<QuestTransition> acceptFlow(int acquiredNpc, String target,
			List<QuestAction> acceptActions) {
		List<QuestTransition> flow = new ArrayList<>();
		String source = "unaccepted";
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_SELECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id()))));
		flow.add(talk(acquiredNpc, QuestDialogAction.ASK_QUEST_ACCEPT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
		List<QuestCondition> eligible = List.of(new QuestCondition.StartEligible());
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.QUEST_ACCEPT_1.id()),
			eligible, acceptActions, target,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), null, source));
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()),
			eligible, acceptActions, target,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), null, source));
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_REFUSE_1, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id()))));
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_REFUSE_2,
			QuestDialogAction.QUEST_REFUSE_SIMPLE)) {
			flow.add(talk(acquiredNpc, action, source, source, null, List.of(new AfterCommitAction.CloseDialog())));
		}
		for (String finishSource : new TreeSet<>(List.of(source, target))) {
			flow.add(talk(acquiredNpc, QuestDialogAction.FINISH_DIALOG, finishSource, finishSource, null,
				List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()))));
		}
		return List.copyOf(flow);
	}

	/**
	 * S3c-obj 规范形：**物件哨兵接取者**的接取段。与 NPC 规范形同构（提交边 = `StartEligible` + 接取动作 +
	 * `Sync(VISIBILITY_REFRESH)`），差异只有两处、均有真端/客户端证据：
	 * <ol>
	 *   <li>入口边是 `USE_OBJECT` 自环并**保留入口页（select1）与中转页（接取窗 4）**——物件的对话窗口由
	 *       服务端下发（`QuestStartItemNpcAi2` 在物件无任务路由时回落 `SELECT1`），客户端 5.8 的入口页按钮
	 *       是 `HACTION_ASK_QUEST_ACCEPT(1007)`；随段退场会把入口页打成 `CLIENT_PAGE_UNREACHED`
	 *       （判例 1323 / P0c-42 三页合同：入口页 1011、接取窗 4、拒绝页 1004）。</li>
	 *   <li>关窗出口取 `CLOSE`（物件没有 NPC 的任务选择列表，登记记录同形），不套 NPC 形的任务列表页 10。</li>
	 * </ol>
	 * 提交边只登记 `QUEST_ACCEPT_1`：客户端的 ask 页对该物件只发 `1002`/`1003`，不加客户端不会发出的
	 * inert 边（合同逐键相等由 {@link #assertObjectAcceptContract} 兜底）。
	 * The object-sentinel accept ladder: the NPC canonical shape plus the client-authored entry/relay pages
	 * and the object's close exit; the commit edge is the only one the client can send.
	 */
	private static List<QuestTransition> canonicalObjectAcceptFlow(int objectNpc, String acceptTarget,
			List<QuestAction> acceptActions) {
		List<QuestTransition> flow = new ArrayList<>();
		String source = "unaccepted";
		flow.add(talk(objectNpc, QuestDialogAction.USE_OBJECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id()))));
		flow.add(talk(objectNpc, QuestDialogAction.ASK_QUEST_ACCEPT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(objectNpc, QuestDialogAction.QUEST_ACCEPT_1.id()),
			List.of(new QuestCondition.StartEligible()), acceptActions, acceptTarget,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), null, source));
		flow.add(talk(objectNpc, QuestDialogAction.QUEST_REFUSE_1, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id()))));
		for (String finishSource : new TreeSet<>(List.of(source, acceptTarget))) {
			flow.add(talk(objectNpc, QuestDialogAction.FINISH_DIALOG, finishSource, finishSource, null,
				List.of(new AfterCommitAction.CloseDialog())));
		}
		return List.copyOf(flow);
	}

	/**
	 * S3c-obj 合同守卫（fail-closed）：合成梯的键集必须与该物件在登记表里的**整条对话面**逐键相等
	 * （物件 NPC 上的每条记录都要有承接边，承接边也只能是声明过的键），且入口页/中转页必须与客户端合同
	 * 常量一致。多一条 = 发明客户端不会发的按钮，少一条 = 静默丢出口；客户端合同变更即拒绝编译，交裁定。
	 * Fail-closed contract guard for the object accept ladder: key-set equality plus the client page contract.
	 */
	private static void assertObjectAcceptContract(int questId, int objectNpc,
			List<QuestTransition> transitions, RetailClientTalkChainSteps steps) {
		Set<String> emitted = new TreeSet<>();
		int entryPages = 0;
		int relayPages = 0;
		for (QuestTransition transition : transitions) {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.dialogId() == null
					|| talk.npcId() != objectNpc) {
				continue;
			}
			emitted.add(transition.sourceNode() + ":" + talk.npcId() + ":" + talk.dialogId());
			if (talk.dialogId() == QuestDialogAction.USE_OBJECT.id()
					&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SELECT1.id()))) {
				entryPages++;
			}
			if (talk.dialogId() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
					&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))) {
				relayPages++;
			}
		}
		Set<String> declared = new TreeSet<>();
		// 合同侧的页也必须与合成常量互证：客户端合同（登记表记录的页下发）变更即拒绝编译——只核自己
		// 合成的边等于自证，核不到"客户端真的要哪一页"。
		// The declared contract pages are cross-checked against the synthesized constants too.
		Set<Integer> declaredEntryPages = new TreeSet<>();
		Set<Integer> declaredRelayPages = new TreeSet<>();
		for (RetailClientTalkChainSteps.RouteRecord route : steps.routes(questId)) {
			if (route.npcId() != objectNpc || !"TALK".equals(route.eventType())) {
				continue;
			}
			Integer dialogId = tryParseDialogAction(route.action());
			if (dialogId == null) {
				throw new IllegalStateException("object accept ladder carries an unparsable action: quest "
					+ questId + " action " + route.action());
			}
			declared.add(route.source() + ":" + route.npcId() + ":" + dialogId);
			Set<Integer> pushed = new TreeSet<>();
			for (String page : chainPushedPages(route.afterCommits())) {
				try {
					pushed.add(QuestDialogPage.valueOf(page).id());
				} catch (IllegalArgumentException e) {
					throw new IllegalStateException("object accept ladder pushes an unknown page: quest "
						+ questId + " page " + page);
				}
			}
			if (dialogId == QuestDialogAction.USE_OBJECT.id()) {
				declaredEntryPages.addAll(pushed);
			}
			if (dialogId == QuestDialogAction.ASK_QUEST_ACCEPT.id()) {
				declaredRelayPages.addAll(pushed);
			}
		}
		if (!emitted.equals(declared)) {
			throw new IllegalStateException("object accept ladder key drift: quest " + questId
				+ " emitted=" + emitted + " declared=" + declared);
		}
		if (entryPages != 1 || relayPages != 1
				|| !declaredEntryPages.equals(Set.of(QuestDialogPage.SELECT1.id()))
				|| !declaredRelayPages.equals(Set.of(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))) {
			throw new IllegalStateException("object accept ladder page contract broken: quest " + questId
				+ " entryPages=" + entryPages + " relayPages=" + relayPages
				+ " declaredEntry=" + declaredEntryPages + " declaredRelay=" + declaredRelayPages);
		}
	}

	/**
	 * 接取续页流：真端模板表没有"页链"列，{@code SELECT1_1} 来自客户端对话出口登记表
	 * （客户端 5.8 的 select1 页按钮 {@code HACTION_SELECT1_1}）。
	 * Client dialog continuation for select1; the retail template table carries no page-chain column.
	 */
	private static List<QuestTransition> acceptContinuation(int acquiredNpc, boolean continues) {
		List<QuestTransition> flow = new ArrayList<>(2);
		flow.add(talk(acquiredNpc, QuestDialogAction.SELECT1_1, "unaccepted", "unaccepted", null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id()))));
		if (continues) {
			flow.add(talk(acquiredNpc, QuestDialogAction.SELECT1_1_1, "unaccepted", "unaccepted", null,
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1_1.id()))));
		}
		return List.copyOf(flow);
	}

	/**
	 * 报告 NPC 的关窗出口：交付检查失败会下发 select6 页，该页按钮是 {@code HACTION_FINISH_DIALOG}。
	 * 两个 NPC 相同时接取流已登记同形路由，避免重复转换；系统发放行没有接取流，因此始终登记。
	 * Close-dialog exit for the report NPC; skipped only when the accept flow already registered it.
	 */
	private static List<QuestTransition> reportNpcExit(boolean systemGrant, int acquiredNpc, int rewardNpc) {
		if (!systemGrant && acquiredNpc == rewardNpc) {
			return List.of();
		}
		return List.of(talk(rewardNpc, QuestDialogAction.FINISH_DIALOG, "started", "started", null,
			List.of(new AfterCommitAction.CloseDialog())));
	}

	/**
	 * P0c-10o：工作物品校验需求合成——give_item 符号经 quest_data work_items 通道解析
	 * （与接取发物同通道、同物），数量取符号列（缺省 1）。
	 * Synthesizes the work-item requirement for rows whose item_check targets the accepted doc.
	 */
	private static QuestItemRequirement workItemRequirement(RetailSimpleTalkTable.Entry entry) {
		Integer itemId = RetailQuestWorkItems.first(entry.giveItemSymbol(), entry.questId());
		if (itemId == null) {
			return null;
		}
		String[] parts = entry.giveItemSymbol().trim().split("\\s+");
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		return new QuestItemRequirement(itemId, count);
	}

	/**
	 * 完成流（B 记录全参数回放；P0c-10h B-3）：与 {@code expandNpcComplete} 一一对应——
	 * 固定奖励按 fixed-reward-indices 索引、可选项仅经 choice 子元素授予（旧行的按位近似
	 * 只在无 selectables 时与其重合）、预览按 complete-reward-index 下发本档奖励窗口。
	 * Parameterized completion flow replayed from the registry's NPC_COMPLETE record.
	 */
	private static List<QuestTransition> completeFlowFromBlock(QuestMetadata metadata,
			RetailClientTalkChainSteps.BlockRecord block) {
		Map<String, String> sections = new java.util.LinkedHashMap<>();
		for (String section : block.extra().split("\\|")) {
			int eq = section.indexOf('=');
			sections.put(section.substring(0, eq), section.substring(eq + 1));
		}
		int completeRewardIndex = "cri=-".equals(sections.getOrDefault("cri", "-")) ? 0
			: Integer.parseInt(sections.get("cri"));
		List<QuestReward> group = rewardGroup(metadata, completeRewardIndex);
		List<QuestAction> fixedRewards = new ArrayList<>();
		if (sections.get("fixed").equals("RETAIL")) {
			for (QuestReward reward : group) {
				if (QuestRewardKind.fromWire(reward.kind()) != QuestRewardKind.SELECTABLE_ITEM) {
					fixedRewards.add(grant(reward));
				}
			}
		} else if (!sections.get("fixed").equals("-")) {
			for (String token : sections.get("fixed").split(" ")) {
				QuestReward reward = rewardAt(group, Integer.parseInt(token));
				if (QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM) {
					throw new IllegalArgumentException(
						"fixed reward index " + token + " is SELECTABLE_ITEM");
				}
				fixedRewards.add(grant(reward));
			}
		}
		List<QuestTransition> flow = new ArrayList<>();
		// 预览路由必须下发本档自己的奖励窗口（多档任务重开窗口渲染正确文案与奖励）。
		if (!sections.get("preview").equals("-")) {
			int previewPage = QuestDialogPage.rewardWindowForTier(completeRewardIndex)
				.map(QuestDialogPage::id)
				.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id());
			for (int dialogId : resolveDialogIds(sections.get("preview"))) {
				flow.add(new QuestTransition(new QuestEvent.TalkToNpc(block.npcId(), dialogId),
					List.of(), List.of(), block.source(),
					List.of(new AfterCommitAction.ShowQuestDialog(previewPage)), null, block.source()));
			}
		}
		record CompletionRoute(int dialogId, QuestAction choiceReward) {
		}
		List<CompletionRoute> routes = new ArrayList<>();
		if (sections.get("choice").equals("RETAIL")) {
			List<QuestReward> selectable = group.stream()
				.filter(reward -> QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM)
				.toList();
			for (int dialogId : resolveDialogIds(sections.get("actions"))) {
				int choiceIndex = dialogId - QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				QuestAction choice = choiceIndex >= 0 && choiceIndex < selectable.size()
					? grant(selectable.get(choiceIndex)) : null;
				routes.add(new CompletionRoute(dialogId, choice));
			}
		} else if (!sections.get("actions").equals("-")) {
			for (int dialogId : resolveDialogIds(sections.get("actions"))) {
				routes.add(new CompletionRoute(dialogId, null));
			}
		}
		if (!sections.get("choice").equals("-") && !sections.get("choice").equals("RETAIL")) {
			for (String token : sections.get("choice").split(";")) {
				int colon = token.indexOf(':');
				QuestReward reward = rewardAt(group, Integer.parseInt(token.substring(0, colon)));
				for (int dialogId : resolveDialogIds(token.substring(colon + 1))) {
					routes.add(new CompletionRoute(dialogId, grant(reward)));
				}
			}
		}
		if (!sections.get("fallback").equals("-")) {
			for (int dialogId : resolveDialogIds(sections.get("fallback"))) {
				routes.add(new CompletionRoute(dialogId, null));
			}
		}
		String finish = sections.get("finish");
		for (CompletionRoute route : routes) {
			List<QuestAction> actions = new ArrayList<>(fixedRewards);
			if (route.choiceReward() != null) {
				actions.add(route.choiceReward());
			}
			actions.add(new QuestAction.CompleteQuest(completeRewardIndex));
			List<AfterCommitAction> afterCommit = new ArrayList<>(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION)));
			switch (finish) {
				case "SELECTION_DIALOG" -> afterCommit.add(new AfterCommitAction.ShowQuestSelectionDialog(10));
				case "CLOSE_DIALOG" -> afterCommit.add(new AfterCommitAction.CloseDialog());
				case "NONE" -> {
				}
				default -> throw new IllegalArgumentException("invalid finish: " + finish);
			}
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(block.npcId(), route.dialogId()),
				List.of(), List.copyOf(actions), block.target(), afterCommit, null, block.source()));
		}
		return List.copyOf(flow);
	}

	private static List<QuestReward> rewardGroup(QuestMetadata metadata, int tier) {
		List<QuestRewardGroup> groups = metadata.rewardGroups();
		if (tier < 0 || tier >= groups.size()) {
			throw new IllegalArgumentException("reward tier " + tier + " out of range: " + groups.size());
		}
		return groups.get(tier).rewards();
	}

	private static QuestReward rewardAt(List<QuestReward> group, int index) {
		if (index < 0 || index >= group.size()) {
			throw new IllegalArgumentException("reward index " + index + " out of range: " + group.size());
		}
		return group.get(index);
	}

	/** 动作 raw 解析（与 dialogActions 同语义）：枚举名优先、数字兜底、{@code A..B} 区间展开。 */
	private static List<Integer> resolveDialogIds(String raw) {
		List<Integer> ids = new ArrayList<>();
		for (String token : raw.split("\\s+")) {
			int delimiter = token.indexOf("..");
			if (delimiter < 0) {
				ids.add(parseDialogAction(token));
			} else {
				int first = parseDialogAction(token.substring(0, delimiter));
				int last = parseDialogAction(token.substring(delimiter + 2));
				if (first > last || last - first >= 256) {
					throw new IllegalArgumentException("invalid dialog action range: " + token);
				}
				for (int id = first; id <= last; id++) {
					ids.add(id);
				}
			}
		}
		return ids;
	}

	/** 完成流：与 {@code npc-complete} 展开同构（固定奖励 + 可选项 8..23 + NOREWARD 收尾）。 / Canonical completion flow. */
	private static List<QuestTransition> completeFlow(QuestMetadata metadata, Collection<Integer> rewardNpcs) {
		List<QuestTransition> flow = new ArrayList<>();
		for (int rewardNpc : rewardNpcs) {
			flow.addAll(npcCompleteFlow(metadata, rewardNpc));
		}
		if (metadata.rewardGroups().size() > 1) {
			throw new IllegalArgumentException("multi-tier rewards not supported: "
				+ metadata.rewardGroups().size() + " groups");
		}
		List<QuestReward> group = metadata.rewardGroups().isEmpty()
			? List.of() : metadata.rewardGroups().get(0).rewards();
		List<QuestAction> fixedRewards = new ArrayList<>();
		List<QuestReward> selectables = new ArrayList<>();
		for (QuestReward reward : group) {
			if (QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM) {
				selectables.add(reward);
			} else {
				fixedRewards.add(grant(reward));
			}
		}
		flow.addAll(RetailSimpleHuntDefinitionCompiler.rewardWindowAutoFlow(rewardNpcs, fixedRewards,
			selectables, metadata.classRewards(), "reward", "complete"));
		return List.copyOf(flow);
	}

	private static List<QuestTransition> npcCompleteFlow(QuestMetadata metadata, int rewardNpc) {
		if (metadata.rewardGroups().size() > 1) {
			throw new IllegalArgumentException("multi-tier rewards not supported: "
				+ metadata.rewardGroups().size() + " groups");
		}
		List<QuestReward> group = metadata.rewardGroups().isEmpty()
			? List.of() : metadata.rewardGroups().get(0).rewards();
		List<QuestAction> fixedRewards = new ArrayList<>();
		List<QuestReward> selectables = new ArrayList<>();
		for (QuestReward reward : group) {
			if (QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM) {
				selectables.add(reward);
			} else {
				fixedRewards.add(grant(reward));
			}
		}
		List<QuestTransition> flow = new ArrayList<>();
		for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
			QuestDialogAction.SELECT_QUEST_REWARD)) {
			flow.add(talk(rewardNpc, preview, "reward", "reward", null,
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))));
		}
		List<QuestDialogAction> confirmActions = new ArrayList<>();
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			confirmActions.add(QuestDialogAction.fromId(id));
		}
		for (QuestDialogAction action : confirmActions) {
			int selectableIndex = action.id() - QuestDialogAction.SELECTED_QUEST_REWARD1.id();
			List<QuestAction> actions = new ArrayList<>(fixedRewards);
			if (action != QuestDialogAction.SELECTED_QUEST_NOREWARD && selectableIndex < selectables.size()) {
				actions.add(grant(selectables.get(selectableIndex)));
			}
			actions.add(new QuestAction.CompleteQuest(0));
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, action.id()), List.of(),
				List.copyOf(actions), "complete",
				List.of(new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(10)),
				null, "reward"));
		}
		return List.copyOf(flow);
	}

	private static QuestAction.GrantReward grant(QuestReward reward) {
		QuestRewardKind kind = QuestRewardKind.fromWire(reward.kind());
		QuestRewardKind actionKind = kind == QuestRewardKind.SELECTABLE_ITEM ? QuestRewardKind.ITEM : kind;
		QuestRewardAmountMode mode = switch (actionKind) {
			case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
			default -> QuestRewardAmountMode.EXACT;
		};
		return new QuestAction.GrantReward(actionKind.name(), reward.id(), reward.amount(), mode);
	}

	private static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			Integer priority, List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, priority, source);
	}

	/** 关闭出口闭包的键：同节点、同 NPC 的 FINISH_DIALOG 才算路由到。 /
	 * Close-exit closure key: only a same-node, same-npc FINISH_DIALOG counts as routing the button. */
	private static String closeKey(String sourceNode, int npcId) {
		return sourceNode + ":" + npcId + ":" + QuestDialogAction.FINISH_DIALOG.id();
	}

	/** 该路由是否下发客户端 select6 失败页。 / Whether the transition pushes the client select6 page. */
	private static boolean pushesSelect6(QuestTransition transition) {
		return transition.afterCommit().stream()
			.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog dialog
				&& dialog.dialogId() == QuestDialogPage.SELECT6.id());
	}

	/** 空集合兜底（禁止 null 传播）。 / Empty-set guard used by diagnostic paths. */
	static <T> List<T> emptyIfNull(List<T> values) {
		return values == null ? List.of() : Collections.unmodifiableList(values);
	}
}
