/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

import java.util.Objects;

public final class OAuthUtils {

  private OAuthUtils() {}

  public static <T> boolean literalEquals(Literal<T> l1, Literal<T> l2) {
    if (l1 == null && l2 == null) {
      return true;
    } else if (l1 == null ^ l2 == null) {
      return false;
    }

    return Objects.equals(l1.getLiteralValue().orElse(null), l2.getLiteralValue().orElse(null));
  }

  public static <T> int literalHashCode(Literal<T> literal) {
    return literal != null ? literal.getLiteralValue().map(Object::hashCode).orElse(0) : 0;
  }

  public static int literalHashCodes(Literal... literals) {
    if (literals == null) {
      return 0;
    }

    int result = 1;

    for (Literal literal : literals) {
      result = 31 * result + (literal == null ? 0 : literalHashCode(literal));
    }

    return result;

  }

  public static <T> boolean resolverEquals(ParameterResolver<T> p1, ParameterResolver<T> p2) {
    if (p1 == null && p2 == null) {
      return true;
    } else if (p1 == null ^ p2 == null) {
      return false;
    }

    return Objects.equals(p1.getExpression().orElse(null), p2.getExpression().orElse(null));
  }

  public static <T> int resolverHashCode(ParameterResolver<T> resolver) {
    return resolver != null ? resolver.getExpression().map(Object::hashCode).orElse(0) : 0;
  }
}
