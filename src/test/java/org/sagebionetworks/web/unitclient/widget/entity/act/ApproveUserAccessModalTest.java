package org.sagebionetworks.web.unitclient.widget.entity.act;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.utils.GovernanceServiceHelper;
import org.sagebionetworks.web.client.widget.asynch.AsynchronousProgressHandler;
import org.sagebionetworks.web.client.widget.asynch.JobTrackingWidget;
import org.sagebionetworks.web.client.widget.entity.act.ApproveUserAccessModal;
import org.sagebionetworks.web.client.widget.entity.act.ApproveUserAccessModalView;
import org.sagebionetworks.web.client.widget.entity.act.EmailMessagePreviewModal;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.client.widget.search.SynapseSuggestBox;
import org.sagebionetworks.web.client.widget.search.SynapseSuggestion;
import org.sagebionetworks.web.client.widget.search.UserGroupSuggestionProvider;
import org.sagebionetworks.web.client.widget.table.v2.results.QueryBundleUtils;
import org.sagebionetworks.web.shared.asynch.AsynchType;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class ApproveUserAccessModalTest {

	ApproveUserAccessModal dialog;
	@Mock
	ApproveUserAccessModalView mockView;
	@Mock
	SynapseAlert mockSynAlert;
	@Mock
	SynapseSuggestBox mockPeopleSuggestWidget;
	@Mock
	UserGroupSuggestionProvider mockProvider; 
	@Mock
	SynapseClientAsync mockSynapseClient;
	@Mock
	GlobalApplicationState mockGlobalApplicationState;
	@Mock
	JobTrackingWidget mockProgressWidget;
	@Mock
	EmailMessagePreviewModal mockMessagePreview;
	@Mock
	SynapseSuggestion mockUser;
	@Mock
	QueryResultBundle qrb;
	@Mock
	QueryResult qr;
	@Mock
	RowSet rs;
	@Mock
	List<Row> lr;
	@Mock
	Row r;
	@Mock
	List<String> ls;
	@Mock
	Set<String> strSet;
	@Captor
	ArgumentCaptor<AsyncCallback<AccessApproval>> aaCaptor;
	@Captor
	ArgumentCaptor<AsynchronousProgressHandler> phCaptor;
	@Captor
	ArgumentCaptor<AsyncCallback<String>> sCaptor;
	
	Long accessReq;
	String userId;
	String datasetId;
	EntityBundle entityBundle;
	List<ACTAccessRequirement> actList;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		mockView = Mockito.mock(ApproveUserAccessModalView.class);
		mockSynAlert = Mockito.mock(SynapseAlert.class);
				
		mockPeopleSuggestWidget = Mockito.mock(SynapseSuggestBox.class);
		mockProvider = Mockito.mock(UserGroupSuggestionProvider.class);
		mockSynapseClient = Mockito.mock(SynapseClientAsync.class);
		mockGlobalApplicationState = Mockito.mock(GlobalApplicationState.class);
		mockProgressWidget = Mockito.mock(JobTrackingWidget.class);
		mockMessagePreview = Mockito.mock(EmailMessagePreviewModal.class);
		dialog = new ApproveUserAccessModal(mockView, mockSynAlert, mockPeopleSuggestWidget, mockProvider, mockSynapseClient, mockGlobalApplicationState, mockProgressWidget, mockMessagePreview);
		when(mockGlobalApplicationState.getSynapseProperty(anyString())).thenReturn("syn7444807");
		
		userId = "1234567";
		datasetId = "syn9876543";
		accessReq = 123L;
		
		entityBundle = new EntityBundle();
		Entity entity = new FileEntity();
		entity.setId(datasetId);
		entity.setName("Test Study");
		entityBundle.setEntity(entity);
		ACTAccessRequirement act = Mockito.mock(ACTAccessRequirement.class);
		actList = new ArrayList<ACTAccessRequirement>();
		actList.add(act);
		
		when(qrb.getQueryResult()).thenReturn(qr);		
		when(qr.getQueryResults()).thenReturn(rs);
		when(rs.getRows()).thenReturn(lr);
		when(lr.size()).thenReturn(1);
		when(lr.get(0)).thenReturn(r);
		when(r.getValues()).thenReturn(ls);
		when(ls.size()).thenReturn(1);
		when(ls.get(0)).thenReturn("Message");

		when(mockUser.getId()).thenReturn(userId);
		when(mockView.getAccessRequirement()).thenReturn(Long.toString(accessReq));
	}
	
	@Test
	public void testConfigureNoAccessReqs() {
		List<ACTAccessRequirement> accessReqs = new ArrayList<ACTAccessRequirement>();
		dialog.configure(accessReqs, entityBundle);
		verify(mockView, times(0)).setAccessRequirement(anyString(), anyString());
	}
	
	@Test
	public void testConfigureOneAccessReq() {
		ACTAccessRequirement ar = actList.get(0);
		String num = Long.toString(ar.getId());
		String text = GovernanceServiceHelper.getAccessRequirementText(ar);
		dialog.configure(actList, entityBundle);
		verify(mockView).setAccessRequirement(eq(num), eq(text));
		verify(mockView, times(1)).setAccessRequirement(anyString(), anyString());
		verify(mockView).setDatasetTitle(entityBundle.getEntity().getName());
	}
	
	@Test
	public void testLoadEmailMessageOnFailure() {
		dialog.configure(actList, entityBundle);
		ArgumentCaptor<AsynchronousProgressHandler> captor = ArgumentCaptor.forClass(AsynchronousProgressHandler.class);
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), captor.capture());
		captor.getValue().onFailure(any(Throwable.class));
		verify(mockSynAlert).handleException(any(Throwable.class));
	}
	
	@Test
	public void testLoadEmailMessageOnCancel() {
		dialog.configure(actList, entityBundle);
		ArgumentCaptor<AsynchronousProgressHandler> captor = ArgumentCaptor.forClass(AsynchronousProgressHandler.class);
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), captor.capture());
		captor.getValue().onCancel();
		verify(mockSynAlert).showError("Query cancelled");
	}
	
	@Test
	public void testLoadEmailMessageOnComplete() {
		dialog.configure(actList, entityBundle);
		
		ArgumentCaptor<AsynchronousProgressHandler> captor = ArgumentCaptor.forClass(AsynchronousProgressHandler.class);
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), captor.capture());
		captor.getValue().onComplete(qrb);
		
		verify(mockView).finishLoadingEmail();
	}
	
	@Test
	public void testOnStateSelected() {
		ACTAccessRequirement ar = actList.get(0);
		String num = Long.toString(ar.getId());
		String text = GovernanceServiceHelper.getAccessRequirementText(ar);
		dialog.configure(actList, entityBundle);
		dialog.onStateSelected(num);
		verify(mockView, times(2)).setAccessRequirement(eq(num), eq(text));
	}
	
	@Test
	public void testOnSubmitNoUserSelected() {
		dialog.configure(actList, entityBundle);
		dialog.onSubmit();
		verify(mockSynAlert).showError(eq("You must select a user to approve"));
	}
	
	@Test
	public void testOnSubmitNoMessage() {
		dialog.configure(actList, entityBundle);
		when(mockGlobalApplicationState.getSynapseProperty(anyString())).thenReturn(null);
		dialog.onUserSelected(mockUser);
		dialog.onSubmit();
		verify(mockSynAlert, times(0)).showError(eq("You must select a user to approve"));
		verify(mockSynAlert).showError("An error was encountered while loading the email message body");
	}
	
	@Test
	public void testOnSubmitNullAccessReq() {
		dialog.configure(actList, entityBundle);
		
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), phCaptor.capture());
		phCaptor.getValue().onComplete(qrb);
		
		dialog.onUserSelected(mockUser);
		dialog.onSubmit();
		verify(mockSynAlert, times(0)).showError(eq("You must select a user to approve"));
		verify(mockSynAlert, times(0)).showError("An error was encountered while loading the email message body");
		verify(mockView).getAccessRequirement();
		verify(mockView).setApproveProcessing(true);
	}
	
	@Test
	public void testOnSubmitOnFailure() {
		dialog.configure(actList, entityBundle);
		
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), phCaptor.capture());
		phCaptor.getValue().onComplete(qrb);
		
		dialog.onUserSelected(mockUser);
		dialog.onSubmit();
		verify(mockSynAlert, times(0)).showError(eq("You must select a user to approve"));
		verify(mockSynAlert, times(0)).showError("An error was encountered while loading the email message body");
		verify(mockView).getAccessRequirement();
		verify(mockView).setApproveProcessing(true);
		
		verify(mockSynapseClient).createAccessApproval(any(ACTAccessApproval.class), aaCaptor.capture());
		aaCaptor.getValue().onFailure(any(Throwable.class));
		verify(mockSynAlert).handleException(any(Throwable.class));
	}
	
	@Test
	public void testOnSubmitOnSuccess() {
		dialog.configure(actList, entityBundle);
		
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), phCaptor.capture());
		phCaptor.getValue().onComplete(qrb);
		
		dialog.onUserSelected(mockUser);
		dialog.onSubmit();
		verify(mockSynAlert, times(0)).showError(eq("You must select a user to approve"));
		verify(mockSynAlert, times(0)).showError("An error was encountered while loading the email message body");
		verify(mockView).getAccessRequirement();
		verify(mockView).setApproveProcessing(true);
		
		verify(mockSynapseClient).createAccessApproval(any(ACTAccessApproval.class), aaCaptor.capture());
		aaCaptor.getValue().onSuccess(any(AccessApproval.class));
	}
	
	@Test
	public void testOnSubmitSendMessageOnFailure() {
		dialog.configure(actList, entityBundle);
		
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), phCaptor.capture());
		phCaptor.getValue().onComplete(qrb);
		
		dialog.onUserSelected(mockUser);
		dialog.onSubmit();
		verify(mockSynAlert, times(0)).showError(eq("You must select a user to approve"));
		verify(mockSynAlert, times(0)).showError("An error was encountered while loading the email message body");
		verify(mockView).getAccessRequirement();
		verify(mockView).setApproveProcessing(true);
		
		verify(mockSynapseClient).createAccessApproval(any(ACTAccessApproval.class), aaCaptor.capture());
		aaCaptor.getValue().onSuccess(any(AccessApproval.class));
		
		verify(mockSynapseClient).sendMessage(strSet, anyString(), anyString(), anyString(), sCaptor.capture());
		sCaptor.getValue().onFailure(any(Throwable.class));
		
		verify(mockView).setApproveProcessing(false);
		verify(mockSynAlert).showError("User has been approved; however, an error was encountered while emailing them");
	
	}
	
	@Test
	public void testOnSubmitSendMessageOnSuccess() {
		dialog.configure(actList, entityBundle);
		
		verify(mockProgressWidget).startAndTrackJob(anyString(), anyBoolean(), any(AsynchType.class), any(QueryBundleRequest.class), phCaptor.capture());
		phCaptor.getValue().onComplete(qrb);
		
		dialog.onUserSelected(mockUser);
		dialog.onSubmit();
		verify(mockSynAlert, times(0)).showError(eq("You must select a user to approve"));
		verify(mockSynAlert, times(0)).showError("An error was encountered while loading the email message body");
		verify(mockView).getAccessRequirement();
		verify(mockView).setApproveProcessing(true);
		
		verify(mockSynapseClient).createAccessApproval(any(ACTAccessApproval.class), aaCaptor.capture());
		aaCaptor.getValue().onSuccess(any(AccessApproval.class));
	}
	
//	
//	@Test
//	public void testSetModalPresenter(){
//		// This is the main entry to the page
//		page.setModalPresenter(mockModalPresenter);
//		verify(mockModalPresenter).setPrimaryButtonText(CreateDownloadPageImpl.NEXT);
//		verify(mockView).setFileType(FileType.CSV);
//		verify(mockView).setIncludeHeaders(true);
//		verify(mockView).setIncludeRowMetadata(true);
//		verify(mockView).setTrackerVisible(false);
//	}
//	
//	@Test
//	public void testgetDownloadFromTableRequest(){
//		page.setModalPresenter(mockModalPresenter);
//		DownloadFromTableRequest expected = new DownloadFromTableRequest();
//		CsvTableDescriptor descriptor = new CsvTableDescriptor();
//		descriptor.setSeparator("\t");
//		expected.setCsvTableDescriptor(descriptor);
//		expected.setIncludeRowIdAndRowVersion(false);
//		expected.setSql(sql);
//		expected.setWriteHeader(true);
//		when(mockView.getFileType()).thenReturn(FileType.TSV);
//		when(mockView.getIncludeHeaders()).thenReturn(true);
//		when(mockView.getIncludeRowMetadata()).thenReturn(false);
//		
//		DownloadFromTableRequest request = page.getDownloadFromTableRequest();
//		assertEquals(expected, request);
//	}
//	
//	@Test
//	public void testOnPrimarySuccess(){
//		page.setModalPresenter(mockModalPresenter);
//		when(mockView.getFileType()).thenReturn(FileType.TSV);
//		when(mockView.getIncludeHeaders()).thenReturn(true);
//		when(mockView.getIncludeRowMetadata()).thenReturn(false);
//	
//		String fileHandle = "45678";
//		DownloadFromTableResult results = new DownloadFromTableResult();
//		results.setResultsFileHandleId(fileHandle);
//	
//		jobTrackingWidgetStub.setResponse(results);
//		page.onPrimary();
//		verify(mockModalPresenter).setLoading(true);
//		verify(mockView).setTrackerVisible(true);
//		verify(mockNextPage).configure(fileHandle);
//		verify(mockModalPresenter).setNextActivePage(mockNextPage);
//	}
//	
//	@Test
//	public void testOnPrimaryCancel(){
//		page.setModalPresenter(mockModalPresenter);
//		when(mockView.getFileType()).thenReturn(FileType.TSV);
//		when(mockView.getIncludeHeaders()).thenReturn(true);
//		when(mockView.getIncludeRowMetadata()).thenReturn(false);
//	
//		String fileHandle = "45678";
//		DownloadFromTableResult results = new DownloadFromTableResult();
//		results.setResultsFileHandleId(fileHandle);
//	
//		jobTrackingWidgetStub.setOnCancel(true);
//		page.onPrimary();
//		verify(mockModalPresenter).setLoading(true);
//		verify(mockView).setTrackerVisible(true);
//		verify(mockNextPage, never()).configure(fileHandle);
//		verify(mockModalPresenter).onCancel();
//	}
//	
//	@Test
//	public void testOnPrimaryFailure(){
//		page.setModalPresenter(mockModalPresenter);
//		when(mockView.getFileType()).thenReturn(FileType.TSV);
//		when(mockView.getIncludeHeaders()).thenReturn(true);
//		when(mockView.getIncludeRowMetadata()).thenReturn(false);
//	
//		String fileHandle = "45678";
//		DownloadFromTableResult results = new DownloadFromTableResult();
//		results.setResultsFileHandleId(fileHandle);
//		String error = "failure";
//		jobTrackingWidgetStub.setError(new Throwable(error));
//		page.onPrimary();
//		verify(mockModalPresenter).setLoading(true);
//		verify(mockView).setTrackerVisible(true);
//		verify(mockNextPage, never()).configure(fileHandle);
//		verify(mockModalPresenter).setErrorMessage(error);
//	}
}
