package com.aionemu.gameserver.model.team.legion;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;
import lombok.Setter;

/**
 * 军团徽章，用于团队相关逻辑。
 * Legion Emblem for team logic.
 * @author Simple modified cura
 */
@Getter
@Setter
public class LegionEmblem {

	private int emblemId = 0x00;
	private int color_r = 0x00;
	private int color_g = 0x00;
	private int color_b = 0x00;
	private boolean defaultEmblem = true;
	private LegionEmblemType emblemType = LegionEmblemType.DEFAULT;
	private PersistentState persistentState;

	private boolean isUploading = false;
	private int uploadSize = 0;
	private int uploadedSize = 0;
	private byte[] uploadData;

	private byte[] customEmblemData;

	/**
	 * @param customEmblemData the customEmblemData to set
	 */
	public void setCustomEmblemData(byte[] customEmblemData) {
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		this.customEmblemData = customEmblemData;
		this.emblemType = LegionEmblemType.CUSTOM;
	}

	public LegionEmblem() {
		setPersistentState(PersistentState.NEW);
	}

	public void setEmblem(int emblemId, int color_r, int color_g, int color_b, LegionEmblemType emblemType,
			byte[] emblem_data) {
		this.emblemId = emblemId;
		this.color_r = color_r;
		this.color_g = color_g;
		this.color_b = color_b;
		this.emblemType = emblemType;
		this.customEmblemData = emblem_data;
		if (this.emblemType.equals(LegionEmblemType.CUSTOM) && customEmblemData == null) {
			this.emblemId = 0;
			this.emblemType = LegionEmblemType.DEFAULT;
		}

		setPersistentState(PersistentState.UPDATE_REQUIRED);
		this.defaultEmblem = false;
	}

	/**
	 * @param data the uploadData to set
	 */
	public void addUploadData(byte[] data) {
		byte[] newData = new byte[uploadedSize];
		int i = 0;
		if (uploadData != null) {
			for (byte dataByte : uploadData) {
				newData[i] = dataByte;
				i++;
			}
		}
		for (byte dataByte : data) {
			newData[i] = dataByte;
			i++;
		}
		this.uploadData = newData;
	}

	/**
	 * @param uploadedSize the uploadedSize to set
	 */
	public void addUploadedSize(int uploadedSize) {
		this.uploadedSize += uploadedSize;
	}

	/**
	 * 清空全部上传数据。
	 * Clears out all upload data.
	 */
	public void resetUploadSettings() {
		this.isUploading = false;
		this.uploadedSize = 0;
		this.uploadData = null;
	}

	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case UPDATE_REQUIRED:
			if (this.persistentState == PersistentState.NEW) {
				break;
			}
		default:
			this.persistentState = persistentState;
		}
	}
}
