package info.androidhive.loginandregistration.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class SessionManager {
	// LogCat tag
	private static String TAG = SessionManager.class.getSimpleName();

	// Shared Preferences
	SharedPreferences pref;

	Editor editor;
	Context _context;

	// Shared pref mode
	int PRIVATE_MODE = 0;

	// Shared preferences file name
	private static final String PREF_NAME = "AndroidHiveLogin";
	
	private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

	private static final String USERNAME = "user";
	private static final String GROUP = "group";
	private static final String ID = "id";

	public SessionManager(Context context) {
		this._context = context;
		pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
		editor = pref.edit();
	}

	public void setLogin(boolean isLoggedIn, String user, String group, String id) {

		editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
		editor.putString(USERNAME, user);
		editor.putString(GROUP,group);
		editor.putString(ID,id);
		// commit changes
		editor.commit();

		Log.d(TAG, "User login session modified!");
	}

	public String getUser(){return pref.getString(USERNAME,null);}

	public String getGroup(){
		return pref.getString(GROUP,null);
	}

	public String getId(){
		return pref.getString(ID,null);
	}

	public boolean isLoggedIn(){
		return pref.getBoolean(KEY_IS_LOGGED_IN, false);
	}
}
