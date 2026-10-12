package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;

import com.aionemu.gameserver.persistence.entity.PlayerAppearancePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_appearance 的映射层，仅保留无法用 Flex CRUD 表达的 REPLACE INTO。
 * Mapper for table player_appearance, keeping only the REPLACE INTO that Flex CRUD cannot express.
 */
public interface PlayerAppearanceMapper extends BaseMapper<PlayerAppearancePo> {

	/**
	 * 整体替换玩家外观行（REPLACE INTO，列全量覆盖）。
	 * Replaces the whole player appearance row (REPLACE INTO, all columns overwritten).
	 *
	 * @param po 外观行 / appearance row
	 * @return 影响行数 / affected rows
	 */
	@Insert("REPLACE INTO player_appearance ("
		+ "player_id, voice, skin_rgb, hair_rgb, eye_rgb, lip_rgb, face, hair, "
		+ "deco, tattoo, face_contour, expression, pupil_shape, remove_mane, right_eye_rgb, eye_lash_shape, "
		+ "jaw_line, forehead, eye_height, eye_space, eye_width, eye_size, eye_shape, eye_angle, "
		+ "brow_height, brow_angle, brow_shape, nose, nose_bridge, nose_width, nose_tip, cheek, "
		+ "lip_height, mouth_size, lip_size, smile, lip_shape, jaw_height, chin_jut, ear_shape, "
		+ "head_size, neck, neck_length, shoulder_size, torso, chest, waist, hips, "
		+ "arm_thickness, hand_size, leg_thickness, facial_rate, foot_size, arm_length, leg_length, shoulders, "
		+ "face_shape, pupil_size, upper_torso, fore_arm_thickness, hand_span, calf_thickness, height "
		+ ") VALUES ("
		+ "#{playerId}, #{voice}, #{skinRgb}, #{hairRgb}, #{eyeRgb}, #{lipRgb}, "
		+ "#{face}, #{hair}, #{deco}, #{tattoo}, #{faceContour}, #{expression}, "
		+ "#{pupilShape}, #{removeMane}, #{rightEyeRgb}, #{eyeLashShape}, #{jawLine}, #{forehead}, "
		+ "#{eyeHeight}, #{eyeSpace}, #{eyeWidth}, #{eyeSize}, #{eyeShape}, #{eyeAngle}, "
		+ "#{browHeight}, #{browAngle}, #{browShape}, #{nose}, #{noseBridge}, #{noseWidth}, "
		+ "#{noseTip}, #{cheek}, #{lipHeight}, #{mouthSize}, #{lipSize}, #{smile}, "
		+ "#{lipShape}, #{jawHeight}, #{chinJut}, #{earShape}, #{headSize}, #{neck}, "
		+ "#{neckLength}, #{shoulderSize}, #{torso}, #{chest}, #{waist}, #{hips}, "
		+ "#{armThickness}, #{handSize}, #{legThickness}, #{facialRate}, #{footSize}, #{armLength}, "
		+ "#{legLength}, #{shoulders}, #{faceShape}, #{pupilSize}, #{upperTorso}, #{foreArmThickness}, "
		+ "#{handSpan}, #{calfThickness}, #{height})")
	int replaceAppearance(PlayerAppearancePo po);
}
