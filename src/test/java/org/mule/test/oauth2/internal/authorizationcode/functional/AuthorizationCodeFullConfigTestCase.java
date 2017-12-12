/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Protocol.HTTPS;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.test.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.test.runner.RunnerDelegateTo;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class AuthorizationCodeFullConfigTestCase extends AbstractAuthorizationCodeFullConfigTestCase {


  private String[] configFiles;

  @Override
  protected String[] getConfigFiles() {
    return configFiles;
  }

  public AuthorizationCodeFullConfigTestCase(String[] configFiles) {
    this.configFiles = configFiles;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    final String operationsConfig = "operations/operations-config.xml";
    return Arrays.asList(new Object[][] {
        new String[] {"authorization-code/authorization-code-full-config-tls-global.xml", operationsConfig}},
                         new Object[][] {new String[] {"authorization-code/authorization-code-full-config-tls-nested.xml",
                             operationsConfig}});
  }

  @Test
  public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception {
    wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(OK.getStatusCode())));

    HttpRequest request = HttpRequest.builder().uri(localAuthorizationUrl.getValue()).method(GET).build();
    httpClient.send(request, RECEIVE_TIMEOUT, true, null);

    final List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
    assertThat(requests, hasSize(1));

    AuthorizationRequestAsserter.create((requests.get(0))).assertMethodIsGet().assertClientIdIs(clientId.getValue())
        .assertRedirectUriIs(localCallbackUrl.getValue()).assertScopeIs(scopes.getValue()).assertStateIs(state.getValue())
        .assertContainsCustomParameter(authenticationRequestParam1.getValue(), authenticationRequestValue1.getValue())
        .assertContainsCustomParameter(authenticationRequestParam2.getValue(), authenticationRequestValue2.getValue())
        .assertResponseTypeIsCode();
  }

  protected TlsContextFactory createClientTlsContextFactory() {
    try {
      return TlsContextFactory.builder()
          .trustStorePath("ssltest-cacerts.jks")
          .trustStorePassword("changeit")
          .keyStorePath("ssltest-keystore.jks")
          .keyStorePassword("changeit")
          .keyPassword("changeit")
          .build();
    } catch (CreateException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected String getProtocol() {
    return HTTPS.getScheme();
  }
}
