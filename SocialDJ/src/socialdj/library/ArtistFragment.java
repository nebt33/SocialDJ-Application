package socialdj.library;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import socialdj.Album;
import socialdj.Artist;
import socialdj.ConnectedSocket;
import socialdj.MessageHandler;
import socialdj.config.R;
import android.app.Activity;
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
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Fragment for artists.  Show all artists database holds.
 * @author Nathan
 *
 */
public class ArtistFragment extends Fragment {

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


	/*@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//Populate list
		adapter = new CustomAlbumAdapter(getActivity(), R.layout.albums_list, new ArrayList<Album>());

		//asynchronously initial list
		GetSongTask task = new GetSongTask();

		setListAdapter(adapter);
		getListView().setOnScrollListener(new SongListScrollListener());

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
	}*/
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.artist_main, null);
        ExpandableListView elv = (ExpandableListView) v.findViewById(R.id.lvExp);
        
        //test dummy data
        ArrayList<Artist> listArtists = new ArrayList<Artist>();
        ArrayList<Album> listAlbums = new ArrayList<Album>();
        for(int i = 0; i < 10; i++) {
			String word = "";
			Random randomGenerator = new Random();
			for(int j = 0; j < 30; j++) {
				int randomInt = randomGenerator.nextInt(26) + 1;
				word += getCharForNumber(randomInt);
			}
			Artist artist = new Artist(Integer.toString(i));
			artist.setArtistName(word);
			
			Album album = new Album(Integer.toString(1));
			album.setAlbumName(word);
			
			listArtists.add(artist);
			listAlbums.add(album);
		}
        
        adapter = new CustomExpandableArtistListAdapter(getActivity(), listArtists, new ArrayList<Album>());
        //adapter = new CustomExpandableArtistListAdapter(getActivity(), new ArrayList<Artist>(), new ArrayList<Album>());
        elv.setAdapter(adapter);
        return v;
    }
	
	private static String getCharForNumber(int i) {
		return i > 0 && i < 27 ? String.valueOf((char)(i + 64)) : null;
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
		private ArrayList<Artist> listArtists; // header titles
		// child data in format of header title, child title
		private ArrayList<Album> listAlbums;

		public CustomExpandableArtistListAdapter(Context context, ArrayList<Artist> listArtists,
				ArrayList<Album> listAlbums) {
			this._context = context;
			this.listArtists = listArtists;
			this.listAlbums = listAlbums;
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			for(Album a: MessageHandler.getAlbums()) {
				if(listArtists.get(groupPosition).getArtistId().equalsIgnoreCase(a.getArtistId())) {
					return a.getAlbumName();
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

			final String childText = (String) getChild(groupPosition, childPosition);

			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this._context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.artist_list, null);
			}

			TextView txtListChild = (TextView) convertView
					.findViewById(R.id.artistName);

			txtListChild.setText(childText);
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

			TextView lblListHeader = (TextView) convertView
					.findViewById(R.id.artistName);
			lblListHeader.setTypeface(null, Typeface.BOLD);
			lblListHeader.setText(headerTitle);

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

	/**
	 * Method that listens for clicks on an item in the listview. 
	 */
	/*@Override
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
	}*/
}
