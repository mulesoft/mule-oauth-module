/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.runtime.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractClientCredentialsBasicTestCase extends AbstractOAuthAuthorizationTestCase {

  private static final String RESOURCE_PATH = "/resource";
  private static final String NEW_ACCESS_TOKEN = "abcdefghjkl";
  public static final String TEST_FLOW_NAME = "testFlow";
  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials/client-credentials-minimal-config.xml", "operations/operations-config.xml"};
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType();
  }

  @Test
  public void authenticationHeaderIsSentWithRequest() throws Exception {
    wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse().withBody(TEST_MESSAGE)));

    flowRunner(TEST_FLOW_NAME).withPayload(TEST_MESSAGE).run();

    wireMockRule.verify(postRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, equalTo("Bearer " + ACCESS_TOKEN)));
  }

  @Test
  public void authenticationFailedTriggersRefreshAccessToken() throws Exception {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(NEW_ACCESS_TOKEN, EXPIRES_IN, 50);

    wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, containing(ACCESS_TOKEN))
        .willReturn(aResponse().withStatus(UNAUTHORIZED.getStatusCode()).withHeader(WWW_AUTHENTICATE,
                                                                                    "Basic realm=\"myRealm\"")));

    wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, containing(NEW_ACCESS_TOKEN))
        .willReturn(aResponse().withBody(TEST_MESSAGE).withStatus(OK.getStatusCode())));

    flowRunner("testFlow").withPayload(TEST_MESSAGE).run();

    verifyRequestDoneToTokenUrlForClientCredentials();

    wireMockRule
        .verify(postRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, equalTo("Bearer " + NEW_ACCESS_TOKEN)));
  }

  @Test
  @DisplayName("W-11680326: Fails authentication and refresh three times")
  public void authenticationFailedTriggersRefreshAccessTokenThreeTimes() throws Exception {

    for (int i = 0; i < 3; i++) {
      configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(NEW_ACCESS_TOKEN, EXPIRES_IN, 50);

      wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, containing(ACCESS_TOKEN))
          .willReturn(aResponse().withStatus(UNAUTHORIZED.getStatusCode()).withHeader(WWW_AUTHENTICATE,
                                                                                      "Basic realm=\"myRealm\"")));

      wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, containing(NEW_ACCESS_TOKEN))
          .willReturn(aResponse().withBody(TEST_MESSAGE).withStatus(OK.getStatusCode())));

      flowRunner("testFlow").withPayload(TEST_MESSAGE).run();

      verifyRequestDoneToTokenUrlForClientCredentials();

      wireMockRule
          .verify(postRequestedFor(urlEqualTo(RESOURCE_PATH)).withHeader(AUTHORIZATION, equalTo("Bearer " + NEW_ACCESS_TOKEN)));
    }
  }
}
