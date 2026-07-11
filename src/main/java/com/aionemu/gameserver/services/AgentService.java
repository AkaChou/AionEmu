package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.AgentSchedule;
import com.aionemu.gameserver.configs.schedule.AgentSchedule.Agent;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.agent.AgentLocation;
import com.aionemu.gameserver.model.agent.AgentStateType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.agentspawns.AgentSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.agentservice.AgentFight;
import com.aionemu.gameserver.services.agentservice.AgentStartRunnable;
import com.aionemu.gameserver.services.agentservice.Fight;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 天族/魔族神代代理人（Agent / Empyrean Lord Agent）战斗活动服务。
 * Service for Empyrean Lord Agent fight events (scheduled world spawns and battle messages).
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class AgentService {
	private static volatile ObjectProvider<AgentService> instanceProvider;
	private AgentSchedule agentSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, AgentLocation> agent;
	private final ConcurrentMap<Integer, AgentFight<?>> activeFights = new ConcurrentHashMap<Integer, AgentFight<?>>();

	/**
	 * 初始化代理人活动地点：按配置加载并在和平状态刷怪。
	 * Initialize agent locations: load data and spawn peace-state NPCs when enabled.
	 */
	public void initAgentLocations() {
		if (CustomConfig.AGENT_ENABLED) {
			agent = DataManager.AGENT_DATA.getAgentLocations();
			for (AgentLocation loc : getAgentLocations().values()) {
				spawn(loc, AgentStateType.PEACE);
			}
			log.info(I18n.get("log.055cf27529a4", agent.size()));
		} else {
			log.info(I18n.get("log.976c5af9708b"));
			agent = Collections.emptyMap();
		}
	}

	/**
	 * 初始化代理人活动并装载 cron 调度。
	 * Initialize the agent event and load its cron schedule.
	 */
	public void initAgent() {
		if (CustomConfig.AGENT_ENABLED) {
			log.info(I18n.get("log.60d4830bec5f"));
		}
		reloadSchedule();
	}

	/**
	 * 重新加载代理人战斗时间表：取消旧任务并按新 cron 注册。
	 * Reload the agent fight schedule: cancel old tasks and re-register from cron.
	 */
	public synchronized void reloadSchedule() {
		AgentSchedule newSchedule = CustomConfig.AGENT_ENABLED ? AgentSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		agentSchedule = newSchedule;
		if (agentSchedule != null) {
			for (Agent agent : agentSchedule.getAgentsList()) {
				for (String fightTime : agent.getFightTimes()) {
					Runnable task = new AgentStartRunnable(agent.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, fightTime);
				}
			}
		}
	}

	/**
	 * 启动指定地点的代理人战斗，并在持续时长结束后自动停止。
	 * Start the agent fight at the given location and auto-stop after the configured duration.
	 *
	 * @param id 活动地点 ID / agent location id
	 */
	public void startAgentFight(final int id) {
		final AgentFight<?> fight = new Fight(agent.get(id));
		if (activeFights.putIfAbsent(id, fight) != null) {
			return;
		}
		fight.start();
		empyreanLordCountdownMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopAgentFight(id);
			}
		}, CustomConfig.AGENT_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定地点的代理人战斗。
	 * Stop the agent fight at the given location.
	 *
	 * @param id 活动地点 ID / agent location id
	 */
	public void stopAgentFight(int id) {
		AgentFight<?> fight = activeFights.remove(id);
		if (fight == null || fight.isFinished()) {
			return;
		}
		fight.stop();
	}

	/**
	 * 按状态刷出代理人活动相关 NPC。
	 * Spawn agent-event NPCs for the given location and state.
	 *
	 * @param loc 活动地点 / agent location
	 * spawn state
	 */
	public void spawn(AgentLocation loc, AgentStateType astate) {
		if (astate.equals(AgentStateType.FIGHT)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getAgentSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				AgentSpawnTemplate agenttemplate = (AgentSpawnTemplate) st;
				if (agenttemplate.getAStateType().equals(astate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(agenttemplate, 1));
				}
			}
		}
	}

	/**
	 * 广播神代代理人倒计时系统消息。
	 * Broadcast Empyrean Lord Agent countdown system messages.
	 *
	 * @param id 活动地点 ID / agent location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean empyreanLordCountdownMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 主神代理人将在 30 分钟后结束战斗。 / The Empyrean Lord's Agent will end the battle in 30 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_TimeAttack_Start,
							5400000);
					// 主神代理人已消失。 / The Empyrean Lord's Agent has disappeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GODELITE_TimeAttack_Fail,
							7200000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播代理人战斗 10 分钟预告。
	 * Broadcast the 10-minute agent battle warning.
	 *
	 * @param id 活动地点 ID / agent location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean agentBattleMsg1(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 代理人之战将在 10 分钟后开始。 / The Agent battle will start in 10 minutes.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_GodElite_time_01, 0);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播代理人战斗 5 分钟预告。
	 * Broadcast the 5-minute agent battle warning.
	 *
	 * @param id 活动地点 ID / agent location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean agentBattleMsg2(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 代理人之战将在 5 分钟后开始。 / The Agent battle will start in 5 minutes.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_GodElite_time_02, 0);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播提亚马特化身（Governor Sunayaka）出现消息。
	 * Broadcast Governor Sunayaka (Tiamat incarnation) appearance messages.
	 *
	 * @param id 活动地点 ID / agent location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean governorSunayakaMsg(int id) {
		switch (id) {
		case 2:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 提亚马特的化身已出现。 / Tiamat's Incarnation has appeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_TIAMATAVATAR_WAKEUP, 0);
					// 提亚马特越来越强。 / Tiamat is getting stronger and stronger.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_TIAMATDOWN_USERKICK_MESSAGE, 10000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播狂暴提亚马特化身（Berserker Sunayaka）出现消息。
	 * Broadcast Berserker Sunayaka (Tiamat incarnation) appearance messages.
	 *
	 * @param id 活动地点 ID / agent location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean berserkerSunayakaMsg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 提亚马特的化身已出现。 / Tiamat's Incarnation has appeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_TIAMATAVATAR_WAKEUP, 0);
					// 提亚马特越来越强。 / Tiamat is getting stronger and stronger.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_TIAMATDOWN_USERKICK_MESSAGE, 10000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 清除指定地点已刷出的代理人活动 NPC。
	 * Despawn agent-event NPCs at the given location.
	 *
	 * @param loc 活动地点 / agent location
	 */
	public void despawn(AgentLocation loc) {
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
	 * 判断指定地点是否正在进行代理人战斗。
	 * Whether an agent fight is in progress at the given location.
	 *
	 * @param id 活动地点 ID / agent location id
	 * @return 若 in progress 则为 true / true if in progress
	 */
	public boolean isFightInProgress(int id) {
		return activeFights.containsKey(id);
	}

	/**
	 * 返回当前活跃的代理人战斗映射。
	 * Return the map of currently active agent fights.
	 *
	 * @return 地点 ID → 战斗实例 / location id to fight instance
	 */
	public Map<Integer, AgentFight<?>> getActiveFights() {
		return activeFights;
	}

	/**
	 * 返回代理人战斗持续时长（小时，来自配置）。
	 * Return agent fight duration in hours (from config).
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.AGENT_DURATION;
	}

	/**
	 * 按 ID 获取代理人活动地点。
	 * Get an agent location by id.
	 *
	 * @param id 活动地点 ID / agent location id
	 * agent location
	 */
	public AgentLocation getAgentLocation(int id) {
		return agent.get(id);
	}

	/**
	 * 返回全部代理人活动地点。
	 * Return all agent locations.
	 *
	 * location map
	 */
	public Map<Integer, AgentLocation> getAgentLocations() {
		return agent;
	}

	/**
	 * 获取 AgentService 单例（Spring 提供者优先，否则 holder）。
	 * Return the AgentService singleton (Spring provider first, else holder).
	 *
	 * service instance
	 */
	public static AgentService getInstance() {
		ObjectProvider<AgentService> provider = instanceProvider;
		if (provider == null) {
			return AgentServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> AgentServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring ObjectProvider，供 getInstance 使用。
	 * Inject the Spring ObjectProvider used by getInstance().
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<AgentService> instanceProvider) {
		AgentService.instanceProvider = instanceProvider;
	}

	private static class AgentServiceHolder {
		private static final AgentService INSTANCE = new AgentService();
	}
}
