package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 针对任务快照求值的纯条件封闭集合。
 * Closed set of pure conditions evaluated against a quest snapshot.
 */
public sealed interface QuestCondition permits QuestCondition.StatusIs, QuestCondition.HasItem,
		QuestCondition.QuestVariableIs, QuestCondition.VariableAtLeast, QuestCondition.VariableBelow,
		QuestCondition.VariableSumIs, QuestCondition.VariableSumBelow,
		QuestCondition.RecipeKnown, QuestCondition.CanGrantCraftSkill, QuestCondition.PvpVictimLevelDelta,
		QuestCondition.PvpRecipientInZone, QuestCondition.StartEligible, QuestCondition.PlayerClassIs,
		QuestCondition.AdvancedClassIs, QuestCondition.GenderIs, QuestCondition.PlayerRaceIs,
		QuestCondition.PlayerInGroup,
		QuestCondition.WorldIs, QuestCondition.WorldNpcIs, QuestCondition.ZoneIs,
		QuestCondition.NpcHpBelowPercent, QuestCondition.CurrencyAtLeast, QuestCondition.CurrencyBelow,
		QuestCondition.QuestsFinished, QuestCondition.UnfinishedQuest, QuestCondition.NoAcquiredQuest,
		QuestCondition.AcquiredQuest, QuestCondition.EquipmentSetEquipped, QuestCondition.EquippedItem,
		QuestCondition.MembershipPermission, QuestCondition.DpAtMax, QuestCondition.CompleteCountIs,
		QuestCondition.EventActive {

	/**
	 * 判断两组条件是否互斥：任意一对条件不可能同时成立即互斥。
	 * Determines whether two condition lists are mutually exclusive: any pair that can never
	 * both hold makes the lists exclusive.
	 */
	static boolean listsAreMutuallyExclusive(List<QuestCondition> left, List<QuestCondition> right) {
		for (QuestCondition a : left) {
			for (QuestCondition b : right) {
				if (areMutuallyExclusive(a, b)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 判断两个条件是否不可能同时成立；无法判定时返回 false（保守）。
	 * Determines whether two conditions can never both hold; returns false (conservatively) when undecidable.
	 */
	static boolean areMutuallyExclusive(QuestCondition left, QuestCondition right) {
		if (left instanceof StatusIs(QuestStatus status1) && right instanceof StatusIs(QuestStatus status)) {
			return status1 != status;
		}
		if (factConditionsAreMutuallyExclusive(left, right)) {
			return true;
		}
		return variableConditionsAreMutuallyExclusive(left, right);
	}

	/**
	 * 判断依赖玩家实时事实的条件对是否互斥。
	 * Determines exclusivity for condition pairs that depend on live player facts.
	 */
	private static boolean factConditionsAreMutuallyExclusive(QuestCondition left, QuestCondition right) {
		if (left instanceof PlayerInGroup(boolean expected19) && right instanceof PlayerInGroup(boolean expected18)) {
			return expected19 != expected18;
		}
		if (left instanceof HasItem(int itemId3, int count5, boolean expected17) && right instanceof HasItem(int itemId2, int count4, boolean expected16)) {
			return itemId3 == itemId2 && count5 == count4 && expected17 != expected16;
		}
		if (left instanceof GenderIs(Gender gender1) && right instanceof GenderIs(Gender gender)) {
			return gender1 != gender;
		}
		if (left instanceof PlayerRaceIs(Race race1) && right instanceof PlayerRaceIs(Race race)) {
			return race1 != race;
		}
		if (left instanceof PlayerClassIs(PlayerClass startingClass1) && right instanceof PlayerClassIs(PlayerClass startingClass)) {
			return startingClass1 != startingClass;
		}
		if (left instanceof AdvancedClassIs(PlayerClass aClass) && right instanceof AdvancedClassIs(PlayerClass playerClass)) {
			return aClass != playerClass;
		}
		if (left instanceof WorldIs(int worldId1, boolean expected15) && right instanceof WorldIs(int worldId, boolean expected14)) {
			return worldId1 == worldId && expected15 != expected14;
		}
		if (left instanceof WorldNpcIs(int npcId1, boolean expected13) && right instanceof WorldNpcIs(int npcId, boolean expected12)) {
			return npcId1 == npcId && expected13 != expected12;
		}
		if (left instanceof ZoneIs(String zone1, boolean expected11) && right instanceof ZoneIs(String zone, boolean expected10)) {
			return zone1.equals(zone) && expected11 != expected10;
		}
		if (left instanceof EquipmentSetEquipped(Set<Integer> ids, int count3, boolean expected9) && right instanceof EquipmentSetEquipped(
			Set<Integer> setIds, int count2, boolean expected8
		)) {
			return count3 == count2 && ids.equals(setIds) && expected9 != expected8;
		}
		if (left instanceof EquippedItem(
			int itemId1, int count1, boolean expected7
		) && right instanceof EquippedItem(int itemId, int count, boolean expected6)) {
			return itemId1 == itemId && count1 == count && expected7 != expected6;
		}
		if (left instanceof MembershipPermission(QuestMembershipPermission permission1, boolean expected5) && right instanceof MembershipPermission(
			QuestMembershipPermission permission, boolean expected4
		)) {
			return permission1 == permission && expected5 != expected4;
		}
		if (left instanceof CompleteCountIs(int value1, boolean expected3) && right instanceof CompleteCountIs(int value, boolean expected2)) {
			return value1 == value && expected3 != expected2;
		}
		if (left instanceof EventActive(int id, boolean expected1) && right instanceof EventActive(int questId, boolean expected)) {
			return id == questId && expected1 != expected;
		}
		return false;
	}

	/**
	 * 判断任务变量条件对在数值区间上是否互斥。
	 * Determines exclusivity for quest-variable condition pairs by value range.
	 */
	private static boolean variableConditionsAreMutuallyExclusive(QuestCondition left, QuestCondition right) {
		if (left instanceof QuestVariableIs(String field13, int value13) && right instanceof QuestVariableIs(String field12, int value12)) {
			return field13.equals(field12) && value13 != value12;
		}
		if (left instanceof QuestVariableIs(String field11, int value11) && right instanceof VariableAtLeast(String field10, int value10)) {
			return field11.equals(field10) && value11 < value10;
		}
		if (left instanceof VariableAtLeast(String field9, int value9) && right instanceof QuestVariableIs(String field8, int value8)) {
			return field9.equals(field8) && value8 < value9;
		}
		if (left instanceof QuestVariableIs(String field7, int value7) && right instanceof VariableBelow(String field6, int value6)) {
			return field7.equals(field6) && value7 >= value6;
		}
		if (left instanceof VariableBelow(String field5, int value5) && right instanceof QuestVariableIs(String field4, int value4)) {
			return field5.equals(field4) && value4 >= value5;
		}
		if (left instanceof VariableAtLeast(String field3, int value3) && right instanceof VariableBelow(String field2, int value2)) {
			return field3.equals(field2) && value3 >= value2;
		}
		if (left instanceof VariableBelow(String field1, int value1) && right instanceof VariableAtLeast(String field, int value)) {
			return field1.equals(field) && value >= value1;
		}
		return false;
	}

	/**
	 * 匹配从活跃账户捕获的类型化成员权限。
	 * Matches a typed membership capability captured from the live account.
	 */
	record MembershipPermission(QuestMembershipPermission permission, boolean expected) implements QuestCondition {
		public MembershipPermission {
			if (permission == null) {
				throw new NullPointerException("permission");
			}
		}

		public MembershipPermission(QuestMembershipPermission permission) {
			this(permission, true);
		}
	}

	/**
	 * 匹配装备投影中某具体物品的副本数量。
	 * Matches the number of copies of a concrete item in the equipment projection.
	 */
	record EquippedItem(int itemId, int count, boolean expected) implements QuestCondition {
		public EquippedItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("item id and count must be positive");
			}
		}

		public EquippedItem(int itemId, int count) {
			this(itemId, count, true);
		}

		public EquippedItem(int itemId) {
			this(itemId, 1, true);
		}
	}

	/**
	 * 匹配任务是否满足全部启动条件。
	 * Matches when the quest's start eligibility holds.
	 */
	record StartEligible() implements QuestCondition {
	}

	/**
	 * 当列出的每个前置任务都已完成时匹配。
	 * Matches when every listed prerequisite quest is already completed.
	 */
	record QuestsFinished(Set<Integer> questIds) implements QuestCondition {
		public QuestsFinished {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/**
	 * 当列出的前置任务均未完成时匹配。
	 * Matches when none of the listed prerequisite quests is completed yet.
	 */
	record UnfinishedQuest(Set<Integer> questIds) implements QuestCondition {
		public UnfinishedQuest {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/**
	 * 当列出的任务均未完成且当前未接取（START/REWARD）时匹配。
	 * Matches when none of the listed quests is completed or currently acquired (START/REWARD).
	 */
	record NoAcquiredQuest(Set<Integer> questIds) implements QuestCondition {
		public NoAcquiredQuest {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/**
	 * 当列出的任务均已接取（完成或在进行中）时匹配，镜像旧版 {@code acquired} 起始条件。
	 * Matches when every listed quest is already acquired (completed or in progress),
	 * mirroring the legacy {@code acquired} start condition.
	 */
	record AcquiredQuest(Set<Integer> questIds) implements QuestCondition {
		public AcquiredQuest {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/**
	 * 匹配玩家的起始职业（进阶职业归一化为其基础职业）。
	 * Matches the player's starting class (advanced classes normalize to their base).
	 */
	record PlayerClassIs(PlayerClass startingClass) implements QuestCondition {
		public PlayerClassIs {
			if (startingClass == null) {
				throw new NullPointerException("startingClass");
			}
		}
	}

	/**
	 * 匹配从玩家捕获的具体进阶职业。
	 * Matches the concrete advanced class captured from the player.
	 */
	record AdvancedClassIs(PlayerClass playerClass) implements QuestCondition {
		public AdvancedClassIs {
			if (playerClass == null || playerClass == PlayerClass.ALL || playerClass.isStartingClass()) {
				throw new IllegalArgumentException("playerClass must be a concrete advanced class");
			}
		}
	}

	/**
	 * 匹配快照中的玩家性别；未知事实安全失败。
	 * Matches captured gender; unknown facts fail closed.
	 */
	record GenderIs(Gender gender) implements QuestCondition {
		public GenderIs {
			if (gender == null || gender == Gender.DUMMY) {
				throw new IllegalArgumentException("gender must be MALE or FEMALE");
			}
		}
	}

	/**
	 * 匹配玩家的权威种族投影。
	 * Matches the player's authoritative race projection.
	 */
	record PlayerRaceIs(Race race) implements QuestCondition {
		public PlayerRaceIs {
			if (race == null || race == Race.PC_ALL) {
				throw new IllegalArgumentException("race must be ELYOS or ASMODIANS");
			}
		}
	}

	/**
	 * 匹配玩家是否在常规小队中（而非仅仅是同盟）。
	 * Matches whether the player is in a regular group (not merely an alliance).
	 */
	record PlayerInGroup(boolean expected) implements QuestCondition {
		public PlayerInGroup() {
			this(true);
		}
	}

	/**
	 * 匹配玩家当前所在世界；expected=false 为显式「不在该世界」情形。
	 * Matches the world the player is currently in; expected=false is the explicit "not in world" case.
	 */
	record WorldIs(int worldId, boolean expected) implements QuestCondition {
		public WorldIs {
			if (worldId <= 0) {
				throw new IllegalArgumentException("worldId must be positive");
			}
		}
	}

	/**
	 * 匹配玩家占据的一个权威区域。
	 * Matches one authoritative zone occupied by the player.
	 */
	record ZoneIs(String zone, boolean expected) implements QuestCondition {
		public ZoneIs {
			if (zone == null || zone.isBlank()) {
				throw new IllegalArgumentException("zone must not be blank");
			}
			zone = zone.toUpperCase(java.util.Locale.ROOT);
		}

		public ZoneIs(String zone) {
			this(zone, true);
		}
	}

	/**
	 * 目标严格低于某一 HP 百分比时匹配权威 AttackNpc 回调。
	 * Matches an authoritative AttackNpc callback while the target is below a strict HP percentage.
	 */
	record NpcHpBelowPercent(int npcId, int percent) implements QuestCondition {
		public NpcHpBelowPercent {
			if (npcId <= 0) {
				throw new IllegalArgumentException("npcId must be positive");
			}
			if (percent < 0 || percent > 100) {
				throw new IllegalArgumentException("percent must be between 0 and 100");
			}
		}
	}

	/**
	 * 货币扣减分支前匹配捕获的余额。
	 * Matches a captured balance before a currency debit branch.
	 */
	record CurrencyAtLeast(QuestRewardKind kind, long amount) implements QuestCondition {
		public CurrencyAtLeast {
			if (kind == null || !kind.isCurrency()) {
				throw new IllegalArgumentException("kind must be a supported currency");
			}
			if (amount <= 0) {
				throw new IllegalArgumentException("amount must be positive");
			}
		}
	}

	/**
	 * 匹配严格低于所需金额的捕获货币余额。
	 * Matches a captured currency balance strictly below the required amount.
	 */
	record CurrencyBelow(QuestRewardKind kind, long amount) implements QuestCondition {
		public CurrencyBelow {
			if (kind == null || !kind.isCurrency()) {
				throw new IllegalArgumentException("kind must be a supported currency");
			}
			if (amount <= 0) {
				throw new IllegalArgumentException("amount must be positive");
			}
		}
	}

	/**
	 * 匹配任一列出的装备套装是否恰好具有请求的已装备部件数量。
	 * Matches whether any listed equipment set has exactly the requested equipped-part count.
	 */
	record EquipmentSetEquipped(Set<Integer> setIds, int count, boolean expected) implements QuestCondition {
		public EquipmentSetEquipped {
			if (setIds == null || setIds.isEmpty() || setIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("setIds must contain positive ids");
			}
			if (count < 0) {
				throw new IllegalArgumentException("count must be non-negative");
			}
			setIds = Set.copyOf(setIds);
		}

		public EquipmentSetEquipped(Set<Integer> setIds, int count) {
			this(setIds, count, true);
		}
	}

	/**
	 * 当捕获的当前 DP 等于捕获的最大 DP 时匹配。
	 * Matches when the captured current DP equals the captured maximum DP.
	 */
	record DpAtMax() implements QuestCondition {
	}

	/**
	 * 匹配任务至今完成的次数（例如第九次完成解锁额外的事件奖励）。
	 * Matches the number of times the quest has been completed so far
	 * (for example the ninth completion unlocking an extra event reward).
	 */
	record CompleteCountIs(int value, boolean expected) implements QuestCondition {
		public CompleteCountIs {
			if (value < 0) {
				throw new IllegalArgumentException("value must be non-negative");
			}
		}

		public CompleteCountIs(int value) {
			this(value, true);
		}
	}

	/**
	 * 匹配当前游戏事件是否仍包含此任务，镜像 {@code EventService.checkQuestIsActive}。
	 * 未知事实安全失败。
	 * Matches whether the current game event still contains this quest,
	 * mirroring {@code EventService.checkQuestIsActive}. Unknown facts fail closed.
	 */
	record EventActive(int questId, boolean expected) implements QuestCondition {
		public EventActive {
			if (questId < 0) {
				throw new IllegalArgumentException("questId must not be negative");
			}
		}

		public EventActive() {
			this(0, true);
		}

		public EventActive(boolean expected) {
			this(0, expected);
		}

		public EventActive(int questId) {
			this(questId, true);
		}
	}

	/**
	 * 匹配玩家当前世界实例中是否存在某 NPC 模板。
	 * Matches presence of an NPC template in the player's current world instance.
	 */
	record WorldNpcIs(int npcId, boolean expected) implements QuestCondition {
		public WorldNpcIs {
			if (npcId <= 0) {
				throw new IllegalArgumentException("npcId must be positive");
			}
		}
	}

	/**
	 * 匹配任务状态投影等于给定状态。
	 * Matches when the quest status projection equals the given status.
	 */
	record StatusIs(QuestStatus status) implements QuestCondition {
		public StatusIs {
			if (status == null) {
				throw new NullPointerException("status");
			}
		}
	}

	/**
	 * 匹配玩家背包中某物品的副本数量。
	 * Matches the number of copies of an item in the player's inventory.
	 */
	record HasItem(int itemId, int count, boolean expected) implements QuestCondition {
		public HasItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("item id and count must be positive");
			}
		}

		public HasItem(int itemId, int count) {
			this(itemId, count, true);
		}
	}

	/**
	 * 匹配任务变量字段等于给定值。
	 * Matches when a quest variable field equals the given value.
	 */
	record QuestVariableIs(String field, int value) implements QuestCondition {
		public QuestVariableIs {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/**
	 * 字段值达到阈值（大于等于）。
	 * Matches when the field is at least the given value.
	 */
	record VariableAtLeast(String field, int value) implements QuestCondition {
		public VariableAtLeast {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/**
	 * 字段值低于上限（小于）。
	 * Matches when the field is below the given cap.
	 */
	record VariableBelow(String field, int value) implements QuestCondition {
		public VariableBelow {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/**
	 * 列出的进度字段之和等于目标值时匹配。
	 * Matches when the sum of the listed progress fields equals the target value.
	 */
	record VariableSumIs(List<String> fields, int value) implements QuestCondition {
		public VariableSumIs {
			fields = validatedFields(fields);
		}
	}

	/**
	 * 列出的进度字段之和低于目标值时匹配。
	 * Matches when the sum of the listed progress fields is below the target value.
	 */
	record VariableSumBelow(List<String> fields, int value) implements QuestCondition {
		public VariableSumBelow {
			fields = validatedFields(fields);
		}
	}

	/**
	 * 匹配权威配方事实，包括显式「未学会」情形。
	 * Matches an authoritative recipe fact, including the explicit "not known" case.
	 */
	record RecipeKnown(int recipeId, boolean expected) implements QuestCondition {
		public RecipeKnown {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("recipeId must be positive");
			}
		}
	}

	/**
	 * 匹配事件前捕获的专家/大师资格。
	 * Matches expert/master eligibility captured before the event.
	 */
	record CanGrantCraftSkill(int skillId, int targetLevel) implements QuestCondition {
		public CanGrantCraftSkill {
			if (skillId <= 0 || targetLevel <= 0) {
				throw new IllegalArgumentException("skillId and targetLevel must be positive");
			}
		}
	}

	/**
	 * 按闭区间旧版 PvP 窗口匹配 recipientLevel - victimLevel。
	 * Matches recipientLevel - victimLevel against an inclusive legacy PvP window.
	 */
	record PvpVictimLevelDelta(int minimumRecipientDelta, int maximumRecipientDelta) implements QuestCondition {
		public PvpVictimLevelDelta {
			if (minimumRecipientDelta > maximumRecipientDelta) {
				throw new IllegalArgumentException("minimumRecipientDelta must not exceed maximumRecipientDelta");
			}
		}
	}

	/**
	 * 匹配获得积分的 PvP 接收者占据的一个权威区域。
	 * Matches one authoritative Zone occupied by the credited PvP recipient.
	 */
	record PvpRecipientInZone(String zone) implements QuestCondition {
		public PvpRecipientInZone {
			if (zone == null || zone.isBlank()) {
				throw new IllegalArgumentException("zone must not be blank");
			}
			zone = zone.toUpperCase(java.util.Locale.ROOT);
		}
	}

	private static List<String> validatedFields(List<String> fields) {
		Objects.requireNonNull(fields, "fields");
		if (fields.isEmpty()) {
			throw new IllegalArgumentException("fields must not be empty");
		}
		List<String> copy = List.copyOf(fields);
		if (copy.stream().anyMatch(field -> field == null || field.isBlank())) {
			throw new IllegalArgumentException("fields must not contain blank values");
		}
		if (copy.size() != new HashSet<>(copy).size()) {
			throw new IllegalArgumentException("fields must not contain duplicates");
		}
		return copy;
	}
}
