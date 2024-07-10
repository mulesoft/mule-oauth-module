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
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.appendQueryParam;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.STATE_PARAMETER;

import static java.lang.String.format;

import static org.apache.http.client.fluent.Request.Get;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.tck.junit4.rule.DynamicPort;

import io.qameta.allure.Description;
import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeAuthorizationCustomTokenExprTestCase extends AbstractAuthorizationCodeBasicTestCase {

  @Rule
  public DynamicPort onCompleteUrlPort = new DynamicPort("onCompleteUrlPort");

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-custom-token-expr-config.xml";
  }

  @Test
  @Description("Can extract tokens from a text response instead of JSON (default). This happens when the server omits the mime-type or sets it to text/plain.")
  public void callToTokenUrlSuccess() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(ACCESS_TOKEN, REFRESH_TOKEN);
    Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()).connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT)
        .execute();
    final TokenManagerConfig tokenManagerConfig =
        getTokenManagerConfigByName("default-token-manager-config-" + (defaultTokenManagerConfigIndex.get() - 1));
    Object oauthContext = tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);

    assertThat(getAccessToken(oauthContext), is(ACCESS_TOKEN));
    assertThat(getRefreshToken(oauthContext), is(REFRESH_TOKEN));
  }

  private String getRedirectUrlWithOnCompleteUrlQueryParam() {
    return appendQueryParam(localCallbackUrl.getValue(), STATE_PARAMETER,
                            ":onCompleteRedirectTo=" + format("http://localhost:%s/afterLogin", onCompleteUrlPort.getNumber()));
  }

  private String getRedirectUrlWithOnCompleteUrlAndCodeQueryParams() {
    return appendQueryParam(getRedirectUrlWithOnCompleteUrlQueryParam(), CODE_PARAMETER, AUTHENTICATION_CODE);
  }

  @Override
  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(String accessToken, String refreshToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody("{" + "\""
        + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER
        + "\":" + EXPIRES_IN + "," + "\"" + REFRESH_TOKEN_PARAMETER + "\":\"" + refreshToken + "\"}",
                                                                                   MediaType.TEXT.toRfcString());
  }
}
