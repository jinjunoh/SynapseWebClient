package org.sagebionetworks.web.client.widget.entity.browse;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils.SelectedHandler;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.cache.ClientCache;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.shared.PaginatedResults;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityFinder implements EntityFinderView.Presenter, IsWidget {
	public static final String ENTITY_FINDER_AREA_KEY = "org.sagebionetworks.web.client.entityfinder.area";
	private EntityFinderView view;
	private SynapseClientAsync synapseClient;
	private boolean showVersions = true;
	private List<Reference> selectedEntity;
	GlobalApplicationState globalApplicationState;
	AuthenticationController authenticationController;
	ClientCache cache;
	private SelectedHandler<Reference> selectedHandler;
	private SelectedHandler<List<Reference>> selectedMultiHandler;
	private EntityFilter filter;
	private SynapseAlert synAlert;
	@Inject
	public EntityFinder(EntityFinderView view,
			SynapseClientAsync synapseClient,
			GlobalApplicationState globalApplicationState,
			AuthenticationController authenticationController,
			ClientCache cache,
			SynapseAlert synAlert) {
		this.view = view;
		this.synapseClient = synapseClient;
		this.globalApplicationState = globalApplicationState;
		this.authenticationController = authenticationController;
		this.cache = cache;
		this.synAlert = synAlert;
		view.setPresenter(this);
		view.setSynAlert(synAlert.asWidget());
	}	

	@SuppressWarnings("unchecked")
	public void clearState() {
		view.clear();
	}

	public void configure(boolean showVersions, SelectedHandler<Reference> handler) {
		configure(EntityFilter.ALL, showVersions, handler);
	}
	

	public void configure(EntityFilter filter, boolean showVersions, SelectedHandler<Reference> handler) {
		this.filter = filter;
		this.showVersions = showVersions;
		this.selectedHandler = handler;
		this.selectedEntity = new ArrayList<Reference>();
	}
	
	public void configureMulti(boolean showVersions, SelectedHandler<List<Reference>> handler) {
		configureMulti(EntityFilter.ALL, showVersions, handler);
	}
	

	public void configureMulti(EntityFilter filter, boolean showVersions, SelectedHandler<List<Reference>> handler) {
		this.filter = filter;
		this.showVersions = showVersions;
		this.selectedMultiHandler = handler;
		this.selectedEntity = new ArrayList<Reference>();
	}
	
	@Override
	public void setSelectedEntity(Reference selected) {
		synAlert.clear();
		if (selectedEntity.isEmpty()) {
			selectedEntity.add(selected);			
		}
		selectedEntity.set(0, selected);
	}
	
	@Override
	public void okClicked() {
		synAlert.clear();
		//check for valid selection
		if (selectedEntity == null || selectedEntity.get(0).getTargetId() == null) {
			synAlert.showError(DisplayConstants.PLEASE_MAKE_SELECTION);
		} else {
			// fetch the entity for a type check
			lookupEntity(selectedEntity.get(0).getTargetId(), new AsyncCallback<PaginatedResults<EntityHeader>>() {
				@Override
				public void onFailure(Throwable caught) {
					synAlert.handleException(caught);
				}

				@Override
				public void onSuccess(PaginatedResults<EntityHeader> result) {
					if (validateEntityTypeAgainstFilter(result.getResults())) {
						if (selectedHandler != null) {
							selectedHandler.onSelected(selectedEntity.get(0));
						}
						if (selectedMultiHandler != null) {
							selectedMultiHandler.onSelected(selectedEntity);
						}
					}
				}
			});
		}
	}
	
	public boolean validateEntityTypeAgainstFilter(List<EntityHeader> list) {
		boolean flag = selectedEntity.size() == filter.filterForBrowsing(list).size();
		if (!flag) {
			synAlert.showError("Please select a " + filter.toString().toLowerCase());
		}
		return flag;
	}
	
	public List<Reference> getSelectedEntity() {
		return selectedEntity;
	}

	@Override
	public void lookupEntity(String entityId, final AsyncCallback<PaginatedResults<EntityHeader>> callback) {
		synAlert.clear();
		processEntities(entityId);
		ReferenceList rl = new ReferenceList();
		rl.setReferences(selectedEntity);
		synapseClient.getEntityHeaderBatch(rl, new AsyncCallback<PaginatedResults<EntityHeader>>() {

			@Override
			public void onFailure(Throwable caught) {
				synAlert.handleException(caught);
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(PaginatedResults<EntityHeader> result) {
				callback.onSuccess(result);
			}
			
		});
	}

	private void processEntities(String entityId) {
		String[] entities = entityId.split(",");
		selectedEntity.clear();
		for (int i = 0; i < entities.length; i++) {
			Reference r = new Reference();
			r.setTargetId(entities[i].trim());
			selectedEntity.add(r);
		}		
	}

	@Override
	public void loadVersions(String entityId) {
		synAlert.clear();
		synapseClient.getEntityVersions(entityId, 1, 200, new AsyncCallback<PaginatedResults<VersionInfo>>() {
			@Override
			public void onSuccess(PaginatedResults<VersionInfo> result) {
				PaginatedResults<VersionInfo> versions = result;
				view.setVersions(versions.getResults());
			}
			@Override
			public void onFailure(Throwable caught) {
				synAlert.handleException(caught);
			}
		});
	}
	
	@Override
	public boolean showVersions() {
		return this.showVersions;
	}
	
	@Override
	public void show() {
		view.clear();
		//get the area from the cache, if available
		String areaString = cache.get(ENTITY_FINDER_AREA_KEY);
		//default to browse
		EntityFinderArea area = EntityFinderArea.BROWSE;
		if (areaString != null) {
			area = EntityFinderArea.valueOf(areaString);
		}
		view.initFinderComponents(filter);
		switch (area) {
			case SEARCH:
				view.setSearchAreaVisible();
				break;
			case SYNAPSE_ID:
				view.setSynapseIdAreaVisible();
				break;
			case BROWSE:
			default:
				view.setBrowseAreaVisible();
				break;
		}
		view.show();
	}
	
	@Override
	public void hide() {
		//save area
		EntityFinderArea area = view.getCurrentArea();
		if (area != null) {
			cache.put(ENTITY_FINDER_AREA_KEY, area.toString());	
		}
		
		view.hide();
	}
	
	public void showError(String error) {
		synAlert.showError(error);
	}
	
	@Override
	public Widget asWidget() {
		return view.asWidget();
	}
}
