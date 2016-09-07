/**
 * Copyright (c) 2016 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trustedanalytics.modelcatalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustedanalytics.modelcatalog.domain.Model;
import org.trustedanalytics.modelcatalog.storage.ModelStore;
import org.trustedanalytics.modelcatalog.storage.OperationStatus;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ModelService {

  private enum UpdateMode {
    PATCH,
    OVERWRITE,
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelService.class);

  private ModelStore modelStore;

  @Autowired
  public ModelService(ModelStore modelStore) {
    this.modelStore = modelStore;
  }

  public Collection<Model> listModels(UUID orgId) {
    return modelStore.listModels(orgId);
  }

  public Model retrieveModel(UUID modelId) {
    Model model = modelStore.retrieveModel(modelId);
    throwExceptionIfNotFound(model);
    return model;
  }

  public Model addModel(Model model, UUID orgId) {
    model.setId(UUID.randomUUID());
    OperationStatus additionStatus = modelStore.addModel(model, orgId);
    throwExceptionIfUpdateWasNotSuccessful(additionStatus);
    return model;
  }

  public Model updateModel(UUID modelId, Model model) {
    return update(modelId, model, UpdateMode.OVERWRITE);
  }

  public Model patchModel(UUID modelId, Model model) {
    return update(modelId, model, UpdateMode.PATCH);
  }

  public Model deleteModel(UUID modelId) {
    Model model = modelStore.retrieveModel(modelId);
    throwExceptionIfNotFound(model);
    OperationStatus deleteStatus = modelStore.deleteModel(modelId);
    throwExceptionIfUpdateWasNotSuccessful(deleteStatus);
    return model;
  }

  private Model update(UUID modelId, Model model, UpdateMode updateMode) {
    throwExceptionIfIdsMismatch(modelId, model);
    retrieveModel(modelId);
    Map<String, Object> propertiesToUpdate = null;
    try {
      propertiesToUpdate = PropertiesReader.preparePropertiesToUpdateMap(
              model,
              updateMode == UpdateMode.OVERWRITE ? false : true);
    } catch (Exception e) {
      LOGGER.error("Exception while preparing properties map: " + e);
      throw new CannotMapPropertiesException();
    }
    throwExceptionIfNothingToUpdate(propertiesToUpdate);
    OperationStatus operationStatus = modelStore.updateModel(modelId, propertiesToUpdate);
    throwExceptionIfUpdateWasNotSuccessful(operationStatus);
    if (updateMode == UpdateMode.OVERWRITE) {
      model.setId(modelId);
      return model;
    } else {
      return retrieveModel(modelId);
    }
  }

  private void throwExceptionIfNotFound(Model model) {
    if (Objects.isNull(model)) {
      throw new ModelNotFoundException();
    }
  }

  private void throwExceptionIfUpdateWasNotSuccessful(OperationStatus operationStatus) {
    if (operationStatus == OperationStatus.FAILURE) {
      throw new FailedUpdateException();
    }
  }

  private void throwExceptionIfIdsMismatch(UUID modelId, Model model) {
    if (!Objects.isNull(model.getId()) && !Objects.equals(modelId, model.getId())) {
      throw new MismatchedIdsException();
    }
  }

  private void throwExceptionIfNothingToUpdate(Map<String, Object> propertiesToUpdate) {
    if (propertiesToUpdate.isEmpty()) {
      throw new NothingToUpdateException();
    }
  }

}
