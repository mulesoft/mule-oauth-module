/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.createLockForResourceOwner;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Map;
import java.util.concurrent.locks.Lock;

class DefaultOAuthContextServiceImpl implements OAuthContextService {

  @Override
  public Object createResourceOwnerOAuthContext(String resourceOwnerId, String name, LockFactory lockFactory) {
    return new org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext(createLockForResourceOwner(resourceOwnerId, name,
                                                                                                            lockFactory),
                                                                                 resourceOwnerId);
  }

  @Override
  public Object migrateContextIfNeeded(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext context =
        (org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext) resourceOwnerOAuthContext;
    context.setRefreshUserOAuthContextLock(createLockForResourceOwner(getResourceOwnerId(resourceOwnerOAuthContext), name,
                                                                      lockFactory));
    return context;
  }

  @Override
  public Lock getRefreshUserOAuthContextLock(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    return ((org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext) resourceOwnerOAuthContext)
        .getRefreshUserOAuthContextLock();
  }

  @Override
  public Lock createRefreshUserOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId) {
    return lockProvider.createLock(lockNamePrefix + "-config-oauth-context");
  }

  @Override
  public <T> OAuthDancerBuilder<T> dancerName(OAuthDancerBuilder<T> dancerBuilder, String name) {
    return dancerBuilder; // No-op in V1
  }

  @Override
  public String getResourceOwnerId(Object resourceOwnerOAuthContext) {
    return ((ResourceOwnerOAuthContext) resourceOwnerOAuthContext).getResourceOwnerId();
  }

  @Override
  public Map<String, Object> getTokenResponseParameters(Object contextForResourceOwner) {
    return ((ResourceOwnerOAuthContext) contextForResourceOwner).getTokenResponseParameters();
  }

  @Override
  public String getAccessToken(Object contextForResourceOwner) {
    return ((ResourceOwnerOAuthContext) contextForResourceOwner).getAccessToken();
  }

  @Override
  public String getRefreshToken(Object contextForResourceOwner) {
    return ((ResourceOwnerOAuthContext) contextForResourceOwner).getRefreshToken();
  }

  @Override
  public String getExpiresIn(Object contextForResourceOwner) {
    return ((ResourceOwnerOAuthContext) contextForResourceOwner).getExpiresIn();
  }

  @Override
  public String getState(Object contextForResourceOwner) {
    return ((ResourceOwnerOAuthContext) contextForResourceOwner).getState();
  }
}
