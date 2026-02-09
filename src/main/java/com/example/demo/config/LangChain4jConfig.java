package com.example.demo.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.ollama.chat-model.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name:llama2}")
    private String chatModelName;

    @Value("${langchain4j.ollama.embedding-model.model-name:nomic-embed-text}")
    private String embeddingModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .temperature(0.7)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        // Using local embeddings (no external dependencies):
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean("langchain4jChatMemory")
    public dev.langchain4j.memory.ChatMemory langchain4jChatMemory() {
        return MessageWindowChatMemory.withMaxMessages(100);
    }
}
