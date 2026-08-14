package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dao.LimitedQuestDAO;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/** 全服共享限量任务名额服务。 / Shared limited quest-slot service across the server. */
@Slf4j
public final class LimitedQuestService {

	private static final String DEFINITIONS_FILE = "./definitions/compact/quests/limited-quests.xml";

	private final LimitedQuestDAO dao;
	private final Map<Integer, Limit> limits;

	LimitedQuestService(LimitedQuestDAO dao, Map<Integer, Limit> limits) {
		this.dao = dao;
		this.limits = Map.copyOf(limits);
	}

	/** 在任务状态写入前原子占用一个全服名额。 */
	public static boolean tryAcquire(QuestTemplate template) {
		return Holder.INSTANCE.acquire(template.getId(), template.getMaxCountLimitedQuest());
	}

	/** Whether starting this quest requires the global, transaction-external quota mutation. */
	public static boolean requiresAcquisition(QuestTemplate template) {
		return requiresAcquisitionByMetadata(template.getId(), template.getMaxCountLimitedQuest());
	}

	/** Canonical metadata entry point used by typed quest-start eligibility. */
	public static boolean requiresAcquisitionByMetadata(int questId, int maxCount) {
		return Holder.INSTANCE.requiresAcquisition(questId, maxCount);
	}

	/** 真实 AI 的 charge_limitedquest：恢复默认数量或直接充满。 */
	public static boolean charge(int questId, boolean chargeMaxCount) {
		return Holder.INSTANCE.chargeConfigured(questId, chargeMaxCount);
	}

	boolean acquire(int questId, int templateMax) {
		Limit limit = limits.get(questId);
		if (limit == null && templateMax <= 1) {
			return true;
		}
		return dao.tryAcquire(questId, limit == null ? templateMax : limit.maxCount());
	}

	boolean requiresAcquisition(int questId, int templateMax) {
		return limits.containsKey(questId) || templateMax > 1;
	}

	boolean chargeConfigured(int questId, boolean chargeMaxCount) {
		Limit limit = limits.get(questId);
		if (limit == null) {
			var metadata = GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null);
			if (metadata != null && metadata.maxCountLimitedQuest() > 1) {
				limit = new Limit(metadata.maxCountLimitedQuest(), metadata.countRecoverLimitedQuest());
			}
		}
		if (limit == null) {
			log.warn(I18n.get("log.limited_quest.charge_ignored", questId));
			return false;
		}
		int amount = chargeMaxCount ? limit.maxCount() : limit.recoverCount();
		return dao.recover(questId, amount, limit.maxCount());
	}

	static Map<Integer, Limit> loadLimits(File file) {
		Map<Integer, Limit> result = new HashMap<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !reader.getLocalName().equals("quest")) {
					continue;
				}
				int questId = integerAttribute(reader, "id");
				Limit previous = result.put(questId, new Limit(integerAttribute(reader, "max_count"),
					integerAttribute(reader, "recover_count")));
				if (previous != null) {
					throw new IllegalStateException("Duplicate limited quest definition: " + questId);
				}
			}
			reader.close();
			return result;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load limited quest definitions from " + file.getPath(), e);
		}
	}

	private static int integerAttribute(XMLStreamReader reader, String name) {
		String value = reader.getAttributeValue(null, name);
		if (value == null) {
			throw new IllegalStateException("Missing limited quest attribute: " + name);
		}
		return Integer.parseInt(value);
	}

	record Limit(int maxCount, int recoverCount) {
		Limit {
			if (maxCount <= 1 || recoverCount <= 0) {
				throw new IllegalArgumentException("Invalid limited quest counts: " + maxCount + "/" + recoverCount);
			}
		}
	}

	private static final class Holder {
		private static final LimitedQuestService INSTANCE = create();

		private static LimitedQuestService create() {
			File file = Config.definitionFile(DEFINITIONS_FILE);
			Map<Integer, Limit> limits = loadLimits(file);
			log.info(I18n.get("log.limited_quest.loaded", limits.size(), file.getPath()));
			return new LimitedQuestService(DAOManager.getDAO(LimitedQuestDAO.class), limits);
		}
	}
}
