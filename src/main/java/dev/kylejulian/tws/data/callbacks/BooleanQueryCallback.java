package dev.kylejulian.tws.data.callbacks;

public interface BooleanQueryCallback extends BaseQueryCallback<Boolean> {

	public void onQueryComplete(Boolean result);
	
}
