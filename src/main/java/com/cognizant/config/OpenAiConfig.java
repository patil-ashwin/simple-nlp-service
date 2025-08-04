package com.cognizant.config;

import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Autowired
    private AzureOpenAiChatModel azureOpenAiChatModel;

    // The AzureOpenAiChatModel is auto-configured by the starter
    // This class is optional and can be used for custom configuration
}