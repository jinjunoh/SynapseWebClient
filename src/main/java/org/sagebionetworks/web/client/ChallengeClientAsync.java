package org.sagebionetworks.web.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.web.shared.ChallengePagedResults;
import org.sagebionetworks.web.shared.ChallengeTeamPagedResults;
import org.sagebionetworks.web.shared.UserProfilePagedResults;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ChallengeClientAsync {

	void getEvaluations(List<String> evaluationIds, AsyncCallback<String> callback) throws RestServiceException;
	void getAvailableEvaluations(AsyncCallback<String> callback) throws RestServiceException;
	void getAvailableEvaluations(Set<String> targetEvaluationIds, AsyncCallback<String> callback) throws RestServiceException;
	void getSharableEvaluations(String entityId, AsyncCallback<ArrayList<String>> callback);
	
	/**
	 * Create a new Submission object.  Callback returning the updated version of the Submission object
	 * @param submissionJson
	 * @param etag
	 * @param callback
	 */
	void createSubmission(Submission submission, String etag, String teamId, String memberStateHash, AsyncCallback<Submission> callback) throws RestServiceException;
	void getSubmissionTeams(String userId, String challengeId, AsyncCallback<List<Team>> submissionTeams);
	
	void registerChallengeTeam(ChallengeTeam challengeTeam, AsyncCallback<ChallengeTeam> callback);
	void unregisterChallengeTeam(String challengeId, String teamId, AsyncCallback<Void> callback);
	void updateRegisteredChallengeTeam(ChallengeTeam challengeTeam, AsyncCallback<ChallengeTeam> callback);
	void getChallengeTeams(String userId, String challengeId, Integer limit, Integer offset, AsyncCallback<ChallengeTeamPagedResults> callback);
	void getChallengeParticipants(boolean affiliated, String challengeId, Integer limit, Integer offset, AsyncCallback<UserProfilePagedResults> callback);
	void getChallenge(String projectId, AsyncCallback<Challenge> callback);
	void getChallenges(String userId, Integer limit, Integer offset, AsyncCallback<ChallengePagedResults> callback);
	void getRegistratableTeams(String challengeId, AsyncCallback<List<Team>> callback);
	
	void getUserEvaluationPermissions(String evalId, AsyncCallback<String> callback); 
	void getEvaluationAcl(String evalId, AsyncCallback<String> callback);
	void updateEvaluationAcl(AccessControlList acl, AsyncCallback<AccessControlList> callback);
	
	
	/**
	 * Get all unique submission user aliases associated to the available evaluations (OPEN evaluations that the current user has joined).
	 * The return list is sorted by Submission created date.
	 * @param callback
	 * @throws RestServiceException
	 */
	void getAvailableEvaluationsSubmitterAliases(AsyncCallback<String> callback) throws RestServiceException;

	/**
	 * Return true if the current user has created at least one submission in the given evaluations
	 * @param evaluationIds
	 * @param callback
	 * @throws RestServiceException
	 */
	void hasSubmitted(AsyncCallback<Boolean> callback)	throws RestServiceException;
}
