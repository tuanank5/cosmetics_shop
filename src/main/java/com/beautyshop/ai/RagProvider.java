package com.beautyshop.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

public record RagProvider(EmbeddingModel embedding, ChatModel chat) {}
