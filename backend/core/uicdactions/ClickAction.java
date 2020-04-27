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

package com.google.uicd.backend.core.uicdactions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.uicd.backend.core.constants.StrategyType;
import com.google.uicd.backend.core.devicesdriver.AndroidDeviceDriver;
import com.google.uicd.backend.core.exceptions.UicdDeviceHttpConnectionResetException;
import com.google.uicd.backend.core.uicdactions.ActionContext.PlayStatus;
import com.google.uicd.backend.core.xmlparser.NodeContext;
import com.google.uicd.backend.core.xmlparser.Position;

/** ClickAction */
public class ClickAction extends BaseAction {

  // need by the jackson to deserialize
  public ClickAction() {
    this.positionHelper = new PositionHelper();
  }
  // raw XY
  public ClickAction(NodeContext nodeContext, boolean isDoubleClick) {
    this();
    this.nodeContext = nodeContext;
    if (nodeContext != null) {
      this.setName(nodeContext.getDisplayEstimate());
    }
    this.isDoubleClick = isDoubleClick;
    this.failTestIfNotFound = true;
  }

  public NodeContext nodeContext;
  private boolean isRawXY = false;
  private boolean isDoubleClick = false;

  // For backward compatibility, origin logic was if our engine can not found the element it will
  // just skip current step, but we want to fail the test faster so that it won't confuse users.
  private boolean failTestIfNotFound = false;

  private boolean isByElement;
  private boolean isOcrMode;
  private StrategyType strategy;
  private String selector;

  @JsonIgnore PositionHelper positionHelper;

  @Override
  public void updateAction(BaseAction baseAction) {
    super.updateCommonFields(baseAction);
    if (baseAction instanceof ClickAction) {
      ClickAction otherAction = (ClickAction) baseAction;
      this.isRawXY = otherAction.isRawXY;
      this.strategy = otherAction.strategy;
      this.selector = otherAction.selector;
      this.failTestIfNotFound = true;
      this.isByElement = otherAction.isByElement;
      this.isOcrMode = otherAction.isOcrMode;
    }
  }

  @Override
  public String getDisplay() {
    if (isDoubleClick) {
      return "Double Click";
    }
    if (isOcrMode) {
      return String.format("%s(OCR mode) - %s", strategy, selector);
    }
    if (isByElement) {

    }
    String clickedPos = nodeContext == null ? "" : nodeContext.getClickedPos().toString();
    if (isRawXY) {
      return clickedPos;
    } else if (clickedPos.isEmpty()) {
      // For such case, if nodeContext is absent we need to name the action as well
      return String.format("Click Action, %s", selector);
    } else {
      return String.format("%s, %s", this.getName(), clickedPos);
    }
  }

  @Override
  protected int play(AndroidDeviceDriver androidDeviceDriver, ActionContext actionContext)
      throws UicdDeviceHttpConnectionResetException {
    Position pos;
    if (isRawXY) {
      pos = nodeContext.getClickedPos();
      pos.setPhysicalPos(true);
      androidDeviceDriver.clickDevice(pos, isDoubleClick);
      return 0;
    }
    String clickedText =
        actionContext.expandUicdGlobalVariable(selector, androidDeviceDriver.getDeviceId());
    //  whether use ocr or not, but later the system should automatically choose.
    if (isOcrMode) {
      pos =
          positionHelper.getPositionFromScreenByORC(
              clickedText, androidDeviceDriver, actionContext);
      logger.info(String.format("Position from orc engine: (x:%f, y:%f)", pos.x, pos.y));
    } else if (isByElement) {
      pos =
          androidDeviceDriver.getPosByElment(
              androidDeviceDriver.fetchCurrentXML(/* withClassName */ true),
              strategy,
              clickedText,
              androidDeviceDriver.getWidthRatio(),
              androidDeviceDriver.getHeightRatio());
      if (!pos.isValidPos()) {
        pos =
            positionHelper.getPositionFromScreenByORC(
                clickedText, androidDeviceDriver, actionContext);
        logger.info(String.format("Fallback to orc engine: (x:%f, y:%f)", pos.x, pos.y));
      }
    } else {
      pos = positionHelper.getPositionFromScreen(androidDeviceDriver, nodeContext, actionContext);
      if (failTestIfNotFound && !pos.isValidPos()) {
        actionContext.setFailStatus(androidDeviceDriver.getDeviceId());
        this.playStatus = ActionContext.PlayStatus.FAIL;
        return -1;
      }
    }
    androidDeviceDriver.clickDevice(pos, isDoubleClick);
    return 0;
  }

  @Override
  protected ActionExecutionResult genActionExecutionResults(
      AndroidDeviceDriver androidDeviceDriver, ActionContext actionContext) {

    if (this.playStatus != PlayStatus.FAIL) {
      return super.genActionExecutionResults(androidDeviceDriver, actionContext);
    }
    ActionExecutionResult actionExecutionResult = new ActionExecutionResult();
    String targetText = "";
    if (this.nodeContext != null) {
      targetText = this.nodeContext.getDisplayEstimate();
    }
    String logContent = String.format("Can not find element in xml, looking for: '%s'", targetText);
    actionExecutionResult.setRegularOutput(logContent);
    actionExecutionResult.setActionId(this.getActionId().toString());
    actionExecutionResult.setPlayStatus(this.playStatus);
    return actionExecutionResult;
  }
}
