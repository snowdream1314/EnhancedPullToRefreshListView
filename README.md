# EnhancedPullToRefreshListView

An Android sample integrates [EnhancedListView](https://github.com/timroes/EnhancedListView) with [PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh)

## Demo

![demo](https://github.com/snowdream1314/EnhancedPullToRefreshListView/blob/master/demo.gif)

## Usage

#### Add dependency

        compile 'com.snowdream1314:enhancedpulltorefreshlistview:1.0.0'
        
#### Use it in your layout xml replacing listview:

        <com.snowdream1314.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView
            android:id="@+id/lv_like"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

        </com.snowdream1314.enhancedpulltorefreshlistview.EnhancedPullToRefreshListView>

#### Get instance and use it like EnhancedListView.

        public class MyActivity extends Activity implements IXListViewListener {
        
            ...

            EnhancedPullToRefreshListView mListView = (EnhancedPullToRefreshListView) findViewById(...);
            
            //dismiss callback function
            mListView.setDismissCallback(new EnhancedPullToRefreshListView.OnDismissCallback() {
                    @Override
                    public EnhancedPullToRefreshListView.Undoable onDismiss(EnhancedPullToRefreshListView listView, int position) {
                        final int deletePosition = position;
                        final Account account = accounts.get(position);
                        
                        //delete data from local
                        accounts.remove(position)

                        return new EnhancedPullToRefreshListView.Undoable() {
                            @Override
                            public void undo() {
                                accounts.add(deletePosition, account);
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public String getTitle() {
                                return null;
                            }

                            @Override
                            public void discard() {
                                //delete data from database
                            }
                        };
                    }
                });
                
            ...
        }
            
#### settings

* pull to refresh       

        mListView.setPullRefreshEnable(true);//open pull to refresh, false for close

* pull to load more

        mListView.setOnScrollListener(your-pull-to-load-more-function);
        
* swipe to dismiss

        mListView.enableSwipeToDismiss();//open swipe to dismiss
        
        //close swipe to dismiss
        //mListView.disableSwipeToDismiss()

* others
        
        //SwipeDirection 
        mListView.setSwipeDirection(EnhancedPullToRefreshListView.SwipeDirection.BOTH);
        
        //undo delay, millseconds
        mListView.setUndoHideDelay(2500);
        
        //undo style
        mListView.setUndoStyle(EnhancedPullToRefreshListView.UndoStyle.COLLAPSED_POPUP);
        
        //dismiss after undo delay without touch
        mListView.setRequireTouchBeforeDismiss(false);
        
#### refresh callback

        mListView.setXListViewListener(MyActivity.this);
        
        public void onRefresh() {
            //refresh time
            SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
            RefreshTime.setRefreshTime(getContext(), df.format(new Date()));
            mListView.setRefreshTime(RefreshTime.getRefreshTime(getContext()));
            
            //refresh function
            reloadData();
        }

        public void onLoadMore() {
            //loadMore function
        }
        
## Thanks

* [EnhancedListView](https://github.com/timroes/EnhancedListView)

* [PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh)

* [Android-PullToRefresh-SwipeMenuListView-Sample](https://github.com/licaomeng/Android-PullToRefresh-SwipeMenuListView-Sample)

## 关于我

* email: xxq2dream@gmail.com


License
=======

    The MIT License (MIT)

    Copyright (c) 2016 snowdream1314

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:
	
	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.


            