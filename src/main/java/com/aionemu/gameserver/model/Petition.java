package com.aionemu.gameserver.model;

import lombok.Getter;

/**
 * 请愿模型。
 * Petition model.
 *
 * @author zdead
 */
public class Petition {

	@Getter
	private final int petitionId;
	@Getter
	private final int playerObjId;
	private final PetitionType type;
	@Getter
	private final String title;
	@Getter
	private final String contentText;
	@Getter
	private final String additionalData;
	@Getter
	private final PetitionStatus status;

	/**
	 * 仅含请愿 ID 的构造（其余字段置空/默认）。
	 * Constructor with petition id only (other fields empty/default).
	 *
	 * @param petitionId 请愿 ID / petition id
	 */
	public Petition(int petitionId) {
		this.petitionId = petitionId;
		this.playerObjId = 0;
		this.type = PetitionType.INQUIRY;
		this.title = "";
		this.contentText = "";
		this.additionalData = "";
		this.status = PetitionStatus.PENDING;
	}

	/**
	 * 完整构造：将客户端类型/状态 ID 映射为对应枚举。
	 * Full constructor: maps client type/status ids to the matching enums.
	 *
	 * @param petitionId 请愿 ID / petition id
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param petitionTypeId 客户端类型 ID / client type id
	 * @param title 标题 / title
	 * @param contentText 内容 / content text
	 * @param additionalData 附加数据 / additional data
	 * @param petitionStatus 客户端状态 ID / client status id
	 */
	public Petition(int petitionId, int playerObjId, int petitionTypeId, String title, String contentText,
			String additionalData, int petitionStatus) {
		this.petitionId = petitionId;
		this.playerObjId = playerObjId;
		switch (petitionTypeId) {
		case 256:
			type = PetitionType.CHARACTER_STUCK;
			break;
		case 512:
			type = PetitionType.CHARACTER_RESTORATION;
			break;
		case 768:
			type = PetitionType.BUG;
			break;
		case 1024:
			type = PetitionType.QUEST;
			break;
		case 1280:
			type = PetitionType.UNACCEPTABLE_BEHAVIOR;
			break;
		case 1536:
			type = PetitionType.SUGGESTION;
			break;
		case 65280:
			type = PetitionType.INQUIRY;
			break;
		default:
			type = PetitionType.INQUIRY;
			break;
		}
		this.title = title;
		this.contentText = contentText;
		this.additionalData = additionalData;
		switch (petitionStatus) {
		case 0:
			status = PetitionStatus.PENDING;
			break;
		case 1:
			status = PetitionStatus.IN_PROGRESS;
			break;
		case 2:
			status = PetitionStatus.REPLIED;
			break;
		default:
			status = PetitionStatus.PENDING;
			break;
		}
	}

	/** 获取请愿类型。 / Returns the petition type. */
	public PetitionType getPetitionType() {
		return type;
	}

}
