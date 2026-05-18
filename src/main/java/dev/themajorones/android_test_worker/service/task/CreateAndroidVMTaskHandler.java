package dev.themajorones.android_test_worker.service.task;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.themajorones.android_test_worker.repository.AndroidVMRepository;
import dev.themajorones.android_test_worker.repository.DockerRepository;
import dev.themajorones.android_test_worker.repository.TaskLogRepository;
import dev.themajorones.models.client.DockerClient;
import dev.themajorones.models.constants.ConnectionStatusConstant;
import dev.themajorones.models.constants.TaskLogConstant;
import dev.themajorones.models.dto.TaskCommandEnvelope;
import dev.themajorones.models.entity.AndroidVMRecord;
import dev.themajorones.models.entity.Docker;
import dev.themajorones.models.entity.RetroidAndroidVM;
import dev.themajorones.models.entity.TaskLog;
import dev.themajorones.models.mapper.AndroidVmMapper;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class CreateAndroidVMTaskHandler implements TaskHandler {

    private static final Duration PORT_CHECK_TIMEOUT = Duration.ofSeconds(2);
    private static final long START_TIMEOUT_MILLIS = Duration.ofMinutes(3).toMillis();
    private static final long POLL_INTERVAL_MILLIS = Duration.ofSeconds(3).toMillis();

    private final TaskLogRepository taskLogRepository;
    private final AndroidVMRepository androidVMRepository;
    private final DockerRepository dockerRepository;
    private final DockerClient dockerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String type) {
        return TaskLogConstant.Type.CREATE_ANDROID_VM.equals(type);
    }

    @Override
    @Transactional
    public void handle(TaskCommandEnvelope command) {
        TaskLog taskLog = taskLogRepository.findById(command.getTaskLogId())
            .orElseThrow(() -> new IllegalArgumentException("Task log not found"));
        taskLog
            .setStatus(TaskLogConstant.Status.RUNNING)
            .setStartedAt(System.currentTimeMillis())
            .setEndedAt(null)
            .setResult(null);
        taskLogRepository.save(taskLog);

        RetroidAndroidVM vm = null;
        try {
            JsonNode content = objectMapper.readTree(taskLog.getContent());
            Integer androidVMId = content.path("androidVMId").intValue(0);
            AndroidVMRecord record = androidVMRepository.findById(androidVMId)
                .orElseThrow(() -> new IllegalArgumentException("Android VM not found"));
            vm = asRetroid(AndroidVmMapper.fromRecord(record));
            Docker docker = dockerRepository.findById(record.getDocker().getId())
                .orElseThrow(() -> new IllegalArgumentException("Docker connection not found"));

            vm.setStatus(ConnectionStatusConstant.CREATING);
            androidVMRepository.save(AndroidVmMapper.toRecord(vm));

            if (!dockerClient.imageExists(docker.getBaseUrl(), vm.getImage())) {
                dockerClient.pullImage(docker.getBaseUrl(), vm.getImage());
            }

            String containerId = dockerClient.createAndroidContainer(docker.getBaseUrl(), vm.getId(), vm);
            dockerClient.startContainer(docker.getBaseUrl(), containerId);

            Integer adbPort = waitForRunningContainer(docker, containerId);
            String adbHost = dockerClient.hostFromBaseUrl(docker.getBaseUrl());

            vm
                .setContainerId(containerId)
                .setContainerName("tmos-android-vm-" + vm.getId())
                .setAdbHost(adbHost)
                .setAdbPort(adbPort)
                .setStatus(ConnectionStatusConstant.READY);
            androidVMRepository.save(AndroidVmMapper.toRecord(vm));

            taskLog
                .setStatus(TaskLogConstant.Status.SUCCESS)
                .setResult(writeJson(Map.of(
                    "androidVMId", vm.getId(),
                    "containerId", containerId,
                    "adbHost", adbHost,
                    "adbPort", adbPort
                )))
                .setEndedAt(System.currentTimeMillis());
            taskLogRepository.save(taskLog);
        } catch (Exception ex) {
            if (vm != null) {
                vm.setStatus(ConnectionStatusConstant.FAILED);
                androidVMRepository.save(AndroidVmMapper.toRecord(vm));
            }
            taskLog
                .setStatus(TaskLogConstant.Status.FAILED)
                .setResult(writeJson(errorResult(ex)))
                .setEndedAt(System.currentTimeMillis());
            taskLogRepository.save(taskLog);
            throw new IllegalStateException("Android VM creation failed", ex);
        }
    }

    private Integer waitForRunningContainer(Docker docker, String containerId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MILLIS;
        Integer mappedPort = null;
        while (System.currentTimeMillis() < deadline) {
            if (dockerClient.isContainerRunning(docker.getBaseUrl(), containerId)) {
                mappedPort = dockerClient.mappedAdbPort(docker.getBaseUrl(), containerId);
                String host = dockerClient.hostFromBaseUrl(docker.getBaseUrl());
                if (mappedPort != null && dockerClient.isTcpPortReachable(host, mappedPort, PORT_CHECK_TIMEOUT)) {
                    return mappedPort;
                }
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new IllegalStateException("Timed out waiting for Android VM container port");
    }

    private Map<String, Object> errorResult(Exception ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("error", ex.getMessage());
        result.put("exception", ex.getClass().getName());
        return result;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize task result", ex);
        }
    }

    private RetroidAndroidVM asRetroid(dev.themajorones.models.entity.AndroidVM vm) {
        if (vm instanceof RetroidAndroidVM retroid) {
            return retroid;
        }
        throw new IllegalArgumentException("Unsupported Android VM type: " + vm.getVmType());
    }
}
