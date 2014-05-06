package socialdj.library;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import socialdj.MessageHandler;
import socialdj.SendMessage;
import socialdj.Song;
import socialdj.config.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SongActivity extends Activity {

	private CustomSongAdapter adapter = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.songs_within_album_main);
		
		ListView listView = (ListView)findViewById(R.id.listViewSongs);
		
		SharedPreferences settings = getSharedPreferences("songsInAlbum", MODE_PRIVATE);
		Set<String> set = settings.getStringSet("songsInAlbum", null);
		ArrayList<String> songIds = new ArrayList<String>(set);
		ArrayList<Song> songs = new ArrayList<Song>();

		for(int i = 0; i < songIds.size(); i++) {
			for(Song s: MessageHandler.getSongs()) {
				if(s.getSongId().equalsIgnoreCase(songIds.get(i))) {
					songs.add(s);
					break;
				}
			}
		}
		
		//header for song activity(album & artist)
		settings = getSharedPreferences("albumName", MODE_PRIVATE);
		((TextView)findViewById (R.id.albumName)).setText (settings.getString("albumName", ""));
		settings = getSharedPreferences("artistName", MODE_PRIVATE);
		((TextView)findViewById (R.id.artistName)).setText (settings.getString("artistName", ""));

		//Populate list
		adapter = new CustomSongAdapter(this, R.layout.songs_list, songs);

		listView.setAdapter(adapter);
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
			this.items = items;
			this.rowViewId = rowViewId;
		}

		public boolean contains(Song item) {
			for(Song r: items){
				if(r.getSongId().equals(item.getSongId())) {
					return true;
				}
			}
			return false;
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

			if(items.get(position).getSongTitle().length() > 35)
			  songTitle.setText(items.get(position).getSongTitle().substring(0,34) + "...");
			else 
				songTitle.setText(items.get(position).getSongTitle());
			
			if(items.get(position).getArtistName().length() > 35)
				artistName.setText(items.get(position).getArtistName().substring(0,35) + "...");
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
					Toast.makeText(getApplicationContext(), "Song request sent", Toast.LENGTH_SHORT).show();
				} 
			});

			return rowView;
		}

	}

}
