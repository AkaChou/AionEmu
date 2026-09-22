package com.aionemu.gameserver.model.gameobjects.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;

/**
 * 玩家 Appearance 游戏对象。
 * Player Appearance game object.
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
	 * characters hair
	 */
	private int hair;
	/**
	 * 返回未知外观字段。
	 * Returns dunno what is this
	 * some crap, ask Neme what it is
	 */
	private int deco;
	/**
	 * 返回纹身。
	 * Returns sexy tattoo
	 */
	private int tattoo;
	private int faceContour; // 2.5
	private int expression; // 2.5
	private int pupilShape; // 5.0
	private int removeMane; // 5.0
	private int rightEyeRGB; // 5.0
	private int eyeLashshape; // 5.0
	private int jawLine; // 2.5
	private int skinRGB;
	private int hairRGB;
	private int lipRGB;
	private int eyeRGB;
	/**
	 * 返回脸型。
	 * Returns face shape
	 */
	private int faceShape;
	private int pupilSize; // 5.0
	private int upperTorso; // 5.0
	private int foreArmThickness; // 5.0
	private int handSpan; // 5.0
	private int calfThickness; // 5.0
	/**
	 * 返回前额。
	 * Returns forehead
	 */
	private int forehead;
	/**
	 * 返回眼高。
	 * Returns eye heigth
	 * eye height
	 */
	private int eyeHeight;
	private int eyeSpace;
	/**
	 * 返回眼宽。
	 * Returns eye width
	 */
	private int eyeWidth;
	/**
	 * 返回眼睛大小（动漫少女通常眼睛很大）。
	 * Returns eye size. Hentai girls usually have very big eyes
	 */
	private int eyeSize;
	/**
	 * 返回眼型。
	 * Return eye shape
	 */
	private int eyeShape;
	/**
	 * 返回眼睛角度。
	 * Return eye angle
	 */
	private int eyeAngle;
	private int browHeight;
	/**
	 * 返回眉毛角度。
	 * Returns brow angle
	 */
	private int browAngle;
	/**
	 * 返回眉毛形状。
	 * Returns brow shape
	 */
	private int browShape;
	/**
	 * 返回鼻部。
	 * Returns nose
	 */
	private int nose;
	/**
	 * 返回鼻梁。
	 * Returns nose bridge
	 */
	private int noseBridge;
	/**
	 * 返回鼻宽。
	 * Returns nose width
	 */
	private int noseWidth;
	/**
	 * 返回鼻尖。
	 * Returns nose tip
	 */
	private int noseTip;
	/**
	 * 返回脸颊。
	 * Returns cheeks
	 */
	private int cheek;
	/**
	 * 返回嘴唇高度。
	 * Returns lip height
	 */
	private int lipHeight;
	/**
	 * 返回嘴部大小。
	 * Returns mouth size
	 */
	private int mouthSize;
	/**
	 * 返回嘴唇大小。
	 * Returns lips size
	 */
	private int lipSize;
	/**
	 * 返回笑容。
	 * Returns smile
	 */
	private int smile;
	/**
	 * 返回嘴唇形状。
	 * Returns lips shape
	 */
	private int lipShape;
	/**
	 * 返回下颌高度。
	 * Returns jaws height
	 */
	private int jawHeigh;
	/**
	 * 返回下巴突出。
	 * Returns chin jut
	 */
	private int chinJut;
	/**
	 * 返回耳朵形状。
	 * Returns ear shape
	 */
	private int earShape;
	/**
	 * 返回头部大小。
	 * Returns head size
	 */
	private int headSize;
	/**
	 * 返回颈部。
	 * Returns neck
	 */
	private int neck;
	/**
	 * 返回颈长。
	 * Returns neck length
	 */
	private int neckLength;
	private int shoulders;
	private int shoulderSize;
	private int torso;
	/**
	 * 返回胸部。
	 * Returns tits
	 */
	private int chest;
	/**
	 * 返回腰部。
	 * Returns waist
	 */
	private int waist;
	/**
	 * 返回臀部。
	 * Returns hips
	 */
	private int hips;
	/**
	 * 返回臂粗。
	 * Returns arm thickness
	 */
	private int armThickness;
	/**
	 * 返回手臂长度。
	 * Returns arm length
	 */
	private int armLength;
	/**
	 * 返回手部尺寸。
	 * Returns hand size
	 */
	private int handSize;
	/**
	 * 返回腿部粗细。
	 * Returns legs thickness
	 * 腿部粗细 / leg thickness
	 */
	private int legThickness;
	/**
	 * 返回腿长。
	 * Returns legs Length
	 * 腿长 / leg Length
	 */
	private int legLength;
	/**
	 * 返回脚部尺寸。
	 * Returns foot size
	 */
	private int footSize;
	private int facialRate;
	/**
	 * 返回声线。
	 * Returns sexy voice
	 */
	private int voice;
	/**
	 * 返回身高。
	 * Returns height
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
			log.error(I18n.get("log.3854e6534415"), e);
		}
		return newObject;
	}
}
