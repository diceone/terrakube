package org.terrakube.api.plugin.storage.local;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.terrakube.api.plugin.storage.StorageTypeService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LocalStorageTypeServiceImpl implements StorageTypeService {

    private static final String OUTPUT_DIRECTORY = "/.terraform-spring-boot/local/output/%s/%s/%s.tfoutput";
    private static final String CONTEXT_DIRECTORY = "/.terraform-spring-boot/local/output/context/%s/context.json";
    private static final String STATE_DIRECTORY = "/.terraform-spring-boot/local/state/%s/%s/%s/%s/terraformLibrary.tfPlan";
    private static final String STATE_DIRECTORY_JSON = "/.terraform-spring-boot/local/state/%s/%s/state/%s.json";
    private static final String NO_DATA_FOUND = "NO DATA FOUND IN LOCAL STORAGE";
    private static final String NO_CONTEXT_FOUND = "{}";

    @Override
    public byte[] getStepOutput(String organizationId, String jobId, String stepId) {
        log.info("Searching: /.terraform-spring-boot/local/tfoutput/{}/{}/{}.tfoutput", organizationId, jobId, stepId);
        String outputFilePath = String.format(OUTPUT_DIRECTORY, organizationId, jobId, stepId);
        return getOutputBytes(outputFilePath);
    }

    @Override
    public byte[] getTerraformPlan(String organizationId, String workspaceId, String jobId, String stepId) {
        log.info("Searching: /.terraform-spring-boot/local/state/{}/{}/{}/{}/terraformLibrary.tfPlan", organizationId, workspaceId, jobId, stepId);
        String outputFilePath = String.format(STATE_DIRECTORY, organizationId, workspaceId, jobId, stepId);
        return getOutputBytes(outputFilePath);
    }

    @Override
    public byte[] getTerraformStateJson(String organizationId, String workspaceId, String stateFileName) {
        log.info("Searching: /.terraform-spring-boot/local/tfstate/{}/{}/state/{}.json", organizationId, workspaceId, stateFileName);
        String outputFilePath = String.format(STATE_DIRECTORY_JSON, organizationId, workspaceId, stateFileName);
        return getOutputBytes(outputFilePath);
    }

    @Override
    public String saveContext(int jobId, String jobContext) {
        try {
            String contextFilename = String.format(CONTEXT_DIRECTORY, jobId);
            log.info("contextFile: {}", contextFilename);
            File context = new File(FileUtils.getUserDirectoryPath().concat(FilenameUtils.separatorsToSystem(contextFilename)));
            FileUtils.forceMkdir(context.getParentFile());
            FileUtils.writeStringToFile(context, jobContext, Charset.defaultCharset());
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return jobContext;
    }

    @Override
    public String getContext(int jobId) {
        String searchContextFile = String.format(CONTEXT_DIRECTORY, jobId);
        log.info("contextFile: {}", searchContextFile);
        File context = new File(FileUtils.getUserDirectoryPath().concat(FilenameUtils.separatorsToSystem(searchContextFile)));
        String outputContext = NO_CONTEXT_FOUND;
        if (context.exists()) {
            try {
                outputContext = new String(IOUtils.toByteArray(new FileInputStream(context)));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }

        return outputContext;
    }

    private byte [] getOutputBytes(String path){
        File localOutputDirectory = new File(FileUtils.getUserDirectoryPath().concat(path));
        if (localOutputDirectory.exists()) {
            try {
                return IOUtils.toByteArray(new FileInputStream(localOutputDirectory));
            } catch (IOException e) {
                log.error(e.getMessage());
                return NO_DATA_FOUND.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            return NO_DATA_FOUND.getBytes(StandardCharsets.UTF_8);
        }
    }
}
