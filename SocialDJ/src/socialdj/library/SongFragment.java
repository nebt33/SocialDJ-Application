package socialdj.library;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import socialdj.Album;
import socialdj.ConnectedSocket;
import socialdj.MessageHandler;
import socialdj.SendMessage;
import socialdj.Song;
import socialdj.config.R;
import socialdj.library.AlbumFragment.AlbumListScrollListener;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Fragment for songs.  Show all songs database holds.
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
	final Handler handler = new Handler();
	ViewHandler viewHandler = new ViewHandler();


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//Populate list
		adapter = new CustomSongAdapter(getActivity(), R.layout.songs_list, new ArrayList<Song>());
	    
	    setListAdapter(adapter);

		//asynchronously initial list
		GetSongTask task = new GetSongTask();
		//task.execute(new Integer[] {0, BLOCK_SIZE});

		setListAdapter(adapter);
		
		new Thread(viewHandler).start();

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
	
	@Override 		
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {			 			
		return inflater.inflate(R.layout.footer, container, false); 		
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
		viewHandler.kill();
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
			System.out.println("totalSizetoBe: " + totalSizeToBe);
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
	
	public class ViewHandler implements Runnable {
		boolean running = true;
		public void run() {
			while(running){
				//Do time consuming listener call
				getListView().setOnScrollListener(new SongListScrollListener());

				//The handler schedules the new runnable on the UI thread
				handler.post(new Runnable() {
					@Override
					public void run() {
						synchronized(MessageHandler.getSongs()) {
							for(Song item: MessageHandler.getSongs()) {
								synchronized(adapter) {
									if(!adapter.contains(item)) {
										adapter.add(item);
										adapter.notifyDataSetChanged();
									}
								}
							}
						}
					}
				});
				//Add some downtime
				try {
					Thread.sleep(100);
				}catch (InterruptedException e) {e.printStackTrace();}
			}
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
			List<Song> results = new ArrayList<Song>();

			if(params.length > TOP_ITEM_INDEX)
				listTopPosition = params[TOP_ITEM_INDEX];

			//excute network call
			System.out.println("line 143: " + MessageHandler.getSongs().size());
			if(MessageHandler.getSongs().size() < params[0] + params[1]) {
				PrintWriter out = null;
				try {
					out = new PrintWriter(ConnectedSocket.getSocket().getOutputStream());
					out.write("list_songs|" + params[0] + "|" + params[1] + "\n");
					out.flush();
				} catch (IOException e) {e.printStackTrace();}
				/*try {
					int start = MessageHandler.getSongs().size();
					int end = start + params[1];
					while(start < end) {
						start = MessageHandler.getSongs().size();
						Thread.sleep(10);
					}
				} catch (InterruptedException e) {e.printStackTrace();}*/
			}
			
			/*synchronized(MessageHandler.getSongs()) {
				for(int i = params[0]; i < ((params[0] + params[1])); i++) 
					results.add(MessageHandler.getSongs().get(i));
			}
			return results;*/
			return MessageHandler.getSongs();
		}

		@Override
		protected void onPostExecute(List<Song> result) {
			/*adapter.setNotifyOnChange(true);
			for(Song item: result) {
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
			
			//--------------------test---------------------------------
			TextView id = (TextView) rowView.findViewById(R.id.id);
			id.setText(Integer.toString(position));

			songTitle.setText(items.get(position).getSongTitle());
			artistName.setText(items.get(position).getArtistName());
			songDuration.setText(items.get(position).getSongDuration());

			final int currentlyClicked = position;
			addQButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					SendMessage message = new SendMessage();
					message.prepareMessageAddSong(getItem(currentlyClicked).getSongId());
					new Thread(message).start();
				} 
			});

			return rowView;
		}

	}
}
