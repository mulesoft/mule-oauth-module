/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.api.exception;

import static java.util.Optional.ofNullable;

import org.mule.extension.http.api.error.HttpError;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

/**
 * Errors that can happen during an OAuth dance.
 *
 * @since 1.0
 */
public enum OAuthClientErrors implements ErrorTypeDefinition<OAuthClientErrors> {

  OAUTH_CLIENT_SECURITY(HttpError.CLIENT_SECURITY), TOKEN_NOT_FOUND(OAUTH_CLIENT_SECURITY), TOKEN_URL_FAIL(OAUTH_CLIENT_SECURITY);

  private ErrorTypeDefinition<?> parentErrortype;

  private OAuthClientErrors(ErrorTypeDefinition parentErrorType) {
    this.parentErrortype = parentErrorType;
  }

  private OAuthClientErrors() {}

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parentErrortype);
  }
}
