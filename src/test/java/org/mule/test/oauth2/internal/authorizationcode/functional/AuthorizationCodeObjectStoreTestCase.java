/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.store.SimpleMemoryObjectStore;

public class AuthorizationCodeObjectStoreTestCase extends AuthorizationCodeMinimalConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-object-store-config.xml";
  }

  @Override
  public void hitRedirectUrlAndGetToken() throws Exception {
    super.hitRedirectUrlAndGetToken();

    SimpleMemoryObjectStore objectStore = registry.<SimpleMemoryObjectStore>lookupByName("customObjectStore").get();
    assertThat(objectStore.allKeys().size(), is(1));
    assertThat(objectStore.retrieve("default"), notNullValue());
  }
}
