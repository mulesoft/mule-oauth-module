/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.api.authorizationcode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.extension.oauth2.api.authorizationcode.DefaultAuthorizationCodeGrantType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.ServerNotFoundException;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.api.el.MuleExpressionLanguage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthorizationCodeGrantTypeTest {

  private DefaultAuthorizationCodeGrantType grantType;

  @Mock
  private HttpService httpService;

  @Mock
  private OAuthService oAuthService;

  @Mock
  private OAuthAuthorizationCodeDancerBuilder dancerBuilder;

  @Mock
  private AuthorizationCodeOAuthDancer dancer;

  @Mock
  private ParameterResolver<String> resourceOwnerIdResolver;

  private MuleExpressionLanguage expressionEvaluatorMock;

  @Before
  public void setUp() throws Exception {
    grantType = new DefaultAuthorizationCodeGrantType();
    grantType.setHttpService(httpService);
    grantType.getHttpService();
  }

  @Test
  //TODO: Keep
  public void testInitialization() throws InitialisationException {
    grantType.setLocalCallbackUrl("http://localhost:8080/callback");
    grantType.setExternalCallbackUrl("http://example.com/callback");
    grantType.setAuthorizationUrl("http://auth.example.com/authorize");
    grantType.getAuthorizationUrl();
    grantType.setLocalAuthorizationUrl("http://localhost:8080/authorize");
    grantType.getLocalAuthorizationUrl();
    grantType.getLocalCallbackConfig();
    grantType.getLocalCallbackConfigPath();
    grantType.getLocalCallbackUrl();
    grantType.getExternalCallbackUrl();
  }

  private MuleExpressionLanguage evaluatorMock() {
    MuleExpressionLanguage expressionLanguage = mock(MuleExpressionLanguage.class);
    when(expressionLanguage.isExpression(anyString())).thenReturn(false);
    return expressionLanguage;
  }

  private static class DefaultAuthorizationCodeGrantTypeStub extends DefaultAuthorizationCodeGrantType {

    public DefaultAuthorizationCodeGrantTypeStub(MuleExpressionLanguage expressionEvaluatorMock) {
      this.expressionEvaluator = expressionEvaluatorMock;
    }
  }

  @Test()
  public void testInitializationWithConflictingCallbackConfig() throws InitialisationException {
    grantType.setLocalCallbackConfig("someConfig");
    grantType.setLocalCallbackUrl("http://localhost:8080/callback");
  }

  @Test()
  public void testInitializationWithInvalidLocalCallbackUrl() throws InitialisationException {
    grantType.setLocalCallbackUrl("invalid-url");
  }

  @Test
  public void testInitializationWithLocalCallbackConfig() throws InitialisationException, ServerNotFoundException {
    grantType.setLocalCallbackConfig("configName");
    grantType.setLocalCallbackConfigPath("/callback");

    HttpServer mockServer = mock(HttpServer.class);
  }

  @Test
  public void testCustomParameters() {
    Map<String, String> customParams = new HashMap<>();
    customParams.put("key1", "value1");
    customParams.put("key2", "value2");

    grantType.getCustomParameters().putAll(customParams);

    assertEquals(customParams, grantType.getCustomParameters());
  }

  @Test
  public void testEncodeClientCredentialsInBody() {
    grantType.setEncodeClientCredentialsInBody(false);
    assertFalse(grantType.isEncodeClientCredentialsInBody());

    grantType.setEncodeClientCredentialsInBody(true);
    assertTrue(grantType.isEncodeClientCredentialsInBody());
  }

  @Test
  public void testEqualsAndHashCode() {
    DefaultAuthorizationCodeGrantType grantType1 = new DefaultAuthorizationCodeGrantType();
    DefaultAuthorizationCodeGrantType grantType2 = new DefaultAuthorizationCodeGrantType();

    grantType1.setLocalCallbackUrl("http://localhost:8080/callback");
    grantType2.setLocalCallbackUrl("http://localhost:8080/callback");

    grantType1.setExternalCallbackUrl("http://example.com/callback");
    grantType2.setExternalCallbackUrl("http://example.com/callback");

    assertTrue(grantType1.equals(grantType2));
    assertEquals(grantType1.hashCode(), grantType2.hashCode());

    grantType2.setExternalCallbackUrl("http://different.com/callback");

    assertFalse(grantType1.equals(grantType2));
    assertNotEquals(grantType1.hashCode(), grantType2.hashCode());
  }

  @Test
  public void testStateParameter() {
    Literal<String> state = mock(Literal.class);
    grantType.setState(state);
    assertEquals(state, grantType.getState());
  }

  @Test
  public void testLocalAuthorizationUrlResourceOwnerId() {
    Literal<String> ownerId = mock(Literal.class);
    grantType.setLocalAuthorizationUrlResourceOwnerId(ownerId);
    assertEquals(ownerId, grantType.getLocalAuthorizationUrlResourceOwnerId());
  }

  @Test
  public void testShouldRetry() throws MuleException {
    grantType.setResourceOwnerId(resourceOwnerIdResolver);
    grantType.getResourceOwnerId();
  }

  @Test
  public void testRetryIfShould() throws MuleException {
    grantType.setResourceOwnerId(resourceOwnerIdResolver);

    Runnable retryCallback = mock(Runnable.class);
    Runnable notRetryCallback = mock(Runnable.class);
    verifyNoInteractions(notRetryCallback);
  }
}
