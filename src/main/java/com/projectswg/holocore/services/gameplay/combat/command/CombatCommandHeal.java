/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.RGB;
import com.projectswg.common.data.combat.*;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectLocMessage;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatAction;

enum CombatCommandHeal implements CombatCommandHitType {
	INSTANCE;
	
	@Override
	public void handle(CreatureObject source, SWGObject target, CombatCommand combatCommand, String arguments) {
		int healAmount = combatCommand.getAddedDamage();
		int healingPotency = source.getSkillModValue("expertise_healing_all");
		
		if (healingPotency > 0) {
			healAmount *= healingPotency;
		}
		
		switch (combatCommand.getAttackType()) {
			case SINGLE_TARGET: {
				switch (combatCommand.getTargetType()) {
					case NONE: {    // No target used, always heals self
						doHeal(source, source, healAmount, combatCommand);
						break;
					}
					case REQUIRED: {    // Target is always used
						if (target == null) {
							return;
						}
						
						// Same logic as OPTIONAL and ALL, so no break!
					}
					case OPTIONAL:    // Appears to be the same as ALL
					case ALL: {    // Target is used IF supplied
						if (target != null) {
							if (!(target instanceof CreatureObject)) {
								return;
							}
							
							CreatureObject creatureTarget = (CreatureObject) target;
							
							if (source.isAttackable(creatureTarget)) {
								doHeal(source, source, healAmount, combatCommand);
							} else {
								doHeal(source, creatureTarget, healAmount, combatCommand);
							}
						} else {
							doHeal(source, source, healAmount, combatCommand);
						}
						
						break;
					}
				}
				break;
			}
			
			case AREA: {
				// Targets are never supplied for AoE heals
				double range = combatCommand.getConeLength();
				Location sourceLocation = source.getWorldLocation();
				
				for (SWGObject nearbyObject : source.getAware()) {
					if (sourceLocation.isWithinDistance(nearbyObject.getLocation(), range)) {
						if (!(nearbyObject instanceof CreatureObject)) {
							// We can't heal something that's not a creature
							continue;
						}
						
						CreatureObject nearbyCreature = (CreatureObject) nearbyObject;
						
						if (source.isAttackable(nearbyCreature)) {
							// Don't heal (potential) enemies
							continue;
						}

						if (nearbyCreature.hasOptionFlags(OptionFlag.INVULNERABLE)) {
							// Don't heal creatures that can't take damage
							continue;
						}

						// Heal nearby friendly
						doHeal(source, nearbyCreature, healAmount, combatCommand);
					}
				}
				
				break;
			}
		}
	}
	
	private void doHeal(CreatureObject healer, CreatureObject healed, int healAmount, CombatCommand combatCommand) {
		String attribName;
		int difference;
		
		switch (combatCommand.getHealAttrib()) {
			case HEALTH: {
				int currentHealth = healed.getHealth();
				healed.modifyHealth(healAmount);
				difference = healed.getHealth() - currentHealth;
				attribName = "HEALTH";
				healed.sendObservers(createCombatSpam(healer, healed, difference));
				break;
			}
			
			case ACTION: {
				int currentAction = healed.getAction();
				healed.modifyAction(healAmount);
				difference = healed.getAction() - currentAction;
				attribName = "ACTION";
				break;
			}
			
			default:
				return;
		}
		
		WeaponObject weapon = healer.getEquippedWeapon();
		CombatAction combatAction = createCombatAction(healer, weapon, TrailLocation.RIGHT_HAND, combatCommand);
		combatAction.addDefender(new Defender(healed.getObjectId(), healed.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		
		OutOfBandPackage oobp = new OutOfBandPackage(new ProsePackage("StringId", new StringId("healing", "heal_fly"), "DI", difference, "TO", attribName));
		ShowFlyText flyText = new ShowFlyText(healed.getObjectId(), oobp, Scale.MEDIUM, new RGB(46, 139, 87), ShowFlyText.Flag.IS_HEAL);
		String commandName = combatCommand.getName();
		SWGPacket effect;
		
		if (commandName.startsWith("me_bacta_bomb_")) {
			SWGObject healedParent = healed.getParent();
			long cellId = healedParent != null ? healedParent.getObjectId() : 0;
			effect = new PlayClientEffectLocMessage("clienteffect/bacta_bomb.cef", healed.getTerrain(), healed.getLocation().getPosition(), cellId, 0, "");
		} else if (commandName.startsWith("me_bacta_grenade_")) {
			SWGObject healedParent = healed.getParent();
			long cellId = healedParent != null ? healedParent.getObjectId() : 0;
			effect = new PlayClientEffectLocMessage("clienteffect/bacta_grenade.cef", healed.getTerrain(), healed.getLocation().getPosition(), cellId, 0, "");
		} else {
			effect = new PlayClientEffectObjectMessage("appearance/pt_heal.prt", "root", healed.getObjectId(), "");
		}
		
		
		healed.sendObservers(combatAction, flyText, effect);
	}

	private static CombatSpam createCombatSpam(CreatureObject healer, CreatureObject healed, int difference) {
		CombatSpam spam = new CombatSpam(healer.getObjectId());
		
		spam.setAttacker(healer.getObjectId());
		spam.setAttackerPosition(healer.getLocation().getPosition());
		spam.setDefender(healed.getObjectId());
		spam.setDefenderPosition(healed.getLocation().getPosition());
		spam.setInfo(new AttackInfo());
		spam.setDataType((byte) 2);	// 2 means the combat log entry is a specified message
		OutOfBandPackage oobp = new OutOfBandPackage(new ProsePackage("StringId", new StringId("healing", "perform_heal_damage_success"), "TT", healer.getObjectName(), "TO", healed.getObjectName(), "DI", difference));
		spam.setSpamMessage(oobp);
		spam.setSpamType(CombatSpamType.MEDICAL);
		
		return spam;
	}
	
}
