// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.uicd.xmldumper.utils;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Android Platform Reflection Utils */
public class AndroidPlatformReflectionUtils {

  private static String TAG = AndroidPlatformReflectionUtils.class.getSimpleName();

  /**
   * Clears the in-process Accessibility cache, removing any stale references. Because the
   * AccessibilityInteractionClient singleton stores copies of AccessibilityNodeInfo instances,
   * calls to public APIs such as `recycle` do not guarantee cached references get updated. See the
   * android.view.accessibility AIC and ANI source code for more information.
   */
  public static boolean clearAccessibilityCache() throws RuntimeException {
    boolean success = false;

    try {
      final Class c = Class.forName("android.view.accessibility.AccessibilityInteractionClient");
      final Method getInstance = AndroidPlatformReflectionUtils.method(c, "getInstance");
      final Object instance = getInstance.invoke(null);
      final Method clearCache =
          AndroidPlatformReflectionUtils.method(instance.getClass(), "clearCache");
      clearCache.invoke(instance);
      success = true;
    } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
      Log.e(TAG, "Failed to clear Accessibility Node cache. " + e.getMessage());
    }
    return success;
  }

  public static Class getClass(final String name) throws RuntimeException {
    try {
      return Class.forName(name);
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException(String.format("unable to find class %s", name), e);
    }
  }

  public static Object getField(final Class clazz, final String fieldName, final Object object)
      throws RuntimeException {
    try {
      final Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(object);
    } catch (final Exception e) {
      final String msg =
          String.format("error while getting field %s from object %s", fieldName, object);
      Log.e(TAG, msg + " " + e.getMessage());
      throw new RuntimeException(msg, e);
    }
  }

  public static Object getField(final String field, final Object object) throws RuntimeException {
    return getField(object.getClass(), field, object);
  }

  public static Object getField(final String className, final String field, final Object object)
      throws RuntimeException {
    return getField(getClass(className), field, object);
  }

  public static Object invoke(final Method method, final Object object, final Object... parameters)
      throws RuntimeException {
    try {
      return method.invoke(object, parameters);
    } catch (final Exception e) {
      final String msg =
          String.format(
              "error while invoking method %s on object %s with parameters %s",
              method, object, Arrays.toString(parameters));
      Log.e(TAG, msg + " " + e.getMessage());
      throw new RuntimeException(msg, e);
    }
  }

  public static Method method(
      final Class clazz, final String methodName, final Class... parameterTypes)
      throws RuntimeException {
    try {
      final Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return method;
    } catch (final Exception e) {
      final String msg =
          String.format(
              "error while getting method %s from class %s with parameter types %s",
              methodName, clazz, Arrays.toString(parameterTypes));
      Log.e(TAG, msg + " " + e.getMessage());
      throw new RuntimeException(msg, e);
    }
  }

  public static Method method(
      final String className, final String method, final Class... parameterTypes)
      throws RuntimeException {
    return method(getClass(className), method, parameterTypes);
  }
}
