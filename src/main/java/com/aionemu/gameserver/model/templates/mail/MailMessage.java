package com.aionemu.gameserver.model.templates.mail;

import lombok.Getter;

/**
 * 邮件消息枚举。
 * Mail Message enumeration.
 *
 * @author kosyachok
 */
@Getter
public enum MailMessage {
	/** Mail Send Seccess / Mail Send Seccess */
	MAIL_SEND_SECCESS(0), NO_SUCH_CHARACTER_NAME(1), RECIPIENT_MAILBOX_FULL(2), MAIL_IS_ONE_RACE_ONLY(3),
	/** YouAreInRecipientIgnore 列表 / You Are In Recipient Ignore List */
	YOU_ARE_IN_RECIPIENT_IGNORE_LIST(4), RECIPIENT_IGNORING_MAIL_FROM_PLAYERS_LOWER_206_LVL(5), // WTF??
	/** Mailspam Wait For Some Time / Mailspam Wait For Some Time */
	MAILSPAM_WAIT_FOR_SOME_TIME(6);

	/** 返回 ID / Returns the id */
	private final int id;

	MailMessage(int id) {
		this.id = id;
	}
}
