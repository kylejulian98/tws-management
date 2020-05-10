package dev.kylejulian.tws.data.callbacks;

public interface BaseQueryCallback<T> {

	void onQueryComplete(T result);
	
}
