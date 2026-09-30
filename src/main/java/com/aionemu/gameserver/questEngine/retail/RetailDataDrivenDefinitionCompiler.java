package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;

import java.util.List;
import java.util.Objects;

/**
 * 真端 DataDriven 表行 → 完整任务定义的合成器（P5-1/P5-2 覆盖主导形状）。
 * <p>
 * P5-1：Talk 接取 + 全 Hunt 步骤（552 行）→ hunt 计数网格复用。
 * P5-2：Talk 接取 + 全 CollectItem 步骤（285 行）→ 采集族规范形（交付物由 metadata 承载、
 * 交付检查对挂在报告 NPC 上）。
 * P5-3 wave A：无进度行（Talk 接取 → 交付即完成）→ 极简对话信件（{@link RetailDataDrivenTalkCompiler}）；
 * Talk 步骤链（wave B）与 PVP/EnterArea/ItemPlay/EnterWorld 混合步骤按批补齐，
 * 未覆盖形状按稳定码 {@code RETAIL_STEP_UNSUPPORTED} 留 XML。
 * <p>
 * Synthesizes DataDriven rows for the dominant shapes: Talk acquire with pure Hunt progress
 * (reuses the hunt counter grid) or pure CollectItem progress (collect-family canonical shape).
 */
public final class RetailDataDrivenDefinitionCompiler {

	private RetailDataDrivenDefinitionCompiler() {
	}

	/** 编译结果（复用 hunt 编译器的语义）。 / Compilation outcome (hunt semantics reused). */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/** 编译一行。 / Compiles one row. */
	public static Outcome compile(RetailDataDrivenTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex, RetailQuestMetadataCompiler.Outcome metadata,
			RetailClientRewardNpcs clientRewardNpcs, RetailQuestAreaIndex questAreas,
			RetailClientDialogExits clientDialogExits,
			RetailClientSummaryRows clientSummaryRows,
			RetailClientHandinPages clientHandinPages,
			RetailQuestUseItemNpcs interactionObjects,
			RetailClientKillTargets clientKillTargets,
			RetailClientHuntProgressRows huntProgressRows) {
		return compile(entry, itemIndex, npcIndex, metadata, clientRewardNpcs, questAreas,
			clientDialogExits, clientSummaryRows, clientHandinPages,
			interactionObjects, clientKillTargets,
			huntProgressRows, RetailEnterAreaZoneResolution.empty());
	}

	/**
	 * 完整编译入口（含 enterarea 别名解析表；EA 别名未登记 → RETAIL_ENTERAREA_ZONE_UNRESOLVED）。
	 * The full compile entry (with the enterarea alias resolution table; unregistered EA aliases
	 * reject with RETAIL_ENTERAREA_ZONE_UNRESOLVED).
	 */
	public static Outcome compile(RetailDataDrivenTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex, RetailQuestMetadataCompiler.Outcome metadata,
			RetailClientRewardNpcs clientRewardNpcs, RetailQuestAreaIndex questAreas,
			RetailClientDialogExits clientDialogExits,
			RetailClientSummaryRows clientSummaryRows,
			RetailClientHandinPages clientHandinPages,
			RetailQuestUseItemNpcs interactionObjects,
			RetailClientKillTargets clientKillTargets,
			RetailClientHuntProgressRows huntProgressRows,
			RetailEnterAreaZoneResolution enterAreaZones) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(metadata, "metadata");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		// P5-4：接取形三分类——Talk 走 NPC 唯一性门；EnterArea/none 无 NPC，由世界 quest_area 进区域
		// 发放（P0c-4 机制），暂只在 hunt/pvp 计数网格族落地，其余族如实拒绝、保留 XML。
		// P5-4: three acquire shapes — Talk via the NPC uniqueness gate; EnterArea/none have no NPC and
		// are granted on world quest-area entry (P0c-4), landing only in the hunt/pvp grid family.
		String acquireCategory = entry.acquireCategory() == null ? "" : entry.acquireCategory();
		String rewardName = entry.rewardNpc() == null ? "" : entry.rewardNpc();
		boolean isEnterWorld = "enterworld".equalsIgnoreCase(acquireCategory);
		boolean isEnterArea = "enterarea".equalsIgnoreCase(acquireCategory);
		boolean isNone = "none".equalsIgnoreCase(acquireCategory);
		boolean isLevelUpLogIn = "leveluplogin".equalsIgnoreCase(acquireCategory);
		boolean noneNpcAcquire = isNone && !questAreas.isBound(entry.questId())
			&& (entry.allHunt() || entry.allPvp()) && !rewardName.isBlank();
		// 物品接取轴（P0c-56）：真端 `category_acquire_ = ItemPlay` 的接取参数是**任务起始道具**符号
		// （13952 形：道具模板自带 `<queststart questid=...>`/`<read/>`，遗留 XML 的接取边正是
		// `<use-item item-id=...>` + 无主 QUEST_ACCEPT_SIMPLE/QUEST_REFUSE_SIMPLE，客户端该行
		// select_none 页也只有 20000/20001 两个按钮）⇒ 无接取 NPC，发放由「使用道具」触发。
		// The item-acquire axis (P0c-56): with `category_acquire_ = ItemPlay` the parameter is a
		// quest-start item symbol (the 13952 shape — the item template declares queststart/read, the
		// legacy accept edge is `<use-item>`, and the client's select_none page for the row carries
		// exactly the two buttons 20000/20001) ⇒ no acquire npc; the item use opens the window.
		boolean itemAcquire = "itemplay".equalsIgnoreCase(acquireCategory);
		// 事件轴（`category1=event`）的接取语义未落地：其遗留见证是**事件 NPC 接取**（80885→834244 /
		// 80940→835137 / 80961→835607），与本轴的「使用道具触发」冲突，且发放/激活（EventActive）轴
		// 已登记为真端缺口 ⇒ 本通道不覆盖，按更准确的码暂缓（不再谎报「接取 NPC 解析失败」）。判据
		// 优先于道具解析：事件行整体延后，不因道具表变动在 ITEM_UNRESOLVED/EVENT_DEFERRED 之间漂移。
		// The event axis (category1=event) keeps its own decision: its legacy witness acquires at an
		// event npc (80885→834244, 80940→835137, 80961→835607), which contradicts the item trigger,
		// and the event activation/EventActive axis is a registered retail gap ⇒ this channel does not
		// cover those rows; they defer under a more accurate code than "acquire npc unresolved". This
		// predicate outranks the item resolution so event rows never drift between
		// ITEM_UNRESOLVED and EVENT_DEFERRED as the item table changes.
		if (itemAcquire && "EVENT".equals(metadata.metadata().category())) {
			return new Outcome(null, "RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED", acquireCategory);
		}
		Integer itemAcquireId = itemAcquire ? itemIndex.resolve(itemSymbol(entry.acquireParam())) : null;
		if (itemAcquire && itemAcquireId == null) {
			return new Outcome(null, "RETAIL_ITEMPLAY_ACQUIRE_ITEM_UNRESOLVED",
				entry.acquireParam() == null ? "" : entry.acquireParam().trim());
		}
		// 物品接取行与升级登录行无接取 NPC（道具或系统事件发放），不查 NPC 名表；其余族照旧。
		// Item-acquired and level-up-login rows carry no acquire npc; the deliverable name channel stays untouched for the rest.
		boolean npcAcquire = (!isEnterArea && !isNone && !isEnterWorld && !itemAcquire && !isLevelUpLogIn) || noneNpcAcquire;
		int worldAcquireId = 0;
		if (isEnterWorld) {
			String param = entry.acquireParam() == null ? "" : entry.acquireParam().trim();
			if (!param.chars().allMatch(Character::isDigit) || param.isEmpty()) {
				return new Outcome(null, "RETAIL_ACQUIRE_GRANT_UNSUPPORTED", acquireCategory + " " + param);
			}
			worldAcquireId = Integer.parseInt(param);
		}
		// enterworld/enterarea/leveluplogin 接取的混合链行有发放路由（10010 进世界 / 16823 升级+区域任务结束 / 10031 等级登录），
		// 不落本门；其余非 NPC 接取形状仍如实拒绝。
		// enterworld/enterarea/leveluplogin mixed-chain rows carry grant routes (10010 world-entry / 16823
		// level-up + zone-mission-end / 10031 level-up-login) and bypass this gate; other non-npc shapes stay rejected.
		boolean grantedMixAcquire = (isEnterWorld || isEnterArea || isLevelUpLogIn)
			&& entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("hunt")
					|| category.equals("collectitem") || category.equals("enterarea")
					|| category.equals("enterworld") || category.equals("itemplay")
					|| category.equals("talkfobj"))
			&& (entry.stepCategories().contains("hunt") || entry.stepCategories().contains("talk")
				|| entry.stepCategories().contains("itemplay")
				|| isAreaWorldOnly(entry));
		// 区域接取 + 交付即完成（13842 形）：真端世界文件绑定了区域 → 进区域发放、无接取 NPC；
		// 未绑定的同形行（15548 形：等级里程碑误标 EnterArea）保持拒绝。
		// Area acquire with a no-progress row (the 13842 shape): bound in the retail world files —
		// granted on area entry with no acquire npc; unbound look-alikes stay rejected.
		boolean areaNoProgressAcquire = isEnterArea && entry.noProgress()
			&& questAreas.isBound(entry.questId());
		// 链式接取（none）混合行：前序任务完成 / 区域任务结束自动接取（10011/10501 形），
		// 步词汇与混合链同域（talk/hunt/collectitem/enterarea + itemplay/talkfobj/enterworld 骑行者）。
		// Chain-acquire (none) mixed rows: auto-granted on prior-quest completion / zone-mission end
		// (the 10011/10501 shapes); the step vocabulary shares the mixed-chain domain
		// (talk/hunt/collectitem/enterarea plus the itemplay/talkfobj/enterworld riders).
		boolean chainMixAcquire = isNone && !entry.stepCategories().isEmpty()
			&& entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("hunt")
					|| category.equals("collectitem") || category.equals("enterarea")
					|| category.equals("itemplay") || category.equals("talkfobj")
					|| category.equals("enterworld"))
			&& (entry.stepCategories().contains("talk") || entry.stepCategories().contains("hunt"));
		// 物品接取行（itemAcquire）与 hunt/pvp 网格行同权：它们都有自己的接取段（使用道具开窗），
		// 不走 NPC 接取名门。
		// Item-acquired rows join the hunt/pvp grid rows here: both carry their own accept segment
		// (item use opens the window) and do not ride the npc acquire name gate.
		// 纯 Talk 链行的接取词汇与混合链同域（Talk/EnterArea/EnterWorld/none/LevelUpLogIn 均有
		// 已裁定发放边）；只要有 talk 步，就不把该行挡在接取轴门外。
		// Pure-talk chain rows share the mixed chain's acquire vocabulary (talk / enterarea /
		// enterworld / none / leveluplogin all have adjudicated grant edges); any talk step keeps
		// the row out of the acquire-axis gate.
		boolean talkChainAcquire = entry.allTalk() && entry.stepCategories().contains("talk");
		if (!npcAcquire && !itemAcquire && !grantedMixAcquire && !areaNoProgressAcquire
				&& !chainMixAcquire && !talkChainAcquire && !entry.allHunt() && !entry.allPvp()) {
			return new Outcome(null, "RETAIL_ACQUIRE_GRANT_UNSUPPORTED", acquireCategory);
		}
		// select_none 阶梯：客户端未接态首屏 select_none 的唯一按钮是续页 4763 时，接取流必须发
		// 续页路由（否则契约门禁报 BUTTON_WITHOUT_ROUTE，15478 形）；两个混合链编译器共用本判据。
		// The select_none rung: when the client's unaccepted first page select_none turns to page 4763,
		// the accept flow must route it (else the contract gate reports BUTTON_WITHOUT_ROUTE); both
		// mixed-chain compilers share this predicate.
		boolean selectNoneLadder = clientDialogExits.requires(entry.questId(),
			RetailClientDialogExits.SELECT_NONE_1);
		if (entry.allTalk() && !hasEveryTalkStageHead(entry.questId(), entry.talkSteps().size())) {
			// P5-3 wave B：客户端契约缺阶段首屏（select{i} 无同名页、也无标准 selectN 页）→ 如实拒绝，
			// 不按页梯登记表发明页。阶段内翻页由客户端本地完成，服务端不再消费逐页路由。
			// Wave B: the client contract lacks a stage head (no same-named select{i} page and no standard
			// selectN page) → stay deferred instead of inventing pages from the ladder registry. In-stage
			// page turns stay client-local, so no per-page routes are consumed anymore.
			return new Outcome(null, "RETAIL_TALK_CHAIN_DEFERRED", "steps=" + entry.talkSteps().size());
		}
		if (entry.noProgress()) {
			// W5-g3：极简信件登记（页链）退场后，无进度行的客户端证据收窄为**任务书行**——形状的奖励
			// 投影与修复行都取 lastRowIndex，客户端无任务书时该投影静默退化为 0（无效行号）。判据可
			// 派生（quest_client_summary_rows 为常设生产表）且 fail-closed：缺行即拒。
			// W5-g3: with the minimal-letter registry retired, the client evidence for a no-progress row
			// narrows to the journal rows — both the reward projection and the repair row use
			// lastRowIndex, which silently degrades to 0 when the client has no journal (an invalid
			// row). The predicate is derivable (the summary-rows table stays in production) and
			// fail-closed: no journal row, no acceptance.
			if (clientSummaryRows.rows(entry.questId()) == 0) {
				return new Outcome(null, "RETAIL_TALK_JOURNAL_MISSING",
					"client quest journal rows absent");
			}
		} else if (isTalkCollectMix(entry)) {
			// 混合长尾切片 2：talk/collectitem 交错行在 acquire/交付 NPC 解析后路由到混合采集链合成器
			// （单 var0 阶梯：talk 步 = 信件页梯推进、collectitem 步 = 39 检查整组过/扣；15301/16942 形）。
			// Mixed-tail slice 2: talk/collectitem interleave rows route to the mixed collect-chain
			// compiler after the acquire/hand-in NPCs resolve (single var0 ladder: talks advance via
			// letter ladders, collectitem steps via the group check — the 15301/16942 shapes).
			// 链式/区域接取（none / enterarea）：前序任务完成或进区域自动接取，无接取 NPC →
			// -1 哨兵（合成器发 level-up + zone-mission-end 发放边）。
			// Chain/area acquire (none / enterarea): auto-granted with no acquire npc — the -1
			// sentinel (the synthesizer emits the level-up + zone-mission-end grant edges).
			boolean chainAcquireHere = "none".equalsIgnoreCase(acquireCategory) || isEnterArea || isLevelUpLogIn;
			var acquired = chainAcquireHere
				? java.util.Set.<Integer>of()
				: npcIndex.resolveAll(
					List.of(entry.acquireParam() == null ? "" : entry.acquireParam())).npcIds();
			var reward = npcIndex.resolveAll(
				List.of(entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
			if (!chainAcquireHere && acquired.size() != 1) {
				return new Outcome(null, "RETAIL_ACQUIRE_NPC_UNRESOLVED",
					entry.acquireParam() + " -> " + acquired);
			}
			if (reward.size() != 1) {
				return new Outcome(null, "RETAIL_REWARD_NPC_UNRESOLVED",
					entry.rewardNpc() + " -> " + reward);
			}
			var talkCollectOutcome = RetailDataDrivenTalkCollectChainCompiler.compile(entry, npcIndex,
				metadata, chainAcquireHere ? -1 : acquired.iterator().next(), reward.iterator().next(),
				clientSummaryRows, enterAreaZones, itemIndex, selectNoneLadder);
			return new Outcome(talkCollectOutcome.definition(), talkCollectOutcome.rejectionCode(),
				talkCollectOutcome.detail());
		} else if (isTalkHuntMix(entry) || isTalkCollectHuntMix(entry) || isHuntEaMix(entry)
				|| isHuntCollectMix(entry) || isTalkFobjHuntMix(entry) || isTalkFobjOnly(entry)
				|| isAreaWorldOnly(entry)
				|| isTalkAreaWorldMix(entry) || isItemPlayChain(entry) || isPvpMix(entry)) {
			// 混合长尾切片 1：talk/hunt 交错行在 acquire/交付 NPC 解析后路由到混合链合成器
			// （块序展开；信件页梯覆盖全部 talk 步）。EnterWorld 接取行（副本任务，进世界发放）
			// 无接取 NPC：acquireParam = 世界 id，走 EnterWorld + WorldIs 发放路由（10010 形）。
			// Mixed-tail slice 1: talk/hunt interleave rows route to the mixed-chain compiler after
			// the acquire/hand-in NPCs resolve. EnterWorld-acquired rows (instance quests granted on
			// world entry) have no acquire npc — acquireParam is the world id, granted via an
			// EnterWorld + WorldIs route (the 10010 shape).
			// enterarea 接取：无接取 NPC（升级/区域任务结束自动接取，16823 形），参数留空即可。
			// enterarea acquire: no acquire npc (level-up / zone-mission-end auto-grant, the 16823
			// shape); the empty parameter is expected.
			boolean areaAcquire = "enterarea".equalsIgnoreCase(acquireCategory);
			// 接取轴守卫：混合链只支持 talk/enterarea/enterworld 三种接取；其余轴（leveluplogin/
			// itemplay 等系统发放）没有接取 NPC，此处拒绝以免数字参数被 NPC 解析按 id 误收
			//（resolve 对纯数字串直接返回 id，LevelUpLogIn 的等级参数会静默变成假 NPC 路由）。
			// Acquire-axis guard: the mixed chain only supports talk/enterarea/enterworld acquires;
			// other axes (leveluplogin/itemplay system grants) have no acquire npc — reject here so a
			// numeric parameter is never mis-wired as an npc id (resolve returns the number itself
			// for all-digit names, which would silently fabricate an npc route from a level).
			boolean levelUpAcquire = "leveluplogin".equalsIgnoreCase(acquireCategory);
			// 链式接取（none）：前序任务完成 / 区域任务结束自动接取（遗留 10011/10501 形 ——
			// level-up + zone-mission-end 双事件 + StartEligible/QuestsFinished 门），与 hunt 路径
			// 的 none→AREA 归并口径一致。
			// Chain acquire (none): auto-granted on prior-quest completion / zone-mission end (the
			// legacy 10011/10501 shape), same caliber as the hunt path's none→AREA mapping.
			boolean chainAcquire = "none".equalsIgnoreCase(acquireCategory);
			if (worldAcquireId == 0 && !areaAcquire && !levelUpAcquire && !chainAcquire
					&& !"talk".equalsIgnoreCase(acquireCategory)) {
				return new Outcome(null, "RETAIL_ACQUIRE_GRANT_UNSUPPORTED", acquireCategory);
			}
			var acquired = worldAcquireId > 0 || areaAcquire || levelUpAcquire || chainAcquire
				? java.util.Set.<Integer>of()
				: npcIndex.resolvePartyName(entry.acquireParam() == null ? "" : entry.acquireParam());
			var reward = npcIndex.resolvePartyName(entry.rewardNpc() == null ? "" : entry.rewardNpc());
			// 接取 NPC 允许变体家族 / 声明组（18738 形：真端表基名展开成阶段变体家族，各建接取路由）
			// ——只要求非空，不再要求唯一。
			// The acquire npc may be a variant family or a declared dialog-name group (the 18738 shape:
			// the retail table's base name expands to a family of stage variants, each getting accept
			// routes) — only non-emptiness is required, not uniqueness.
			if (worldAcquireId == 0 && !areaAcquire && !levelUpAcquire && !chainAcquire
					&& acquired.isEmpty()) {
				return new Outcome(null, "RETAIL_ACQUIRE_NPC_UNRESOLVED",
					entry.acquireParam() + " -> " + acquired);
			}
			if (reward.size() != 1) {
				return new Outcome(null, "RETAIL_REWARD_NPC_UNRESOLVED",
					entry.rewardNpc() + " -> " + reward);
			}
			// 纯 TalkFOBJ 单步行（25070 形）只在**没有采集要求与掉落源**时才归链内 FOBJ 步：链内 FOBJ
			// 步不带物品门，若真端元数据声明 collect_item（itemRequirements）或 drop_monster（drops），
			// 放行会静默丢掉「交 N 件」合同 ⇒ 如实拒绝。**例外 = 掉落驱动的采集形**（25052 形，
			// 单步 FOBJ + collect_item + 掉落源就在该 FOBJ 上）：链编译器把它编译成「掉落源自环
			// （TalkToNpc/CanAct）+ 领奖 NPC 的 1009 组检查对」，交付合同不丢。inventoryItems 一并纳入
			// 判据（同一「物品要求」语义的另一通道），避免漏拦。
			// A pure single-step TalkFOBJ row (the 25070 shape) rides the in-chain fobj step only when it
			// declares neither a collect requirement nor a drop source: that step carries no item gate,
			// so honouring a collect_item (itemRequirements) or drop_monster (drops) declaration through
			// it would silently drop the hand-in contract — reject honestly instead. The exception is the
			// drop-driven collect form (the 25052 shape: single fobj step + collect_item + the drop
			// source on that very fobj): the chain compiler turns it into drop-source self loops plus
			// the reward npc's 1009 check pair, so the contract survives. inventoryItems is folded into
			// the same predicate as the other channel of the same "item requirement" semantics.
			if (isTalkFobjOnly(entry)
					&& !RetailDataDrivenTalkHuntChainCompiler.isDropDrivenFobjCollect(entry, metadata)
					&& (!metadata.metadata().itemRequirements().isEmpty()
						|| !metadata.metadata().inventoryItems().isEmpty()
						|| !metadata.metadata().drops().isEmpty())) {
				return new Outcome(null, "RETAIL_FOBJ_COLLECT_UNSUPPORTED",
					"itemRequirements=" + metadata.metadata().itemRequirements()
						+ " inventoryItems=" + metadata.metadata().inventoryItems()
						+ " drops=" + metadata.metadata().drops());
			}
			var talkHuntOutcome = RetailDataDrivenTalkHuntChainCompiler.compile(entry, npcIndex, metadata,
				acquired,
				reward.iterator().next(),
				interactionObjects, huntProgressRows, clientSummaryRows, enterAreaZones, itemIndex,
				levelUpAcquire, selectNoneLadder);
			return new Outcome(talkHuntOutcome.definition(), talkHuntOutcome.rejectionCode(),
				talkHuntOutcome.detail());
		} else if (!entry.allTalk() && !entry.allHunt() && !entry.allCollect() && !entry.allPvp()) {
			return new Outcome(null, "RETAIL_STEP_UNSUPPORTED", "mixed progress steps");
		}
		// 接取/交付字段走统一名字通道（精确 → 名前变体族 → 真端对话名组）：组名只在真端数据自己写出
		// 组名的字段上展开，hunt 合成器按组员逐条发接取/报告路由，与遗留 XML 的多 NPC 形同构；
		// 其余族保持精确/变体解析，组名在那里仍按未解析如实拒绝（单 owner 形的门照旧拦多值）。
		// The acquire/hand-in fields ride the unified name channel (exact, then variant families, then a
		// declared dialog-name group). Only the fields the retail data itself writes a group name into
		// widen: the hunt synthesizer emits one accept/report route per member, isomorphic to the legacy
		// multi-npc shape; every other family keeps exact/variant resolution and still rejects a group
		// name as unresolved (the single-owner gates refuse multi-value sets as before).
		boolean huntFamily = entry.allHunt() || entry.allPvp();
		// 挑战任务哨兵（P0c-58）：真端表用 `value0_acquire_ = _challengetask_`（哨兵，不是 NPC 名）
		// 表示"由挑战任务 NPC 发放"。挑战任务由**交付 NPC 本人**发放：客户端 npc 块在 804699/804719 上
		// 声明的对话路由名（`quest_ai_name`）正是真端 reward 名，客户端该行的 accept 页（select_none
		// 4762 + 20000/20001）也落在同一个 NPC 上（遗留 XML 的见证同形：`NPC_START npc-id=804699
		// start-page=SELECT_NONE` + 同 id 的 `npc-complete`）⇒ 接取名回退到真端 reward 名，
		// 仍走同一条名字通道解析（未解析即由下方门如实拒绝，不静默放行）。
		// Challenge-task sentinel (P0c-58): the retail table writes `_challengetask_` (a sentinel, not an
		// npc name) to mean "granted by the challenge-task npc". That npc is the hand-in npc itself: the
		// client npc block declares the retail reward name as its dialog routing name (`quest_ai_name`)
		// on 804699/804719, the client's accept page for the row lands on the same npc, and the legacy
		// witness is isomorphic (an NPC_START on that id plus a npc-complete on the same id) ⇒ the
		// acquire name falls back to the retail reward name and still rides the same name channel
		// (an unresolved fallback is refused by the gate below, never silently accepted).
		String acquireParam = entry.acquireParam() == null ? "" : entry.acquireParam();
		if (CHALLENGE_TASK_SENTINEL.equalsIgnoreCase(acquireParam.trim())
				&& "CHALLENGE_TASK".equals(metadata.metadata().category())) {
			acquireParam = rewardName;
		} else if (noneNpcAcquire) {
			acquireParam = rewardName;
		}
		// 无进度交付行（交付即完成）的**声明组**也走统一名字通道：18737 形 = 真端 reward 名是
		// IDRaksha_Solo_StageStart 组、客户端 npc 块把组名声明在三个阶段 NPC 上、遗留 XML 正是三个
		// npc-report/npc-complete 块。其余非 hunt 族保持精确解析（组名在那里不展开，未声明的同名
		// 多刷点照旧按歧义拒绝）。
		// A no-progress hand-in row's declared group also rides the unified name channel (the 18737
		// shape: the retail reward name is the IDRaksha_Solo_StageStart group, the client npc block
		// declares it on three stage npcs, and the legacy XML carries exactly three report/complete
		// blocks). Every other non-hunt family keeps exact resolution: a group name is not expanded
		// there, and an undeclared same-name multi-spawn is still refused as ambiguous.
		boolean noProgressGroup = entry.noProgress() && npcIndex.isQuestAiNameGroup(rewardName);
		var acquiredIds = npcAcquire
			? (huntFamily
				? npcIndex.resolvePartyName(acquireParam)
				: npcIndex.resolveAll(List.of(acquireParam)).npcIds())
			: java.util.List.<Integer>of();
		var rewardIds = huntFamily || noProgressGroup
			? npcIndex.resolvePartyName(rewardName)
			: npcIndex.resolveAll(List.of(rewardName)).npcIds();
		// 交付型行（走到 collect 分支的行）允许接取 NPC 变体家族（18742 形）：门只拦非交付行，
		// 交付分支内部按变体集合解析并自行校验非空。
		// Hand-in-bound rows allow an acquire variant family (the 18742 shape): the gate only blocks
		// non-hand-in rows; the hand-in branch resolves the variant set and validates non-emptiness.
		boolean collectDestined = !entry.noProgress() && !entry.allTalk() && !entry.allHunt()
			&& !entry.allPvp();
		// hunt 族放行多值当且仅当该名字在组表里有声明（组员全展开）；零命中仍在门户如实拒绝，
		// 与 hunt 合成器内部的稳定码口径一致。同名多刷点（未声明）仍按歧义拒绝。
		// The hunt family accepts multiple ids only when the name is a declared group (all members
		// expanded); a zero hit is still rejected here, matching the synthesizer's stable code. An
		// undeclared same-name multi-spawn stays ambiguous.
		boolean acquireGroup = huntFamily && npcIndex.isQuestAiNameGroup(acquireParam);
		boolean rewardGroup = huntFamily
			&& npcIndex.isQuestAiNameGroup(entry.rewardNpc() == null ? "" : entry.rewardNpc());
		// 哨兵行的拒绝详情同时给出原始参数与回退名（排障不必反查真端表）。
		// Sentinel rows report both the raw parameter and the fallback name in rejections.
		String acquireTrace = acquireParam.equals(entry.acquireParam() == null ? "" : entry.acquireParam())
			? acquireParam
			: entry.acquireParam() + " -> " + acquireParam;
		if (npcAcquire && acquiredIds.size() != 1 && !collectDestined
				&& !(acquireGroup && acquiredIds.size() > 1)) {
			return new Outcome(null, "RETAIL_ACQUIRE_NPC_UNRESOLVED",
				acquireTrace + " -> " + acquiredIds);
		}
		// 交付分支的奖励 NPC 允许同名多刷点（50052 形），由交付分支的变体解析自行把关。
		// The hand-in branch's reward npc allows same-name multi-spawn (the 50052 shape); the
		// hand-in branch's variant resolution validates it.
		// 无进度交付行的声明组同样放行多 owner（组员全展开，见 noProgressGroup 的判定注释）。
		// A no-progress hand-in row's declared group likewise admits multiple owners (every member
		// is expanded; see the noProgressGroup note above).
		if (rewardIds.size() != 1 && !collectDestined && !(rewardGroup && rewardIds.size() > 1)
				&& !(noProgressGroup && rewardIds.size() > 1)) {
			return new Outcome(null, "RETAIL_REWARD_NPC_UNRESOLVED",
				entry.rewardNpc() + " -> " + rewardIds);
		}
		int acquiredNpc = acquiredIds.isEmpty() ? -1 : acquiredIds.iterator().next();
		// 交付行允许精确零命中（下方变体解析接管并自行给稳定拒绝码）；非交付行上方门已保证非空。
		// Hand-in rows allow an exact miss (the variant resolution below takes over and emits the
		// stable rejection code); the gate above guarantees non-emptiness for other rows.
		int rewardNpc = rewardIds.isEmpty() ? -1 : rewardIds.iterator().next();
		try {
			QuestDefinition definition;
			if (entry.allHunt() || entry.allPvp()) {
				if (entry.allPvp()) {
					// P5-4：纯 PVP 行 = 单计数段；多段留待后续批。
					// P5-4: pure PVP row = single counter stage; multi-stage stays deferred.
					if (entry.pvpCounts().size() != 1) {
						return new Outcome(null, "RETAIL_PVP_MULTI_STAGE_DEFERRED",
							"stages=" + entry.pvpCounts().size());
					}
					// 军衔上限恒为 15（真端 1877–1887/2877–2887 全族如此）；其他上限未见于真端表，
					// 保持待裁定，避免把未知阈值语义当通配计数采纳。
					// The ceiling is always 15 across the retail urgent-order family; unseen ceilings
					// stay deferred rather than being adopted as wildcard counters.
					if (entry.pvpRankCeiling() != 0 && entry.pvpRankCeiling() != 15) {
						return new Outcome(null, "RETAIL_PVP_RANKED_WINDOW_DEFERRED",
							"unseen rank ceiling " + entry.pvpRankCeiling());
					}
				} else if (entry.huntStages().size() > 5) {
					// 多段 hunt 行的布局硬上限：5 个计数段占满 var0..var4（SECTION_5 是简报标志位），
					// 6 段会越过 32 位 quest_vars；真端 4 行 6 段样本留待布局扩展再裁定。
					// Hard layout cap for multi-stage hunt rows: 5 slots fill var0..var4 (SECTION_5 is
					// the briefing flag); 6 slots would overflow the 32-bit quest_vars.
					return new Outcome(null, "RETAIL_HUNT_MULTI_STAGE_DEFERRED",
						"stages=" + entry.huntStages().size() + " exceeds the 5-slot layout cap");
				}
				// 接取名用本函数已归一化的值（`_challengetask_` 哨兵已回退为真端 reward 名）——
				// hunt 合成器从 plan 重新读接取名，若仍传原始参数会把哨兵当未知类别哨兵拒绝。
				// The acquire name comes from the normalized local (a `_challengetask_` sentinel has
				// already fallen back to the retail reward name): the hunt synthesizer re-reads the
				// acquire name from the plan, so leaking the raw parameter would reject the sentinel.
				var huntEntry = toHuntEntry(entry, acquireParam);
				var basePlan = RetailSimpleHuntPlan.bind(huntEntry, npcIndex);
				var plan = basePlan
					.withPvpProgress(entry.allPvp())
					.withPvpMinRank(entry.pvpMinRank())
					.withPvpLevelGap(entry.pvpLevelGap())
					.withWorldAcquireId(worldAcquireId)
					// 变体轴裁定：单段行并入客户端任务书名单；多段行按计数槽并入逐段名单
					// （真端表只给基础模板名，客户端 SECTION 行含同族 T_ 实刷变体）。
					// Variant-axis adjudication: single-stage rows merge the client journal list;
					// multi-stage rows merge the per-stage lists by counter slot (the retail table
					// names only base templates, the client SECTION rows carry the T_ variants).
					.withClientKillTargets(resolveClientKillTargets(entry, clientKillTargets,
						huntProgressRows, npcIndex, basePlan))
					.withClientStageKillTargets(resolveClientStageKillTargets(entry, clientKillTargets,
						huntProgressRows, npcIndex, basePlan))
					// 计数轴裁定（单段）：客户端进度行与真端段同怪名时以客户端计数为准
					// （服务端表修订的读数与客户端门控在少数行不一致，常设门登记该分歧集）。
					// Count-axis adjudication (single stage): when the client progress row names the
					// same monsters, the client count wins (a server-table revision readout differs
					// from the client gate on a few rows; the permanent gate tracks that set).
					.withClientStageCounts(entry.allHunt()
						? huntProgressRows.rows(entry.questId()) : java.util.List.of());
				// 多段 hunt 行（2..5 段）走顺序 SECTION 链：客户端任务书只在当前段列出目标怪，
				// 乱序击杀不计数；单段行维持并行网格。PVP 行永远单段（上方已拒）。
				// Multi-stage hunt rows (2..5 stages) use the sequential SECTION chain — the client
				// journal lists only the current stage's targets and out-of-order kills never count;
				// single-stage rows keep the parallel grid. PVP rows are always single-stage.
				boolean sequentialStages = !entry.allPvp() && entry.huntStages().size() > 1;
				// P0-2 DD 切片：单段网格与多段顺序链的对话都走家族规范形（页 4 接取窗/满段
				// QUEST_SELECT 分档窗交付）；链式击杀边保持"首个未满段推进"语义。
				// P0-2 DD slice: single-stage grids and multi-stage sequential chains both follow the
				// family canonical dialog shape; chained kill edges keep the first-unfinished-slot
				// advance semantics.
				var huntOutcome = sequentialStages
					? RetailSimpleHuntDefinitionCompiler.compileSequentialStages(plan, metadata,
						clientRewardNpcs, questAreas, clientDialogExits)
					: RetailSimpleHuntDefinitionCompiler.compileCanonical(plan, metadata, clientRewardNpcs,
						questAreas, clientDialogExits);
				return new Outcome(huntOutcome.definition(), huntOutcome.rejectionCode(),
					huntOutcome.detail());
			}
			if (entry.noProgress()) {
				// P5-3 wave A：交付即完成（交付 NPC 的 QUEST_SELECT 直接进领奖态）；交付集可为声明组的
				// 多 owner（18737 形），逐 owner 发报告流、完成流按集合形态发一次。
				// 物品接取形（P0c-55，13952 形）只换接取段：使用道具开窗 + 无主接受/拒绝；报告/完成流共用。
				// Wave A: talking to the report npc completes the quest. The reward set may be a declared
				// group's members (the 18737 shape): one report flow per owner, one family completion flow.
				// The item-acquired shape (P0c-55, the 13952 shape) swaps only the accept segment: the
				// item use opens the window and the accept/refuse buttons are targetless; report and
				// completion flows are shared with the npc shape.
				definition = itemAcquireId != null
					? RetailDataDrivenTalkCompiler.buildItemAcquire(entry.questId(), itemAcquireId, rewardNpc,
						metadata.metadata(), clientSummaryRows.lastRowIndex(entry.questId()), interactionObjects)
					: RetailDataDrivenTalkCompiler.build(entry.questId(), acquiredNpc, rewardIds,
						metadata.metadata(), clientSummaryRows.lastRowIndex(entry.questId()), interactionObjects);
				return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
			}
			if (entry.allTalk()) {
				// P5-3 wave B：链步骤 NPC 逐个解析后走链式构建器（SETPRO 阶梯 + 末步 SET_SUCCEED）。
				// Wave B: resolve every chain npc, then build the SETPRO ladder.
				List<Integer> stepNpcs = new java.util.ArrayList<>(entry.talkSteps().size());
				for (String stepName : entry.talkSteps()) {
					var ids = npcIndex.resolveAll(List.of(stepName)).npcIds();
					if (ids.size() != 1) {
						return new Outcome(null, "RETAIL_TALK_CHAIN_NPC_UNRESOLVED",
							stepName + " -> " + ids);
					}
					stepNpcs.add(ids.iterator().next());
				}
				definition = RetailDataDrivenTalkCompiler.buildChain(entry.questId(), acquiredNpc, rewardNpc,
					metadata.metadata(), stepNpcs, clientSummaryRows.lastRowIndex(entry.questId()),
					acquireCategory, acquireParam, worldAcquireId, entry.stepCutscenes());
				return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
			}
			if (metadata.metadata().itemRequirements().isEmpty()) {
				return new Outcome(null, "RETAIL_COLLECT_ITEM_SHAPE", "真端交付物为空");
			}
			// 交付分支的接取/交付 NPC 走统一名字通道（精确 → 声明组 → 名前变体）：18742 形是多值
			// 变体族、登陆点基地守卫（15478 形）与活动商人（50088 形）是声明组，两者都在这里展开；
			// 系统发放行保持既有 -1 哨兵。
			// The hand-in branch resolves its acquire/reward npcs through the unified name channel
			// (exact, declared group, name variants): the 18742 shape is a variant family while the
			// landing-base guards (the 15478 shape) and the event vendors (the 50088 shape) are declared
			// groups; system-grant rows keep the existing -1 sentinel.
			java.util.Set<Integer> collectAcquires = npcAcquire
				? npcIndex.resolvePartyName(entry.acquireParam() == null ? "" : entry.acquireParam())
				: java.util.Set.of(-1);
			// 奖励侧同口径：同名多刷点（NPC_event_goldstar 5 点）每个实例都是合法交付 NPC。
			// Same caliber on the reward side: every instance of a same-name multi-spawn npc
			// (NPC_event_goldstar's five spawns) is a legal hand-in npc.
			java.util.Set<Integer> rewardNpcs = npcIndex.resolvePartyName(
				entry.rewardNpc() == null ? "" : entry.rewardNpc());
			if (rewardNpcs.isEmpty()) {
				return new Outcome(null, "RETAIL_REWARD_NPC_UNRESOLVED", entry.rewardNpc());
			}
			definition = RetailDataDrivenCollectCompiler.buildSimple(entry.questId(), collectAcquires, rewardNpcs,
				metadata.metadata(), clientSummaryRows, clientDialogExits, clientHandinPages, interactionObjects);
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 判定客户端契约声明的 select 页族是否覆盖 Talk 链的每个阶段（DataDriven 链行的接取走 select_none
	 * 询问窗，声明的首个 select 页族即第一个对话阶段；页族号可以跳号，覆盖只按声明序计数）。
	 * Whether the select families the client contract declares cover every talk-chain stage (DataDriven
	 * chain rows accept through the select_none ask window, so the first declared family is the first
	 * talk stage; family numbers may skip, coverage counts declaration order only).
	 * @param questId 任务 ID / quest id
	 * @param steps Talk 阶段数 / number of talk stages
	 * @return 声明族覆盖全部阶段时 true / true when the declared families cover every stage
	 */
	static boolean hasEveryTalkStageHead(int questId, int steps) {
		return steps > 0 && RetailQuestDialogPages.familyCount(questId)
			>= RetailDataDrivenTalkCompiler.ACQUIRE_PAGE_FAMILIES + steps;
	}

	/** talk/collectitem 交错行：步骤类别仅含 talk 与 collectitem 且两者皆有。 / A talk/collectitem interleave row. */
	private static boolean isTalkCollectMix(RetailDataDrivenTable.Entry entry) {
		boolean talk = false;
		boolean collect = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("talk")) {
				talk = true;
			} else if (category.equals("collectitem")) {
				collect = true;
			} else if (category.equals("enterarea") || category.equals("enterworld")
					|| category.equals("itemplay") || category.equals("talkfobj")) {
				// EnterArea/EnterWorld/ItemPlay 步占行不占对话段：随混合链推进（交织片）。
				// EnterArea / EnterWorld / ItemPlay steps occupy a row without a dialog stage: they
				// ride the mixed chain.
			} else {
				return false;
			}
		}
		return talk && collect;
	}

	/** talk/hunt/collectitem 三族交错行。 / A talk/hunt/collectitem three-kind interleave row. */
	private static boolean isTalkCollectHuntMix(RetailDataDrivenTable.Entry entry) {
		boolean talk = false;
		boolean hunt = false;
		boolean collect = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("talk")) {
				talk = true;
			} else if (category.equals("hunt")) {
				hunt = true;
			} else if (category.equals("collectitem")) {
				collect = true;
			} else if (category.equals("enterarea") || category.equals("enterworld")
					|| category.equals("itemplay") || category.equals("talkfobj")) {
				// EnterArea/EnterWorld/ItemPlay 步占行不占对话段：随混合链推进（交织片）。
				// EnterArea / EnterWorld / ItemPlay steps occupy a row without a dialog stage: they
				// ride the mixed chain.
			} else {
				return false;
			}
		}
		return talk && hunt && collect;
	}

	/** talk/hunt 交错行：步骤类别仅含 talk 与 hunt 且两者皆有。 / A talk/hunt interleave row. */
	private static boolean isTalkHuntMix(RetailDataDrivenTable.Entry entry) {
		boolean talk = false;
		boolean hunt = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("talk")) {
				talk = true;
			} else if (category.equals("hunt")) {
				hunt = true;
			} else if (category.equals("enterarea") || category.equals("enterworld")
					|| category.equals("itemplay") || category.equals("talkfobj")) {
				// EnterArea/EnterWorld/ItemPlay 步占行不占对话段：随混合链推进（交织片）。
				// EnterArea / EnterWorld / ItemPlay steps occupy a row without a dialog stage: they
				// ride the mixed chain.
			} else {
				return false;
			}
		}
		return talk && hunt;
	}

	/**
	 * 含 ItemPlay 步的链行：步骤类别仅含 talk/hunt/collectitem/enterarea/itemplay 且 itemplay
	 * 至少一个（ItemPlay 步占行不占对话段，随混合链推进；切片 1 = Talk 接取 + 单 ItemPlay 步）。
	 * Chain rows carrying an ItemPlay step: categories only from talk/hunt/collectitem/enterarea/
	 * itemplay with at least one itemplay (itemplay steps occupy a row without a dialog stage and
	 * ride the mixed chain; slice 1 = Talk acquire plus a single ItemPlay step).
	 */
	private static boolean isItemPlayChain(RetailDataDrivenTable.Entry entry) {
		boolean itemPlay = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("itemplay")) {
				itemPlay = true;
			} else if (!category.equals("talk") && !category.equals("collectitem")
					&& !category.equals("hunt") && !category.equals("enterarea")
					&& !category.equals("enterworld") && !category.equals("talkfobj")) {
				return false;
			}
		}
		return itemPlay;
	}

	/**
	 * hunt/collectitem 交错行（无 talk；15608 形：EA → 采集 → EA → 首领；80848 形：首领 → 交付）。
	 * 交付物整组检查与 hunt 段计数都由混合链合成器承担，不需要 talk 对话段。
	 * A hunt/collectitem interleave row without talk (the 15608 shape: EA → collect → EA → hunt;
	 * the 80848 shape: hunt → hand-in). The collect group check and the hunt counter both belong to
	 * the mixed-chain synthesizer; no talk dialog stage is involved.
	 */
	private static boolean isHuntCollectMix(RetailDataDrivenTable.Entry entry) {
		boolean hunt = false;
		boolean collect = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("hunt")) {
				hunt = true;
			} else if (category.equals("collectitem")) {
				collect = true;
			} else if (category.equals("enterarea") || category.equals("enterworld")
					|| category.equals("itemplay") || category.equals("talkfobj")) {
				// EnterArea/EnterWorld/ItemPlay/TalkFOBJ 步占行不占对话段：随混合链推进。
				// EnterArea / EnterWorld / ItemPlay / TalkFOBJ steps occupy a row without a dialog
				// stage and ride the mixed chain.
			} else {
				return false;
			}
		}
		return hunt && collect;
	}

	/**
	 * 物品接取参数 → 符号：真端表值形如 `SYMBOL [count]`，符号取首段（15025 的 `QUEST_15025A` 与
	 * 13952 的 `doc_quest_13952a` 共存 ⇒ 解析必须大小写无关，由 {@code RetailItemNameIndex} 承担）。
	 * The acquire parameter is "SYMBOL [count]"; the symbol is the first token (the index is
	 * case-insensitive, since both `QUEST_15025A` and `doc_quest_13952a` occur).
	 */
	private static String itemSymbol(String param) {
		if (param == null || param.isBlank()) {
			return "";
		}
		return param.trim().split("\\s+")[0];
	}

	/**
	 * 挑战任务哨兵的接取参数原值：真端 `value0_acquire_ = _challengetask_`，含义是"由挑战任务 NPC 发放"，
	 * 不是 NPC 名（P0c-58）。接取人回退到真端 reward 名（见 compile 内的哨兵注释）。
	 * <p>
	 * The challenge-task sentinel acquire parameter (P0c-58): the retail value means "granted by the
	 * challenge-task npc" rather than naming one; the acquire falls back to the retail reward name.
	 */
	private static final String CHALLENGE_TASK_SENTINEL = "_challengetask_";

	/**
	 * 纯 TalkFOBJ 单步行（25070 形：与 FOBJ 交互即入领奖，无 talk/hunt/collect 步）。
	 * A pure single-step TalkFOBJ row (the 25070 shape: the fobj interaction alone lands on reward,
	 * with no talk/hunt/collect step).
	 */
	private static boolean isTalkFobjOnly(RetailDataDrivenTable.Entry entry) {
		return entry.stepCategories().equals(List.of("talkfobj"));
	}

	/**
	 * TalkFOBJ/hunt 交错行（无 talk/collect；18931 形：FOBJ 物体交互推进 → 首领击杀 2 次）。
	 * TalkFOBJ 步占行不占对话段（FOBJ 模板 USE_OBJECT 交互边），hunt 段计数由混合链合成器承担。
	 * A TalkFOBJ/hunt interleave row without talk/collect (the 18931 shape: an fobj interaction step
	 * then a two-kill hunt block). The TalkFOBJ step occupies a journal row without a dialog stage
	 * (an fobj-template USE_OBJECT interaction edge); the hunt counter belongs to the mixed-chain
	 * synthesizer.
	 */
	private static boolean isTalkFobjHuntMix(RetailDataDrivenTable.Entry entry) {
		boolean talkFobj = false;
		boolean hunt = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("talkfobj")) {
				talkFobj = true;
			} else if (category.equals("hunt")) {
				hunt = true;
			} else if (category.equals("talk") || category.equals("collectitem")
					|| category.equals("enterarea") || category.equals("enterworld")
					|| category.equals("itemplay")) {
				// 骑行者放行：talk/collectitem 走对话段与检查对，EA/EW/itemplay 占行不占对话段。
				// Riders pass: talk/collectitem own dialog stages and check pairs while
				// EA/EW/itemplay occupy rows without stages.
			} else {
				return false;
			}
		}
		return talkFobj && hunt;
	}

	/**
	 * 链内 PVP 计数行（续片 22）：PVP 步与其他步骤类别交错（80846 形 pvp+collectitem、15673 形
	 * enterarea+pvp、9639 形 hunt+hunt+pvp+talk+hunt）；接取形不限，PVP 计数段由混合链合成器
	 * 按链内计数惯例分配 SECTION_1+。纯 PVP 行没有「其他」步骤，不上本判据（allPvp 网格分支接管）。
	 * An in-chain PVP counter row (slice 22): a PVP step interleaved with other step categories (the
	 * 80846 pvp+collectitem, 15673 enterarea+pvp and 9639 hunt+hunt+pvp+talk+hunt shapes); any
	 * acquire form, with the counter section allocated by the mixed-chain synthesizer's in-chain
	 * convention (SECTION_1+). A pure-PVP row owns no "other" step and never matches here (the
	 * allPvp grid branch takes it).
	 */
	private static boolean isPvpMix(RetailDataDrivenTable.Entry entry) {
		boolean pvp = false;
		boolean other = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("pvp")) {
				pvp = true;
			} else if (category.equals("talk") || category.equals("hunt")
					|| category.equals("collectitem") || category.equals("enterarea")
					|| category.equals("enterworld") || category.equals("itemplay")
					|| category.equals("talkfobj")) {
				other = true;
			} else {
				return false;
			}
		}
		return pvp && other;
	}

	/** hunt/enterarea/enterworld 交错行（无 talk/collect；13956 形：EA → 首领 → EA 链）。 /
	 * A hunt/enterarea/enterworld interleave row without talk/collect (the 13956 shape). */
	private static boolean isHuntEaMix(RetailDataDrivenTable.Entry entry) {
		boolean hunt = false;
		boolean ea = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("hunt")) {
				hunt = true;
			} else if (category.equals("enterarea") || category.equals("enterworld")) {
				// EnterArea/EnterWorld 步占行不占对话段：随混合链推进。
				// EnterArea / EnterWorld steps ride the mixed chain without a dialog stage.
				ea = true;
			} else {
				return false;
			}
		}
		return hunt && ea;
	}

	/** 纯 enterarea/enterworld 链（无计数无对话；16831 连续区域 / 15031 单世界推进形）。 /
	 * A pure enterarea/enterworld chain with no counters or dialogs (the 16831 zone and 15031
	 * world-advance shapes). */
	private static boolean isAreaWorldOnly(RetailDataDrivenTable.Entry entry) {
		return !entry.stepCategories().isEmpty()
			&& entry.stepCategories().stream().allMatch(
				category -> category.equals("enterarea") || category.equals("enterworld"));
	}

	/** talk 与区域/世界骑行者交错行（无 hunt/collect；13960 形：进世界 → 对话 → 进区域）。 /
	 * A talk plus area/world rider interleave row without hunt/collect (the 13960 shape). */
	private static boolean isTalkAreaWorldMix(RetailDataDrivenTable.Entry entry) {
		boolean talk = false;
		boolean rider = false;
		for (String category : entry.stepCategories()) {
			if (category.equals("talk")) {
				talk = true;
			} else if (category.equals("enterarea") || category.equals("enterworld")
					|| category.equals("talkfobj")) {
				rider = true;
			} else {
				return false;
			}
		}
		return talk && rider;
	}

	/**
	 * Hunt 段 → hunt 表行：每个分号段 = 一个计数槽（段内多怪共享同一 SECTION）。
	 * {@code acquireName} 是已归一化的接取名（哨兵行 = 真端 reward 名，见 compile 内的哨兵注释）。
	 * <p>
	 * Converts hunt stages to a hunt table entry: each semicolon stage = one counter slot. The acquire
	 * name is already normalized (sentinel rows carry the retail reward name).
	 */
	private static RetailSimpleHuntTable.Entry toHuntEntry(RetailDataDrivenTable.Entry entry, String acquireName) {
		List<RetailSimpleHuntTable.Counter> counters = new java.util.ArrayList<>();
		int slot = 1;
		// P5-4：纯 PVP 步骤 → PVP 计数槽（value0 = 所需击杀数；击杀边由网格发 KillInWorld(0)）。
		// P5-4: pure PVP steps become PVP counter slots; grid steps emit KillInWorld(0).
		for (int count : entry.pvpCounts()) {
			counters.add(new RetailSimpleHuntTable.Counter(slot++, count, List.of("")));
		}
		for (RetailDataDrivenTable.HuntStage stage : entry.huntStages()) {
			List<String> monsters = new java.util.ArrayList<>();
			for (String names : stage.monsters()) {
				for (String name : names.split(",")) {
					if (!name.isBlank()) {
						monsters.add(name.trim());
					}
				}
			}
			if (monsters.isEmpty()) {
				continue;
			}
			counters.add(new RetailSimpleHuntTable.Counter(slot, stage.count(), List.copyOf(monsters)));
			slot++;
		}
		if (counters.isEmpty()) {
			counters.add(new RetailSimpleHuntTable.Counter(1, 1, List.of("")));
		}
		// EnterArea/none 接取 = 世界 quest_area 进区域发放（P0c-4 机制）；未绑定的行由 requireAcquire 稳定码拒绝。
		// EnterArea/none acquires are granted on world quest-area entry (P0c-4); unbound rows are rejected by requireAcquire.
		RetailGrantKind grantKind = "enterarea".equalsIgnoreCase(entry.acquireCategory())
			|| ("none".equalsIgnoreCase(entry.acquireCategory()) && (acquireName == null || acquireName.isBlank()))
				? RetailGrantKind.AREA
				: ("enterworld".equalsIgnoreCase(entry.acquireCategory())
					? RetailGrantKind.WORLD
					: RetailGrantKind.NPC);
		return new RetailSimpleHuntTable.Entry(entry.questId(), List.copyOf(counters),
			acquireName, entry.rewardNpc() == null ? "" : entry.rewardNpc(), null, grantKind);
	}
	private static java.util.Set<Integer> resolveClientKillTargets(RetailDataDrivenTable.Entry entry,
			RetailClientKillTargets clientKillTargets, RetailClientHuntProgressRows huntProgressRows,
			RetailNpcNameIndex npcIndex, RetailSimpleHuntPlan plan) {
		if (!entry.allHunt()) {
			return java.util.Set.of();
		}
		java.util.Set<Integer> targets = clientKillTargets.targets(entry.questId());
		if (!targets.isEmpty()) {
			return targets;
		}
		// 仅在真端名单完全无法解析到任何怪物时（Q 后缀改名模板等），才退回客户端任务书名单（15306 形同口径）
		// Only when the retail list fails to resolve to any npc at all (e.g. Q-suffix renamed templates),
		// fall back to the client journal progress rows.
		if (!plan.counters().isEmpty() && plan.counters().get(0).npcIds().isEmpty()) {
			java.util.List<RetailClientHuntProgressRows.Row> pRows = huntProgressRows.rows(entry.questId());
			if (pRows != null && !pRows.isEmpty()) {
				java.util.List<String> clientNames = pRows.stream()
					.flatMap(r -> r.monsters().stream())
					.toList();
				return npcIndex.withDisplayNameVariants(npcIndex.resolveAll(clientNames).npcIds());
			}
		}
		return java.util.Set.of();
	}
	private static java.util.Map<Integer, java.util.Set<Integer>> resolveClientStageKillTargets(
			RetailDataDrivenTable.Entry entry, RetailClientKillTargets clientKillTargets,
			RetailClientHuntProgressRows huntProgressRows, RetailNpcNameIndex npcIndex,
			RetailSimpleHuntPlan plan) {
		if (!entry.allHunt()) {
			return java.util.Map.of();
		}
		java.util.Map<Integer, java.util.Set<Integer>> stages = clientKillTargets.stageTargets(entry.questId());
		if (!stages.isEmpty()) {
			return stages;
		}
		boolean anyUnresolved = plan.counters().stream().anyMatch(c -> c.npcIds().isEmpty());
		if (!anyUnresolved) {
			return java.util.Map.of();
		}
		java.util.List<RetailClientHuntProgressRows.Row> pRows = huntProgressRows.rows(entry.questId());
		if (pRows == null || pRows.isEmpty()) {
			return java.util.Map.of();
		}
		java.util.Map<Integer, java.util.Set<Integer>> resolved = new java.util.HashMap<>();
		for (RetailClientHuntProgressRows.Row row : pRows) {
			java.util.Set<Integer> ids = npcIndex.withDisplayNameVariants(
				npcIndex.resolveAll(row.monsters()).npcIds());
			if (!ids.isEmpty()) {
				resolved.computeIfAbsent(row.section(), k -> new java.util.LinkedHashSet<>()).addAll(ids);
			}
		}
		return java.util.Map.copyOf(resolved);
	}
}
