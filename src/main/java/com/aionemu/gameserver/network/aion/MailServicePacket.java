package com.aionemu.gameserver.network.aion;

import lombok.extern.slf4j.Slf4j;
import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;

/**
 * 邮件相关服务端包的写入辅助基类。
 * Base helper for writing mail-related server packets.
 *
 * @rework Ranastic
 */
@Slf4j
public abstract class MailServicePacket extends AionServerPacket {
	/** 所属玩家 / owning player */
	protected Player player;

	/**
	 * 所属玩家 / owning player
	 */
	public MailServicePacket(Player player) {
		this.player = player;
	}

	/**
	 * 写入邮件列表。
	 * Writes a letter list.
	 *
	 * letter collection
	 * 玩家 / player
	 * @param isPostman 是否邮差快递视图 / whether postman/express view
	 * @param showCount 邮差视图展示数量 / postman view show count
	 */
	protected void writeLettersList(Collection<Letter> letters, Player player, boolean isPostman, int showCount) {
		writeD(player.getObjectId());
		writeC(0);
		writeH(isPostman ? -showCount : -letters.size());
		for (Letter letter : letters) {
			if (isPostman) {
				if (!letter.isExpress()) {
					continue;
				} else if (!letter.isUnread()) {
					continue;
				}
			}
			writeD(letter.getObjectId());
			writeS(letter.getSenderName());
			writeS(letter.getTitle());
			writeC(letter.isUnread() ? 0 : 1);
			if (letter.getAttachedItem() != null) {
				writeD(letter.getAttachedItem().getObjectId());
				writeD(letter.getAttachedItem().getItemTemplate().getTemplateId());
			} else {
				writeD(0);
				writeD(0);
			}
			writeQ(letter.getAttachedKinah());
			writeQ(letter.getAttachedAp());
			writeC(letter.getLetterType().getId());
		}
	}

	/**
	 * 写入邮件操作结果消息 ID。
	 * Writes a mail operation message id.
	 *
	 * message id
	 */
	protected void writeMailMessage(int messageId) {
		writeC(messageId);
	}

	/**
	 * 写入邮箱状态计数。
	 * Writes mailbox state counters.
	 *
	 * total count
	 * unread count
	 * express count
	 * black cloud count
	 */
	protected void writeMailboxState(int totalCount, int unreadCount, int expressCount, int blackCloudCount) {
		writeH(totalCount);
		writeH(unreadCount);
		writeH(expressCount);
		writeH(blackCloudCount);
	}

	/**
	 * 写入单封已读信件详情。
	 * Writes a single letter read detail.
	 *
	 * letter
	 * @param time 时间戳（毫秒） / timestamp in ms
	 * total count
	 * unread count
	 * express count
	 * black cloud count
	 */
	protected void writeLetterRead(Letter letter, long time, int totalCount, int unreadCount, int expressCount,
			int blackCloudCount) {
		writeD(letter.getRecipientId());
		writeD(totalCount + unreadCount * 0x10000);
		writeD(expressCount + blackCloudCount);
		writeD(letter.getObjectId());
		writeD(letter.getRecipientId());
		writeS(letter.getSenderName());
		writeS(letter.getTitle());
		writeS(letter.getMessage());
		Item item = letter.getAttachedItem();
		if (item != null) {
			ItemTemplate itemTemplate = item.getItemTemplate();
			writeD(item.getObjectId());
			writeD(itemTemplate.getTemplateId());
			writeD(1);
			writeD(0);
			writeNameId(itemTemplate.getNameId());
			ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
			itemInfoBlob.writeMe(getBuf());
		} else {
			writeQ(0);
			writeQ(0);
			writeD(0);
		}
		writeQ((int) letter.getAttachedKinah());
		writeQ((int) letter.getAttachedAp());
		writeC(0);
		writeD((int) (time / 1000));
		writeC(letter.getLetterType().getId());
	}

	/**
	 * 写入信件状态（附件类型等）。
	 * Writes letter state (attachment type, etc.).
	 *
	 * letter id
	 * attachment type
	 */
	protected void writeLetterState(int letterId, int attachmentType) {
		writeD(letterId);
		writeC(attachmentType);
		writeC(1);
	}

	/**
	 * 写入删除信件结果。
	 * Writes letter delete result.
	 *
	 * total count
	 * unread count
	 * express count
	 * black cloud count
	 * deleted letter ids
	 */
	protected void writeLetterDelete(int totalCount, int unreadCount, int expressCount, int blackCloudCount,
			int... letterIds) {
		writeD(totalCount + unreadCount * 0x10000);
		writeD(expressCount + blackCloudCount);
		writeH(letterIds.length);
		for (int letterId : letterIds) {
			writeD(letterId);
		}
	}
}
