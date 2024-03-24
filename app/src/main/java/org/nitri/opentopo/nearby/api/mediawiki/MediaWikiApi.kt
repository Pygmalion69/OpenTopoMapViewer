package org.nitri.opentopo.nearby.api.mediawiki

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MediaWikiApi {
    @GET("w/api.php")
    fun getNearbyPages(
        @Query("action") action: String?,
        @Query("prop") prop: String?,
        @Query("colimit") colimit: Int,
        @Query("piprop") piprop: String?,
        @Query("pithumbsize") pithumbsize: Int,
        @Query("pilimit") pilimit: Int,
        @Query("wbptterms") wbptterms: String?,
        @Query("generator") generator: String?,
        @Query("ggscoord") ggscoord: String?,
        @Query("ggsradius") ggsradius: Int,
        @Query("ggslimit") ggslimit: Int,
        @Query("inprop") inprop: String?,
        @Query("format") format: String?
    ): Call<MediaWikiResponse?>?
}
