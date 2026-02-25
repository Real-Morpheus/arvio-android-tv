package com.arflix.tv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudStreamExtensionManifest(
    val name: String,
    val description: String? = null,
    val version: String? = null,
    val authors: List<String>? = null,
    val repositoryUrl: String? = null,
    val apiVersion: Int? = null,
    val pluginLists: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class CloudStreamPluginEntry(
    val name: String,
    val url: String,
    val version: Int? = null,
    val authors: List<String>? = null,
    val description: String? = null,
    @Json(name = "repositoryUrl") val repositoryUrl: String? = null,
    val tvTypes: List<String>? = null,
    val language: String? = null,
    val iconUrl: String? = null,
    val status: Int? = null,
    val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class CloudStreamPluginList(
    val plugins: List<CloudStreamPluginEntry>? = null,
    @Json(name = "all") val allPlugins: List<CloudStreamPluginEntry>? = null
)

@JsonClass(generateAdapter = true)
data class CloudStreamSearchResult(
    val name: String,
    val url: String,
    val posterUrl: String? = null,
    val year: Int? = null,
    val type: String? = null,
    val id: String? = null,
    val quality: String? = null,
    val dubStatus: String? = null,
    val rating: Double? = null
)

@JsonClass(generateAdapter = true)
data class CloudStreamSearchResponse(
    val results: List<CloudStreamSearchResult>? = null,
    val hasNextPage: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CloudStreamStreamSource(
    val url: String,
    val name: String? = null,
    val quality: Int? = null,
    val headers: Map<String, String>? = null,
    val referer: String? = null,
    val mimeType: String? = null,
    val subtitleUrls: List<CloudStreamSubtitle>? = null
)

@JsonClass(generateAdapter = true)
data class CloudStreamSubtitle(
    val url: String,
    val lang: String,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class CloudStreamLoadResponse(
    val name: String? = null,
    val url: String? = null,
    val posterUrl: String? = null,
    val plot: String? = null,
    val year: Int? = null,
    val rating: Int? = null,
    val tags: List<String>? = null,
    val duration: Int? = null,
    val actors: List<String>? = null,
    val contentRating: String? = null
)

data class CloudStreamExtension(
    val repositoryUrl: String,
    val manifest: CloudStreamExtensionManifest,
    val plugins: List<CloudStreamPluginEntry> = emptyList(),
    val enabled: Boolean = true
)
