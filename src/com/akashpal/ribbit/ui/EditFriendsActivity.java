package com.akashpal.ribbit.ui;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.akashpal.ribbit.ParseConstants;
import com.akashpal.ribbit.R;
import com.akashpal.ribbit.adapter.UserAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class EditFriendsActivity extends Activity {
	
	protected final String TAG = EditFriendsActivity.class.getSimpleName(); 

	List<ParseUser> mUsers; //List of all users in parse
	ParseRelation<ParseUser> mFriendRelation;
	ParseUser mCurrentUser;
	GridView mGridView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.user_grid);
		mGridView = (GridView) findViewById(R.id.firendsGrid);
		
		TextView emptyTextView = (TextView) findViewById(android.R.id.empty);
		mGridView.setEmptyView(emptyTextView);
		mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
		mGridView.setOnItemClickListener(mOnItemClickListener);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
	
		setProgressBarIndeterminateVisibility(true);
		
		mCurrentUser = ParseUser.getCurrentUser();
		mFriendRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);
		
	
		ParseQuery<ParseUser> query = ParseUser.getQuery();
		query.addAscendingOrder(ParseConstants.KEY_USERNAME);
		query.setLimit(1000);
		query.findInBackground(new FindCallback<ParseUser>() {
			
			@Override
			public void done(List<ParseUser> users, ParseException e) {

				setProgressBarIndeterminateVisibility(false);
				if(e==null)
				{
					//success
					mUsers = users;
					String[] username = new String[mUsers.size()];
					int i=0;
					for(ParseUser user : mUsers)
					{
						username[i]=user.getUsername();
						i++;
					}
					

					if (mGridView.getAdapter() == null) {

						UserAdapter adapter = new UserAdapter(EditFriendsActivity.this,mUsers);
						mGridView.setAdapter(adapter);
					}
					else{
						((UserAdapter)mGridView.getAdapter()).refill(mUsers);
					}

					addFriendCheckmarks();
				}
				else
				{
					//error
					Log.e(TAG, e.getMessage());
					AlertDialog.Builder builder = new AlertDialog.Builder(EditFriendsActivity.this);
					builder.setMessage(e.getMessage())
					.setTitle(R.string.error_title)
					.setPositiveButton(android.R.string.ok, null);
					
					AlertDialog dialog = builder.create();
					dialog.show();	
				}
			}
		});
		
	}
	
	protected void addFriendCheckmarks()
	{
		mFriendRelation.getQuery().findInBackground(new FindCallback<ParseUser>() {

			@Override
			public void done(List<ParseUser> friends, ParseException e) {
				
				if(e==null)
				{
					for(int i=0;i<mUsers.size();i++)
					{
						ParseUser user = mUsers.get(i);
						for(ParseUser friend : friends)
						{
							if(friend.getObjectId().equals(user.getObjectId()))
							{
							  mGridView.setItemChecked(i, true);
							}
						}
					}	
				}
				else
				{
				    Log.e(TAG, e.getMessage());	
				}
			}
		});
	}
	
	protected OnItemClickListener mOnItemClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
			ImageView checkImageView = (ImageView) view.findViewById(R.id.checkImageView);
			
			
			if(mGridView.isItemChecked(position))
			{
				Log.i(TAG,"add friend");
				//add friend
				mFriendRelation.add(mUsers.get(position));
				checkImageView.setVisibility(View.VISIBLE);
			}
			else
			{
				Log.i(TAG,"remove friend");
				//remove friend
				mFriendRelation.remove(mUsers.get(position));
				checkImageView.setVisibility(View.INVISIBLE);
			}
			
			mCurrentUser.saveInBackground(new SaveCallback() {
				
				@Override
				public void done(ParseException e) {
	              if(e!=null)
	              {
	            	  Log.e(TAG, e.getMessage());
	              }
				}
			});		
		}
		
	};
}
