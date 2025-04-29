/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.api.tokenmanager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;

import java.io.Serializable;

@RunWith(MockitoJUnitRunner.class)
public class TokenManagerConfigTest {

  @Mock
  private ObjectStoreManager objectStoreManager;

  @Mock
  private LockFactory lockFactory;

  @Mock
  private ObjectStore objectStore;

  private TokenManagerConfig tokenManagerConfig;

  @Before
  public void setUp() {
    tokenManagerConfig = new TokenManagerConfig<>();
    tokenManagerConfig.setName("test-token-manager");
    tokenManagerConfig.setObjectStoreManager(objectStoreManager);
    tokenManagerConfig.setLockFactory(lockFactory);
  }

  @Test
  public void testInitializeWithoutObjectStore() throws InitialisationException {
    // Setup
    when(objectStoreManager.getOrCreateObjectStore(anyString(), any(ObjectStoreSettings.class)))
        .thenReturn(objectStore);

    // Execute
    tokenManagerConfig.initialise();

    // Verify
    verify(objectStoreManager).getOrCreateObjectStore(
                                                      eq("token-manager-store-test-token-manager"),
                                                      any(ObjectStoreSettings.class));
    assertNotNull(tokenManagerConfig.getConfigOAuthContext());
    assertSame(objectStore, tokenManagerConfig.getResolvedObjectStore());
  }

  @Test
  public void testInitializeWithObjectStore() throws InitialisationException {
    // Setup
    tokenManagerConfig.setObjectStore(objectStore);

    // Execute
    tokenManagerConfig.initialise();

    // Verify
    verify(objectStoreManager, never()).getOrCreateObjectStore(anyString(), any(ObjectStoreSettings.class));
    assertNotNull(tokenManagerConfig.getConfigOAuthContext());
    assertSame(objectStore, tokenManagerConfig.getResolvedObjectStore());
  }

  @Test
  public void testLifecycleMethods() throws Exception {
    // Setup
    tokenManagerConfig.setObjectStore(objectStore);
    tokenManagerConfig.initialise();

    // Test start
    tokenManagerConfig.start();
    //verify(objectStore).startMonitoring();

    // Test stop
    tokenManagerConfig.stop();
    //verify(objectStore).stopMonitoring();

    // Test dispose
    tokenManagerConfig.dispose();
    assertNull(tokenManagerConfig.getConfigOAuthContext());
  }

  @Test
  public void testCreateDefault() {
    // Execute
    TokenManagerConfig config = TokenManagerConfig.createDefault();

    // Verify
    assertNotNull(config);
    assertTrue(config.getName().startsWith("default-token-manager-config-"));
    assertNotNull(TokenManagerConfig.getTokenManagerConfigByName(config.getName()));
  }

  @Test
  public void testEqualsAndHashCode() {
    // Setup
    TokenManagerConfig config1 = new TokenManagerConfig<>();
    config1.setName("test1");
    TokenManagerConfig config2 = new TokenManagerConfig<>();
    config2.setName("test1");
    TokenManagerConfig config3 = new TokenManagerConfig<>();
    config3.setName("test2");

    // Verify equals
    assertTrue(config1.equals(config2));
    assertFalse(config1.equals(config3));
    assertFalse(config1.equals(null));
    assertFalse(config1.equals(new Object()));

    // Verify hashCode
    assertEquals(config1.hashCode(), config2.hashCode());
    assertNotEquals(config1.hashCode(), config3.hashCode());
  }

  @Test
  public void testDoubleInitialization() throws InitialisationException {
    // Setup
    when(objectStoreManager.getOrCreateObjectStore(anyString(), any(ObjectStoreSettings.class)))
        .thenReturn(objectStore);

    // Execute
    tokenManagerConfig.initialise();
    tokenManagerConfig.initialise(); // Should not throw exception

    // Verify
    verify(objectStoreManager, times(1)).getOrCreateObjectStore(anyString(), any(ObjectStoreSettings.class));
  }

  @Test
  public void testGettersAndSetters() {
    // Setup
    String name = "test-name";
    tokenManagerConfig.setName(name);

    // Verify
    assertEquals(name, tokenManagerConfig.getName());
    assertSame(objectStoreManager, tokenManagerConfig.getObjectStoreManager());
    assertSame(lockFactory, tokenManagerConfig.getLockFactory());
  }
}
