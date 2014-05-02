package socialdj.library;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import socialdj.Album;
import socialdj.Artist;
import socialdj.ConnectedSocket;
import socialdj.MessageHandler;
import socialdj.MetaItem;
import socialdj.SendMessage;
import socialdj.config.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
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
	//size of next amount of albums
	private static final int BLOCK_SIZE = 100;
	//starts thread to load next amount of data 
	private static final int LOAD_AHEAD_SIZE = 50;
	private static final int INCREMENT_TOTAL_MINIMUM_SIZE = 100;
	private static final String PROP_TOP_ITEM = "top_list_item";
	
	//Create handler in the thread it should be associated with 
	//in this case the UI thread
	final Handler handler = new Handler();
	ViewHandlerScroll viewHandlerScroll = new ViewHandlerScroll();
	ViewHandlerSearch viewHandlerSearch = new ViewHandlerSearch();


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//Populate list
		adapter = new CustomAlbumAdapter(getActivity(), R.layout.albums_list, new ArrayList<Album>());

		//asynchronously initial list
		GetAlbumTask task = new GetAlbumTask();

		setListAdapter(adapter);
		
		new Thread(viewHandlerScroll).start();

		if(savedInstanceState != null) {
			//Restore last state from top list position
			int listTopPosition = savedInstanceState.getInt(PROP_TOP_ITEM, 0);

			//load elements to get to the top of the list
			task = new GetAlbumTask();
			if(listTopPosition > BLOCK_SIZE) {
				//download asynchronously inital list
				task.execute(new Integer[] {BLOCK_SIZE, listTopPosition + BLOCK_SIZE, listTopPosition});
			}
		}
	}
	
	/**
	 * Search fixed footer for albums.
	 */
	@Override 		
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		//will get weird error if using 3rd party keyboard
		View v = inflater.inflate(R.layout.footer, container, false);
		final EditText searchText = (EditText) v.findViewById(R.id.editText);
		ImageButton searchButton = (ImageButton) v.findViewById(R.id.footerButton);
		searchText.setHint("Enter an Album Name");
	    searchButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	 //ask server for albums not in cache for similar songs
	            //---fulfill meta item requirements
	            MetaItem item = new MetaItem();
	            item.setMetaItem("album");
	            item.setValue(searchText.getText().toString());
	            ArrayList<MetaItem> metaItems = new ArrayList<MetaItem>();
	            metaItems.add(item);
	            String notCountable = "0";
	            
	            //ask server for albums similar to query
	            SendMessage query = new SendMessage();
	            query.prepareMessageListAlbums(metaItems, notCountable, notCountable);
	            new Thread(query).start();
	            
	            //stop handler on uiThread for scrolling
	            viewHandlerScroll.kill();
	            //search cache for any album that contains this substring
	            viewHandlerSearch.setQuery(searchText.getText().toString());
	            new Thread(viewHandlerSearch).start();
	            searchText.setText("");
	            searchText.setHint("Enter an Album Name");
	        }
	    });
	    setHasOptionsMenu(true);  
		return v; 		
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.refresh, menu);
	    super.onCreateOptionsMenu(menu,inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		//refresh button of endless list
		case R.id.action_refresh:
			viewHandlerSearch.kill();
			handler.post(new Runnable() {
				@Override
				public void run() {
					synchronized(MessageHandler.getAlbums()) {
						synchronized(adapter) {
							adapter.clear();
						}
						synchronized(MessageHandler.getAlbums()) {
							//if Handler has 100 albums, display first 100
							if(MessageHandler.getAlbums().size() >= BLOCK_SIZE) {
								synchronized(adapter) {
									for(int i = 0; i < BLOCK_SIZE; i++) {
										if(!adapter.contains(MessageHandler.getAlbums().get(i))) 
											adapter.add(MessageHandler.getAlbums().get(i));
									}
									Collections.sort(adapter.getList());
									adapter.notifyDataSetChanged();
								}
							} 
						}
					}
					new Thread(viewHandlerScroll).start();
				}
			});
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public class ViewHandlerSearch implements Runnable {
		boolean running = true;
		String query;
		
		public void setQuery(String query) {
			this.query = query;
		}
		
		public void run() {
			while(running){
				//clear adapter, add new items, updateview
				//The handler schedules the new runnable on the UI thread
				handler.post(new Runnable() {
					@Override
					public void run() {
						synchronized(MessageHandler.getAlbums()) {
							if(query.toLowerCase().equalsIgnoreCase("")) {
								synchronized(adapter) {
									adapter.clear();
								}
								running = false;
								synchronized(MessageHandler.getAlbums()) {
									//if Handler has 100 albums, display first 100
									if(MessageHandler.getAlbums().size() >= BLOCK_SIZE) {
										System.out.println("INSIDE >= BLOCK_SIZE");
										for(int i = 0; i < BLOCK_SIZE; i++) {
											synchronized(adapter) {
												if(!adapter.contains(MessageHandler.getAlbums().get(i))) {
													adapter.add(MessageHandler.getAlbums().get(i));
													adapter.notifyDataSetChanged();
												}
											}
										}
									} //else display what the database does have
									else {
										for(Album item: MessageHandler.getAlbums()) {
											synchronized(adapter) {
												if(!adapter.contains(item)) {
													adapter.add(item);
													adapter.notifyDataSetChanged();
												}
											}
										}
									}
								}
							} 
							else {
								synchronized(adapter) {
									adapter.clear();
								}
								for(Album item: MessageHandler.getAlbums()) {
									synchronized(adapter) {
										if(item.getAlbumName().toLowerCase().contains(query.toLowerCase())) {
											adapter.add(item);
											adapter.notifyDataSetChanged();
										}
									}
								}
							}
						}
					}
				});
				//Add some downtime to click on button for queue
				try {
					Thread.sleep(1000);
				}catch (InterruptedException e) {e.printStackTrace();}
			}
			running = true;
			new Thread(viewHandlerScroll).start();
		}

		public void kill() {
			running = false;
		}
	}
	
	public class ViewHandlerScroll implements Runnable {
		boolean running = true;

		public void run() {
			int size = adapter.getList().size();
			while(running){
				//Do time consuming listener call
				getListView().setOnScrollListener(new AlbumListScrollListener());

				//The handler schedules the new runnable on the UI thread
				if(size != MessageHandler.getAlbums().size()) {
					size = MessageHandler.getAlbums().size();
					handler.post(new Runnable() {
						@Override
						public void run() {

							synchronized(MessageHandler.getAlbums()) {
								for(Album item: MessageHandler.getAlbums()) {
									synchronized(adapter) {
										if(!adapter.contains(item)) {
											adapter.add(item);
										}
									}
								}
								Collections.sort(adapter.getList());
								adapter.notifyDataSetChanged();
							}
						}
					});
				}
				//Add some downtime
				try {
					Thread.sleep(100);
				}catch (InterruptedException e) {e.printStackTrace();}
			}
			running = true;
		}

		public void kill() {
			running = false;
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
	 * Use to clear thread.  This will ensure getListview will be created everytime and get rid of the
	 * content view not yet being created.
	 */
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		viewHandlerScroll.kill();
		viewHandlerSearch.kill();
	}

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
				GetAlbumTask task = new GetAlbumTask();
				task.execute(new Integer[] {totalItemCount, BLOCK_SIZE});
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState){

		}
	}

	/**
	 * Asynchronous call.  This class is responsible for calling the network for more albums
	 * and managing the isLoading boolean.
	 */
	class GetAlbumTask extends AsyncTask<Integer, Void, List<Album>> {
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
			if(MessageHandler.getAlbums().size() < params[0] + params[1]) {
					SendMessage list = new SendMessage();
					list.prepareMessageListAlbums(new ArrayList<MetaItem>(), Integer.toString(params[0]), Integer.toString(params[1]));
					new Thread(list).start();
			}
			return MessageHandler.getAlbums();
		}

		@Override
		protected void onPostExecute(List<Album> result) {
			/*adapter.setNotifyOnChange(true);
			for(Album item: result) {
				synchronized(adapter) {
					//if(!adapter.contains(item))
					  adapter.add(item);
				}
			}*/

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
			this.items = Collections.synchronizedList(items);
			this.rowViewId = rowViewId;
		}
		
		public List<Album> getList() {
			return items;
		}

		public boolean contains(Album item) {
			synchronized(items) {
				for(Album r: items){
					if(r.getAlbumId().equals(item.getAlbumId())) {
						return true;
					}
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
			synchronized(MessageHandler.getArtists()) {
				for(Artist a: MessageHandler.getArtists()) {
					if(a.getArtistId().equalsIgnoreCase(items.get(position).getArtistId())) {
						artistName.setText(a.getArtistName());
						break;
					}
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
