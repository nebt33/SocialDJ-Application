package socialdj.library;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import socialdj.Album;
import socialdj.Artist;
import socialdj.ConnectedSocket;
import socialdj.MessageHandler;
import socialdj.MetaItem;
import socialdj.SendMessage;
import socialdj.Song;
import socialdj.config.R;
import socialdj.library.SongFragment.SongListScrollListener;
import socialdj.library.SongFragment.ViewHandlerScroll;
import socialdj.library.SongFragment.ViewHandlerSearch;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageButton;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

/**
 * Fragment for artists.  Show all artists database holds.
 * @author Nathan
 *
 */
public class ArtistFragment extends Fragment implements OnChildClickListener, OnGroupClickListener {

	//private CustomAlbumAdapter adapter = null;
	private CustomExpandableArtistListAdapter adapter = null;
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
	private ExpandableListView elv = null;
	
	Handler handler = new Handler();
	ViewHandlerScroll viewHandlerScroll = new ViewHandlerScroll();
	ViewHandlerSearch viewHandlerSearch = new ViewHandlerSearch();
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		final WifiManager manager = (WifiManager) super.getActivity().getSystemService(getActivity().WIFI_SERVICE);
		//check if wifi is enabled
		if(!manager.isWifiEnabled()) {
			Toast.makeText(getActivity(), "Please enable Wifi and refresh page", Toast.LENGTH_SHORT).show();
		}
		//make options menu visible
	    setHasOptionsMenu(true);  
	    
		//hides the keyboard on start up
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	    
        View v = inflater.inflate(R.layout.artist_main, null);
        elv = (ExpandableListView) v.findViewById(R.id.lvExp);
        
        adapter = new CustomExpandableArtistListAdapter(getActivity(), new ArrayList<Artist>(), new HashMap<Artist,List<Album>>());

		elv.setAdapter(adapter);

		new Thread(viewHandlerScroll,"viewHandlerScroll").start();
        
        //footer
	    final EditText searchText = (EditText) v.findViewById(R.id.editText);
		ImageButton searchButton = (ImageButton) v.findViewById(R.id.footerButton);
		searchText.setHint("Enter an Artist Name");

	    searchButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            //ask server for songs not in cache for similar songs
	            //---fulfill meta item requirements
	            String notCountable = "0";
	            
	            //ask server for songs similar to query
	            SendMessage query = new SendMessage();
	            query.prepareMesssageListArtists(searchText.getText().toString(), notCountable, notCountable);
	            new Thread(query).start();
	            
	            //stop handler on uiThread for scrolling
	            viewHandlerScroll.kill();
	            //search cache for any song that contains this substring
	            viewHandlerSearch.setQuery(searchText.getText().toString());
	            new Thread(viewHandlerSearch,"viewHandlerSearch").start();
	            searchText.setText("");
	            searchText.setHint("Enter a Artist Name");
	            InputMethodManager inputManager = (InputMethodManager)
	            		getActivity().getSystemService(Context.INPUT_METHOD_SERVICE); 

	            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
	            		InputMethodManager.HIDE_NOT_ALWAYS);
	        }
	    });
	    
	    elv.setOnGroupClickListener(this);
	    elv.setOnChildClickListener(this);
        return v;
    }
	
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
						synchronized(MessageHandler.getArtists()) {
							if(query.toLowerCase().equalsIgnoreCase("")) {
								synchronized(adapter) {
									adapter.listArtists.clear();
									adapter.listAlbums.clear();
								}
								running = false;
								synchronized(MessageHandler.getArtists()) {
									for(Artist item: MessageHandler.getArtists()) {
										synchronized(adapter) {
											if(!adapter.listArtists.contains(item)) {
												adapter.add(item);
												//adapter.notifyDataSetChanged();
											}
										}
									}
									synchronized(adapter) {
										Collections.sort(adapter.getArtistList());
										adapter.notifyDataSetChanged();
									}
								}
							} 
							else {
								synchronized(adapter) {
									adapter.listArtists.clear();
									adapter.listAlbums.clear();
								}
								for(Artist item: MessageHandler.getArtists()) {
									synchronized(adapter) {
										if(item.getArtistName().toLowerCase().contains(query.toLowerCase())) {
											adapter.add(item);
										}
									}
								}
								synchronized(adapter) {
									//removeSong();
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
			new Thread(viewHandlerScroll, "viewHandlerScroll").start();
		}

		public void kill() {
			running = false;
		}
	}
	
	public class ViewHandlerScroll implements Runnable {
		boolean running = true;

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			int artistSize = 0;
			int albumSize = 0;
			synchronized(adapter) {
				artistSize = adapter.getArtistList().size();
				//count up albums
				for (Artist key : adapter.getAlbumsList().keySet()) {
					List<Album> value = adapter.getAlbumsList().get(key);
					if (value != null) 
						for (Album element : value) {
							if(!element.getAlbumId().equalsIgnoreCase("-1")) {
								if (element != null) 
									albumSize++;
							}
						}
				}
			}

			while(running){
				//Do time consuming listener call
				elv.setOnScrollListener(new ArtistListScrollListener());

				//The handler schedules the new runnable on the UI thread
				if(artistSize != MessageHandler.getArtists().size()) {
					//update artistSize
					artistSize = MessageHandler.getArtists().size();

					handler.post(new Runnable() {
						@Override
						public void run() {
							synchronized(MessageHandler.getArtists()) {
								for(Artist item: MessageHandler.getArtists()) {
									synchronized(adapter) {
										if(!adapter.contains(item)) {
											adapter.add(item);
										} 
									}
								}
								Collections.sort(adapter.getArtistList());
							}
							adapter.notifyDataSetChanged();	

							//Forget any song in the queue forget song list
							/*synchronized(adapter) {
								for(String t: MessageHandler.getForgetSongList()) {
									for(Song s: adapter.getList()) {
										if(s.getSongId().equalsIgnoreCase(t)) {
											adapter.remove(s);
											totalSizeToBe -= 1;
											System.out.println("INSIDE REMOVE");
											MessageHandler.getForgetSongList().remove(t);
											break;
										}
									}
								}
								adapter.notifyDataSetChanged();		
							}*/
						}
					});
				} else if(albumSize != MessageHandler.getAlbums().size()) {


					//update albumSize
					int albumSizeReset = 0;
					for (Artist key : adapter.getAlbumsList().keySet()) {
						List<Album> value = adapter.getAlbumsList().get(key);
						if (value != null) 
							for (Album element : value) {
								if(!element.getAlbumId().equalsIgnoreCase("-1")) {
									if (element != null) 
										albumSizeReset++;
								}
							}
					}
					albumSize = albumSizeReset;

					//add albums to request group parent called
					synchronized(adapter) {
						for (Artist key : adapter.getAlbumsList().keySet()) {
							// gets the value
							List<Album> albumsInCacheNotInUI = new ArrayList<Album>();
							synchronized(MessageHandler.getAlbums()) {
								//for each album in cache
								for(Album album: MessageHandler.getAlbums()) {
									//if the album is by the parent artist
									if(album.getArtistId().equalsIgnoreCase(key.getArtistId())) {
										//if the album by parent isn't in the UI yet, add it
										if(!adapter.getAlbumsList().get(key).contains(album)) {
											albumsInCacheNotInUI.add(album);
											albumSize++;
										}
									}		
								}
							}

							List<Album> albumsForArtistInUI  = adapter.getAlbumsList().get(key);
							albumsForArtistInUI.addAll(albumsInCacheNotInUI);
							
							//create all albums child
							boolean checkChild = false;
							int allAlbumsPosition = -1;
							/*if(albumsForArtistInUI.size() > 1) {	
								for(Album a: albumsForArtistInUI) {
									//there exists an all_albums album
									if(a.getAlbumId().equalsIgnoreCase("-1")) {
										checkChild = true;
										break;
									}
								}
							}*/
							
							if(albumsForArtistInUI.size() > 1) {	
								for(int i = 0; i < albumsForArtistInUI.size(); i++) {
									//there exists an all_albums album
									if(albumsForArtistInUI.get(i).getAlbumId().equalsIgnoreCase("-1")) {
										checkChild = true;
										allAlbumsPosition = i;
										break;
									}
								}
								
								//modify allAllbums
								if(allAlbumsPosition != -1) {
								  albumsForArtistInUI.get(allAlbumsPosition).getSongs().clear();
								  
								  Album allAlbums = new Album("-1");
								  allAlbums.setAlbumName("All Albums");
								  allAlbums.setArtistId(key.getArtistId());
								  
								  for(int i = 1; i < albumsForArtistInUI.size(); i++) 
										for(int j = 0; j < albumsForArtistInUI.get(i).getSongs().size(); j++)
										  allAlbums.addSong(albumsForArtistInUI.get(i).getSongs().get(j));
								  
								  //add all songs to allAlbums
								  albumsForArtistInUI.get(allAlbumsPosition).getSongs().addAll(allAlbums.getSongs());
								  synchronized(adapter) {
										Collections.sort(adapter.getAlbumsList().get(key));
									}
								  //so all-albums is shown on top
								 // albumsForArtistInUI.remove(allAlbumsPosition);
								 // albumsForArtistInUI.add(0,allAlbums);
								}
								else {
									Album allAlbums = new Album("-1");
									for(Album a: albumsForArtistInUI) {
										allAlbums.setAlbumName("All Albums");
										allAlbums.setArtistId(key.getArtistId());
										for(int i = 0; i < a.getSongs().size(); i++) 
											allAlbums.addSong(a.getSongs().get(i));
									}
									synchronized(adapter) {
										Collections.sort(adapter.getAlbumsList().get(key));
									}
									adapter.getAlbumsList().get(key).add(0,allAlbums);
								}
							}
							
							
							/*if(checkChild) {
								Album allAlbums = new Album("-1");
								allAlbums.setAlbumName("All Albums");
								allAlbums.setArtistId(key.getArtistId());
								for(int i = 1; i < albumsForArtistInUI.size(); i++) 
									for(int j = 0; j < albumsForArtistInUI.get(i).getSongs().size(); j++)
									  allAlbums.addSong(albumsForArtistInUI.get(i).getSongs().get(j));
								
								//sort before adding all_albums so on top
								synchronized(adapter) {
									Collections.sort(adapter.getAlbumsList().get(key));
								}
								adapter.getAlbumsList().get(key).set(0,allAlbums);
							}
							else {
								Album allAlbums = new Album("-1");
								for(Album a: albumsForArtistInUI) {
									allAlbums.setAlbumName("All Albums");
									allAlbums.setArtistId(key.getArtistId());
									for(int i = 0; i < a.getSongs().size(); i++) 
										allAlbums.addSong(a.getSongs().get(i));
								}
								//sort before adding all_albums album, so on top
								synchronized(adapter) {
									Collections.sort(adapter.getAlbumsList().get(key));
								}
								adapter.getAlbumsList().get(key).add(0,allAlbums);
							}*/
						}
					}
					synchronized(adapter) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								adapter.notifyDataSetChanged();
							}
						});
					}




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
					synchronized(MessageHandler.getArtists()) {
						synchronized(adapter) {
							adapter.listArtists.clear();
							adapter.listAlbums.clear();
						}
						synchronized(MessageHandler.getArtists()) {
							for(Artist item: MessageHandler.getArtists()) {
								synchronized(adapter) {
									if(!adapter.contains(item)) {
										adapter.add(item);
									}
								}
							}
							synchronized(adapter) {
								Collections.sort(adapter.getArtistList());
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
	
	/**
	 * Listener which handles the endless list.  It is responsible for
	 * determining when the network calls will be asynchronously.
	 */
	class ArtistListScrollListener implements OnScrollListener {

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
				GetArtistTask task = new GetArtistTask();
				task.execute(new Integer[] {totalItemCount, BLOCK_SIZE});
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState){

		}
	}
	
	@Override
	public boolean onGroupClick(ExpandableListView elv, View v, int groupPosition,
			long id) {
		//request server for all albums associated with album
		if(!elv.isGroupExpanded(groupPosition)) {
			SendMessage message = new SendMessage();
			MetaItem item = new MetaItem();
			item.setMetaItem("artist");
			item.setValue(((Artist)adapter.getGroup(groupPosition)).getArtistId());
			ArrayList<MetaItem> metaItems = new ArrayList<MetaItem>();
			metaItems.add(item);
			message.prepareMessageListAlbums(metaItems, "0", "0");
			new Thread(message).start();
		}
		return false;
	}

	//child listener
	public boolean onChildClick(ExpandableListView elv, View v,
			int groupPosition, int childPosition, long id) {
		
		//get ids of songs in album and save
		Set<String> set = new HashSet<String>();
		//set.addAll(((Album) elv.getItemAtPosition(childPosition)).getSongs());
		set.addAll(((Album) adapter.getChild(groupPosition, childPosition)).getSongs());
		SharedPreferences settings = getActivity().getSharedPreferences("songsInAlbum", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putStringSet("songsInAlbum", set);
		editor.commit();

		//save album name to be used in song activity
		settings = getActivity().getSharedPreferences("albumName", Context.MODE_PRIVATE);
		editor = settings.edit();
		//editor.putString("albumName", (((Album)elv.getItemAtPosition(childPosition)).getAlbumName()).trim());
		editor.putString("albumName", ((Album) adapter.getChild(groupPosition, childPosition)).getAlbumName());
		editor.commit();

		//save artist name to be used in song activity
		settings = getActivity().getSharedPreferences("artistName", Context.MODE_PRIVATE);
		editor = settings.edit();
		for(Artist a: MessageHandler.getArtists()) {
			if(a.getArtistId().equalsIgnoreCase(((Album) adapter.getChild(groupPosition, childPosition)).getArtistId())) {
				editor.putString("artistName", a.getArtistName());
				break;
			}
		}
		editor.commit();

		//start activity to display songs within album
		Intent intent = new Intent(getActivity().getApplicationContext(), SongActivity.class);
		startActivity(intent);

		return false;
	}
	
	/**
	 * Asynchronous call.  This class is responsible for calling the network for more artists
	 * and managing the isLoading boolean.
	 */
	class GetArtistTask extends AsyncTask<Integer, Void, List<Artist>> {
		private static final int TOP_ITEM_INDEX = 2;

		//position to scroll list to
		private int listTopPosition = 0;

		@Override 
		public void onPreExecute() {
			isLoading = true;
		}

		@Override
		protected List<Artist> doInBackground(Integer... params) {
			List<Artist> results = new ArrayList<Artist>();

			if(params.length > TOP_ITEM_INDEX)
				listTopPosition = params[TOP_ITEM_INDEX];

			//excute network call
			if(MessageHandler.getArtists().size() < params[0] + params[1]) {
				SendMessage list = new SendMessage();
				list.prepareMesssageListArtists("", Integer.toString(params[0]), Integer.toString(params[1]));
				new Thread(list).start();
			}
			return MessageHandler.getArtists();
		}

		@Override
		protected void onPostExecute(List<Artist> result) {

			//loading is done
			isLoading = false;

			//update top list item
			if(listTopPosition > 0) {
				elv.setSelection(listTopPosition);
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

	public class CustomExpandableArtistListAdapter extends BaseExpandableListAdapter {

		private Context context;
		private List<Artist> listArtists; // header titles
		// child data in format of header title, child title
		private HashMap<Artist, List<Album>> listAlbums;

		public CustomExpandableArtistListAdapter(Context context, List<Artist> listArtists,
				HashMap<Artist, List<Album>> listAlbums) {
			this.context = context;
			this.listArtists = listArtists;
			this.listAlbums = listAlbums;
		}
		
		public boolean contains(Artist item) {
			for(Artist r: listArtists) {
				if(r.getArtistId().equalsIgnoreCase(item.getArtistId()))
					return true;
			}
			return false;
		}
		
		public List<Artist> getArtistList() {
			return listArtists;
		}
		
		public HashMap<Artist, List<Album>> getAlbumsList() {
			return listAlbums;
		}
		
		public void add(Artist artist) {
			listArtists.add(artist);
			//find albums with artist
			List<Album> newAlbums = new ArrayList<Album>();
			synchronized(MessageHandler.getAlbums()) {
				for(Album a: MessageHandler.getAlbums()) {
					if(artist.getArtistId().equalsIgnoreCase(a.getArtistId())) {
						newAlbums.add(a);
					}
				}
				
				//create all albums child
				if(newAlbums.size() > 1) {	
					if(!newAlbums.get(0).getAlbumId().equalsIgnoreCase("-1")){
						Album allAlbums = new Album("-1");
						for(Album a: newAlbums) {
							allAlbums.setAlbumName("All Albums");
							allAlbums.setArtistId(artist.getArtistId());
							for(int i = 0; i < a.getSongs().size(); i++) 
								allAlbums.addSong(a.getSongs().get(i));
						}
						newAlbums.add(0,allAlbums);
					}
				}
			}
			
			//add children
			listAlbums.put(artist, newAlbums);
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return listAlbums.get(listArtists.get(groupPosition)).get(childPosititon);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

			final String childText = (String)((Album)getChild(groupPosition, childPosition)).getAlbumName();

			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.artist_list, null);
			}

			TextView albumName = (TextView) convertView
					.findViewById(R.id.artistName);

			if(childText.length() > 30)
			  albumName.setText(childText.substring(0,29) + "...");
			else 
			  albumName.setText(childText);
			
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			//Always have all albums
			return this.listAlbums.get(listArtists.get(groupPosition)).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return this.listArtists.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return listArtists.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			String headerTitle = (String)((Artist)getGroup(groupPosition)).getArtistName();
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.artist_list, null);
			}

			TextView artistName = (TextView) convertView
					.findViewById(R.id.artistName);
			artistName.setTypeface(null, Typeface.BOLD);
			if(headerTitle.length() > 30)
			  artistName.setText(headerTitle.substring(0,29) + "...");
			else
			  artistName.setText(headerTitle);
			
			//set gap between items
			elv.setDividerHeight(2);

			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}
}
