package com.akashpal.ribbit.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akashpal.ribbit.FileHelper;
import com.akashpal.ribbit.ParseConstants;
import com.akashpal.ribbit.R;
import com.akashpal.ribbit.adapter.UserAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class RecipientActivity extends Activity {
	
	public static final String TAG = RecipientActivity.class.getSimpleName(); 
	protected List<ParseUser> mFriends;
	protected ParseRelation<ParseUser> mFriendRelation;
	protected ParseUser mCurrentUser;
	protected GridView mGridView;
	protected MenuItem mSendMenuItem;
	protected Uri mMediaUri;
	protected String mFileType;
	private String mExtension;

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
	
		mMediaUri = getIntent().getData();
		mFileType = getIntent().getExtras().getString(ParseConstants.KEY_FILE_TYPE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recepient, menu);
		mSendMenuItem = menu.getItem(0);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_send) {
			ParseObject message = createMessage();
			if(message == null)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.error_creating_message);
				builder.setTitle(R.string.error_label_dialog);
				builder.setPositiveButton(android.R.string.ok,null);
				AlertDialog dialog = builder.create();
				dialog.show();
			}
			else
			{
				send(message);	
				finish();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	protected ParseObject createMessage()
	{
		ParseObject message = new ParseObject(ParseConstants.CLASS_MESSAGE);
		message.put(ParseConstants.KEY_SENDER_ID,ParseUser.getCurrentUser().getObjectId());
		message.put(ParseConstants.KEY_SENDER_NAME,ParseUser.getCurrentUser().getUsername());
		message.put(ParseConstants.KEY_RECIPIENT_IDS,getRecipientsIds());
		message.put(ParseConstants.KEY_FILE_TYPE, mFileType);
		
		byte [] fileBytes =  FileHelper.getByteArrayFromFile(this,mMediaUri);
		Log.d("SKY_FILE SIZE",fileBytes+"");
		if(fileBytes == null)
		{
			return null;
		}
		else
		{
			if(mFileType.equals(ParseConstants.TYPE_IMAGE))
			{
				mExtension = FilenameUtils.getExtension(mMediaUri.toString());
				fileBytes = FileHelper.reduceImageForUpload(fileBytes,mExtension);
				Log.d("SKY_FILE SIZE",fileBytes+"");
			}
			
			String fileName = FileHelper.getFileName(this,mMediaUri,mFileType);
			
			ParseFile file = new ParseFile(fileName, fileBytes);
			message.put(ParseConstants.KEY_FILE, file);
		}
		
		return message;
		
	}
	

	protected ArrayList<String> getRecipientsIds()
	{
		ArrayList<String> recipientIds = new ArrayList<String>();
	    for(int i=0;i< mGridView.getCount() ; i++)
	    {
	       if(mGridView.isItemChecked(i))
	       {
	    	   recipientIds.add(mFriends.get(i).getObjectId());
	       }
	    }
		return recipientIds; 
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		setProgressBarIndeterminateVisibility(true);
		
		mCurrentUser = ParseUser.getCurrentUser();
		mFriendRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);
		
		ParseQuery<ParseUser> query = mFriendRelation.getQuery();
		query.addAscendingOrder(ParseConstants.KEY_USERNAME);
		query.setLimit(1000);
		query.findInBackground(new FindCallback<ParseUser>() {
			
			@Override
			public void done(List<ParseUser> friends, ParseException e) {

				setProgressBarIndeterminateVisibility(false);
				if(e==null)
				{
					//success
					mFriends = friends;
					String[] username = new String[mFriends.size()];
					int i=0;
					for(ParseUser user : mFriends)
					{
						username[i]=user.getUsername();
						i++;
					}
			

					if (mGridView.getAdapter() == null) {

						UserAdapter adapter = new UserAdapter(RecipientActivity.this,mFriends);
						mGridView.setAdapter(adapter);
					}
					else{
						((UserAdapter)mGridView.getAdapter()).refill(mFriends);
					}

				}
				else
				{
					//error
					Log.e(TAG, e.getMessage());
					AlertDialog.Builder builder = new AlertDialog.Builder(mGridView.getContext());
					builder.setMessage(e.getMessage())
					.setTitle(R.string.error_title)
					.setPositiveButton(android.R.string.ok, null);
					
					AlertDialog dialog = builder.create();
					dialog.show();	
				}
			}
		});
		
	}
	
	private void send(ParseObject message) {
		message.saveInBackground(new SaveCallback() {
			
			@Override
			public void done(ParseException e) {
				if(e==null)
				{
					//success
					Toast.makeText(RecipientActivity.this, R.string.message_sent, Toast.LENGTH_LONG).show();
	                sendPushNotifications();			
				}
				else
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(RecipientActivity.this);
					builder.setMessage(R.string.error_sending_message);
					builder.setTitle(R.string.error_label_dialog);
					builder.setPositiveButton(android.R.string.ok,null);
					AlertDialog dialog = builder.create();
					dialog.show();
				}
				
			}
		});
	}
	
	
	
	protected OnItemClickListener mOnItemClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
			ImageView checkImageView = (ImageView) view.findViewById(R.id.checkImageView);
			

			if(mGridView.getCheckedItemCount()>0)
			{
			   mSendMenuItem.setVisible(true);
			}
			else
			{
			   mSendMenuItem.setVisible(false);
			}
			
			if(mGridView.isItemChecked(position))
			{
				Log.i(TAG,"add friend");
				//add recipient
				checkImageView.setVisibility(View.VISIBLE);
			}
			else
			{
				Log.i(TAG,"remove friend");
				//remove recipient
				checkImageView.setVisibility(View.INVISIBLE);
			}
					
		}
		
	};
	
	protected void sendPushNotifications(){
           ParseQuery<ParseInstallation> pushQuery = ParseInstallation.getQuery();
           pushQuery.whereContainedIn("userId", getRecipientsIds());
           Log.d("RECIPINT ID",getRecipientsIds()+" ");
           //send push notification
           ParsePush push = new ParsePush();
           push.setQuery(pushQuery);
           push.setMessage(getString(R.string.pushMessage,ParseUser.getCurrentUser().getUsername()));
           push.sendInBackground();
	}

}
