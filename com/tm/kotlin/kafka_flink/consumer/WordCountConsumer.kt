package com.tm.kotlin.kafka_flink.consumer

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.api.java.tuple.Tuple2
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

fun main() {
    val env = StreamExecutionEnvironment.getExecutionEnvironment()

    // Configure Kafka source
    val kafkaSource = KafkaSource.builder<String>()
        .setBootstrapServers("localhost:9092")
        .setTopics("word-count-topic")
        .setGroupId("word-count-consumer-group")
        .setStartingOffsets(OffsetsInitializer.latest())
        .setValueOnlyDeserializer(SimpleStringSchema())
        .build()

    println("🚀 Word Count Consumer started")
    println("   Counting words in 10-second windows...")
    println("")

    // Create streaming pipeline
    env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
        // Split sentences into words
        .flatMap(object : FlatMapFunction<String, Tuple2<String, Int>> {
            override fun flatMap(sentence: String, out: Collector<Tuple2<String, Int>>) {
                // Clean and split sentence
                sentence.lowercase()
                    .replace(Regex("[^a-z\\s]"), "")
                    .split("\\s+".toRegex())
                    .filter { it.isNotBlank() }
                    .forEach { word ->
                        out.collect(Tuple2(word, 1))
                    }
            }
        })
        // Group by word (with explicit KeySelector)
        .keyBy(object : KeySelector<Tuple2<String, Int>, String> {
            override fun getKey(value: Tuple2<String, Int>): String {
                return value.f0
            }
        })
        // 10-second tumbling window
        .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
        // Sum counts
        .sum(1)
        // Print results
        .map(object : MapFunction<Tuple2<String, Int>, String> {
            override fun map(wordCount: Tuple2<String, Int>): String {
                val result = "📊 Word: '${wordCount.f0}' | Count: ${wordCount.f1}"
                println(result)
                return result
            }
        })

    // Execute
    env.execute("Word Count Stream Processing")
}
