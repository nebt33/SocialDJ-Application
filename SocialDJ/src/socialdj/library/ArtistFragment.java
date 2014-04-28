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
import socialdj.config.R;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

/**
 * Fragment for artists.  Show all artists database holds.
 * @author Nathan
 *
 */
public class ArtistFragment extends Fragment implements OnChildClickListener {

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
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.artist_main, null);
        elv = (ExpandableListView) v.findViewById(R.id.lvExp);

		//adapter = new CustomExpandableArtistListAdapter(getActivity(), MessageHandler.getArtists(), MessageHandler.getAlbums());
		adapter = new CustomExpandableArtistListAdapter(getActivity(), new ArrayList<Artist>(), new ArrayList<Album>());

		elv.setAdapter(adapter);

        elv.setOnScrollListener(new ArtistListScrollListener());
        
        elv.setOnChildClickListener(this);
        return v;
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
	
	//child listener
	public boolean onChildClick(ExpandableListView elv, View v,
			int groupPosition, int childPosition, long id) {
		//System.out.println("INSIDE ON CHILD CLICK: " + (((Album)elv.getItemAtPosition(childPosition)).getAlbumName()).trim());
		
		//get ids of songs in album and save
		Set<String> set = new HashSet<String>();
		System.out.println(((Album) adapter.getChild(groupPosition, childPosition)).getSongs());
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
				PrintWriter out = null;
				try {
					out = new PrintWriter(ConnectedSocket.getSocket().getOutputStream());
				} catch (IOException e) {e.printStackTrace();}
				out.write("list_artists|" + params[0] + "|" + params[1] + "\n");
				out.flush();
				try {
					int start = MessageHandler.getArtists().size();
					int end = start + params[1];
					while(start < end) {
						start = MessageHandler.getArtists().size();
						Thread.sleep(10);
					}
				} catch (InterruptedException e) {e.printStackTrace();}
			}
			
			synchronized(MessageHandler.getArtists()) {
				for(int i = params[0]; i < ((params[0] + params[1])); i++) 
					results.add(MessageHandler.getArtists().get(i));
			}
			return results;
		}

		@Override
		protected void onPostExecute(List<Artist> result) {
			for(Artist item: result) {
				synchronized(adapter) {
					//if(!adapter.contains(item))
					  adapter.add(item);
					  adapter.notifyDataSetChanged();
				}
			}
			//System.out.println("adapter count: " + adapter.getCount());

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

		private Context _context;
		private List<Artist> listArtists; // header titles
		// child data in format of header title, child title
		private List<Album> listAlbums;

		public CustomExpandableArtistListAdapter(Context context, List<Artist> listArtists,
				List<Album> listAlbums) {
			this._context = context;
			this.listArtists = listArtists;
			this.listAlbums = listAlbums;
		}
		
		public void add(Artist artist) {
			listArtists.add(artist);
			//find albums with artist
			for(Album a: MessageHandler.getAlbums()) {
				if(artist.getArtistId().equalsIgnoreCase(a.getArtistId()))
					listAlbums.add(a);
			}
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			for(Album a: MessageHandler.getAlbums()) {
				if(listArtists.get(groupPosition).getArtistId().equalsIgnoreCase(a.getArtistId())) {
					return a;
				}
			}
			//if not found
			return "Album not found";
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
				LayoutInflater infalInflater = (LayoutInflater) this._context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.artist_list, null);
			}

			TextView albumName = (TextView) convertView
					.findViewById(R.id.artistName);

			albumName.setText(childText);
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			int childCount = 0;
			for(Album a: MessageHandler.getAlbums()) {
				if(a.getArtistId().equalsIgnoreCase(listArtists.get(groupPosition).getArtistId()))
					childCount++;
			}
			return childCount;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return this.listArtists.get(groupPosition).getArtistName();
		}

		@Override
		public int getGroupCount() {
			return this.listArtists.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			String headerTitle = (String) getGroup(groupPosition);
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this._context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.artist_list, null);
			}

			TextView artistName = (TextView) convertView
					.findViewById(R.id.artistName);
			artistName.setTypeface(null, Typeface.BOLD);
			artistName.setText(headerTitle);

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
