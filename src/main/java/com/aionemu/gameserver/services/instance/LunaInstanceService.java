package com.aionemu.gameserver.services.instance;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerInstanceLimitsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerInstanceLimit;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceAdmissionService.LunaAdmission;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

public final class LunaInstanceService {
	private static final int LIMIT_KEY_BASE = -1_000_000;
	private static final int RESULT_FAILED = 1;
	private static final int RESULT_NOT_ENOUGH_LUNA = 2;
	private static final int RESULT_LIMIT = 7;

	private LunaInstanceService() {
	}

	public static synchronized void enter(Player player, int dungeonId, boolean reentry) {
		Row dungeon = DataManager.RETAIL_INSTANCE_DATA.lunaDungeon(dungeonId);
		if (dungeon == null || !isOpen(dungeon, System.currentTimeMillis())) {
			result(player, dungeonId, RESULT_FAILED);
			return;
		}
		int worldId = dungeon.requiredInt("world_id");
		if (reentry) {
			WorldMapInstance instance = InstanceService.getRegisteredInstance(worldId, player.getObjectId());
			if (instance == null || !DynamicInstanceManager.hasJoined(instance, player.getObjectId())
					|| !teleport(player, dungeon, instance)) {
				result(player, dungeonId, RESULT_FAILED);
				return;
			}
			result(player, dungeonId, 0);
			return;
		}
		if (player.isInGroup2() || player.isInAlliance2() || player.isInLeague()) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_NEED_SOLO(dungeon.value("name")));
			result(player, dungeonId, RESULT_FAILED);
			return;
		}
		Row price = DataManager.RETAIL_INSTANCE_DATA.lunaPrice(dungeon.requiredInt("luna_price_id"));
		PlayerInstanceLimit limit = player.getInstanceLimits().getOrCreate(limitKey(price.requiredInt("id")));
		PriceStatus status;
		synchronized (limit) {
			status = status(dungeon, price, limit, player.getLevel(), System.currentTimeMillis());
		}
		if (!status.allowed()) {
			result(player, dungeonId, RESULT_LIMIT);
			return;
		}
		if (player.getLunaAccount() < status.price()) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
			result(player, dungeonId, RESULT_NOT_ENOUGH_LUNA);
			return;
		}
		WorldMapInstance previous = InstanceService.getPersonalInstance(worldId, player.getObjectId());
		if (previous != null) {
			InstanceService.destroyInstance(previous);
		}
		LunaAdmission admission = InstanceAdmissionService.admitLuna(dungeon, player, status.price());
		if (admission == null) {
			result(player, dungeonId, RESULT_FAILED);
			return;
		}
		boolean countConsumed = false;
		try {
			synchronized (limit) {
				refresh(price, limit, System.currentTimeMillis());
				limit.setUsed(limit.getUsed() + 1);
			}
			countConsumed = true;
			DAOManager.getDAO(PlayerInstanceLimitsDAO.class).store(player);
			if (!teleport(player, dungeon, admission.instance())) {
				restoreCount(player, limit);
				countConsumed = false;
				admission.rollback();
				result(player, dungeonId, RESULT_FAILED);
				return;
			}
		} catch (RuntimeException | Error e) {
			if (countConsumed) {
				restoreCount(player, limit);
			}
			admission.rollback();
			throw e;
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, price.requiredInt("id"), limit.getUsed()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0));
		result(player, dungeonId, 0);
	}

	public static void sendCounts(Player player) {
		for (Row dungeon : DataManager.RETAIL_INSTANCE_DATA.lunaDungeons()) {
			Row price = DataManager.RETAIL_INSTANCE_DATA.lunaPrice(dungeon.requiredInt("luna_price_id"));
			PlayerInstanceLimit limit = player.getInstanceLimits().getOrCreate(limitKey(price.requiredInt("id")));
			int used;
			synchronized (limit) {
				refresh(price, limit, System.currentTimeMillis());
				used = limit.getUsed();
			}
			if (used > 0) {
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, price.requiredInt("id"), used));
			}
		}
	}

	static int limitKey(int priceId) {
		return LIMIT_KEY_BASE - priceId;
	}

	static PriceStatus status(Row dungeon, Row price, PlayerInstanceLimit limit, int level, long now) {
		refresh(price, limit, now);
		int used = limit.getUsed();
		int free = price.requiredInt("free_turn");
		int paidMax = price.requiredInt("price_max_count");
		if (used >= free + paidMax) {
			return new PriceStatus(false, 0, used, limit.getResetAt());
		}
		if (used < free) {
			return new PriceStatus(true, 0, used, limit.getResetAt());
		}
		int step = used - free + 1;
		long basePrice = price.requiredInt("price%02d".formatted(step));
		int levelBand = Math.max(0, (level - 1) / 10);
		long scaledPrice = (long) (basePrice + basePrice * (dungeon.requiredInt("price_ratio") * levelBand) / 100F);
		return new PriceStatus(true, scaledPrice, used, limit.getResetAt());
	}

	static void refresh(Row price, PlayerInstanceLimit limit, long now) {
		if (limit.getResetAt() == 0) {
			limit.setResetAt(nextReset(price, now));
		} else if (limit.getResetAt() <= now) {
			limit.setUsed(0);
			limit.setBonusAvailable(0);
			limit.setPurchasedCount(0);
			limit.setPurchaseStep(0);
			limit.setResetAt(nextReset(price, now));
		}
	}

	static long nextReset(Row price, long nowMillis) {
		ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(InstanceLimitService.RETAIL_ZONE);
		int value = price.requiredInt("value");
		LocalTime time = LocalTime.of(value / 100, value % 100);
		if ("Daily".equals(price.value("reset_type"))) {
			ZonedDateTime candidate = now.with(time);
			return (candidate.isAfter(now) ? candidate : candidate.plusDays(1)).toInstant().toEpochMilli();
		}
		DayOfWeek day = switch (price.value("type_value").toLowerCase()) {
			case "mon" -> DayOfWeek.MONDAY;
			case "tue" -> DayOfWeek.TUESDAY;
			case "wed" -> DayOfWeek.WEDNESDAY;
			case "thu" -> DayOfWeek.THURSDAY;
			case "fri" -> DayOfWeek.FRIDAY;
			case "sat" -> DayOfWeek.SATURDAY;
			case "sun" -> DayOfWeek.SUNDAY;
			default -> throw new IllegalStateException("Unknown retail Luna reset day " + price.value("type_value"));
		};
		ZonedDateTime candidate = now.with(TemporalAdjusters.nextOrSame(day)).with(time);
		return (candidate.isAfter(now) ? candidate : candidate.plusWeeks(1)).toInstant().toEpochMilli();
	}

	static boolean isOpen(Row dungeon, long nowMillis) {
		ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(InstanceLimitService.RETAIL_ZONE);
		String day = now.getDayOfWeek().name().substring(0, 3).toLowerCase();
		int hour = now.getHour();
		String field = day + "_" + (hour < 12 ? "am" + hour : "pm" + (hour - 12));
		return dungeon.intValue("active", 0) == 1 && now.getMinute() < dungeon.requiredInt(field);
	}

	private static boolean teleport(Player player, Row dungeon, WorldMapInstance instance) {
		String[] point = dungeon.value("start_point").split(";")[0].split(",");
		return TeleportService2.teleportTo(player, dungeon.requiredInt("world_id"), instance.getInstanceId(),
				Float.parseFloat(point[0]), Float.parseFloat(point[1]), Float.parseFloat(point[2]),
				(byte) Math.round(Float.parseFloat(point[3])));
	}

	private static void restoreCount(Player player, PlayerInstanceLimit limit) {
		synchronized (limit) {
			if (limit.getUsed() > 0) {
				limit.setUsed(limit.getUsed() - 1);
			}
		}
		DAOManager.getDAO(PlayerInstanceLimitsDAO.class).store(player);
	}

	private static void result(Player player, int dungeonId, int result) {
		PacketSendUtility.sendPacket(player, SM_LUNA_SHOP.lunaInstanceResult(dungeonId, result));
	}

	public record PriceStatus(boolean allowed, long price, int used, long resetAt) {
	}
}
