package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.mail.MailMessage;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.MailServicePacket;
import com.aionemu.gameserver.utils.collections.ListSplitter;

/**
 * 邮件服务相关操作的服务端包（邮箱状态、列表、阅读、附件、删除等）。
 * Server packet for mail-service operations (mailbox state, list, read, attachment, delete, etc.).
 */
public class SM_MAIL_SERVICE extends MailServicePacket {
	private int serviceId;
	private Collection<Letter> letters;
	private int totalCount;
	private int unreadCount;
	private int unreadExpressCount;
	private int unreadBlackCloudCount;
	private int mailMessage;
	private Letter letter;
	private long time;
	private int letterId;
	private int[] letterIds;
	private int attachmentType;
	private boolean isExpress;

	/**
	 * 同步邮箱总体状态（serviceId = 0）。
	 * Syncs overall mailbox state (serviceId = 0).
	 *
	 * @param mailbox 邮箱（触发状态刷新） / mailbox (triggers state refresh)
	 */
	public SM_MAIL_SERVICE(Mailbox mailbox) {
		super(null);
		this.serviceId = 0;
	}

	/**
	 * 返回发信/邮件操作消息（serviceId = 1）。
	 * Returns send-mail/operation message (serviceId = 1).
	 *
	 * @param mailMessage 邮件消息结果 / mail message result
	 */
	public SM_MAIL_SERVICE(MailMessage mailMessage) {
		super(null);
		this.serviceId = 1;
		this.mailMessage = mailMessage.getId();
	}

	/**
	 * 下发邮件列表（serviceId = 2）。
	 * Delivers the letter list (serviceId = 2).
	 *
	 * target player
	 * letter collection
	 */
	public SM_MAIL_SERVICE(Player player, Collection<Letter> letters) {
		super(player);
		this.serviceId = 2;
		this.letters = letters;
	}

	/**
	 * 下发邮件列表，可标记是否快递（serviceId = 2）。
	 * Delivers the letter list, optionally marked as express (serviceId = 2).
	 *
	 * target player
	 * letter collection
	 * @param isExpress 是否快递邮件模式 / whether express-mail mode
	 */
	public SM_MAIL_SERVICE(Player player, Collection<Letter> letters, boolean isExpress) {
		super(player);
		this.serviceId = 2;
		this.letters = letters;
		this.isExpress = isExpress;
	}

	/**
	 * 打开/阅读单封信件（serviceId = 3）。
	 * Opens/reads a single letter (serviceId = 3).
	 *
	 * target player
	 * letter
	 * timestamp
	 */
	public SM_MAIL_SERVICE(Player player, Letter letter, long time) {
		super(player);
		this.serviceId = 3;
		this.letter = letter;
		this.time = time;
	}

	/**
	 * 更新信件附件状态（serviceId = 5）。
	 * Updates letter attachment state (serviceId = 5).
	 *
	 * letter id
	 * attachment type
	 */
	public SM_MAIL_SERVICE(int letterId, int attachmentType) {
		super(null);
		this.serviceId = 5;
		this.letterId = letterId;
		this.attachmentType = attachmentType;
	}

	/**
	 * 删除信件结果（serviceId = 6）。
	 * Letter delete result (serviceId = 6).
	 *
	 * @param letterIds 被删除信件 ID 数组 / deleted letter ids
	 */
	public SM_MAIL_SERVICE(int[] letterIds) {
		super(null);
		this.serviceId = 6;
		this.letterIds = letterIds;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Mailbox mailbox = con.getActivePlayer().getMailbox();
		this.totalCount = mailbox.size();
		this.unreadCount = mailbox.getUnreadCount();
		this.unreadExpressCount = mailbox.getUnreadCountByType(LetterType.EXPRESS);
		this.unreadBlackCloudCount = mailbox.getUnreadCountByType(LetterType.BLACKCLOUD);
		writeC(serviceId);
		switch (serviceId) {
		case 0:
			mailbox.isMailListUpdateRequired = true;
			writeMailboxState(totalCount, unreadCount, unreadExpressCount, unreadBlackCloudCount);
			break;
		case 1:
			writeMailMessage(mailMessage);
			break;
		case 2:
			Collection<Letter> _letters;
			if (!letters.isEmpty()) {
				ListSplitter<Letter> splittedLetters = new ListSplitter<Letter>(letters, 100);
				_letters = splittedLetters.getNext();
			} else {
				_letters = letters;
			}
			writeLettersList(_letters, player, isExpress, unreadExpressCount + unreadBlackCloudCount);
			break;
		case 3:
			writeLetterRead(letter, time, totalCount, unreadCount, unreadExpressCount, unreadBlackCloudCount);
			break;
		case 5:
			writeLetterState(letterId, attachmentType);
			break;
		case 6:
			mailbox.isMailListUpdateRequired = true;
			writeLetterDelete(totalCount, unreadCount, unreadExpressCount, unreadBlackCloudCount, letterIds);
			break;
		}
	}
}
