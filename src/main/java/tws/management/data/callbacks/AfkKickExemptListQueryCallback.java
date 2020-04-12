package tws.management.data.callbacks;

import tws.management.data.entities.AfkKickExemptList;

public interface AfkKickExemptListQueryCallback extends BaseQueryCallback<AfkKickExemptList> {

	public void onQueryComplete(AfkKickExemptList result);
}
