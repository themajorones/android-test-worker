package dev.themajorones.android_test_worker.service.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import dev.themajorones.models.constants.RabbitMqConstant;
import dev.themajorones.models.dto.TaskCommandEnvelope;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TaskCommandListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCommandListener.class);

    private final List<TaskHandler> handlers;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitMqConstant.Queue.ConnectionManager.NAME)
    public void listen(String message) {
        try {
            TaskCommandEnvelope command = objectMapper.readValue(message, TaskCommandEnvelope.class);
            TaskHandler handler = handlers.stream()
                .filter(candidate -> candidate.supports(command.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No handler for task type " + command.getType()));
            handler.handle(command);
        } catch (Exception ex) {
            LOGGER.error("Failed to process task command: {}", message, ex);
            throw new IllegalStateException("Failed to process task command", ex);
        }
    }
}
