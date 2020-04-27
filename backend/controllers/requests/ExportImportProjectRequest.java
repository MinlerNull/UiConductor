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

package com.google.uicd.backend.controllers.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;

/** Contains exports request details */
@AutoValue
public abstract class ExportImportProjectRequest {
  public abstract String getProjectId();

  public abstract String getProjectName();

  public abstract String getZipFileName();

  @JsonCreator
  public static ExportImportProjectRequest create(
      String projectId, String projectName, String zipFileName) {
    return new AutoValue_ExportImportProjectRequest(projectId, projectName, zipFileName);
  }
}
