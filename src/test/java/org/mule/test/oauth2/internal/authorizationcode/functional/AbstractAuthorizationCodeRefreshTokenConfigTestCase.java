/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.service.oauth.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.GRANT_TYPE_REFRESH_TOKEN;
import static org.mule.service.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import org.junit.Rule;

public abstract class AbstractAuthorizationCodeRefreshTokenConfigTestCase extends AbstractOAuthAuthorizationTestCase {

  private static final String RESOURCE_PATH = "/resource";
  public static final String RESOURCE_RESULT = "resource result";
  public static final String REFRESHED_ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4guasdfsdfa";

  public static final String TEST_HEADER_NAME = "test_name";
  public static final String TEST_HEADER_VALUE = "test_value";

  @Rule
  public SystemProperty originalPayload = new SystemProperty("payload.original", TEST_PAYLOAD);
  @Rule
  public SystemProperty localAuthorizationUrl =
      new SystemProperty("local.authorization.url",
                         format("http://localhost:%d/authorization", localHostPort.getNumber()));
  @Rule
  public SystemProperty authorizationUrl =
      new SystemProperty("authorization.url", format("http://localhost:%d" + AUTHORIZE_PATH, oauthServerPort.getNumber()));
  @Rule
  public SystemProperty redirectUrl =
      new SystemProperty("redirect.url", format("http://localhost:%d/redirect", localHostPort.getNumber()));
  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));
  @Rule
  public SystemProperty tokenHost = new SystemProperty("token.host", format("localhost"));
  @Rule
  public SystemProperty tokenPort = new SystemProperty("token.port", valueOf(oauthServerPort.getNumber()));
  @Rule
  public SystemProperty tokenPath = new SystemProperty("token.path", TOKEN_PATH);

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-refresh-token-config.xml";
  }

  protected void executeRefreshToken(String flowName, String oauthConfigName, String userId, int failureStatusCode)
      throws Exception {
    configureResourceResponsesForRefreshToken(oauthConfigName, userId, failureStatusCode);

    final CoreEvent result = flowRunner(flowName).withPayload("message").withVariable("userId", userId)
        .withVariable("headerName", TEST_HEADER_NAME).withVariable("headerValue", TEST_HEADER_VALUE).run();
    assertThat(result.getMessage().getPayload().getValue(), is(RESOURCE_RESULT));

    wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
        .withRequestBody(containing(CLIENT_ID_PARAMETER + "=" + encode(clientId.getValue(), UTF_8.name())))
        .withRequestBody(containing(REFRESH_TOKEN_PARAMETER + "=" + encode(REFRESH_TOKEN, UTF_8.name())))
        .withRequestBody(containing(CLIENT_SECRET_PARAMETER + "=" + encode(clientSecret.getValue(), UTF_8.name())))
        .withRequestBody(containing(GRANT_TYPE_PARAMETER + "=" + encode(GRANT_TYPE_REFRESH_TOKEN, UTF_8.name()))));

    wireMockRule.verify(2, postRequestedFor(urlEqualTo(RESOURCE_PATH)).withRequestBody(equalTo(TEST_PAYLOAD)));
    wireMockRule.verify(2,
                        postRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(TEST_HEADER_NAME, containing(TEST_HEADER_VALUE)));
  }

  protected void executeRefreshTokenUsingOldRefreshTokenOnTokenCallAndRevokedByUsers(String flowName, String oauthConfigName,
                                                                                     String userId, int resourceFailureStatusCode,
                                                                                     int tokenFailureStatusCode)
      throws Exception {
    configureResourceResponsesForRefreshToken(oauthConfigName, userId, resourceFailureStatusCode);

    wireMockRule
        .stubFor(post(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, containing(REFRESHED_ACCESS_TOKEN))
            .willReturn(aResponse().withStatus(tokenFailureStatusCode).withBody("")));
    flowRunner(flowName).withPayload("message").withVariable("userId", userId).withVariable("headerName", TEST_HEADER_NAME)
        .withVariable("headerValue", TEST_HEADER_VALUE).run();
  }

  private void configureResourceResponsesForRefreshToken(String oauthConfigName, String userId, int failureStatusCode) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(REFRESHED_ACCESS_TOKEN);

    wireMockRule
        .stubFor(post(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, containing(REFRESHED_ACCESS_TOKEN))
            .willReturn(aResponse().withStatus(200).withBody(RESOURCE_RESULT)));
    wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, containing(ACCESS_TOKEN))
        .willReturn(aResponse().withStatus(failureStatusCode).withBody("")));

    final ConfigOAuthContext configOAuthContext = getTokenManagerConfig().getConfigOAuthContext();
    final ResourceOwnerOAuthContext resourceOwnerOauthContext = configOAuthContext.getContextForResourceOwner(userId);
    setTokens(resourceOwnerOauthContext, ACCESS_TOKEN, REFRESH_TOKEN);
    configOAuthContext.updateResourceOwnerOAuthContext(resourceOwnerOauthContext);
  }

  protected abstract TokenManagerConfig getTokenManagerConfig();

}

