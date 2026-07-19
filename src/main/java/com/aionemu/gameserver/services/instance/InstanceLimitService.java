package com.aionemu.gameserver.services.instance;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerInstanceLimit;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;

public final class InstanceLimitService {
	static final ZoneId RETAIL_ZONE = ZoneId.of("Asia/Shanghai");

	private InstanceLimitService() {
	}

	public static Row rule(int worldId) {
		return DataManager.RETAIL_INSTANCE_DATA.limit(worldId);
	}

	public static Row cooldown(Player player, int worldId) {
		Row rule = rule(worldId);
		if (rule == null) {
			return null;
		}
		boolean hasGoldPack = player.getF2p() != null && player.getF2p().getF2pAccount() != null;
		int cooldownId = rule.intValue(hasGoldPack ? "coolt_tbl_id" : "f2p_coolt_tbl_id", 0);
		return cooldownId == 0 ? null : DataManager.RETAIL_INSTANCE_DATA.cooldown(cooldownId);
	}

	public static int clientCooldownId(int worldId) {
		Row rule = rule(worldId);
		return rule == null ? 0 : rule.requiredInt("id");
	}

	public static int limitKey(int worldId) {
		Row rule = rule(worldId);
		return rule == null ? 0 : rule.intValue("coolt_sync_id", 0);
	}

	public static LimitStatus status(Player player, int worldId) {
		return status(player, worldId, System.currentTimeMillis());
	}

	static LimitStatus status(Player player, int worldId, long now) {
		Row cooldown = cooldown(player, worldId);
		int key = limitKey(worldId);
		if (cooldown == null || key == 0) {
			return new LimitStatus(key, clientCooldownId(worldId), 0, 0, 0, 0, true);
		}
		PlayerInstanceLimit limit = player.getInstanceLimits().getOrCreate(key);
		synchronized (limit) {
			refresh(limit, cooldown, now);
			int baseMax = cooldown.intValue("maxcount", 0);
			int totalMax = baseMax == 0 ? 0 : baseMax + limit.getBonusAvailable() + purchasedEntries(cooldown, limit);
			boolean allowed = totalMax == 0 || limit.getUsed() < totalMax;
			return new LimitStatus(key, clientCooldownId(worldId), totalMax, limit.getUsed(), limit.getResetAt(),
					limit.getPurchasedCount(), allowed);
		}
	}

	public static boolean consume(Player player, int worldId) {
		Row cooldown = cooldown(player, worldId);
		int key = limitKey(worldId);
		if (cooldown == null || key == 0 || cooldown.intValue("maxcount", 0) == 0) {
			return true;
		}
		PlayerInstanceLimit limit = player.getInstanceLimits().getOrCreate(key);
		synchronized (limit) {
			long now = System.currentTimeMillis();
			refresh(limit, cooldown, now);
			int totalMax = cooldown.requiredInt("maxcount") + limit.getBonusAvailable() + purchasedEntries(cooldown, limit);
			if (limit.getUsed() >= totalMax) {
				return false;
			}
			limit.setUsed(limit.getUsed() + 1);
			if (limit.getResetAt() == 0) {
				limit.setResetAt(nextReset(cooldown, now));
			}
		}
		sendUpdate(player, worldId);
		return true;
	}

	public static boolean restoreEntry(Player player, int limitKey) {
		PlayerInstanceLimit limit = player.getInstanceLimits().get(limitKey);
		if (limit == null) {
			return false;
		}
		synchronized (limit) {
			if (limit.getUsed() == 0) {
				return false;
			}
			limit.setUsed(limit.getUsed() - 1);
		}
		for (Row rule : DataManager.RETAIL_INSTANCE_DATA.limitsForSync(limitKey)) {
			sendUpdate(player, rule.requiredInt("world_id"));
		}
		return true;
	}

	public static void clear(Player player, int worldId) {
		int key = limitKey(worldId);
		if (key == 0) {
			return;
		}
		player.getInstanceLimits().remove(key);
		for (Row rule : DataManager.RETAIL_INSTANCE_DATA.limitsForSync(key)) {
			sendUpdate(player, rule.requiredInt("world_id"));
		}
	}

	public static void clearAll(Player player) {
		player.getInstanceLimits().clear();
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_INFO(player, false, null));
	}

	public static boolean purchase(Player player, int worldId) {
		Row cooldown = cooldown(player, worldId);
		int key = limitKey(worldId);
		if (cooldown == null || key == 0 || !"luna".equalsIgnoreCase(cooldown.value("component"))) {
			return false;
		}
		PlayerInstanceLimit limit = player.getInstanceLimits().getOrCreate(key);
		synchronized (limit) {
			refresh(limit, cooldown, System.currentTimeMillis());
			int maxPurchases = cooldown.intValue("pricemaxcount", 1);
			if (limit.getPurchasedCount() >= maxPurchases) {
				return false;
			}
			long cost = cooldown.intValue("component_count", 0);
			if (cost <= 0 || player.getLunaAccount() < cost) {
				return false;
			}
			player.setLunaAccount(player.getLunaAccount() - cost);
			limit.setPurchasedCount(limit.getPurchasedCount() + 1);
			limit.setPurchaseStep(limit.getPurchaseStep() + 1);
		}
		sendUpdate(player, worldId);
		return true;
	}

	static long nextReset(Row cooldown, long nowMillis) {
		String type = cooldown.value("type");
		int value = cooldown.intValue("value", 0);
		if (value > 2359 || "Relative".equalsIgnoreCase(type)) {
			return value <= 0 ? nowMillis : nowMillis + value * 60_000L;
		}
		LocalTime time = LocalTime.of(value / 100, value % 100);
		ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(RETAIL_ZONE);
		if ("Daily".equalsIgnoreCase(type)) {
			ZonedDateTime candidate = now.with(time);
			return (candidate.isAfter(now) ? candidate : candidate.plusDays(1)).toInstant().toEpochMilli();
		}
		if ("Weekly".equalsIgnoreCase(type)) {
			List<DayOfWeek> days = Arrays.stream(cooldown.value("typevalue").split(","))
					.map(InstanceLimitService::dayOfWeek).sorted(Comparator.comparingInt(DayOfWeek::getValue)).toList();
			return days.stream().map(day -> now.with(TemporalAdjusters.nextOrSame(day)).with(time))
					.filter(candidate -> candidate.isAfter(now)).min(ZonedDateTime::compareTo)
					.orElseGet(() -> now.with(TemporalAdjusters.next(days.getFirst())).with(time))
					.toInstant().toEpochMilli();
		}
		throw new IllegalStateException("Unknown retail instance cooldown type " + type);
	}

	static void refresh(PlayerInstanceLimit limit, Row cooldown, long now) {
		if (limit.getResetAt() == 0 || limit.getResetAt() > now) {
			return;
		}
		int baseMax = cooldown.intValue("maxcount", 0);
		int buildup = cooldown.intValue("extra_count_buildup", 0);
		int buildupCap = cooldown.intValue("extra_count_buildup_level", 0);
		if (baseMax > 0 && buildup > 0 && buildupCap > 0) {
			int unused = Math.max(0, baseMax - limit.getUsed());
			limit.setBonusAvailable(Math.min(buildupCap,
					limit.getBonusAvailable() + Math.min(buildup, unused)));
		}
		limit.setUsed(0);
		limit.setPurchasedCount(0);
		limit.setPurchaseStep(0);
		limit.setResetAt(nextReset(cooldown, now));
	}

	private static int purchasedEntries(Row cooldown, PlayerInstanceLimit limit) {
		return limit.getPurchasedCount() * Math.max(1, cooldown.intValue("price", 1));
	}

	private static DayOfWeek dayOfWeek(String value) {
		return switch (value) {
			case "Mon" -> DayOfWeek.MONDAY;
			case "Tue" -> DayOfWeek.TUESDAY;
			case "Wed" -> DayOfWeek.WEDNESDAY;
			case "Thu" -> DayOfWeek.THURSDAY;
			case "Fri" -> DayOfWeek.FRIDAY;
			case "Sat" -> DayOfWeek.SATURDAY;
			case "Sun" -> DayOfWeek.SUNDAY;
			default -> throw new IllegalStateException("Unknown retail weekday " + value);
		};
	}

	private static void sendUpdate(Player player, int worldId) {
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_INFO(player, worldId));
	}

	public record LimitStatus(int limitKey, int clientCooldownId, int maxEntries, int usedEntries, long resetAt,
			int purchasedCount, boolean allowed) {
		public long remainingSeconds(long now) {
			return resetAt <= now ? 0 : (resetAt - now) / 1000;
		}
	}
}
