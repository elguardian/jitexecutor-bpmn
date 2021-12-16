/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.jitexecutor.process;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.inject.Singleton;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.quarkus.logging.Log;

@Singleton
public class KafkaManager {

    private Producer<String, String> kafkaProducer;
    private Consumer<String, String> kafkaConsumer;

    public static class Reference {
        int counter;
        String topic;
        BiConsumer<String, String> consumer;

        public Reference(String topic, BiConsumer<String, String> consumer) {
            this.counter = 1;
            this.topic = topic;
            this.consumer = consumer;
        }

        public String topic() {
            return topic;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Reference) {
                return ((Reference) obj).topic.equals(topic);
            }
            return false;
        }
    }

    private Map<String, Reference> references;

    private ScheduledExecutorService executorService;

    public KafkaManager() {
        kafkaProducer = new KafkaProducer<>(createProducerProperties());
        kafkaConsumer = new KafkaConsumer<>(createConsumerProperties());
        references = new HashMap<>();
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleWithFixedDelay(this::pollEvent, 0, 1000L, TimeUnit.MILLISECONDS);
    }

    public synchronized void subscribeTopic(String topicName, BiConsumer<String, String> consumer) {
        if (references.containsKey(topicName)) {
            references.get(topicName).counter++;
            return;
        }
        Log.infov("subscribing to kafka topic {0}", topicName);
        references.put(topicName, new Reference(topicName, consumer));
        kafkaConsumer.subscribe(Collections.singleton(topicName));
    }

    public synchronized void clear() {
        Log.info("unsubscribe from all topics");
        kafkaConsumer.unsubscribe();
        references.clear();
    }

    public synchronized void unsubscribeTopic(String topicName) {
        Reference reference = references.get(topicName);
        if (reference != null) {
            reference.counter--;
            if (reference.counter == 0) {
                references.remove(topicName);
            }
        }
    }

    public void pushEvent(String topicName, Object data) {
        kafkaProducer.send(new ProducerRecord<String, String>(topicName, data.toString()));
    }

    public synchronized void pollEvent() {
        try {
            if (kafkaConsumer.subscription().isEmpty()) {
                return;
            }
            for (ConsumerRecord<String, String> record : kafkaConsumer.poll(Duration.of(1000L, ChronoUnit.MILLIS))) {
                Reference reference = references.get(record.topic());
                if (reference != null) {
                    reference.consumer.accept(record.topic(), record.value());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Properties createConsumerProperties() {
        Properties props = createCommonProperties();
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    private Properties createProducerProperties() {
        Properties props = createCommonProperties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return props;
    }

    private Properties createCommonProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9092");
        props.put("group.id", "group-id");
        return props;
    }

    public synchronized void dispose() {
        executorService.shutdownNow();
        kafkaProducer.close();
        kafkaConsumer.close();
    }
}
