package com.aionemu.gameserver.utils;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.objects.filter.ObjectFilter;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.SiegeZoneInstance;

/**
 * 数据包发送工具：向玩家发送聊天消息与服务端包，并支持范围/军团/区域广播。
 * Packet send utility: chat messages and server packets to players, with range/legion/zone broadcast helpers.
 */
public class PacketSendUtility {

	/**
	 * 向玩家发送金色（GOLDEN_YELLOW）系统消息。
	 * Send a golden-yellow system message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendMessage(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.GOLDEN_YELLOW));
	}

	/**
	 * 向玩家发送白色消息。
	 * Send a white message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendWhiteMessage(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.WHITE));
	}

	/**
	 * 向玩家发送居中白色消息。
	 * Send a white center message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendWhiteMessageOnCenter(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.WHITE_CENTER));
	}

	/**
	 * 向玩家发送黄色消息。
	 * Send a yellow message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendYellowMessage(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.YELLOW));
	}

	/**
	 * 向玩家发送居中黄色消息。
	 * Send a yellow center message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendYellowMessageOnCenter(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.YELLOW_CENTER));
	}

	/**
	 * 向玩家发送亮黄消息。
	 * Send a bright-yellow message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendBrightYellowMessage(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.BRIGHT_YELLOW));
	}

	/**
	 * 向玩家发送居中亮黄消息。
	 * Send a bright-yellow center message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendBrightYellowMessageOnCenter(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.BRIGHT_YELLOW_CENTER));
	}

	/**
	 * 以小队队长频道样式发送系统消息。
	 * Send a system message styled as group-leader chat.
	 *
	 * Target player
	 * @param sender 发送者显示名 / Sender display name
	 * @param msg 消息内容 / Message text
	 */
	public static void sendSys1Message(Player player, String sender, String msg) {
		sendPacket(player, new SM_MESSAGE(0, sender, msg, ChatType.GROUP_LEADER));
	}

	/**
	 * 以白色系统样式发送消息（sender 未使用）。
	 * Send a white system-style message (sender unused).
	 *
	 * Target player
	 * @param sender 发送者（未使用） / Sender (unused)
	 * @param msg 消息内容 / Message text
	 */
	public static void sendSys2Message(Player player, String sender, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.WHITE));
	}

	/**
	 * 以命令频道样式发送系统消息。
	 * Send a system message styled as command chat.
	 *
	 * Target player
	 * @param sender 发送者显示名 / Sender display name
	 * @param msg 消息内容 / Message text
	 */
	public static void sendSys3Message(Player player, String sender, String msg) {
		sendPacket(player, new SM_MESSAGE(0, sender, msg, ChatType.COMMAND));
	}

	/**
	 * 以军团频道样式发送系统消息。
	 * Send a system message styled as legion chat.
	 *
	 * Target player
	 * @param sender 发送者显示名 / Sender display name
	 * @param msg 消息内容 / Message text
	 */
	public static void sendSys4Message(Player player, String sender, String msg) {
		sendPacket(player, new SM_MESSAGE(0, sender, msg, ChatType.LEGION));
	}

	/**
	 * 以联军频道样式发送系统消息。
	 * Send a system message styled as coalition chat.
	 *
	 * Target player
	 * @param sender 发送者显示名 / Sender display name
	 * @param msg 消息内容 / Message text
	 */
	public static void sendSys5Message(Player player, String sender, String msg) {
		sendPacket(player, new SM_MESSAGE(0, sender, msg, ChatType.COALITION));
	}

	/**
	 * 以联盟频道样式发送系统消息。
	 * Send a system message styled as league chat.
	 *
	 * Target player
	 * @param sender 发送者显示名 / Sender display name
	 * @param msg 消息内容 / Message text
	 */
	public static void sendSys6Message(Player player, String sender, String msg) {
		sendPacket(player, new SM_MESSAGE(0, sender, msg, ChatType.LEAGUE));
	}

	/**
	 * 向玩家发送居中警告消息（联盟警报样式）。
	 * Send a center warning message (league-alert style).
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void sendWarnMessageOnCenter(Player player, String msg) {
		sendPacket(player, new SM_MESSAGE(0, null, msg, ChatType.LEAGUE_ALERT));
	}

	/**
	 * 向玩家发送服务端数据包（连接存在时）。
	 * Send a server packet to a player when the connection exists.
	 *
	 * Target player
	 * Server packet
	 */
	public static void sendPacket(Player player, AionServerPacket packet) {
		if (player.getClientConnection() != null) {
			player.getClientConnection().sendPacket(packet);
		}
	}

	/**
	 * 延迟向玩家发送数据包。
	 * Schedule a delayed packet send to a player.
	 *
	 * Target player
	 * Server packet
	 * @param time 延迟毫秒 / Delay in milliseconds
	 */
	public static void playerSendPacketTime(final Player player, final AionServerPacket packet, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (player.getClientConnection() != null) {
					player.getClientConnection().sendPacket(packet);
				}
			}
		}, time);
	}

	/**
	 * 延迟向 NPC 已知列表中的在线玩家广播数据包。
	 * Schedule a delayed packet broadcast to online players in an NPC known-list.
	 *
	 * Source NPC
	 * Server packet
	 * @param time 延迟毫秒 / Delay in milliseconds
	 */
	public static void npcSendPacketTime(final Npc npc, final AionServerPacket packet, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				npc.getKnownList().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						if (player.isOnline()) {
							sendPacket(player, packet);
						}
					}
				});
			}
		}, time);
	}

	/**
	 * 延迟向全服在线玩家发送金色消息。
	 * Schedule a delayed golden message to all online players.
	 *
	 * @param player 触发上下文玩家（未用于过滤） / Context player (unused for filtering)
	 * Message text
	 * @param time 延迟毫秒 / Delay in milliseconds
	 */
	public static void sendMessageTime(final Player player, final String message, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						if (player.isOnline()) {
							sendMessage(player, message);
						}
					}
				});
			}
		}, time);
	}

	/**
	 * 向玩家已知列表广播包，可选同时发给自己。
	 * Broadcast a packet to a player's known-list, optionally including self.
	 *
	 * Source player
	 * Server packet
	 * @param toSelf 是否发给自己 / Whether to send to self
	 */
	public static void broadcastPacket(Player player, AionServerPacket packet, boolean toSelf) {
		if (toSelf) {
			sendPacket(player, packet);
		}
		broadcastPacket(player, packet);
	}

	/**
	 * 向可见对象已知列表广播包，若自身是玩家则同时接收。
	 * Broadcast to a visible object's known-list and also deliver to self if it is a player.
	 *
	 * @param visibleObject 源可见对象 / Source visible object
	 * Server packet
	 */
	public static void broadcastPacketAndReceive(VisibleObject visibleObject, AionServerPacket packet) {
		if (visibleObject instanceof Player) {
			sendPacket((Player) visibleObject, packet);
		}
		broadcastPacket(visibleObject, packet);
	}

	/**
	 * 向可见对象已知列表中的在线玩家广播包。
	 * Broadcast a packet to online players in a visible object's known-list.
	 *
	 * @param visibleObject 源可见对象 / Source visible object
	 * Server packet
	 */
	public static void broadcastPacket(VisibleObject visibleObject, final AionServerPacket packet) {
		visibleObject.getKnownList().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					sendPacket(player, packet);
				}
			}
		});
	}

	/**
	 * 向可见对象已知列表中通过过滤器的在线玩家广播包。
	 * Broadcast a packet to online known-list players accepted by a filter.
	 *
	 * @param visibleObject 源可见对象 / Source visible object
	 * Server packet
	 * @param filter 玩家过滤器 / Player filter
	 */
	public static void broadcastPacket(VisibleObject visibleObject, AionServerPacket packet, ObjectFilter<Player> filter) {
		visibleObject.getKnownList().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline() && filter.acceptObject(player)) {
					sendPacket(player, packet);
				}
			}
		});
	}

	/**
	 * 向玩家已知列表中通过过滤器的对象广播包，可选发给自己。
	 * Broadcast to known-list players accepted by a filter, optionally including self.
	 *
	 * Source player
	 * Server packet
	 * @param toSelf 是否发给自己 / Whether to send to self
	 * @param filter 玩家过滤器 / Player filter
	 */
	public static void broadcastPacket(Player player, final AionServerPacket packet, boolean toSelf,
			final ObjectFilter<Player> filter) {
		if (toSelf) {
			sendPacket(player, packet);
		}
		player.getKnownList().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player object) {
				if (filter.acceptObject(object)) {
					sendPacket(object, packet);
				}
			}
		});
	}

	/**
	 * 向 3D 距离内的已知玩家广播包。
	 * Broadcast a packet to known players within a 3D distance.
	 *
	 * @param visibleObject 源可见对象 / Source visible object
	 * Server packet
	 * 3D distance
	 */
	public static void broadcastPacket(final VisibleObject visibleObject, final AionServerPacket packet,
			final int distance) {
		visibleObject.getKnownList().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player p) {
				if (MathUtil.isIn3dRange(visibleObject, p, distance)) {
					sendPacket(p, packet);
				}
			}
		});
	}

	/**
	 * 向全服通过过滤器的玩家广播包。
	 * Broadcast a packet to all world players accepted by a filter.
	 *
	 * Server packet
	 * @param filter 玩家过滤器 / Player filter
	 */
	public static void broadcastFilteredPacket(final AionServerPacket packet, final ObjectFilter<Player> filter) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player object) {
				if (filter.acceptObject(object)) {
					sendPacket(object, packet);
				}
			}
		});
	}

	/**
	 * 向军团所有在线成员广播包。
	 * Broadcast a packet to all online legion members.
	 *
	 * Legion
	 * Server packet
	 */
	public static void broadcastPacketToLegion(Legion legion, AionServerPacket packet) {
		for (Player onlineLegionMember : legion.getOnlineLegionMembers()) {
			sendPacket(onlineLegionMember, packet);
		}
	}

	/**
	 * 向军团在线成员广播包，排除指定玩家。
	 * Broadcast a packet to online legion members excluding one player.
	 *
	 * Legion
	 * Server packet
	 * @param playerObjId 排除的玩家对象 ID / Excluded player object id
	 */
	public static void broadcastPacketToLegion(Legion legion, AionServerPacket packet, int playerObjId) {
		for (Player onlineLegionMember : legion.getOnlineLegionMembers()) {
			if (onlineLegionMember.getObjectId() != playerObjId) {
				sendPacket(onlineLegionMember, packet);
			}
		}
	}

	/**
	 * 向攻城区域内所有玩家广播包。
	 * Broadcast a packet to all players in a siege zone.
	 *
	 * @param zone 攻城区域 / Siege zone
	 * Server packet
	 */
	public static void broadcastPacketToZone(SiegeZoneInstance zone, final AionServerPacket packet) {
		zone.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				sendPacket(player, packet);
			}
		});
	}
}
