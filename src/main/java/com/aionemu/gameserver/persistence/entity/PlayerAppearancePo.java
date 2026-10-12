package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_appearance 的持久化实体。
 * Persistence entity for table player_appearance.
 */
@Data
@Table("player_appearance")
public class PlayerAppearancePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Integer voice;

	private Integer skinRgb;

	private Integer hairRgb;

	private Integer eyeRgb;

	private Integer lipRgb;

	private Integer face;

	private Integer hair;

	private Integer deco;

	private Integer tattoo;

	private Integer faceContour;

	private Integer expression;

	private Integer pupilShape;

	private Integer removeMane;

	private Integer rightEyeRgb;

	private Integer eyeLashShape;

	private Integer jawLine;

	private Integer forehead;

	private Integer eyeHeight;

	private Integer eyeSpace;

	private Integer eyeWidth;

	private Integer eyeSize;

	private Integer eyeShape;

	private Integer eyeAngle;

	private Integer browHeight;

	private Integer browAngle;

	private Integer browShape;

	private Integer nose;

	private Integer noseBridge;

	private Integer noseWidth;

	private Integer noseTip;

	private Integer cheek;

	private Integer lipHeight;

	private Integer mouthSize;

	private Integer lipSize;

	private Integer smile;

	private Integer lipShape;

	private Integer jawHeight;

	private Integer chinJut;

	private Integer earShape;

	private Integer headSize;

	private Integer neck;

	private Integer neckLength;

	private Integer shoulderSize;

	private Integer torso;

	private Integer chest;

	private Integer waist;

	private Integer hips;

	private Integer armThickness;

	private Integer handSize;

	private Integer legThickness;

	private Integer facialRate;

	private Integer footSize;

	private Integer armLength;

	private Integer legLength;

	private Integer shoulders;

	private Integer faceShape;

	private Integer pupilSize;

	private Integer upperTorso;

	private Integer foreArmThickness;

	private Integer handSpan;

	private Integer calfThickness;

	private Float height;

}
