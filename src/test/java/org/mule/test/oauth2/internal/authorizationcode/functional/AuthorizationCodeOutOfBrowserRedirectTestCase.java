/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static java.lang.String.format;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.ServerCreationException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

public class AuthorizationCodeOutOfBrowserRedirectTestCase extends AbstractOAuthAuthorizationTestCase {

  @Inject
  private HttpService httpService;

  @Rule
  public SystemProperty localAuthorizationUrl =
      new SystemProperty("local.authorization.url",
                         format("%s://localhost:%d/authorization", getProtocol(), localHostPort.getNumber()));

  @Rule
  public SystemProperty authorizationUrl =
      new SystemProperty("authorization.url",
                         format("%s://localhost:%d" + AUTHORIZE_PATH, getProtocol(), resolveOauthServerPort()));

  private int resolveOauthServerPort() {
    return getProtocol().equals("http") ? oauthServerPort.getNumber() : oauthHttpsServerPort.getNumber();
  }

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("%s://localhost:%d" + TOKEN_PATH, getProtocol(), resolveOauthServerPort()));

  @Override
  protected String getRedirectUrl() {
    return "urn:ietf:wg:oauth:2.0:oob";
  }

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-minimal-config.xml";
  }

  /**
   * Expect No failure since the application didn't create a listener in the port
   */
  @Test
  public void listenerCreated() throws ServerCreationException {
    HttpServer server = httpService.getServerFactory()
        .create(new HttpServerConfiguration.Builder().setHost("localhost").setPort(localHostPort.getNumber()).setName("test")
            .build());

    server.dispose();
  }
}
