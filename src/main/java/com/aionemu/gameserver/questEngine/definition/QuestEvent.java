package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;
import java.util.Set;

/** Closed set of event facts accepted by the quest engine. */
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

	record KillNpc(int npcId) implements QuestEvent {
		public KillNpc {
			checkId(npcId, "npcId");
		}

		@Override
		public String type() {
			return "KILL_NPC";
		}
	}

	/** Kills of any listed npc satisfy the event (one transition covering a mob family). */
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

	record GetItem(int itemId) implements QuestEvent {
		public GetItem {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "GET_ITEM";
		}
	}

	record LevelUp() implements QuestEvent {
		@Override
		public String type() {
			return "LEVEL_UP";
		}
	}

	record ZoneMissionEnd() implements QuestEvent {
		@Override
		public String type() {
			return "ZONE_MISSION_END";
		}
	}

	/** Internal event used by delayed, typed cross-owner event-quest refreshes. */
	record EventQuestRefresh() implements QuestEvent {
		@Override
		public String type() {
			return "EVENT_QUEST_REFRESH";
		}
	}

	record Die() implements QuestEvent {
		@Override
		public String type() {
			return "DIE";
		}
	}

	record LogOut(QuestRecoveryFacts facts) implements QuestEvent {
		public LogOut() { this(null); }
		@Override
		public String type() {
			return "LOG_OUT";
		}
	}

	/** Authoritative player request to abandon this quest. */
	record Abandon() implements QuestEvent {
		@Override
		public String type() {
			return "ABANDON";
		}
	}

	record EnterWorld() implements QuestEvent {
		@Override
		public String type() {
			return "ENTER_WORLD";
		}
	}

	record EnterZone(String zone) implements QuestEvent {
		public EnterZone {
			zone = checkText(zone, "zone");
		}

		@Override
		public String type() {
			return "ENTER_ZONE";
		}
	}

	record LeaveZone(String zone) implements QuestEvent {
		public LeaveZone {
			zone = checkText(zone, "zone");
		}

		@Override
		public String type() {
			return "LEAVE_ZONE";
		}
	}

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

	record MovieEnd(int movieId) implements QuestEvent {
		public MovieEnd {
			checkId(movieId, "movieId");
		}

		@Override
		public String type() {
			return "MOVIE_END";
		}
	}

	record QuestTimerEnd() implements QuestEvent {
		@Override
		public String type() {
			return "QUEST_TIMER_END";
		}
	}

	record InvisibleTimerEnd() implements QuestEvent {
		@Override
		public String type() {
			return "INVISIBLE_TIMER_END";
		}
	}

	/** Definition rankId is a minimum threshold; runtime facts are server-only. */
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

	record KillInWorld(int worldId, QuestPvpKillFacts facts) implements QuestEvent {
		public KillInWorld(int worldId) {
			this(worldId, null);
		}

		public KillInWorld {
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

	record FailCraft(int itemId) implements QuestEvent {
		public FailCraft {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "FAIL_CRAFT";
		}
	}

	record EquipItem(int itemId) implements QuestEvent {
		public EquipItem {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "EQUIP_ITEM";
		}
	}

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

	record DredgionReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public DredgionReward() { this(null); }
		@Override
		public String type() {
			return "DREDGION_REWARD";
		}
	}

	record KamarReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public KamarReward() { this(null); }
		@Override
		public String type() {
			return "KAMAR_REWARD";
		}
	}

	record OphidanReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public OphidanReward() { this(null); }
		@Override
		public String type() {
			return "OPHIDAN_REWARD";
		}
	}

	record BastionReward(QuestPvpInstanceFacts facts) implements QuestEvent {
		public BastionReward() { this(null); }
		@Override
		public String type() {
			return "BASTION_REWARD";
		}
	}

	record BonusApply(String bonusType) implements QuestEvent {
		public BonusApply {
			bonusType = checkText(bonusType, "bonusType");
		}

		@Override
		public String type() {
			return "BONUS_APPLY";
		}
	}

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

	record ProtectEnd() implements QuestEvent {
		@Override
		public String type() {
			return "PROTECT_END";
		}
	}

	record ProtectFail() implements QuestEvent {
		@Override
		public String type() {
			return "PROTECT_FAIL";
		}
	}

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

	record RideAction(int itemId) implements QuestEvent {
		public RideAction {
			checkId(itemId, "itemId");
		}

		@Override
		public String type() {
			return "RIDE_ACTION";
		}
	}

	record CreativityPoint() implements QuestEvent {
		@Override
		public String type() {
			return "CREATIVITY_POINT";
		}
	}

	record NpcReachTarget() implements QuestEvent {
		@Override
		public String type() {
			return "NPC_REACH_TARGET";
		}
	}

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

	/** Returns the event key used by the route index. Dialog is matched after routing. */
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

	/** Matches a definition event against one authoritative runtime event. */
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

	/** True when two definition events can consume the same runtime event. */
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
