/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.locks.Lock;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;
import org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class OAuthContextServiceAdapterTestCase {

  private static final String RESOURCE_OWNER_ID = "testUser";
  private static final String CONFIG_NAME = "testConfig";
  private static final String ACCESS_TOKEN = "testAccessToken";
  private static final String REFRESH_TOKEN = "testRefreshToken";
  private static final String EXPIRES_IN = "3600";
  private static final String STATE = "testState";

  private LockFactory lockFactory;
  private Lock mockLock;

  @Before
  public void setUp() {
    lockFactory = mock(LockFactory.class);
    mockLock = mock(Lock.class);
    when(lockFactory.createLock(anyString())).thenReturn(mockLock);
  }

  @Test
  public void testCreateResourceOwnerOAuthContext() {
    Object context = OAuthContextServiceAdapter.createResourceOwnerOAuthContext(RESOURCE_OWNER_ID, CONFIG_NAME, lockFactory);
    assertNotNull(context);
    assertEquals(DefaultResourceOwnerOAuthContext.class, context.getClass());
  }

  @Test
  public void testGetResourceOwnerId() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    String ownerId = OAuthContextServiceAdapter.getResourceOwnerId(context);
    assertEquals(RESOURCE_OWNER_ID, ownerId);
  }

  @Test
  public void testGetTokenResponseParameters() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    Map<String, Object> params = new HashMap<>();
    params.put("testKey", "testValue");
    context.setTokenResponseParameters(params);

    Map<String, Object> result = OAuthContextServiceAdapter.getTokenResponseParameters(context);
    assertEquals(params, result);
  }

  @Test
  public void testGetAccessToken() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    context.setAccessToken(ACCESS_TOKEN);

    String result = OAuthContextServiceAdapter.getAccessToken(context);
    assertEquals(ACCESS_TOKEN, result);
  }

  @Test
  public void testGetRefreshToken() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    context.setRefreshToken(REFRESH_TOKEN);

    String result = OAuthContextServiceAdapter.getRefreshToken(context);
    assertEquals(REFRESH_TOKEN, result);
  }

  @Test
  public void testGetExpiresIn() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    context.setExpiresIn(EXPIRES_IN);

    String result = OAuthContextServiceAdapter.getExpiresIn(context);
    assertEquals(EXPIRES_IN, result);
  }

  @Test
  public void testGetState() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    context.setState(STATE);

    String result = OAuthContextServiceAdapter.getState(context);
    assertEquals(STATE, result);
  }

  @Test
  public void testGetRefreshUserOAuthContextLock() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    Lock result = OAuthContextServiceAdapter.getRefreshUserOAuthContextLock(context, CONFIG_NAME, lockFactory);
    assertNotNull(result);
  }

  @Test
  public void testCreateRefreshUserOAuthContextLock() {
    Lock result = OAuthContextServiceAdapter.createRefreshUserOAuthContextLock(CONFIG_NAME, lockFactory, RESOURCE_OWNER_ID);
    assertNotNull(result);
  }

  @Test
  public void testDancerName() {
    OAuthDancerBuilder<?> builder = mock(OAuthDancerBuilder.class);
    OAuthDancerBuilder<?> result = OAuthContextServiceAdapter.dancerName(builder, CONFIG_NAME);
    assertNotNull(result);
  }

  @Test
  public void testMigrateContextIfNeeded() {
    DefaultResourceOwnerOAuthContext context = new DefaultResourceOwnerOAuthContext(mockLock, RESOURCE_OWNER_ID);
    Object result = OAuthContextServiceAdapter.migrateContextIfNeeded(context, CONFIG_NAME, lockFactory);
    assertNotNull(result);
    assertEquals(DefaultResourceOwnerOAuthContext.class, result.getClass());
  }

  private String anyString() {
    return any();
  }
}
