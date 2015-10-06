package com.akashpal.ribbit;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

public class RibbitApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		Parse.initialize(this, "0r8rs0t3IbIUyQSSS5KWsOwYRy67yuFAmrorWRxG", "dMRnRvt7a0xajLbKfk023kK8YU4SxBOh4FlPt0q0");		
		
		//PushService.setDefaultPushCallback(this,MainActivity.class);
		//ParseInstallation.getCurrentInstallation().saveInBackground();
		
		
	}
	
	public static void updateParseInstallation(ParseUser user){
		Log.d("TEST","updateParseInstallation");
		ParseInstallation parseInstallation =  ParseInstallation.getCurrentInstallation();
		Log.d("TEST",user.getObjectId()+" ");
		parseInstallation.put("userId", user.getObjectId());
	    parseInstallation.saveInBackground();
	}
}
