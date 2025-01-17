package org.sagebionetworks.web.server.servlet;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.web.server.servlet.filter.HtmlInjectionFilter;
import org.sagebionetworks.web.shared.WebConstants;

/**
 * Handles file handler uploads.
 *
 * @author jay
 *
 */
public class ProjectAliasServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  protected static final ThreadLocal<HttpServletRequest> perThreadRequest =
    new ThreadLocal<HttpServletRequest>();

  private SynapseProvider synapseProvider = new SynapseProviderImpl();
  private TokenProvider tokenProvider = () ->
    UserDataProvider.getThreadLocalUserToken(
      ProjectAliasServlet.perThreadRequest.get()
    );
  private RequestHostProvider requestHostProvider = () ->
    UserDataProvider.getThreadLocalRequestHost(
      ProjectAliasServlet.perThreadRequest.get()
    );

  /**
   * Unit test can override this.
   */
  public void setSynapseProvider(SynapseProvider synapseProvider) {
    this.synapseProvider = synapseProvider;
  }

  /**
   * Unit test uses this to provide a mock token provider
   *
   * @param tokenProvider
   */
  public void setTokenProvider(TokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
    throws ServletException, IOException {
    ProjectAliasServlet.perThreadRequest.set(arg0);
    super.service(arg0, arg1);
  }

  @Override
  public void service(ServletRequest arg0, ServletResponse arg1)
    throws ServletException, IOException {
    super.service(arg0, arg1);
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    // instruct not to cache
    response.setHeader(
      WebConstants.CACHE_CONTROL_KEY,
      WebConstants.CACHE_CONTROL_VALUE_NO_CACHE
    ); // Set standard HTTP/1.1 no-cache headers.
    response.setHeader(WebConstants.PRAGMA_KEY, WebConstants.NO_CACHE_VALUE); // Set standard HTTP/1.0 no-cache header.
    response.setDateHeader(WebConstants.EXPIRES_KEY, 0L); // Proxy
    String token = null;
    try {
      token = getToken();
    } catch (Throwable e) {
      // unable to get session token, so it's an anonymous request
    }
    HttpServletRequest httpRqst = (HttpServletRequest) request;
    URL requestURL = new URL(httpRqst.getRequestURL().toString());
    try {
      SynapseClient client = createNewClient(token);
      perThreadRequest.set(httpRqst);
      String path = requestURL.getPath().substring(1);
      String[] tokens = path.split("/");
      String alias = tokens[0];

      // is alias a known GWT place name?  If so, do nothing
      if (HtmlInjectionFilter.isGWTPlace(alias)) {
        return;
      }

      EntityId entityId = client.getEntityIdByAlias(alias);
      StringBuilder newPathBuilder = new StringBuilder();
      newPathBuilder.append("/Synapse:");
      newPathBuilder.append(entityId.getId());
      for (int i = 1; i < tokens.length; i++) {
        newPathBuilder.append("/");
        newPathBuilder.append(tokens[i]);
      }
      String newPath = newPathBuilder.toString();
      URL redirectURL = new URL(
        requestURL.getProtocol(),
        requestURL.getHost(),
        requestURL.getPort(),
        newPath
      );
      response.sendRedirect(response.encodeRedirectURL(redirectURL.toString()));
    } catch (SynapseException e) {
      // redirect to error place
      response.sendRedirect(
        FileHandleAssociationServlet.getBaseUrl(request) +
        FileHandleAssociationServlet.ERROR_PLACE +
        URLEncoder.encode(e.getMessage())
      );
    }
  }

  /**
   * Get the session token
   */
  public String getToken() {
    return tokenProvider.getToken();
  }

  /**
   * Create a new Synapse client.
   *
   * @return
   */
  private SynapseClient createNewClient(String accessToken) {
    SynapseClient client = synapseProvider.createNewClient(
      requestHostProvider.getRequestHost()
    );
    if (accessToken != null) client.setBearerAuthorizationToken(accessToken);
    return client;
  }
}
