package com.aicommit.provider

class CustomProvider(
    apiKey: String,
    model: String,
    baseUrl: String
) : OpenAiProvider(apiKey = apiKey, model = model, baseUrl = baseUrl) {

    override val id = "custom"
    override val displayName = "自定义"
}
