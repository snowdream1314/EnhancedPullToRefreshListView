package com.snowdream1314.enhancedpulltorefreshlistview.pulltorefresh.interfaces;

import android.view.View;
import android.widget.AbsListView.OnScrollListener;

/**
 * you can listen ListView.OnScrollListener or this one. it will invoke
 * onXScrolling when header/footer scroll back.
 */
public interface OnXScrollListener extends OnScrollListener {
    public void onXScrolling(View view);
}