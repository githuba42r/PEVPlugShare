package com.a42r.pevplugshare.ui.settings.model

data class AiModel(
    val id: String,
    val name: String,
    val provider: String,
    val cost: String,
    val contextSize: String? = null,
    val description: String? = null,
    val pricing: ModelPricing? = null
)

data class ModelPricing(
    val prompt: String,  // Cost per million prompt tokens
    val completion: String  // Cost per million completion tokens
)
