package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.templates.survey.SurveyItem;
import com.aionemu.gameserver.persistence.dao.SurveyDao;
import com.aionemu.gameserver.persistence.entity.SurveyPo;
import com.aionemu.gameserver.persistence.mapper.SurveyMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 问卷控制器数据访问实现。
 * Data access implementation for survey controller items.
 */
@Slf4j
@Service
@Lazy
public class SurveyDaoImpl extends ServiceImpl<SurveyMapper, SurveyPo> implements SurveyDao {

	/** 未使用的问卷标记。 / Flag of an unused survey. */
	private static final int UNUSED = 0;

	@Override
	public List<SurveyItem> getAllNew() {
		List<SurveyItem> list = new ArrayList<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(SurveyPo::getUsed).eq(UNUSED)).forEach(po -> {
				SurveyItem item = new SurveyItem();
				item.uniqueId = po.getUniqueId();
				item.ownerId = po.getOwnerId();
				item.itemId = po.getItemId();
				item.count = po.getItemCount();
				item.html = po.getHtmlText();
				item.radio = po.getHtmlRadio();
				list.add(item);
			});
		} catch (Exception e) {
			log.warn(I18n.get("log.05d62bb9dc4d"), e);
		}
		return list;
	}

	@Override
	public boolean useItem(int id) {
		try {
			return mapper.markUsed(1, id) > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.4f720321ff23"), e);
			return false;
		}
	}
}
