package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.AnohaSchedule;
import com.aionemu.gameserver.configs.schedule.AnohaSchedule.Anoha;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.anoha.AnohaLocation;
import com.aionemu.gameserver.model.anoha.AnohaStateType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.anohaspawns.AnohaSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.anohaservice.AnohaStartRunnable;
import com.aionemu.gameserver.services.anohaservice.BerserkAnoha;
import com.aionemu.gameserver.services.anohaservice.DanuarHero;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 狂暴阿诺哈（Berserk Anoha）世界 Boss 活动服务。
 * Service for the Berserk Anoha world-boss event (spawn, schedule, teleport invites).
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class AnohaService {
	private static volatile ObjectProvider<AnohaService> instanceProvider;
	private AnohaSchedule anohaSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, AnohaLocation> anoha;

	// 狂暴阿诺哈 4.7 / Berserk Anoha 4.7
	private Map<Integer, VisibleObject> adventSwordEffect = new HashMap<>();

	private final ConcurrentMap<Integer, BerserkAnoha<?>> activeAnoha = new ConcurrentHashMap<Integer, BerserkAnoha<?>>();

	/**
	 * 初始化阿诺哈活动地点：按配置加载并在和平状态刷怪。
	 * Initialize Anoha locations: load data and spawn peace-state NPCs when enabled.
	 */
	public void initAnohaLocations() {
		if (CustomConfig.ANOHA_ENABLED) {
			anoha = DataManager.ANOHA_DATA.getAnohaLocations();
			for (AnohaLocation loc : getAnohaLocations().values()) {
				spawn(loc, AnohaStateType.PEACE);
			}
			log.info(I18n.get("log.70a3e3cac875", anoha.size()));
		} else {
			log.info(I18n.get("log.1175d3b2bf44"));
			anoha = Collections.emptyMap();
		}
	}

	/**
	 * 初始化阿诺哈活动并装载 cron 调度。
	 * Initialize the Anoha event and load its cron schedule.
	 */
	public void initAnoha() {
		if (CustomConfig.ANOHA_ENABLED) {
			log.info(I18n.get("log.aa7a391f4ea1"));
		}
		reloadSchedule();
	}

	/**
	 * 重新加载阿诺哈狂暴时间表：取消旧任务并按新 cron 注册。
	 * Reload the Anoha berserk schedule: cancel old tasks and re-register from cron.
	 */
	public synchronized void reloadSchedule() {
		AnohaSchedule newSchedule = CustomConfig.ANOHA_ENABLED ? AnohaSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		anohaSchedule = newSchedule;
		if (anohaSchedule != null) {
			for (Anoha anoha : anohaSchedule.getAnohasList()) {
				for (String berserkTime : anoha.getBerserkTimes()) {
					Runnable task = new AnohaStartRunnable(anoha.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, berserkTime);
				}
			}
		}
	}

	/**
	 * 启动指定地点的狂暴阿诺哈，并在持续时长结束后自动停止。
	 * Start Berserk Anoha at the given location and auto-stop after the configured duration.
	 *
	 * @param id 活动地点 ID / anoha location id
	 */
	public void startAnoha(final int id) {
		final BerserkAnoha<?> danuarhero = new DanuarHero(anoha.get(id));
		if (activeAnoha.putIfAbsent(id, danuarhero) != null) {
			return;
		}
		danuarhero.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopAnoha(id);
			}
		}, CustomConfig.ANOHA_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定地点的狂暴阿诺哈。
	 * Stop Berserk Anoha at the given location.
	 *
	 * @param id 活动地点 ID / anoha location id
	 */
	public void stopAnoha(int id) {
		BerserkAnoha<?> danuarhero = activeAnoha.remove(id);
		if (danuarhero == null || danuarhero.isFinished()) {
			return;
		}
		danuarhero.stop();
	}

	/**
	 * 按状态刷出阿诺哈活动相关 NPC。
	 * Spawn Anoha-event NPCs for the given location and state.
	 *
	 * @param loc 活动地点 / anoha location
	 * spawn state
	 */
	public void spawn(AnohaLocation loc, AnohaStateType cstate) {
		if (cstate.equals(AnohaStateType.FIGHT)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getAnohaSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				AnohaSpawnTemplate anohatemplate = (AnohaSpawnTemplate) st;
				if (anohatemplate.getCStateType().equals(cstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(anohatemplate, 1));
				}
			}
		}
	}

	/**
	 * 刷出阿诺哈降临剑光特效 NPC。
	 * Spawn the Anoha advent sword-effect NPC.
	 *
	 * @param id 活动地点 ID / anoha location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventSwordEffectSP(int id) {
		switch (id) {
		case 1:
			adventSwordEffect.put(702644, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(600090000, 702644, 791.27985f, 489.02353f, 142.90796f, (byte) 30), 1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播狂暴阿诺哈回归系统消息。
	 * Broadcast the Berserk Anoha return system message.
	 *
	 * @param id 活动地点 ID / anoha location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean berserkAnohaMsg1(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Named_Spawn_System, 0); // Berserk Anoha will return to Kaldor in 30 minutes.
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播愤怒维尔斯之守护者 5 分钟预告。
	 * Broadcast the 5-minute Enraged Wealhtheow Guardian warning.
	 *
	 * @param id 活动地点 ID / anoha location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean wealhtheowGuardianMsg1(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Anoha_01, 0); // Enraged Wealhtheow Guardian will appear in 5 minutes.
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播愤怒维尔斯之守护者 3 分钟预告。
	 * Broadcast the 3-minute Enraged Wealhtheow Guardian warning.
	 *
	 * @param id 活动地点 ID / anoha location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean wealhtheowGuardianMsg2(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Anoha_02, 0); // Enraged Wealhtheow Guardian will appear in 3 minutes.
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播愤怒维尔斯之守护者 1 分钟预告。
	 * Broadcast the 1-minute Enraged Wealhtheow Guardian warning.
	 *
	 * @param id 活动地点 ID / anoha location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean wealhtheowGuardianMsg3(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Anoha_03, 0); // Enraged Wealhtheow Guardian will appear in 1 minute.
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 向玩家发送是否前往挑战狂暴阿诺哈的确认框。
	 * Send the player a confirm dialog to teleport and fight Berserk Anoha.
	 *
	 * target player
	 */
	public void sendRequest(final Player player) {
	    if (player.getLevel() < 75) {
            return;
        }
        
        if (player == null || !player.isSpawned()) {
            return;
        }
        
        String message = "Berserk Anoha has appeared. Do you want to fight ?";
        RequestResponseHandler responseHandler = new RequestResponseHandler(player) {
            @Override
            public void acceptRequest(Creature requester, Player responder) {
                if (responder != null && responder.isOnline() && responder.getLevel() <= 75) {
                    TeleportService2.teleportTo(responder, 600090000, 813.6149f, 503.42126f, 143.75f, (byte) 72);
                }
            }
            
            @Override
            public void denyRequest(Creature requester, Player responder) {
            }
        };
        
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isSpawned() && player.getLevel() <= 75) {
                    boolean requested = player.getResponseRequester().putRequest(902247, responseHandler);
                    if (requested) {
                        PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(902247, 0, 0, message));
                    }
                }
            }
        }, 10000);
    }

	/**
	 * 清除指定地点已刷出的阿诺哈活动 NPC。
	 * Despawn Anoha-event NPCs at the given location.
	 *
	 * @param loc 活动地点 / anoha location
	 */
	public void despawn(AnohaLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<VisibleObject>(loc.getSpawned())) {
			Npc spawned = (Npc) obj;
			spawned.setDespawnDelayed(true);
			if (spawned.getAggroList().getList().isEmpty()) {
				spawned.getController().cancelTask(TaskId.RESPAWN);
				obj.getController().onDelete();
			}
		}
		loc.getSpawned().clear();
	}

	/**
	 * 判断指定地点是否正在进行狂暴阿诺哈。
	 * Whether Berserk Anoha is in progress at the given location.
	 *
	 * @param id 活动地点 ID / anoha location id
	 * @return 若 in progress 则为 true / true if in progress
	 */
	public boolean isAnohaInProgress(int id) {
		return activeAnoha.containsKey(id);
	}

	/**
	 * 返回当前活跃的阿诺哈活动映射。
	 * Return the map of currently active Anoha events.
	 *
	 * @return 地点 ID → 活动实例 / location id to event instance
	 */
	public Map<Integer, BerserkAnoha<?>> getActiveAnoha() {
		return activeAnoha;
	}

	/**
	 * 返回阿诺哈活动持续时长（小时，来自配置）。
	 * Return Anoha event duration in hours (from config).
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.ANOHA_DURATION;
	}

	/**
	 * 按 ID 获取阿诺哈活动地点。
	 * Get an Anoha location by id.
	 *
	 * @param id 活动地点 ID / anoha location id
	 * anoha location
	 */
	public AnohaLocation getAnohaLocation(int id) {
		return anoha.get(id);
	}

	/**
	 * 返回全部阿诺哈活动地点。
	 * Return all Anoha locations.
	 *
	 * location map
	 */
	public Map<Integer, AnohaLocation> getAnohaLocations() {
		return anoha;
	}

	/**
	 * 获取 AnohaService 单例（Spring 提供者优先，否则 holder）。
	 * Return the AnohaService singleton (Spring provider first, else holder).
	 *
	 * service instance
	 */
	public static AnohaService getInstance() {
		ObjectProvider<AnohaService> provider = instanceProvider;
		if (provider == null) {
			return AnohaServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> AnohaServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring ObjectProvider，供 getInstance 使用。
	 * Inject the Spring ObjectProvider used by getInstance().
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<AnohaService> instanceProvider) {
		AnohaService.instanceProvider = instanceProvider;
	}

	private static class AnohaServiceHolder {
		private static final AnohaService INSTANCE = new AnohaService();
	}
}
