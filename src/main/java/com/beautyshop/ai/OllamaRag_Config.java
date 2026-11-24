package com.beautyshop.ai;

import java.time.Duration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
class OllamaRag_Config {
    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
    RagProvider ollamaRag(@Value("${app.ai.ollama.base-url}") String baseUrl,
                          @Value("${app.ai.ollama.chat-model}") String chatModel,
                          @Value("${app.ai.ollama.embed-model}") String embedModel) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        var api = OllamaApi.builder().baseUrl(baseUrl)
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory)).build();
        var management = ModelManagementOptions.builder().pullModelStrategy(PullModelStrategy.NEVER).build();
        var embed = OllamaEmbeddingModel.builder().ollamaApi(api).modelManagementOptions(management)
            .defaultOptions(OllamaEmbeddingOptions.builder().model(embedModel).build()).build();
        var chat = OllamaChatModel.builder().ollamaApi(api).modelManagementOptions(management)
            .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
            .defaultOptions(OllamaChatOptions.builder().model(chatModel).temperature(0.0).numPredict(48)
                .internalToolExecutionEnabled(false).build()).build();
        return new RagProvider(embed, chat);
    }
}
