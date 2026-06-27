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
package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.agent.AgentLocation;
import com.aionemu.gameserver.model.templates.agent.AgentTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "agent_fight")
public class AgentData {
	@XmlElement(name = "agent_location")
	private List<AgentTemplate> agentTemplates;

	@XmlTransient
	private Map<Integer, AgentLocation> agent = new LinkedHashMap<Integer, AgentLocation>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AgentTemplate template : agentTemplates) {
			agent.put(template.getId(), new AgentLocation(template));
		}
	}

	public int size() {
		return agent.size();
	}

	public Map<Integer, AgentLocation> getAgentLocations() {
		return agent;
	}
}