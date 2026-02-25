package com.arflix.tv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class AddonType {
    OFFICIAL,
    COMMUNITY,
    SUBTITLE,
    METADATA,
    CUSTOM,
    CLOUDSTREAM
}

@JsonClass(generateAdapter = true)
data class AddonManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val resources: List<String>? = null,
    val types: List<String>? = null,
    val catalogs: List<ManifestCatalog>? = null,
    val idPrefixes: List<String>? = null,
    val logo: String? = null,
    val background: String? = null,
    val contactEmail: String? = null
)

@JsonClass(generateAdapter = true)
data class ManifestCatalog(
    val type: String,
    val id: String,
    val name: String? = null
)

data class Addon(
    val url: String,
    val manifest: AddonManifest,
    val type: AddonType = AddonType.COMMUNITY,
    val enabled: Boolean = true
) {
    val supportsStreams: Boolean
        get() = manifest.resources?.any { it == "stream" || it.contains("stream") } == true
}

@JsonClass(generateAdapter = true)
data class StreamResponse(
    val streams: List<StreamSource>? = null
)

@JsonClass(generateAdapter = true)
data class StreamSource(
    val url: String? = null,
    val title: String? = null,
    val name: String? = null,
    val description: String? = null,
    @Json(name = "infoHash") val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val ytId: String? = null,
    val subtitles: List<Subtitle>? = null,
    val behaviorHints: BehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class Subtitle(
    val url: String,
    val lang: String,
    val id: String? = null
)

@JsonClass(generateAdapter = true)
data class BehaviorHints(
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val proxyHeaders: Map<String, Map<String, String>>? = null
)

@JsonClass(generateAdapter = true)
data class CatalogResponse(
    val metas: List<MetaItem>? = null
)

@JsonClass(generateAdapter = true)
data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String>? = null,
    val runtime: String? = null,
    val year: Int? = null
)
