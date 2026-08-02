package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Typed Java DSL that lowers through the same compiler as XML. */
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

	public static QuestEvent attackNpc(int npcId) {
		return new QuestEvent.AttackNpc(npcId);
	}

	public static QuestEvent useItem(int itemId) {
		return new QuestEvent.UseItem(itemId);
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

	public static QuestAction removeItem(int itemId, int count) {
		return new QuestAction.RemoveItem(itemId, count);
	}

	public static QuestAction setVariable(String field, int value) {
		return new QuestAction.SetVariable(field, value);
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

	public static AfterCommitAction despawnNpc(String slot) {
		return new AfterCommitAction.DespawnNpc(slot);
	}

	public static AfterCommitAction startFollow(String slot) {
		return new AfterCommitAction.StartFollow(slot);
	}

	public static AfterCommitAction stopFollow(String slot) {
		return new AfterCommitAction.StopFollow(slot);
	}

	public static AfterCommitAction attackTarget(String slot) {
		return new AfterCommitAction.AttackTarget(slot);
	}

	public static AfterCommitAction startWalking(String slot) {
		return new AfterCommitAction.StartWalking(slot);
	}

	public static AfterCommitAction broadcastNpcEmotion(String slot, QuestNpcEmotion emotion) {
		return new AfterCommitAction.BroadcastNpcEmotion(slot, emotion);
	}

	public static AfterCommitAction watchFollowZone(String slot, String zone) {
		return new AfterCommitAction.WatchFollowZone(slot, zone);
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

	public static final class QuestBuilder {
		private final int id;
		private int version = 1;
		private QuestOwnership ownership = QuestOwnership.COMPILED_CANDIDATE;
		private final List<EvidenceRef> evidence = new ArrayList<>();
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

		public QuestBuilder ownership(QuestOwnership ownership) {
			this.ownership = Objects.requireNonNull(ownership, "ownership");
			return this;
		}

		public QuestBuilder evidence(EvidenceRef... evidence) {
			for (EvidenceRef ref : evidence) {
				this.evidence.add(Objects.requireNonNull(ref, "evidence"));
			}
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
			QuestDefinition definition = new QuestDefinition(id, version, ownership, evidence, metadata,
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
			owner.transitions.add(new QuestTransition(event, conditions, actions, targetNode, afterCommit, priority, sourceNode));
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
