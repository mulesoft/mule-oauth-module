/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;

import java.util.Map;
import java.util.concurrent.locks.Lock;

public interface OAuthContextService {

  Object createResourceOwnerOAuthContext(String resourceOwnerId, String name, LockFactory lockFactory);

  Object migrateContextIfNeeded(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory);

  Lock getRefreshUserOAuthContextLock(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory);

  Lock createRefreshUserOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId);

  <T> OAuthDancerBuilder<T> dancerName(OAuthDancerBuilder<T> dancerBuilder, String name);

  String getResourceOwnerId(Object resourceOwnerOAuthContext);

  Map<String, Object> getTokenResponseParameters(Object contextForResourceOwner);

  String getAccessToken(Object contextForResourceOwner);

  String getRefreshToken(Object contextForResourceOwner);

  String getExpiresIn(Object contextForResourceOwner);

  String getState(Object contextForResourceOwner);
}

