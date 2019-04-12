package org.nitri.opentopo.nearby.api.mediawiki;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MediaWikiApi {

    @GET("w/api.php")
    Call<MediaWikiResponse> getNearbyPages(@Query("action") String action,
                                           @Query("prop") String prop,
                                           @Query("colimit") int colimit,
                                           @Query("piprop") String piprop,
                                           @Query("pithumbsize") int pithumbsize,
                                           @Query("pilimit") int pilimit,
                                           @Query("wbptterms") String wbptterms,
                                           @Query("generator") String generator,
                                           @Query("ggscoord") String ggscoord,
                                           @Query("ggsradius") int ggsradius,
                                           @Query("ggslimit") int ggslimit,
                                           @Query("inprop") String inprop,
                                           @Query("format") String format);
}
