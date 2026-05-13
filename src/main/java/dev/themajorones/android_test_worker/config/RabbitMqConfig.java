package dev.themajorones.android_test_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.themajorones.models.constants.RabbitMqConstant;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(RabbitMqConstant.DIRECT_EXCHANGE);
    }

    @Bean
    public Queue workerQueue() {
        return QueueBuilder.durable(RabbitMqConstant.Queue.Message.NAME).build();
    }

    @Bean
    public Binding workerQueueBinding(Queue workerQueue, DirectExchange directExchange) {
        return BindingBuilder
            .bind(workerQueue)
            .to(directExchange)
            .with(RabbitMqConstant.RoutingKey.Message.NAME);
    }
}
