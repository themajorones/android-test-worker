package dev.themajorones.android_test_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.themajorones.models.queue.RabbitMqTopology;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public DirectExchange directExchange() {
        return RabbitMqTopology.directExchange();
    }

    @Bean
    public Queue connectionManagerQueue() {
        return RabbitMqTopology.connectionManagerQueue();
    }

    @Bean
    public Binding connectionManagerBinding(Queue connectionManagerQueue, DirectExchange directExchange) {
        return RabbitMqTopology.connectionManagerBinding(connectionManagerQueue, directExchange);
    }
}
