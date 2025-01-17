package org.sagebionetworks.web.client.presenter;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.binder.EventHandler;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GWTWrapper;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.cache.ClientCache;
import org.sagebionetworks.web.client.context.KeyFactoryProvider;
import org.sagebionetworks.web.client.context.QueryClientProvider;
import org.sagebionetworks.web.client.events.DownloadListUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.jsinterop.KeyFactory;
import org.sagebionetworks.web.client.jsinterop.reactquery.InvalidateQueryFilters;
import org.sagebionetworks.web.client.jsinterop.reactquery.QueryClient;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.view.EntityView;
import org.sagebionetworks.web.client.widget.entity.EntityPageTop;
import org.sagebionetworks.web.client.widget.entity.controller.StuAlert;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.team.OpenTeamInvitationsWidget;
import org.sagebionetworks.web.shared.OpenUserInvitationBundle;
import org.sagebionetworks.web.shared.WebConstants;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;

public class EntityPresenter
  extends AbstractActivity
  implements EntityView.Presenter, Presenter<Synapse>, IsWidget {

  private EntityView view;
  private AuthenticationController authenticationController;
  private StuAlert synAlert;
  private String entityId;
  private Long versionNumber;
  private Synapse.EntityArea area;
  private String areaToken;
  private Header headerWidget;
  private EntityPageTop entityPageTop;
  private OpenTeamInvitationsWidget openTeamInvitesWidget;
  private GlobalApplicationState globalAppState;
  private GWTWrapper gwt;
  private SynapseJavascriptClient jsClient;
  private QueryClient queryClient;
  private ClientCache clientCache;
  private KeyFactoryProvider keyFactoryProvider;

  @Inject
  public EntityPresenter(
    EntityView view,
    EntityPresenterEventBinder entityPresenterEventBinder,
    GlobalApplicationState globalAppState,
    AuthenticationController authenticationController,
    SynapseJavascriptClient jsClient,
    StuAlert synAlert,
    EntityPageTop entityPageTop,
    Header headerWidget,
    OpenTeamInvitationsWidget openTeamInvitesWidget,
    GWTWrapper gwt,
    EventBus eventBus,
    QueryClientProvider queryClientProvider,
    ClientCache clientCache,
    KeyFactoryProvider keyFactoryProvider
  ) {
    this.headerWidget = headerWidget;
    this.entityPageTop = entityPageTop;
    this.globalAppState = globalAppState;
    this.openTeamInvitesWidget = openTeamInvitesWidget;
    this.view = view;
    this.synAlert = synAlert;
    this.authenticationController = authenticationController;
    this.jsClient = jsClient;
    this.gwt = gwt;
    this.queryClient = queryClientProvider.getQueryClient();
    this.clientCache = clientCache;
    this.keyFactoryProvider = keyFactoryProvider;
    clear();
    entityPresenterEventBinder
      .getEventBinder()
      .bindEventHandlers(this, eventBus);
  }

  @Override
  public void start(AcceptsOneWidget panel, EventBus eventBus) {
    clear();
    // Install the view
    panel.setWidget(view);
    view.setLoadingVisible(true);
  }

  @Override
  public void setPlace(Synapse place) {
    this.entityId = place.getEntityId();
    this.versionNumber = place.getVersionNumber();
    this.area = place.getArea();
    this.areaToken = place.getAreaToken();
    refresh();
  }

  public static boolean isValidEntityId(String entityId) {
    if (
      entityId == null ||
      entityId.length() == 0 ||
      !entityId.toLowerCase().startsWith("syn")
    ) {
      return false;
    }

    // try to parse the actual syn id
    try {
      Long.parseLong(entityId.substring("syn".length()).trim());
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  @Override
  public void clear() {
    entityPageTop.clearState();
    synAlert.clear();
    openTeamInvitesWidget.clear();
    view.clear();
    view.setAccessDependentMessageVisible(false);
  }

  @Override
  public String mayStop() {
    view.clear();
    return null;
  }

  @Override
  public void refresh() {
    clear();
    headerWidget.refresh();
    // place widgets and configure
    view.setEntityPageTopWidget(entityPageTop);
    view.setOpenTeamInvitesWidget(openTeamInvitesWidget);
    view.setSynAlertWidget(synAlert.asWidget());
    // Hide the view panel contents until async callback completes
    view.setLoadingVisible(true);
    checkEntityIdAndVersion();
  }

  /**
   * First step of loading the page.  Check for a valid entity ID, and check it's version.
   * If this is a File and the version number specified is the latest,
   * then clear the version to allow user to operate on the current FileEntity.
   */
  public void checkEntityIdAndVersion() {
    // before anything else, figure out the latest version to determine if it should be nulled out here (so user can operate on the latest version of the entity)
    if (isValidEntityId(entityId)) {
      if (versionNumber == null) {
        // version is already null, continue loading...
        getEntityBundleAndLoadPageTop();
      } else {
        jsClient.getEntity(
          entityId,
          new AsyncCallback<Entity>() {
            @Override
            public void onFailure(Throwable caught) {
              onError(caught);
            }

            @Override
            public void onSuccess(Entity entity) {
              if (entity instanceof Versionable) {
                if (
                  versionNumber.equals(
                    ((Versionable) entity).getVersionNumber()
                  )
                ) {
                  // we've been asked to load the current file version
                  versionNumber = null;
                }
              }
              // continue loading...
              getEntityBundleAndLoadPageTop();
            }
          }
        );
      }
    } else {
      // invalid entity detected, indicate that the page was not found
      gwt.scheduleDeferred(
        new Callback() {
          @Override
          public void invoke() {
            onError(new NotFoundException());
          }
        }
      );
    }
  }

  private FluentFuture<Long> getLatestSnapshotVersionNumber(String entityId) {
    return jsClient
      .getEntityVersions(entityId, 0, 1)
      .transform(
        new Function<List<VersionInfo>, Long>() {
          @Nullable
          @Override
          public Long apply(@Nullable List<VersionInfo> result) {
            if (result.size() > 0) {
              return result.get(0).getVersionNumber();
            }
            return null;
          }
        },
        directExecutor()
      );
  }

  public void loadStableDatasetIfAvailable(String entityId) {
    getLatestSnapshotVersionNumber(entityId)
      .addCallback(
        new FutureCallback<Long>() {
          @Override
          public void onSuccess(@Nullable Long result) {
            if (result == null) {
              // no stable versions found, force load the draft version
              clientCache.put(
                entityId + WebConstants.FORCE_LOAD_DRAFT_DATASET_SUFFIX,
                "true"
              );
            } else {
              // stable version found, load that instead
              versionNumber = result;
            }
            getEntityBundleAndLoadPageTop();
          }

          @Override
          public void onFailure(Throwable t) {
            synAlert.showError(t.getMessage());
          }
        },
        directExecutor()
      );
  }

  public void getEntityBundleAndLoadPageTop() {
    final AsyncCallback<EntityBundle> callback = new AsyncCallback<
      EntityBundle
    >() {
      @Override
      public void onSuccess(EntityBundle bundle) {
        //SWC-6551: If entity is the draft version of a Dataset, the current user cannot edit, and we are not told to force load the draft version, then load the latest stable version (if available)
        String forceLoadDraftDataset = clientCache.get(
          bundle.getEntity().getId() +
          WebConstants.FORCE_LOAD_DRAFT_DATASET_SUFFIX
        );
        if (
          bundle.getEntity() instanceof Dataset &&
          !bundle.getPermissions().getCanCertifiedUserEdit() &&
          versionNumber == null &&
          forceLoadDraftDataset == null
        ) {
          loadStableDatasetIfAvailable(bundle.getEntity().getId());
          return;
        }
        clientCache.remove(
          bundle.getEntity().getId() +
          WebConstants.FORCE_LOAD_DRAFT_DATASET_SUFFIX
        );
        synAlert.clear();
        view.setLoadingVisible(false);
        // Redirect if Entity is a Link
        if (bundle.getEntity() instanceof Link) {
          Reference ref = ((Link) bundle.getEntity()).getLinksTo();
          entityId = null;
          if (ref != null) {
            // redefine where the page is and refresh
            entityId = ref.getTargetId();
            versionNumber = ref.getTargetVersionNumber();
            refresh();
            return;
          } else {
            // show error and then allow entity bundle to go to view
            view.showErrorMessage(DisplayConstants.ERROR_NO_LINK_DEFINED);
          }
        }
        EntityHeader projectHeader = DisplayUtils.getProjectHeader(
          bundle.getPath()
        );
        if (projectHeader == null) {
          synAlert.showError(DisplayConstants.ERROR_GENERIC_RELOAD);
        } else {
          entityPageTop.configure(
            bundle,
            versionNumber,
            projectHeader,
            area,
            areaToken
          );
          view.setEntityPageTopWidget(entityPageTop);
          view.setEntityPageTopVisible(true);
        }
      }

      @Override
      public void onFailure(Throwable caught) {
        onError(caught);
      }
    };

    if (versionNumber == null) {
      jsClient.getEntityBundle(
        entityId,
        EntityPageTop.ALL_PARTS_REQUEST,
        callback
      );
    } else {
      jsClient.getEntityBundleForVersion(
        entityId,
        versionNumber,
        EntityPageTop.ALL_PARTS_REQUEST,
        callback
      );
    }
  }

  public void onError(Throwable caught) {
    view.setLoadingVisible(false);
    headerWidget.configure();
    if (caught instanceof NotFoundException) {
      show404();
    } else if (
      caught instanceof ForbiddenException &&
      authenticationController.isLoggedIn()
    ) {
      show403();
    } else {
      view.clear();
      synAlert.handleException(caught);
    }
  }

  public void show403() {
    if (entityId != null) {
      synAlert.show403(entityId);
    }
    view.setLoadingVisible(false);
    view.setEntityPageTopVisible(false);
    // also add the open team invitations widget (accepting may gain access to this project)
    openTeamInvitesWidget.configure(
      new Callback() {
        @Override
        public void invoke() {
          // when team is updated, refresh to see if we can now access
          refresh();
        }
      },
      new CallbackP<List<OpenUserInvitationBundle>>() {
        @Override
        public void invoke(List<OpenUserInvitationBundle> invites) {
          // if there are any, then also add the title text to the panel
          if (invites != null && invites.size() > 0) {
            view.setAccessDependentMessageVisible(true);
          }
        }
      }
    );
    view.setOpenTeamInvitesVisible(true);
  }

  public void show404() {
    synAlert.show404();
    view.setLoadingVisible(false);
    view.setEntityPageTopVisible(false);
    view.setOpenTeamInvitesVisible(false);
  }

  @Override
  public Widget asWidget() {
    return view.asWidget();
  }

  @EventHandler
  public void onEntityUpdatedEvent(EntityUpdatedEvent event) {
    KeyFactory keyFactory = keyFactoryProvider.getKeyFactory(
      authenticationController.getCurrentUserAccessToken()
    );

    queryClient.invalidateQueries(
      InvalidateQueryFilters.create(
        keyFactory.getEntityQueryKey(event.getEntityId())
      )
    );
    globalAppState.refreshPage();
  }

  @EventHandler
  public void onDownloadListUpdatedUpdatedEvent(
    DownloadListUpdatedEvent _event
  ) {
    KeyFactory keyFactory = keyFactoryProvider.getKeyFactory(
      authenticationController.getCurrentUserAccessToken()
    );
    queryClient.invalidateQueries(
      InvalidateQueryFilters.create(keyFactory.getDownloadListBaseQueryKey())
    );
  }

  // for testing only

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }
}
