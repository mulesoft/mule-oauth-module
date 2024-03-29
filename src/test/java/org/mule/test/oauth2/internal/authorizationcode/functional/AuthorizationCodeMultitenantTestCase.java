/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static org.apache.http.client.fluent.Request.Get;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.encodeQueryString;
import static org.mule.service.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.STATE_PARAMETER;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.test.oauth2.asserter.AuthorizationRequestAsserter;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeMultitenantTestCase extends AbstractOAuthAuthorizationTestCase {

  public static final String USER_ID_JOHN = "john";
  public static final String JOHN_ACCESS_TOKEN = "123456789";
  public static final String JOHN_STATE = "rock";
  public static final String USER_ID_TONY = "tony";
  public static final String TONY_ACCESS_TOKEN = "abcdefghi";
  public static final String TONY_STATE = "punk";
  public static final String NO_STATE = null;

  @Rule
  public SystemProperty localAuthorizationUrl =
      new SystemProperty("local.authorization.url", format("http://localhost:%d/authorization", localHostPort.getNumber()));
  @Rule
  public SystemProperty authorizationUrl =
      new SystemProperty("authorization.url", format("http://localhost:%d" + AUTHORIZE_PATH, oauthServerPort.getNumber()));
  @Rule
  public SystemProperty localCallbackUrl =
      new SystemProperty("local.callback.url", format("http://localhost:%d/callback", localHostPort.getNumber()));
  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"authorization-code/authorization-code-multitenant-config.xml", "operations/operations-config.xml"};
  }

  @Test
  public void danceWithCustomResourceOwnerId() throws Exception {
    executeForUserWithAccessToken(USER_ID_JOHN, JOHN_ACCESS_TOKEN, NO_STATE);
    wireMockRule.resetRequests();
    executeForUserWithAccessToken(USER_ID_TONY, TONY_ACCESS_TOKEN, NO_STATE);

    verifyTokenManagerAccessToken(USER_ID_JOHN, JOHN_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_JOHN, null);
    verifyTokenManagerAccessToken(USER_ID_TONY, TONY_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_TONY, null);
  }

  @Test
  public void danceWithCustomResourceOwnerIdAndState() throws Exception {
    executeForUserWithAccessToken(USER_ID_JOHN, JOHN_ACCESS_TOKEN, JOHN_STATE);
    wireMockRule.resetRequests();
    executeForUserWithAccessToken(USER_ID_TONY, TONY_ACCESS_TOKEN, TONY_STATE);

    verifyTokenManagerAccessToken(USER_ID_JOHN, JOHN_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_JOHN, JOHN_STATE);
    verifyTokenManagerAccessToken(USER_ID_TONY, TONY_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_TONY, TONY_STATE);
  }

  @Test
  public void refreshToken() throws Exception {
    executeForUserWithAccessToken(USER_ID_JOHN, JOHN_ACCESS_TOKEN, NO_STATE);
    wireMockRule.resetRequests();
    executeForUserWithAccessToken(USER_ID_TONY, TONY_ACCESS_TOKEN, NO_STATE);

    verifyTokenManagerAccessToken(USER_ID_JOHN, JOHN_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_JOHN, null);
    verifyTokenManagerAccessToken(USER_ID_TONY, TONY_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_TONY, null);
  }

  private void executeForUserWithAccessToken(String userId, String accessToken, String state) throws IOException {
    wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(OK.getStatusCode())));

    final String expectedState = (state == null ? "" : state) + ":resourceOwnerId=" + userId;

    final ImmutableMap.Builder<String, String> localAuthorizationUrlParametersBuilder =
        ImmutableMap.<String, String>builder().put("userId", userId);
    if (state != NO_STATE) {
      localAuthorizationUrlParametersBuilder.put("state", state);
    }

    Get(localAuthorizationUrl.getValue() + "?" + encodeQueryString(localAuthorizationUrlParametersBuilder.build()))
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();

    AuthorizationRequestAsserter.create((wireMockRule.findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*"))).get(0)))
        .assertStateIs(expectedState);

    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse()
        .withBody("{" + "\"" + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER + "\":"
            + EXPIRES_IN + "," + "\"" + REFRESH_TOKEN_PARAMETER + "\":\"" + REFRESH_TOKEN + "\"}")
        .withHeader(CONTENT_TYPE, MediaType.JSON.toRfcString())));

    final String redirectUrlQueryParams =
        encodeQueryString(ImmutableMap.<String, String>builder().put(CODE_PARAMETER, AUTHENTICATION_CODE)
            .put(STATE_PARAMETER, expectedState).build());
    Get(localCallbackUrl.getValue() + "?" + redirectUrlQueryParams).connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT)
        .execute();
  }

}
