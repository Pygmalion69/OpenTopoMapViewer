package org.nitri.opentopo.overlay;

import android.annotation.SuppressLint;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.nitri.opentopo.Util;
import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

public class WayPointInfoWindow extends BasicInfoWindow {

    private final String mSubDescription;
    private final int mTitleId;
    private final int mDescriptionId;
    private final int mSubDescriptionId;

    public WayPointInfoWindow(int layoutResId, int titleId, int descriptionId, int subDescriptionId,
                              String subDescription, MapView mapView) {
        super(layoutResId, mapView);
        mTitleId = titleId;
        mDescriptionId = descriptionId;
        mSubDescriptionId = subDescriptionId;
        mSubDescription = subDescription;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onOpen(Object item) {
        OverlayItem overlayItem = (OverlayItem) item;
        String title = overlayItem.getTitle();
        if (title == null)
            title = "";
        if (mView == null) {
            Log.w(IMapView.LOGTAG, "Error trapped, BasicInfoWindow.open, mView is null!");
            return;
        }
        TextView temp = mView.findViewById(mTitleId);

        if (temp != null) temp.setText(title);

        String snippet = overlayItem.getSnippet();
        if (snippet == null)
            snippet = "";
        Spanned snippetHtml = Util.fromHtml(snippet.replace("href=\"//", "href=\"http://"));
        TextView snippetText = mView.findViewById(mDescriptionId);
        snippetText.setText(snippetHtml);
        snippetText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean ret = false;
                CharSequence text = ((TextView) v).getText();
                Spannable stext = Spannable.Factory.getInstance().newSpannable(text);
                TextView widget = (TextView) v;
                int action = event.getAction();

                if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    x -= widget.getTotalPaddingLeft();
                    y -= widget.getTotalPaddingTop();

                    x += widget.getScrollX();
                    y += widget.getScrollY();

                    Layout layout = widget.getLayout();
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    ClickableSpan[] link = stext.getSpans(off, off, ClickableSpan.class);

                    if (link.length != 0) {
                        if (action == MotionEvent.ACTION_UP) {
                            link[0].onClick(widget);
                        }
                        ret = true;
                    }
                }
                return ret;
            }
        });

        TextView subDescText = mView.findViewById(mSubDescriptionId);
        if (mSubDescription != null && !("".equals(mSubDescription))) {
            subDescText.setText(Util.fromHtml(mSubDescription));
            subDescText.setVisibility(View.VISIBLE);
            subDescText.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            subDescText.setVisibility(View.GONE);
        }

    }
}
