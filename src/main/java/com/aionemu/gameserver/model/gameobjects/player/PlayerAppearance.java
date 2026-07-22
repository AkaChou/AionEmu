package com.aionemu.gameserver.model.gameobjects.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 Appearance 游戏对象。
 * Player Appearance game object.
 *
 * @author SoulKeeper, srx47, alexa026
 */

@Slf4j
public class PlayerAppearance implements Cloneable {
	/**
	 * 玩家面部。 / Player's face
	 */
	private int face;
	private int hair;
	private int deco;
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
	private int faceShape;
	private int pupilSize; // 5.0
	private int upperTorso; // 5.0
	private int foreArmThickness; // 5.0
	private int handSpan; // 5.0
	private int calfThickness; // 5.0
	private int forehead;
	private int eyeHeight;
	private int eyeSpace;
	private int eyeWidth;
	private int eyeSize;
	private int eyeShape;
	private int eyeAngle;
	private int browHeight;
	private int browAngle;
	private int browShape;
	private int nose;
	private int noseBridge;
	private int noseWidth;
	private int noseTip;
	private int cheek;
	private int lipHeight;
	private int mouthSize;
	private int lipSize;
	private int smile;
	private int lipShape;
	private int jawHeigh;
	private int chinJut;
	private int earShape;
	private int headSize;
	private int neck;
	private int neckLength;
	private int shoulders;
	private int shoulderSize;
	private int torso;
	private int chest;
	private int waist;
	private int hips;
	private int armThickness;
	private int armLength;
	private int handSize;
	private int legThickness;
	private int legLength;
	private int footSize;
	private int facialRate;
	private int voice;
	private float height;

	 /**
	  * 返回角色脸型。
	  * Returns character face
	  * @return character face
	  */
	public int getFace() {
		return face;
	}

	/**
	 * 设置 charactersface。
	 * Sets character's face
	 *
	 * @param face characters face
	 */
	public void setFace(int face) {
		this.face = face;
	}

	 /**
	  * 返回角色发型。
	  * Returns character's hair
	  * @return characters hair
	  */
	public int getHair() {
		return hair;
	}

	/**
	 * 设置 charaxctershair。
	 * Sets charaxcters hair
	 *
	 * @param hair characters hair
	 */
	public void setHair(int hair) {
		this.hair = hair;
	}

	 /**
	  * 返回未知外观字段。
	  * Returns dunno what is this
	  * @return some crap, ask Neme what it is
	  */
	public int getDeco() {
		return deco;
	}

	/**
	 * 设置 somecrapasknemewhatit。
	 * Sets some crap, ask Neme what it is
	 *
	 * @param deco crap
	 */
	public void setDeco(int deco) {
		this.deco = deco;
	}

	 /**
	  * 返回纹身。
	  * Returns sexy tattoo
	  * @return sexy tattoo
	  */
	public int getTattoo() {
		return tattoo;
	}

	/**
	 * 设置纹身。<br>。 / Set's sexy tattoo.<br> Not sexy will throw NotSexyTattooException. Just kidding ;).
	 */
	public void setTattoo(int tattoo) {
		this.tattoo = tattoo;
	}

	/**
	 * @return the faceContour
	 */
	public int getFaceContour() {
		return faceContour;
	}

	/**
	 * @param faceContour the faceContour to set
	 */
	public void setFaceContour(int faceContour) {
		this.faceContour = faceContour;
	}

	/**
	 * @return the expression
	 */
	public int getExpression() {
		return expression;
	}

	/**
	 * @param expression the expression to set
	 */
	public void setExpression(int expression) {
		this.expression = expression;
	}

	/**
	 * @return the pupilShape
	 */
	public int getPupilShape() {
		return pupilShape;
	}

	/**
	 * @param pupilShape the pupilShape to set
	 */
	public void setPupilShape(int pupilShape) {
		this.pupilShape = pupilShape;
	}

	/**
	 * @return the removeMane
	 */
	public int getRemoveMane() {
		return removeMane;
	}

	/**
	 * @param removeMane the removeMane to set
	 */
	public void setRemoveMane(int removeMane) {
		this.removeMane = removeMane;
	}

	/**
	 * @return the rightEyeRGB
	 */
	public int getRightEyeRGB() {
		return rightEyeRGB;
	}

	/**
	 * @param rightEyeRGB the rightEyeRGB to set
	 */
	public void setRightEyeRGB(int rightEyeRGB) {
		this.rightEyeRGB = rightEyeRGB;
	}

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
	 * @return the pupilSize
	 */
	public int getPupilSize() {
		return pupilSize;
	}

	/**
	 * @param pupilSize the pupilSize to set
	 */
	public void setPupilSize(int pupilSize) {
		this.pupilSize = pupilSize;
	}

	/**
	 * @return the upperTorso
	 */
	public int getUpperTorso() {
		return upperTorso;
	}

	/**
	 * @param upperTorso the upperTorso to set
	 */
	public void setUpperTorso(int upperTorso) {
		this.upperTorso = upperTorso;
	}

	/**
	 * @return the foreArmThickness
	 */
	public int getForeArmThickness() {
		return foreArmThickness;
	}

	/**
	 * @param foreArmThickness the foreArmThickness to set
	 */
	public void setForeArmThickness(int foreArmThickness) {
		this.foreArmThickness = foreArmThickness;
	}

	/**
	 * @return the handSpan
	 */
	public int getHandSpan() {
		return handSpan;
	}

	/**
	 * @param handSpan the handSpan to set
	 */
	public void setHandSpan(int handSpan) {
		this.handSpan = handSpan;
	}

	/**
	 * @return the calfThickness
	 */
	public int getCalfThickness() {
		return calfThickness;
	}

	/**
	 * @param calfThickness the calfThickness to set
	 */
	public void setCalfThickness(int calfThickness) {
		this.calfThickness = calfThickness;
	}

	/**
	 * @return the jawLine
	 */
	public int getJawLine() {
		return jawLine;
	}

	/**
	 * @param jawLine the jawLine to set
	 */
	public void setJawLine(int jawLine) {
		this.jawLine = jawLine;
	}

	/**
	 * @return Skin color, let's create pink lesbians :D skin color。
	 */
	public int getSkinRGB() {
		return skinRGB;
	}

	/**
	 * @param skinRGB 设置肤色。 肤色 RGB / Here is the valid place to make lesbians skin pink skin color
	 */
	public void setSkinRGB(int skinRGB) {
		this.skinRGB = skinRGB;
	}

	/**
	 * @return Hair color, personally i prefer brunettes har color。
	 */
	public int getHairRGB() {
		return hairRGB;
	}

	/**
	 * 设置 haircolorsblondsmustpassiqtest。
	 * Sets hair colors. Blonds must pass IQ test ;)
	 *
	 * @param hairRGB Hair color
	 */
	public void setHairRGB(int hairRGB) {
		this.hairRGB = hairRGB;
	}

	/**
	 * @param eyeRGB 眼睛颜色。 / Eye colour
	 */
	public void setEyeRGB(int eyeRGB) {
		this.eyeRGB = eyeRGB;
	}

	/**
	 * 设置 eyecolour。
	 * Sets eye colour
	 */
	public int getEyeRGB() {
		return eyeRGB;
	}

	/**
	 * @return Lips color. lips color。
	 */
	public int getLipRGB() {
		return lipRGB;
	}

	/**
	 * 设置 lipscolor。
	 * Sets lips color
	 *
	 * @param lipRGB face shape
	 */
	public void setLipRGB(int lipRGB) {
		this.lipRGB = lipRGB;
	}

	 /**
	  * 返回脸型。
	  * Returns face shape
	  * @return face shape
	  */
	public int getFaceShape() {
		return faceShape;
	}

	/**
	 * 设置 faceshape。
	 * Sets face shape
	 *
	 * @param faceShape face shape
	 */
	public void setFaceShape(int faceShape) {
		this.faceShape = faceShape;
	}

	 /**
	  * 返回前额。
	  * Returns forehead
	  * @return forehead
	  */
	public int getForehead() {
		return forehead;
	}

	/**
	 * 设置 forehead。
	 * Sets forehead
	 *
	 * @param forehead size
	 */
	public void setForehead(int forehead) {
		this.forehead = forehead;
	}

	 /**
	  * 返回眼高。
	  * Returns eye heigth
	  * @return eye height
	  */
	public int getEyeHeight() {
		return eyeHeight;
	}

	/**
	 * 设置 eyeheigth。
	 * Sets eye heigth
	 *
	 * @param eyeHeight eye heigth
	 */
	public void setEyeHeight(int eyeHeight) {
		this.eyeHeight = eyeHeight;
	}

	/**
	 * @return Eye space eye space。
	 */
	public int getEyeSpace() {
		return eyeSpace;
	}

	/**
	 * @param eyeSpace 设置眼距。 眼距 / Eye space someting connected to eyes
	 */
	public void setEyeSpace(int eyeSpace) {
		this.eyeSpace = eyeSpace;
	}

	 /**
	  * 返回眼宽。
	  * Returns eye width
	  * @return eye width
	  */
	public int getEyeWidth() {
		return eyeWidth;
	}

	/**
	 * 设置 eyewidth。
	 * Sets eye width
	 *
	 * @param eyeWidth eye width
	 */
	public void setEyeWidth(int eyeWidth) {
		this.eyeWidth = eyeWidth;
	}

	/**
	 * Returns eye size. Hentai girls usually have very big eyes
	 *
	 * @return eyes
	 */
	public int getEyeSize() {
		return eyeSize;
	}

	/**
	 * 设置眼睛大小。 / Set's eye size.<br> Can be . o O ;).
	 */
	public void setEyeSize(int eyeSize) {
		this.eyeSize = eyeSize;
	}

	/**
	 * Return eye shape
	 *
	 * @return eye shape
	 */
	public int getEyeShape() {
		return eyeShape;
	}

	/**
	 * 设置 eyeshapebrcanbe_0ooetc。
	 * Sets Eye shape.<br> Can be . _ | 0 o O etc :)
	 *
	 * @param eyeShape eye shape
	 */
	public void setEyeShape(int eyeShape) {
		this.eyeShape = eyeShape;
	}

	/**
	 * Return eye angle
	 *
	 * @return eye angle
	 */
	public int getEyeAngle() {
		return eyeAngle;
	}

	/**
	 * 设置眼睛角度。 / Sets eye angle, / | \.
	 */
	public void setEyeAngle(int eyeAngle) {
		this.eyeAngle = eyeAngle;
	}

	/**
	 * @return 返回眉高。 眉高 / Rerturn brow heigth brow heigth
	 */
	public int getBrowHeight() {
		return browHeight;
	}

	/**
	 * @param browHeight 设置眉高。 / Brow heigth brow heigth
	 */
	public void setBrowHeight(int browHeight) {
		this.browHeight = browHeight;
	}

	/**
	 * Returns brow angle
	 *
	 * @return brow angle
	 */
	public int getBrowAngle() {
		return browAngle;
	}

	/**
	 * 设置 browangle。
	 * Sets brow angle
	 *
	 * @param browAngle brow angle
	 */
	public void setBrowAngle(int browAngle) {
		this.browAngle = browAngle;
	}

	/**
	 * Returns brow shape
	 *
	 * @return brow shape
	 */
	public int getBrowShape() {
		return browShape;
	}

	/**
	 * 设置 browshape。
	 * Sets brow shape
	 *
	 * @param browShape brow shape
	 */
	public void setBrowShape(int browShape) {
		this.browShape = browShape;
	}

	/**
	 * 返回 nose。 / Returns nose
	 *
	 * @return nose
	 */
	public int getNose() {
		return nose;
	}

	/**
	 * 设置 nose。
	 * Sets nose
	 *
	 * @param nose nose
	 */
	public void setNose(int nose) {
		this.nose = nose;
	}

	/**
	 * Returns nose bridge
	 *
	 * @return nose bridge
	 */
	public int getNoseBridge() {
		return noseBridge;
	}

	/**
	 * 设置 nosebridge。
	 * Sets nose bridge
	 *
	 * @param noseBridge nose bridge
	 */
	public void setNoseBridge(int noseBridge) {
		this.noseBridge = noseBridge;
	}

	/**
	 * Returns nose width
	 *
	 * @return nose width
	 */
	public int getNoseWidth() {
		return noseWidth;
	}

	/**
	 * 设置 nosewidth。
	 * Sets nose width
	 *
	 * @param noseWidth nose width
	 */
	public void setNoseWidth(int noseWidth) {
		this.noseWidth = noseWidth;
	}

	/**
	 * Returns noce tip
	 *
	 * @return noce tip
	 */
	public int getNoseTip() {
		return noseTip;
	}

	/**
	 * 设置 nocetip。
	 * Sets noce tip
	 *
	 * @param noseTip noce tip
	 */
	public void setNoseTip(int noseTip) {
		this.noseTip = noseTip;
	}

	/**
	 * Returns cheeks
	 *
	 * @return cheeks
	 */
	public int getCheek() {
		return cheek;
	}

	/**
	 * 设置 cheeks。
	 * Sets cheeks
	 *
	 * @param cheek checks
	 */
	public void setCheek(int cheek) {
		this.cheek = cheek;
	}

	/**
	 * Returns lip heigth
	 *
	 * @return lip heigth
	 */
	public int getLipHeight() {
		return lipHeight;
	}

	/**
	 * 设置 lipheigth。
	 * Sets lip heigth
	 *
	 * @param lipHeight lip heith
	 */
	public void setLipHeight(int lipHeight) {
		this.lipHeight = lipHeight;
	}

	/**
	 * Returns mouth size
	 *
	 * @return mouth size
	 */
	public int getMouthSize() {
		return mouthSize;
	}

	/**
	 * 设置 mouth 大小。
	 * Sets mouth size
	 *
	 * @param mouthSize mouth size
	 */
	public void setMouthSize(int mouthSize) {
		this.mouthSize = mouthSize;
	}

	/**
	 * Returns lips size
	 *
	 * @return lips size
	 */
	public int getLipSize() {
		return lipSize;
	}

	/**
	 * 设置 lips 大小。
	 * Sets lips size
	 *
	 * @param lipSize lips size
	 */
	public void setLipSize(int lipSize) {
		this.lipSize = lipSize;
	}

	/**
	 * 返回 smile。 / Returns smile
	 *
	 * @return smile
	 */
	public int getSmile() {
		return smile;
	}

	/**
	 * 设置 smile。
	 * Sets smile
	 *
	 * @param smile smile
	 */
	public void setSmile(int smile) {
		this.smile = smile;
	}

	/**
	 * Returns lips shape
	 *
	 * @return lips shape
	 */
	public int getLipShape() {
		return lipShape;
	}

	/**
	 * 设置 lipsshape。
	 * Sets lips shape
	 *
	 * @param lipShape lips shape
	 */
	public void setLipShape(int lipShape) {
		this.lipShape = lipShape;
	}

	/**
	 * Returns jaws height
	 *
	 * @return jaws height
	 */
	public int getJawHeigh() {
		return jawHeigh;
	}

	/**
	 * 设置 jawsheight。
	 * Sets jaws height
	 *
	 * @param jawHeigh jaws height
	 */
	public void setJawHeigh(int jawHeigh) {
		this.jawHeigh = jawHeigh;
	}

	/**
	 * Returns chin jut
	 *
	 * @return chin jut
	 */
	public int getChinJut() {
		return chinJut;
	}

	/**
	 * 设置 chinjut。
	 * Sets chin jut
	 *
	 * @param chinJut chin jut
	 */
	public void setChinJut(int chinJut) {
		this.chinJut = chinJut;
	}

	/**
	 * Returns ear shape
	 *
	 * @return ear shape
	 */
	public int getEarShape() {
		return earShape;
	}

	/**
	 * 设置 earshape。
	 * Sets ear shape
	 *
	 * @param earShape ear shape
	 */
	public void setEarShape(int earShape) {
		this.earShape = earShape;
	}

	/**
	 * Returns head size
	 *
	 * @return head size
	 */
	public int getHeadSize() {
		return headSize;
	}

	/**
	 * 设置 head 大小。
	 * Sets head size
	 *
	 * @param headSize head size
	 */
	public void setHeadSize(int headSize) {
		this.headSize = headSize;
	}

	/**
	 * 返回 neck。 / Returns neck
	 *
	 * @return neck
	 */
	public int getNeck() {
		return neck;
	}

	/**
	 * 设置 neck。
	 * Sets neck
	 *
	 * @param neck neck
	 */
	public void setNeck(int neck) {
		this.neck = neck;
	}

	/**
	 * Returns neck length
	 *
	 * @return neck length
	 */
	public int getNeckLength() {
		return neckLength;
	}

	/**
	 * 设置 necklengthjustcuriousitpossiblegiraffe。
	 * Sets neck length, just curious, is it possible to create a giraffe?
	 *
	 * @param neckLength neck length
	 */
	public void setNeckLength(int neckLength) {
		this.neckLength = neckLength;
	}

	/**
	 * @return 返回肩。 / Shoulders shouldeers
	 */
	public int getShoulders() {
		return shoulders;
	}

	/**
	 * @param shoulders 设置肩。 / Shoulders shoulders
	 */
	public void setShoulders(int shoulders) {
		this.shoulders = shoulders;
	}

	/**
	 * @return 返回肩宽。 / Shoulder Size shouldeerSize
	 */
	public int getShoulderSize() {
		return shoulderSize;
	}

	/**
	 * @param shoulderSize 设置肩。 / Shoulder Size shoulderSize
	 */
	public void setShoulderSize(int shoulderSize) {
		this.shoulderSize = shoulderSize;
	}

	/**
	 * @return 返回躯干。 / Torso torso
	 */
	public int getTorso() {
		return torso;
	}

	/**
	 * 设置 torso。
	 * Sets torso
	 *
	 * @param torso torso
	 */
	public void setTorso(int torso) {
		this.torso = torso;
	}

	/**
	 * 返回 tits。 / Returns tits
	 *
	 * @return tits
	 */
	public int getChest() {
		return chest;
	}

	/**
	 * 设置 tits。
	 * Sets tits
	 *
	 * @param chest tits
	 */
	public void setChest(int chest) {
		this.chest = chest;
	}

	/**
	 * 返回 waist。 / Returns waist
	 *
	 * @return waist
	 */
	public int getWaist() {
		return waist;
	}

	/**
	 * 设置 waist。
	 * sets waist
	 *
	 * @param waist waist
	 */
	public void setWaist(int waist) {
		this.waist = waist;
	}

	/**
	 * 返回 hips。 / Returns hips
	 *
	 * @return hips
	 */
	public int getHips() {
		return hips;
	}

	/**
	 * 设置 hips。
	 * Sets hips
	 *
	 * @param hips hips
	 */
	public void setHips(int hips) {
		this.hips = hips;
	}

	 /**
	  * 返回臂粗。
	  * Returns arm thickness
	  * @return arm thickness
	  */
	public int getArmThickness() {
		return armThickness;
	}

	/**
	 * 设置 armthickness。
	 * Sets arm thickness
	 *
	 * @param armThickness arm thickness
	 */
	public void setArmThickness(int armThickness) {
		this.armThickness = armThickness;
	}

	/**
	 * Returns arm length
	 *
	 * @return arm length
	 */
	public int getArmLength() {
		return armLength;
	}

	/**
	 * 设置 armlength。
	 * Sets arm length
	 *
	 * @param armLength arm length
	 */
	public void setArmLength(int armLength) {
		this.armLength = armLength;
	}

	 /**
	  * 返回手部尺寸。
	  * Returns hand size
	  * @return hand size
	  */
	public int getHandSize() {
		return handSize;
	}

	/**
	 * 设置 hand 大小。
	 * Sets hand size
	 *
	 * @param handSize hand size
	 */
	public void setHandSize(int handSize) {
		this.handSize = handSize;
	}

	/**
	 * Returns legs thickness
	 *
	 * @return leg thickness
	 */
	public int getLegThickness() {
		return legThickness;
	}

	/**
	 * 设置 legthickness。
	 * Sets leg thickness
	 *
	 * @param legThickness leg thickness
	 */
	public void setLegThickness(int legThickness) {
		this.legThickness = legThickness;
	}

	/**
	 * Returns legs Length
	 *
	 * @return leg Length
	 */
	public int getLegLength() {
		return legLength;
	}

	/**
	 * 设置 leglength。
	 * Sets leg length
	 *
	 * @param legLength leg length
	 */
	public void setLegLength(int legLength) {
		this.legLength = legLength;
	}

	 /**
	  * 返回脚部尺寸。
	  * Returns foot size
	  * @return foot size
	  */
	public int getFootSize() {
		return footSize;
	}

	/**
	 * 设置 foot 大小。
	 * Sets foot size
	 *
	 * @param footSize foot size
	 */
	public void setFootSize(int footSize) {
		this.footSize = footSize;
	}

	/**
	 * @return 返回面部比例。 / Retunrs facial rate facial rate
	 */
	public int getFacialRate() {
		return facialRate;
	}

	/**
	 * 设置 facialrate。
	 * Sets facial rate
	 *
	 * @param facialRate facial rate
	 */
	public void setFacialRate(int facialRate) {
		this.facialRate = facialRate;
	}

	/**
	 * Returns sexy voice
	 *
	 * @return sexy voice
	 */
	public int getVoice() {
		return voice;
	}

	/**
	 * 设置 sexyvoice。
	 * Sets sexy voice
	 *
	 * @param voice sexy voice
	 */
	public void setVoice(int voice) {
		this.voice = voice;
	}

	 /**
	  * 返回身高。
	  * Returns height
	  * @return height
	  */
	public float getHeight() {
		return height;
	}

	/**
	 * Returns the approximate in-world height represented by the client scale value
	 */
	public float getBoundHeight() {
		return height * 1.75f;
	}

	/**
	 * 设置 height。
	 * Sets height
	 *
	 * @param height height
	 */
	public void setHeight(float height) {
		this.height = height;
	}

	/**
	 * @return 允许复制该对象。 / Allow to copy the object @author Divinity
	 */
	public Object clone() {
		Object newObject = null;

		try {
			newObject = super.clone();
		} catch (CloneNotSupportedException e) {
			log.error(I18n.get("log.3854e6534415", e), e);
		}
		return newObject;
	}
}
