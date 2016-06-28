package com.snowdream.enhancedpulltorefreshlistview;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.snowdream.enhancedpulltorefreshlistview.pulltorefresh.interfaces.IXListViewListener;
import com.snowdream.enhancedpulltorefreshlistview.pulltorefresh.interfaces.OnXScrollListener;
import com.snowdream.enhancedpulltorefreshlistview.pulltorefresh.view.PullToRefreshListFooter;
import com.snowdream.enhancedpulltorefreshlistview.pulltorefresh.view.PullToRefreshListHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by snow on 2016/6/24.
 */

public class EnhancedPullToRefreshListView extends ListView implements AbsListView.OnScrollListener {

    /**
     * Defines the style in which <i>undos</i> should be displayed and handled in the list.
     * Pass this to {@link #setUndoStyle(com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.UndoStyle)}
     * to change the default behavior from {@link #SINGLE_POPUP}.
     */
    public enum UndoStyle {

        /**
         * Shows a popup window, that allows the user to undo the last
         * dismiss. If another element is deleted, the undo popup will undo that deletion.
         * The user is only able to undo the last deletion.
         */
        SINGLE_POPUP,

        /**
         * Shows a popup window, that allows the user to undo the last dismiss.
         * If another item is deleted, this will be added to the chain of undos. So pressing
         * undo will undo the last deletion, pressing it again will undo the deletion before that,
         * and so on. As soon as the popup vanished (e.g. because {@link #setUndoHideDelay(int) autoHideDelay}
         * is over) all saved undos will be discarded.
         */
        MULTILEVEL_POPUP,

        /**
         * Shows a popup window, that allows the user to undo the last dismisses.
         * If another item is deleted, while there is still an undo popup visible, the label
         * of the button changes to <i>Undo all</i> and a press on the button, will discard
         * all stored undos. As soon as the popup vanished (e.g. because {@link #setUndoHideDelay(int) autoHideDelay}
         * is over) all saved undos will be discarded.
         */
        COLLAPSED_POPUP

    }

    /**
     * Defines the direction in which list items can be swiped out to delete them.
     * Use {@link #setSwipeDirection(com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.SwipeDirection)}
     * to change the default behavior.
     * <p>
     * <b>Note:</b> This method requires the <i>Swipe to Dismiss</i> feature enabled. Use
     * {@link #enableSwipeToDismiss()}
     * to enable the feature.
     */
    public enum SwipeDirection {

        /**
         * The user can swipe each item into both directions (left and right) to delete it.
         */
        BOTH,

        /**
         * The user can only swipe the items to the beginning of the item to
         * delete it. The start of an item is in Left-To-Right languages the left
         * side and in Right-To-Left languages the right side. Before API level
         * 17 this is always the left side.
         */
        START,

        /**
         * The user can only swipe the items to the end of the item to delete it.
         * This is in Left-To-Right languages the right side in Right-To-Left
         * languages the left side. Before API level 17 this will always be the
         * right side.
         */
        END

    }

    /**
     * The callback interface used by {@link #setShouldSwipeCallback(EnhancedPullToRefreshListView.OnShouldSwipeCallback)}
     * to inform its client that a list item is going to be swiped and check whether is
     * should or not. Implement this to prevent some items from be swiped.
     */
    public interface OnShouldSwipeCallback {

        /**
         * Called when the user is swiping an item from the list.
         * <p>
         * If the user should get the possibility to swipe the item, return true.
         * Otherwise, return false to disable swiping for this item.
         *
         * @param listView The {@link EnhancedPullToRefreshListView} the item is wiping from.
         * @param position The position of the item to swipe in your adapter.
         * @return Whether the item should be swiped or not.
         */
        boolean onShouldSwipe(EnhancedPullToRefreshListView listView, int position);

    }

    /**
     * The callback interface used by {@link #setDismissCallback(EnhancedPullToRefreshListView.OnDismissCallback)}
     * to inform its client about a successful dismissal of one or more list item positions.
     * Implement this to remove items from your adapter, that has been swiped from the list.
     */
    public interface OnDismissCallback {

        /**
         * Called when the user has deleted an item from the list. The item has been deleted from
         * the {@code listView} at {@code position}. Delete this item from your adapter.
         * <p>
         * Don't return from this method, before your item has been deleted from the adapter, meaning
         * if you delete the item in another thread, you have to make sure, you don't return from
         * this method, before the item has been deleted. Since the way how you delete your item
         * depends on your data and adapter, the {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
         * cannot handle that synchronizing for you. If you return from this method before you removed
         * the view from the adapter, you will most likely get errors like exceptions and flashing
         * items in the list.
         * <p>
         * If the user should get the possibility to undo this deletion, return an implementation
         * of {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.Undoable} from this method.
         * If you return {@code null} no undo will be possible. You are free to return an {@code Undoable}
         * for some items, and {@code null} for others, though it might be a horrible user experience.
         *
         * @param listView The {@link EnhancedPullToRefreshListView} the item has been deleted from.
         * @param position The position of the item to delete from your adapter.
         * @return An {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.Undoable}, if you want
         *      to give the user the possibility to undo the deletion.
         */
        Undoable onDismiss(EnhancedPullToRefreshListView listView, int position);

    }

    /**
     * Extend this abstract class and return it from
     * {@link EnhancedPullToRefreshListView.OnDismissCallback#onDismiss(EnhancedPullToRefreshListView, int)}
     * to let the user undo the deletion you've done with your {@link EnhancedPullToRefreshListView.OnDismissCallback}.
     * You have at least to implement the {@link #undo()} method, and can override {@link #discard()}
     * and {@link #getTitle()} to offer more functionality. See the README file for example implementations.
     */
    public abstract static class Undoable {

        /**
         * This method must undo the deletion you've done in
         * {@link EnhancedPullToRefreshListView.OnDismissCallback#onDismiss(EnhancedPullToRefreshListView, int)} and reinsert
         * the element into the adapter.
         * <p>
         * In the most implementations, you will only remove the list item from your adapter
         * in the {@code onDismiss} method and delete it from the database (or your permanent
         * storage) in {@link #discard()}. In that case you only need to reinsert the item
         * to the adapter.
         */
        public abstract void undo();

        /**
         * Returns the individual undo message for this undo. This will be displayed in the undo
         * window, beside the undo button. The default implementation returns {@code null},
         * what will lead in a default message to be displayed in the undo window.
         * Don't call the super method, when overriding this method.
         *
         * @return The title for a special string.
         */
        public String getTitle() {
            return null;
        }

        /**
         * Discard the undo, meaning the user has no longer the possibility to undo the deletion.
         * Implement this, to finally delete your stuff from permanent storages like databases
         * (whereas in {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.OnDismissCallback#onKeyDown(int, android.view.KeyEvent)}
         * you should only remove it from the list adapter).
         */
        public void discard() { }

    }

    private class PendingDismissData implements Comparable<PendingDismissData> {

        public int position;
        /**
         * The view that should get swiped out.
         */
        public View view;
        /**
         * The whole list item view.
         */
        public View childView;

        PendingDismissData(int position, View view, View childView) {
            this.position = position;
            this.view = view;
            this.childView = childView;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }

    }

    private class UndoClickListener implements OnClickListener {

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            if(!mUndoActions.isEmpty()) {
                switch(mUndoStyle) {
                    case SINGLE_POPUP:
                        mUndoActions.get(0).undo();
                        mUndoActions.clear();
                        break;
                    case COLLAPSED_POPUP:
                        Collections.reverse(mUndoActions);
                        for(Undoable undo : mUndoActions) {
                            undo.undo();
                        }
                        mUndoActions.clear();
                        break;
                    case MULTILEVEL_POPUP:
                        mUndoActions.get(mUndoActions.size() - 1).undo();
                        mUndoActions.remove(mUndoActions.size() - 1);
                        break;
                }
            }

            // Dismiss dialog or change text
            if(mUndoActions.isEmpty()) {
                if(mUndoPopup.isShowing()) {
                    mUndoPopup.dismiss();
                }
            } else {
//                changePopupText();
                changeButtonLabel();
            }

            mValidDelayedMsgId++;
        }
    }

//    private class SureDoClickListener implements OnClickListener {
//        @Override
//        public void onClick(View v) {
//            discardUndo();
//        }
//    }

    private class HideUndoPopupHandler extends Handler {

        /**
         * Subclasses must implement this to receive messages.
         */
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == mValidDelayedMsgId) {
                discardUndo();
            }
        }
    }

    // Cached ViewConfiguration and system-wide constant values
    private float mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    private static final int TOUCH_STATE_NONE = 0;
    private static final int TOUCH_STATE_X = 1;
    private static final int TOUCH_STATE_Y = 2;

    private int MAX_Y = 5;
    private int MAX_X = 3;
    private float mDownX;
    private float mDownY;
    private int mTouchState;
    private int mTouchPosition;
    private final Object[] mAnimationLock = new Object[0];

    // Swipe-To-Dismiss
    private boolean mSwipeEnabled;
    private OnDismissCallback mDismissCallback;
    private OnShouldSwipeCallback mShouldSwipeCallback;
    private UndoStyle mUndoStyle = UndoStyle.SINGLE_POPUP;
    private boolean mTouchBeforeAutoHide = true;
    private SwipeDirection mSwipeDirection = SwipeDirection.BOTH;
    private int mUndoHideDelay = 5000;
    private int mSwipingLayout;

    private List<Undoable> mUndoActions = new ArrayList<Undoable>();
    private SortedSet<PendingDismissData> mPendingDismisses = new TreeSet<PendingDismissData>();
    private List<View> mAnimatedViews = new LinkedList<View>();
    private int mDismissAnimationRefCount;

    private boolean mSwipePaused;
    private boolean mSwiping;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private View mSwipeDownView;
    private View mSwipeDownChild;
    //    private TextView mUndoPopupTextView;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private float mScreenDensity;

    private PopupWindow mUndoPopup;
    private int mValidDelayedMsgId;
    private Handler mHideUndoHandler = new HideUndoPopupHandler();
    private Button mUndoButton;
    private Button mSureDoButton;
    // END Swipe-To-Dismiss


    // -- header view
    private PullToRefreshListHeader mHeaderView;
    // header view content, use it to calculate the Header's height. And hide it
    // when disable pull refresh.
    private RelativeLayout mHeaderViewContent;
    private TextView mHeaderTimeView;
    private int mHeaderViewHeight; // header view's height
    private boolean mEnablePullRefresh = true;
    private boolean mPullRefreshing = false; // is refreashing.

    // -- footer view
    private PullToRefreshListFooter mFooterView;
    private boolean mEnablePullLoad = false;//enable pull to load more, default is false
    private boolean mPullLoading;
    private boolean mIsFooterReady = false;
    private final static int SCROLLBACK_HEADER = 0;
    private final static int SCROLLBACK_FOOTER = 1;
    private int mScrollBack;
    private int mTotalItemCount;

    private Scroller mScroller; // used for scroll back
    private float mLastY = -1; // save event y
    private OnScrollListener mScrollListener; // user's scroll listener

    private final static int SCROLL_DURATION = 400;
    private final static int PULL_LOAD_MORE_DELTA = 50;
    private final static float OFFSET_RADIO = 1.8f;

    private IXListViewListener mListViewListener;

    /**
     * {@inheritDoc}
     */
    public EnhancedPullToRefreshListView(Context context) {
        super(context);
        init(context);
    }

    /**
     * {@inheritDoc}
     */
    public EnhancedPullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * {@inheritDoc}
     */
    public EnhancedPullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context ctx) {

        if(isInEditMode()) {
            // Skip initializing when in edit mode (IDE preview).
            return;
        }
        ViewConfiguration vc =ViewConfiguration.get(ctx);
        mSlop = getResources().getDimension(R.dimen.elv_touch_slop);
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = ctx.getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        // Initialize undo popup
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View undoView = inflater.inflate(R.layout.elv_undo_popup, null);
        mUndoButton = (Button)undoView.findViewById(R.id.undo);
        mUndoButton.setOnClickListener(new UndoClickListener());
        mUndoButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // If the user touches the screen invalidate the current running delay by incrementing
                // the valid message id. So this delay won't hide the undo popup anymore
                mValidDelayedMsgId++;
                return false;
            }
        });
        mSureDoButton = (Button) undoView.findViewById(R.id.sureDo);
        mSureDoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                discardUndo();
            }
        });
//        mUndoPopupTextView = (TextView)undoView.findViewById(R.id.text);

        mUndoPopup = new PopupWindow(undoView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mUndoPopup.setAnimationStyle(R.style.elv_fade_animation);

        mScreenDensity = getResources().getDisplayMetrics().density;
        // END initialize undo popup

        setOnScrollListener(makeScrollListener());

        mScroller = new Scroller(ctx, new DecelerateInterpolator());

        // init header view
        mHeaderView = new PullToRefreshListHeader(ctx);
        mHeaderViewContent = (RelativeLayout) mHeaderView.findViewById(R.id.xlistview_header_content);
        mHeaderTimeView = (TextView) mHeaderView.findViewById(R.id.xlistview_header_time);
        addHeaderView(mHeaderView);

        // init footer view
        mFooterView = new PullToRefreshListFooter(ctx);

        // init header height
        mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mHeaderViewHeight = mHeaderViewContent.getHeight();
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        MAX_X = dp2px(MAX_X);
        MAX_Y = dp2px(MAX_Y);
        mTouchState = TOUCH_STATE_NONE;

    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mIsFooterReady == false) {
            mIsFooterReady = true;
            addFooterView(mFooterView);
        }
        super.setAdapter(adapter);
    }

    /**
     * Enables the <i>Swipe to Dismiss</i> feature for this list. This allows users to swipe out
     * an list item element to delete it from the list. Every time the user swipes out an element
     * {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.OnDismissCallback#onDismiss(EnhancedPullToRefreshListView, int)}
     * of the given {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView} will be called. To enable
     * <i>undo</i> of the deletion, return an {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.Undoable}
     * from {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.OnDismissCallback#onDismiss(EnhancedPullToRefreshListView, int)}.
     * Return {@code null}, if you don't want the <i>undo</i> feature enabled. Read the README file
     * or the demo project for more detailed samples.
     *
     * @return The {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     * @throws java.lang.IllegalStateException when you haven't passed an {@link EnhancedPullToRefreshListView.OnDismissCallback}
     *      to {@link #setDismissCallback(EnhancedPullToRefreshListView.OnDismissCallback)} before calling this
     *      method.
     */
    public EnhancedPullToRefreshListView enableSwipeToDismiss() {

        if(mDismissCallback == null) {
            throw new IllegalStateException("You must pass an OnDismissCallback to the list before enabling Swipe to Dismiss.");
        }

        mSwipeEnabled = true;

        return this;
    }

    /**
     * Disables the <i>Swipe to Dismiss</i> feature for this list.
     *
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     */
    public EnhancedPullToRefreshListView disableSwipeToDismiss() {
        mSwipeEnabled = false;
        return this;
    }

    /**
     * Sets the callback to be called when the user dismissed an item from the list (either by
     * swiping it out - with <i>Swipe to Dismiss</i> enabled - or by deleting it with
     * {@link #delete(int)}). You must call this, before you call {@link #delete(int)} or
     * {@link #enableSwipeToDismiss()} otherwise you will get an {@link java.lang.IllegalStateException}.
     *
     * @param dismissCallback The callback used to handle dismisses of list items.
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     */
    public EnhancedPullToRefreshListView setDismissCallback(OnDismissCallback dismissCallback) {
        mDismissCallback = dismissCallback;
        return this;
    }

    /**
     * Sets the callback to be called when the user is swiping an item from the list.
     *
     * @param shouldSwipeCallback The callback used to handle swipes of list items.
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     */
    public EnhancedPullToRefreshListView setShouldSwipeCallback(OnShouldSwipeCallback shouldSwipeCallback) {
        mShouldSwipeCallback = shouldSwipeCallback;
        return this;
    }

    /**
     * Sets the undo style of this list. See the javadoc of {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.UndoStyle}
     * for a detailed explanation of the different styles. The default style (if you never call this
     * method) is {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.UndoStyle#SINGLE_POPUP}.
     *
     * @param undoStyle The style of this listview.
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     */
    public EnhancedPullToRefreshListView setUndoStyle(UndoStyle undoStyle) {
        mUndoStyle = undoStyle;
        return this;
    }

    /**
     * Sets the time in milliseconds after which the undo popup automatically disappears.
     * The countdown will start when the user touches the screen. If you want to start the countdown
     * immediately when the popups appears, call {@link #setRequireTouchBeforeDismiss(boolean)} with
     * {@code false}.
     *
     * @param hideDelay The delay in milliseconds.
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     */
    public EnhancedPullToRefreshListView setUndoHideDelay(int hideDelay) {
        mUndoHideDelay = hideDelay;
        return this;
    }

    /**
     * Sets whether another touch on the view is required before the popup counts down to dismiss
     * the undo popup. By default this is set to {@code true}.
     *
     * @param touchBeforeDismiss Whether the screen needs to be touched before the countdown starts.
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     *
     * @see #setUndoHideDelay(int)
     */
    public EnhancedPullToRefreshListView setRequireTouchBeforeDismiss(boolean touchBeforeDismiss) {
        mTouchBeforeAutoHide = touchBeforeDismiss;
        return this;
    }

    /**
     * Sets the directions in which a list item can be swiped to delete.
     * By default this is set to {@link SwipeDirection#BOTH} so that an item
     * can be swiped into both directions.
     * <p>
     * <b>Note:</b> This method requires the <i>Swipe to Dismiss</i> feature enabled. Use
     * {@link #enableSwipeToDismiss()} to enable the feature.
     *
     * @param direction The direction to which the swipe should be limited.
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     */
    public EnhancedPullToRefreshListView setSwipeDirection(SwipeDirection direction) {
        mSwipeDirection = direction;
        return this;
    }

    /**
     * Sets the id of the view, that should be moved, when the user swipes an item.
     * Only the view with the specified id will move, while all other views in the list item, will
     * stay where they are. This might be usefull to have a background behind the view that is swiped
     * out, to stay where it is (and maybe explain that the item is going to be deleted).
     * If you never call this method (or call it with 0), the whole view will be swiped. Also if there
     * is no view in a list item, with the given id, the whole view will be swiped.
     * <p>
     * <b>Note:</b> This method requires the <i>Swipe to Dismiss</i> feature enabled. Use
     * {@link #enableSwipeToDismiss()} to enable the feature.
     *
     * @param swipingLayoutId The id (from R.id) of the view, that should be swiped.
     * @return This {@link com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView}
     */
    public EnhancedPullToRefreshListView setSwipingLayout(int swipingLayoutId) {
        mSwipingLayout = swipingLayoutId;
        return this;
    }

    /**
     * Discard all stored undos and hide the undo popup dialog.
     * This method must be called in {@link android.app.Activity#onStop()}. Otherwise
     * {@link EnhancedPullToRefreshListView.Undoable#discard()} might not be called for several items, what might
     * break your data consistency.
     */
    public void discardUndo() {
        for(Undoable undoable : mUndoActions) {
            undoable.discard();
        }
        mUndoActions.clear();
        if(mUndoPopup.isShowing()) {
            mUndoPopup.dismiss();
        }
    }

    /**
     * Delete the list item at the specified position. This will animate the item sliding out of the
     * list and then collapsing until it vanished (same as if the user slides out an item).
     * <p>
     * NOTE: If you are using list headers, be aware, that the position argument must take care of
     * them. Meaning 0 references the first list header. So if you want to delete the first list
     * item, you have to pass the number of list headers as {@code position}. Most of the times
     * that shouldn't be a problem, since you most probably will evaluate the position which should
     * be deleted in a way, that respects the list headers.
     *
     * @param position The position of the item in the list.
     * @throws java.lang.IndexOutOfBoundsException when trying to delete an item outside of the list range.
     * @throws java.lang.IllegalStateException when this method is called before an {@link EnhancedPullToRefreshListView.OnDismissCallback}
     *      is set via {@link #setDismissCallback(com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.OnDismissCallback)}.
     * */
    public void delete(int position) {
        if(mDismissCallback == null) {
            throw new IllegalStateException("You must set an OnDismissCallback, before deleting items.");
        }
        if(position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException(String.format("Tried to delete item %d. #items in list: %d", position, getCount()));
        }
        View childView = getChildAt(position - getFirstVisiblePosition());
        View view = null;
        if(mSwipingLayout > 0) {
            view = childView.findViewById(mSwipingLayout);
        }
        if(view == null) {
            view = childView;
        }
        slideOutView(view, childView, position, true);
    }

    /**
     * Slide out a view to the right or left of the list. After the animation has finished, the
     * view will be dismissed by calling {@link #performDismiss(android.view.View, android.view.View, int)}.
     *
     * @param view The view, that should be slided out.
     * @param childView The whole view of the list item.
     * @param position The item position of the item.
     * @param toRightSide Whether it should slide out to the right side.
     */
    private void slideOutView(final View view, final View childView, final int position, boolean toRightSide) {

        // Only start new animation, if this view isn't already animated (too fast swiping bug)
        synchronized(mAnimationLock) {
            if(mAnimatedViews.contains(view)) {
                return;
            }
            ++mDismissAnimationRefCount;
            mAnimatedViews.add(view);
        }

        ViewPropertyAnimator.animate(view)
                .translationX(toRightSide ? mViewWidth : -mViewWidth)
                .alpha(0)
                .setDuration(mAnimationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        performDismiss(view, childView, position);
                    }
                });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev){
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }

        // Send a delayed message to hide popup
        if(mTouchBeforeAutoHide && mUndoPopup.isShowing()) {
            mHideUndoHandler.sendMessageDelayed(mHideUndoHandler.obtainMessage(mValidDelayedMsgId), mUndoHideDelay);
        }

        // Store width of this list for usage of swipe distance detection
        if (mViewWidth < 2) {
            mViewWidth = getWidth();
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();

                int oldPos = mTouchPosition;
                mDownX = ev.getX();
                mDownY = ev.getY();
                mTouchState = TOUCH_STATE_NONE;

                mTouchPosition = pointToPosition((int) ev.getX(), (int) ev.getY());

                if (mSwipePaused) {
                    return super.onTouchEvent(ev);
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = getChildCount();
                int[] listViewCoords = new int[2];
                getLocationOnScreen(listViewCoords);
                int x = (int) ev.getRawX() - listViewCoords[0];
                int y = (int) ev.getRawY() - listViewCoords[1];
                View child;
                for (int i = getHeaderViewsCount(); i < childCount; i++) {
                    child = getChildAt(i);
                    if(child != null) {
                        child.getHitRect(rect);
                        if (rect.contains(x, y)) {
                            // if a specific swiping layout has been giving, use this to swipe.
                            if(mSwipingLayout > 0) {
                                View swipingView = child.findViewById(mSwipingLayout);
                                if(swipingView != null) {
                                    mSwipeDownView = swipingView;
                                    mSwipeDownChild = child;
                                    break;
                                }
                            }
                            // If no swiping layout has been found, swipe the whole child
                            mSwipeDownView = mSwipeDownChild = child;
                            break;
                        }
                    }
                }

                if (mSwipeDownView != null) {
                    // test if the item should be swiped
                    int position = getPositionForView(mSwipeDownView) - getHeaderViewsCount();
                    if ((mShouldSwipeCallback == null) ||
                            mShouldSwipeCallback.onShouldSwipe(this, position)) {
                        mDownX = ev.getRawX();
                        mDownPosition = position;

                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(ev);
                    } else {
                        // set back to null to revert swiping
                        mSwipeDownView = mSwipeDownChild = null;
                    }
                }
                super.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_MOVE:
                final float deltaY = ev.getRawY() - mLastY;

                float dy = Math.abs((ev.getY() - mDownY));
                float dx = Math.abs((ev.getX() - mDownX));
                mLastY = ev.getRawY();

                if (!mSwiping && Math.pow(dx, 2) / Math.pow(dy, 2) <= 3 ) {
                    if (getFirstVisiblePosition() == 0 && (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {
                        // the first item is showing, header has shown or pull down.
                        updateHeaderHeight(deltaY / OFFSET_RADIO);
                        invokeOnScrolling();
                    } else if ((mFooterView.getBottomMargin() > 0 || deltaY < 0)  && mEnablePullLoad) {
                        // last item, already pulled up or want to pull up.
                        updateFooterHeight(-deltaY / OFFSET_RADIO);
                    }
                }

                if (mTouchState == TOUCH_STATE_X && mSwipeEnabled) {
                    if (mVelocityTracker == null || mSwipePaused) {
                        break;
                    }

                    mVelocityTracker.addMovement(ev);
                    float deltaX = ev.getRawX() - mDownX;
                    // Only start swipe in correct direction
                    if(isSwipeDirectionValid(deltaX)) {
                        ViewParent parent = getParent();
                        if(parent != null) {
                            // If we swipe don't allow parent to intercept touch (e.g. like NavigationDrawer does)
                            // otherwise swipe would not be working.
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                        if (Math.abs(deltaX) > mSlop) {
                            mSwiping = true;
                            requestDisallowInterceptTouchEvent(true);

                            // Cancel ListView's touch (un-highlighting the item)
                            MotionEvent cancelEvent = MotionEvent.obtain(ev);
                            cancelEvent.setAction(MotionEvent.ACTION_CANCEL
                                    | (ev.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                            super.onTouchEvent(cancelEvent);
                        }
                    } else {
                        // If we swiped into wrong direction, act like this was the new
                        // touch down point
                        mDownX = ev.getRawX();
                        deltaX = 0;
                    }

                    if (mSwiping) {
                        ViewHelper.setTranslationX(mSwipeDownView, deltaX);
                        ViewHelper.setAlpha(mSwipeDownView, Math.max(0f, Math.min(1f,
                                1f - 2f * Math.abs(deltaX) / mViewWidth)));
                        return true;
                    }
                } else if (mTouchState == TOUCH_STATE_NONE) {
                    if (Math.abs(dy) > MAX_Y) {
                        mTouchState = TOUCH_STATE_Y;
                    } else if (dx > MAX_X && mSwipeEnabled) {
                        mTouchState = TOUCH_STATE_X;
//                        if (mOnSwipeListener != null) {
//                            mOnSwipeListener.onSwipeStart(mTouchPosition);
//                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mLastY = -1; // reset
                if (mEnablePullLoad && mFooterView.getHeight()>0 && mFooterView.getBottomMargin() > PULL_LOAD_MORE_DELTA) {
                    startLoadMore();
                    resetFooterHeight();
                    new ResetHeaderHeightTask().execute();
                } else if (getFirstVisiblePosition() == 0) {
                    // invoke refresh
                    if (mEnablePullRefresh && mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                        mPullRefreshing = true;
                        mHeaderView.setState(PullToRefreshListHeader.STATE_REFRESHING);
                        if (mListViewListener != null) {
                            mListViewListener.onRefresh();
                        }
                    }
                    resetHeaderHeight();
                }


                if (mTouchState == TOUCH_STATE_X && mSwipeEnabled) {
                    if (mVelocityTracker == null) {
                        break;
                    }

                    float deltaX = ev.getRawX() - mDownX;
                    mVelocityTracker.addMovement(ev);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = Math.abs(mVelocityTracker.getXVelocity());
                    float velocityY = Math.abs(mVelocityTracker.getYVelocity());
                    boolean dismiss = false;
                    boolean dismissRight = false;
                    if (Math.abs(deltaX) > mViewWidth / 2 && mSwiping) {
                        dismiss = true;
                        dismissRight = deltaX > 0;
                    } else if (mMinFlingVelocity <= velocityX && velocityX <= mMaxFlingVelocity
                            && velocityY < velocityX && mSwiping && isSwipeDirectionValid(mVelocityTracker.getXVelocity())
                            && deltaX >= mViewWidth * 0.2f) {
                        dismiss = true;
                        dismissRight = mVelocityTracker.getXVelocity() > 0;
                    }
                    if (dismiss) {
                        // dismiss
                        slideOutView(mSwipeDownView, mSwipeDownChild, mDownPosition, dismissRight);
                    } else if(mSwiping) {
                        // Swipe back to regular position
                        ViewPropertyAnimator.animate(mSwipeDownView)
                                .translationX(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    }
                    mVelocityTracker = null;
                    mDownX = 0;
                    mSwipeDownView = null;
                    mSwipeDownChild = null;
                    mDownPosition = AbsListView.INVALID_POSITION;
                    mSwiping = false;

                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    class ResetHeaderHeightTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            mPullRefreshing = false;
            mHeaderView.setState(PullToRefreshListHeader.STATE_NORMAL);
            resetHeaderHeight();

        }
    }

    /**
     * set last refresh time
     *
     * @param time
     */
    public void setRefreshTime(String time) {
        mHeaderTimeView.setText(time);
    }

    private void invokeOnScrolling() {
        if (mScrollListener instanceof OnXScrollListener) {
            OnXScrollListener l = (OnXScrollListener) mScrollListener;
            l.onXScrolling(this);
        }
    }

    private void updateHeaderHeight(float delta) {
        mHeaderView.setVisiableHeight((int) delta + mHeaderView.getVisiableHeight());
        if (mEnablePullRefresh && !mPullRefreshing) {
            if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                mHeaderView.setState(PullToRefreshListHeader.STATE_READY);
            } else {
                mHeaderView.setState(PullToRefreshListHeader.STATE_NORMAL);
            }
        }
        setSelection(0); // scroll to top each time
    }

    private void updateFooterHeight(float delta) {
        int height = mFooterView.getBottomMargin() + (int) delta;
        if (mEnablePullLoad && !mPullLoading) {
            if (height > PULL_LOAD_MORE_DELTA) { // height enough to invoke load
                // more.
                mFooterView.setState(PullToRefreshListFooter.STATE_READY);
            } else {
                mFooterView.setState(PullToRefreshListFooter.STATE_NORMAL);
            }
        }
        mFooterView.setBottomMargin(height);
    }

    /**
     * reset header view's height.
     */
    private void resetHeaderHeight() {
        int height = mHeaderView.getVisiableHeight();
        if (height == 0) // not visible.
            return;
        // refreshing and header isn't shown fully. do nothing.
        if (mPullRefreshing && height <= mHeaderViewHeight) {
            return;
        }
        int finalHeight = 0; // default: scroll back to dismiss header.
        // is refreshing, just scroll back to show all the header.
        if (mPullRefreshing && height > mHeaderViewHeight) {
            finalHeight = mHeaderViewHeight;
        }
        mScrollBack = SCROLLBACK_HEADER;
        mScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);
        // trigger computeScroll
        invalidate();
    }

    private void resetFooterHeight() {
        int bottomMargin = mFooterView.getBottomMargin();
        if (bottomMargin > 0) {
            mScrollBack = SCROLLBACK_FOOTER;
            mScroller.startScroll(0, bottomMargin, 0, -bottomMargin, SCROLL_DURATION);
            invalidate();
        }
    }

    private void startLoadMore() {
        mPullLoading = true;
        mFooterView.setState(PullToRefreshListFooter.STATE_LOADING);
        if (mListViewListener != null) {
            mListViewListener.onLoadMore();
        }
    }

    /**
     * enable or disable pull up load more feature.
     *
     * @param enable
     */
    public void setPullLoadEnable(boolean enable) {
        mEnablePullLoad = enable;
        if (!mEnablePullLoad) {
            mFooterView.hide();
            mFooterView.setOnClickListener(null);
        } else {
            mPullLoading = false;
            mFooterView.show();
            mFooterView.setState(PullToRefreshListFooter.STATE_NORMAL);
            // both "pull up" and "click" will invoke load more.
            mFooterView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startLoadMore();
                }
            });
        }
    }

    public void setPullRefreshEnable(boolean enable) {
        mEnablePullRefresh = enable;
        if (!mEnablePullRefresh) { // disable, hide the content
            mHeaderViewContent.setVisibility(View.INVISIBLE);
        } else {
            mHeaderViewContent.setVisibility(View.VISIBLE);
        }
    }

    /**
     * stop refresh, reset header view.
     */
    public void stopRefresh() {
        if (mPullRefreshing == true) {
            mPullRefreshing = false;
            resetHeaderHeight();
        }
    }


    /**
     * stop load more, reset footer view.
     */
    public void stopLoadMore() {
        if (mPullLoading == true) {
            mPullLoading = false;
            mFooterView.setState(PullToRefreshListFooter.STATE_NORMAL);
        }
    }


    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScrollBack == SCROLLBACK_HEADER) {
                mHeaderView.setVisiableHeight(mScroller.getCurrY());
            } else {
                mFooterView.setBottomMargin(mScroller.getCurrY());
            }
            postInvalidate();
            invokeOnScrolling();
        }
        super.computeScroll();
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mScrollListener = l;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // send to user's listener
        mTotalItemCount = totalItemCount;
        if (mScrollListener != null) {
            mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }


    public void setXListViewListener(IXListViewListener l) {
        mListViewListener = l;
    }

    /*
	* enable or disable swipe feature
	*
	* @param enable
	* */
    public void setSwipeEnable(boolean enable) {
        mSwipeEnabled = enable;
    }

    /**
     * Animate the dismissed list item to zero-height and fire the dismiss callback when
     * all dismissed list item animations have completed.
     *
     * @param dismissView The view that has been slided out.
     * @param listItemView The list item view. This is the whole view of the list item, and not just
     *                     the part, that the user swiped.
     * @param dismissPosition The position of the view inside the list.
     */
    private void performDismiss(final View dismissView, final View listItemView, final int dismissPosition) {

        final ViewGroup.LayoutParams lp = listItemView.getLayoutParams();
        final int originalLayoutHeight = lp.height;

        int originalHeight = listItemView.getHeight();
        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

                // Make sure no other animation is running. Remove animation from running list, that just finished
                boolean noAnimationLeft;
                synchronized(mAnimationLock) {
                    --mDismissAnimationRefCount;
                    mAnimatedViews.remove(dismissView);
                    noAnimationLeft = mDismissAnimationRefCount == 0;
                }

                if (noAnimationLeft) {
                    // No active animations, process all pending dismisses.

                    for(PendingDismissData dismiss : mPendingDismisses) {
                        if(mUndoStyle == UndoStyle.SINGLE_POPUP) {
                            for(Undoable undoable : mUndoActions) {
                                undoable.discard();
                            }
                            mUndoActions.clear();
                        }
                        Undoable undoable = mDismissCallback.onDismiss(EnhancedPullToRefreshListView.this, dismiss.position);
                        if(undoable != null) {
                            mUndoActions.add(undoable);
                        }
                        mValidDelayedMsgId++;
                    }

                    if(!mUndoActions.isEmpty()) {
//                        changePopupText();
                        changeButtonLabel();

                        // Show undo popup
                        float yLocationOffset = getResources().getDimension(R.dimen.elv_undo_bottom_offset);
                        mUndoPopup.setWidth((int)Math.min(mScreenDensity * 400, getWidth() * 0.6f));
                        mUndoPopup.showAtLocation(EnhancedPullToRefreshListView.this,
                                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
                                0, (int) yLocationOffset);

                        // Queue the dismiss only if required
                        if(!mTouchBeforeAutoHide) {
                            // Send a delayed message to hide popup
                            mHideUndoHandler.sendMessageDelayed(mHideUndoHandler.obtainMessage(mValidDelayedMsgId),
                                    mUndoHideDelay);
                        }
                    }

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : mPendingDismisses) {
                        ViewHelper.setAlpha(pendingDismiss.view, 1f);
                        ViewHelper.setTranslationX(pendingDismiss.view, 0);
                        lp = pendingDismiss.childView.getLayoutParams();
                        lp.height = originalLayoutHeight;
                        pendingDismiss.childView.setLayoutParams(lp);
                    }

                    mPendingDismisses.clear();
                }
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                listItemView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView, listItemView));
        animator.start();
    }

    /**
     * Changes the label of the undo button.
     */
    private void changeButtonLabel() {
        String undoMsg, suredoMsg;
        if(mUndoActions.size() > 1 && mUndoStyle == UndoStyle.COLLAPSED_POPUP) {
            undoMsg = getResources().getString(R.string.elv_undo_all);
            suredoMsg = getResources().getString(R.string.elv_suredo_all);
        } else {
            undoMsg = getResources().getString(R.string.elv_undo);
            suredoMsg = getResources().getString(R.string.elv_suredo);
        }
        mUndoButton.setText(undoMsg);
        mSureDoButton.setText(suredoMsg);
    }

    private OnScrollListener makeScrollListener() {
        return new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                mSwipePaused = scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        };
    }

    /**
     * Checks whether the delta of a swipe indicates, that the swipe is in the
     * correct direction, regarding the direction set via
     * {@link #setSwipeDirection(com.snowdream.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView.SwipeDirection)}
     *
     * @param deltaX The delta of x coordinate of the swipe.
     * @return Whether the delta of a swipe is in the right direction.
     */
    private boolean isSwipeDirectionValid(float deltaX) {

        int rtlSign = 1;
        // On API level 17 and above, check if we are in a Right-To-Left layout
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if(getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                rtlSign = -1;
            }
        }

        // Check if swipe has been done in the correct direction
        switch(mSwipeDirection) {
            default:
            case BOTH:
                return true;
            case START:
                return rtlSign * deltaX < 0;
            case END:
                return rtlSign * deltaX > 0;
        }

    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

		/*
		 * If the container window no longer visiable,
		 * dismiss visible undo popup window so it won't leak,
		 * cos the container window will be destroyed before dismissing the popup window.
		 */
        if(visibility != View.VISIBLE) {
            discardUndo();
        }
    }
}
