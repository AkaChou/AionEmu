package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 类型化 Java DSL，经与 XML 相同的编译器降级。
 * Typed Java DSL that lowers through the same compiler as XML.
 */
public final class QuestDsl {
	private QuestDsl() {
	}

	public static QuestBuilder quest(int id) {
		return new QuestBuilder(id);
	}

	public static BitField bitField(String name, int offset, int width, PersistenceMode persistence) {
		return new BitField(name, offset, width, persistence);
	}

	public static Map<String, Integer> vars(String name, int value) {
		return Map.of(name, value);
	}

	public static Map<String, Integer> vars(String firstName, int firstValue, String secondName, int secondValue) {
		Map<String, Integer> values = new LinkedHashMap<>();
		values.put(firstName, firstValue);
		values.put(secondName, secondValue);
		return Map.copyOf(values);
	}

	public static NodeProjection project(QuestStatus status, Map<String, Integer> variables) {
		return new NodeProjection(status, variables);
	}

	public static QuestEvent talkToNpc(int npcId) {
		return new QuestEvent.TalkToNpc(npcId);
	}

	public static QuestEvent talkToNpc(int npcId, QuestDialog dialog) {
		return new QuestEvent.TalkToNpc(npcId, Objects.requireNonNull(dialog, "dialog").id());
	}

	public static QuestEvent killNpc(int npcId) {
		return new QuestEvent.KillNpc(npcId);
	}

	/**
	 * 任一列出的 npc 击杀都满足该事件。
	 * Any listed npc kill satisfies the event.
	 */
	public static QuestEvent killNpcIds(int... npcIds) {
		return new QuestEvent.KillNpcSet(java.util.Arrays.stream(npcIds).boxed().collect(java.util.stream.Collectors.toSet()));
	}

	public static QuestEvent attackNpc(int npcId) {
		return new QuestEvent.AttackNpc(npcId);
	}

	public static QuestEvent useItem(int itemId) {
		return new QuestEvent.UseItem(itemId);
	}

	/**
	 * 无目标任务对话选择，例如物品触发任务的接受/拒绝流程。
	 * Targetless quest-dialog selection, such as an item-start accept/refuse flow.
	 */
	public static QuestEvent questDialog(int dialogId) {
		return new QuestEvent.QuestDialog(dialogId);
	}

	public static QuestEvent questDialog(QuestDialog dialog) {
		return new QuestEvent.QuestDialog(Objects.requireNonNull(dialog, "dialog").id());
	}

	public static QuestEvent collectItem(int itemId, int count) {
		return new QuestEvent.CollectItem(itemId, count);
	}

	public static QuestEvent itemPlay(int itemId, int animationMillis) {
		return new QuestEvent.ItemPlay(itemId, animationMillis);
	}

	public static QuestEvent houseItemUse(int itemId) {
		return new QuestEvent.HouseItemUse(itemId);
	}

	public static QuestEvent getItem(int itemId) {
		return new QuestEvent.GetItem(itemId);
	}

	public static QuestEvent levelUp() {
		return new QuestEvent.LevelUp();
	}

	public static QuestEvent zoneMissionEnd() {
		return new QuestEvent.ZoneMissionEnd();
	}

	public static QuestEvent die() {
		return new QuestEvent.Die();
	}

	public static QuestEvent logOut() {
		return new QuestEvent.LogOut();
	}

	public static QuestEvent abandon() {
		return new QuestEvent.Abandon();
	}

	public static QuestEvent enterWorld() {
		return new QuestEvent.EnterWorld();
	}

	public static QuestEvent enterZone(String zone) {
		return new QuestEvent.EnterZone(zone);
	}

	public static QuestEvent leaveZone(String zone) {
		return new QuestEvent.LeaveZone(zone);
	}

	public static QuestEvent passFlyingRing(String ring) {
		return new QuestEvent.PassFlyingRing(ring);
	}

	public static QuestEvent movieEnd(int movieId) {
		return new QuestEvent.MovieEnd(movieId);
	}

	public static QuestEvent questTimerEnd() {
		return new QuestEvent.QuestTimerEnd();
	}

	public static QuestEvent invisibleTimerEnd() {
		return new QuestEvent.InvisibleTimerEnd();
	}

	public static QuestEvent killRanked(int rankId) {
		return new QuestEvent.KillRanked(rankId);
	}

	public static QuestEvent killInWorld(int worldId) {
		return new QuestEvent.KillInWorld(worldId);
	}

	public static QuestEvent useSkill(int skillId) {
		return new QuestEvent.UseSkill(skillId);
	}

	public static QuestEvent failCraft(int itemId) {
		return new QuestEvent.FailCraft(itemId);
	}

	public static QuestEvent equipItem(int itemId) {
		return new QuestEvent.EquipItem(itemId);
	}

	public static QuestEvent canAct(int templateId, String actionType) {
		return new QuestEvent.CanAct(templateId, actionType);
	}

	public static QuestEvent dredgionReward() {
		return new QuestEvent.DredgionReward();
	}

	public static QuestEvent kamarReward() {
		return new QuestEvent.KamarReward();
	}

	public static QuestEvent ophidanReward() {
		return new QuestEvent.OphidanReward();
	}

	public static QuestEvent bastionReward() {
		return new QuestEvent.BastionReward();
	}

	public static QuestEvent bonusApply(String bonusType) {
		return new QuestEvent.BonusApply(bonusType);
	}

	public static QuestEvent addAggroList(int npcId) {
		return new QuestEvent.AddAggroList(npcId);
	}

	public static QuestEvent atDistance(int npcId) {
		return new QuestEvent.AtDistance(npcId);
	}

	public static QuestEvent protectEnd() {
		return new QuestEvent.ProtectEnd();
	}

	public static QuestEvent protectFail() {
		return new QuestEvent.ProtectFail();
	}

	public static QuestEvent enterWindStream(int teleportId) {
		return new QuestEvent.EnterWindStream(teleportId);
	}

	public static QuestEvent rideAction(int itemId) {
		return new QuestEvent.RideAction(itemId);
	}

	public static QuestEvent creativityPoint() {
		return new QuestEvent.CreativityPoint();
	}

	public static QuestEvent npcReachTarget() {
		return new QuestEvent.NpcReachTarget();
	}

	public static QuestEvent npcLostTarget() {
		return new QuestEvent.NpcLostTarget();
	}

	public static QuestCondition statusIs(QuestStatus status) {
		return new QuestCondition.StatusIs(status);
	}

	public static QuestCondition hasItem(int itemId, int count) {
		return new QuestCondition.HasItem(itemId, count);
	}

	public static QuestCondition variableIs(String field, int value) {
		return new QuestCondition.QuestVariableIs(field, value);
	}

	public static QuestCondition variableAtLeast(String field, int value) {
		return new QuestCondition.VariableAtLeast(field, value);
	}

	public static QuestCondition variableBelow(String field, int value) {
		return new QuestCondition.VariableBelow(field, value);
	}

	public static QuestCondition variableSumIs(List<String> fields, int value) {
		return new QuestCondition.VariableSumIs(fields, value);
	}

	public static QuestCondition variableSumBelow(List<String> fields, int value) {
		return new QuestCondition.VariableSumBelow(fields, value);
	}

	public static QuestCondition recipeKnown(int recipeId) {
		return new QuestCondition.RecipeKnown(recipeId, true);
	}

	public static QuestCondition recipeNotKnown(int recipeId) {
		return new QuestCondition.RecipeKnown(recipeId, false);
	}

	public static QuestCondition canGrantCraftSkill(int skillId, int targetLevel) {
		return new QuestCondition.CanGrantCraftSkill(skillId, targetLevel);
	}

	public static QuestCondition pvpVictimLevelDelta(int minimumRecipientDelta, int maximumRecipientDelta) {
		return new QuestCondition.PvpVictimLevelDelta(minimumRecipientDelta, maximumRecipientDelta);
	}

	public static QuestCondition pvpRecipientInZone(String zone) {
		return new QuestCondition.PvpRecipientInZone(zone);
	}

	public static QuestCondition startEligible() {
		return new QuestCondition.StartEligible();
	}

	public static QuestCondition questsFinished(Set<Integer> questIds) {
		return new QuestCondition.QuestsFinished(questIds);
	}

	public static QuestCondition questsFinished(int... questIds) {
		Set<Integer> ids = new java.util.LinkedHashSet<>();
		for (int questId : questIds) {
			ids.add(questId);
		}
		return new QuestCondition.QuestsFinished(ids);
	}

	public static QuestCondition advancedClassIs(PlayerClass playerClass) {
		return new QuestCondition.AdvancedClassIs(playerClass);
	}

	public static QuestCondition playerInGroup() {
		return new QuestCondition.PlayerInGroup();
	}

	public static QuestCondition playerInGroup(boolean expected) {
		return new QuestCondition.PlayerInGroup(expected);
	}

	public static QuestCondition worldNpcIs(int npcId, boolean expected) {
		return new QuestCondition.WorldNpcIs(npcId, expected);
	}

	public static QuestCondition zoneIs(String zone) {
		return new QuestCondition.ZoneIs(zone);
	}

	public static QuestCondition npcHpBelowPercent(int npcId, int percent) {
		return new QuestCondition.NpcHpBelowPercent(npcId, percent);
	}

	public static QuestCondition currencyAtLeast(QuestRewardKind kind, long amount) {
		return new QuestCondition.CurrencyAtLeast(kind, amount);
	}

	public static QuestCondition currencyBelow(QuestRewardKind kind, long amount) {
		return new QuestCondition.CurrencyBelow(kind, amount);
	}

	public static QuestCondition equipmentSetEquipped(Set<Integer> setIds, int count) {
		return new QuestCondition.EquipmentSetEquipped(setIds, count);
	}

	public static QuestCondition equipmentSetEquipped(Set<Integer> setIds, int count, boolean expected) {
		return new QuestCondition.EquipmentSetEquipped(setIds, count, expected);
	}

	public static QuestCondition dpAtMax() {
		return new QuestCondition.DpAtMax();
	}

	/**
	 * 已完成次数等于给定值（如第 9 次完成解锁额外奖励）。
	 * Matches when the completion count equals the given value (e.g. the 9th completion unlocks an extra reward).
	 */
	public static QuestCondition completeCountIs(int value) {
		return new QuestCondition.CompleteCountIs(value);
	}

	public static QuestCondition completeCountIs(int value, boolean expected) {
		return new QuestCondition.CompleteCountIs(value, expected);
	}

	/**
	 * 当前活动包含本任务（EventService.checkQuestIsActive）。
	 * Matches when the current event still contains this quest (EventService.checkQuestIsActive).
	 */
	public static QuestCondition eventActive() {
		return new QuestCondition.EventActive();
	}

	public static QuestCondition eventActive(boolean expected) {
		return new QuestCondition.EventActive(expected);
	}

	/**
	 * 指定任务属于当前活动。
	 * Matches when the given quest belongs to the current event.
	 */
	public static QuestCondition eventActive(int questId) {
		return new QuestCondition.EventActive(questId);
	}

	public static QuestCondition eventActive(int questId, boolean expected) {
		return new QuestCondition.EventActive(questId, expected);
	}

	public static QuestCondition playerRaceIs(Race race) {
		return new QuestCondition.PlayerRaceIs(race);
	}

	public static QuestCondition equippedItem(int itemId) {
		return new QuestCondition.EquippedItem(itemId);
	}

	public static QuestCondition equippedItem(int itemId, int count) {
		return new QuestCondition.EquippedItem(itemId, count);
	}

	public static QuestCondition equippedItem(int itemId, int count, boolean expected) {
		return new QuestCondition.EquippedItem(itemId, count, expected);
	}

	public static QuestCondition membershipPermission(QuestMembershipPermission permission) {
		return new QuestCondition.MembershipPermission(permission);
	}

	public static QuestCondition membershipPermission(QuestMembershipPermission permission, boolean expected) {
		return new QuestCondition.MembershipPermission(permission, expected);
	}

	public static QuestAction giveItem(int itemId, int count) {
		return new QuestAction.GiveItem(itemId, count);
	}

	public static QuestAction removeItem(int itemId, int count) {
		return new QuestAction.RemoveItem(itemId, count);
	}

	/**
	 * 在同一事务中移除当前持有的某物品全部数量。
	 * Removes every currently held copy of an item in the same transaction.
	 */
	public static QuestAction removeAllItem(int itemId) {
		return new QuestAction.RemoveItem(itemId, QuestAction.RemoveItem.ALL);
	}

	public static QuestAction unequipItem(int itemId) {
		return new QuestAction.UnequipItem(itemId);
	}

	/**
	 * 卸下物品，并移除归还背包的最多 {@code count} 个副本。
	 * Unequips the item and removes up to {@code count} copies returned to inventory.
	 */
	public static QuestAction unequipItemAndRemoveReturned(int itemId, int count) {
		return new QuestAction.UnequipItem(itemId, count);
	}

	public static QuestAction setVariable(String field, int value) {
		return new QuestAction.SetVariable(field, value);
	}

	public static QuestAction incrementVariable(String field, int delta) {
		return new QuestAction.IncrementVariable(field, delta);
	}

	public static QuestAction blockDefaultItemUse() {
		return new QuestAction.BlockDefaultItemUse();
	}

	public static QuestAction setStatus(QuestStatus status) {
		return new QuestAction.SetStatus(status);
	}

	public static QuestAction grantReward(String kind, int id, long amount) {
		return new QuestAction.GrantReward(kind, id, amount);
	}

	public static QuestAction grantQuestBaseReward(String kind, int id, long amount) {
		return new QuestAction.GrantReward(kind, id, amount, QuestRewardAmountMode.QUEST_BASE);
	}

	public static QuestAction grantSelectedReward(int rewardIndex) {
		return new QuestAction.GrantSelectedReward(rewardIndex);
	}

	public static QuestAction decreaseCurrency(QuestRewardKind kind, long amount) {
		return new QuestAction.DecreaseCurrency(kind, amount);
	}

	/**
	 * 在同一事务中将受支持货币设置为精确的非负余额。
	 * Sets a supported currency to an exact non-negative balance in the same transaction.
	 */
	public static QuestAction setCurrency(QuestRewardKind kind, long amount) {
		return new QuestAction.SetCurrency(kind, amount);
	}

	/**
	 * 在同一事务中将受支持货币重置为零。
	 * Resets a supported currency to zero in the same transaction.
	 */
	public static QuestAction resetCurrency(QuestRewardKind kind) {
		return new QuestAction.SetCurrency(kind, 0);
	}

	public static QuestAction completeQuest(int rewardIndex) {
		return new QuestAction.CompleteQuest(rewardIndex);
	}

	public static QuestAction learnRecipe(int recipeId, QuestRecipeOwnership ownership) {
		return new QuestAction.LearnRecipe(recipeId, ownership);
	}

	public static QuestAction forgetRecipe(int recipeId) {
		return new QuestAction.ForgetRecipe(recipeId);
	}

	public static QuestAction grantCraftSkill(int skillId, int targetLevel, boolean autoLearnRecipes) {
		return new QuestAction.GrantCraftSkill(skillId, targetLevel, autoLearnRecipes);
	}

	/**
	 * 服务端强制放弃任务（QuestService.abandonQuest 完整清理语义）；要求目标节点为 NONE 投影。
	 * Server-forced quest abandon (full QuestService.abandonQuest cleanup semantics);
	 * requires the target node to project to NONE.
	 */
	public static QuestAction abandonQuest() {
		return new QuestAction.AbandonQuest();
	}

	public static AfterCommitAction closeDialog() {
		return new AfterCommitAction.CloseDialog();
	}

	public static AfterCommitAction syncQuestState(QuestStateSyncMode mode) {
		return new AfterCommitAction.SyncQuestState(mode);
	}

	public static AfterCommitAction refreshPlayerStats() {
		return new AfterCommitAction.RefreshPlayerStats();
	}

	public static AfterCommitAction showQuestDialog(int dialogId) {
		return new AfterCommitAction.ShowQuestDialog(dialogId);
	}

	/**
	 * 发送不附带任务 id 的原始 SM_DIALOG_WINDOW。
	 * Sends a raw SM_DIALOG_WINDOW without attaching the quest id.
	 */
	public static AfterCommitAction showDialogWindow(int dialogId) {
		return new AfterCommitAction.ShowDialogWindow(dialogId);
	}

	public static AfterCommitAction showQuestSelectionDialog(int dialogId) {
		return new AfterCommitAction.ShowQuestSelectionDialog(dialogId);
	}

	public static AfterCommitAction teleportPlayer(int worldId, float x, float y, float z, byte heading) {
		return new AfterCommitAction.TeleportPlayer(worldId, x, y, z, heading);
	}

	public static AfterCommitAction teleportPlayer(QuestInstanceTarget instanceTarget, int worldId,
			float x, float y, float z, byte heading) {
		return new AfterCommitAction.TeleportPlayer(instanceTarget, worldId, x, y, z, heading);
	}

	public static AfterCommitAction playMovie(int movieId) {
		return new AfterCommitAction.PlayMovie(movieId);
	}

	public static AfterCommitAction playMovie(int movieId, QuestMovieType type) {
		return new AfterCommitAction.PlayMovie(movieId, type);
	}

	/**
	 * 从多个影片中等概率随机播放一个。
	 * Plays one of several movies chosen uniformly at random.
	 */
	public static AfterCommitAction playMovieRandom(int... movieIds) {
		return new AfterCommitAction.PlayMovieRandom(
			java.util.Arrays.stream(movieIds).boxed().collect(java.util.stream.Collectors.toList()));
	}

	public static AfterCommitAction morph(int ascensionState) {
		return new AfterCommitAction.Morph(ascensionState);
	}

	public static AfterCommitAction setPlayerClass(PlayerClass playerClass) {
		return new AfterCommitAction.SetPlayerClass(playerClass);
	}

	public static AfterCommitAction applyEffect(int skillId, int durationMillis) {
		return new AfterCommitAction.ApplyEffect(skillId, durationMillis);
	}

	public static AfterCommitAction removeEffect(int effectId) {
		return new AfterCommitAction.RemoveEffect(effectId);
	}

	public static AfterCommitAction systemMessage(QuestSystemMessage message) {
		return new AfterCommitAction.SendSystemMessage(message);
	}

	public static AfterCommitAction systemMessage(QuestSystemMessagePacket message) {
		return new AfterCommitAction.SendSystemMessagePacket(message);
	}

	public static QuestCondition genderIs(Gender gender) {
		return new QuestCondition.GenderIs(gender);
	}

	public static AfterCommitAction playerEmotion(QuestPlayerEmotion emotion) {
		return new AfterCommitAction.PlayerEmotion(emotion);
	}

	public static AfterCommitAction addNpcAggro(int npcTemplateId, int damage) {
		return new AfterCommitAction.AddNpcAggro(npcTemplateId, damage);
	}

	public static AfterCommitAction spawnNpc(String slot, int worldId, int templateId, float x, float y, float z,
			byte heading) {
		return new AfterCommitAction.SpawnNpc(slot, worldId, templateId, x, y, z, heading);
	}

	public static AfterCommitAction spawnNpcInInstance(String slot, int worldId, int instanceId, int templateId,
			float x, float y, float z, byte heading) {
		return spawnNpc(slot, templateId, new QuestSpawnLocation.Fixed(worldId,
			QuestInstanceTarget.fixed(instanceId), x, y, z, heading));
	}

	public static AfterCommitAction spawnNpc(String slot, int templateId, QuestSpawnLocation location) {
		return new AfterCommitAction.SpawnNpc(slot, templateId, location);
	}

	public static AfterCommitAction spawnNpcAtPlayer(String slot, int templateId, byte heading) {
		return spawnNpc(slot, templateId, new QuestSpawnLocation.PlayerPosition(heading));
	}

	public static QuestSpawnVariant spawnVariant(int templateId, int worldId, float x, float y, float z,
		byte heading) {
		return new QuestSpawnVariant(templateId, new QuestSpawnLocation.Fixed(worldId,
			QuestInstanceTarget.currentOrDefault(), x, y, z, heading));
	}

	public static AfterCommitAction spawnNpcRandom(String slot, boolean replaceExisting,
		QuestSpawnVariant... variants) {
		return new AfterCommitAction.SpawnNpcRandom(slot, List.of(variants), replaceExisting);
	}

	public static AfterCommitAction despawnNpc(String slot) {
		return new AfterCommitAction.DespawnNpc(slot);
	}

	public static AfterCommitAction startFollow(String slot) {
		return new AfterCommitAction.StartFollow(slot);
	}

	public static AfterCommitAction startFollowCurrentTargetToPoint(float x, float y, float z) {
		return new AfterCommitAction.StartFollowCurrentTargetToPoint(x, y, z);
	}

	public static AfterCommitAction startFollowCurrentTargetToNpc(int npcId) {
		return new AfterCommitAction.StartFollowCurrentTargetToNpc(npcId);
	}

	public static AfterCommitAction stopFollow(String slot) {
		return new AfterCommitAction.StopFollow(slot);
	}

	public static AfterCommitAction attackTarget(String slot) {
		return new AfterCommitAction.AttackTarget(slot);
	}

	public static AfterCommitAction attackNpcTemplate(String slot, int templateId) {
		return new AfterCommitAction.AttackNpcTemplate(slot, templateId);
	}

	public static AfterCommitAction startWalking(String slot) {
		return new AfterCommitAction.StartWalking(slot);
	}

	public static AfterCommitAction broadcastNpcEmotion(String slot, QuestNpcEmotion emotion) {
		return new AfterCommitAction.BroadcastNpcEmotion(slot, emotion);
	}

	public static AfterCommitAction broadcastInteractionNpcEmotion(QuestNpcEmotion emotion) {
		return new AfterCommitAction.BroadcastInteractionNpcEmotion(emotion);
	}

	public static AfterCommitAction watchFollowZone(String slot, String zone) {
		return new AfterCommitAction.WatchFollowZone(slot, zone);
	}

	public static AfterCommitAction watchFollowCoordinate(String slot, float x, float y, float z) {
		return new AfterCommitAction.WatchFollowCoordinate(slot, x, y, z);
	}

	public static AfterCommitAction watchLuredNpcCoordinate(float x, float y, float z, float radius) {
		return new AfterCommitAction.WatchLuredNpcCoordinate(x, y, z, radius);
	}

	public static AfterCommitAction startQuestTimer(int seconds) {
		return new AfterCommitAction.StartQuestTimer(seconds);
	}

	public static AfterCommitAction startQuestTimer(int seconds, QuestTimerPolicy policy) {
		return new AfterCommitAction.StartQuestTimer(seconds, policy);
	}

	public static AfterCommitAction startInvisibleTimer(int seconds) {
		return new AfterCommitAction.StartInvisibleTimer(seconds);
	}

	public static AfterCommitAction startInvisibleTimer(int seconds, QuestTimerPolicy policy) {
		return new AfterCommitAction.StartInvisibleTimer(seconds, policy);
	}

	public static AfterCommitAction cancelQuestTimer() {
		return new AfterCommitAction.CancelQuestTimer();
	}

	public static AfterCommitAction cancelQuestTimer(QuestTimerPolicy.Identity identity) {
		return new AfterCommitAction.CancelQuestTimer(identity);
	}

	/**
	 * 对若干目标任务广播 zone-mission-end 事件，触发其启动/推进。
	 * Broadcasts a zone-mission-end event to the given quests, triggering their start/progress.
	 */
	public static AfterCommitAction broadcastZoneMissionEnd(int... questIds) {
		return new AfterCommitAction.BroadcastZoneMissionEnd(questIds);
	}

	public static AfterCommitAction scheduleEventQuestRefresh(int seconds, int... questIds) {
		return new AfterCommitAction.ScheduleEventQuestRefresh(seconds, questIds);
	}

	public static final class QuestBuilder {
		private final int id;
		private int version = 1;
		private QuestMetadata metadata;
		private final ProgressLayout.Builder progress = new ProgressLayout.Builder();
		private final List<QuestNode> nodes = new ArrayList<>();
		private final List<QuestTransition> transitions = new ArrayList<>();

		private QuestBuilder(int id) {
			if (id <= 0) {
				throw new IllegalArgumentException("quest id must be positive");
			}
			this.id = id;
			this.metadata = QuestMetadata.minimal("quest-" + id, 0, "QUEST");
		}

		public QuestBuilder version(int version) {
			this.version = version;
			return this;
		}

		public QuestBuilder metadata(QuestMetadata metadata) {
			this.metadata = Objects.requireNonNull(metadata, "metadata");
			return this;
		}

		public QuestBuilder progress(BitField... fields) {
			for (BitField field : fields) {
				progress.add(field);
			}
			return this;
		}

		public QuestBuilder node(String label, NodeProjection projection) {
			nodes.add(new QuestNode(label, projection));
			return this;
		}

		public TransitionBuilder on(QuestEvent event) {
			return new TransitionBuilder(this, event);
		}

		/** 在最后一条 transition 上追加 after-commit action;可连续调用追加多个。 */
		public QuestBuilder afterCommit(AfterCommitAction action) {
			if (transitions.isEmpty()) {
				throw new IllegalStateException("goTo must be called before afterCommit");
			}
			QuestTransition current = transitions.remove(transitions.size() - 1);
			List<AfterCommitAction> updated = new ArrayList<>(current.afterCommit());
			updated.add(Objects.requireNonNull(action, "action"));
			transitions.add(new QuestTransition(current.event(), current.conditions(), current.actions(),
				current.targetNode(), updated, current.priority(), current.sourceNode()));
			return this;
		}

		public CompiledQuestDefinition compile() {
			QuestDefinition definition = new QuestDefinition(id, version, metadata,
				progress.build(), nodes, transitions);
			return QuestDefinitionCompiler.compile(definition);
		}
	}

	public static final class TransitionBuilder {
		private final QuestBuilder owner;
		private final QuestEvent event;
		private final List<QuestCondition> conditions = new ArrayList<>();
		private final List<QuestAction> actions = new ArrayList<>();
		private final List<AfterCommitAction> afterCommit = new ArrayList<>();
		private Integer priority;
		private String sourceNode;

		private TransitionBuilder(QuestBuilder owner, QuestEvent event) {
			this.owner = owner;
			this.event = Objects.requireNonNull(event, "event");
		}

		public TransitionBuilder when(QuestCondition condition) {
			conditions.add(Objects.requireNonNull(condition, "condition"));
			return this;
		}

		public TransitionBuilder then(QuestAction action) {
			actions.add(Objects.requireNonNull(action, "action"));
			return this;
		}

		public TransitionBuilder priority(int priority) {
			if (priority < 0) {
				throw new IllegalArgumentException("priority must be non-negative");
			}
			this.priority = priority;
			return this;
		}

		public TransitionBuilder from(String sourceNode) {
			if (sourceNode == null || sourceNode.isBlank()) {
				throw new IllegalArgumentException("sourceNode must not be blank");
			}
			this.sourceNode = sourceNode;
			return this;
		}

		public QuestBuilder goTo(String targetNode) {
			if (targetNode == null || targetNode.isBlank()) {
				throw new IllegalArgumentException("targetNode must not be blank");
			}
			owner.transitions.add(new QuestTransition(event, conditions, actions, targetNode, afterCommit, priority,
				sourceNode));
			return owner;
		}

		public QuestBuilder afterCommit(AfterCommitAction action) {
			return owner.afterCommit(action);
		}

		public CompiledQuestDefinition compile() {
			return owner.compile();
		}
	}
}
