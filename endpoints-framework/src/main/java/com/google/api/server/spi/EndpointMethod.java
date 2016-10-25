/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.server.spi;

import com.google.api.server.spi.config.model.Types;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A class which takes care to resolved any parameterized types for endpoints. That way we
 * can support inheritance from generic base classes without needing to override and specialize
 * the method when it has a sensible default.
 */
public class EndpointMethod {
  /**
   * Adapter class to compare {@code EndpointMethod} instances based on their Java method signature
   * rather than the actual method implementation.  Comparisons are based on resolved types.
   *
   * <p>This is a pure adapter class containing no state of its own, relying entirely on the state
   * of the owning {@code EndpointMethod} object.  All access to the owning object is made through
   * qualified this calls to make dependencies as clear as possible.
   */
  public class ResolvedSignature {
    public String getSignatureName() {
      return EndpointMethod.this.getMethod().getName();
    }

    public Class<?>[] getSignatureParameterClasses() {
      return EndpointMethod.this.getParameterClasses();
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof ResolvedSignature)) {
        return false;
      }
      ResolvedSignature signature = (ResolvedSignature) o;
      return (getSignatureName().equals(signature.getSignatureName()) &&
          Arrays.equals(getSignatureParameterClasses(), signature.getSignatureParameterClasses()));
    }

    @Override
    public int hashCode() {
      return Objects.hash(getSignatureName(), Arrays.hashCode(getSignatureParameterClasses()));
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder(getSignatureName()).append("(");
      for (Class<?> clazz : getSignatureParameterClasses()) {
        builder.append(clazz.toString()).append(", ");
      }
      builder.delete(builder.length() - 2, builder.length());
      builder.append(")");
      return builder.toString();
    }
  }

  /**
   * The endpoint class.
   */
  private final Class<?> endpointClass;
  private final TypeToken<?> endpointToken;

  /**
   * The underlying method.
   */
  private final Method method;

  /**
   * A {@code TypeToken} for the method's declaring class.  Used to resolve types.
   */
  private final TypeToken<?> typeToken;

  private final ResolvedSignature resolvedMethodSignature;

  private List<String> parameterNames;

  private EndpointMethod(Class<?> endpointClass, Method method, TypeToken<?> declaringClass) {
    this.endpointClass = endpointClass;
    this.endpointToken = TypeToken.of(endpointClass);
    this.method = method;
    this.resolvedMethodSignature = new ResolvedSignature();
    this.typeToken = declaringClass;
  }

  private TypeToken<?> resolve(Type type) {
    return endpointToken.resolveType(type);
  }

  private TypeToken<?>[] resolve(Type[] types) {
    TypeToken<?>[] resolved = new TypeToken<?>[types.length];
    for (int i = 0; i < types.length; ++i) {
      resolved[i] = resolve(types[i]);
    }
    return resolved;
  }

  private static Class<?>[] resolveClasses(TypeToken<?>[] types) {
    Class<?>[] resolved = new Class<?>[types.length];
    for (int i = 0; i < types.length; ++i) {
      resolved[i] = types[i].getRawType();
    }
    return resolved;
  }

  /**
   * Returns the class associated with {@code type}. If this is a parameterized type or generic
   * array, then its raw or component type, respectively, will be returned. Null is returned
   * if {@code type} is not implemented by a {@code Class<?>}, {@code ParameterizedType}, or
   * {@code GenericArrayType} object.
   */
  public static Class<?> getClassFromType(Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType) {
      return getClassFromType(((ParameterizedType) type).getRawType());
    } else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      Class<?> componentClass = getClassFromType(componentType);
      if (componentClass != null) {
        return Array.newInstance(componentClass, 0).getClass();
      }
    }
    return null;
  }

  /**
   * Returns the endpoint class.
   */
  public Class<?> getEndpointClass() {
    return endpointClass;
  }

  /**
   * Returns the underlying method object.
   */
  public Method getMethod() {
    return method;
  }

  /**
   * Returns the return type of the method.
   */
  public TypeToken<?> getReturnType() {
    return resolve(method.getGenericReturnType());
  }

  /**
   * Returns the parameter types of the method.
   */
  public TypeToken<?>[] getParameterTypes() {
    return resolve(method.getGenericParameterTypes());
  }

  /**
   * Returns the parameter classes of the method.
   */
  public Class<?>[] getParameterClasses() {
    return resolveClasses(getParameterTypes());
  }

  private void validateNoWildcards(TypeToken<?>[] types) {
    for (TypeToken<?> type : types) {
      Type resolved = type.getType();
      if (resolved instanceof ParameterizedType) {
        Class<?> clazz = type.getRawType();
        TypeToken<?>[] typeArgs = new TypeToken<?>[clazz.getTypeParameters().length];
        for (int i = 0; i < typeArgs.length; i++) {
          typeArgs[i] = type.resolveType(clazz.getTypeParameters()[i]);
        }
        validateNoWildcards(typeArgs);
      } else if (Types.isWildcardType(type)) {
        throw new IllegalArgumentException(
            // TODO: Figure out a more useful error message.  Maybe try to provide the
            // location of the wildcard instead of just its name ('T' is not the most useful info).
            String.format("Wildcard type %s not supported", resolved));
      }
    }
  }

  /**
   * Creates an {@link EndpointMethod} using type information from the given {@link TypeToken}.
   *
   * @param method Must not have wildcard types (all generic types must be resolvable to a concrete
   *        type using the given {@link TypeToken}).
   * @param declaringClass A token for the method's declaring class.
   */
  public static EndpointMethod create(
      Class<?> endpointClass, Method method, TypeToken<?> declaringClass) {
    Preconditions.checkNotNull(endpointClass, "endpointClass");
    Preconditions.checkNotNull(method, "method");
    Preconditions.checkArgument(method.getDeclaringClass().isAssignableFrom(endpointClass),
          "Method '%s' does belong to interface of class '%s'", method, endpointClass);
    Preconditions.checkArgument(method.getDeclaringClass().equals(declaringClass.getRawType()),
          "Token must be of the method's declaring class '%s'.", method.getDeclaringClass());

    EndpointMethod endpointMethod = new EndpointMethod(endpointClass, method, declaringClass);

    endpointMethod.validateNoWildcards(new TypeToken<?>[] { endpointMethod.getReturnType() });
    endpointMethod.validateNoWildcards(endpointMethod.getParameterTypes());

    return endpointMethod;
  }

  /**
   * Creates an {@link EndpointMethod} using type information directly available from the method's
   * declaring class.
   *
   * @param method Must not have wildcard types (all generic types must be resolvable to a concrete
   *        type using type information from the method's declaring class).
   */
  public static EndpointMethod create(Class<?> endpointClass, Method method) {
    return create(endpointClass, method, TypeToken.of(method.getDeclaringClass()));
  }

  public ResolvedSignature getResolvedMethodSignature() {
    return resolvedMethodSignature;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof EndpointMethod)) {
      return false;
    }
    EndpointMethod m = (EndpointMethod) o;
    return this.getMethod().equals(m.getMethod());
  }

  @Override
  public String toString() {
    return new StringBuilder("Method: ").append(getMethod().toString())
        .append(", Resolved Return Type: ").append(getReturnType())
        .append(", Resolved Parameter Types: ").append(Arrays.toString(getParameterTypes()))
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMethod());
  }

  public List<String> getParameterNames() {
    List<String> names = parameterNames;
    return names == null ? null : new ArrayList<String>(names);
  }

  public void setParameterNames(List<String> parameterNames) {
    this.parameterNames = parameterNames;
  }
}
