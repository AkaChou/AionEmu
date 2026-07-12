package com.aionemu.gameserver.model.templates.spawns;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 刷新点 Spot 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpawnSpotTemplate")
public class SpawnSpotTemplate {
	@XmlAttribute(name = "state")
	private Integer state = 0;

	@XmlAttribute(name = "astate")
	private Integer astate = 0;

	@XmlAttribute(name = "bstate")
	private Integer bstate = 0;

	@XmlAttribute(name = "cstate")
	private Integer cstate = 0;

	@XmlAttribute(name = "dstate")
	private Integer dstate = 0;

	@XmlAttribute(name = "estate")
	private Integer estate = 0;

	@XmlAttribute(name = "fstate")
	private Integer fstate = 0;

	@XmlAttribute(name = "istate")
	private Integer istate = 0;

	@XmlAttribute(name = "mstate")
	private Integer mstate = 0;

	@XmlAttribute(name = "nstate")
	private Integer nstate = 0;

	@XmlAttribute(name = "ostate")
	private Integer ostate = 0;

	@XmlAttribute(name = "pstate")
	private Integer pstate = 0;

	@XmlAttribute(name = "rstate")
	private Integer rstate = 0;

	@XmlAttribute(name = "tstate")
	private Integer tstate = 0;

	@XmlAttribute(name = "zstate")
	private Integer zstate = 0;

	@XmlAttribute(name = "iustate")
	private Integer iustate = 0;

	@XmlAttribute(name = "opstate")
	private Integer opstate = 0;

	@XmlAttribute(name = "anchor")
	private String anchor;

	@XmlAttribute(name = "fly")
	private Integer fly = 0;

	@XmlAttribute(name = "walker_index")
	private Integer walkerIdx;

	@XmlAttribute(name = "walker_id")
	private String walkerId;

	@XmlAttribute(name = "alternate_id")
	private String alternateIdValues;
	
	@XmlAttribute(name = "select_prob")
	private String selectprobValues;

	@XmlAttribute(name = "random_walk")
	private Integer randomWalk = 0;

	@XmlAttribute(name = "entity_id")
	private Integer entityId = 0;

	@XmlAttribute(name = "h", required = true)
	private byte h;

	@XmlAttribute(name = "z", required = true)
	private float z;

	@XmlAttribute(name = "y", required = true)
	private float y;

	@XmlAttribute(name = "x", required = true)
	private float x;

	@XmlElement(name = "temporary_spawn")
	private TemporarySpawn temporaySpawn;

	@XmlElement(name = "model")
	private SpawnModel model;
	private static final Integer ZERO = Integer.valueOf(0);

	public SpawnSpotTemplate() {
	}

	void beforeMarshal(Marshaller marshaller) {
		if (ZERO.equals(entityId)) {
			entityId = null;
		}
		if (ZERO.equals(fly)) {
			fly = null;
		}
		if (ZERO.equals(randomWalk)) {
			randomWalk = null;
		}
		if (ZERO.equals(state)) {
			state = null;
		}
		if (ZERO.equals(astate)) {
			astate = null;
		}
		if (ZERO.equals(bstate)) {
			bstate = null;
		}
		if (ZERO.equals(cstate)) {
			cstate = null;
		}
		if (ZERO.equals(dstate)) {
			dstate = null;
		}
		if (ZERO.equals(estate)) {
			estate = null;
		}
		if (ZERO.equals(fstate)) {
			fstate = null;
		}
		if (ZERO.equals(istate)) {
			istate = null;
		}
		if (ZERO.equals(mstate)) {
			mstate = null;
		}
		if (ZERO.equals(nstate)) {
			nstate = null;
		}
		if (ZERO.equals(ostate)) {
			ostate = null;
		}
		if (ZERO.equals(pstate)) {
			pstate = null;
		}
		if (ZERO.equals(rstate)) {
			rstate = null;
		}
		if (ZERO.equals(tstate)) {
			tstate = null;
		}
		if (ZERO.equals(zstate)) {
			zstate = null;
		}
		if (ZERO.equals(iustate)) {
			iustate = null;
		}
		if (ZERO.equals(opstate)) {
			opstate = null;
		}
		if (ZERO.equals(walkerIdx)) {
			walkerIdx = null;
		}
	}

	void afterMarshal(Marshaller marshaller) {
		if (entityId == null) {
			entityId = 0;
		}
		if (fly == null) {
			fly = 0;
		}
		if (randomWalk == null) {
			randomWalk = 0;
		}
		if (state == null) {
			state = 0;
		}
		if (astate == null) {
			astate = 0;
		}
		if (bstate == null) {
			bstate = 0;
		}
		if (cstate == null) {
			cstate = 0;
		}
		if (dstate == null) {
			dstate = 0;
		}
		if (estate == null) {
			estate = 0;
		}
		if (fstate == null) {
			fstate = 0;
		}
		if (istate == null) {
			istate = 0;
		}
		if (mstate == null) {
			mstate = 0;
		}
		if (nstate == null) {
			nstate = 0;
		}
		if (ostate == null) {
			ostate = 0;
		}
		if (pstate == null) {
			pstate = 0;
		}
		if (rstate == null) {
			rstate = 0;
		}
		if (tstate == null) {
			tstate = 0;
		}
		if (zstate == null) {
			zstate = 0;
		}
		if (iustate == null) {
			iustate = 0;
		}
		if (opstate == null) {
			opstate = 0;
		}
		if (walkerIdx == null) {
			walkerIdx = 0;
		}
	}

	public SpawnSpotTemplate(float x, float y, float z, byte h, int randomWalk, String walkerId, Integer walkerIndex) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.h = h;
		if (randomWalk > 0) {
			this.randomWalk = randomWalk;
		}
		this.walkerId = walkerId;
		this.walkerIdx = walkerIndex;
	}

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 返回 heading / Returns the heading */
	public byte getHeading() {
		return h;
	}

	/** 返回 entity id / Returns the entity id */
	public int getEntityId() {
		return entityId;
	}

	/** 设置 entity id / Sets the entity id */
	public void setEntityId(int entityId) {
		this.entityId = entityId;
	}

	/** 返回 walker id / Returns the walker id */
	public String getWalkerId() {
		return walkerId;
	}

	/** 设置 walker id / Sets the walker id */
	public void setWalkerId(String walkerId) {
		this.walkerId = walkerId;
	}

	/** 返回 walker index / Returns the walker index */
	public int getWalkerIndex() {
		if (walkerIdx == null) {
			return 0;
		}
		return walkerIdx;
	}

	/** 返回 random walk / Returns the random walk */
	public int getRandomWalk() {
		return randomWalk;
	}

	/** 获取飞行。 / Returns the fly. */
	public int getFly() {
		return fly;
	}

	/** 返回 anchor / Returns the anchor */
	public String getAnchor() {
		return anchor;
	}

	/** 返回 model / Returns the model */
	public SpawnModel getModel() {
		return model;
	}

	/** 获取状态。 / Returns the state. */
	public int getState() {
		if (state == null) {
			return 0;
		}
		return state;
	}

	/** 返回状态 / Returns the a state */
	public int getAState() {
		if (astate == null) {
			return 0;
		}
		return astate;
	}

	/** 返回 b state / Returns the b state */
	public int getBState() {
		if (bstate == null) {
			return 0;
		}
		return bstate;
	}

	/** 返回 c state / Returns the c state */
	public int getCState() {
		if (cstate == null) {
			return 0;
		}
		return cstate;
	}

	/** 返回 d state / Returns the d state */
	public int getDState() {
		if (dstate == null) {
			return 0;
		}
		return dstate;
	}

	/** 返回 e state / Returns the e state */
	public int getEState() {
		if (estate == null) {
			return 0;
		}
		return estate;
	}

	/** 返回 f state / Returns the f state */
	public int getFState() {
		if (fstate == null) {
			return 0;
		}
		return fstate;
	}

	/** 返回 i state / Returns the i state */
	public int getIState() {
		if (istate == null) {
			return 0;
		}
		return istate;
	}

	/** 返回 m state / Returns the m state */
	public int getMState() {
		if (mstate == null) {
			return 0;
		}
		return mstate;
	}

	/** 返回 n state / Returns the n state */
	public int getNState() {
		if (nstate == null) {
			return 0;
		}
		return nstate;
	}

	/** 返回 o state / Returns the o state */
	public int getOState() {
		if (ostate == null) {
			return 0;
		}
		return ostate;
	}

	/** 返回 p state / Returns the p state */
	public int getPState() {
		if (pstate == null) {
			return 0;
		}
		return pstate;
	}

	/** 返回 r state / Returns the r state */
	public int getRState() {
		if (rstate == null) {
			return 0;
		}
		return rstate;
	}

	/** 返回 t state / Returns the t state */
	public int getTState() {
		if (tstate == null) {
			return 0;
		}
		return tstate;
	}

	/** 返回 z state / Returns the z state */
	public int getZState() {
		if (zstate == null) {
			return 0;
		}
		return zstate;
	}

	/** 返回 iu state / Returns the iu state */
	public int getIUState() {
		if (iustate == null) {
			return 0;
		}
		return iustate;
	}

	/** 返回 op state / Returns the op state */
	public int getOPState() {
		if (opstate == null) {
			return 0;
		}
		return opstate;
	}
	
	/** 返回 alternate ids / Returns the alternate ids */
	public String getAlternateIds() {
		return alternateIdValues;
	}
	
	/** 返回 select probs / Returns the select probs */
	public String getSelectProbs() {
		return selectprobValues;
	}

	/** 返回临时刷新 / Returns the temporary spawn*/
	public TemporarySpawn getTemporarySpawn() {
		return temporaySpawn;
	}
}
