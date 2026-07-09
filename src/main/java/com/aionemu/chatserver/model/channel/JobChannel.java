/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.model.Gender;
import com.aionemu.chatserver.model.PlayerClass;
import com.aionemu.chatserver.model.Race;
import lombok.Getter;

/**
 * @author ATracer
 */
public class JobChannel extends RaceChannel {

    @Getter
    private PlayerClass playerClass;
    @Getter
    private Gender gender;

    /**
     * @param gender
     * @param playerClass
     */
    public JobChannel(Gender gender, PlayerClass playerClass, Race race, String identifier) {
        super(ChannelType.JOB, race, identifier);
        this.playerClass = playerClass;
        this.gender = gender;
    }

}
