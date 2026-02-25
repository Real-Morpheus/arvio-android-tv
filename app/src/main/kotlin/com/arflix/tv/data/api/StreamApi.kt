package com.arflix.tv.data.api

import com.arflix.tv.data.model.AddonManifest
import com.arflix.tv.data.model.CatalogResponse
import com.arflix.tv.data.model.StreamResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface StreamApi {

    @GET("manifest.json")
    suspend fun getManifest(): AddonManifest

    @GET("stream/movie/{id}.json")
    suspend fun getMovieStreams(
        @Path("id") id: String
    ): StreamResponse

    @GET("stream/series/{id}.json")
    suspend fun getSeriesStreams(
        @Path("id") id: String
    ): StreamResponse

    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("id") id: String
    ): CatalogResponse

    @GET("catalog/{type}/{id}/search={query}.json")
    suspend fun searchCatalog(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("query") query: String
    ): CatalogResponse
}
