package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 mail 的持久化实体。
 * Persistence entity for table mail.
 */
@Data
@Table("mail")
public class MailPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer mailUniqueId;

	private Integer mailRecipientId;

	private String senderName;

	private String mailTitle;

	private String mailMessage;

	private Integer unread;

	private Integer attachedItemId;

	private Long attachedKinahCount;

	private Long attachedApCount;

	private Integer express;

	private Timestamp recievedTime;

}
