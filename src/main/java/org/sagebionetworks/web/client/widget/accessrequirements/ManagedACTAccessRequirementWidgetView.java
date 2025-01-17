package org.sagebionetworks.web.client.widget.accessrequirements;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.web.client.widget.lazyload.SupportsLazyLoadInterface;

public interface ManagedACTAccessRequirementWidgetView
  extends IsWidget, SupportsLazyLoadInterface {
  /**
   * Set the presenter.
   *
   * @param presenter
   */
  void setPresenter(Presenter presenter);

  void addStyleNames(String styleNames);

  void setWikiTermsWidget(Widget wikiWidget);

  void setWikiTermsWidgetVisible(boolean visible);

  void showRequestSubmittedByOtherUser();

  void showApprovedHeading();

  void showUnapprovedHeading();

  void showRequestSubmittedMessage();

  void showRequestApprovedMessage();

  void showRequestRejectedMessage(String reason);

  void showCancelRequestButton();

  void showUpdateRequestButton();

  void showRequestAccessButton();

  void resetState();

  void setEditAccessRequirementWidget(IsWidget w);

  void setIDUReportButton(IsWidget w);

  void setSubmitterUserBadge(IsWidget w);

  void setManageAccessWidget(IsWidget w);

  void setReviewAccessRequestsWidget(IsWidget w);

  void setTeamSubjectsWidget(IsWidget w);

  void setVisible(boolean visible);

  void setSynAlert(IsWidget w);

  void hideControls();

  void setReviewAccessRequestsWidgetContainerVisible(boolean visible);

  void showExpirationDate(String dateString);

  void showLoginButton();

  void setAccessRequirementIDVisible(boolean visible);
  void setAccessRequirementID(String arID);
  void setAccessRequirementName(String description);

  void showRequestAccessModal(
    ManagedACTAccessRequirement accessRequirement,
    RestrictableObjectDescriptor targetSubject
  );

  /**
   * Presenter interface
   */
  public interface Presenter {
    void onCancelRequest();

    void onRequestAccess();

    void refreshApprovalState();

    void handleException(Throwable t);
  }

  void setCoveredEntitiesHeadingVisible(boolean visible);

  void setEntitySubjectsWidget(IsWidget entitySubjectsWidget);

  void setAccessRequirementRelatedProjectsList(
    IsWidget accessRequirementRelatedProjectsList
  );

  void setSubjectsDefinedByAnnotations(Boolean subjectsDefinedByAnnotations);
}
