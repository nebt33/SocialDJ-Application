package socialdj.queue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import socialdj.Album;
import socialdj.Artist;
import socialdj.MessageHandler;
import socialdj.QueueElement;
import socialdj.SendMessage;
import socialdj.Song;
import socialdj.config.R;
import socialdj.library.SongActivity;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.support.v4.app.ListFragment;

/**
 * Fragment of the queue.  
 * @author David
 *
 */
public class QueueFragment extends Fragment {
	
	private CustomQueueAdapter adapter = null;
	final Handler handler = new Handler();
	ViewHandler viewHandler = new ViewHandler();
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.queue, null, false);
        return rootView;
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ListView listView = (ListView) getActivity().findViewById(R.id.QueueListView);
		System.out.println("HERE");
		adapter = new CustomQueueAdapter(getActivity(), R.layout.queue, MessageHandler.getQueueElements());
		listView.setAdapter(adapter);
		new Thread(viewHandler).start();
		final ImageButton playPause = (ImageButton) getActivity().findViewById(R.id.PlayPauseButton);
        playPause.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	if(adapter.items != null && adapter.items.size() > 0){
		        	SendMessage message = new SendMessage();
		        	if(MessageHandler.getMusicState()){
		        		message.prepareMessagePause();
		        	}
		        	else{
		        		message.prepareMessagePlay();
		        	}
					new Thread(message).start();
					adapter.notifyDataSetChanged();
	        	}
	        }
	    });
        ImageButton skip = (ImageButton) getActivity().findViewById(R.id.SkipButton);
        skip.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	if(adapter.items != null && adapter.items.size() > 0){
		        	SendMessage message = new SendMessage();
		        	message.prepareMessageSkip();
					new Thread(message).start();
					Collections.sort(adapter.items);
					adapter.notifyDataSetChanged();
	        	}
	        }
	    });
		if(adapter != null && adapter.items!=null){
	        SharedPreferences settings = getActivity().getSharedPreferences("upVotedSongs", Context.MODE_PRIVATE);
			Set<String> set = settings.getStringSet("upVotedSongs", null);
			if(set != null){
				ArrayList<String> songIds = new ArrayList<String>(set);
				for(String s : songIds){
					for(QueueElement q : adapter.items){
						
						if(s.equals(q.getSongId())){
							q.upVote();
							Collections.sort(adapter.items);
							adapter.notifyDataSetChanged();
						}
					}
				}
			}
        }
		
		if(adapter != null && adapter.items!=null){
	        SharedPreferences settings = getActivity().getSharedPreferences("downVotedSongs", Context.MODE_PRIVATE);
			Set<String> set = settings.getStringSet("downVotedSongs", null);
			if(set != null){
				ArrayList<String> songIds = new ArrayList<String>(set);
				for(String s : songIds){
					for(QueueElement q : adapter.items){
						
						if(s.equals(q.getSongId())){
							q.downVote();
							Collections.sort(adapter.items);
							adapter.notifyDataSetChanged();
						}
					}
				}
			}
        }
	}

	class CustomQueueAdapter extends ArrayAdapter<QueueElement> {
		private final Activity context;
		private List<QueueElement> items = new ArrayList<QueueElement>();
		private final int rowViewId;
	
		public CustomQueueAdapter(Activity context, int rowViewId, List<QueueElement> items) {
			super(context, rowViewId, items);
			this.context = context;
			for(QueueElement q : items){
				this.items.add(new QueueElement(q));
			}
			System.out.println(this.items.size());
			this.rowViewId = rowViewId;
		}
		
		@Override
		public int getCount(){
			return items.size();
		}
		
		@Override 
		public View getView(int position, View convertView, ViewGroup parent) {

			final TextView songTitle;
			final TextView artistName;
			final TextView songDuration;
			final TextView score;
			final RadioButton upVoteButton;
			final RadioButton downVoteButton;
			
			View rowView = convertView;
			//convertView = null;
			
	        LayoutInflater mInflater = (LayoutInflater)
	        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
	        convertView = mInflater.inflate(R.layout.queue_element, null);
			
			songTitle = (TextView) convertView.findViewById(R.id.QueueSongName);
			artistName = (TextView) convertView.findViewById(R.id.QueueArtistName);
			score = (TextView) convertView.findViewById(R.id.QueueScore);
			upVoteButton = (RadioButton) convertView.findViewById(R.id.UpVoteButton);
			downVoteButton = (RadioButton) convertView.findViewById(R.id.DownVoteButton);

			//System.out.println(position + " " + items.size());
			if(items.get(position).getSongTitle()!=null)
				if(items.get(position).getSongTitle().length() > 23)
			      songTitle.setText(items.get(position).getSongTitle().substring(0,22));
				else
					songTitle.setText(items.get(position).getSongTitle());
			if(items.get(position).getArtistName()!=null)
				if(items.get(position).getArtistName().length() > 30)
			      artistName.setText(items.get(position).getArtistName().substring(0,29));
				else
				  artistName.setText(items.get(position).getArtistName());
			upVoteButton.setChecked(items.get(position).isVotedUp());
			downVoteButton.setChecked(items.get(position).isVotedDown());
			if(items.get(position).getScore() >= Integer.MAX_VALUE){
				score.setText("");
			}
			else{
				score.setText(""+items.get(position).getScore());
			}
			if(position == 0){
				convertView.setBackgroundColor(Color.GRAY);
				upVoteButton.setVisibility(View.GONE);
				downVoteButton.setVisibility(View.GONE);
			}
			
			
			final int currentlyClicked = position;
			upVoteButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					items.get(currentlyClicked).upVote();
					if(currentlyClicked == 0){
						score.setText("");
					}
					else{
						score.setText(""+items.get(currentlyClicked).getScore());
					}
					SendMessage message = new SendMessage();
					message.prepareMessageVote(getItem(currentlyClicked).getSongId(), "1");
					new Thread(message).start();
					
					// save the click to local memory
					Set<String> set = new HashSet<String>();
					for(QueueElement i : items){
						if(i.isVotedUp()){
						  set.add(i.getSongId());
						}
					}
					SharedPreferences settings = getActivity().getSharedPreferences("upVotedSongs", Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putStringSet("upVotedSongs", set);
					editor.commit();
					
					Collections.sort(adapter.items);
					adapter.notifyDataSetChanged();
				}
			});

			downVoteButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					items.get(currentlyClicked).downVote();
					if(currentlyClicked == 0){
						score.setText("");
					}
					else{
						score.setText(""+items.get(currentlyClicked).getScore());
					}
					SendMessage message = new SendMessage();
					message.prepareMessageVote(getItem(currentlyClicked).getSongId(), "-1");
					new Thread(message).start();
					
					// save the click to local memory
					Set<String> set = new HashSet<String>();
					for(QueueElement i : items){
						if(i.isVotedDown()){
						  set.add(i.getSongId());
						}
					}
					SharedPreferences settings = getActivity().getSharedPreferences("downVotedSongs", Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putStringSet("downVotedSongs", set);
					editor.commit();
					Collections.sort(adapter.items);
					adapter.notifyDataSetChanged();
				}
			});

			return convertView;
		}
	}
	
	public class ViewHandler implements Runnable {
		boolean running = true;
		public void run() {
			while(running){

				//The handler schedules the new runnable on the UI thread
				handler.post(new Runnable() {
					@Override
					public void run() {
						
						ArrayList<QueueElement> temp = new ArrayList<QueueElement>();
						// Remove QueueElements from local data which are no longer in the Queue
						synchronized(adapter)
						{
							for(QueueElement i : adapter.items){
								synchronized(MessageHandler.getQueueElements())
								{
									boolean found = false;
									for(QueueElement q : MessageHandler.getQueueElements()){
										if(i.getSongId().equals(q.getSongId())){
											found = true;
											break;
										}
									}
									if(!found){
										temp.add(i);
									}
								}
							}
						}
						adapter.items.removeAll(temp);
						Collections.sort(adapter.items);
						adapter.notifyDataSetChanged();
						temp.clear();
						// Updates local QueueElements adding new songs
						synchronized(MessageHandler.getQueueElements()) {
							for(QueueElement q: MessageHandler.getQueueElements()) {
								synchronized(adapter)
								{
									boolean found = false;
									for(QueueElement i : adapter.items)
									{
										if(i.getSongId().equals(q.getSongId())){
											found = true;
											break;
										}
									}
									if(!found){
										temp.add(new QueueElement(q));
									}
								}
							}
							adapter.items.addAll(temp);
							Collections.sort(adapter.items);
							adapter.notifyDataSetChanged();
						}
						
						synchronized(MessageHandler.getQueueElements()) {
							for(QueueElement q: MessageHandler.getQueueElements())
							{
								// Update score for local QueueElements
								for(QueueElement i : adapter.items){
									if(q.getSongId().equals(i.getSongId())){
										if(q.getScore() != i.getScore()){
											i.setScore(q.getScore());
											adapter.notifyDataSetChanged();
										}
										break;
									}
								}
							}
						}
						synchronized(adapter) {
							if(getView() != null){
								TextView currentSong = (TextView) getView().findViewById(R.id.CurrentSong);
								if(adapter.items != null && adapter.items.size() > 0){
									currentSong.setText(adapter.items.get(0).getSongTitle());
								}
							}
						}
						Collections.sort(adapter.items);
						adapter.notifyDataSetChanged();
					}
				});
				//Add some downtime
				try {
					Thread.sleep(1000);
				}catch (InterruptedException e) {e.printStackTrace();}
			}
		}

		public void kill() {
			running = false;
		}
	}
}
