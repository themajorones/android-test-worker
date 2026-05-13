package dev.themajorones.android_test_worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import dev.themajorones.models.constants.RabbitMqConstant;

@Service
public class QueueMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueMessageListener.class);

    @RabbitListener(queues = RabbitMqConstant.Queue.Test.NAME)
    public void listen(String message) {
        LOGGER.info("Received RabbitMQ message: {}", message);
    }
}
