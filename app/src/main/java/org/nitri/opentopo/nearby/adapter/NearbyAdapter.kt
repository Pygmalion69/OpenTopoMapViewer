package org.nitri.opentopo.nearby.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.nitri.opentopo.BuildConfig
import org.nitri.opentopo.R
import org.nitri.opentopo.nearby.adapter.NearbyAdapter.ItemViewHolder
import org.nitri.opentopo.nearby.entity.NearbyItem

class NearbyAdapter(
    private val mItems: MutableList<NearbyItem?>,
    private val mListener: OnItemClickListener
) : RecyclerView.Adapter<ItemViewHolder>() {

    @Volatile
    private var picassoInstance: Picasso? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ItemViewHolder {
        val view = LayoutInflater
            .from(viewGroup.context)
            .inflate(R.layout.nearby_item, viewGroup, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val currentPos = viewHolder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
        val item = mItems.getOrNull(currentPos) ?: return
        val originalUrl = item.thumbnail
        val thumbUrl = originalUrl?.let {
            when {
                it.startsWith("http://") -> it.replaceFirst("http://", "https://")
                it.startsWith("//") -> "https:$it"
                else -> it
            }
        }

        val picasso = getPicasso(viewHolder.itemView.context)

        if (BuildConfig.DEBUG && !loggingConfigured) {
            try {
                picasso.setIndicatorsEnabled(false)
                picasso.isLoggingEnabled = true
                loggingConfigured = true
            } catch (t: Throwable) {
                // ignore – these methods are debug helpers and may not exist on some builds
            }
        }

//        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "Binding position=$currentPos pageId=${item.pageid} title=${item.title}")
//            Log.d(TAG, "Thumbnail URL original=$originalUrl normalized=$thumbUrl")
//        }

        picasso
            .load(thumbUrl)
            .placeholder(R.drawable.ic_place)
            .error(R.drawable.ic_place)
            .resize(60, 60)
            .centerCrop()
            .into(viewHolder.ivThumb, object : Callback {
                override fun onSuccess() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Picasso load SUCCESS for pageId=${item.pageid}")
                    }
                }

                override fun onError(e: Exception?) {
                    Log.w(TAG, "Picasso load FAILED for pageId=${item.pageid} url=$thumbUrl", e)
                }
            })
        viewHolder.tvTitle.text = item.title
        viewHolder.tvDescription.text = item.description
        if (currentPos == mItems.size - 1) viewHolder.divider.visibility = View.GONE
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun getItemId(position: Int): Long {
        val pageId = mItems[position]?.pageid ?: return 0
        return pageId.toLong()
    }

    inner class ItemViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        var tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private var ivMap: ImageView = itemView.findViewById(R.id.ivMap)
        var divider: View

        init {
            ivMap.setOnClickListener(this)
            divider = itemView.findViewById(R.id.divider)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.ivMap) {
                mListener.onMapItemClick(getBindingAdapterPosition())
            } else {
                mListener.onItemClick(getBindingAdapterPosition())
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(index: Int)
        fun onMapItemClick(index: Int)
    }

    private fun getPicasso(context: android.content.Context): Picasso {
        val cached = picassoInstance
        if (cached != null) return cached

        synchronized(this) {
            val again = picassoInstance
            if (again != null) return again

            val ua = "OpenTopoMapViewer/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})"
            val referer = "https://www.wikipedia.org/"

            val headerInterceptor = Interceptor { chain ->
                val req: Request = chain.request().newBuilder()
                    .header("User-Agent", ua)
                    .header("Accept", "image/*")
                    .header("Referer", referer)
                    .build()
                chain.proceed(req)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(headerInterceptor)
                .build()

            val p = Picasso.Builder(context.applicationContext)
                .downloader(OkHttp3Downloader(okHttpClient))
                .build()
            picassoInstance = p
            return p
        }
    }

    companion object {
        private val TAG = NearbyAdapter::class.java.simpleName
        private var loggingConfigured = false
    }
}
