package com.aionemu.gameserver.model.gameobjects.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_NOTIFY;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_UPDATE;

/**
 * Friend 列表。
 * Friend List game object.
 *
 * @author Ben
 */
@Slf4j
public class FriendList implements Iterable<Friend> {

	private Status status = Status.OFFLINE;
	private volatile byte friendListSent = 0;

	private final Queue<Friend> friends;

	private Player player;

	/**
	 * 为指定玩家构造空好友列表。
	 * Constructs an empty friend list for the given player.
	 */
	public FriendList(Player player) {
		this(player, new ConcurrentLinkedQueue<Friend>());
	}

	/**
	 * 为指定玩家构造含初始好友的好友列表。
	 * Constructs a friend list for the given player, with the given friends.
	 */
	public FriendList(Player owner, Collection<Friend> newFriends) {
		this.friends = new ConcurrentLinkedQueue<Friend>(newFriends);
		this.player = owner;
	}

	/**
	 * 按对象 ID 获取好友，非本玩家好友则返回 null。
	 * Gets the friend with this objId<br /> Returns null if it is not our friend
	 *
	 * @param objId 好友对象 ID / objId of friend
	 * @return Friend
	 */
	public Friend getFriend(int objId) {
		for (Friend friend : friends) {
			if (friend.getOid() == objId) {
				return friend;
			}
		}
		return null;
	}

	/**
	 * 返回列表中的好友数量。
	 * Returns number of friends in list
	 *
	 * @return 列表中的好友数量 / Num Friends in list
	 */
	public int getSize() {
		return friends.size();
	}

	/**
	 * 将给定好友添加到列表；数据库持久化见 {@code PlayerService}。
	 * Adds the given friend to the list<br /> To add a friend in the database, see <tt>PlayerService</tt>
	 *
	 * @param friend
	 */
	public void addFriend(Friend friend) {
		friends.add(friend);
	}

	/**
	 * 按名称获取好友。
	 * Gets the Friend by this name
	 *
	 * @param name 好友名称 / Name of friend
	 * @return 匹配名称的好友 / Friend matching name
	 */
	public Friend getFriend(String name) {
		for (Friend friend : friends) {
			if (friend.getName().equalsIgnoreCase(name)) {
				return friend;
			}
		}
		return null;
	}

	/**
	 * 从本玩家好友列表删除指定好友（仅影响本玩家，不影响对方）。
	 * Deletes given friend from this friends list<br /> <ul> <li>Note: This will only affect this player, not the friend.</li> <li>Note: Sends the packet to update the client automatically</li> <li>Note: You should use requestDel to delete from both lists</li> </ul>
	 */
	public void delFriend(int friendOid) {
		Iterator<Friend> it = iterator();
		while (it.hasNext()) {
			if (it.next().getOid() == friendOid) {
				it.remove();
			}
		}
	}

	/** 是否已满。 / Whether Full. */
	public boolean isFull() {
		int MAX_FRIENDS = player.havePermission(MembershipConfig.ADVANCED_FRIENDLIST_ENABLE)
				? MembershipConfig.ADVANCED_FRIENDLIST_SIZE
				: CustomConfig.FRIENDLIST_SIZE;
		return getSize() >= MAX_FRIENDS;
	}

	/**
	 * 获取 players 状态。
	 * Gets players status
	 *
	 * @return Status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * 设置玩家状态，注意不会同步给好友。
	 * Sets the status of the player<br /> <ul> <li>Note: Does not update friends</li> </ul>
	 *
	 * @param status
	 */
	public void setStatus(Status status, PlayerCommonData pcd) {
		Status previousStatus = this.status;
		this.status = status;

		// 遍历所有好友。 / For all my friends
		for (Friend friend : friends)
		{
			// 若该玩家在线。 / If the player is online
			if (friend.isOnline())
			{
				Player friendPlayer = friend.getPlayer();
				if (friendPlayer == null) {
					continue;
				}
				if (friendPlayer.getClientConnection() == null) {
					log.warn(I18n.get("log.58a385963397"));
					continue;
				}
				friendPlayer.getFriendList().getFriend(pcd.getPlayerObjId()).setPCD(pcd);
				friendPlayer.getClientConnection().sendPacket(new SM_FRIEND_UPDATE(player.getObjectId()));

				if (previousStatus == Status.OFFLINE) {
					// 显示登录消息 / Show LOGIN message
					friendPlayer.getClientConnection()
							.sendPacket(new SM_FRIEND_NOTIFY(SM_FRIEND_NOTIFY.LOGIN, player.getName()));
				} else if (status == Status.OFFLINE) {
					// 显示登出消息 / Show LOGOUT message
					friendPlayer.getClientConnection()
							.sendPacket(new SM_FRIEND_NOTIFY(SM_FRIEND_NOTIFY.LOGOUT, player.getName()));
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Friend> iterator() {
		return friends.iterator();
	}

	/** 返回 is friend list sent / Returns the is friend list sent */
	public boolean getIsFriendListSent() {
		return friendListSent == 1;
	}

	/** 设置 is friend list sent / Sets the is friend list sent */
	public void setIsFriendListSent(boolean value) {
		this.friendListSent = (byte) (value ? 1 : 0);
	}

	public enum Status {
		/**
		 * 用户离线或隐身。
		 * User is offline or invisible
		 */
		OFFLINE((byte) 0),
		/**
		 * 用户在线。
		 * User is online
		 */
		ONLINE((byte) 1),
		/**
		 * 用户离开或忙碌。
		 * User is away or busy
		 */
		AWAY((byte) 3);

		byte value;

		private Status(byte value) {
			this.value = value;
		}

		/** 返回 ID / Returns the id */
		public byte getId() {
			return value;
		}

		/**
	 * 按整数值获取状态，超出范围返回 null。
	 * Gets the Status from its int value<br /> Returns null if out of range
	 *
	 * @param value 状态值，范围 0-3 / range 0-3
	 * @return Status
	 */
		public static Status getByValue(byte value) {
			for (Status stat : values()) {
				if (stat.getId() == value) {
					return stat;
				}
			}
			return null;
		}
	}
}
