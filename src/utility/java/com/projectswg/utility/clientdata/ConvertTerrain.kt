/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */

package com.projectswg.utility.clientdata

import com.projectswg.common.data.swgiff.parsers.SWGParser
import com.projectswg.common.data.swgiff.parsers.terrain.TerrainDataParser
import com.projectswg.holocore.utilities.SdbGenerator
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper
import java.io.File
import java.io.IOException
import java.util.*

internal class ConvertTerrain : Converter {
	
	override fun convert() {
		println("Converting terrains...")
		Log.addWrapper(ConsoleLogWrapper())
		try {
			SdbGenerator(File("serverdata/nge/terrain/terrains.sdb")).use { sdb ->
				sdb.writeColumnNames("slotName", "global", "modifiable", "observeWithParent", "exposeToWorld")
				convertFile(sdb, File("clientdata/terrain/tatooine.trn"))
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}
	
	@Throws(IOException::class)
	override fun convertFile(sdb: SdbGenerator, file: File) {
		val clientdata = SWGParser.parse(file) as TerrainDataParser
		Objects.requireNonNull(clientdata, "Failed to load clientdata")
		
	}
}
