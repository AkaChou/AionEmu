package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.zone.Point2D;

/**
 * 巡逻队：按队形排列成员、同步路径步骤并协同行走。
 * Walker group: arranges members by formation, syncs route steps and walks together.
 *
 * @author vlog
 * @modified Rolandas
 */
@Slf4j
public class WalkerGroup {

	/**
	 * 编队成员（按 walkerIndex 排序）。
	 * Formation members (sorted by walkerIndex).
	 */
	private List<ClusteredNpc> members;

	/**
	 * 队形类型。
	 * Formation type.
	 */
	private WalkerGroupType type;

	/**
	 * 基准点 X。
	 * Anchor X of the group.
	 */
	private float walkerXpos;

	/**
	 * 基准点 Y。
	 * Anchor Y of the group.
	 */
	private float walkerYpos;

	/**
	 * 各成员当前路径步骤。
	 * Current route step per member.
	 */
	private int[] memberSteps;

	/**
	 * 编队整体当前步骤。
	 * Current group-wide route step.
	 */
	private volatile int groupStep;

	/**
	 * 以成员列表构造巡逻队并初始化基准点与队形。
	 * Builds a walker group from members and initializes anchor and type.
	 *
	 * @param members 集群成员列表 / clustered members
	 */
	public WalkerGroup(List<ClusteredNpc> members) {
		this.members = members;
		Collections.sort(this.members, new Comparator<ClusteredNpc>() {
			@Override
			public int compare(ClusteredNpc o1, ClusteredNpc o2) {
				return Integer.compare(o1.getWalkerIndex(), o2.getWalkerIndex());
			}
		});
		memberSteps = new int[members.size()];
		walkerXpos = members.get(0).getX();
		walkerYpos = members.get(0).getY();
		type = members.get(0).getWalkTemplate().getType();
	}

	/**
	 * 按队形类型计算并写入各成员站位偏移。
	 * Computes and applies per-member standing offsets for the formation type.
	 */
	public void form() {
		if (getWalkType() == WalkerGroupType.SQUARE) {
			int[] rows = members.get(0).getWalkTemplate().getRows();
			if (sumRows(rows) != members.size()) {
				log.warn(I18n.get("log.d3b5ea5b2bb4", members.get(0).getWalkTemplate().getRouteId()));
			}
			if (rows.length == 1) {
				// 一字队形：彼此间距 2 米。 / Line formation: distance 2 meters from each other (divide by 2 and multiple
				// by 2)
				// 左手为负、右手为正 / negative at left hand and positive at the right hand
				float bounds = sumMemberBoundSides();
				float distance = (1 - members.size()) / 2f * (WalkerGroupShift.DISTANCE + bounds);
				Point2D origin = new Point2D(walkerXpos, walkerYpos);
				Point2D destination = new Point2D(members.get(0).getWalkTemplate().getRouteStep(2).getX(),
						members.get(0).getWalkTemplate().getRouteStep(2).getY());
				for (int i = 0; i < members.size(); i++, distance += WalkerGroupShift.DISTANCE) {
					WalkerGroupShift shift = new WalkerGroupShift(distance, 0);
					Point2D loc = getLinePoint(origin, destination, shift);
					members.get(i).setX(loc.getX());
					members.get(i).setY(loc.getY());
					Npc member = members.get(i).getNpc();
					member.setWalkerGroup(this);
					member.setWalkerGroupShift(shift);
					// distance += npc.getObjectTemplate().getBoundRadius().getSide();
				}
			} else if (rows.length != 0) {
				float rowDistances[] = new float[rows.length - 1];
				float coronalDist = 0;
				for (int i = 0; i < rows.length - 1; i++) {
					if (rows[i] % 2 != rows[i + 1] % 2) {
						rowDistances[i] = 0.86602540378443864676372317075294f * WalkerGroupShift.DISTANCE;
					} else {
						rowDistances[i] = WalkerGroupShift.DISTANCE;
					}
					coronalDist -= rowDistances[i];
				}
				Point2D origin = new Point2D(walkerXpos, walkerYpos);
				Point2D destination = new Point2D(members.get(0).getWalkTemplate().getRouteStep(2).getX(),
						members.get(0).getWalkTemplate().getRouteStep(2).getY());
				int index = 0;
				for (int i = 0; i < rows.length; i++) {
					float sagittalDist = (1 - rows[i]) / 2f * WalkerGroupShift.DISTANCE;
					for (int j = 0; j < rows[i]; j++, sagittalDist += WalkerGroupShift.DISTANCE) {
						if (index > members.size() - 1)
							break;
						WalkerGroupShift shift = new WalkerGroupShift(sagittalDist, coronalDist);
						Point2D loc = getLinePoint(origin, destination, shift);
						ClusteredNpc cnpc = members.get(index++);
						cnpc.setX(loc.getX());
						cnpc.setY(loc.getY());
						cnpc.getNpc().setWalkerGroup(this);
						cnpc.getNpc().setWalkerGroupShift(shift);
					}
					if (i < rows.length - 1)
						coronalDist += rowDistances[i];
				}
			}
		} else if (getWalkType() == WalkerGroupType.OFFSET) {
			int[] offsetsX = members.get(0).getWalkTemplate().getoffsetsX();
			int[] offsetsY = members.get(0).getWalkTemplate().getoffsetsY();
			Point2D origin = new Point2D(walkerXpos, walkerYpos);
			Point2D destination = new Point2D(members.get(0).getWalkTemplate().getRouteStep(2).getX(), members.get(0).getWalkTemplate().getRouteStep(2).getY());
			for (int i =0; i< members.size(); i++)
			{
				WalkerGroupShift shift = new WalkerGroupShift(offsetsX[i], offsetsY[i]);
				Point2D loc = getLinePoint(origin, destination, shift);
				members.get(i).setX(loc.getX());
				members.get(i).setY(loc.getY());
				Npc member = members.get(i).getNpc();
				member.setWalkerGroup(this);
				member.setWalkerGroupShift(shift);
			}
		}else if (getWalkType() == WalkerGroupType.POINT) {
			log.warn(I18n.get("log.225af1e9aeb7", members.get(0).getWalkTemplate().getRouteId()));
		}
	}

	/**
	 * 行数数组求和。
	 * Sums row counts.
	 *
	 * @param rows 各行人数 / members per row
	 * @return 总人数 / the sum
	 */
	private int sumRows(int[] rows) {
		int sum = 0;
		for (int row : rows) {
			sum += row;
		}
		return sum;
	}

	/**
	 * 成员碰撞体侧边长度之和。
	 * Sum of member bound-radius side lengths.
	 *
	 * @return 侧边长度之和 / sum of sides
	 */
	private float sumMemberBoundSides() {
		float sum = 0;
		for (ClusteredNpc member : members) {
			sum += member.getNpc().getObjectTemplate().getBoundRadius().getSide();
		}
		return sum;
	}

	/**
	 * 行间额外间距（预留，当前恒为 0）。
	 * Extra inter-row spacing (stub; currently always 0).
	 *
	 * @param rows 行数数组 / the row counts
	 * @param startIndex 起始行下标 / start row index
	 * @param endIndex 结束行下标 / end row index
	 * @return 额外间距 / extra distance
	 */
	@SuppressWarnings("unused")
	private float getSidesExtra(int[] rows, int startIndex, int endIndex) {
		return 0;
	}

	/**
	 * 由初始点、下一路径点与偏移计算 2D 站位坐标。
	 * Computes 2D standing coordinates from origin, next route point and shift.
	 *
	 * @param origin 初始刷怪点 / initial spawn location
	 * @param destination 下一移动点 / next move point
	 * @param shift 相对 origin 的垂直偏移：矢状负为左、正为右；冠状负为后、正为前 / offset perpendicular to destination; sagittal negative=left, coronal negative=back
	 * @return 计算后的点 / computed point
	 */
	public static Point2D getLinePoint(Point2D origin, Point2D destination, WalkerGroupShift shift) {
		WalkerGroupShift dir = getShiftSigns(origin, destination);
		Point2D result = null;
		if (origin.getY() - destination.getY() == 0) {
			return new Point2D(origin.getX() + dir.getCoronalShift() * shift.getCoronalShift(),
					origin.getY() - dir.getSagittalShift() * shift.getSagittalShift());
		} else if (origin.getX() - destination.getX() == 0) {
			return new Point2D(origin.getX() + dir.getCoronalShift() * shift.getSagittalShift(),
					origin.getY() + dir.getCoronalShift() * shift.getCoronalShift());
		} else {
			double slope = (origin.getX() - destination.getX()) / (origin.getY() - destination.getY());
			double dx = Math.abs(shift.getSagittalShift()) / Math.sqrt(1 + slope * slope);
			if (shift.getSagittalShift() * dir.getCoronalShift() < 0) {
				result = new Point2D((float) (origin.getX() - dx), (float) (origin.getY() + dx * slope));
			} else {
				result = new Point2D((float) (origin.getX() + dx), (float) (origin.getY() - dx * slope));
			}
		}
		if (shift.getCoronalShift() != 0) {
			Point2D rotatedShift = null;
			if (shift.getSagittalShift() != 0) {
				rotatedShift = getLinePoint(origin, destination, new WalkerGroupShift(
						Math.signum(shift.getSagittalShift()) * Math.abs(shift.getCoronalShift()), 0));
			} else {
				rotatedShift = getLinePoint(origin, destination,
						new WalkerGroupShift(Math.abs(shift.getCoronalShift()), 0));
			}

			// 因已旋转且垂直，未旋转时 dx/dy 互为倒数。 / since it's rotated, and perpendicular, dx and dy are reciprocal when not
			// 已旋转 / rotated
			float dx = Math.abs(origin.getX() - rotatedShift.getX());
			float dy = Math.abs(origin.getY() - rotatedShift.getY());
			if (shift.getCoronalShift() < 0) {
				if (dir.getSagittalShift() < 0 && dir.getCoronalShift() < 0) {
					result = new Point2D(result.getX() + dy, result.getY() + dx);
				} else if (dir.getSagittalShift() > 0 && dir.getCoronalShift() > 0) {
					result = new Point2D(result.getX() - dy, result.getY() - dx);
				} else if (dir.getSagittalShift() < 0 && dir.getCoronalShift() > 0) {
					result = new Point2D(result.getX() + dy, result.getY() - dx);
				} else if (dir.getSagittalShift() > 0 && dir.getCoronalShift() < 0) {
					result = new Point2D(result.getX() - dy, result.getY() + dx);
				}
			} else {
				if (dir.getSagittalShift() < 0 && dir.getCoronalShift() < 0) {
					result = new Point2D(result.getX() - dy, result.getY() - dx);
				} else if (dir.getSagittalShift() > 0 && dir.getCoronalShift() > 0) {
					result = new Point2D(result.getX() + dy, result.getY() + dx);
				} else if (dir.getSagittalShift() < 0 && dir.getCoronalShift() > 0) {
					result = new Point2D(result.getX() - dy, result.getY() + dx);
				} else if (dir.getSagittalShift() > 0 && dir.getCoronalShift() < 0) {
					result = new Point2D(result.getX() + dy, result.getY() - dx);
				}
			}
		}
		return result;
	}

	/**
	 * 返回归一化方向符号向量。
	 * Returns a normalized direction sign vector.
	 *
	 * @param origin 起始点 / origin point
	 * @param destination 目标点 / destination point
	 * @return 方向偏移符号 / direction shift signs
	 */
	private static WalkerGroupShift getShiftSigns(Point2D origin, Point2D destination) {
		float dx = Math.signum(destination.getX() - origin.getX());
		float dy = Math.signum(destination.getY() - origin.getY());
		return new WalkerGroupShift(dx, dy);
	}

	/**
	 * 更新成员路径步骤，并在必要时推进编队整体步骤。
	 * Updates a member's route step and advances the group step when appropriate.
	 *
	 * @param member 成员 NPC / the member NPC
	 * @param step 新步骤 / the new step
	 */
	public void setStep(Npc member, int step) {
		int currentStep = 0;
		for (int i = 0; i < members.size(); i++) {
			if (memberSteps[i] > currentStep) {
				currentStep = memberSteps[i];
			}
			if (members.get(i).getNpc().equals(member)) {
				AI2Logger.info(members.get(i).getNpc().getAi2(), "Setting step to " + step);
				memberSteps[i] = step;
			}
		}
		if (step > currentStep || step == 1) {
			groupStep = step;
		}
	}

	/**
	 * 成员到达目标后的编队同步：等待全员到齐再统一推进。
	 * Sync after a member reaches a target: wait for all, then advance together.
	 *
	 * @param npcAI 到达目标的成员 AI / the arriving member AI
	 */
	public void targetReached(NpcAI2 npcAI) {
		synchronized (members) {
			npcAI.setSubStateIfNot(AISubState.WALK_WAIT_GROUP);
			boolean allArrived = true;
			for (ClusteredNpc snpc : members) {
				allArrived &= snpc.getNpc().getAi2().getSubState() == AISubState.WALK_WAIT_GROUP;
				if (!allArrived) {
					break;
				}
			}

			for (int i = 0; i < members.size(); i++) {
				ClusteredNpc snpc = members.get(i);
				if ((memberSteps[i] == groupStep) && !allArrived) {
					npcAI.getOwner().getMoveController().pauseAtRoutePoint();
					npcAI.setStateIfNot(AIState.WALKING);
					npcAI.setSubStateIfNot(AISubState.WALK_WAIT_GROUP);
					continue;
				}
				npcAI = (NpcAI2) (snpc.getNpc().getAi2());
				WalkManager.targetReached(npcAI);
			}
		}
	}

	/**
	 * 将所有成员刷入世界。
	 * Spawns all members into the world.
	 */
	public void spawn() {
		for (ClusteredNpc snpc : members) {
			float height = getHeight(snpc.getX(), snpc.getY(), snpc.getNpc().getSpawn());
			snpc.spawn(height);
		}
	}

	/**
	 * 将重生 NPC 绑定回编队对应槽位并重置步骤。
	 * Rebinds a respawned NPC into its formation slot and resets its step.
	 *
	 * @param npc 重生的 NPC / the respawned NPC
	 */
	public void respawn(Npc npc) {
		for (int index = 0; index < members.size(); index++) {
			ClusteredNpc snpc = members.get(index);
			if (snpc.getWalkerIndex() == npc.getSpawn().getWalkerIndex()
					&& snpc.getNpc().getNpcId() == npc.getNpcId()) {
				synchronized (members) {
					snpc.setNpc(npc);
					memberSteps[index] = 1;
				}
				break;
			}
		}
	}

	/**
	 * 按 NPC 查找对应的集群数据。
	 * Finds clustered data for the given NPC.
	 *
	 * @param npc 成员 NPC / the member NPC
	 * @return 集群数据，未找到则为 null / clustered data or null
	 */
	public ClusteredNpc getClusterData(Npc npc) {
		for (ClusteredNpc snpc : members) {
			if (snpc.getNpc().equals(npc)) {
				return snpc;
			}
		}
		return null;
	}

	/**
	 * 解析刷怪高度（当前直接使用模板 Z）。
	 * Resolves spawn height (currently uses template Z).
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param template 刷怪模板 / spawn template
	 * @return 高度 Z / height Z
	 */
	private float getHeight(float x, float y, SpawnTemplate template) {
		/*
		 * if (GameWorldServices.geoService().isGeoOn()) { return
		 * GameWorldServices.geoService().getZ(template.getWorldId(), x, y, z, ); }
		 */
		return template.getZ();
	}

	/**
	 * 编队人数（池大小）。
	 * Formation size (pool).
	 *
	 * @return 成员数量 / member count
	 */
	public int getPool() {
		return members.size();
	}

	/**
	 * 编队类型 / formation type
	 */
	public WalkerGroupType getWalkType() {
		return type;
	}

	/**
	 * 判断 NPC 是否处于单行（线性）方阵站位。
	 * Whether the NPC is in a single-row (linear) square formation.
	 *
	 * @param npc 成员 NPC / the member NPC
	 * @return 线性站位则为 true / true if linearly positioned
	 */
	public boolean isLinearlyPositioned(Npc npc) {
		if (type != WalkerGroupType.SQUARE) {
			return false;
		}
		for (ClusteredNpc snpc : members) {
			if (snpc.getNpc().equals(npc)) {
				return snpc.getWalkTemplate().getRows().length == 1;
			}
		}
		return false;
	}

	/**
	 * @return 编队当前路径步骤 / current group route step
	 */
	public int getGroupStep() {
		return groupStep;
	}
}
