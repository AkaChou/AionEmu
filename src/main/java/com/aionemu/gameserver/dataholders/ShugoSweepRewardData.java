package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.shugosweep.ShugoSweepReward;

/**
 * 术古扫荡奖励数据容器，按棋盘 ID 与奖励序号复合键索引。
 * Shugo Sweep reward data holder, indexed by composite board id and reward number key.
 *
 * Created by Wnkrz on 23/10/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "ShugoSweepRewardData" })
@XmlRootElement(name = "shugo_sweeps")
public class ShugoSweepRewardData {
	@XmlElement(name = "shugo_sweep")
	protected List<ShugoSweepReward> ShugoSweepRewardData;

	@XmlTransient
	protected List<ShugoSweepReward> ShugoSweepRewardList = new ArrayList<ShugoSweepReward>();
	@XmlTransient
	private Map<Long, ShugoSweepReward> rewardsByBoardAndNum = new HashMap<Long, ShugoSweepReward>();

	/**
	 * JAXB 反序列化完成后，写入列表与复合键索引并释放 XML 列表。
	 * After JAXB unmarshalling, populates the list and composite-key index, then releases the XML list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (ShugoSweepReward reward : ShugoSweepRewardData) {
			ShugoSweepRewardList.add(reward);
			rewardsByBoardAndNum.putIfAbsent(rewardKey(reward.getBoardId(), reward.getRewardNum()), reward);
		}
		ShugoSweepRewardData.clear();
		ShugoSweepRewardData = null;
	}

	/**
	 * 按棋盘 ID 与奖励序号获取奖励。
	 * Returns the reward for the given board id and reward number.
	 *
	 * board id
	 * reward number
	 * @return 奖励，不存在则为 null / reward or null
	 */
	public ShugoSweepReward getRewardBoard(int boardId, int rewardNum) {
		return rewardsByBoardAndNum.get(rewardKey(boardId, rewardNum));
	}

	/**
	 * 返回已加载的奖励数量。
	 * Returns the number of loaded rewards.
	 *
	 * reward count
	 */
	public int size() {
		return ShugoSweepRewardList.size();
	}

	private static long rewardKey(int boardId, int rewardNum) {
		return ((long) boardId << 32) ^ (rewardNum & 0xffffffffL);
	}
}
