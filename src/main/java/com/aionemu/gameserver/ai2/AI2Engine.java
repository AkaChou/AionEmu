/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.ai2;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.scripting.CompiledScriptLoader;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * @author ATracer
 */
public class AI2Engine implements GameEngine {

	private static volatile ObjectProvider<AI2Engine> instanceProvider;
	private static final Logger log = LoggerFactory.getLogger(AI2Engine.class);
	private final Map<String, Class<? extends AbstractAI>> aiMap = new HashMap<String, Class<? extends AbstractAI>>();

	@Override
	public void load(CountDownLatch progressLatch) {
		log.info("AI2 engine load started");
		AggregatedClassListener acl = new AggregatedClassListener();
		acl.addClassListener(new OnClassLoadUnloadListener());
		acl.addClassListener(new ScheduledTaskClassListener());
		acl.addClassListener(new AI2HandlerClassListener());

		try {
			acl.postLoad(CompiledScriptLoader.load("com.aionemu.gameserver.ai"));
			log.info("Loaded " + aiMap.size() + " AI2.");
			validateScripts();
		} catch (Exception e) {
			throw new GameServerError("Can't initialize ai handlers.", e);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
	}

	@Override
	public void shutdown() {
		log.info("AI2 engine shutdown started");
		aiMap.clear();
		log.info("AI2 engine shutdown complete");
	}

	public void registerAI(Class<? extends AbstractAI> class1) {
		AIName nameAnnotation = class1.getAnnotation(AIName.class);
		if (nameAnnotation != null) {
			aiMap.put(nameAnnotation.value(), class1);
		}
	}

	public final AI2 setupAI(String name, Creature owner) {
		AbstractAI aiInstance = null;
		try {
			aiInstance = aiMap.get(name).getDeclaredConstructor().newInstance();
			aiInstance.setOwner(owner);
			owner.setAi2(aiInstance);
			if (AIConfig.ONCREATE_DEBUG) {
				aiInstance.setLogging(true);
			}
		} catch (Exception e) {
			log.error("[AI2] AI factory error: " + name, e);
		}
		return aiInstance;
	}

	/**
	 * @param aiName
	 * @param owner
	 */
	public void setupAI(AiNames aiName, Npc owner) {
		setupAI(aiName.getName(), owner);
	}

	private void validateScripts() {
		Collection<String> npcAINames = new HashSet<String>();
		for (NpcTemplate npcTemplate : DataManager.NPC_DATA.getNpcData().values()) {
			npcAINames.add(npcTemplate.getAi());
		}
		npcAINames.removeAll(aiMap.keySet());
		if (npcAINames.size() > 0) {
			log.warn("Bad AI names: " + StringUtils.join(npcAINames, ", "));
		}
	}

	public static final AI2Engine getInstance() {
		ObjectProvider<AI2Engine> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	public static void setInstanceProvider(ObjectProvider<AI2Engine> provider) {
		instanceProvider = provider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final AI2Engine instance = new AI2Engine();
	}
}
