/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Protocol.HTTPS;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.encodeQueryString;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.encodeString;
import static org.mule.service.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.STATE_PARAMETER;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractAuthorizationCodeFullConfigTestCase extends AbstractOAuthAuthorizationTestCase {

  private final String CUSTOM_RESPONSE_PARAMETER1_VALUE = "token-resp-value1";
  private final String CUSTOM_RESPONSE_PARAMETER2_VALUE = "token-resp-value2";

  @Rule
  public SystemProperty authenticationRequestParam1 = new SystemProperty("auth.request.param1", "auth-req-param1");

  @Rule
  public SystemProperty authenticationRequestParam2 = new SystemProperty("auth.request.param2", "auth-req-param2");

  @Rule
  public SystemProperty authenticationRequestValue1 = new SystemProperty("auth.request.value1", "auth-req-value1");

  @Rule
  public SystemProperty authenticationRequestValue2 = new SystemProperty("auth.request.value2", "auth-req-value2");

  @Rule
  public SystemProperty customTokenResponseParameter1Name = new SystemProperty("custom.param.extractor1", "token-resp-param1");

  @Rule
  public SystemProperty customTokenResponseParameter2Name = new SystemProperty("custom.param.extractor2", "token-resp-param2");

  @Rule
  public SystemProperty localAuthorizationUrl =
      new SystemProperty("local.authorization.url",
                         String.format("%s://localhost:%d/authorization", getProtocol(), localHostPort.getNumber()));

  @Rule
  public SystemProperty authorizationUrl =
      new SystemProperty("authorization.url",
                         String.format("%s://localhost:%d" + AUTHORIZE_PATH, getProtocol(), getOAuthPort()));

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url",
                         String.format("%s://localhost:%d" + TOKEN_PATH, getProtocol(), getOAuthPort()));

  @Rule
  public TestHttpClient httpClient =
      new TestHttpClient.Builder(getService(HttpService.class)).tlsContextFactory(this::createClientTlsContextFactory).build();


  @Test
  public void hitRedirectUrlAndGetToken() throws Exception {

    final ImmutableMap<Object, Object> tokenUrlResponseParameters =
        ImmutableMap.builder().put(ACCESS_TOKEN_PARAMETER, ACCESS_TOKEN)
            .put(EXPIRES_IN_PARAMETER, EXPIRES_IN).put(REFRESH_TOKEN_PARAMETER, REFRESH_TOKEN)
            .put(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE)
            .put(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE).build();


    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse()
        .withHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
        .withBody(encodeString(tokenUrlResponseParameters, UTF_8))));

    final ImmutableMap<String, String> redirectUrlQueryParams = ImmutableMap.<String, String>builder()
        .put(CODE_PARAMETER, AUTHENTICATION_CODE).put(STATE_PARAMETER, state.getValue()).build();

    HttpRequest request = HttpRequest.builder()
        .uri(localCallbackUrl.getValue() + "?" + encodeQueryString(redirectUrlQueryParams)).method(GET).build();
    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    verifyRequestDoneToTokenUrlForAuthorizationCode(isRequestThroughProxy());

    verifyTokenManagerAccessToken();
    verifyTokenManagerRefreshToken();
    verifyTokenManagerExpiresIn();
    verifyTokenManagerState();
    verifyTokenManagerCustomParameterExtractor(customTokenResponseParameter1Name.getValue(), CUSTOM_RESPONSE_PARAMETER1_VALUE);
    verifyTokenManagerCustomParameterExtractor(customTokenResponseParameter2Name.getValue(), CUSTOM_RESPONSE_PARAMETER2_VALUE);
  }

  protected TlsContextFactory createClientTlsContextFactory() {
    return null;
  }

  protected boolean isRequestThroughProxy() {
    return false;
  }

  private int getOAuthPort() {
    return getProtocol().equals(HTTPS.getScheme()) ? oauthHttpsServerPort.getNumber() : oauthServerPort.getNumber();
  }

}

