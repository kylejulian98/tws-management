package dev.kylejulian.tws.data.callbacks;

public interface BooleanQueryCallback extends BaseQueryCallback<Boolean> {

	@Override
	void onQueryComplete(Boolean result);
	
}
