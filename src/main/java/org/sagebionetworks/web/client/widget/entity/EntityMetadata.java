package org.sagebionetworks.web.client.widget.entity;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.VersionableEntity;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.file.ExternalGoogleCloudUploadDestination;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreUploadDestination;
import org.sagebionetworks.repo.model.file.ExternalS3UploadDestination;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.widget.doi.DoiWidgetV2;
import org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl;
import org.sagebionetworks.web.client.widget.entity.menu.v3.Action;
import org.sagebionetworks.web.client.widget.entity.menu.v3.EntityActionMenu;
import org.sagebionetworks.web.client.widget.entity.restriction.v2.RestrictionWidget;

public class EntityMetadata {

  private final EntityMetadataView view;
  private final DoiWidgetV2 doiWidgetV2;
  private VersionHistoryWidget versionHistoryWidget;
  private final SynapseJavascriptClient jsClient;
  private final SynapseJSNIUtils jsni;
  private final PortalGinInjector ginInjector;
  private final ContainerItemCountWidget containerItemCountWidget;
  private final org.sagebionetworks.web.client.widget.entity.restriction.v2.RestrictionWidget restrictionWidgetV2;
  private final EntityModalWidget entityModalWidget;

  private boolean annotationsAreVisible = false;

  @Inject
  public EntityMetadata(
    EntityMetadataView view,
    DoiWidgetV2 doiWidgetV2,
    SynapseJavascriptClient jsClient,
    SynapseJSNIUtils jsni,
    RestrictionWidget restrictionWidgetV2,
    ContainerItemCountWidget containerItemCountWidget,
    PortalGinInjector ginInjector,
    EntityModalWidget entityModalWidget
  ) {
    this.view = view;
    this.doiWidgetV2 = doiWidgetV2;
    this.jsClient = jsClient;
    this.jsni = jsni;
    this.restrictionWidgetV2 = restrictionWidgetV2;
    this.containerItemCountWidget = containerItemCountWidget;
    this.ginInjector = ginInjector;
    this.entityModalWidget = entityModalWidget;
    this.view.setDoiWidget(doiWidgetV2);
    this.view.setEntityModalWidget(entityModalWidget);
    this.view.setRestrictionWidgetV2(restrictionWidgetV2);
    this.view.setContainerItemCountWidget(containerItemCountWidget);
    restrictionWidgetV2.setShowChangeLink(true);
    view.setRestrictionWidgetV2Visible(true);
  }

  public Widget asWidget() {
    return view.asWidget();
  }

  public VersionHistoryWidget getVersionHistoryWidget() {
    if (versionHistoryWidget == null) {
      versionHistoryWidget = ginInjector.getVersionHistoryWidget();
      view.setVersionHistoryWidget(versionHistoryWidget);
    }
    return versionHistoryWidget;
  }

  public void configure(
    EntityBundle bundle,
    Long versionNumber,
    EntityActionMenu actionMenu
  ) {
    clear();
    // The "detailed metadata" is shown in the title bar React component for non-project entities.
    view.setDetailedMetadataVisible(bundle.getEntity() instanceof Project);
    Entity en = bundle.getEntity();
    view.setEntityId(en.getId());
    entityModalWidget.configure(
      en.getId(),
      versionNumber,
      () -> setAnnotationsVisible(false),
      "ANNOTATIONS",
      false
    );

    // See comments on SWC-5763
    // TL;DR: we plan to show the description at some point, but not until we implement new designs
    // view.setDescriptionVisible(bundle.getEntity() instanceof Table && en.getDescription() != null && DisplayUtils.isInTestWebsite(ginInjector.getCookieProvider()));
    view.setDescriptionVisible(false);
    view.setDescription(en.getDescription());

    setAnnotationsVisible(false);
    actionMenu.setActionListener(
      Action.SHOW_ANNOTATIONS,
      (action, e) -> setAnnotationsVisible(!annotationsAreVisible)
    );

    actionMenu.setActionListener(
      Action.SHOW_VERSION_HISTORY,
      (action, e) -> {
        getVersionHistoryWidget()
          .setVisible(!getVersionHistoryWidget().isVisible());
      }
    );

    boolean isCurrentVersion = en instanceof VersionableEntity
      ? ((VersionableEntity) en).getIsLatestVersion()
      : true;
    if (EntityActionControllerImpl.isVersionSupported(bundle.getEntity())) {
      getVersionHistoryWidget()
        .setVisible(
          !((VersionableEntity) bundle.getEntity()).getIsLatestVersion()
        );
      getVersionHistoryWidget().setEntityBundle(bundle, versionNumber);
      view.setRestrictionPanelVisible(true);
    } else {
      if (versionHistoryWidget != null) {
        versionHistoryWidget.setVisible(false);
      }
      view.setRestrictionPanelVisible(
        en instanceof TableEntity ||
        en instanceof Folder ||
        en instanceof DockerRepository
      );
    }
    if (bundle.getEntity() instanceof Folder) {
      containerItemCountWidget.configure(bundle.getEntity().getId());
    }
    configureStorageLocation(en);

    // An unversioned DOI may not have been included in the (versioned) entity bundle, so we should see if one exists
    if (
      bundle.getDoiAssociation() == null && // If a versioned DOI exists, we should show that
      en instanceof VersionableEntity &&
      isCurrentVersion // We only do this for the current/latest version because the unversioned DOI points to that version
    ) {
      jsClient
        .getDoiAssociation(en.getId(), ObjectType.ENTITY, null)
        .addCallback(
          new FutureCallback<DoiAssociation>() {
            @Override
            public void onSuccess(@Nullable DoiAssociation doiAssociation) {
              doiWidgetV2.configure(doiAssociation);
            }

            @Override
            public void onFailure(Throwable t) {
              // no op
            }
          },
          directExecutor()
        );
    } else if (bundle.getDoiAssociation() != null) {
      doiWidgetV2.configure(bundle.getDoiAssociation());
    }
    restrictionWidgetV2.configure(
      en,
      bundle.getPermissions().getCanChangePermissions()
    );
  }

  public void setAnnotationsVisible(boolean visible) {
    annotationsAreVisible = visible;
    entityModalWidget.setOpen(visible);
  }

  public void clear() {
    doiWidgetV2.clear();
    containerItemCountWidget.clear();
    view.clear();
  }

  public void configureStorageLocation(Entity en) {
    view.setUploadDestinationPanelVisible(false);
    if (en instanceof Folder || en instanceof Project) {
      String containerEntityId = en.getId();
      jsClient.getUploadDestinations(
        containerEntityId,
        new AsyncCallback<List<UploadDestination>>() {
          public void onSuccess(List<UploadDestination> uploadDestinations) {
            if (
              uploadDestinations == null ||
              uploadDestinations.isEmpty() ||
              uploadDestinations.get(0) instanceof S3UploadDestination
            ) {
              view.setUploadDestinationText("Synapse Storage");
            } else if (
              uploadDestinations.get(0) instanceof ExternalUploadDestination
            ) {
              ExternalUploadDestination externalUploadDestination =
                (ExternalUploadDestination) uploadDestinations.get(0);
              String externalUrl = externalUploadDestination.getUrl();
              UploadType type = externalUploadDestination.getUploadType();
              if (type == UploadType.SFTP) {
                int indexOfLastSlash = externalUrl.lastIndexOf('/');
                if (indexOfLastSlash > -1) {
                  externalUrl = externalUrl.substring(0, indexOfLastSlash);
                }
              }
              view.setUploadDestinationText(externalUrl);
            } else if (
              uploadDestinations.get(0) instanceof ExternalS3UploadDestination
            ) {
              ExternalS3UploadDestination externalUploadDestination =
                (ExternalS3UploadDestination) uploadDestinations.get(0);
              String description =
                "s3://" + externalUploadDestination.getBucket() + "/";
              if (externalUploadDestination.getBaseKey() != null) {
                description += externalUploadDestination.getBaseKey();
              }
              view.setUploadDestinationText(description);
            } else if (
              uploadDestinations.get(0) instanceof
              ExternalGoogleCloudUploadDestination
            ) {
              ExternalGoogleCloudUploadDestination externalUploadDestination =
                (ExternalGoogleCloudUploadDestination) uploadDestinations.get(
                  0
                );
              String description =
                "gs://" + externalUploadDestination.getBucket() + "/";
              if (externalUploadDestination.getBaseKey() != null) {
                description += externalUploadDestination.getBaseKey();
              }
              view.setUploadDestinationText(description);
            } else if (
              uploadDestinations.get(0) instanceof
              ExternalObjectStoreUploadDestination
            ) {
              ExternalObjectStoreUploadDestination destination =
                (ExternalObjectStoreUploadDestination) uploadDestinations.get(
                  0
                );
              String description =
                destination.getEndpointUrl() + "/" + destination.getBucket();
              view.setUploadDestinationText(description);
            }
            view.setUploadDestinationPanelVisible(true);
          }

          @Override
          public void onFailure(Throwable err) {
            jsni.consoleLog(err.getMessage());
          }
        }
      );
    }
  }
}
