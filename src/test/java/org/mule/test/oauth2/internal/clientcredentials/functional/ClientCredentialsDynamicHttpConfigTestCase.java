/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.runtime.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.qameta.allure.Issue;

public class ClientCredentialsDynamicHttpConfigTestCase extends AbstractOAuthAuthorizationTestCase {

  private static final String NEW_ACCESS_TOKEN = "abcdefghjkl";

  @Rule
  public SystemProperty expirationMaxIdleTime = new SystemProperty("expirationMaxIdleTime", "500");

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public WireMockRule wireMockRuleApp = new WireMockRule(wireMockConfig().port(port.getNumber()));

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Override
  protected String getConfigFile() {
    return "client-credentials/client-credentials-dynamic-http-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();

    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(ACCESS_TOKEN, "10000", 50);
  }

  // In order to fully reproduce OAMOD-4, the oauth-service dependency needs to be one including the fix for MULE-17010
  @Test
  @Issue("OAMOD-4")
  public void testRefreshAfterAuthenticationLifecycle() throws Exception {
    final StubMapping okStub = wireMockRuleApp.stubFor(get(anyUrl()).withHeader(AUTHORIZATION, containing(ACCESS_TOKEN))
        .willReturn(aResponse().withStatus(OK.getStatusCode())));

    // generate another http config, that will expire during the for loop below
    flowRunner("do-request").withVariable("host", "127.0.0.1").run();

    for (int i = 0; i < 7; ++i) {
      flowRunner("do-request").run();
      sleep(100);
    }

    // force token refresh after a dynamic config has been disposed
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType(NEW_ACCESS_TOKEN, EXPIRES_IN, 500);

    wireMockRuleApp.removeStub(okStub);
    wireMockRuleApp.stubFor(get(anyUrl())
        .willReturn(aResponse()
            .withStatus(INTERNAL_SERVER_ERROR.getStatusCode())));
    wireMockRuleApp.stubFor(get(anyUrl()).withHeader(AUTHORIZATION, containing(ACCESS_TOKEN))
        .willReturn(aResponse()
            .withStatus(UNAUTHORIZED.getStatusCode())
            .withHeader(WWW_AUTHENTICATE, "Basic realm=\"myRealm\"")));
    wireMockRuleApp.stubFor(get(anyUrl()).withHeader(AUTHORIZATION, containing(NEW_ACCESS_TOKEN))
        .willReturn(aResponse().withBody(TEST_MESSAGE).withStatus(OK.getStatusCode())));

    // ensure that the expired config did not affect other configs
    // These are run in parallel to force the scenario where an scheduler is needed to wait for a refresh in progress
    final ExecutorService executor = newFixedThreadPool(8);
    final List<Future<CoreEvent>> futures = new ArrayList<>(8);
    for (int i = 0; i < 8; ++i) {
      futures.add(executor.submit(() -> flowRunner("do-request").run()));
      // TODO OAMOD-8 This sleep can be removed when a new major version is released. It is necessary to prevent the test from being
      //  flaky, but the bug that caused a deadlock in the oauth service was fixed in version 2.0.0 of the oauth service (MULE-18169).
      sleep(10);
    }
    for (Future<CoreEvent> future : futures) {
      future.get();
    }
  }

}
