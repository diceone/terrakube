package org.terrakube.api.plugin.storage.controller;

import lombok.AllArgsConstructor;
import org.terrakube.api.plugin.storage.StorageTypeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/tfoutput/v1")
public class TerraformOutputController {

    private StorageTypeService storageTypeService;

    @GetMapping(
            value = "/organization/{organizationId}/job/{jobId}/step/{stepId}",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public @ResponseBody byte[] getFile(@PathVariable("organizationId") String organizationId, @PathVariable("jobId") String jobId, @PathVariable("stepId") String stepId) {
        return storageTypeService.getStepOutput(organizationId, jobId, stepId);
    }
}
