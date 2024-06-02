package org.nitri.opentopo.nearby

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.nitri.opentopo.BuildConfig
import org.nitri.opentopo.R
import org.nitri.opentopo.Util.distance
import org.nitri.opentopo.nearby.adapter.NearbyAdapter
import org.nitri.opentopo.nearby.api.NearbyDatabase.Companion.getDatabase
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiApi
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.nearby.repo.NearbyRepository
import org.nitri.opentopo.nearby.viewmodel.NearbyViewModel
import org.nitri.opentopo.nearby.viewmodel.NearbyViewModelFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NearbyFragment : Fragment(), NearbyAdapter.OnItemClickListener {
    private var mListener: OnFragmentInteractionListener? = null
    private var mLatitude = 0.0
    private var mLongitude = 0.0
    private val gson = GsonBuilder().setLenient().create()
    private val mNearbyItems: MutableList<NearbyItem?> = ArrayList()
    private lateinit var mNearbyAdapter: NearbyAdapter
    @SuppressLint("NotifyDataSetChanged")
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
                    "User-Agent", getString(R.string.app_name) + " "
                            + BuildConfig.VERSION_NAME
                )
                .method(original.method(), original.body())
                .build()
            chain.proceed(request)
        }
        val wikiBaseUrl = requireContext().getString(R.string.wiki_base_url)
        val retrofit = Retrofit.Builder()
            .baseUrl(wikiBaseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()
        mNearbyAdapter = NearbyAdapter(mNearbyItems, this)
        mNearbyAdapter.setHasStableIds(true)
        val api = retrofit.create(MediaWikiApi::class.java)
        val nearbyDatabase = getDatabase(requireActivity())
        val dao = nearbyDatabase.nearbyDao()
        val nearbyRepository = NearbyRepository(dao, api, mLatitude, mLongitude)
        val factory = NearbyViewModelFactory(nearbyRepository)
        val nearbyViewModel = ViewModelProvider(this, factory)[NearbyViewModel::class.java]
        val nearbyObserver = Observer { items: List<NearbyItem?>? ->
            mNearbyItems.clear()
            items?.let {
                mNearbyItems.addAll(items)
                setDistance()
                mNearbyItems.sortBy { it?.distance }
                mNearbyAdapter.notifyDataSetChanged()
            }
        }
        nearbyViewModel.items.observe(this, nearbyObserver)
    }

    private fun setDistance() {
        for (item in mNearbyItems) {
            item?.apply {
                distance =
                    Math.round(distance(mLatitude, mLongitude, item.lat, item.lon)).toInt()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_nearby, container, false)
        val nearbyRecyclerView = rootView.findViewById<RecyclerView>(R.id.nearby_recycler_view)
        nearbyRecyclerView.setLayoutManager(LinearLayoutManager(activity))
        nearbyRecyclerView.itemAnimator?.let {
            it.changeDuration = 0
        }
        nearbyRecyclerView.setAdapter(mNearbyAdapter)
        return rootView
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

    override fun onItemClick(index: Int) {
        val uri = mNearbyItems[index]?.url?.let { Uri.parse(it) } ?: Uri.EMPTY
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(browserIntent)
    }

    override fun onMapItemClick(index: Int) {
        mListener?.showNearbyPlace(mNearbyItems[index])
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
