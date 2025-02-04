/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class ClientApiOAuthContextServiceImpl implements OAuthContextService {

  private static final Class<?> clientApi_ctxClass;
  private static final Class<?> clientApi_ctxWithStateClass;
  private static final Constructor<?> clientApi_ctxWithStateCopyConstructor;
  private static final Method clientApi_getResourceOwnerId;
  private static final Method clientApi_getTokenResponseParameters;
  private static final Method clientApi_getAccessToken;
  private static final Method clientApi_getRefreshToken;
  private static final Method clientApi_getExpiresIn;
  private static final Method clientApi_getState;

  static {
    try {
      clientApi_ctxClass = Class.forName("org.mule.oauth.client.api.state.ResourceOwnerOAuthContext");
      clientApi_ctxWithStateClass = Class.forName("org.mule.oauth.client.api.state.ResourceOwnerOAuthContextWithRefreshState");
      clientApi_ctxWithStateCopyConstructor = clientApi_ctxWithStateClass.getConstructor(clientApi_ctxClass);
      clientApi_getResourceOwnerId = clientApi_ctxClass.getMethod("getResourceOwnerId");
      clientApi_getTokenResponseParameters = clientApi_ctxClass.getMethod("getTokenResponseParameters");
      clientApi_getAccessToken = clientApi_ctxClass.getMethod("getAccessToken");
      clientApi_getRefreshToken = clientApi_ctxClass.getMethod("getRefreshToken");
      clientApi_getExpiresIn = clientApi_ctxClass.getMethod("getExpiresIn");
      clientApi_getState = clientApi_ctxClass.getMethod("getState");
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Object createResourceOwnerOAuthContext(String resourceOwnerId, String name, LockFactory lockFactory) {
    try {
      return clientApi_ctxClass.getConstructor(String.class).newInstance(resourceOwnerId);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Object migrateContextIfNeeded(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    try {
      return clientApi_ctxWithStateCopyConstructor.newInstance(resourceOwnerOAuthContext);
    } catch (InstantiationException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Lock getRefreshUserOAuthContextLock(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    return lockFactory.createLock(name + "-" + getResourceOwnerId(resourceOwnerOAuthContext));
  }

  @Override
  public Lock createRefreshUserOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId) {
    return lockProvider.createLock(lockNamePrefix + "-" + resourceOwnerId);
  }

  @Override
  public <T> OAuthDancerBuilder<T> dancerName(OAuthDancerBuilder<T> dancerBuilder, String name) {
    return dancerBuilder;
  }

  @Override
  public String getResourceOwnerId(Object resourceOwnerOAuthContext) {
    try {
      return (String) clientApi_getResourceOwnerId.invoke(resourceOwnerOAuthContext);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Map<String, Object> getTokenResponseParameters(Object contextForResourceOwner) {
    try {
      return (Map<String, Object>) clientApi_getTokenResponseParameters.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getAccessToken(Object contextForResourceOwner) {
    try {
      return (String) clientApi_getAccessToken.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getRefreshToken(Object contextForResourceOwner) {
    try {
      return (String) clientApi_getRefreshToken.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getExpiresIn(Object contextForResourceOwner) {
    try {
      return (String) clientApi_getExpiresIn.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getState(Object contextForResourceOwner) {
    try {
      return (String) clientApi_getState.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
