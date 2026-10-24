package org.nitri.opentopo.nearby

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.nitri.opentopo.BuildConfig
import org.nitri.opentopo.R
import org.nitri.opentopo.nearby.api.NearbyDatabase.Companion.getDatabase
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiApi
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.nearby.repo.NearbyRepository
import org.nitri.opentopo.nearby.viewmodel.NearbyViewModel
import org.nitri.opentopo.nearby.viewmodel.NearbyViewModelFactory
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NearbyFragment : Fragment() {
    private var mListener: OnFragmentInteractionListener? = null
    private var mLatitude = 0.0
    private var mLongitude = 0.0
    private val gson = GsonBuilder().setStrictness(Strictness.LENIENT).create()
    private lateinit var nearbyViewModel: NearbyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (arguments != null) {
            mLatitude = requireArguments().getDouble(PARAM_LATITUDE)
            mLongitude = requireArguments().getDouble(PARAM_LONGITUDE)
        }
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor { chain: Interceptor.Chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header(
                    "User-Agent",
                    WikiRequestHeaders.userAgent(BuildConfig.VERSION_NAME)
                )
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        val wikiBaseUrl = requireContext().getString(R.string.wiki_base_url)
        val retrofit = Retrofit.Builder()
            .baseUrl(wikiBaseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()
        val api = retrofit.create(MediaWikiApi::class.java)
        val nearbyDatabase = getDatabase(requireActivity())
        val dao = nearbyDatabase.nearbyDao()
        val nearbyRepository = NearbyRepository(dao, api, mLatitude, mLongitude)
        val factory = NearbyViewModelFactory(nearbyRepository)
        nearbyViewModel = ViewModelProvider(this, factory)[NearbyViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        id = R.id.nearby_compose_view
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenTopoTheme(dynamicColor = false) {
                val items by nearbyViewModel.items.observeAsState(emptyList())
                val sortedItems = remember(items, mLatitude, mLongitude) {
                    sortNearbyItems(items, mLatitude, mLongitude)
                }
                NearbyScreen(
                    items = sortedItems,
                    onArticleClick = ::openArticle,
                    onMapClick = { mListener?.showNearbyPlace(it) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mListener?.setUpNavigation(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mListener?.setUpNavigation(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener =
            if (context is OnFragmentInteractionListener) {
                context
            } else {
                throw RuntimeException(
                    context
                        .toString() + " must implement OnFragmentInteractionListener"
                )
            }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun openArticle(item: NearbyItem) {
        val uri = item.url?.let { Uri.parse(it) } ?: Uri.EMPTY
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.no_browser_found, Toast.LENGTH_SHORT).show()
        }
    }

    interface OnFragmentInteractionListener {
        /**
         * Show nearby item on map
         *
         * @param nearbyItem
         */
        fun showNearbyPlace(nearbyItem: NearbyItem?)

        /**
         * Set up navigation arrow
         */
        fun setUpNavigation(upNavigation: Boolean)
    }

    companion object {
        private const val PARAM_LATITUDE = "latitude"
        private const val PARAM_LONGITUDE = "longitude"
        private val TAG = NearbyFragment::class.java.simpleName

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param lat latitude
         * @param lon longitude
         * @return A new instance of fragment NearbyFragment.
         */
        fun newInstance(lat: Double, lon: Double): NearbyFragment {
            val fragment = NearbyFragment()
            val arguments = Bundle()
            arguments.putDouble(PARAM_LATITUDE, lat)
            arguments.putDouble(PARAM_LONGITUDE, lon)
            fragment.setArguments(arguments)
            return fragment
        }
    }
}
