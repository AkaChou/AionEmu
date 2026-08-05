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

	public CompositeQuestActionPort(QuestInventoryPort inventory, QuestCurrencyPort currency,
			QuestRewardPort rewards) {
		this(inventory, currency, rewards, null);
	}

	public CompositeQuestActionPort(QuestInventoryPort inventory, QuestCurrencyPort currency,
			QuestRewardPort rewards, QuestCraftPort craft) {
		this.inventory = Objects.requireNonNull(inventory, "inventory");
		this.currency = Objects.requireNonNull(currency, "currency");
		this.rewards = Objects.requireNonNull(rewards, "rewards");
		this.craft = craft;
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
			throws SQLException {
		Buckets buckets = Buckets.from(actions);
		inventory.preflight(connection, snapshot, buckets.removals(), buckets.gives());
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
			participants.add(inventory.apply(connection, snapshot, buckets.removals(), buckets.gives()));
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
			List<QuestAction.GrantReward> currencyRewards,
			List<QuestAction.GrantReward> durableRewards, List<QuestAction> craftActions) {
		private static Buckets from(List<QuestAction> actions) {
			Objects.requireNonNull(actions, "actions");
			List<QuestAction.RemoveItem> removals = new ArrayList<>();
			List<QuestAction.GiveItem> gives = new ArrayList<>();
			List<QuestAction.GrantReward> currency = new ArrayList<>();
			List<QuestAction.GrantReward> durable = new ArrayList<>();
			List<QuestAction> craft = new ArrayList<>();
			for (QuestAction action : actions) {
				if (action instanceof QuestAction.RemoveItem removal) {
					removals.add(removal);
				} else if (action instanceof QuestAction.GiveItem give) {
					gives.add(give);
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
			return new Buckets(List.copyOf(removals), List.copyOf(gives), List.copyOf(currency), List.copyOf(durable),
				List.copyOf(craft));
		}
	}
}
