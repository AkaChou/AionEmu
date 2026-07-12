package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.mail_reward.MailRewardTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 邮件奖励模板数据容器，按奖励 ID 索引 {@link MailRewardTemplate}。
 * Mail reward template data holder, indexing {@link MailRewardTemplate} by reward id.
 *
 * Created by Wnkrz on 26/07/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "reward_mail_templates")
public class MailRewardData {

	@XmlElement(name = "reward_mail_template")
	private List<MailRewardTemplate> RewardMail;

	@XmlTransient
	private IntObjectHashMap<MailRewardTemplate> templates = new IntObjectHashMap<MailRewardTemplate>();

	@XmlTransient
	private Map<Integer, MailRewardTemplate> templatesMap = new HashMap<Integer, MailRewardTemplate>();

	/**
	 * JAXB 反序列化完成后，按奖励 ID 建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by reward id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (MailRewardTemplate template : RewardMail) {
			templates.put(template.getId(), template);
			templatesMap.put(template.getId(), template);
		}
		RewardMail.clear();
		RewardMail = null;
	}

	/**
	 * 返回已加载的邮件奖励数量。
	 * Returns the number of loaded mail rewards.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按奖励 ID 获取邮件奖励模板。
	 * Returns the mail reward template for the given reward id.
	 *
	 * reward id
	 *
	 * @param rewardId
	 * @return 邮件奖励模板或 null / mail reward template or null
	 */
	public MailRewardTemplate getMailReward(int rewardId) {
		return templates.get(rewardId);
	}

	/**
	 * 返回全部邮件奖励映射。
	 * Returns the full mail reward map.
	 *
	 * @return ID 到奖励模板的映射 / map of id to reward template
	 */
	public Map<Integer, MailRewardTemplate> getAll() {
		return templatesMap;
	}
}
