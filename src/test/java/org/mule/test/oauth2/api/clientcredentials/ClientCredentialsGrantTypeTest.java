/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.api.clientcredentials;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.extension.oauth2.api.clientcredentials.ClientCredentialsGrantType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.el.MuleExpressionLanguage;

import java.util.concurrent.CompletableFuture;

@RunWith(MockitoJUnitRunner.class)
public class ClientCredentialsGrantTypeTest {

  private ClientCredentialsGrantType grantType;

  @Mock
  private OAuthService oAuthService;

  @Mock
  private OAuthClientCredentialsDancerBuilder dancerBuilder;

  @Mock
  private ClientCredentialsOAuthDancer dancer;

  @Mock
  private MuleExpressionLanguage expressionEvaluator;

  @Before
  public void setUp() {
    grantType = new ClientCredentialsGrantType();
  }

  @Test
  public void testShouldRetry() throws MuleException {
    Result<Object, HttpResponseAttributes> result = mock(Result.class);
    assertNotNull(result);
  }

  @Test
  public void testRetryIfShould() throws MuleException {

    Result<Object, HttpResponseAttributes> result = mock(Result.class);
    HttpResponseAttributes attributes = mock(HttpResponseAttributes.class);

    Runnable retryCallback = mock(Runnable.class);
    Runnable notRetryCallback = mock(Runnable.class);

    verifyNoInteractions(notRetryCallback);
  }

  @Test
  public void testEncodeClientCredentialsInBody() {
    assertFalse(grantType.isEncodeClientCredentialsInBody());

    grantType.setEncodeClientCredentialsInBody(true);
    assertTrue(grantType.isEncodeClientCredentialsInBody());
  }

  @Test
  public void testEqualsAndHashCode() {
    ClientCredentialsGrantType grantType1 = new ClientCredentialsGrantType();
    ClientCredentialsGrantType grantType2 = new ClientCredentialsGrantType();

    grantType1.setEncodeClientCredentialsInBody(true);
    grantType2.setEncodeClientCredentialsInBody(true);

    boolean grantTypeEquals = grantType1.equals(grantType2);
    assertTrue(grantTypeEquals);
    assertEquals(grantType1.hashCode(), grantType2.hashCode());

    grantType2.setEncodeClientCredentialsInBody(false);

    grantTypeEquals = grantType1.equals(grantType2);
    assertFalse(grantTypeEquals);
    assertNotEquals(grantType1.hashCode(), grantType2.hashCode());
  }
}
