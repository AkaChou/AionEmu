package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcShoutData;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npcshout.NpcShout;
import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * NPC 喊话服务，处理 IDLE 轮询喊话、事件喊话与系统消息下发。
 * NPC shout service handling IDLE poll shouts, event shouts, and system message delivery.
 *
 * @author Rolandas
 */
@Slf4j
public class NpcShoutsService {

	private static volatile ObjectProvider<NpcShoutsService> instanceProvider;

	NpcShoutData shoutsCache = DataManager.NPC_SHOUT_DATA;

	/**
	 * 初始化：为带 IDLE 喊话的 NPC 注册固定频率轮询任务。
	 * Initializes fixed-rate poll tasks for NPCs that have IDLE shouts.
	 */
	public NpcShoutsService() {
		for (Npc npc : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getNpcs()) {
			final int npcId = npc.getNpcId();
			final int worldId = npc.getSpawn().getWorldId();
			final int objectId = npc.getObjectId();

			if (!shoutsCache.hasAnyShout(worldId, npcId, ShoutEventType.IDLE)) {
				continue;
			}
			final List<NpcShout> shouts = shoutsCache.getNpcShouts(worldId, npcId, ShoutEventType.IDLE, null, 0);
			if (shouts.size() == 0) {
				continue;
			}
			int defaultPollDelay = Rnd.get(180, 360) * 1000;
			for (NpcShout shout : shouts) {
				if (shout.getPollDelay() != 0 && shout.getPollDelay() < defaultPollDelay) {
					defaultPollDelay = shout.getPollDelay();
				}
			}

			GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					AionObject npcObj = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(objectId);
					if (npcObj != null && npcObj instanceof Npc) {
						Npc npc2 = (Npc) npcObj;
						// 检查 AI 是否覆盖 / check if AI overrides
						if (!npc2.getAi2().poll(AIQuestion.CAN_SHOUT)) {
							return;
						}
						int randomShout = Rnd.get(shouts.size());
						NpcShout shout = shouts.get(randomShout);
						if (shout.getPattern() != null && !((AITemplate) npc2.getAi2())
								.onPatternShout(ShoutEventType.IDLE, shout.getPattern(), 0)) {
							return;
						}
						Iterator<Player> iter = npc2.getKnownList().getKnownPlayers().values().iterator();
						while (iter.hasNext()) {
							Player kObj = iter.next();
							if (kObj.getLifeStats().isAlreadyDead()) {
								return;
							}
							shout(npc2, kObj, shout, 0);
						}
					}
				}
			}, 0, defaultPollDelay);
		}
	}

	/**
	 * 对目标播放一组喊话（顺序或随机）。
	 * Plays a list of shouts to the target (sequential or random).
	 *
	 * shouting NPC
	 * target creature
	 * shout list
	 * delay in seconds
	 * @param isSequence 是否顺序播放 / whether sequential
	 */
	public void shout(Npc owner, Creature target, List<NpcShout> shouts, int delaySeconds, boolean isSequence) {
		if (owner == null || shouts == null) {
			return;
		}
		if (shouts.size() > 1) {
			if (isSequence) {
				int nextDelay = 5;
				for (NpcShout shout : shouts) {
					if (delaySeconds == -1) {
						shout(owner, target, shout, nextDelay);
						nextDelay += 5;
					} else {
						shout(owner, target, shout, delaySeconds);
						delaySeconds = -1;
					}
				}
			} else {
				int randomShout = Rnd.get(shouts.size());
				shout(owner, target, shouts.get(randomShout), delaySeconds);
			}
		} else if (shouts.size() == 1)
			shout(owner, target, shouts.get(0), delaySeconds);
	}

	/**
	 * 对目标播放单条喊话，并解析 username 等参数。
	 * Plays a single shout to the target, resolving params such as username.
	 *
	 * shouting NPC
	 * target creature
	 * @param shout 喊话模板 / shout template
	 * delay in seconds
	 */
	public void shout(Npc owner, Creature target, NpcShout shout, int delaySeconds) {
		if (owner == null || shout == null) {
			return;
		}
		Object param = shout.getParam();

		if (target instanceof Player) {
			Player player = (Player) target;
			if ("username".equals(param)) {
				param = player.getName();
			} else if ("userclass".equals(param)) {
				param = (240000 + player.getCommonData().getPlayerClass().getClassId()) * 2 + 1;
			} else if ("usernation".equals(param)) {
				log.warn(I18n.get("log.cc7b7027e30e"));
				return;
			} else if ("usergender".equals(param)) {
				param = (902012 + player.getCommonData().getGender().getGenderId()) * 2 + 1;
			} else if ("mainslotitem".equals(param)) {
				Item weapon = player.getEquipment().getMainHandWeapon();
				if (weapon == null) {
					return;
				}
				param = weapon.getItemTemplate().getNameId();
			} else if ("quest".equals(shout.getPattern())) {
				delaySeconds = 0;
			}
		}

		if ("target".equals(param) && target != null) {
			param = target.getObjectTemplate().getName();
		}

		owner.shout(shout, target, param, delaySeconds);
	}

	/**
	 * 向 NPC 知会范围内玩家发送系统消息（默认非喊话、颜色 25）。
	 * Sends a system message to players knowing the NPC (default non-shout, color 25).
	 *
	 * source NPC
	 * message id
	 * @param Obj 对象参数 / object parameter
	 * color
	 * @param delay 延迟毫秒 / delay ms
	 */
	public void sendMsg(Npc npc, int msg, int Obj, int color, int delay) {
		sendMsg(npc, null, msg, Obj, false, color, delay);
	}

	/**
	 * 向 NPC 知会范围内玩家发送系统消息。
	 * Sends a system message to players knowing the NPC.
	 *
	 * source NPC
	 * message id
	 * @param Obj 对象参数 / object parameter
	 * @param isShout 是否喊话样式 / whether shout style
	 * color
	 * @param delay 延迟毫秒 / delay ms
	 */
	public void sendMsg(Npc npc, int msg, int Obj, boolean isShout, int color, int delay) {
		sendMsg(npc, null, msg, Obj, isShout, color, delay);
	}

	/**
	 * 向 NPC 知会范围内玩家发送系统消息（简化参数）。
	 * Sends a system message to players knowing the NPC (simplified args).
	 *
	 * source NPC
	 * message id
	 * @param delay 延迟毫秒 / delay ms
	 */
	public void sendMsg(Npc npc, int msg, int delay) {
		sendMsg(npc, null, msg, 0, false, 25, delay);
	}

	/**
	 * 立即向 NPC 知会范围内玩家发送系统消息。
	 * Immediately sends a system message to players knowing the NPC.
	 *
	 * source NPC
	 * message id
	 */
	public void sendMsg(Npc npc, int msg) {
		sendMsg(npc, null, msg, 0, false, 25, 0);
	}

	/**
	 * 向副本内全部玩家发送系统消息。
	 * Sends a system message to all players in the map instance.
	 *
	 * map instance
	 * message id
	 * @param Obj 对象参数 / object parameter
	 * @param isShout 是否喊话样式 / whether shout style
	 * color
	 * @param delay 延迟毫秒 / delay ms
	 */
	public void sendMsg(WorldMapInstance instance, int msg, int Obj, boolean isShout, int color, int delay) {
		sendMsg(null, instance, msg, Obj, isShout, color, delay);
	}

	/**
	 * 向副本内全部玩家发送系统消息（简化参数）。
	 * Sends a system message to all players in the map instance (simplified args).
	 *
	 * map instance
	 * message id
	 * @param delay 延迟毫秒 / delay ms
	 */
	public void sendMsg(WorldMapInstance instance, int msg, int delay) {
		sendMsg(null, instance, msg, 0, false, 25, delay);
	}

	/**
	 * 延迟后向 NPC 知会玩家或副本内玩家广播系统消息。
	 * After delay, broadcasts a system message to NPC known players or instance players.
	 *
	 * @param npc 来源 NPC，可为 null / source NPC, may be null
	 * @param instance 地图实例，可为 null / map instance, may be null
	 * message id
	 * @param Obj 对象参数 / object parameter
	 * @param isShout 是否喊话样式 / whether shout style
	 * color
	 * @param delay 延迟毫秒 / delay ms
	 */
	public void sendMsg(final Npc npc, final WorldMapInstance instance, final int msg, final int Obj,
			final boolean isShout, final int color, int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (npc != null && npc.isSpawned()) {
					npc.getKnownList().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(isShout, msg, Obj, color));
						}
					});
				} else if (instance != null) {
					instance.doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(isShout, msg, Obj, color));
						}

					});
				}
			}
		}, delay);
	}

	/**
	 * 获取 NPC 喊话服务单例（优先 Spring ObjectProvider）。
	 * Returns the NPC shouts service singleton (preferring Spring ObjectProvider).
	 *
	 * service instance
	 */
	public static final NpcShoutsService getInstance() {
		ObjectProvider<NpcShoutsService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<NpcShoutsService> instanceProvider) {
		NpcShoutsService.instanceProvider = instanceProvider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final NpcShoutsService instance = new NpcShoutsService();
	}
}
