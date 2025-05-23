/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig.defaultTokenManagerConfigIndex;
import static org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig.getTokenManagerConfigByName;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.getAccessToken;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.getRefreshToken;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.appendQueryParam;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.NO_AUTHORIZATION_CODE_STATUS;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.TOKEN_NOT_FOUND_STATUS;
import static org.mule.runtime.oauth.api.OAuthAuthorizationStatusCode.TOKEN_URL_CALL_FAILED_STATUS;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.STATE_PARAMETER;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import static java.lang.String.format;
import static java.lang.String.valueOf;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.client.fluent.Request.Get;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.processor.FlowAssert;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeAuthorizationFailureTestCase extends AbstractAuthorizationCodeBasicTestCase {

  private static final String EXPECTED_STATUS_CODE_SYSTEM_PROPERTY = "expectedStatusCode";
  public static final String REFRESHED_ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4guasdfsdfa";

  @Rule
  public DynamicPort onCompleteUrlPort = new DynamicPort("onCompleteUrlPort");

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-failure-scenarios-config.xml";
  }

  @Test
  public void urlRedirectHandlerDoNotRetrieveAuthorizationCode() throws Exception {
    Response response = Get(localCallbackUrl.getValue()).connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();
    HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
  }

  @Test
  public void urlRedirectHandlerDoNotRetrieveAuthorizationCodeWithOnCompleteRedirect() throws Exception {
    testWithSystemProperty(EXPECTED_STATUS_CODE_SYSTEM_PROPERTY, valueOf(NO_AUTHORIZATION_CODE_STATUS),
                           () -> {
                             Response response = Get(getRedirectUrlWithOnCompleteUrlQueryParam())
                                 .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();
                             response.returnResponse();

                             FlowAssert.verify();
                           });
  }

  @Test
  public void callToTokenUrlFails() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeAndFail();

    verifyCallToRedirectUrlFails();
  }

  @Test
  public void callToTokenUrlFailsWithOnCompleteRedirect() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeAndFail();

    testWithSystemProperty(EXPECTED_STATUS_CODE_SYSTEM_PROPERTY, valueOf(TOKEN_URL_CALL_FAILED_STATUS),
                           () -> {
                             Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()).connectTimeout(REQUEST_TIMEOUT)
                                 .socketTimeout(REQUEST_TIMEOUT).execute();

                             FlowAssert.verify();
                           });

    verifyCallToRedirectUrlFails();
  }

  @Test
  public void callToTokenUrlSuccessButNoAccessTokenRetrievedEmptyResponse() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody(EMPTY, EMPTY);

    verifyCallToRedirectUrlFails();
  }

  @Test
  public void callToTokenUrlSuccessButNoAccessTokenRetrieved() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(null, null);

    testWithSystemProperty(EXPECTED_STATUS_CODE_SYSTEM_PROPERTY, valueOf(TOKEN_NOT_FOUND_STATUS),
                           () -> {
                             Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()).connectTimeout(REQUEST_TIMEOUT)
                                 .socketTimeout(REQUEST_TIMEOUT).execute();

                             FlowAssert.verify();
                           });

  }

  @Test
  public void callToTokenUrlSuccessButNoRefreshTokenRetrieved() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(ACCESS_TOKEN, null);
    Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()).connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT)
        .execute();
    final TokenManagerConfig tokenManagerConfig =
        getTokenManagerConfigByName("default-token-manager-config-" + (defaultTokenManagerConfigIndex.get() - 1));
    Object oauthContext = tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);

    assertThat(getAccessToken(oauthContext), is(ACCESS_TOKEN));
    assertThat(getRefreshToken(oauthContext), is(nullValue()));
  }

  @Test
  public void callToTokenUrlSuccessWithOfflineRefreshTokenSupportedByAuthorizationServer() throws Exception {
    // During the initial call to token url it returns an access_token and refresh_token
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(ACCESS_TOKEN, REFRESH_TOKEN);
    Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()).connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT)
        .execute();

    final TokenManagerConfig tokenManagerConfig =
        getTokenManagerConfigByName("default-token-manager-config-" + (defaultTokenManagerConfigIndex.get() - 1));
    Object oauthContext = tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);

    // Validates that the oauth context has both tokens
    assertThat(getAccessToken(oauthContext), is(ACCESS_TOKEN));
    assertThat(getRefreshToken(oauthContext), is(REFRESH_TOKEN));

    // In order to validate that oauth context is updated (due to it is persisted with OS) we now do another call but only
    // returning the REFRESHED_ACCESS_TOKEN without
    // a refresh token.
    configureWireMockToExpectOfflineTokenPathRequestForAuthorizationCodeGrantType(REFRESHED_ACCESS_TOKEN);
    Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()).connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT)
        .execute();

    // We need to retrieve the oauth context again to get it updated...
    oauthContext = tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);

    assertThat(getAccessToken(oauthContext), is(REFRESHED_ACCESS_TOKEN));
    assertThat(getRefreshToken(oauthContext), is(REFRESH_TOKEN));
  }

  private void verifyCallToRedirectUrlFails() throws IOException {
    Response response = Get(format(localCallbackUrl.getValue() + "%s%s=%s", "?", CODE_PARAMETER, AUTHENTICATION_CODE))
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();
    HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  private String getRedirectUrlWithOnCompleteUrlQueryParam() {
    return appendQueryParam(localCallbackUrl.getValue(), STATE_PARAMETER,
                            ":onCompleteRedirectTo=" + format("http://localhost:%s/afterLogin", onCompleteUrlPort.getNumber()));
  }

  private String getRedirectUrlWithOnCompleteUrlAndCodeQueryParams() {
    return appendQueryParam(getRedirectUrlWithOnCompleteUrlQueryParam(), CODE_PARAMETER, AUTHENTICATION_CODE);
  }

}
