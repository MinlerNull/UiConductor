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

package com.google.uicd.backend.core.utils;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/** UicdTypeConverter Used by GlobalVariableValidationAction, to support the advance expression. */
public class UicdTypeConverter {
  protected Logger logger = LogManager.getLogManager().getLogger("uicd");

  public int toInt(String str) {
    int ret = 0;
    try {
      ret = Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      logger.warning(String.format("Cannot convert %s to int", str));
    }
    return ret;
  }

  public double toDouble(String str) {
    double ret = 0;
    try {
      ret = Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      logger.warning(String.format("Cannot convert %s to double", str));
    }
    return ret;
  }
}
