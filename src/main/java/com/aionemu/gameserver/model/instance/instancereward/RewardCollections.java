package com.aionemu.gameserver.model.instance.instancereward;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;

final class RewardCollections {
	private RewardCollections() {
	}

	static <T> List<T> sortedByScoreDescending(Collection<T> rewards, final ToIntFunction<T> scoreFunction) {
		List<T> sorted = new ArrayList<T>(rewards);
		Collections.sort(sorted, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return Integer.compare(scoreFunction.applyAsInt(o2), scoreFunction.applyAsInt(o1));
			}
		});
		return sorted;
	}

	static <T extends InstancePlayerReward> int maxPoints(Collection<T> rewards) {
		int max = 0;
		for (T reward : rewards) {
			if (reward.getPoints() > max) {
				max = reward.getPoints();
			}
		}
		return max;
	}

	static <T extends InstancePlayerReward> int minPoints(Collection<T> rewards) {
		int min = 0;
		boolean initialized = false;
		for (T reward : rewards) {
			if (!initialized || reward.getPoints() < min) {
				min = reward.getPoints();
				initialized = true;
			}
		}
		return min;
	}

	static <T> int sum(Collection<T> rewards, ToIntFunction<T> scoreFunction) {
		int sum = 0;
		for (T reward : rewards) {
			sum += scoreFunction.applyAsInt(reward);
		}
		return sum;
	}
}
