package dev.kylejulian.tws.afk.events;

import java.util.UUID;

public class AfkCommandEvent extends AfkEventBase {
	
	public AfkCommandEvent(UUID playerId) {
		super(playerId);
	}
}