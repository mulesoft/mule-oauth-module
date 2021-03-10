/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static java.lang.String.format;
import static java.lang.Thread.sleep;

import org.mule.functional.junit4.rules.HttpServerRule;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Issue;

public class ClientCredentialsDynamicHttpConfigTestCase extends AbstractOAuthAuthorizationTestCase {

  @Rule
  public SystemProperty expirationMaxIdleTime = new SystemProperty("expirationMaxIdleTime", "500");

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public HttpServerRule httpServerRules = new HttpServerRule("port");

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Override
  protected String getConfigFile() {
    return "client-credentials/client-credentials-dynamic-http-config.xml";
  }

  @Test
  @Issue("OAMOD-4")
  public void testAuthenticationLifecycle() throws Exception {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantType();

    flowRunner("do-request").run();
    // generate another http config, that will expire during the for loop below
    flowRunner("do-request").withVariable("host", "127.0.0.1").run();

    for (int i = 0; i < 5; ++i) {
      flowRunner("do-request").run();
      sleep(100);
    }

    sleep(100);
    // ensure that the expired config does not affect other configs
    flowRunner("do-request").run();
  }

}
