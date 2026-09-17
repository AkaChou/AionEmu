package com.aionemu.gameserver.model.gameobjects.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;

/**
 * 玩家 Appearance 游戏对象。
 * Player Appearance game object.
 *
 * @author SoulKeeper, srx47, alexa026
 */

@Getter
@Setter
@Slf4j
public class PlayerAppearance implements Cloneable {
	/**
	 * 玩家面部。
	 * Player's face
	 */
	private int face;
	/**
	 * 返回角色发型。
	 * Returns character's hair
	 * @return characters hair
	 */
	private int hair;
	/**
	 * 返回未知外观字段。
	 * Returns dunno what is this
	 * @return some crap, ask Neme what it is
	 */
	private int deco;
	/**
	 * 返回纹身。
	 * Returns sexy tattoo
	 * @return sexy tattoo
	 */
	private int tattoo;
	/**
	 * @return the faceContour
	 */
	private int faceContour; // 2.5
	/**
	 * @return the expression
	 */
	private int expression; // 2.5
	/**
	 * @return the pupilShape
	 */
	private int pupilShape; // 5.0
	/**
	 * @return the removeMane
	 */
	private int removeMane; // 5.0
	/**
	 * @return the rightEyeRGB
	 */
	private int rightEyeRGB; // 5.0
	private int eyeLashshape; // 5.0
	/**
	 * @return the jawLine
	 */
	private int jawLine; // 2.5
	/**
	 * @return 肤色 / Skin color
	 */
	private int skinRGB;
	/**
	 * @return 发色 / Hair color
	 */
	private int hairRGB;
	/**
	 * @return 唇色 / Lips color
	 */
	private int lipRGB;
	/**
	 * @param eyeRGB 眼睛颜色。 / Eye colour
	 */
	private int eyeRGB;
	/**
	 * 返回脸型。
	 * Returns face shape
	 * @return face shape
	 */
	private int faceShape;
	/**
	 * @return the pupilSize
	 */
	private int pupilSize; // 5.0
	/**
	 * @return the upperTorso
	 */
	private int upperTorso; // 5.0
	/**
	 * @return the foreArmThickness
	 */
	private int foreArmThickness; // 5.0
	/**
	 * @return the handSpan
	 */
	private int handSpan; // 5.0
	/**
	 * @return the calfThickness
	 */
	private int calfThickness; // 5.0
	/**
	 * 返回前额。
	 * Returns forehead
	 * @return forehead
	 */
	private int forehead;
	/**
	 * 返回眼高。
	 * Returns eye heigth
	 * @return eye height
	 */
	private int eyeHeight;
	/**
	 * @return 眼距 / Eye space
	 */
	private int eyeSpace;
	/**
	 * 返回眼宽。
	 * Returns eye width
	 * @return eye width
	 */
	private int eyeWidth;
	/**
	 * 返回眼睛大小（动漫少女通常眼睛很大）。
	 * Returns eye size. Hentai girls usually have very big eyes
	 *
	 * @return 眼睛大小 / eyes
	 */
	private int eyeSize;
	/**
	 * 返回眼型。
	 * Return eye shape
	 *
	 * @return 眼型 / eye shape
	 */
	private int eyeShape;
	/**
	 * 返回眼睛角度。
	 * Return eye angle
	 *
	 * @return 眼睛角度 / eye angle
	 */
	private int eyeAngle;
	/**
	 * @return 眉高 / brow height
	 */
	private int browHeight;
	/**
	 * 返回眉毛角度。
	 * Returns brow angle
	 *
	 * @return 眉毛角度 / brow angle
	 */
	private int browAngle;
	/**
	 * 返回眉毛形状。
	 * Returns brow shape
	 *
	 * @return 眉毛形状 / brow shape
	 */
	private int browShape;
	/**
	 * 返回鼻部。
	 * Returns nose
	 *
	 * @return 鼻部 / nose
	 */
	private int nose;
	/**
	 * 返回鼻梁。
	 * Returns nose bridge
	 *
	 * @return 鼻梁 / nose bridge
	 */
	private int noseBridge;
	/**
	 * 返回鼻宽。
	 * Returns nose width
	 *
	 * @return 鼻宽 / nose width
	 */
	private int noseWidth;
	/**
	 * 返回鼻尖。
	 * Returns nose tip
	 *
	 * @return 鼻尖 / nose tip
	 */
	private int noseTip;
	/**
	 * 返回脸颊。
	 * Returns cheeks
	 *
	 * @return 脸颊 / cheeks
	 */
	private int cheek;
	/**
	 * 返回嘴唇高度。
	 * Returns lip height
	 *
	 * @return 嘴唇高度 / lip height
	 */
	private int lipHeight;
	/**
	 * 返回嘴部大小。
	 * Returns mouth size
	 *
	 * @return 嘴部大小 / mouth size
	 */
	private int mouthSize;
	/**
	 * 返回嘴唇大小。
	 * Returns lips size
	 *
	 * @return 嘴唇大小 / lips size
	 */
	private int lipSize;
	/**
	 * 返回笑容。
	 * Returns smile
	 *
	 * @return 笑容 / smile
	 */
	private int smile;
	/**
	 * 返回嘴唇形状。
	 * Returns lips shape
	 *
	 * @return 嘴唇形状 / lips shape
	 */
	private int lipShape;
	/**
	 * 返回下颌高度。
	 * Returns jaws height
	 *
	 * @return 下颌高度 / jaws height
	 */
	private int jawHeigh;
	/**
	 * 返回下巴突出。
	 * Returns chin jut
	 *
	 * @return 下巴突出 / chin jut
	 */
	private int chinJut;
	/**
	 * 返回耳朵形状。
	 * Returns ear shape
	 *
	 * @return 耳朵形状 / ear shape
	 */
	private int earShape;
	/**
	 * 返回头部大小。
	 * Returns head size
	 *
	 * @return 头部大小 / head size
	 */
	private int headSize;
	/**
	 * 返回颈部。
	 * Returns neck
	 *
	 * @return 颈部 / neck
	 */
	private int neck;
	/**
	 * 返回颈长。
	 * Returns neck length
	 *
	 * @return 颈长 / neck length
	 */
	private int neckLength;
	/**
	 * @return 肩部 / Shoulders
	 */
	private int shoulders;
	/**
	 * @return 肩宽 / Shoulder Size
	 */
	private int shoulderSize;
	/**
	 * @return 躯干 / Torso
	 */
	private int torso;
	/**
	 * 返回胸部。
	 * Returns tits
	 *
	 * @return 胸部 / tits
	 */
	private int chest;
	/**
	 * 返回腰部。
	 * Returns waist
	 *
	 * @return 腰部 / waist
	 */
	private int waist;
	/**
	 * 返回臀部。
	 * Returns hips
	 *
	 * @return 臀部 / hips
	 */
	private int hips;
	/**
	 * 返回臂粗。
	 * Returns arm thickness
	 * @return arm thickness
	 */
	private int armThickness;
	/**
	 * 返回手臂长度。
	 * Returns arm length
	 *
	 * @return 手臂长度 / arm length
	 */
	private int armLength;
	/**
	 * 返回手部尺寸。
	 * Returns hand size
	 * @return hand size
	 */
	private int handSize;
	/**
	 * 返回腿部粗细。
	 * Returns legs thickness
	 *
	 * @return 腿部粗细 / leg thickness
	 */
	private int legThickness;
	/**
	 * 返回腿长。
	 * Returns legs Length
	 *
	 * @return 腿长 / leg Length
	 */
	private int legLength;
	/**
	 * 返回脚部尺寸。
	 * Returns foot size
	 * @return foot size
	 */
	private int footSize;
	/**
	 * @return 面部比例 / Facial rate
	 */
	private int facialRate;
	/**
	 * 返回声线。
	 * Returns sexy voice
	 *
	 * @return 声线 / sexy voice
	 */
	private int voice;
	/**
	 * 返回身高。
	 * Returns height
	 * @return height
	 */
	private float height;

	/**
	 * @return the eyeLashshape
	 */
	public int getEyeLashShape() {
		return eyeLashshape;
	}

	/**
	 * @param eyeLashshape the eyeLashshape to set
	 */
	public void setEyeLashShape(int eyeLashshape) {
		this.eyeLashshape = eyeLashshape;
	}

	/**
	 * 返回客户端缩放值对应的近似游戏内身高。
	 * Returns the approximate in-world height represented by the client scale value
	 */
	public float getBoundHeight() {
		return height * 1.75f;
	}

	/**
	 * @return 对象的副本 / a copy of the object
	 */
	public Object clone() {
		Object newObject = null;

		try {
			newObject = super.clone();
		} catch (CloneNotSupportedException e) {
			log.error(I18n.get("log.3854e6534415", e));
		}
		return newObject;
	}
}
