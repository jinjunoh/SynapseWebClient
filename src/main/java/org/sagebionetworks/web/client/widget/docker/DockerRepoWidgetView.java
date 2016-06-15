package org.sagebionetworks.web.client.widget.docker;

import com.google.gwt.user.client.ui.Widget;

public interface DockerRepoWidgetView {

	public interface Presenter{
		
	}

	void setPresenter(Presenter presenter);

	void setProvenance(Widget widget);

	Widget asWidget();

	void setWikiPage(Widget widget);

	void setProvenanceVisible(boolean visible);

	void setWikiPageWidgetVisible(boolean visible);

	void setSynapseAlert(Widget widget);
}