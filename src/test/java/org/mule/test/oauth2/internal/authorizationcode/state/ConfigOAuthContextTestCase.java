/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.state;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Answers;

@SmallTest
public class ConfigOAuthContextTestCase extends AbstractMuleTestCase {

  public static final String USER_ID = "user";
  public static final String TEST_CONFIG_NAME = "test-config-name";
  private final LockFactory mockLockFactory = mock(LockFactory.class, Answers.RETURNS_DEEP_STUBS.get());
  private final Map<String, ResourceOwnerOAuthContext> objectStore = new HashMap<>();

  @Test
  public void nonExistentUserIdReturnNewConfig() throws Exception {
    assertThat(new ConfigOAuthContext(mockLockFactory, objectStore, TEST_CONFIG_NAME).getContextForResourceOwner(USER_ID),
               notNullValue());
  }

}
