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

  private static Class<?> clientApi_ctxClass;
  private static Class<?> runtimeApi_ctxClass;
  private static Class<?> clientApi_ctxWithStateClass;
  private static Class<?> runtimeApi_ctxWithStateClass;
  private static Constructor<?> clientApi_ctxWithStateConstructor;
  private static Constructor<?> clientApi_ctxWithStateCopyConstructor;
  private static Constructor<?> runtimeApi_ctxWithStateCopyConstructor;
  private static Method clientApi_createRefreshUserOAuthContextLock = null;
  private static Method clientApi_getRefreshUserOAuthContextLock = null;
  private static Method clientApi_getResourceOwnerId = null;
  private static Method clientApi_getTokenResponseParameters = null;
  private static Method clientApi_getRefreshToken = null;
  private static Method clientApi_getAccessToken = null;
  private static Method clientApi_getExpiresIn = null;
  private static Method clientApi_getState = null;
  private static Method runtimeApi_createRefreshUserOAuthContextLock = null;
  private static Method runtimeApi_getRefreshUserOAuthContextLock = null;
  private static Method runtimeApi_getResourceOwnerId = null;
  private static Method runtimeApi_getTokenResponseParameters = null;
  private static Method runtimeApi_getRefreshToken = null;
  private static Method runtimeApi_getAccessToken = null;
  private static Method runtimeApi_getExpiresIn = null;
  private static Method runtimeApi_getState = null;
  private static Method dancerName = null;

  static {
    // This code uses reflection to detect what version of the OAuth service API is in the runtime.
    // In case the new methods are detected, those are called via reflection. Can't use them directly because this code has to
    // compile against the older version of the service API.
    // In case the new methods are not found, the original logic is executed.

    try {
      clientApi_ctxClass = classForNameOrNull("org.mule.oauth.client.api.state.ResourceOwnerOAuthContext");
      runtimeApi_ctxClass = classForNameOrNull("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext");

      clientApi_ctxWithStateClass =
          classForNameOrNull("org.mule.oauth.client.api.state.ResourceOwnerOAuthContextWithRefreshState");
      runtimeApi_ctxWithStateClass =
          classForNameOrNull("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContextWithRefreshState");

      if (clientApi_ctxWithStateClass != null) {
        clientApi_ctxWithStateConstructor = clientApi_ctxWithStateClass.getConstructor(String.class);
        clientApi_ctxWithStateCopyConstructor = clientApi_ctxWithStateClass.getConstructor(clientApi_ctxClass);

        clientApi_getResourceOwnerId = clientApi_ctxWithStateClass.getDeclaredMethod("getResourceOwnerId");
        clientApi_getTokenResponseParameters = clientApi_ctxWithStateClass.getDeclaredMethod("getTokenResponseParameters");
        clientApi_getRefreshToken = clientApi_ctxWithStateClass.getDeclaredMethod("getRefreshToken");
        clientApi_getAccessToken = clientApi_ctxWithStateClass.getDeclaredMethod("getAccessToken");
        clientApi_getExpiresIn = clientApi_ctxWithStateClass.getDeclaredMethod("getExpiresIn");
        clientApi_getState = clientApi_ctxWithStateClass.getDeclaredMethod("getState");
        clientApi_getRefreshUserOAuthContextLock =
            clientApi_ctxWithStateClass.getDeclaredMethod("getRefreshOAuthContextLock", String.class, LockFactory.class);
        clientApi_createRefreshUserOAuthContextLock = clientApi_ctxWithStateClass
            .getDeclaredMethod("createRefreshOAuthContextLock", String.class, LockFactory.class, String.class);
      }

      if (runtimeApi_ctxWithStateClass != null) {
        runtimeApi_ctxWithStateCopyConstructor = runtimeApi_ctxWithStateClass.getConstructor(runtimeApi_ctxClass);
        runtimeApi_getRefreshUserOAuthContextLock =
            runtimeApi_ctxWithStateClass.getDeclaredMethod("getRefreshOAuthContextLock", String.class, LockFactory.class);
        runtimeApi_createRefreshUserOAuthContextLock =
            runtimeApi_ctxWithStateClass.getDeclaredMethod("createRefreshOAuthContextLock", String.class, LockFactory.class,
                                                           String.class);
      }

      if (runtimeApi_ctxClass != null) {
        runtimeApi_getResourceOwnerId = methodForNameOrNull(runtimeApi_ctxClass, "getResourceOwnerId");
        runtimeApi_getTokenResponseParameters = methodForNameOrNull(runtimeApi_ctxClass, "getTokenResponseParameters");
        runtimeApi_getRefreshToken = methodForNameOrNull(runtimeApi_ctxClass, "getRefreshToken");
        runtimeApi_getAccessToken = methodForNameOrNull(runtimeApi_ctxClass, "getAccessToken");
        runtimeApi_getExpiresIn = methodForNameOrNull(runtimeApi_ctxClass, "getExpiresIn");
        runtimeApi_getState = methodForNameOrNull(runtimeApi_ctxClass, "getState");
      }

      dancerName = OAuthDancerBuilder.class.getDeclaredMethod("name", String.class);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      // Nothing to do, this is just using an older version of the api
    } catch (SecurityException e) {
      throw e;
    }
  }

  private static Class<?> classForNameOrNull(String className) {
    try {
      return forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static Method methodForNameOrNull(Class<?> clazz, String methodName) {
    try {
      return clazz.getDeclaredMethod(methodName);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private OAuthContextServiceAdapter() {
    // Nothing to do
  }

  public static Object createResourceOwnerOAuthContext(String resourceOwnerId, String name, LockFactory lockFactory) {
    if (clientApi_ctxWithStateClass != null) {
      try {
        return clientApi_ctxWithStateConstructor.newInstance(resourceOwnerId);
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
    if (clientApi_ctxWithStateClass != null && clientApi_ctxClass.isInstance(resourceOwnerOAuthContext)) {
      try {
        return clientApi_ctxWithStateCopyConstructor.newInstance(resourceOwnerOAuthContext);
      } catch (InstantiationException | InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      }
    } else if (runtimeApi_ctxWithStateClass != null && runtimeApi_ctxClass.isInstance(resourceOwnerOAuthContext)) {
      try {
        return runtimeApi_ctxWithStateCopyConstructor.newInstance(resourceOwnerOAuthContext);
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
    if (clientApi_getRefreshUserOAuthContextLock != null && clientApi_ctxClass.isInstance(resourceOwnerOAuthContext)) {
      try {
        return (Lock) clientApi_getRefreshUserOAuthContextLock.invoke(resourceOwnerOAuthContext, name, lockFactory);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else if (runtimeApi_getRefreshUserOAuthContextLock != null && runtimeApi_ctxClass.isInstance(resourceOwnerOAuthContext)) {
      try {
        return (Lock) runtimeApi_getRefreshUserOAuthContextLock.invoke(resourceOwnerOAuthContext, name, lockFactory);
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
    if (clientApi_createRefreshUserOAuthContextLock != null) {
      try {
        return (Lock) clientApi_createRefreshUserOAuthContextLock.invoke(null, lockNamePrefix, lockProvider, resourceOwnerId);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else if (runtimeApi_createRefreshUserOAuthContextLock != null) {
      try {
        return (Lock) runtimeApi_createRefreshUserOAuthContextLock.invoke(null, lockNamePrefix, lockProvider, resourceOwnerId);
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

  public static String getResourceOwnerId(Object resourceOwnerOAuthContext) {
    try {
      if (clientApi_ctxClass != null && clientApi_ctxClass.isInstance(resourceOwnerOAuthContext)) {
        return (String) clientApi_getResourceOwnerId.invoke(resourceOwnerOAuthContext);
      } else {
        return (String) runtimeApi_getResourceOwnerId.invoke(resourceOwnerOAuthContext);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, Object> getTokenResponseParameters(Object contextForResourceOwner) {
    try {
      if (clientApi_ctxClass != null && clientApi_ctxClass.isInstance(contextForResourceOwner)) {
        return (Map<String, Object>) clientApi_getTokenResponseParameters.invoke(contextForResourceOwner);
      } else {
        return (Map<String, Object>) runtimeApi_getTokenResponseParameters.invoke(contextForResourceOwner);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getAccessToken(Object contextForResourceOwner) {
    try {
      if (clientApi_ctxClass != null && clientApi_ctxClass.isInstance(contextForResourceOwner)) {
        return (String) clientApi_getAccessToken.invoke(contextForResourceOwner);
      } else {
        return (String) runtimeApi_getAccessToken.invoke(contextForResourceOwner);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getRefreshToken(Object contextForResourceOwner) {
    try {
      if (clientApi_ctxClass != null && clientApi_ctxClass.isInstance(contextForResourceOwner)) {
        return (String) clientApi_getRefreshToken.invoke(contextForResourceOwner);
      } else {
        return (String) runtimeApi_getRefreshToken.invoke(contextForResourceOwner);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getExpiresIn(Object contextForResourceOwner) {
    try {
      if (clientApi_ctxClass != null && clientApi_ctxClass.isInstance(contextForResourceOwner)) {
        return (String) clientApi_getExpiresIn.invoke(contextForResourceOwner);
      } else {
        return (String) runtimeApi_getExpiresIn.invoke(contextForResourceOwner);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getState(Object contextForResourceOwner) {
    try {
      if (clientApi_ctxClass != null && clientApi_ctxClass.isInstance(contextForResourceOwner)) {
        return (String) clientApi_getState.invoke(contextForResourceOwner);
      } else {
        return (String) runtimeApi_getState.invoke(contextForResourceOwner);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
