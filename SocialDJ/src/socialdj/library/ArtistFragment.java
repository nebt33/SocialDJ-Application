package socialdj.library;

import socialdj.config.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment for artists.  Show all artists database holds.
 * @author Nathan
 *
 */
public class ArtistFragment extends Fragment {

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
 
        View rootView = inflater.inflate(R.layout.queue, container, false);
         
        return rootView;
    }
}
