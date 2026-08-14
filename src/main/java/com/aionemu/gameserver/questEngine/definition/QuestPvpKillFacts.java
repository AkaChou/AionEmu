package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仅附着于权威运行时 PvP 击杀事件的不可变服务器事实。
 * Immutable server facts attached only to an authoritative runtime PvP kill event.
 */
public record QuestPvpKillFacts(int killerId, int recipientId, int victimId,
		int recipientLevel, int victimLevel, int victimRankId, int worldId,
		QuestPvpCreditSource creditSource, Set<String> recipientZones) {
	public QuestPvpKillFacts {
		if (killerId <= 0 || recipientId <= 0 || victimId <= 0 || killerId == victimId
				|| recipientId == victimId) {
			throw new IllegalArgumentException("PvP player ids must be positive and the victim must be distinct");
		}
		if (recipientLevel <= 0 || victimLevel <= 0) {
			throw new IllegalArgumentException("PvP player levels must be positive");
		}
		if (victimRankId < 1 || victimRankId > 18 || worldId <= 0) {
			throw new IllegalArgumentException("PvP victim rank/world facts are invalid");
		}
		creditSource = Objects.requireNonNull(creditSource, "creditSource");
		recipientZones = Set.copyOf(Objects.requireNonNull(recipientZones, "recipientZones").stream()
			.map(zone -> {
				if (zone == null || zone.isBlank()) {
					throw new IllegalArgumentException("PvP recipient zone must not be blank");
				}
				return zone.toUpperCase(java.util.Locale.ROOT);
			})
			.collect(Collectors.toSet()));
	}

	/**
	 * 击杀双方等级差是否落在给定区间内。
	 * Whether the level delta between killer and victim is within the given range.
	 */
	public boolean victimLevelDeltaBetween(int minimumRecipientDelta, int maximumRecipientDelta) {
		int delta = recipientLevel - victimLevel;
		return delta >= minimumRecipientDelta && delta <= maximumRecipientDelta;
	}

	public boolean recipientInZone(String zone) {
		return recipientZones.contains(zone.toUpperCase(java.util.Locale.ROOT));
	}
}
