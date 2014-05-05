package socialdj.library;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import socialdj.Album;
import socialdj.Artist;
import socialdj.ConnectedSocket;
import socialdj.MessageHandler;
import socialdj.MetaItem;
import socialdj.SendMessage;
import socialdj.Song;
import socialdj.config.R;
import socialdj.library.AlbumFragment.AlbumListScrollListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment for songs.  Show all songs database holds and search.
 * @author Nathan
 *
 */
public class SongFragment extends ListFragment {

	private CustomSongAdapter adapter = null;
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

	//Create handler in the thread it should be associated with 
	//in this case the UI thread
	Handler handler = new Handler();
	ViewHandlerScroll viewHandlerScroll = new ViewHandlerScroll();
	ViewHandlerSearch viewHandlerSearch = new ViewHandlerSearch();

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//Populate list
		adapter = new CustomSongAdapter(getActivity(), R.layout.songs_list, new ArrayList<Song>());
	    
	    setListAdapter(adapter);

		//asynchronously initial list
		GetSongTask task = new GetSongTask();
		//task.execute(new Integer[] {0, BLOCK_SIZE});
		
		new Thread(viewHandlerScroll).start();

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
	
	/**
	 * Search fixed footer for songs.
	 */
	@Override 		
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		//will get weird error if using 3rd party keyboard
		View v = inflater.inflate(R.layout.footer, container, false);
		final EditText searchText = (EditText) v.findViewById(R.id.editText);
		ImageButton searchButton = (ImageButton) v.findViewById(R.id.footerButton);
	    searchButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            //ask server for songs not in cache for similar songs
	            //---fulfill meta item requirements
	            MetaItem item = new MetaItem();
	            item.setMetaItem("title");
	            item.setValue(searchText.getText().toString());
	            ArrayList<MetaItem> metaItems = new ArrayList<MetaItem>();
	            metaItems.add(item);
	            String notCountable = "0";
	            
	            //ask server for songs similar to query
	            SendMessage query = new SendMessage();
	            query.prepareMessageListSongs(metaItems, notCountable, notCountable);
	            new Thread(query).start();
	            
	            //stop handler on uiThread for scrolling
	            viewHandlerScroll.kill();
	            //search cache for any song that contains this substring
	            viewHandlerSearch.setQuery(searchText.getText().toString());
	            new Thread(viewHandlerSearch).start();
	            searchText.setText("");
	            searchText.setHint("Enter a Song Name");
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
					synchronized(MessageHandler.getSongs()) {
						synchronized(adapter) {
							adapter.clear();
						}
						synchronized(MessageHandler.getSongs()) {
							for(Song item: MessageHandler.getSongs()) {
								synchronized(adapter) {
									if(!adapter.contains(item)) {
										adapter.add(item);
									}
								}
							}
							synchronized(adapter) {
								Collections.sort(adapter.getList());
								adapter.notifyDataSetChanged();
							}
						}
					}
				}
			});
			return true;
		}
		return super.onOptionsItemSelected(item);
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
	class SongListScrollListener implements OnScrollListener {

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
			/*System.out.println("loadMore: " + loadMore);
			System.out.println("firstVisibleItem: " + firstVisibleItem);
			System.out.println("visibleItemCount: " + visibleItemCount);
			System.out.println("totalSizeToBe: " + totalSizeToBe);
			System.out.println("totalItemCount: " + totalItemCount);*/
			
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
						synchronized(MessageHandler.getSongs()) {
							if(query.toLowerCase().equalsIgnoreCase("")) {
								synchronized(adapter) {
									adapter.clear();
								}
								running = false;
								synchronized(MessageHandler.getSongs()) {
									for(Song item: MessageHandler.getSongs()) {
										synchronized(adapter) {
											if(!adapter.contains(item)) 
												adapter.add(item);
										}
									}
									synchronized(adapter) {
										Collections.sort(adapter.getList());
										adapter.notifyDataSetChanged();
									}
								}
							} 
							else {
								synchronized(adapter) {
									adapter.clear();
								}
								for(Song item: MessageHandler.getSongs()) {
									synchronized(adapter) {
										if(item.getSongTitle().toLowerCase().contains(query.toLowerCase())) 
											adapter.add(item);
									}
								}
								synchronized(adapter) {
									removeSong();
									adapter.notifyDataSetChanged();
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
	
	public void removeSong() {
		synchronized(adapter) {
			for(String t: MessageHandler.getForgetSongList()) {
				for(Song s: adapter.getList()) {
					if(s.getSongId().equalsIgnoreCase(t)) {
						adapter.remove(s);
						//totalSizeToBe -= 1;
						MessageHandler.getForgetSongList().remove(t);
						break;
					}
				}
			}
		}
	}
	
	public class ViewHandlerScroll implements Runnable {
		boolean running = true;
		
		public void run() {
			int size = adapter.getList().size();
			while(running){
				//Do time consuming listener call
				getListView().setOnScrollListener(new SongListScrollListener());

				//The handler schedules the new runnable on the UI thread
				if(size != MessageHandler.getSongs().size()) {
					size = MessageHandler.getSongs().size();
					handler.post(new Runnable() {
						@Override
						public void run() {
							synchronized(MessageHandler.getSongs()) {
								for(Song item: MessageHandler.getSongs()) {
									synchronized(adapter) {
										if(!adapter.contains(item)) {
											adapter.add(item);
										} 
									}
								}
								Collections.sort(adapter.getList());
							}

							//Forget any song in the queue forget song list
							synchronized(adapter) {
								for(String t: MessageHandler.getForgetSongList()) {
									for(Song s: adapter.getList()) {
										if(s.getSongId().equalsIgnoreCase(t)) {
											adapter.remove(s);
											totalSizeToBe -= 1;
											MessageHandler.getForgetSongList().remove(t);
											break;
										}
									}
								}
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

	/**
	 * Asynchronous call.  This class is responsible for calling the network for more songs
	 * and managing the isLoading boolean.
	 */
	class GetSongTask extends AsyncTask<Integer, Void, List<Song>> {
		private static final int TOP_ITEM_INDEX = 2;

		//position to scroll list to
		private int listTopPosition = 0;

		@Override 
		public void onPreExecute() {
			isLoading = true;
		}

		@Override
		protected List<Song> doInBackground(Integer... params) {
			if(params.length > TOP_ITEM_INDEX)
				listTopPosition = params[TOP_ITEM_INDEX];

			//excute network call
			if(MessageHandler.getSongs().size() < params[0] + params[1]) {
				SendMessage list = new SendMessage();
				list.prepareMessageListSongs(new ArrayList<MetaItem>(), Integer.toString(params[0]), Integer.toString(params[1]));
				new Thread(list).start();
			}
			return MessageHandler.getSongs();
		}

		@Override
		protected void onPostExecute(List<Song> result) {
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
	class CustomSongAdapter extends ArrayAdapter<Song> {
		private final Activity context;
		private final List<Song> items;
		private final int rowViewId;

		public CustomSongAdapter(Activity context, int rowViewId, List<Song> items) {
			super(context, rowViewId, items);
			this.context = context;
			this.items = Collections.synchronizedList(items);
			this.rowViewId = rowViewId;
		}

		public boolean contains(Song item) {
			synchronized(items) {
				for(Song r: items){
					if(r.getSongId().equals(item.getSongId())) {
						return true;
					}
				}
				return false;
			}
		}
							
		public List<Song> getList() {
				return items;
		}

		public Song getItemAt(int index) {
			return items.get(index);
		}

		@Override 
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView songTitle;
			TextView artistName;
			TextView songDuration;
			Button addQButton;

			View rowView = convertView;

			/*
			 * Inflate row using the layout.
			 */
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(rowViewId, null, true);
			songTitle = (TextView) rowView.findViewById(R.id.songTitle);
			artistName =  (TextView) rowView.findViewById(R.id.artistName);
			songDuration = (TextView) rowView.findViewById(R.id.songDuration);
			addQButton = (Button) rowView.findViewById(R.id.AddQButton);
			
			//get string length for display
			if(items.get(position).getSongTitle().length() > 35) 
				songTitle.setText(items.get(position).getSongTitle().substring(0,34) + "...");
			else 
				songTitle.setText(items.get(position).getSongTitle());
			
			if(items.get(position).getArtistName().length() > 35)
				artistName.setText(items.get(position).getArtistName().substring(0,34) + "..."); 
			else
				artistName.setText(items.get(position).getArtistName());
			songDuration.setText(items.get(position).getSongDuration());

			final int currentlyClicked = position;
			addQButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					SendMessage message = new SendMessage();
					message.prepareMessageAddSong(getItem(currentlyClicked).getSongId());
					new Thread(message).start();
					
					//display nice message for that your request for song has been sent to the server
					Toast.makeText(getActivity(), "Song request sent", Toast.LENGTH_SHORT).show();
				} 
			});

			return rowView;
		}

	}
}
