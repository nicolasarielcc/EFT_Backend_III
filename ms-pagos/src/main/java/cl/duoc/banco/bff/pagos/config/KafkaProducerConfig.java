package cl.duoc.banco.bff.pagos.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import cl.duoc.banco.bff.pagos.dto.TransaccionEvent;

/**
 * Productor Kafka: publica eventos de "transacciones completadas".
 */
@EnableKafka
@Configuration
public class KafkaProducerConfig {

    public static final String TOPIC = "transacciones";

    private final String bootstrapServers;

    public KafkaProducerConfig(
            @Value("${app.kafka.bootstrap-servers:localhost:29092}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Bean
    KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    NewTopic topicTransacciones() {
        return new NewTopic(TOPIC, 3, (short) 1);
    }

    @Bean
    ProducerFactory<String, TransaccionEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    KafkaTemplate<String, TransaccionEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
