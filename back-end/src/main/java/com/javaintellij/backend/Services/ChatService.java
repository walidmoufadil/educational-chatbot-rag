package com.javaintellij.backend.Services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private ChatClient chatClient;
    private VectorStore vectorStore;
    @Value("classPath:/prompts/prompt.st")
    private Resource promptResource;

    public ChatService(ChatClient.Builder chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient.build();
        this.vectorStore = vectorStore;
    }
    public String chat(String message) {
       List<Document> documents = vectorStore.similaritySearch(message);
       List<String> context = documents.stream().map(Document::getContent).toList();
        PromptTemplate promptTemplate = new PromptTemplate(promptResource);
        Prompt prompt = promptTemplate.create(
                Map.of("context",context,"question",message)
        );
        return chatClient.prompt(prompt).call().content();
    }
}
