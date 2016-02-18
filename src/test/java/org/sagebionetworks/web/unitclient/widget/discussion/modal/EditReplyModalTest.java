package org.sagebionetworks.web.unitclient.widget.discussion.modal;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.web.client.DiscussionForumClientAsync;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.widget.discussion.modal.EditReplyModal;
import org.sagebionetworks.web.client.widget.discussion.modal.ReplyModalView;
import org.sagebionetworks.web.client.widget.entity.MarkdownEditorWidget;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class EditReplyModalTest {
	@Mock
	ReplyModalView mockView;
	@Mock
	DiscussionForumClientAsync mockDiscussionForumClient;
	@Mock
	SynapseAlert mockSynAlert;
	@Mock
	Callback mockCallback;
	@Mock
	DiscussionReplyBundle mockDiscussionReplyBundle;
	@Mock
	MarkdownEditorWidget mockMarkdownEditor;
	String replyId = "123";
	String message = "message";
	EditReplyModal modal;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		modal = new EditReplyModal(mockView, mockDiscussionForumClient, mockSynAlert, mockMarkdownEditor);
		modal.configure(replyId, message, mockCallback);
	}

	@Test
	public void testConstructor() {
		verify(mockView).setPresenter(modal);
		verify(mockView).setAlert(any(Widget.class));
		verify(mockView).setModalTitle(anyString());
		verify(mockView).setMarkdownEditor(any(Widget.class));
		verify(mockMarkdownEditor).hideUploadRelatedCommands();
	}

	@Test
	public void testShowDialog() {
		modal.show();
		verify(mockView).clear();
		verify(mockMarkdownEditor).configure(anyString());
		verify(mockView).showDialog();
	}

	@Test
	public void testHideDialog() {
		modal.hide();
		verify(mockView).hideDialog();
	}

	@Test
	public void asWidgetTest() {
		modal.asWidget();
		verify(mockView).asWidget();
	}

	@Test
	public void testOnSaveInvalidArgument() {
		when(mockMarkdownEditor.getMarkdown()).thenReturn("");
		modal.onSave();
		verify(mockSynAlert).clear();
		verify(mockSynAlert).showError(anyString());
		verify(mockView, never()).hideDialog();
		verifyZeroInteractions(mockDiscussionForumClient);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testOnSaveSuccess() {
		when(mockMarkdownEditor.getMarkdown()).thenReturn("message");
		AsyncMockStubber.callSuccessWith(mockDiscussionReplyBundle)
			.when(mockDiscussionForumClient).updateReplyMessage(anyString(), any(UpdateReplyMessage.class), any(AsyncCallback.class));
		modal.onSave();
		verify(mockSynAlert).clear();
		verify(mockView).showSaving();
		verify(mockView).hideDialog();
		verify(mockView).showSuccess(anyString(), anyString());
		verify(mockDiscussionForumClient).updateReplyMessage(anyString(), any(UpdateReplyMessage.class), any(AsyncCallback.class));
		verify(mockCallback).invoke();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testOnSaveFailure() {
		when(mockMarkdownEditor.getMarkdown()).thenReturn("message");
		AsyncMockStubber.callFailureWith(new Exception())
			.when(mockDiscussionForumClient).updateReplyMessage(anyString(), any(UpdateReplyMessage.class), any(AsyncCallback.class));

		modal.onSave();
		verify(mockSynAlert).clear();
		verify(mockView).showSaving();
		verify(mockDiscussionForumClient).updateReplyMessage(anyString(), any(UpdateReplyMessage.class), any(AsyncCallback.class));
		verifyZeroInteractions(mockCallback);
		verify(mockSynAlert).handleException(any(Throwable.class));
		verify(mockView).resetButton();
	}
}
