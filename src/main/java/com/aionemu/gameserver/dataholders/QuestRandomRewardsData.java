package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.model.templates.quest.QuestItems;

/**
 * 任务随机奖励池数据：按池 id 索引 {@link QuestRandomReward}，{@link #draw} 按概率抽选一个物品选项。
 * Quest random-reward pools, indexed by pool id; {@link #draw} picks one item option by weighted probability.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "randomRewards" })
@XmlRootElement(name = "quest_random_rewards")
public class QuestRandomRewardsData {

	@XmlElement(name = "quest_random_reward", required = true)
	protected List<QuestRandomReward> randomRewards = new ArrayList<>();

	@XmlTransient
	private IntObjectHashMap<QuestRandomReward> pools = new IntObjectHashMap<>();

	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		pools = new IntObjectHashMap<>();
		for (QuestRandomReward pool : randomRewards) {
			if (pool == null || pool.id <= 0) {
				throw new IllegalArgumentException("quest random reward pool id must be positive");
			}
			if (pools.containsKey(pool.id)) {
				throw new IllegalArgumentException("duplicate quest random reward pool: " + pool.id);
			}
			if (pool.items == null || pool.items.isEmpty()) {
				throw new IllegalArgumentException("quest random reward pool is empty: " + pool.id);
			}
			long total = 0;
			for (QuestRandomRewardItem option : pool.items) {
				if (option == null || option.itemId <= 0 || option.count <= 0 || option.prob <= 0) {
					throw new IllegalArgumentException("invalid item option in quest random reward pool: " + pool.id);
				}
				total += option.prob;
				if (total > Integer.MAX_VALUE) {
					throw new IllegalArgumentException("quest random reward probability total is too large: " + pool.id);
				}
			}
			pool.totalProbability = (int) total;
			pools.put(pool.id, pool);
		}
	}

	public boolean containsPool(int poolId) {
		return pools.containsKey(poolId);
	}

	/**
	 * 按池内概率抽选一个物品选项；池缺失或为空时抛异常（fail closed）。
	 * Draws one weighted item option from the pool; fails closed on unknown or empty pools.
	 */
	public QuestItems draw(int poolId) {
		return draw(poolId, total -> Rnd.get(1, total));
	}

	QuestItems draw(int poolId, IntUnaryOperator roller) {
		QuestRandomReward pool = pools.get(poolId);
		if (pool == null) {
			throw new IllegalArgumentException("unknown or empty quest random reward pool: " + poolId);
		}
		int roll = Objects.requireNonNull(roller, "roller").applyAsInt(pool.totalProbability);
		if (roll < 1 || roll > pool.totalProbability) {
			throw new IllegalArgumentException("quest random reward roll is out of range: " + roll);
		}
		int acc = 0;
		for (QuestRandomRewardItem option : pool.items) {
			acc += option.prob();
			if (roll <= acc) {
				return new QuestItems(option.itemId(), option.count());
			}
		}
		throw new IllegalStateException("quest random reward pool has inconsistent probability data: " + poolId);
	}

	/** 单一随机奖励池。 Single random-reward pool. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class QuestRandomReward {

		@XmlElement(name = "id")
		protected int id;

		@XmlElement(name = "name")
		protected String name;

		@XmlElementWrapper(name = "items")
		@XmlElement(name = "data")
		protected List<QuestRandomRewardItem> items = new ArrayList<>();

		@XmlTransient
		private int totalProbability;

		public int id() {
			return id;
		}
	}

	/** 池内概率选项（真端 prob 基数 1_000_000）。 Weighted option; retail prob base is 1_000_000. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class QuestRandomRewardItem {

		@XmlElement(name = "item")
		protected int itemId;

		@XmlElement(name = "item_count")
		protected int count;

		@XmlElement(name = "prob")
		protected int prob;

		public int itemId() {
			return itemId;
		}

		public int count() {
			return count;
		}

		public int prob() {
			return prob;
		}
	}
}
