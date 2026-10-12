package com.aionemu.gameserver.persistence.dao;

import java.util.List;

import com.aionemu.gameserver.model.templates.survey.SurveyItem;
import com.aionemu.gameserver.persistence.entity.SurveyPo;
import com.mybatisflex.core.service.IService;

/**
 * 问卷/调查控制器数据访问层。
 * Data access layer for survey controller item persistence.
 */
public interface SurveyDao extends IService<SurveyPo> {

	/**
	 * 标记调查物品为已使用。
	 * Marks a survey item as used.
	 * @param id 调查物品 ID / survey item id
	 * @return 是否使用成功 / true if used
	 */
	boolean useItem(int id);

	/**
	 * 查询全部新增调查物品。
	 * Returns all new survey items.
	 * @return 调查物品列表 / survey item list
	 */
	List<SurveyItem> getAllNew();
}
