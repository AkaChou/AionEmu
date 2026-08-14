package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;
import java.util.Set;

/**
 * 任务引擎接受的封闭事件事实集合。
 * Closed set of event facts accepted by the quest engine.
 */
public sealed interface QuestEvent permits QuestEvent.TalkToNpc, QuestEvent.KillNpc,
		QuestEvent.KillNpcSet, QuestEvent.AttackNpc, QuestEvent.UseItem, QuestEvent.CollectItem,
		QuestEvent.ItemPlay, QuestEvent.HouseItemUse, QuestEvent.GetItem, QuestEvent.LevelUp,
		QuestEvent.ZoneMissionEnd, QuestEvent.EventQuestRefresh, QuestEvent.Die, QuestEvent.LogOut,
		QuestEvent.EnterWorld,
		QuestEvent.EnterZone, QuestEvent.LeaveZone, QuestEvent.PassFlyingRing, QuestEvent.MovieEnd,
		QuestEvent.QuestTimerEnd, QuestEvent.InvisibleTimerEnd, QuestEvent.KillRanked,
		QuestEvent.KillInWorld, QuestEvent.UseSkill, QuestEvent.FailCraft, QuestEvent.EquipItem,
		QuestEvent.CanAct, QuestEvent.DredgionReward, QuestEvent.KamarReward, QuestEvent.OphidanReward,
		QuestEvent.BastionReward, QuestEvent.BonusApply, QuestEvent.AddAggroList,
		QuestEvent.AtDistance, QuestEvent.ProtectEnd, QuestEvent.ProtectFail,
		QuestEvent.EnterWindStream, QuestEvent.RideAction, QuestEvent.CreativityPoint,
		QuestEvent.NpcReachTarget, QuestEvent.NpcLostTarget, QuestEvent.QuestDialog,
		QuestEvent.Abandon {
	String type();

	/**
	 * 与 NPC 对话事件（可指定对话 id 与交互物件）。
	 * Talk-to-NPC event (optionally keyed by dialog id and interaction object).
	 */
	record TalkToNpc(int npcId, Integer dialogId, int interactionObjectId) implements QuestEvent {
		public TalkToNpc(int npcId) {
			this(npcId, null, 0);
		}

		public TalkToNpc(int npcId, Integer dialogId) {
			this(npcId, dialogId, 0);
		}

		public TalkToNpc {
			checkId(npcId, "npcId");
			if (interactionObjectId < 0) {
				throw new IllegalArgumentException("interactionObjectId must be non-negative");
			}
		}

		@Override
		public String type() {
			return "TALK_TO_NPC";
		}
	}

	/**
	 * 击杀指定 NPC 事件。
	 * Kill-NPC event.
	 */
	record KillNpc(int npcId) implements QuestEvent {
		public KillNpc {
			checkId(npcId, "npcId");
		}

		@Override
		public String type() {
			return "KILL_NPC";
		}
	}

	/**
	 * 击杀任意列出的 NPC 即满足事件（一条转换覆盖整个怪物族群）。
	 * Kills of any listed npc satisfy the event (one transition covering a mob family).
	 */
	record KillNpcSet(Set<Integer> npcIds) implements QuestEvent {
		public KillNpcSet {
			if (npcIds == null || npcIds.isEmpty()) {
				throw new IllegalArgumentException("npcIds must not be empty");
			}
			if (npcIds.stream().anyMatch(id -> id <= 0)) {
				throw new IllegalArgumentException("npcIds must be positive");
			}
			npcIds = Set.copyOf(npcIds);
		}

		@Override
		public String type() {
			return "KILL_NPC";
		}
	}

	/**
	 * 攻击指定 NPC 事件（携带权威战斗事实）。
	 * Attack-NPC event (carries authoritative combat facts).
	 */
	record AttackNpc(int npcId, QuestNpcAttackFacts facts) implements QuestEvent {
		public AttackNpc(int npcId) {
			this(npcId, null);
		}

		public AttackNpc {
			checkId(npcId, "npcId");
			if (facts != null && facts.npcTemplateId() != npcId) {
				throw new IllegalArgumentException("NPC attack facts do not match the route");
			}
		}

		@Override
		public String type() {
			return "ATTACK_NPC";
		}
	}

	/**
	 * 使用物品事件。
	 * Use-item event.
	 */
	record UseItem(int itemId, int itemObjectId) implements QuestEvent {
		public UseItem(int itemId) {
			this(itemId, 0);
		}

		public UseItem {
			checkId(itemId, "itemId");
			if (itemObjectId < 0) {
				throw new IllegalArgumentException("itemObjectId must be non-negative");
			}
		}

		@Override
		public String type() {
			return "USE_ITEM";
		}
	}

	/**
	 * 收集物品事件。
	 * Collect-item event.
	 */
	record CollectItem(int itemId, int count) implements QuestEvent {
		public CollectItem {
			checkId(itemId, "itemId");
			if (count <= 0) {
				throw new IllegalArgumentException("count must be positive");
			}
		}

		@Override
		public String type() {
			return "COLLECT_ITEM";
		}
	}

	/**
	 * 物品动画播放事件。
	 * Item animation-play event.
	 */
	record ItemPlay(int itemId, int animationMillis) implements QuestEvent {
		public ItemPlay {
			checkId(itemId, "itemId");
			if (animationMillis < 0) {
				throw new IllegalArgumentException("animationMillis must be non-negative");
			}
		}

		@Override
		public String type() {
			return "ITEM_PLAY";
		}
	}

	/**
	 * 房屋物件使用事件。
	 * House-object use event.
	 */
	record HouseItemUse(int itemId, QuestHousingFacts facts) implements QuestEvent {
		public HouseItemUse(int itemId) { this(itemId, null); }
		public HouseItemUse {
			checkId(itemId, "itemId");
			if (facts != null && facts.itemTemplateId() != itemId) throw new IllegalArgumentException("housing facts do not match item");
		}

		@Override
		public String type() {
			return "HOUSE_ITEM_USE";
		}
	}

	/**
	 * 获得物品事件。
	 * Get-item event.
	 */
	record GetItem(int itemId) implements QuestEvent {
		public GetItem {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "GET_ITEM";
		}
	}

	/**
	 * 玩家升级事件。
	 * Player level-up event.
	 */
	record LevelUp() implements QuestEvent {
		@Override
		public String type() {
			return "LEVEL_UP";
		}
	}

	/**
	 * 区域任务结束事件。
	 * Zone-mission-end event.
	 */
	record ZoneMissionEnd() implements QuestEvent {
		@Override
		public String type() {
			return "ZONE_MISSION_END";
		}
	}

	/**
	 * 延迟的、类型化的跨所有者事件任务刷新使用的内部事件。
	 * Internal event used by delayed, typed cross-owner event-quest refreshes.
	 */
	record EventQuestRefresh() implements QuestEvent {
		@Override
		public String type() {
			return "EVENT_QUEST_REFRESH";
		}
	}

	/**
	 * 玩家死亡事件。
	 * Player death event.
	 */
	record Die() implements QuestEvent {
		@Override
		public String type() {
			return "DIE";
		}
	}

	/**
	 * 玩家登出事件（携带恢复事实）。
	 * Player logout event (carries recovery facts).
	 */
	record LogOut(QuestRecoveryFacts facts) implements QuestEvent {
		public LogOut() { this(null); }
		@Override
		public String type() {
			return "LOG_OUT";
		}
	}

	/**
	 * 玩家请求放弃此任务的权威事件。
	 * Authoritative player request to abandon this quest.
	 */
	record Abandon() implements QuestEvent {
		@Override
		public String type() {
			return "ABANDON";
		}
	}

	/**
	 * 玩家进入世界事件。
	 * Enter-world event.
	 */
	record EnterWorld() implements QuestEvent {
		@Override
		public String type() {
			return "ENTER_WORLD";
		}
	}

	/**
	 * 玩家进入区域事件。
	 * Enter-zone event.
	 */
	record EnterZone(String zone) implements QuestEvent {
		public EnterZone {
			zone = checkText(zone, "zone");
		}

		@Override
		public String type() {
			return "ENTER_ZONE";
		}
	}

	/**
	 * 玩家离开区域事件。
	 * Leave-zone event.
	 */
	record LeaveZone(String zone) implements QuestEvent {
		public LeaveZone {
			zone = checkText(zone, "zone");
		}

		@Override
		public String type() {
			return "LEAVE_ZONE";
		}
	}

	/**
	 * 穿过飞行环事件（携带移动事实）。
	 * Pass-flying-ring event (carries movement facts).
	 */
	record PassFlyingRing(String ring, QuestMovementFacts facts) implements QuestEvent {
		public PassFlyingRing(String ring) { this(ring, null); }
		public PassFlyingRing {
			ring = checkText(ring, "ring");
			if (facts != null && !facts.actionId().equals(ring)) throw new IllegalArgumentException("movement facts do not match ring");
		}

		@Override
		public String type() {
			return "PASS_FLYING_RING";
		}
	}

	/**
	 * 影片结束事件。
	 * Movie-end event.
	 */
	record MovieEnd(int movieId) implements QuestEvent {
		public MovieEnd {
			checkId(movieId, "movieId");
		}

		@Override
		public String type() {
			return "MOVIE_END";
		}
	}

	/**
	 * 任务可见定时器结束事件。
	 * Quest visible-timer-end event.
	 */
	record QuestTimerEnd() implements QuestEvent {
		@Override
		public String type() {
			return "QUEST_TIMER_END";
		}
	}

	/**
	 * 任务隐形定时器结束事件。
	 * Quest invisible-timer-end event.
	 */
	record InvisibleTimerEnd() implements QuestEvent {
		@Override
		public String type() {
			return "INVISIBLE_TIMER_END";
		}
	}

	/**
	 * 定义中的 rankId 是最低阈值；运行时事实仅存于服务端。
	 * Definition rankId is a minimum threshold; runtime facts are server-only.
	 */
	record KillRanked(int rankId, QuestPvpKillFacts facts) implements QuestEvent {
		public KillRanked(int rankId) {
			this(rankId, null);
		}

		public KillRanked {
			checkId(rankId, "rankId");
			if (rankId > 18) {
				throw new IllegalArgumentException("rankId must be at most 18");
			}
			if (facts != null && facts.victimRankId() != rankId) {
				throw new IllegalArgumentException("runtime rank does not match PvP facts");
			}
		}

		@Override
		public String type() {
			return "KILL_RANKED";
		}
	}

	/**
	 * 指定世界中的击杀事件（官方通配 world-id 0）。
	 * Kill-in-world event (retail wildcard world-id 0).
	 */
	record KillInWorld(int worldId, QuestPvpKillFacts facts) implements QuestEvent {
		public KillInWorld(int worldId) {
			this(worldId, null);
		}

		public KillInWorld {
			// world-id 0 是官方数据驱动 PVP 任务使用的类型化定义通配符。
			// 运行时事实仍携带具体正数世界 id，并由 QuestPvpKillFacts 校验。
			// world-id 0 is the typed-definition wildcard used by retail
			// data-driven PVP quests. Runtime facts still carry a concrete
			// positive world id and are validated by QuestPvpKillFacts.
			if (worldId < 0) {
				throw new IllegalArgumentException("worldId must not be negative");
			}
			if (facts != null && facts.worldId() != worldId) {
				throw new IllegalArgumentException("runtime world does not match PvP facts");
			}
		}

		@Override
		public String type() {
			return "KILL_IN_WORLD";
		}
	}

	/**
	 * 使用技能事件（携带技能事实）。
	 * Use-skill event (carries skill facts).
	 */
	record UseSkill(int skillId, QuestSkillFacts facts) implements QuestEvent {
		public UseSkill(int skillId) { this(skillId, null); }
		public UseSkill {
			checkId(skillId, "skillId");
			if (facts != null && facts.skillId() != skillId) throw new IllegalArgumentException("skill facts do not match skill");
		}

		@Override
		public String type() {
			return "USE_SKILL";
		}
	}

	/**
	 * 制作失败事件。
	 * Craft-fail event.
	 */
	record FailCraft(int itemId) implements QuestEvent {
		public FailCraft {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "FAIL_CRAFT";
		}
	}

	/**
	 * 装备物品事件。
	 * Equip-item event.
	 */
	record EquipItem(int itemId) implements QuestEvent {
		public EquipItem {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "EQUIP_ITEM";
		}
	}

	/**
	 * 可交互动作事件。
	 * Can-act event.
	 */
	record CanAct(int templateId, String actionType) implements QuestEvent {
		public CanAct {
			checkId(templateId, "templateId");
			actionType = checkText(actionType, "actionType");
		}

		@Override
		public String type() {
			return "CAN_ACT";
		}
	}

	/**
	 * 德雷得奇安副本结算事件。
	 * Dredgion instance settlement event.
	 */
	record DredgionReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public DredgionReward() { this(null); }
		@Override
		public String type() {
			return "DREDGION_REWARD";
		}
	}

	/**
	 * 卡马尔副本结算事件。
	 * Kamar instance settlement event.
	 */
	record KamarReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public KamarReward() { this(null); }
		@Override
		public String type() {
			return "KAMAR_REWARD";
		}
	}

	/**
	 * 奥菲达副本结算事件。
	 * Ophidan instance settlement event.
	 */
	record OphidanReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public OphidanReward() { this(null); }
		@Override
		public String type() {
			return "OPHIDAN_REWARD";
		}
	}

	/**
	 * 堡垒副本结算事件。
	 * Bastion instance settlement event.
	 */
	record BastionReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public BastionReward() { this(null); }
		@Override
		public String type() {
			return "BASTION_REWARD";
		}
	}

	/**
	 * 奖励附加事件。
	 * Bonus-apply event.
	 */
	record BonusApply(String bonusType) implements QuestEvent {
		public BonusApply {
			bonusType = checkText(bonusType, "bonusType");
		}

		@Override
		public String type() {
			return "BONUS_APPLY";
		}
	}

	/**
	 * NPC 仇恨列表新增玩家事件（携带 AI 感知事实）。
	 * NPC aggro-list addition event (carries AI perception facts).
	 */
	record AddAggroList(int npcId, QuestAiPerceptionFacts facts) implements QuestEvent {
		public AddAggroList(int npcId) { this(npcId, null); }
		public AddAggroList {
			checkId(npcId, "npcId");
			if (facts != null && facts.npcTemplateId() != npcId) throw new IllegalArgumentException("AI facts do not match NPC route");
		}

		@Override
		public String type() {
			return "ADD_AGGRO_LIST";
		}
	}

	/**
	 * 玩家与 NPC 距离进入范围事件（携带邻近事实）。
	 * At-distance-to-NPC event (carries proximity facts).
	 */
	record AtDistance(int npcId, QuestProximityFacts facts) implements QuestEvent {
		public AtDistance(int npcId) {
			this(npcId, null);
		}

		public AtDistance {
			checkId(npcId, "npcId");
			if (facts != null && facts.targetNpcId() != npcId) {
				throw new IllegalArgumentException("runtime proximity facts do not match the NPC route");
			}
		}

		@Override
		public String type() {
			return "AT_DISTANCE";
		}
	}

	/**
	 * 护送保护结束事件。
	 * Protect-end event.
	 */
	record ProtectEnd() implements QuestEvent {
		@Override
		public String type() {
			return "PROTECT_END";
		}
	}

	/**
	 * 护送保护失败事件。
	 * Protect-fail event.
	 */
	record ProtectFail() implements QuestEvent {
		@Override
		public String type() {
			return "PROTECT_FAIL";
		}
	}

	/**
	 * 进入气流事件（携带移动事实）。
	 * Wind-stream entry event (carries movement facts).
	 */
	record EnterWindStream(int teleportId, QuestMovementFacts facts) implements QuestEvent {
		public EnterWindStream(int teleportId) { this(teleportId, null); }
		public EnterWindStream {
			checkId(teleportId, "teleportId");
			if (facts != null && !facts.actionId().equals(Integer.toString(teleportId))) throw new IllegalArgumentException("movement facts do not match wind-stream route");
		}

		@Override
		public String type() {
			return "ENTER_WIND_STREAM";
		}
	}

	/**
	 * 骑乘动作事件。
	 * Ride-action event.
	 */
	record RideAction(int itemId) implements QuestEvent {
		public RideAction {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "RIDE_ACTION";
		}
	}

	/**
	 * 创造力点数事件。
	 * Creativity-point event.
	 */
	record CreativityPoint() implements QuestEvent {
		@Override
		public String type() {
			return "CREATIVITY_POINT";
		}
	}

	/**
	 * NPC 到达目标事件。
	 * NPC reached target event.
	 */
	record NpcReachTarget() implements QuestEvent {
		@Override
		public String type() {
			return "NPC_REACH_TARGET";
		}
	}

	/**
	 * NPC 失去目标事件。
	 * NPC lost target event.
	 */
	record NpcLostTarget() implements QuestEvent {
		@Override
		public String type() {
			return "NPC_LOST_TARGET";
		}
	}

	/**
	 * 无目标任务对话选择，用于物品触发任务及其他协议 object id 为零而非 NPC 的对话。
	 * Targetless quest-dialog selection, used by item-start quests and other
	 * dialogs whose protocol object id is zero rather than an NPC.
	 */
	record QuestDialog(int dialogId) implements QuestEvent {
		public QuestDialog {
			if (dialogId < 0) {
				throw new IllegalArgumentException("dialogId must be non-negative");
			}
		}

		@Override
		public String type() {
			return "QUEST_DIALOG";
		}
	}

	/**
	 * 返回路由索引使用的事件键；对话在路由之后匹配。
	 * Returns the event key used by the route index. Dialog is matched after routing.
	 */
	static QuestEvent routeKey(QuestEvent event) {
		Objects.requireNonNull(event, "event");
		if (event instanceof TalkToNpc talk) {
			return new TalkToNpc(talk.npcId());
		}
		if (event instanceof AttackNpc attack) {
			return new AttackNpc(attack.npcId());
		}
		if (event instanceof UseItem useItem) {
			return new UseItem(useItem.itemId());
		}
		if (event instanceof CollectItem collectItem) {
			// 收集路由以物品为键；执行路由时按权威背包快照校验数量。
			// Collection routes are keyed by item; the count is checked against the
			// authoritative inventory snapshot when the route is executed.
			return new CollectItem(collectItem.itemId(), 1);
		}
		if (event instanceof ItemPlay itemPlay) {
			return new ItemPlay(itemPlay.itemId(), 0);
		}
		if (event instanceof KillRanked) {
			return new KillRanked(1);
		}
		if (event instanceof KillInWorld kill) {
			return new KillInWorld(kill.worldId());
		}
		if (event instanceof AtDistance atDistance) {
			return new AtDistance(atDistance.npcId());
		}
		if (event instanceof HouseItemUse houseItemUse) {
			return new HouseItemUse(houseItemUse.itemId());
		}
		if (event instanceof AddAggroList addAggroList) {
			return new AddAggroList(addAggroList.npcId());
		}
		if (event instanceof PassFlyingRing passFlyingRing) {
			return new PassFlyingRing(passFlyingRing.ring());
		}
		if (event instanceof EnterWindStream enterWindStream) {
			return new EnterWindStream(enterWindStream.teleportId());
		}
		if (event instanceof UseSkill useSkill) {
			return new UseSkill(useSkill.skillId());
		}
		if (event instanceof DredgionReward) {
			return new DredgionReward();
		}
		if (event instanceof KamarReward) {
			return new KamarReward();
		}
		if (event instanceof OphidanReward) {
			return new OphidanReward();
		}
		if (event instanceof BastionReward) {
			return new BastionReward();
		}
		if (event instanceof LogOut) {
			return new LogOut();
		}
		return event;
	}

	/**
	 * 将定义事件与一个权威运行时事件进行匹配。
	 * Matches a definition event against one authoritative runtime event.
	 */
	static boolean matches(QuestEvent definition, QuestEvent actual) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(actual, "actual");
		if (definition instanceof TalkToNpc expected && actual instanceof TalkToNpc observed) {
			return expected.npcId() == observed.npcId()
				&& (expected.dialogId() == null || expected.dialogId().equals(observed.dialogId()));
		}
		if (definition instanceof UseItem expected && actual instanceof UseItem observed) {
			return expected.itemId() == observed.itemId();
		}
		if (definition instanceof CollectItem expected && actual instanceof CollectItem observed) {
			return expected.itemId() == observed.itemId() && observed.count() >= expected.count();
		}
		if (definition instanceof AttackNpc expected && actual instanceof AttackNpc observed) {
			return expected.npcId() == observed.npcId();
		}
		if (definition instanceof ItemPlay expected && actual instanceof ItemPlay observed) {
			return expected.itemId() == observed.itemId()
				&& expected.animationMillis() == observed.animationMillis();
		}
		if (definition instanceof QuestDialog expected && actual instanceof QuestDialog observed) {
			return expected.dialogId() == observed.dialogId();
		}
		if (definition instanceof KillRanked expected && actual instanceof KillRanked observed) {
			return observed.rankId() >= expected.rankId();
		}
		if (definition instanceof KillInWorld expected && actual instanceof KillInWorld observed) {
			return expected.worldId() == 0 || expected.worldId() == observed.worldId();
		}
		if (definition instanceof KillNpcSet expected && actual instanceof KillNpc observed) {
			return expected.npcIds().contains(observed.npcId());
		}
		if (definition instanceof AtDistance expected && actual instanceof AtDistance observed) {
			return expected.npcId() == observed.npcId();
		}
		if (definition instanceof HouseItemUse expected && actual instanceof HouseItemUse observed) {
			return expected.itemId() == observed.itemId();
		}
		if (definition instanceof AddAggroList expected && actual instanceof AddAggroList observed) {
			return expected.npcId() == observed.npcId();
		}
		if (definition instanceof PassFlyingRing expected && actual instanceof PassFlyingRing observed) {
			return expected.ring().equals(observed.ring());
		}
		if (definition instanceof EnterWindStream expected && actual instanceof EnterWindStream observed) {
			return expected.teleportId() == observed.teleportId();
		}
		if (definition instanceof UseSkill expected && actual instanceof UseSkill observed) {
			return expected.skillId() == observed.skillId();
		}
		if (definition instanceof DredgionReward && actual instanceof DredgionReward
				|| definition instanceof KamarReward && actual instanceof KamarReward
				|| definition instanceof OphidanReward && actual instanceof OphidanReward
				|| definition instanceof BastionReward && actual instanceof BastionReward
				|| definition instanceof LogOut && actual instanceof LogOut) {
			return true;
		}
		return definition.equals(actual);
	}

	/**
	 * 两个定义事件能否消费同一运行时事件。
	 * True when two definition events can consume the same runtime event.
	 */
	static boolean overlaps(QuestEvent left, QuestEvent right) {
		Objects.requireNonNull(left, "left");
		Objects.requireNonNull(right, "right");
		if (left instanceof TalkToNpc a && right instanceof TalkToNpc b) {
			return a.npcId() == b.npcId()
				&& (a.dialogId() == null || b.dialogId() == null || a.dialogId().equals(b.dialogId()));
		}
		if (left instanceof UseItem a && right instanceof UseItem b) {
			return a.itemId() == b.itemId();
		}
		if (left instanceof AttackNpc a && right instanceof AttackNpc b) {
			return a.npcId() == b.npcId();
		}
		if (left instanceof ItemPlay a && right instanceof ItemPlay b) {
			return a.itemId() == b.itemId();
		}
		if (left instanceof QuestDialog a && right instanceof QuestDialog b) {
			return a.dialogId() == b.dialogId();
		}
		if (left instanceof KillRanked && right instanceof KillRanked) {
			return true;
		}
		if (left instanceof AtDistance a && right instanceof AtDistance b) {
			return a.npcId() == b.npcId();
		}
		if (left instanceof KillInWorld a && right instanceof KillInWorld b) {
			return a.worldId() == 0 || b.worldId() == 0 || a.worldId() == b.worldId();
		}
		if (left instanceof KillNpcSet a && right instanceof KillNpcSet b) {
			return !java.util.Collections.disjoint(a.npcIds(), b.npcIds());
		}
		if (left instanceof KillNpcSet a && right instanceof KillNpc single) {
			return a.npcIds().contains(single.npcId());
		}
		if (left instanceof KillNpc single && right instanceof KillNpcSet a) {
			return a.npcIds().contains(single.npcId());
		}
		return left.equals(right);
	}

	/**
	 * 返回运行时路由索引能否在无转换优先级的情况下为两个重叠事件排序。
	 * 同所有者的精确世界击杀先于官方通配世界路由。
	 * Returns whether the runtime route index can order two overlapping events
	 * without a transition priority. Exact world kills precede the retail
	 * wildcard world route for the same owner.
	 */
	static boolean hasDeterministicPrecedence(QuestEvent left, QuestEvent right) {
		return left instanceof KillInWorld a && right instanceof KillInWorld b
			&& (a.worldId() == 0) != (b.worldId() == 0);
	}

	private static void checkId(int value, String field) {
		if (value <= 0) {
			throw new IllegalArgumentException(field + " must be positive");
		}
	}

	private static String checkText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}
