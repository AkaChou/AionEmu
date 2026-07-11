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
	private static final ConcurrentLinkedQueue<VisibleObject> tomb = new ConcurrentLinkedQueue<VisibleObject>();
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
			Runnable schedule = new Runnable() {
				@Override
				public void run() {
					for (RiftEnum rift : RiftEnum.values()) {
						spawnImperialTomb(rift);
					}
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
	 * visible object
	 */
	private static void scheduleDelete(final VisibleObject visObj) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (visObj != null && visObj.isSpawned()) {
					visObj.getController().delete();
					tomb.remove(visObj);
				}
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
			worldInstance.doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.isSpawned()) {
						sendMessage(player, visObj.getObjectTemplate().getTemplateId());
					}
				}
			});
		}
	}

	/**
	 * 按 NPC 模板 ID 发送系统公告。
	 * Sends a system message based on the NPC template id.
	 *
	 * 玩家 / player
	 * NPC 模板 ID / npc template id
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
		/** 因迪亚鲁纳克（英吉森） / Indiarunark Inggison */
		Indiarunark_Inggison(831117, 210130000, 1358.8662f, 299.00287f, 588.7499f, (byte) 0),
		/** 因迪亚鲁纳克（希格尼娅） / Indiarunark Cygnea */
		Indiarunark_Cygnea(831117, 210070000, 2930.079f, 825.9626f, 569.5f, (byte) 71),
		/** 阿尔贝托（潘德蒙） / Alberto Pandaemonium */
		Alberto_Pandaemonium(831131, 120010000, 1584.4727f, 1405.4204f, 193.09547f, (byte) 0),
		/** 阿尔贝托（格尔克玛洛斯） / Alberto Gelkmaros */
		Alberto_Gelkmaros(831131, 220140000, 1794.8785f, 2914.2793f, 554.80853f, (byte) 0),
		/** 阿尔贝托（恩沙尔） / Alberto Enshar */
		Alberto_Enshar(831131, 220080000, 471.96454f, 2319.1738f, 216.45724f, (byte) 23);

		/**
		 * NPC 模板 ID。
		 * NPC template id.
		 */
		private int npc_id;

		/**
		 * 世界 ID。
		 * World id.
		 */
		private int worldId;

		/**
		 * X 坐标。
		 * X coordinate.
		 */
		private float x;

		/**
		 * Y 坐标。
		 * Y coordinate.
		 */
		private float y;

		/**
		 * Z 坐标。
		 * Z coordinate.
		 */
		private float z;

		/**
		 * 朝向。
		 * Heading.
		 */
		private byte h;

		/**
		 * 构造裂隙刷怪点。
		 * Builds a rift spawn point.
		 *
		 * NPC 模板 ID / npc template id
		 * 世界 ID / world id
		 * @param x X 坐标 / X
		 * @param y Y 坐标 / Y
		 * @param z Z 坐标 / Z
		 * 朝向 / heading
		 */
		private RiftEnum(int npc_id, int worldId, float x, float y, float z, byte heading) {
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
	 * 默认单例持有者。
	 * Default singleton holder.
	 */
	private static class SingletonHolder {
		/**
		 * 默认管理器实例。
		 * Default manager instance.
		 */
		protected static final ShugoImperialTombSpawnManager instance = new ShugoImperialTombSpawnManager();
	}

	/**
	 * 获取管理器实例（优先 Spring 提供，否则默认单例）。
	 * Returns the manager instance (Spring provider if set, else default singleton).
	 *
	 * manager
	 */
	public static ShugoImperialTombSpawnManager getInstance() {
		ObjectProvider<ShugoImperialTombSpawnManager> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
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
