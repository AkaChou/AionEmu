package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dispatches the closed required-action set to typed domain ports.
 * State actions remain owned by {@link QuestStatePort}; no action is routed by a string hook.
 */
public final class CompositeQuestActionPort implements QuestActionPort {
	private final QuestInventoryPort inventory;
	private final QuestCurrencyPort currency;
	private final QuestRewardPort rewards;
	private final QuestCraftPort craft;
	private final QuestEquipmentPort equipment;

	public CompositeQuestActionPort(QuestInventoryPort inventory, QuestCurrencyPort currency,
			QuestRewardPort rewards) {
		this(inventory, currency, rewards, null, null);
	}

	public CompositeQuestActionPort(QuestInventoryPort inventory, QuestCurrencyPort currency,
			QuestRewardPort rewards, QuestCraftPort craft) {
		this(inventory, currency, rewards, craft, null);
	}

	public CompositeQuestActionPort(QuestInventoryPort inventory, QuestCurrencyPort currency,
			QuestRewardPort rewards, QuestCraftPort craft, QuestEquipmentPort equipment) {
		this.inventory = Objects.requireNonNull(inventory, "inventory");
		this.currency = Objects.requireNonNull(currency, "currency");
		this.rewards = Objects.requireNonNull(rewards, "rewards");
		this.craft = craft;
		this.equipment = equipment;
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
			throws SQLException {
		Buckets buckets = Buckets.from(actions);
		if (!buckets.unequips().isEmpty()) {
			if (equipment == null) {
				throw new SQLException("equipment actions require an equipment port");
			}
			equipment.preflight(connection, snapshot, buckets.unequips());
		}
		inventory.preflight(connection, snapshot, buckets.removals(), buckets.gives(), buckets.unequips());
		currency.preflight(connection, snapshot, buckets.currencyRewards());
		rewards.preflight(connection, snapshot, buckets.durableRewards());
		if (!buckets.craftActions().isEmpty()) {
			if (craft == null) {
				throw new SQLException("craft actions require a craft port");
			}
			craft.preflight(connection, snapshot, buckets.craftActions());
		}
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
			throws SQLException {
		Buckets buckets = Buckets.from(actions);
		List<QuestTransactionParticipant> participants = new ArrayList<>();
		try {
			if (!buckets.unequips().isEmpty()) {
				if (equipment == null) {
					throw new SQLException("equipment actions require an equipment port");
				}
				// Unequip first: a quest may immediately consume the returned item.
				participants.add(equipment.apply(connection, snapshot, buckets.unequips()));
			}
			participants.add(inventory.apply(connection, snapshot, buckets.removals(), buckets.gives(), buckets.unequips()));
			participants.add(currency.apply(connection, snapshot, buckets.currencyRewards()));
			participants.add(rewards.apply(connection, snapshot, buckets.durableRewards()));
			if (!buckets.craftActions().isEmpty()) {
				if (craft == null) {
					throw new SQLException("craft actions require a craft port");
				}
				participants.add(craft.apply(connection, snapshot, buckets.craftActions()));
			}
			return QuestTransactionParticipant.compose(participants);
		} catch (SQLException | RuntimeException failure) {
			QuestTransactionParticipant.rollbackApplied(participants, failure);
			throw failure;
		}
	}

	private record Buckets(List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives,
			List<QuestAction.UnequipItem> unequips,
			List<QuestAction.GrantReward> currencyRewards,
			List<QuestAction.GrantReward> durableRewards, List<QuestAction> craftActions) {
		private static Buckets from(List<QuestAction> actions) {
			Objects.requireNonNull(actions, "actions");
			List<QuestAction.RemoveItem> removals = new ArrayList<>();
			List<QuestAction.GiveItem> gives = new ArrayList<>();
			List<QuestAction.UnequipItem> unequips = new ArrayList<>();
			List<QuestAction.GrantReward> currency = new ArrayList<>();
			List<QuestAction.GrantReward> durable = new ArrayList<>();
			List<QuestAction> craft = new ArrayList<>();
			for (QuestAction action : actions) {
				if (action instanceof QuestAction.RemoveItem removal) {
					removals.add(removal);
				} else if (action instanceof QuestAction.GiveItem give) {
					gives.add(give);
				} else if (action instanceof QuestAction.UnequipItem unequip) {
					unequips.add(unequip);
				} else if (action instanceof QuestAction.GrantReward reward) {
					QuestRewardKind kind = reward.rewardKind();
					(kind.isCurrency() ? currency : durable).add(reward);
				} else if (action instanceof QuestAction.LearnRecipe
						|| action instanceof QuestAction.ForgetRecipe
						|| action instanceof QuestAction.GrantCraftSkill) {
					craft.add(action);
				} else if (!(action instanceof QuestAction.SetVariable)
						&& !(action instanceof QuestAction.IncrementVariable)
						&& !(action instanceof QuestAction.SetStatus)
						&& !(action instanceof QuestAction.CompleteQuest)
						&& !(action instanceof QuestAction.BlockDefaultItemUse)) {
					throw new IllegalArgumentException("unsupported quest action: " + action.getClass().getName());
				}
				// SetVariable, IncrementVariable, SetStatus, and CompleteQuest are applied by QuestStatePort.
			}
			return new Buckets(List.copyOf(removals), List.copyOf(gives), List.copyOf(unequips),
				List.copyOf(currency), List.copyOf(durable), List.copyOf(craft));
		}
	}
}
