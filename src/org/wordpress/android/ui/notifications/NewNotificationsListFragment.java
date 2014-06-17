package org.wordpress.android.ui.notifications;

import android.app.ListFragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.simperium.client.Bucket;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.SimperiumUtils;
import org.wordpress.android.util.ToastUtils;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

public class NewNotificationsListFragment extends ListFragment implements Bucket.Listener<Note> {
    private PullToRefreshHelper mFauxPullToRefreshHelper;
    private TestNotesAdapter mNotesAdapter;
    private OnNoteClickListener mNoteClickListener;
    private boolean mShouldLoadFirstNote;

    Bucket<Note> mBucket;

    /**
     * For responding to tapping of notes
     */
    public interface OnNoteClickListener {
        public void onClickNote(Note note);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.notifications_fragment_notes_list, container, false);
        return v;
    }

    /*@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // setup the initial notes adapter, starts listening to the bucket
        mBucket = SimperiumUtils.getNotesBucket();

        try {
            JSONArray notesArray = new JSONArray(loadJSONFromAsset());
            List<Note> notesArrayList = new ArrayList<Note>();
            for (int i=0; i < notesArray.length(); i++) {
                Note note = new Note(notesArray.getJSONObject(i));
                notesArrayList.add(note);
            }

            mNotesAdapter = new TestNotesAdapter(getActivity(), R.layout.notifications_list_item, notesArrayList);

            ListView listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
            listView.setDividerHeight(1);
            listView.setBackgroundColor(getResources().getColor(R.color.white));
            setListAdapter(mNotesAdapter);

            // Set empty text if no notifications
            TextView textview = (TextView) listView.getEmptyView();
            if (textview != null) {
                textview.setText(getText(R.string.notifications_empty_list));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }*/

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // setup the initial notes adapter, starts listening to the bucket
        mBucket = SimperiumUtils.getNotesBucket();

        ListView listView = getListView();
        listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
        listView.setDividerHeight(1);
        if (DisplayUtils.isLandscapeTablet(getActivity())) {
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        }
        if (mBucket != null) {
            mNotesAdapter = new TestNotesAdapter(getActivity(), mBucket);
            setListAdapter(mNotesAdapter);
        } else {
            ToastUtils.showToast(getActivity(), R.string.error_refresh_notifications);
        }

        // Set empty text if no notifications
        TextView textview = (TextView) listView.getEmptyView();
        if (textview != null) {
            textview.setText(getText(R.string.notifications_empty_list));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //initPullToRefreshHelper();
        //mFauxPullToRefreshHelper.registerReceiver(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNotes();

        // start listening to bucket change events
        mBucket.addListener(this);
    }

    @Override
    public void onPause() {
        // unregister the listener and close the cursor
        mBucket.removeListener(this);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        //mNotesAdapter.closeCursor();

        //mFauxPullToRefreshHelper.unregisterReceiver(getActivity());
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //boolean isRefreshing = mFauxPullToRefreshHelper.isRefreshing();
        super.onConfigurationChanged(newConfig);
        // Pull to refresh layout is destroyed onDetachedFromWindow,
        // so we have to re-init the layout, via the helper here
        //initPullToRefreshHelper();
        //mFauxPullToRefreshHelper.setRefreshing(isRefreshing);
    }

    private void initPullToRefreshHelper() {
        mFauxPullToRefreshHelper = new PullToRefreshHelper(
                getActivity(),
                (PullToRefreshLayout) getActivity().findViewById(R.id.ptr_layout),
                new PullToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        // Show a fake refresh animation for a few seconds
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (hasActivity()) {
                                    mFauxPullToRefreshHelper.setRefreshing(false);
                                }
                            }
                        }, 2000);
                    }
                }, LinearLayout.class
        );
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Note note = mNotesAdapter.getNote(position);
        if (note != null && mNoteClickListener != null) {
            mNoteClickListener.onClickNote(note);
            if (getListView().getChoiceMode() == ListView.CHOICE_MODE_SINGLE) {
                mNotesAdapter.setSelectedPosition(position);
            }
        }
    }

    public void resetSelection() {
        mNotesAdapter.setSelectedPosition(ListView.INVALID_POSITION);
        refreshNotes();
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        mNoteClickListener = listener;
    }

    protected void updateLastSeenTime() {
       /* // set the timestamp to now
        try {
            if (mNotesAdapter != null && mNotesAdapter.getCount() > 0 && SimperiumUtils.getMetaBucket() != null) {
                Note newestNote = mNotesAdapter.getNote(0);
                BucketObject meta = SimperiumUtils.getMetaBucket().get("meta");
                meta.setProperty("last_seen", newestNote.getTimestamp());
                meta.save();
            }
        } catch (BucketObjectMissingException e) {
            // try again later, meta is created by wordpress.com
        }*/
    }

    public void refreshNotes() {
        if (!hasActivity() || mNotesAdapter == null) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNotesAdapter.reloadNotes();
                /*updateLastSeenTime();

                // Show first note if we're on a landscape tablet
                if (mShouldLoadFirstNote && mNotesAdapter.getCount() > 0) {
                    mShouldLoadFirstNote = false;
                    Note note = mNotesAdapter.getNote(0);
                    if (note != null && mNoteClickListener != null) {
                        mNoteClickListener.onClickNote(note);
                        getListView().setItemChecked(0, true);
                    }
                }*/
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Simperium bucket listener methods
     */
    @Override
    public void onSaveObject(Bucket<Note> bucket, Note object) {
        refreshNotes();
    }

    @Override
    public void onDeleteObject(Bucket<Note> bucket, Note object) {
        refreshNotes();
    }

    @Override
    public void onChange(Bucket<Note> bucket, Bucket.ChangeType type, String key) {
        refreshNotes();
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        //noop
    }

    public void setShouldLoadFirstNote(boolean shouldLoad) {
        mShouldLoadFirstNote = shouldLoad;
    }

    private boolean hasActivity() {
        return getActivity() != null;
    }
}