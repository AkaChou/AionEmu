package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.services.rift.RiftEnum;
import com.aionemu.gameserver.services.rift.RiftInformer;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 裂隙 / 旋涡（Rift/Vortex）控制器，处理入口确认、传送与通行人数同步。
 * Rift/Vortex controller handling entry confirmation, teleport and used-entry sync.
 */
public class RVController extends NpcController {

	/** 是否为裂隙主端（可接受通行）。 / Whether this is the master side of the rift. */
	private boolean isMaster = false;
	/** 是否为入侵旋涡。 / Whether this is an invasion vortex. */
	private boolean isVortex = false;
	/** 已通过旋涡的玩家映射。 / Map of players who have passed through the vortex. */
	protected Map<Integer, Player> passedPlayers = new LinkedHashMap<Integer, Player>();
	/** 从端（出口）生成模板。 / Slave (exit) spawn template. */
	private SpawnTemplate slaveSpawnTemplate;
	/** 从属 NPC / Slave NPC */
	private Npc slave;
	/** 最低可进入等级。 / Minimum entry level. */
	private Integer minLevel;
	/** 最高可进入等级。 / Maximum entry level. */
	private Integer maxLevel;
	/** 预计消失时间（秒级时间戳）。 / Expected despawn time as epoch seconds. */
	private int deSpawnedTime;
	/** 最大可通行人数。 / Maximum number of entries. */
	private Integer maxEntries;
	/** 消耗的欧比斯点数。 / Abyss points cost. */
	private Integer abyssPoint;
	/** 当前是否接受通行。 / Whether entries are currently accepted. */
	private boolean isAccepting;
	/** 已使用的通行次数。 / Number of used entries. */
	private int usedEntries = 0;
	/** 裂隙模板枚举。 / Rift template enum. */
	private RiftEnum riftTemplate;

	/**
	 * 根据从端 NPC 与裂隙模板构造控制器。
	 * Constructs the controller from a slave NPC and rift template.
	 *
	 * @param slave 从端 NPC，主端时非 null / slave NPC, non-null on master side
	 * @param riftTemplate 裂隙模板 / rift template
	 */
	public RVController(Npc slave, RiftEnum riftTemplate) {
		this.riftTemplate = riftTemplate;
		this.isVortex = riftTemplate.isVortex();
		this.maxEntries = riftTemplate.getEntries();
		this.abyssPoint = riftTemplate.getAbyssPoint();
		this.minLevel = riftTemplate.getMinLevel();
		this.maxLevel = riftTemplate.getMaxLevel();
		this.deSpawnedTime = ((int) (System.currentTimeMillis() / 1000))
				+ (isVortex ? GameLocationBootstrapServices.vortexService().getDuration() * 3600
						: GameLocationBootstrapServices.riftService().getDuration() * 3600);
		if (slave != null) {
			this.slave = slave;
			this.slaveSpawnTemplate = slave.getSpawn();
			isMaster = true;
			isAccepting = true;
		}
	}

	/**
	 * 处理玩家对话请求，弹出通行确认窗。
	 * Handles player dialog requests and shows the entry confirmation window.
	 *
	 * @param player 请求的玩家 / requesting player
	 */
	@Override
	public void onDialogRequest(Player player) {
		if (!isMaster && !isAccepting) {
			return;
		}
		onRequest(player);
	}

	/**
	 * 按旋涡或普通裂隙弹出确认并在同意后传送。
	 * Shows vortex or normal-rift confirmation and teleports on accept.
	 *
	 * @param player 请求的玩家 / requesting player
	 */
	private void onRequest(Player player) {
		if (isVortex) {
			RequestResponseHandler responseHandler = new RequestResponseHandler(getOwner()) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (onAccept(responder)) {
						if (responder.isInTeam()) {
							if (responder.getCurrentTeam() instanceof PlayerGroup) {
								PlayerGroupService.removePlayer(responder);
							} else {
								PlayerAllianceService.removePlayer(responder);
							}
						}
						VortexLocation loc = GameLocationBootstrapServices.vortexService().getLocationByRift(getOwner().getNpcId());
						TeleportService2.teleportTo(responder, loc.getStartPoint());
						PacketSendUtility.playerSendPacketTime(responder,
								SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_OPEN_NOTICE, 10000);
						passedPlayers.put(responder.getObjectId(), responder);
						syncPassed(true);
					}
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					onDeny(responder);
				}
			};
			boolean requested = player.getResponseRequester().putRequest(904304, responseHandler);
			if (requested) {
				PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(904304, getOwner().getObjectId(), 5));
			}
		} else {
			RequestResponseHandler responseHandler = new RequestResponseHandler(getOwner()) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (onAccept(responder)) {
						int worldId = slaveSpawnTemplate.getWorldId();
						float x = slaveSpawnTemplate.getX();
						float y = slaveSpawnTemplate.getY();
						float z = slaveSpawnTemplate.getZ();
						TeleportService2.teleportTo(responder, worldId, x, y, z);
						PacketSendUtility.playerSendPacketTime(responder,
								SM_SYSTEM_MESSAGE.STR_MSG_RVR_DIRECT_PORTAL_OPEN_NOTICE, 10000);
						syncPassed(false);
					}
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					onDeny(responder);
				}
			};
			boolean requested = player.getResponseRequester()
					.putRequest(SM_QUESTION_WINDOW.STR_ASK_PASS_BY_CHAOS_DIRECT_PORTAL, responseHandler);
			if (requested) {
				PacketSendUtility.sendPacket(player,
						new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_ASK_PASS_BY_CHAOS_DIRECT_PORTAL, 0, 0));
			}
		}
	}

	/**
	 * 校验玩家是否允许通行（等级、人数、接受状态等）。
	 * Validates whether the player is allowed to pass (level, capacity, accepting state, etc.).
	 *
	 * @param player 请求的玩家 / requesting player
	 * @return 是否允许通行 / whether passage is allowed
	 */
	private boolean onAccept(Player player) {
		if (!isAccepting) {
			return false;
		}
		if (!getOwner().isSpawned()) {
			return false;
		}
		if (player.getLevel() > getMaxLevel() || player.getLevel() < getMinLevel()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_INVADE_DIRECT_PORTAL_LEVEL_LIMIT);
			return false;
		}
		if (isVortex && getUsedEntries() >= getMaxEntries()) {
			// TODO 发送使用欧比斯点请求（尚未实现）。
			// To Do ==> sendRequestUseAp(player);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_INVADE_DIRECT_PORTAL_USE_COUNT_LIMIT);
			return false;
		}
		return true;
	}

	/**
	 * 玩家拒绝通行确认。
	 * Handles player denial of the entry confirmation.
	 *
	 * @param player 拒绝的玩家 / denying player
	 * @return 恒为 true / always true
	 */
	private boolean onDeny(Player player) {
		return true;
	}

	/**
	 * 删除时通知裂隙消失并从生成列表移除。
	 * On delete, notifies rift despawn and removes from the spawn list.
	 */
	@Override
	public void onDelete() {
		RiftInformer.sendRiftDespawn(getOwner().getWorldId(), getOwner().getObjectId());
		RiftManager.getSpawned().remove(getOwner());
		super.onDelete();
	}

	/**
	 * 是否为主端。
	 * Whether this is the master side.
	 *
	 * @return 若 master 则为 true / true if master
	 */
	public boolean isMaster() {
		return isMaster;
	}

	/**
	 * 是否为旋涡。
	 * Whether this is a vortex.
	 *
	 * @return 若 vortex 则为 true / true if vortex
	 */
	public boolean isVortex() {
		return isVortex;
	}

	/**
	 * 获取最大通行人数。
	 * Gets the maximum entry count.
	 *
	 * @return 最大通行人数 / max entries
	 */
	public Integer getMaxEntries() {
		return maxEntries;
	}

	/**
	 * 获取欧比斯点消耗。
	 * Gets the abyss point cost.
	 *
	 * @return 欧比斯点数 / abyss points
	 */
	public Integer getAbyssPoint() {
		return abyssPoint;
	}

	/**
	 * 获取最低等级限制。
	 * Gets the minimum level limit.
	 *
	 * @return 最低等级限制 / min level
	 */
	public Integer getMinLevel() {
		return minLevel;
	}

	/**
	 * 获取最高等级限制。
	 * Gets the maximum level limit.
	 *
	 * @return 最高等级限制 / max level
	 */
	public Integer getMaxLevel() {
		return maxLevel;
	}

	/**
	 * 获取裂隙模板。
	 * Gets the rift template.
	 *
	 * @return 裂隙模板 / rift template
	 */
	public RiftEnum getRiftTemplate() {
		return riftTemplate;
	}

	/**
	 * 获取从端 NPC。
	 * Gets the slave NPC.
	 *
	 * @return 从端 NPC / slave NPC
	 */
	public Npc getSlave() {
		return slave;
	}

	/**
	 * 获取已使用通行次数。
	 * Gets the used entry count.
	 *
	 * @return 已使用通行次数 / used entries
	 */
	public int getUsedEntries() {
		return usedEntries;
	}

	/**
	 * 获取剩余存活秒数。
	 * Gets remaining lifetime in seconds.
	 *
	 * @return 剩余秒数 / remaining seconds
	 */
	public int getRemainTime() {
		return deSpawnedTime - (int) (System.currentTimeMillis() / 1000);
	}

	/**
	 * 获取已通过旋涡的玩家映射。
	 * Gets the map of players who passed the vortex.
	 *
	 * @return 已通过玩家 / passed players
	 */
	public Map<Integer, Player> getPassedPlayers() {
		return passedPlayers;
	}

	/**
	 * 同步已通行人数并广播裂隙信息。
	 * Syncs used entries and broadcasts rift info.
	 *
	 * @param invasion 是否为入侵旋涡模式 / whether invasion vortex mode
	 */
	public void syncPassed(boolean invasion) {
		usedEntries = invasion ? passedPlayers.size() : ++usedEntries;
		RiftInformer.sendRiftInfo(getWorldsList(this));
	}

	/**
	 * 构建需要同步的世界 ID 列表。
	 * Builds the list of world ids that need sync.
	 *
	 * @param controller 裂隙控制器 / rift controller
	 * @return 世界 ID 数组 / world id array
	 */
	private int[] getWorldsList(RVController controller) {
		int first = controller.getOwner().getWorldId();
		if (controller.isMaster()) {
			return new int[] { first, controller.slaveSpawnTemplate.getWorldId() };
		}
		return new int[] { first };
	}
}
