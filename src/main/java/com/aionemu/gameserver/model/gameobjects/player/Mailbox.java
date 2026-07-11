package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.LinkedHashMap;

/**
 * Mailbox 游戏对象。
 * Mailbox game object.
 *
 * @author kosyachok
 * @modified Atracer
 */
public class Mailbox {

	private Map<Integer, Letter> mails = new LinkedHashMap<Integer, Letter>();
	private Map<Integer, Letter> reserveMail = new LinkedHashMap<Integer, Letter>();
	private Player owner;
	public boolean isMailListUpdateRequired;

	// 0x00 - 关闭 / 0x00 - closed
	// 0x01 - 普通 / 0x01 - regular
	// 0x02 - 快递 / 0x02 - express
	public byte mailBoxState = 0;

	public Mailbox(Player player) {
		this.owner = player;
	}

	/**
	 * @param letter
	 */
	public void putLetterToMailbox(Letter letter) {
		if (haveFreeSlots()) {
			mails.put(letter.getObjectId(), letter);
		} else {
			reserveMail.put(letter.getObjectId(), letter);
		}
	}

	/**
	 * 获取全部 lettersmailboxsortedaccording 时间 received。
	 * Get all letters in mailbox (sorted according to time received)
	 *
	 * @return
	 */
	public Collection<Letter> getLetters() {
		SortedSet<Letter> letters = new TreeSet<Letter>(new Comparator<Letter>() {

			/** 比较 / compare. */
			@Override
			public int compare(Letter o1, Letter o2) {
				if (o1.getTimeStamp().getTime() > o2.getTimeStamp().getTime()) {
					return 1;
				}
				if (o1.getTimeStamp().getTime() < o2.getTimeStamp().getTime()) {
					return -1;
				}
				return o1.getObjectId() > o2.getObjectId() ? 1 : -1;
			}
		});

		for (Letter letter : mails.values()) {
			letters.add(letter);
		}
		return letters;
	}

	/**
	 * 获取 systemletterswhichsendersstartstringspecifiedwerereceivedsincelast 玩家 login。
	 * Get system letters which senders start with the string specified and were received since the last player login
	 *
	 * @param substring must start with special characters: % or $$
	 * @return new list of letters
	 */
	public List<Letter> getNewSystemLetters(String substring) {
		List<Letter> letters = new ArrayList<Letter>();
		for (Letter letter : mails.values()) {
			if (letter.getSenderName() == null || !letter.isUnread()) {
				continue;
			}
			if (owner.getCommonData().getLastOnline().getTime() > letter.getTimeStamp().getTime()) {
				continue;
			}
			if (letter.getSenderName().startsWith("%") || letter.getSenderName().startsWith("$$")) {
				if (letter.getSenderName().startsWith(substring)) {
					letters.add(letter);
				}
			}
		}
		return letters;
	}

	/**
	 * 获取 letterspecifiedletterID。
	 * Get letter with specified letter id
	 *
	 * @param letterObjId
	 * @return
	 */
	public Letter getLetterFromMailbox(int letterObjId) {
		return mails.get(letterObjId);
	}

	/**
	 * 检查是否 mailboxcontains 空 letters。 / Check whether mailbox contains empty letters
	 *
	 * @return
	 */
	public boolean haveUnread() {
		for (Letter letter : mails.values()) {
			if (letter.isUnread()) {
				return true;
			}
		}
		return false;
	}

	/** 返回 unread count / Returns the unread count */
	public final int getUnreadCount() {
		int unreadCount = 0;
		for (Letter letter : mails.values()) {
			if (letter.isUnread()) {
				unreadCount++;
			}
		}
		return unreadCount;
	}

	/** 按类型是否有未读 / Have Unread By Type */
	public boolean haveUnreadByType(LetterType letterType) {
		for (Letter letter : mails.values()) {
			if (letter.isUnread() && letter.getLetterType() == letterType) {
				return true;
			}
		}
		return false;
	}

	/** 按 type 返回 unread count / Returns the unread count by type */
	public final int getUnreadCountByType(LetterType letterType) {
		int count = 0;
		for (Letter letter : mails.values()) {
			if (letter.isUnread() && letter.getLetterType() == letterType) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return
	 */
	public boolean haveFreeSlots() {
		return mails.size() < 100;
	}

	/**
	 * @param letterId
	 */
	public void removeLetter(int letterId) {
		mails.remove(letterId);
		uploadReserveLetters();
	}

	/**
	 * @return 邮箱当前容量 / 大小。 / Current size of mailbox @return
	 */
	public int size() {
		return mails.size();
	}

	/** 上传预留信件 / upload Reserve Letters. */
	public void uploadReserveLetters() {
		if (reserveMail.size() > 0 && haveFreeSlots()) {
			boolean promoted = false;
			for (Iterator<Letter> iterator = reserveMail.values().iterator(); iterator.hasNext();) {
				Letter letter = iterator.next();
				if (haveFreeSlots()) {
					mails.put(letter.getObjectId(), letter);
					iterator.remove();
					promoted = true;
				} else
					break;
			}
			if (promoted && getOwner() != null) {
				GameCoreGameplayServices.mailService().refreshMail(getOwner());
			}
		}
	}

	/** 发送邮件列表。 / Send mail list. */
	public void sendMailList(boolean expressOnly) {
		PacketSendUtility.sendPacket(owner, new SM_MAIL_SERVICE(owner, getLetters(), expressOnly));
	}

	/** 返回所有者 / Returns the owner*/
	public Player getOwner() {
		return owner;
	}
}
