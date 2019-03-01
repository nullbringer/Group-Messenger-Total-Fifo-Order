package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String PREFERENCE_FILE = "groupMsg2PreferenceFile";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(values.getAsString(KEY_FIELD), values.getAsString(VALUE_FIELD));
        editor.commit();

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        Log.v("query", selection);

        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);

        String value = sharedPref.getString(selection, null);




        MatrixCursor cursor = new MatrixCursor(
                new String[] {KEY_FIELD, VALUE_FIELD}
        );

        cursor.newRow()
                .add(KEY_FIELD, selection)
                .add(VALUE_FIELD, value);




        return cursor;
    }
}
