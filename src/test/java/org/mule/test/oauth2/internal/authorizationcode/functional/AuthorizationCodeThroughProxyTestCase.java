/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.oauth2.internal.authorizationcode.functional;

import org.junit.Before;

public class AuthorizationCodeThroughProxyTestCase extends AbstractAuthorizationCodeFullConfigTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"authorization-code/authorization-code-through-proxy.xml", "operations/operations-config.xml"};
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
