/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.MediaType.ANY;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.Literal;

public class DeferredExpressionResolver {

  private final MuleExpressionLanguage evaluator;

  public DeferredExpressionResolver(MuleExpressionLanguage evaluator) {
    this.evaluator = evaluator;
  }

  public <T> T resolveExpression(Literal<T> literal, Result<Object, ? extends Object> result) {
    if (literal == null) {
      return null;
    }

    String expr = literal.getLiteralValue().orElse(null);
    if (expr == null) {
      return null;
    }

    if (!evaluator.isExpression(expr)) {
      if (TRUE.toString().equalsIgnoreCase(expr)) {
        return (T) TRUE;
      } else if (FALSE.toString().equalsIgnoreCase(expr)) {
        return (T) FALSE;
      }
      throw new IllegalArgumentException("Invalid value [" + expr + "] can't be converted to Boolean");
    }

    BindingContext resultContext = BindingContext.builder()
        .addBinding("payload",
                    new TypedValue(result.getOutput(), DataType.builder().fromObject(result.getOutput())
                        .mediaType(result.getMediaType().orElse(ANY)).build()))
        .addBinding("attributes",
                    new TypedValue(result.getAttributes().get(), DataType.fromObject(result.getAttributes().get())))
        .addBinding("dataType",
                    new TypedValue(DataType.builder().fromObject(result.getOutput()).mediaType(result.getMediaType().get())
                        .build(), DataType.fromType(DataType.class)))
        .build();

    return (T) evaluator.evaluate(expr, BOOLEAN, resultContext).getValue();
  }

  public <T> String getExpression(Literal<T> literal) {
    return literal != null ? literal.getLiteralValue().orElse(null) : null;
  }

}
