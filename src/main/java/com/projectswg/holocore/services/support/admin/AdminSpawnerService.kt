/***********************************************************************************
 * Copyright (c) 2020 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/

package com.projectswg.holocore.services.support.admin

import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class AdminSpawnerService : Service() {
	
	private val outputFile = File("log/spawners.txt")
	private var outputStream: OutputStreamWriter? = null
	
	override fun start(): Boolean {
		outputStream = outputFile.writer(StandardCharsets.UTF_8)
		return true
	}
	
	override fun stop(): Boolean {
		outputStream?.close()
		outputStream = null
		return true
	}
	
	@IntentHandler
	private fun handleChatCommandIntent(eci: ExecuteCommandIntent) {
		Log.d("Command: %s", eci.command.name)
	}
	
}
