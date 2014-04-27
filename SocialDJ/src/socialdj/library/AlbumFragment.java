package socialdj.library;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import socialdj.Album;
import socialdj.Artist;
import socialdj.ConnectedSocket;
import socialdj.MessageHandler;
import socialdj.SendMessage;
import socialdj.Song;
import socialdj.config.R;
import socialdj.library.SongFragment.CustomSongAdapter;
import socialdj.library.SongFragment.GetSongTask;
import socialdj.library.SongFragment.SongListScrollListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

/**
 * Fragment for albums.  Show all albums database holds.
 * @author Nathan
 *
 */
public class AlbumFragment extends ListFragment {

	private CustomAlbumAdapter adapter = null;
	//size of what list will be
	private int totalSizeToBe = 0;
	//if the data is still sending
	boolean isLoading = false;
	//size of next amount of songs
	private static final int BLOCK_SIZE = 100;
	//starts thread to load next amount of data 
	private static final int LOAD_AHEAD_SIZE = 50;
	private static final int INCREMENT_TOTAL_MINIMUM_SIZE = 100;
	private static final String PROP_TOP_ITEM = "top_list_item";


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//Populate list
		adapter = new CustomAlbumAdapter(getActivity(), R.layout.albums_list, new ArrayList<Album>());

		//asynchronously initial list
		GetSongTask task = new GetSongTask();

		setListAdapter(adapter);
		getListView().setOnScrollListener(new AlbumListScrollListener());

		if(savedInstanceState != null) {
			//Restore last state from top list position
			int listTopPosition = savedInstanceState.getInt(PROP_TOP_ITEM, 0);

			//load elements to get to the top of the list
			task = new GetSongTask();
			if(listTopPosition > BLOCK_SIZE) {
				//download asynchronously inital list
				task.execute(new Integer[] {BLOCK_SIZE, listTopPosition + BLOCK_SIZE, listTopPosition});
			}
		}
	}

	/*@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		int listPosition = getListView().getFirstVisiblePosition();
		if (listPosition > 0) {
			state.putInt(PROP_TOP_ITEM, listPosition);
		}
	}*/

	/**
	 * Listener which handles the endless list.  It is responsible for
	 * determining when the network calls will be asynchronously.
	 */
	class AlbumListScrollListener implements OnScrollListener {

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			//load more elements if there is LOAD_AHEAD_SIZE left in the list being displayed
			boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount - LOAD_AHEAD_SIZE;

			/*
			 * Only get more results if the list achieves a min size. This is to avoid
			 * that this method is called each time the loadMore is reached and scroll
			 * pressed
			 */
			if(loadMore && totalSizeToBe <= totalItemCount) {
				totalSizeToBe += INCREMENT_TOTAL_MINIMUM_SIZE;
				//calls more elements
				GetSongTask task = new GetSongTask();
				task.execute(new Integer[] {totalItemCount, BLOCK_SIZE});
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState){

		}
	}

	/**
	 * Asynchronous call.  This class is responsible for calling the network for more songs
	 * and managing the isLoading boolean.
	 */
	class GetSongTask extends AsyncTask<Integer, Void, List<Album>> {
		private static final int TOP_ITEM_INDEX = 2;

		//position to scroll list to
		private int listTopPosition = 0;

		@Override 
		public void onPreExecute() {
			isLoading = true;
		}

		@Override
		protected List<Album> doInBackground(Integer... params) {
			List<Album> results = new ArrayList<Album>();

			if(params.length > TOP_ITEM_INDEX)
				listTopPosition = params[TOP_ITEM_INDEX];

			//excute network call
			System.out.println("line 143: " + MessageHandler.getAlbums().size());
			if(MessageHandler.getAlbums().size() < params[0] + params[1]) {
				PrintWriter out = null;
				try {
					out = new PrintWriter(ConnectedSocket.getSocket().getOutputStream());
				} catch (IOException e) {e.printStackTrace();}
				out.write("list_albums|" + params[0] + "|" + params[1] + "\n");
				out.flush();
				try {
					System.out.println(MessageHandler.getAlbums().size());
					System.out.println(params[1]);
					int start = MessageHandler.getAlbums().size();
					int end = start + params[1];
					while(start < end) {
						start = MessageHandler.getAlbums().size();
						Thread.sleep(10);
					}
					System.out.println(MessageHandler.getAlbums().size());
				} catch (InterruptedException e) {e.printStackTrace();}
			}
			
			synchronized(MessageHandler.getAlbums()) {
				for(int i = params[0]; i < ((params[0] + params[1])); i++) 
					results.add(MessageHandler.getAlbums().get(i));
			}
			//return MessageHandler.getSongs();
			System.out.println("results.size(): " + results.size());
			return results;
		}

		@Override
		protected void onPostExecute(List<Album> result) {
			adapter.setNotifyOnChange(true);
			for(Album item: result) {
				synchronized(adapter) {
					//if(!adapter.contains(item))
					  adapter.add(item);
				}
			}
			System.out.println("adapter count: " + adapter.getCount());

			//loading is done
			isLoading = false;

			//update top list item
			if(listTopPosition > 0) {
				getListView().setSelection(listTopPosition);
			}
		}
	}

	/**
	 * Adapter which handles the list to be displayed
	 * @author Nathan
	 *
	 */
	class CustomAlbumAdapter extends ArrayAdapter<Album> {
		private final Activity context;
		private final List<Album> items;
		private final int rowViewId;

		public CustomAlbumAdapter(Activity context, int rowViewId, List<Album> items) {
			super(context, rowViewId, items);
			this.context = context;
			this.items = items;
			this.rowViewId = rowViewId;
		}

		public boolean contains(Album item) {
			for(Album r: items){
				if(r.getAlbumId().equals(item.getAlbumId())) {
					return true;
				}
			}
			return false;
		}

		public Album getItemAt(int index) {
			return items.get(index);
		}

		@Override 
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView albumName;
			TextView artistName;

			View rowView = convertView;

			/*
			 * Inflate row using the layout.
			 */
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(rowViewId, null, true);
			albumName = (TextView) rowView.findViewById(R.id.albumName);
			artistName =  (TextView) rowView.findViewById(R.id.artistName);

			albumName.setText(items.get(position).getAlbumName());
			
			//search artists for artist name associated with album
			for(Artist a: MessageHandler.getArtists()) {
				if(a.getArtistId().equalsIgnoreCase(items.get(position).getArtistId())) {
					artistName.setText(a.getArtistName());
					break;
				}
			}

			return rowView;
		}

	}
	
	/**
	 * Method that listens for clicks on an item in the listview. 
	 */
	@Override
	public void onListItemClick (ListView l, View v, int position, long id){	
		//get ids of songs in album and save
		Set<String> set = new HashSet<String>();
		set.addAll(((Album) l.getItemAtPosition(position)).getSongs());
		SharedPreferences settings = this.getActivity().getSharedPreferences("songsInAlbum", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putStringSet("songsInAlbum", set);
		editor.commit();
		
		//save album name to be used in song activity
		settings = this.getActivity().getSharedPreferences("albumName", Context.MODE_PRIVATE);
		editor = settings.edit();
		editor.putString("albumName", (((Album)l.getItemAtPosition(position)).getAlbumName()).trim());
		editor.commit();
		
		//save artist name to be used in song activity
		settings = this.getActivity().getSharedPreferences("artistName", Context.MODE_PRIVATE);
		editor = settings.edit();
		for(Artist a: MessageHandler.getArtists()) {
			if(a.getArtistId().equalsIgnoreCase((((Album)l.getItemAtPosition(position)).getArtistId()).trim())) {
				editor.putString("artistName", a.getArtistName());
				break;
			}
		}
		editor.commit();
		
		//start activity to display songs within album
		Intent intent = new Intent(this.getActivity().getApplicationContext(), SongActivity.class);
    	startActivity(intent);
	}
}
