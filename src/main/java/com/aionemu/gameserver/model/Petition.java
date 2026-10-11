package com.aionemu.gameserver.model;

/**
 * 请愿模型。
 * Petition model.
 * @author zdead
 */
public record Petition(int petitionId, int playerObjId, PetitionType type, String title, String contentText,
                       String additionalData, PetitionStatus status) {

	/**
	 * 仅含请愿 ID 的构造（其余字段置空/默认）。
	 * Constructor with petition id only (other fields empty/default).
	 * @param petitionId 请愿 ID / petition id
	 */
	public Petition(int petitionId) {
		this(petitionId, 0, PetitionType.INQUIRY, "", "", "", PetitionStatus.PENDING);
	}

	/**
	 * 完整构造：将客户端类型/状态 ID 映射为对应枚举。
	 * Full constructor: maps client type/status ids to the matching enums.
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
		this(petitionId, playerObjId, typeOf(petitionTypeId), title, contentText, additionalData,
			statusOf(petitionStatus));
	}

	/** 获取请愿类型。 / Returns the petition type. */
	public PetitionType getPetitionType() {
		return type;
	}

	private static PetitionType typeOf(int petitionTypeId) {
		return switch (petitionTypeId) {
			case 256 -> PetitionType.CHARACTER_STUCK;
			case 512 -> PetitionType.CHARACTER_RESTORATION;
			case 768 -> PetitionType.BUG;
			case 1024 -> PetitionType.QUEST;
			case 1280 -> PetitionType.UNACCEPTABLE_BEHAVIOR;
			case 1536 -> PetitionType.SUGGESTION;
			default -> PetitionType.INQUIRY;
		};
	}

	private static PetitionStatus statusOf(int petitionStatus) {
		return switch (petitionStatus) {
			case 1 -> PetitionStatus.IN_PROGRESS;
			case 2 -> PetitionStatus.REPLIED;
			default -> PetitionStatus.PENDING;
		};
	}
}
