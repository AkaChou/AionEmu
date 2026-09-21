package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 术古皇陵活动刷怪管理器：按 cron 定时开启裂隙并公告、定时回收。
 * Shugo Imperial Tomb event spawn manager: cron-spawns rifts, announces, and schedules despawn.
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class ShugoImperialTombSpawnManager {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<ShugoImperialTombSpawnManager> instanceProvider;

	/**
	 * 当前存活的皇陵裂隙对象队列。
	 * Queue of currently alive imperial tomb rift objects.
	 */
	private static final ConcurrentLinkedQueue<VisibleObject> tomb = new ConcurrentLinkedQueue<>();
	private final List<Runnable> schedules = new ArrayList<>();

	/**
	 * 按配置的 cron 表达式启动皇陵刷怪调度。
	 * Starts imperial tomb spawn schedules from configured cron expressions.
	 */
	public synchronized void start() {
		for (Runnable schedule : schedules) {
			GameCronServices.cronService().cancel(schedule);
		}
		schedules.clear();
		if (!EventsConfig.IMPERIAL_TOMB_ENABLE) {
			return;
		}
		String[] times = EventsConfig.IMPERIAL_TOMB_TIMES.split("\\|");
		for (String cron : times) {
			Runnable schedule = () -> {
				for (RiftEnum rift : RiftEnum.values()) {
					spawnImperialTomb(rift);
				}
			};
			schedules.add(schedule);
			GameCronServices.cronService().schedule(schedule, cron);
			log.info(I18n.get("log.d50384b4d1f3", cron, EventsConfig.IMPERIAL_TOMB_TIMER));
		}
	}

	/**
	 * 刷出一个皇陵裂隙并登记定时删除与公告。
	 * Spawns one imperial tomb rift, schedules delete and announces.
	 *
	 * @param rift 裂隙定义 / rift definition
	 */
	private static void spawnImperialTomb(RiftEnum rift) {
		SpawnTemplate spawn = SpawnEngine.addNewSpawn(rift.getWorldId(), rift.getNpcId(), rift.getX(), rift.getY(),
				rift.getZ(), (byte) 0, 0);
		VisibleObject visibleObject = SpawnEngine.spawnObject(spawn, 1);
		tomb.add(visibleObject);
		scheduleDelete(visibleObject);
		sendAnnounce(visibleObject);
	}

	/**
	 * 在配置时长后删除裂隙对象。
	 * Schedules deletion of the rift object after the configured duration.
	 *
	 * @param visObj 裂隙可见对象 / the visible rift object
	 */
	private static void scheduleDelete(final VisibleObject visObj) {
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (visObj != null && visObj.isSpawned()) {
				visObj.getController().delete();
				tomb.remove(visObj);
			}
		}, EventsConfig.IMPERIAL_TOMB_TIMER * 60 * 1000);
	}

	/**
	 * 向玩家推送同地图上当前皇陵状态消息。
	 * Sends imperial tomb status messages for rifts on the player's map.
	 *
	 * @param activePlayer 玩家 / player
	 */
	public static void sendImperialStatus(Player activePlayer) {
		for (VisibleObject visObj : tomb) {
			if (visObj.getWorldId() == activePlayer.getWorldId()) {
				sendMessage(activePlayer, visObj.getObjectTemplate().getTemplateId());
			}
		}
	}

	/**
	 * 向裂隙所在地图的所有在线玩家公告开启。
	 * Announces rift open to all online players on the rift's map.
	 *
	 * @param visObj 裂隙可见对象 / rift visible object
	 */
	public static void sendAnnounce(final VisibleObject visObj) {
		if (visObj.isSpawned()) {
			WorldMapInstance worldInstance = visObj.getPosition().getMapRegion().getParent();
			worldInstance.doOnAllPlayers(player -> {
				if (player.isSpawned()) {
					sendMessage(player, visObj.getObjectTemplate().getTemplateId());
				}
			});
		}
	}

	/**
	 * 按 NPC 模板 ID 发送系统公告。
	 * Sends a system message based on the NPC template id.
	 *
	 * @param player 玩家 / the player
	 * @param npc_id NPC 模板 ID / the npc template id
	 */
	public static void sendMessage(Player player, int npc_id) {
		switch (npc_id) {
		case 831117:
			PacketSendUtility.sendSys3Message(player, "\uE09B", "<Shugo Imperial Tomb> is now open !!!");
			break;
		}
	}

	/**
	 * 皇陵裂隙刷怪点枚举（4.3/4.8）。
	 * Imperial tomb rift spawn points (4.3/4.8).
	 */
	public enum RiftEnum {
		/** 因迪亚鲁纳克（圣所） / Indiarunark Sanctum */
		Indiarunark_Sanctum(831117, 110010000, 1454.038f, 1520.621f, 573.0719f, (byte) 60),
		/** 因迪亚鲁纳克（英吉斯温） / Indiarunark Inggison */
		Indiarunark_Inggison(831117, 210050000, 1358.8662f, 299.00287f, 588.7499f, (byte) 0),
		/** 因迪亚鲁纳克（希哥尼亚） / Indiarunark Cygnea */
		Indiarunark_Cygnea(831117, 210070000, 2930.079f, 825.9626f, 569.5f, (byte) 71),
		/** 阿尔贝托（伏魔殿） / Alberto Pandaemonium */
		Alberto_Pandaemonium(831131, 120010000, 1584.4727f, 1405.4204f, 193.09547f, (byte) 0),
		/** 阿尔贝托（格尔克马洛斯） / Alberto Gelkmaros */
		Alberto_Gelkmaros(831131, 220140000, 1794.8785f, 2914.2793f, 554.80853f, (byte) 0),
		/** 阿尔贝托（厄夏勒） / Alberto Enshar */
		Alberto_Enshar(831131, 220080000, 471.96454f, 2319.1738f, 216.45724f, (byte) 23);

		/**
		 * NPC 模板 ID。
		 * NPC template id.
		 */
		private final int npc_id;

		/**
		 * 世界 ID。
		 * World id.
		 */
		private final int worldId;

		/**
		 * X 坐标。
		 * X coordinate.
		 */
		private final float x;

		/**
		 * Y 坐标。
		 * Y coordinate.
		 */
		private final float y;

		/**
		 * Z 坐标。
		 * Z coordinate.
		 */
		private final float z;

		/**
		 * 朝向。
		 * Heading.
		 */
		private final byte h;

		/**
		 * 构造裂隙刷怪点。
		 * Builds a rift spawn point.
		 *
		 * @param npc_id NPC 模板 ID / npc template id
		 * @param worldId 世界 ID / world id
		 * @param x X 坐标 / X
		 * @param y Y 坐标 / Y
		 * @param z Z 坐标 / Z
		 * @param heading 朝向 / heading
		 */
		RiftEnum(int npc_id, int worldId, float x, float y, float z, byte heading) {
			this.npc_id = npc_id;
			this.worldId = worldId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.h = heading;
		}

		/**
		 * NPC 模板 ID / npc template id
		 */
		public int getNpcId() {
			return npc_id;
		}

		/**
		 * 世界 ID / world id
		 */
		public int getWorldId() {
			return worldId;
		}

		/**
		 * X 坐标 / X coordinate
		 */
		public float getX() {
			return x;
		}

		/**
		 * Y 坐标 / Y coordinate
		 */
		public float getY() {
			return y;
		}

		/**
		 * Z 坐标 / Z coordinate
		 */
		public float getZ() {
			return z;
		}

		/**
		 * 朝向 / heading
		 */
		public byte getHeading() {
			return h;
		}
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static ShugoImperialTombSpawnManager getInstance() {
		ObjectProvider<ShugoImperialTombSpawnManager> provider = instanceProvider;
		ShugoImperialTombSpawnManager provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("ShugoImperialTombSpawnManager 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<ShugoImperialTombSpawnManager> instanceProvider) {
		ShugoImperialTombSpawnManager.instanceProvider = instanceProvider;
	}
}
