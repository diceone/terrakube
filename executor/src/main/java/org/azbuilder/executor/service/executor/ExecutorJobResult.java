package org.azbuilder.executor.service.executor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutorJobResult {
    boolean successfulExecution;
    String planFile;
    String outputLog;
    String outputErrorLog;
}
