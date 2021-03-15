/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.http.api.HttpHeaders.Values.KEEP_ALIVE;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.encodeString;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE;
import static org.mule.service.oauth.internal.OAuthConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
import static org.mule.service.oauth.internal.OAuthConstants.GRANT_TYPE_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.REDIRECT_URI_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.service.oauth.internal.OAuthConstants.SCOPE_PARAMETER;

import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.UnsupportedEncodingException;

import org.junit.Rule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.common.collect.ImmutableMap;

@ArtifactClassLoaderRunnerConfig(exportPluginClasses = {ConfigOAuthContext.class})
public abstract class AbstractOAuthAuthorizationTestCase extends MuleArtifactFunctionalTestCase {

  public static final int REQUEST_TIMEOUT = 5000;

  public static final String TOKEN_PATH = "/token";
  public static final String AUTHENTICATION_CODE = "9WGJOBZXAvSibONGAxVlLuML0e0RhfX4";
  public static final String ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4gu6vpCobc2ya";
  public static final String REFRESH_TOKEN = "cry825cyCs2O0j7tRXXVS4AXNu7hsO5wbWjcBoFFcJePy5zZwuQEevIp6hsUaywp";
  public static final String EXPIRES_IN = "3897";
  public static final String AUTHORIZE_PATH = "/authorize";
  private static final String PROXY_CONNECTION_HEADER = "Proxy-Connection";
  protected final DynamicPort oauthServerPort = new DynamicPort("port2");
  protected final DynamicPort oauthHttpsServerPort = new DynamicPort("port3");
  private final String keyStorePath = currentThread().getContextClassLoader().getResource("ssltest-keystore.jks").getPath();
  private final String keyStorePassword = "changeit";

  @Rule
  public DynamicPort proxyPort = new DynamicPort("proxyPort");

  @Rule
  public final DynamicPort localHostPort = new DynamicPort("localHostPort");

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(oauthServerPort.getNumber())
      .httpsPort(oauthHttpsServerPort.getNumber()).keystorePath(keyStorePath)
      .keystorePassword(keyStorePassword));

  @Rule
  public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");

  @Rule
  public SystemProperty clientSecret = new SystemProperty("client.secret", "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T");

  @Rule
  public SystemProperty scopes = new SystemProperty("scopes", "expected scope");

  @Rule
  public SystemProperty state = new SystemProperty("state", "expected state");

  @Rule
  public SystemProperty oauthServerPortNumber =
      new SystemProperty("oauth.server.port", String.valueOf(oauthServerPort.getNumber()));

  @Rule
  public SystemProperty localCallbackPath = new SystemProperty("local.callback.path", "/callback");

  @Rule
  public SystemProperty localCallbackUrl = new SystemProperty("local.callback.url", getRedirectUrl());

  @Rule
  public WireMockRule proxyWireMockRule = new WireMockRule(wireMockConfig().port(proxyPort.getNumber()));


  protected String getRedirectUrl() {
    return format("%s://localhost:%d%s", getProtocol(), localHostPort.getNumber(), localCallbackPath.getValue());
  }

  protected String getProtocol() {
    return "http";
  }

  protected void configureProxyWireMock() {
    proxyWireMockRule.stubFor(post(urlMatching(TOKEN_PATH))
        .willReturn(aResponse().proxiedFrom(format("http://localhost:%s", oauthServerPort.getNumber()))));
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType() {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(ACCESS_TOKEN);
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(String accessToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(accessToken, REFRESH_TOKEN);
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(String accessToken, String refreshToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody("{" + "\""
        + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER
        + "\":" + EXPIRES_IN + "," + "\"" + REFRESH_TOKEN_PARAMETER + "\":\"" + refreshToken + "\"}",
                                                                                   MediaType.JSON.toRfcString());
  }

  protected void configureWireMockToExpectOfflineTokenPathRequestForAuthorizationCodeGrantType(String accessToken) {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody("{" + "\""
        + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER
        + "\":" + EXPIRES_IN + "}", MediaType.JSON.toRfcString());
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody(String body, String contentType) {
    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH))
        .willReturn(aResponse().withBody(body).withHeader(CONTENT_TYPE, contentType)));
  }

  protected void configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeAndFail() {
    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.getStatusCode())));
  }

  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType() {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(ACCESS_TOKEN, EXPIRES_IN, 50);
  }

  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ImmutableMap customParameters) {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ACCESS_TOKEN, customParameters);
  }

  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(String accessToken, String expiresIn,
                                                                                        Integer fixedDelay) {
    wireMockRule
        .stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody("{" + "\"" + ACCESS_TOKEN_PARAMETER
            + "\":\"" + accessToken + "\"," + "\"" + EXPIRES_IN_PARAMETER + "\":\"" + expiresIn + "\"}")
            .withHeader(CONTENT_TYPE, MediaType.JSON.toRfcString())
            .withFixedDelay(fixedDelay)));
  }

  protected void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(String accessToken) {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(accessToken,
                                                                                          new ImmutableMap.Builder().build());
  }

  private void configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(String accessToken,
                                                                                                     ImmutableMap customParameters) {
    customParameters = new ImmutableMap.Builder().putAll(customParameters).put(ACCESS_TOKEN_PARAMETER, accessToken)
        .put(EXPIRES_IN_PARAMETER, EXPIRES_IN).build();
    final ImmutableMap.Builder bodyParametersMapBuilder = new ImmutableMap.Builder();
    for (Object customParameterName : customParameters.keySet()) {
      bodyParametersMapBuilder.put(customParameterName, customParameters.get(customParameterName));
    }
    final String body = encodeString(bodyParametersMapBuilder.build(), UTF_8);
    wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(aResponse().withBody(body)
        .withHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())));
  }

  protected void verifyRequestDoneToTokenUrlForAuthorizationCode() throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForAuthorizationCode(false);
  }

  protected void verifyRequestDoneToTokenUrlForAuthorizationCode(boolean requestThroughProxy)
      throws UnsupportedEncodingException {
    final RequestPatternBuilder verification = postRequestedFor(urlEqualTo(TOKEN_PATH))
        .withRequestBody(containing(CLIENT_ID_PARAMETER + "=" + encode(clientId.getValue(), UTF_8.name())))
        .withRequestBody(containing(CODE_PARAMETER + "=" + encode(AUTHENTICATION_CODE, UTF_8.name())))
        .withRequestBody(containing(CLIENT_SECRET_PARAMETER + "=" + encode(clientSecret.getValue(), UTF_8.name())))
        .withRequestBody(containing(GRANT_TYPE_PARAMETER + "=" + encode(GRANT_TYPE_AUTHENTICATION_CODE, UTF_8.name())))
        .withRequestBody(containing(REDIRECT_URI_PARAMETER + "=" + encode(localCallbackUrl.getValue(), UTF_8.name())));
    if (requestThroughProxy) {
      verification.withHeader(PROXY_CONNECTION_HEADER, containing(KEEP_ALIVE));
      proxyWireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH)));
    }
    wireMockRule.verify(verification);
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentialsThroughProxy() throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForClientCredentials(null, false, true);
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentials() throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForClientCredentials(null);
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentials(String scope) throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForClientCredentials(scope, false);
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentials(String scope, boolean encodeInBody)
      throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForClientCredentials(scope, encodeInBody, false);
  }

  protected void verifyRequestDoneToTokenUrlForClientCredentials(String scope, boolean encodeInBody, boolean requestThroughProxy)
      throws UnsupportedEncodingException {
    final RequestPatternBuilder verification =
        postRequestedFor(urlEqualTo(TOKEN_PATH))
            .withRequestBody(containing(GRANT_TYPE_PARAMETER + "=" + encode(GRANT_TYPE_CLIENT_CREDENTIALS, UTF_8.name())));
    if (encodeInBody == true) {
      verification
          .withRequestBody(containing(CLIENT_ID_PARAMETER + "=" + encode(clientId.getValue(), UTF_8.name())))
          .withRequestBody(containing(CLIENT_SECRET_PARAMETER + "=" + encode(clientSecret.getValue(), UTF_8.name())));
    } else {
      verification.withHeader(AUTHORIZATION, containing("Basic "
          + encodeBase64String(format("%s:%s", clientId.getValue(), clientSecret.getValue()).getBytes())));
    }

    if (requestThroughProxy) {
      verification.withHeader(PROXY_CONNECTION_HEADER, containing(KEEP_ALIVE));
      proxyWireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH)));
    }

    if (scope != null) {
      verification.withRequestBody(containing(SCOPE_PARAMETER + "=" + encode(scope, UTF_8.name())));
    }
    wireMockRule.verify(verification);
  }

  protected void verifyTokenManagerAccessToken() throws Exception {
    assertThat(flowRunner("retrieveAccessTokenWithoutOwnerIdFlow").run().getMessage().getPayload().getValue(),
               is(ACCESS_TOKEN));
  }

  protected void verifyTokenManagerAccessToken(String resourceOwnerId, String accessToken) throws Exception {
    // TODO MULE-12009: once fixed, replace this flow call for the extensions call client
    assertThat(runFlowWithResourceOwnerId("retrieveAccessTokenFlow", resourceOwnerId), is(accessToken));
  }

  protected void verifyTokenManagerRefreshToken() throws Exception {
    // TODO MULE-12009: once fixed, replace this flow call for the extensions call client
    assertThat(runFlowWithResourceOwnerId("retrieveRefreshTokenFlow", DEFAULT_RESOURCE_OWNER_ID), is(REFRESH_TOKEN));
  }

  protected void verifyTokenManagerExpiresIn() throws Exception {
    // TODO MULE-12009: once fixed, replace this flow call for the extensions call client
    assertThat(runFlowWithResourceOwnerId("retrieveExpiresInFlow", DEFAULT_RESOURCE_OWNER_ID), is(EXPIRES_IN));
  }

  protected void verifyTokenManagerState() throws Exception {
    assertThat(flowRunner("retrieveStateWithoutOwnerIdFlow").run().getMessage().getPayload().getValue(),
               is(state.getValue()));
  }

  protected void verifyTokenManagerState(String resourceOwnerId, String state) throws Exception {
    // TODO MULE-12009: once fixed, replace this flow call for the extensions call client
    assertThat(runFlowWithResourceOwnerId("retrieveStateFlow", resourceOwnerId), is(state));
  }

  protected void verifyTokenManagerCustomParameterExtractor(String key, String expectedCustomParameterExtractor)
      throws Exception {
    // TODO MULE-12009: once fixed, replace this flow call for the extensions call client
    assertThat(flowRunner("retrieveCustomTokenResponseParamFlow").withVariable("key", key).run().getMessage().getPayload()
        .getValue(),
               is(expectedCustomParameterExtractor));
  }

  private Object runFlowWithResourceOwnerId(String flowName, String defaultResourceOwnerId) throws Exception {
    return flowRunner(flowName).withVariable("resourceOwnerId", defaultResourceOwnerId).run().getMessage().getPayload()
        .getValue();
  }

  protected void setTokens(ResourceOwnerOAuthContext resourceOwnerOauthContext, String accessToken, String refreshToken) {
    try {
      resourceOwnerOauthContext.getClass().getDeclaredMethod("setAccessToken", String.class)
          .invoke(resourceOwnerOauthContext, accessToken);
      resourceOwnerOauthContext.getClass().getDeclaredMethod("setRefreshToken", String.class)
          .invoke(resourceOwnerOauthContext, refreshToken);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }
}
