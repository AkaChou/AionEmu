package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.definition.QuestPvpKillFacts;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.zone.ZoneInstance;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Builds immutable PvP facts from the actual server kill fanout.
 *
 * <p>The caller must be the authoritative {@code PvpService} fanout. The port
 * rechecks the safety boundary so a direct or stale invocation cannot create a
 * typed quest event for a same-race, dead, out-of-range, or wrong-world target.</p>
 */
public final class PlayerQuestPvpEventPort implements QuestPvpEventPort {
	private final BiPredicate<Player, Player> rangeCheck;
	private final Function<Player, Set<String>> zoneResolver;

	public PlayerQuestPvpEventPort() {
		this((recipient, victim) -> MathUtil.isIn3dRange(recipient, victim, GroupConfig.GROUP_MAX_DISTANCE),
			PlayerQuestPvpEventPort::captureZones);
	}

	PlayerQuestPvpEventPort(BiPredicate<Player, Player> rangeCheck,
		Function<Player, Set<String>> zoneResolver) {
		this.rangeCheck = Objects.requireNonNull(rangeCheck, "rangeCheck");
		this.zoneResolver = Objects.requireNonNull(zoneResolver, "zoneResolver");
	}

	@Override
	public QuestEvent.KillRanked killRanked(QuestEnv env, Player killer, int victimRankId,
		QuestPvpCreditSource creditSource) {
		QuestPvpKillFacts facts = facts(env, killer, victimRankId, requireVictim(env).getWorldId(), creditSource);
		return new QuestEvent.KillRanked(victimRankId, facts);
	}

	@Override
	public QuestEvent.KillInWorld killInWorld(QuestEnv env, Player killer, int victimRankId, int worldId,
		QuestPvpCreditSource creditSource) {
		QuestPvpKillFacts facts = facts(env, killer, victimRankId, worldId, creditSource);
		return new QuestEvent.KillInWorld(worldId, facts);
	}

	private QuestPvpKillFacts facts(QuestEnv env, Player killer, int victimRankId, int worldId,
		QuestPvpCreditSource creditSource) {
		Player recipient = requireRecipient(env);
		Player victim = requireVictim(env);
		if (killer == null || creditSource == null) {
			throw new IllegalArgumentException("PvP killer and credit source are required");
		}
		if (killer.getRace() == victim.getRace() || recipient.getRace() != killer.getRace()) {
			throw new IllegalArgumentException("PvP credit requires an enemy-race recipient");
		}
		if (recipient.getLifeStats() == null || recipient.getLifeStats().isAlreadyDead()) {
			throw new IllegalArgumentException("dead PvP recipients cannot receive quest credit");
		}
		if (victim.getWorldId() != worldId) {
			throw new IllegalArgumentException("PvP world fact does not match the victim");
		}
		if (!creditSelected(killer, recipient, creditSource)) {
			throw new IllegalArgumentException("PvP recipient is outside the declared credit source");
		}
		if (!rangeCheck.test(recipient, victim)) {
			throw new IllegalArgumentException("PvP recipient is outside the quest credit range");
		}
		Set<String> zones = zoneResolver.apply(recipient);
		if (zones == null) {
			throw new IllegalStateException("PvP recipient zone facts are unavailable");
		}
		return new QuestPvpKillFacts(killer.getObjectId(), recipient.getObjectId(), victim.getObjectId(),
			recipient.getLevel(), victim.getLevel(), victimRankId, worldId, creditSource, zones);
	}

	private static boolean creditSelected(Player killer, Player recipient, QuestPvpCreditSource source) {
		return switch (source) {
			case SOLO -> killer.getObjectId().equals(recipient.getObjectId());
			case GROUP -> killer.isInGroup2() && killer.getPlayerGroup2() != null
				&& killer.getPlayerGroup2().getOnlineMembers() != null
				&& killer.getPlayerGroup2().getOnlineMembers().contains(recipient);
			case ALLIANCE -> killer.isInAlliance2() && killer.getPlayerAllianceGroup2() != null
				&& killer.getPlayerAllianceGroup2().getOnlineMembers() != null
				&& killer.getPlayerAllianceGroup2().getOnlineMembers().contains(recipient);
		};
	}

	private static Player requireRecipient(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			throw new IllegalArgumentException("PvP quest recipient is unavailable");
		}
		return env.getPlayer();
	}

	private static Player requireVictim(QuestEnv env) {
		if (env == null || !(env.getVisibleObject() instanceof Player victim)) {
			throw new IllegalArgumentException("PvP quest victim is not a player");
		}
		return victim;
	}

	private static Set<String> captureZones(Player player) {
		if (player.getPosition() == null || !player.isSpawned() || player.getPosition().getMapRegion() == null) {
			throw new IllegalStateException("PvP recipient zone facts are unavailable");
		}
		Set<String> zones = new LinkedHashSet<>();
		for (ZoneInstance zone : player.getPosition().getMapRegion().getZones(player)) {
			if (zone != null && zone.getZoneTemplate() != null && zone.getZoneTemplate().getName() != null) {
				zones.add(zone.getZoneTemplate().getName().name());
			}
		}
		return Set.copyOf(zones);
	}
}
