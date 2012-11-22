/*******************************************************************************
 * Gaggle is Copyright 2010 by Geeksville Industries LLC, a California limited liability corporation. 
 * 
 * Gaggle is distributed under a dual license.  We've chosen this approach because within Gaggle we've used a number
 * of components that Geeksville Industries LLC might reuse for commercial products.  Gaggle can be distributed under
 * either of the two licenses listed below.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. 
 * 
 * Commercial Distribution License
 * If you would like to distribute Gaggle (or portions thereof) under a license other than 
 * the "GNU General Public License, version 2", contact Geeksville Industries.  Geeksville Industries reserves
 * the right to release Gaggle source code under a commercial license of its choice.
 * 
 * GNU Public License, version 2
 * All other distribution of Gaggle must conform to the terms of the GNU Public License, version 2.  The full
 * text of this license is included in the Gaggle source, see assets/manual/gpl-2.0.txt.
 ******************************************************************************/
package com.geeksville.android;

import java.io.File;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.geeksville.gaggle.GagglePrefs;
import com.geeksville.gaggle.R;
import com.geeksville.location.CSVReader;
import com.geeksville.location.ExtendedLocation;
import com.geeksville.location.LocationDBWriter;
import com.geeksville.location.LocationLogDbAdapter;
import com.geeksville.location.PositionWriter;
import com.geeksville.view.AsyncProgressDialog;

public abstract class DBListActivity extends ListActivity {

	protected Cursor myCursor;

	// / Should the user be shown a confirming dialog
	protected Boolean isConfirmDeletes = true;

	BaseAdapter adapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myCursor = createCursor();
		startManagingCursor(myCursor);

		adapter = createListAdapter();
		setListAdapter(adapter);

		// Turn on the context menu
		registerForContextMenu(getListView());
	}

	/**
	 * Collect app metrics on Flurry
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
	    GagglePrefs prefs = new GagglePrefs(this);
	    if (prefs.isFlurryEnabled())
	    	FlurryAgent.onStartSession(this, "XBPNNCR4T72PEBX17GKF");
	}

	/**
	 * Collect app metrics on Flurry
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
	    GagglePrefs prefs = new GagglePrefs(this);
	    if (prefs.isFlurryEnabled())
	    	FlurryAgent.onEndSession(this);
	}

	private void doDelete(MenuItem item) {
		if (handleDeleteItem(item)) {
			myCursor.requery(); // We just deleted a
								// row, it seems we need
			// to manually refetch the cursor

			Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
		}

		// adapter.notifyDataSetChanged(); // this
		// doesn't seem to do
		// anything
	}

	private void confirmDelete(final MenuItem item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.confirm_delete_)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								doDelete(item);
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Handle our context menu
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.delete_menu) {
			if (isConfirmDeletes)
				confirmDelete(item);
			else
				doDelete(item);
			return true;
		} else if (itemId == R.id.view_menu) {
			handleViewItem(item);
			return true;
		} else {
		}

		return super.onContextItemSelected(item);
	}

	/**
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 *      android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		getMenuInflater().inflate(R.menu.db_list_activity_context, menu);
	}

	/**
	 * Create our options menu
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// FIXME - our menus are not yet ready
		return super.onCreateOptionsMenu(menu);

		// getMenuInflater().inflate(R.menu.logged_flight_optionmenu, menu);
		// return true;
	}

	/**
	 * Given a row num, return the db id for that row
	 * 
	 * @param rowNum
	 * @return
	 * 
	 */
	protected long rowToRowId(int rowNum) {
		myCursor.moveToPosition(rowNum);

		long flightid = myCursor.getLong(myCursor
				.getColumnIndexOrThrow(LocationLogDbAdapter.KEY_ROWID));

		return flightid;
	}

	/**
	 * Find the row number for the selected context menu item
	 * 
	 * @param item
	 * @return
	 */
	protected int itemToRowNum(MenuItem item) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();

		return info.position;

	}

	/**
	 * Find the flight id for the selected context menu item
	 * 
	 * @param item
	 * @return
	 */
	protected long itemToRowId(MenuItem item) {

		return rowToRowId(itemToRowNum(item));
	}

	/**
	 * Generate a cursor with data for this view
	 * 
	 * @return
	 */
	protected abstract Cursor createCursor();

	/**
	 * Generate the adapter for viewing our cursor
	 * 
	 * @return
	 */
	protected abstract BaseAdapter createListAdapter();

	/**
	 * Called when the user wants to delete a row
	 * 
	 * @param item
	 * @return true if we did the deletion and should show the user a Toast
	 */
	protected abstract boolean handleDeleteItem(MenuItem item);

	/**
	 * Called when the user wants to view a row
	 * 
	 * @param item
	 */
	protected abstract void handleViewItem(MenuItem item);
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (super.onOptionsItemSelected(item)) {
			return true;
		}

		if (false){//item.getItemId() == R.id.import_flight_menu) {
			// just a dummy, introduce fileselector here later:
			AsyncFileReader reader = new AsyncFileReader("hoi.csv", "csv");
			reader.execute();
			return true;
		}
		return false;
	}

	private class AsyncFileReader extends AsyncProgressDialog {
		String filetype;

		public AsyncFileReader(String filename, final String filetype) {
			super(DBListActivity.this, getString(R.string.reading_file),
				getString(R.string.please_wait));

			this.filetype = filetype;
		}

		@Override
		protected void doInBackground() {

			final PositionWriter dbwriter =
				new LocationDBWriter(DBListActivity.this, false, "Hansie", "new imported track");
			File file =
				new File(Environment.getExternalStorageDirectory() + "/Gaggle/TrackLogs/hoi.csv");

			CSVReader iread = new CSVReader(file);
			List<ExtendedLocation> loclist = iread.toLocationList();
			dbwriter.emitProlog();
			for (ExtendedLocation location : loclist) {
				dbwriter.emitPosition(location.getTime(), location.getLatitude(),
					location.getLongitude(), (float) location.getAltitude(),
					(int) location.getBearing(), location.getSpeed(), location.getAccel(),
					location.getVertSpeed());
			}
			dbwriter.emitEpilog();

			// adapter = createListAdapter();
			// setListAdapter(adapter);
		}

		@Override
		protected void onPostExecute(Void unused) {
			// TODO Auto-generated method stub
			super.onPostExecute(unused);
			// adapter.notifyDataSetInvalidated();

			// bit plump way to update, but it works:
			myCursor.close();
			myCursor = createCursor();
			startManagingCursor(myCursor);
			adapter = createListAdapter();
			setListAdapter(adapter);
			onContentChanged();

		}
	}

}
