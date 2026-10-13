package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.persistence.dao.LimitedQuestCounterDao;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 全服共享限量任务名额服务。 / Shared limited quest-slot service across the server.
 */
@Slf4j
public final class LimitedQuestService {

	private static final String DEFINITIONS_FILE = "./definitions/compact/quests/limited-quests.xml";

	private static volatile LimitedQuestService instance;
	private final LimitedQuestCounterDao dao;
	private final Map<Integer, Limit> limits;

	public LimitedQuestService(LimitedQuestCounterDao dao) {
		this(dao, loadLimitsSafe());
	}

	LimitedQuestService(LimitedQuestCounterDao dao, Map<Integer, Limit> limits) {
		this.dao = Objects.requireNonNull(dao, "dao must not be null");
		this.limits = Map.copyOf(limits);
		instance = this;
	}

	public static LimitedQuestService getInstance() {
		LimitedQuestService current = instance;
		if (current == null) {
			throw new IllegalStateException("LimitedQuestService 未由 Spring 提供（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return current;
	}

	/**
	 * 在任务状态写入前原子占用一个全服名额。 / Atomically occupies one server-wide slot before the quest state is written.
	 */
	public static boolean tryAcquire(QuestTemplate template) {
		return getInstance().acquire(template.getId(), template.getMaxCountLimitedQuest());
	}

	/**
	 * Canonical metadata entry point used by typed quest-start eligibility.
	 */
	public static boolean requiresAcquisitionByMetadata(int questId, int maxCount) {
		return getInstance().requiresAcquisition(questId, maxCount);
	}

	/**
	 * 真实 AI 的 charge_limitedquest：恢复默认数量或直接充满。
	 */
	public static boolean charge(int questId, boolean chargeMaxCount) {
		return getInstance().chargeConfigured(questId, chargeMaxCount);
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
			QuestMetadata metadata = QuestEngine.getInstance().questCatalog().findMetadata(questId).orElse(null);
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

	static Map<Integer, Limit> loadLimitsSafe() {
		try {
			File file = Config.definitionFile(DEFINITIONS_FILE);
			if (file.exists()) {
				return loadLimits(file);
			}
		} catch (Exception e) {
			log.warn(I18n.get("log.4dfe90006810", e.getMessage()), e);
		}
		return Map.of();
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
}
