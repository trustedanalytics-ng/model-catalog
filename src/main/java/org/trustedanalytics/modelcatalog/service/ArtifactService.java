/**
 * Copyright (c) 2016 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.trustedanalytics.modelcatalog.service;

import org.trustedanalytics.modelcatalog.domain.Artifact;
import org.trustedanalytics.modelcatalog.domain.ArtifactAction;
import org.trustedanalytics.modelcatalog.domain.Model;
import org.trustedanalytics.modelcatalog.storage.FileStore;
import org.trustedanalytics.modelcatalog.storage.FileStoreException;
import org.trustedanalytics.modelcatalog.storage.ModelStore;
import org.trustedanalytics.modelcatalog.storage.ModelStoreException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ArtifactService {

  private final ModelStore modelStore;
  private final FileStore fileStore;

  @Autowired
  public ArtifactService(ModelStore modelStore, FileStore fileStore) {
    this.modelStore = modelStore;
    this.fileStore = fileStore;
  }

  public Artifact addArtifact(UUID modelId, Set<ArtifactAction> actions, MultipartFile file) {
    try {
      // Try first if the model exists
      Model model = modelStore.retrieveModel(modelId);
      if (Objects.isNull(model)) {
        throw new ModelServiceException(
            ModelServiceExceptionCode.MODEL_NOT_FOUND, "Model with given ID not found.");
      }
    } catch (ModelStoreException e) {
      throw new ModelServiceException(
          ModelServiceExceptionCode.MODEL_NOT_FOUND, "Unable to find model with given ID.", e);
    }

    try {
      Artifact artifact = createArtifact(modelId, actions, file);
      fileStore.addFile(artifact.getLocation(), file.getInputStream());
      modelStore.addArtifact(modelId, artifact);
      return artifact;
    } catch (IOException e) {
      throw new ModelServiceException(
              ModelServiceExceptionCode.ARTIFACT_ADD_FAILED, "Artifact add failed: unable to read" +
              " file input stream.", e);
    } catch (FileStoreException e) {
      throw new ModelServiceException(
              ModelServiceExceptionCode.ARTIFACT_ADD_FAILED, "Artifact file add failed.", e);
    } catch (ModelStoreException e) {
      throw new ModelServiceException(
              ModelServiceExceptionCode.ARTIFACT_ADD_FAILED, "Artifact add failed.", e);
    }
  }

  public Artifact retrieveArtifact(UUID modelId, UUID artifactId) {
    Set<Artifact> artifacts = new HashSet<>();
    try {
      Model model = modelStore.retrieveModel(modelId);
      if (Objects.isNull(model)) {
        throw new ModelServiceException(
            ModelServiceExceptionCode.MODEL_RETRIEVE_FAILED,
            "Model retrieve failed (model store returned null).");
      }
      artifacts = model.getArtifacts();
    } catch (ModelStoreException e) {
      throw new ModelServiceException(
          ModelServiceExceptionCode.MODEL_RETRIEVE_FAILED, "Model retrieve failed.", e);
    }

    if (Objects.isNull(artifacts)) {
      throwArtifactNotFoundException();
    }
    Optional<Artifact> artifact = artifacts.stream()
            .filter(a -> a.getId().equals(artifactId))
            .findFirst();
    if (artifact.isPresent()) {
      return artifact.get();
    } else {
      throwArtifactNotFoundException();
      return null;
    }
  }

  public InputStream retrieveArtifactFile(UUID modelId, UUID artifactId) {
    try {
      Artifact artifact = retrieveArtifact(modelId, artifactId);
      return fileStore.retrieveFile(artifact.getLocation());
    } catch (FileStoreException e) {
      throw new ModelServiceException(
              ModelServiceExceptionCode.ARTIFACT_FILE_RETRIEVE_FAILED, "Artifact file retrieve " +
              "failed.", e);
    }
  }

  public Artifact deleteArtifact(UUID modelId, UUID artifactId) {
    try {
      Artifact artifact = retrieveArtifact(modelId, artifactId);
      fileStore.deleteFile(artifact.getLocation());
      modelStore.deleteArtifact(modelId, artifactId);
      return artifact;
    } catch (FileStoreException e) {
      throw new ModelServiceException(
              ModelServiceExceptionCode.ARTIFACT_DELETE_FAILED, "Artifact file delete failed.", e);
    } catch (ModelStoreException e) {
      throw new ModelServiceException(
              ModelServiceExceptionCode.ARTIFACT_DELETE_FAILED, "Artifact delete failed.", e);
    }
  }

  private Artifact createArtifact(UUID modelId, Set<ArtifactAction> actions, MultipartFile file) {
    UUID artifactId = UUID.randomUUID();
    return Artifact.builder()
            .id(artifactId)
            .filename(file.getOriginalFilename())
            .location(getArtifactLocation(modelId, artifactId))
            .actions(actions)
            .build();
  }

  private void throwArtifactNotFoundException() {
    throw new ModelServiceException(
            ModelServiceExceptionCode.ARTIFACT_NOT_FOUND, "Artifact with given ID not found.");
  }

  private String getArtifactLocation(UUID modelId, UUID artifactId) {
    return String.format("/%s/%s", modelId, artifactId);
  }
}
