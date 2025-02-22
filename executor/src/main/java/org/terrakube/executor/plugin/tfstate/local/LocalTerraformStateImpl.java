package org.terrakube.executor.plugin.tfstate.local;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.TextStringBuilder;
import org.terrakube.client.TerrakubeClient;
import org.terrakube.client.model.organization.workspace.history.History;
import org.terrakube.client.model.organization.workspace.history.HistoryAttributes;
import org.terrakube.client.model.organization.workspace.history.HistoryRequest;
import org.terrakube.executor.plugin.tfstate.TerraformState;
import org.terrakube.executor.plugin.tfstate.TerraformStatePathService;
import org.terrakube.executor.service.mode.TerraformJob;

@Slf4j
@Builder
@Getter
@Setter
public class LocalTerraformStateImpl implements TerraformState {

    private static final String TERRAFORM_PLAN_FILE = "terraformLibrary.tfPlan";
    private static final String LOCAL_BACKEND_DIRECTORY = "/.terraform-spring-boot/local/backend/%s/%s/" + TERRAFORM_PLAN_FILE;
    private static final String LOCAL_STATE_DIRECTORY = "/.terraform-spring-boot/local/state/%s/%s/%s/%s/" + TERRAFORM_PLAN_FILE;
    private static final String LOCAL_STATE_DIRECTORY_JSON = "/.terraform-spring-boot/local/state/%s/%s/state/%s.json";
    private static final String BACKEND_FILE_NAME = "localBackend.hcl";
    private static final String BACKEND_LOCAL_CONTENT = "\n\nterraform {\n" +
            "  backend \"local\" { }\n" +
            "}";

    @NonNull
    TerrakubeClient terrakubeClient;

    @NonNull
    TerraformStatePathService terraformStatePathService;

    @Override
    public String getBackendStateFile(String organizationId, String workspaceId, File workingDirectory) {
        String localBackend = BACKEND_FILE_NAME;
        try {
            String localBackendDirectory = FileUtils.getUserDirectoryPath().concat(
                    FilenameUtils.separatorsToSystem(
                            String.format(LOCAL_BACKEND_DIRECTORY, organizationId, workspaceId)
                    ));

            TextStringBuilder localBackendHcl = new TextStringBuilder();
            localBackendHcl.appendln("path                  = \"" + localBackendDirectory + "\"");

            File localBackendFile = new File(
                    FilenameUtils.separatorsToSystem(
                            String.join(File.separator, Stream.of(workingDirectory.getAbsolutePath(), BACKEND_FILE_NAME).toArray(String[]::new))
                    )
            );

            log.info("Creating Local Backend File: {}", localBackendFile.getAbsolutePath());
            FileUtils.writeStringToFile(localBackendFile, localBackendHcl.toString(), Charset.defaultCharset());

            File localBackendMainTf = new File(
                    FilenameUtils.separatorsToSystem(
                            workingDirectory.getAbsolutePath().concat("/main.tf")
                    )
            );

            FileUtils.writeStringToFile(localBackendMainTf, BACKEND_LOCAL_CONTENT, Charset.defaultCharset(), true);

        } catch (IOException e) {
            log.error(e.getMessage());
            localBackend = null;
        }
        return localBackend;
    }

    @Override
    public String saveTerraformPlan(String organizationId, String workspaceId, String jobId, String stepId, File workingDirectory) {

        String localStateFilePath = String.format(LOCAL_STATE_DIRECTORY, organizationId, workspaceId, jobId, stepId);

        String stepStateDirectory = FileUtils.getUserDirectoryPath().concat(
                FilenameUtils.separatorsToSystem(
                        localStateFilePath
                ));

        File tfPlan = new File(String.join(File.separator, Stream.of(workingDirectory.getAbsolutePath(), TERRAFORM_PLAN_FILE).toArray(String[]::new)));
        log.info("terraformStateFile Path: {} {}", workingDirectory.getAbsolutePath() + "/" + TERRAFORM_PLAN_FILE, tfPlan.exists());

        if (tfPlan.exists()) {
            try {
                FileUtils.copyFile(tfPlan, new File(stepStateDirectory));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            log.info("Local state file saved to {}", stepStateDirectory);
            return stepStateDirectory;
        } else {
            return null;
        }
    }

    @Override
    public boolean downloadTerraformPlan(String organizationId, String workspaceId, String jobId, String stepId, File workingDirectory) {
        AtomicBoolean planExists = new AtomicBoolean(false);
        Optional.ofNullable(terrakubeClient.getJobById(organizationId, jobId).getData().getAttributes().getTerraformPlan())
                .ifPresent(stateFilePath -> {
                    try {
                        log.info("Copying state from {}:", stateFilePath);
                        FileUtils.copyFile(
                                new File(stateFilePath),
                                new File(
                                        String.join(
                                                File.separator, Stream.of(workingDirectory.getAbsolutePath(), TERRAFORM_PLAN_FILE).toArray(String[]::new)
                                        )
                                )
                        );
                        planExists.set(true);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                });
        return planExists.get();
    }

    @Override
    public void saveStateJson(TerraformJob terraformJob, String applyJSON) {
        if (applyJSON != null) {
            String stateFilenameUUID = UUID.randomUUID().toString();
            String stateFileName = String.format(LOCAL_STATE_DIRECTORY_JSON, terraformJob.getOrganizationId(), terraformJob.getWorkspaceId(), stateFilenameUUID) ;
            log.info("terraformStateFile: {}", stateFileName);

            File localStateFile = new File(FileUtils.getUserDirectoryPath()
                    .concat(
                            FilenameUtils.separatorsToSystem(
                                stateFileName
                            )
                    )
            );

            try {
                FileUtils.writeStringToFile(localStateFile, applyJSON, Charset.defaultCharset());

                String stateURL = terraformStatePathService.getStateJsonPath(terraformJob.getOrganizationId(), terraformJob.getWorkspaceId(), stateFilenameUUID);

                HistoryRequest historyRequest = new HistoryRequest();
                History newHistory = new History();
                newHistory.setType("history");
                HistoryAttributes historyAttributes = new HistoryAttributes();
                historyAttributes.setOutput(stateURL);
                newHistory.setAttributes(historyAttributes);
                historyRequest.setData(newHistory);

                terrakubeClient.createHistory(historyRequest, terraformJob.getOrganizationId(), terraformJob.getWorkspaceId());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}
