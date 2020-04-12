package tws.management.data.entities;

import java.util.ArrayList;
import java.util.UUID;

public class AfkKickExemptList {

	private ArrayList<UUID> playerIds;
	
	private int pageCount;
	
	public ArrayList<UUID> getPlayerIds() {
		return this.playerIds;
	}
	
	public int getPageCount() {
		return this.pageCount;
	}
	
	public void setPlayerIds(ArrayList<UUID> playerIds) {
		this.playerIds = playerIds;
	}
	
	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}
}
