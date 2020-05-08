package dev.kylejulian.tws.afk.events;

import java.util.UUID;

/**
 * 
 * Event to Fire when Players go AFK
 *
 */
public class AfkCancelledEvent extends AfkEventBase {
	
	public AfkCancelledEvent(UUID playerId) {
		super(playerId);
	}
}
