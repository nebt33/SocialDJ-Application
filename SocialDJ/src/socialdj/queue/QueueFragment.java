package socialdj.queue;

import java.util.ArrayList;
import java.util.List;

import socialdj.QueueElement;
import socialdj.SendMessage;
import socialdj.Song;
import socialdj.config.R;
import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.ListFragment;

/**
 * Fragment of the queue.  
 * @author Nathan
 *
 */
public class QueueFragment extends Fragment {
	
	private CustomQueueAdapter adapter = null;
	private View footer;
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.queue, container, false);
        //footer = inflater.inflate(R.layout.queue_progress_bar, container, false);
        return rootView;
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		QueueElement test = new QueueElement();
		test.setSongTitle("Test Song Name");
		test.setArtistName("Test Artist");
		test.setSongDuration("4:15");
		ArrayList<QueueElement> list = new ArrayList<QueueElement>();
		list.add(test);
		list.add(new QueueElement("Song 2", "Artist 2", "Album 2", "5:54"));
		
		ListView listView = (ListView) getActivity().findViewById(R.id.QueueListView);
		adapter = new CustomQueueAdapter(getActivity(), R.layout.queue, list);
		//listView.addFooterView(footer);
		listView.setAdapter(adapter);
	}
	
	//look into addFooterView for the fixed view at bottom of Q



	class CustomQueueAdapter extends ArrayAdapter<QueueElement> {
		private final Activity context;
		private List<QueueElement> items = new ArrayList<QueueElement>();
		private final int rowViewId;
	
		public CustomQueueAdapter(Activity context, int rowViewId, List<QueueElement> items) {
			super(context, rowViewId, items);
			this.context = context;
			this.items.addAll(items);
			this.rowViewId = rowViewId;
		}
		
		@Override 
		public View getView(int position, View convertView, ViewGroup parent) {
			final TextView songTitle;
			final TextView artistName;
			final TextView songDuration;
			final TextView score;
			final Button upVoteButton;
			final Button downVoteButton;
			
			if (convertView == null) {
	            LayoutInflater mInflater = (LayoutInflater)
	                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
	            convertView = mInflater.inflate(R.layout.queue_element, null);
	        }
			/*
			 * Inflate row using the layout.
			 */
			//LayoutInflater inflater = context.getLayoutInflater();
			//convertView = inflater.inflate(rowViewId, null, true);
			songTitle = (TextView) convertView.findViewById(R.id.QueueSongName);
			artistName = (TextView) convertView.findViewById(R.id.QueueArtistName);
			songDuration = (TextView) convertView.findViewById(R.id.QueueSongDuration);
			score = (TextView) convertView.findViewById(R.id.QueueScore);
			upVoteButton = (Button) convertView.findViewById(R.id.UpVoteButton);
			downVoteButton = (Button) convertView.findViewById(R.id.DownVoteButton);

			//System.out.println("--------     !!!!!!!!!!!!!!!! "+songTitle);
			if(items.get(position).getSongTitle()!=null)
			    songTitle.setText(items.get(position).getSongTitle());
			if(items.get(position).getArtistName()!=null)
			    artistName.setText(items.get(position).getArtistName());
			if(items.get(position).getSongDuration()!=null)
			    songDuration.setText(items.get(position).getSongDuration());
			score.setText(""+items.get(position).getScore());

			final int currentlyClicked = position;
			upVoteButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					items.get(currentlyClicked).vote(1);
					score.setText(""+items.get(currentlyClicked).getScore());
					v.setEnabled(false);
					downVoteButton.setEnabled(false);
					SendMessage message = new SendMessage();
					message.prepareMessageVote(getItem(currentlyClicked).getSongId(), "1");
					new Thread(message).start();
				} 
			});

			downVoteButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					items.get(currentlyClicked).vote(-1);
					score.setText(""+items.get(currentlyClicked).getScore());
					v.setEnabled(false);
					upVoteButton.setEnabled(false);
					SendMessage message = new SendMessage();
					message.prepareMessageVote(getItem(currentlyClicked).getSongId(), "-1");
					new Thread(message).start();
				} 
			});

			return convertView;
		}
	}
}