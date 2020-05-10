package dev.kylejulian.tws.data.callbacks;

import dev.kylejulian.tws.data.entities.AfkKickExemptList;

public interface AfkKickExemptListQueryCallback extends BaseQueryCallback<AfkKickExemptList> {

	@Override
	void onQueryComplete(AfkKickExemptList result);
}
