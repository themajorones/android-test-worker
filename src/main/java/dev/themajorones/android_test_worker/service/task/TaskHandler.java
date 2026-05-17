package dev.themajorones.android_test_worker.service.task;

import dev.themajorones.models.dto.TaskCommandEnvelope;

public interface TaskHandler {

    boolean supports(String type);

    void handle(TaskCommandEnvelope command);
}
