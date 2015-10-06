package com.akashpal.ribbit.ui;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.akashpal.ribbit.ParseConstants;
import com.akashpal.ribbit.R;
import com.akashpal.ribbit.adapter.UserAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

public class FriendsFragment extends Fragment {

	public static final String TAG = FriendsFragment.class.getSimpleName();
	List<ParseUser> mFriends; // list of all friends for current user in parse
	ParseRelation<ParseUser> mFriendsRelation;
	ParseUser mCurrentUser;
	GridView mGridView;

	public FriendsFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.user_grid, container,
				false);
		mGridView = (GridView) rootView.findViewById(R.id.firendsGrid);
		TextView emptyTextView = (TextView) rootView
				.findViewById(android.R.id.empty);
		mGridView.setEmptyView(emptyTextView);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();

		mCurrentUser = ParseUser.getCurrentUser();
		mFriendsRelation = mCurrentUser
				.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

		getActivity().setProgressBarIndeterminateVisibility(true);

		ParseQuery<ParseUser> query = mFriendsRelation.getQuery();
		query.addAscendingOrder(ParseConstants.KEY_USERNAME);
		query.setLimit(1000);
		query.findInBackground(new FindCallback<ParseUser>() {

			@Override
			public void done(List<ParseUser> friends, ParseException e) {

				getActivity().setProgressBarIndeterminateVisibility(false);
				if (e == null) {
					// success
					mFriends = friends;
					String[] usernames = new String[mFriends.size()];
					int i = 0;
					for (ParseUser user : mFriends) {
						usernames[i] = user.getUsername();
						i++;
					}

					if (mGridView.getAdapter() == null) {

						UserAdapter adapter = new UserAdapter(getActivity(),
								mFriends);
						mGridView.setAdapter(adapter);
					}
					else{
						((UserAdapter)mGridView.getAdapter()).refill(mFriends);
					}
				} else {
					// error
					Log.e(TAG, e.getMessage());
					/*
					 * AlertDialog.Builder builder = new
					 * AlertDialog.Builder(getListView().getContext());
					 * builder.setMessage(e.getMessage())
					 * .setTitle(R.string.error_title)
					 * .setPositiveButton(android.R.string.ok, null);
					 * 
					 * AlertDialog dialog = builder.create(); dialog.show();
					 */
				}
			}
		});

	}
}
