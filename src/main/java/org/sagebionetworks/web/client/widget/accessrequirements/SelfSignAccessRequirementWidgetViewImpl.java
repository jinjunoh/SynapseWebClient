package org.sagebionetworks.web.client.widget.accessrequirements;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.html.Div;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.utils.Callback;

public class SelfSignAccessRequirementWidgetViewImpl
  implements SelfSignAccessRequirementWidgetView {

  @UiField
  Div approvedHeading;

  @UiField
  Div unapprovedHeading;

  @UiField
  SimplePanel wikiContainer;

  @UiField
  Button signTermsButton;

  @UiField
  Button loginButton;

  @UiField
  Button certifyButton;

  @UiField
  Alert certifyNote;

  @UiField
  Button validateProfileButton;

  @UiField
  Alert validateProfileNote;

  @UiField
  Div editAccessRequirementContainer;

  @UiField
  Div teamSubjectsWidgetContainer;

  @UiField
  Div coveredEntitiesHeadingUI;

  @UiField
  Div entitySubjectsWidgetContainer;

  @UiField
  Div accessRequirementRelatedProjectsListContainer;

  @UiField
  Div manageAccessContainer;

  @UiField
  Alert approvedAlert;

  @UiField
  InlineLabel accessRequirementIDField;

  @UiField
  Div accessRequirementIDUI;

  @UiField
  Div controlsContainer;

  Callback onAttachCallback;

  @UiField
  Div subjectsDefinedByAnnotationsUI;

  @UiField
  Div subjectsDefinedInAccessRequirementUI;

  public interface Binder
    extends UiBinder<Widget, SelfSignAccessRequirementWidgetViewImpl> {}

  Widget w;
  Presenter presenter;

  @Inject
  public SelfSignAccessRequirementWidgetViewImpl(
    Binder binder,
    GlobalApplicationState globalAppState
  ) {
    this.w = binder.createAndBindUi(this);
    signTermsButton.addClickHandler(event -> {
      presenter.onSignTerms();
    });
    validateProfileButton.addClickHandler(event -> {
      presenter.onValidateProfile();
    });
    certifyButton.addClickHandler(event -> {
      presenter.onCertify();
    });
    loginButton.addClickHandler(event -> {
      globalAppState
        .getPlaceChanger()
        .goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
    });

    w.addAttachHandler(event -> {
      if (event.isAttached()) {
        onAttachCallback.invoke();
      }
    });
  }

  @Override
  public void addStyleNames(String styleNames) {
    w.addStyleName(styleNames);
  }

  @Override
  public void setPresenter(final Presenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public Widget asWidget() {
    return w;
  }

  @Override
  public void setWikiTermsWidget(Widget wikiWidget) {
    wikiContainer.setWidget(wikiWidget);
  }

  @Override
  public void showApprovedHeading() {
    approvedHeading.setVisible(true);
    approvedAlert.setVisible(true);
  }

  @Override
  public void showUnapprovedHeading() {
    unapprovedHeading.setVisible(true);
  }

  @Override
  public void showSignTermsButton() {
    signTermsButton.setVisible(true);
  }

  @Override
  public void resetState() {
    approvedAlert.setVisible(false);
    approvedHeading.setVisible(false);
    unapprovedHeading.setVisible(false);
    signTermsButton.setVisible(false);
    certifyButton.setVisible(false);
    certifyNote.setVisible(false);
    validateProfileButton.setVisible(false);
    validateProfileNote.setVisible(false);
    loginButton.setVisible(false);
  }

  @Override
  public void showGetCertifiedUI() {
    certifyButton.setVisible(true);
    certifyNote.setVisible(true);
  }

  @Override
  public void showGetProfileValidatedUI() {
    validateProfileButton.setVisible(true);
    validateProfileNote.setVisible(true);
  }

  @Override
  public void setEditAccessRequirementWidget(IsWidget w) {
    editAccessRequirementContainer.clear();
    editAccessRequirementContainer.add(w);
  }

  @Override
  public void setTeamSubjectsWidget(IsWidget w) {
    teamSubjectsWidgetContainer.clear();
    teamSubjectsWidgetContainer.add(w);
  }

  @Override
  public void setEntitySubjectsWidget(IsWidget w) {
    entitySubjectsWidgetContainer.clear();
    entitySubjectsWidgetContainer.add(w);
  }

  @Override
  public void setCoveredEntitiesHeadingVisible(boolean visible) {
    coveredEntitiesHeadingUI.setVisible(visible);
  }

  @Override
  public void setAccessRequirementRelatedProjectsList(IsWidget w) {
    accessRequirementRelatedProjectsListContainer.clear();
    accessRequirementRelatedProjectsListContainer.add(w);
  }

  @Override
  public void setOnAttachCallback(Callback onAttachCallback) {
    this.onAttachCallback = onAttachCallback;
  }

  @Override
  public boolean isInViewport() {
    return DisplayUtils.isInViewport(w);
  }

  @Override
  public boolean isAttached() {
    return w.isAttached();
  }

  @Override
  public void setManageAccessWidget(IsWidget w) {
    manageAccessContainer.clear();
    manageAccessContainer.add(w);
  }

  @Override
  public void showLoginButton() {
    loginButton.setVisible(true);
  }

  @Override
  public void hideControls() {
    controlsContainer.setVisible(false);
  }

  @Override
  public void setAccessRequirementID(String arID) {
    accessRequirementIDField.setText(arID);
  }

  @Override
  public void setAccessRequirementIDVisible(boolean visible) {
    accessRequirementIDUI.setVisible(visible);
  }

  @Override
  public void setSubjectsDefinedByAnnotations(
    Boolean subjectsDefinedByAnnotations
  ) {
    boolean v = subjectsDefinedByAnnotations != null
      ? subjectsDefinedByAnnotations.booleanValue()
      : false;
    subjectsDefinedByAnnotationsUI.setVisible(v);
    subjectsDefinedInAccessRequirementUI.setVisible(!v);
  }
}
