package tws.management.data;

public class MojangUserNameModel {

	private String name;
	private long changedToAt;
	
	public String getName() {
		return this.name;
	}
	
	public long getChangedToAt() {
		return this.changedToAt;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setChangedToAt(long changedToAt) {
		this.changedToAt = changedToAt;
	}
}
