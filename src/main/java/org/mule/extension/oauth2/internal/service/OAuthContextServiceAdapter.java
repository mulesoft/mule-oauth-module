/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import static java.lang.Class.forName;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;
import org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * This provides the adapter layer required for newer versions of the module to work on older versions of the runtime/OAuth
 * service.
 * <p>
 * This must be removed once the {@code minMuleVersion} of this extension is upgraded to 4.3
 *
 * @since 1.2.0, 1.1.9
 *
 * @deprecated to be removed when {@code minMuleVersion} is upgraded to 4.3.0
 */
@Deprecated
public final class OAuthContextServiceAdapter {

  private static Class<?> ctxClass;
  private static Class<?> ctxWithStateClass;
  private static Constructor<?> ctxWithStateConstructor;
  private static Constructor<?> ctxWithStateCopyConstructor;
  private static Method createRefreshUserOAuthContextLock = null;
  private static Method getRefreshUserOAuthContextLock = null;
  private static Method getResourceOwnerId = null;
  private static Method getTokenResponseParameters = null;
  private static Method getRefreshToken = null;
  private static Method getAccessToken = null;
  private static Method getExpiresIn = null;
  private static Method getState = null;
  private static Method dancerName = null;

  static {
    // This code uses reflection to detect what version of the OAuth service API is in the runtime.
    // In case the new methods are detected, those are called via reflection. Can't use them directly because this code has to
    // compile against the older version of the service API.
    // In case the new methods are not found, the original logic is executed.

    try {
      ctxClass = getContextClass();
      ctxWithStateClass = getContextWithStateClass();

      ctxWithStateConstructor = ctxWithStateClass.getConstructor(String.class);
      ctxWithStateCopyConstructor = ctxWithStateClass.getConstructor(ctxClass);

      getResourceOwnerId = ctxWithStateClass.getDeclaredMethod("getResourceOwnerId");
      getTokenResponseParameters = ctxWithStateClass.getDeclaredMethod("getTokenResponseParameters");
      getRefreshToken = ctxWithStateClass.getDeclaredMethod("getRefreshToken");
      getAccessToken = ctxWithStateClass.getDeclaredMethod("getAccessToken");
      getExpiresIn = ctxWithStateClass.getDeclaredMethod("getExpiresIn");
      getState = ctxWithStateClass.getDeclaredMethod("getState");

      getRefreshUserOAuthContextLock =
          ctxWithStateClass.getDeclaredMethod("getRefreshOAuthContextLock", String.class, LockFactory.class);

      createRefreshUserOAuthContextLock =
          ctxWithStateClass.getDeclaredMethod("createRefreshOAuthContextLock", String.class, LockFactory.class, String.class);

      dancerName = OAuthDancerBuilder.class.getDeclaredMethod("name", String.class);
    } catch (NoSuchMethodException | ClassNotFoundException e) {
      e.printStackTrace();
      // Nothing to do, this is just using an older version of the api
    } catch (SecurityException e) {
      throw e;
    }
  }

  private OAuthContextServiceAdapter() {
    // Nothing to do
  }

  public static Object createResourceOwnerOAuthContext(String resourceOwnerId, String name, LockFactory lockFactory) {
    if (ctxWithStateClass != null) {
      try {
        return ctxWithStateConstructor.newInstance(resourceOwnerId);
      } catch (InstantiationException | InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      return new DefaultResourceOwnerOAuthContext(createLockForResourceOwner(resourceOwnerId, name, lockFactory),
                                                  resourceOwnerId);
    }
  }

  private static Lock createLockForResourceOwner(String resourceOwnerId, String configName, LockFactory lockFactory) {
    return lockFactory.createLock(configName + "-" + resourceOwnerId);
  }

  public static Object migrateContextIfNeeded(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    if (ctxWithStateClass != null) {
      try {
        return ctxWithStateCopyConstructor.newInstance(resourceOwnerOAuthContext);
      } catch (InstantiationException | InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      ((DefaultResourceOwnerOAuthContext) resourceOwnerOAuthContext)
          .setRefreshUserOAuthContextLock(createLockForResourceOwner(getResourceOwnerId(resourceOwnerOAuthContext), name,
                                                                     lockFactory));
      return resourceOwnerOAuthContext;
    }
  }

  public static Lock getRefreshUserOAuthContextLock(Object resourceOwnerOAuthContext, String name,
                                                    LockFactory lockFactory) {
    if (getRefreshUserOAuthContextLock != null) {
      try {
        return (Lock) getRefreshUserOAuthContextLock.invoke(resourceOwnerOAuthContext, name, lockFactory);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else {
      return ((DefaultResourceOwnerOAuthContext) resourceOwnerOAuthContext).getRefreshUserOAuthContextLock();
    }
  }

  public static Lock createRefreshUserOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId) {
    if (createRefreshUserOAuthContextLock != null) {
      try {
        return (Lock) createRefreshUserOAuthContextLock.invoke(null, lockNamePrefix, lockProvider, resourceOwnerId);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else {
      return lockProvider.createLock(lockNamePrefix + "-config-oauth-context");
    }
  }

  public static <T> OAuthDancerBuilder<T> dancerName(OAuthDancerBuilder<T> dancerBuilder, String name) {
    if (dancerName != null) {
      try {
        return (OAuthDancerBuilder<T>) dancerName.invoke(dancerBuilder, name);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else {
      return dancerBuilder;
    }
  }

  private static String getResourceOwnerId(Object resourceOwnerOAuthContext) {
    try {
      return (String) getResourceOwnerId.invoke(resourceOwnerOAuthContext);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static Class<?> getContextWithStateClass() throws ClassNotFoundException {
    try {
      return forName("org.mule.oauth.client.api.state.ResourceOwnerOAuthContext");
    } catch (ClassNotFoundException ignored) {
      return forName("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext");
    }
  }

  private static Class<?> getContextClass() throws ClassNotFoundException {
    try {
      return forName("org.mule.oauth.client.api.state.ResourceOwnerOAuthContextWithRefreshState");
    } catch (ClassNotFoundException ignored) {
      return forName("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContextWithRefreshState");
    }
  }

  public static Map<String, Object> getTokenResponseParameters(Object contextForResourceOwner) {
    try {
      return (Map<String, Object>) getTokenResponseParameters.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getAccessToken(Object contextForResourceOwner) {
    try {
      return (String) getAccessToken.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getRefreshToken(Object contextForResourceOwner) {
    try {
      return (String) getRefreshToken.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getExpiresIn(Object contextForResourceOwner) {
    try {
      return (String) getExpiresIn.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getState(Object contextForResourceOwner) {
    try {
      return (String) getState.invoke(contextForResourceOwner);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
