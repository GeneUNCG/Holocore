/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.network;

import java.io.EOFException;

import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;

import network.PacketType;
import network.encryption.Compression;
import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.object_controller.ObjectController;

public class NetworkProtocol {
	
	public NetBuffer encode(Packet p) {
		NetBuffer encoded = p.encode();
		encoded.flip();
		if (encoded.remaining() != encoded.capacity())
			Log.w("Packet %s has invalid array length. Expected: %d  Actual: %d", p, encoded.remaining(), encoded.capacity());
		return preparePacket(encoded);
	}
	
	public boolean canDecode(NetBufferStream buffer) {
		if (buffer.remaining() < 5)
			return false;
		buffer.mark();
		try {
			buffer.getByte();
			int length = buffer.getShort();
			Assert.test(length >= 0);
			buffer.getShort();
			return buffer.remaining() >= length;
		} finally {
			buffer.rewind();
		}
	}
	
	public Packet decode(NetBufferStream buffer) throws EOFException {
		if (buffer.remaining() < 5)
			throw new EOFException("Not enough remaining data for header! Remaining: " + buffer.remaining());
		byte bitfield = buffer.getByte();
		boolean compressed = (bitfield & 0x01) != 0;
		int length = buffer.getShort();
		int decompressedLength = buffer.getShort();
		if (buffer.remaining() < length) {
			buffer.position(buffer.position() - 5);
			throw new EOFException("Not enough remaining data! Remaining: " + buffer.remaining() + "  Length: " + length);
		}
		byte [] pData = buffer.getArray(length);
		if (compressed) {
			pData = Compression.decompress(pData, decompressedLength);
		}
		return processSWG(pData);
	}
	
	private NetBuffer preparePacket(NetBuffer packet) {
		int remaining = packet.remaining();
		NetBuffer data = NetBuffer.allocate(remaining + 5);
		data.addByte(2); // SWG bitmask
		data.addShort(remaining);
		data.addShort(remaining);
		data.add(packet);
		data.flip();
		return data;
	}
	
	private SWGPacket processSWG(byte [] data) throws EOFException {
		if (data.length < 6)
			throw new EOFException("Length too small: " + data.length);
		NetBuffer buffer = NetBuffer.wrap(data);
		buffer.getShort();
		int crc = buffer.getInt();
		buffer.position(0);
		if (crc == ObjectController.CRC) {
			return ObjectController.decodeController(buffer);
		} else {
			SWGPacket packet = PacketType.getForCrc(crc);
			if (packet != null)
				packet.decode(buffer);
			return packet;
		}
	}
	
}
