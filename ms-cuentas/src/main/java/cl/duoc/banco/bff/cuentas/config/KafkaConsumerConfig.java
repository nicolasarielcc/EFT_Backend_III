package cl.duoc.banco.bff.cuentas.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import cl.duoc.banco.bff.cuentas.dto.TransaccionEvent;

/**
 * Consumidor Kafka del microservicio de cuentas: recibe los eventos de
 * "transacciones completadas" emitidos por ms-pagos y actualiza el saldo.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    public static final String TOPIC = "transacciones";
    public static final String GROUP_ID = "cuentas-group";

    private final String bootstrapServers;

    public KafkaConsumerConfig(
            @Value("${app.kafka.bootstrap-servers:localhost:29092}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Bean
    ConsumerFactory<String, TransaccionEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new JsonDeserializer<>(TransaccionEvent.class, false));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TransaccionEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransaccionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
