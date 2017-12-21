/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.oauth2.internal.authorizationcode.functional;

import static java.util.Arrays.asList;

import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Before;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.Parameter;

@RunnerDelegateTo(Parameterized.class)
public class AuthorizationCodeThroughProxyTestCase extends AbstractAuthorizationCodeFullConfigTestCase {

  @Parameter
  public String configFile;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {configFile, "operations/operations-config.xml"};
  }

  @Parameters
  public static Collection<Object> data() {
    String inlineProxyConfig = "authorization-code/authorization-code-through-proxy-inline.xml";
    String globalProxyConfig = "authorization-code/authorization-code-through-proxy.xml";
    return asList(new Object[] {inlineProxyConfig, globalProxyConfig});
  }

  @Before
  public void setUp() {
    configureProxyWireMock();
  }

  @Override
  protected boolean isRequestThroughProxy() {
    return true;
  }

}
