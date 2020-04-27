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

package com.google.uicd.xmldumper.core;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.app.Service;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.uiautomator.UiDevice;
import com.google.uicd.xmldumper.utils.UicdDevice;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlSerializer;

/**
 * The AccessibilityNodeInfoDumper in Android Open Source Project contains a lot of bugs which will
 * stay in old android versions forever. By copying the code of the latest version it is ensured
 * that all patches become available on old android versions.
 *
 * <p>Original from:
 * https://android.googlesource.com/platform/frameworks/uiautomator/+/android-support-test/src/main/java/android/support/test/uiautomator/AccessibilityNodeInfoDumper.java
 *
 * <p>down ported bugs are e.g. { @link https://code.google.com/p/android/issues/detail?id=62906 }
 * { @link https://code.google.com/p/android/issues/detail?id=58733 }
 */
public class AccessibilityNodeInfoDumper {
  private static final String[] NAF_EXCLUDED_CLASSES =
      new String[] {
        android.widget.GridView.class.getName(),
        android.widget.GridLayout.class.getName(),
        android.widget.ListView.class.getName(),
        android.widget.TableLayout.class.getName()
      };
  // XMLdd 1.0 Legal Characters (http://stackoverflow.com/a/4237934/347155)
  // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
  private static Pattern XML10Pattern =
      Pattern.compile(
          "[^"
              + "\u0009\r\n"
              + "\u0020-\uD7FF"
              + "\uE000-\uFFFD"
              + "\ud800\udc00-\udbff\udfff"
              + "]");

  private static String TAG = AccessibilityNodeInfoDumper.class.getSimpleName();

  public static List<String> dumpWindowHierarchy(boolean withClassName) {
    List<String> xmls = new ArrayList<>();
    for (AccessibilityNodeInfo root : UicdDevice.getWindowRoots()) {
      // call requires API 24
      // int order = root.getDrawingOrder();
      xmls.add(getWindowXMLHierarchy(root, withClassName));
    }
    return xmls;
  }

  /**
   * Using {@link AccessibilityNodeInfo} this method will walk the layout hierarchy and return
   * String object of xml hierarchy
   *
   * @param root The root accessibility node.
   * @param withClassName whether the output tag name is classname,
   *                      false, the dump will be exactly same as UiAutomator
   *                      true, each dump node tag name is classname
   */
  public static String getWindowXMLHierarchy(AccessibilityNodeInfo root, boolean withClassName)
      throws RuntimeException {
    StringWriter xmlDump = new StringWriter();
    try {
      XmlSerializer serializer = Xml.newSerializer();
      serializer.setOutput(xmlDump);
      serializer.startDocument("UTF-8", true);
      serializer.startTag("", "hierarchy");

      if (root != null) {
        Point physicalSize = getDevicePhysicalSize();
        dumpNodeRec(root, serializer, 0, physicalSize.x, physicalSize.y, withClassName);
      }

      serializer.endTag("", "hierarchy");
      serializer.endDocument();
    } catch (IOException e) {
      Log.e(TAG, "failed to dump window to file " + e.getMessage());
    }
    return xmlDump.toString();
  }

  public static Point getDevicePhysicalSize() {
    int width;
    int height;
    try {
      UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
      height = mDevice.getDisplayHeight();
      width = mDevice.getDisplayWidth();
      return new Point(width, height);
    } catch (NullPointerException e) {
      Log.e(TAG, "Failed get display size, using getRealMetrics instead. " + e.getMessage());
    }
    WindowManager windowManager = (WindowManager) getInstrumentation().getContext()
        .getSystemService(Service.WINDOW_SERVICE);
    Display display =  windowManager.getDefaultDisplay();
    DisplayMetrics metrics = new DisplayMetrics();
    display.getRealMetrics(metrics);
    height = metrics.heightPixels;
    width = metrics.widthPixels;
    return new Point(width, height);

  }

  private static void dumpNodeRec(
      AccessibilityNodeInfo node,
      XmlSerializer serializer,
      int index,
      int width,
      int height,
      boolean withClassName)
      throws IOException {
    serializer.startTag("", withClassName ? safeTagString(node.getClassName()) : "node");

    if (!nafExcludedClass(node) && !nafCheck(node)) {
      serializer.attribute("", "NAF", Boolean.toString(true));
    }
    serializer.attribute("", "index", Integer.toString(index));
    final String text;
    if (node.getRangeInfo() == null) {
      text = safeCharSeqToString(node.getText());
    } else {
      text = Float.toString(node.getRangeInfo().getCurrent());
    }
    serializer.attribute("", "text", text);
    serializer.attribute("", "class", safeCharSeqToString(node.getClassName()));
    serializer.attribute("", "package", safeCharSeqToString(node.getPackageName()));
    serializer.attribute("", "content-desc", safeCharSeqToString(node.getContentDescription()));
    serializer.attribute("", "checkable", Boolean.toString(node.isCheckable()));
    serializer.attribute("", "checked", Boolean.toString(node.isChecked()));
    serializer.attribute("", "clickable", Boolean.toString(node.isClickable()));
    serializer.attribute("", "enabled", Boolean.toString(node.isEnabled()));
    serializer.attribute("", "focusable", Boolean.toString(node.isFocusable()));
    serializer.attribute("", "focused", Boolean.toString(node.isFocused()));
    serializer.attribute("", "scrollable", Boolean.toString(node.isScrollable()));
    serializer.attribute("", "long-clickable", Boolean.toString(node.isLongClickable()));
    serializer.attribute("", "password", Boolean.toString(node.isPassword()));
    serializer.attribute("", "selected", Boolean.toString(node.isSelected()));

    /** True if the device is >= API 18 */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      serializer.attribute(
          "", "bounds", getVisibleBoundsInScreen(node, width, height).toShortString());
      serializer.attribute("", "resource-id", safeCharSeqToString(node.getViewIdResourceName()));
    }

    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) {
        if (child.isVisibleToUser()) {
          dumpNodeRec(child, serializer, i, width, height, withClassName);
          child.recycle();
        } else {
          Log.i(TAG, String.format("Skipping invisible child: %s", child.toString()));
        }
      } else {
        Log.i(TAG, String.format("Null child %d/%d, parent: %s", i, count, node.toString()));
      }
    }
    serializer.endTag("", withClassName ? safeTagString(node.getClassName()) : "node");
  }

  /**
   * The list of classes to exclude my not be complete. We're attempting to only reduce noise from
   * standard layout classes that may be falsely configured to accept clicks and are also enabled.
   *
   * @return true if node is excluded.
   */
  private static boolean nafExcludedClass(AccessibilityNodeInfo node) {
    String className = safeCharSeqToString(node.getClassName());
    for (String excludedClassName : NAF_EXCLUDED_CLASSES) {
      if (className.endsWith(excludedClassName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * We're looking for UI controls that are enabled, clickable but have no text nor
   * content-description. Such controls configuration indicate an interactive control is present in
   * the UI and is most likely not accessibility friendly. We refer to such controls here as NAF
   * controls (Not Accessibility Friendly)
   *
   * @return false if a node fails the check, true if all is OK
   */
  private static boolean nafCheck(AccessibilityNodeInfo node) {
    boolean isNaf =
        node.isClickable()
            && node.isEnabled()
            && safeCharSeqToString(node.getContentDescription()).isEmpty()
            && safeCharSeqToString(node.getText()).isEmpty();
    if (!isNaf) {
      return true;
    }
    // check children since sometimes the containing element is clickable
    // and NAF but a child's text or description is available. Will assume
    // such layout as fine.
    return childNafCheck(node);
  }

  /**
   * This should be used when it's already determined that the node is NAF and a further check of
   * its children is in order. A node maybe a container such as LinerLayout and may be set to be
   * clickable but have no text or content description but it is counting on one of its children to
   * fulfill the requirement for being accessibility friendly by having one or more of its children
   * fill the text or content-description. Such a combination is considered by this dumper as
   * acceptable for accessibility.
   *
   * @return false if node fails the check.
   */
  private static boolean childNafCheck(AccessibilityNodeInfo node) {
    int childCount = node.getChildCount();
    for (int i = 0; i < childCount; i++) {
      AccessibilityNodeInfo childNode = node.getChild(i);
      if (childNode == null) {
        Log.i(TAG, String.format("Null child %d/%d, parent: %s", i, childCount, node.toString()));
        continue;
      }
      if (!safeCharSeqToString(childNode.getContentDescription()).isEmpty()
          || !safeCharSeqToString(childNode.getText()).isEmpty()) {
        return true;
      }
      if (childNafCheck(childNode)) {
        return true;
      }
    }
    return false;
  }

  // DocumentBuilder failed to parse XML tags with special characters
  //    e.g. android.support.v7.app.ActionBar$Tab
  private static String safeTagString(CharSequence cs) {
    return safeCharSeqToString(cs).replaceAll("[!@#$%^&*(),.?\":{}|<>]", "");
  }

  private static String safeCharSeqToString(CharSequence cs) {
    return cs == null ? "" : stripInvalidXMLChars(cs);
  }

  // Original UIAutomator code here broke UTF characters
  private static String stripInvalidXMLChars(CharSequence charSequence) {
    final StringBuilder sb = new StringBuilder(charSequence.length());
    sb.append(charSequence);
    return XML10Pattern.matcher(sb.toString()).replaceAll("?");
  }

  /**
   * Returns the node's bounds clipped to the size of the display
   *
   * @param width pixel width of the display
   * @param height pixel height of the display
   * @return (-1, -1, -1, -1) if node is null, else a Rect containing visible bounds
   */
  private static Rect getVisibleBoundsInScreen(AccessibilityNodeInfo node, int width, int height) {
    Rect nodeRect = new Rect(-1, -1, -1, -1);
    if (node == null) {
      return nodeRect;
    }
    // targeted node's bounds
    node.getBoundsInScreen(nodeRect);
    Rect displayRect = new Rect();
    displayRect.top = 0;
    displayRect.left = 0;
    displayRect.right = width;
    displayRect.bottom = height;
    nodeRect.intersect(displayRect);
    return nodeRect;
  }
}
