package dev.kylejulian.tws.afk.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

abstract class AfkEventBase extends Event {
	
	private static final HandlerList HANDLERS = new HandlerList();
	private final UUID playerId;
	
	public AfkEventBase(UUID playerId) {
		this.playerId = playerId;
	}
	
	public UUID getPlayerId() {
		return this.playerId;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
}