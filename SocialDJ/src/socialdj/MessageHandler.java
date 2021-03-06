package socialdj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import socialdj.queue.QueueFragment;

import android.widget.Toast;


public class MessageHandler implements Runnable {

    static boolean serverExist = false;
	String currentServer = null;
	String nonActiveIP = "0.0.0.0";
	
    //List for views
	static List<Song> songs = Collections.synchronizedList(new ArrayList<Song>());
	static List<Artist> artists = Collections.synchronizedList(new ArrayList<Artist>());
	static List<Album> albums = Collections.synchronizedList(new ArrayList<Album>());
    static List<QueueElement> queueElements = Collections.synchronizedList(new ArrayList<QueueElement>());
    
    //queue messages of forgets to remove from ui adapter threads
    static List<String> forgetSongList = Collections.synchronizedList(new ArrayList<String>());
    static List<String> forgetAlbumList = Collections.synchronizedList(new ArrayList<String>());
    static List<String> forgetArtistList = Collections.synchronizedList(new ArrayList<String>());
 
    static boolean musicState = false;
	
	public static List<Song> getSongs() {
		return songs;
	}
	
	public static List<String> getForgetSongList() {
		return forgetSongList;
	}
	
	public static List<String> getForgetAlbumList() {
		return forgetAlbumList;
	}
	
	public static List<String> getForgetArtistList(){
		return forgetArtistList;
	}
	
	public static List<Artist> getArtists() {
		return artists;
	}
	
	public static List<Album> getAlbums() {
		return albums;
	}
	
	public static List<QueueElement> getQueueElements() {
		return queueElements;
	}
	
	public static boolean getMusicState() {
		return musicState;
	}
	
	public static boolean containsSong(Song s) {
		synchronized(MessageHandler.getSongs()){
		for(Song item:MessageHandler.getSongs()) {
			if(item.getSongId().equalsIgnoreCase(s.getSongId()))
				return true;	
		}
		}
		return false;
	}
	
	@Override
	public void run() {
		if(!ConnectedSocket.getConnectedIP().equals(nonActiveIP))
			  serverExist = true;
		
		while(serverExist) {
	
			//read in data
			BufferedReader in = null;
			try {
				in = new BufferedReader( 
						new InputStreamReader(ConnectedSocket.getSocket().getInputStream()));

				String inputLine = null; 
				while ((inputLine = in.readLine()) != null) 
				{ 
					//System.out.println(inputLine);
					List<String> temp = Arrays.asList(inputLine.split("\\|"));
					String operation = temp.get(0);
					//---determine operation
					switch(operation) {
					case "download_starting":
						break;
					case "download_fail":
						break;
					case "download_success":
						break;
					case "new_song":
						newSong(inputLine);
						break;
					case "song_info":
						songInfo(inputLine);
						break;
					case "forget_song":
						forgetSong(inputLine);
						break;
					case "new_album":
						newAlbum(inputLine);
						break;
					case "album_info":
						albumInfo(inputLine);
						break;
					case "forget_album":
						forgetAlbum(inputLine);
						break;
					case "new_artist":
						newArtist(inputLine);
						break;
					case "artist_info":
						artistInfo(inputLine);
						break;
					case "forget_artist":
						forgetArtist(inputLine);
						break;
					case "add_bottom":
						addBottom(inputLine);
						break;
					case "remove_top":
						removeTop(inputLine);
						break;
					case "score":
						score(inputLine);
						break;
					case "playing":
						musicState = true;
						break;
					case "paused":
						musicState = false;
						break;
					case "skip":
						//implement skip - talk to david
						break;
					}


				}
			} catch (IOException e) {
                e.printStackTrace();
              }

			//server was changed
			if(!ConnectedSocket.getConnectedIP().equals(currentServer))
				serverExist = false;
		}
		
		//clear data
		songs.clear();
		artists.clear();
		albums.clear();
		queueElements.clear();
		musicState = false;
		 
		
	}
	
	/**
	 * Creates a new Artist and will sort that artist into the list by id.
	 * @param inputLine
	 */
	public void newArtist(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);

		Artist artist = new Artist(id);

		//search list for album to enter into correct spot
		synchronized (artists) {
			boolean inserted = false;
			for(int i = 0; i < artists.size(); i++) {
				//if artist already exist
				if(Integer.parseInt(artist.getArtistId()) == Integer.parseInt(artists.get(i).getArtistId())) {
					inserted = true;
					break;
				}
			}
			
			if(!inserted)
				artists.add(artist);
		}
	}
	
	/**
	 * Updates artist fields with name of artist
	 * @param inputLine
	 */
	public void artistInfo(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		String artistName = temp.get(2);
		
		synchronized (artists) {
			for(Artist artist: artists) {
				if(artist.getArtistId().equalsIgnoreCase(id)){
					artist.setArtistName(artistName);
					break;
				}
			}
		}
	}
	
	/**
	 * Creates a new album and will sort that album into the list by id.
	 * @param inputLine
	 */
	public void newAlbum(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);

		Album album = new Album(id);

		//search list for album to enter into correct spot
		synchronized (albums) {
			albums.add(album);
		}
	}
	
	/**
	 * Updates album info with songs' id
	 * @param inputLine
	 */
	public void albumInfo(String inputLine) {
		//split inputLine up
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		String albumName = temp.get(2);
		String artistId = temp.get(3);
		//search list for album to enter into correct spot
		synchronized (albums) {
			for(Album album: albums) {
				if(album.getAlbumId().equalsIgnoreCase(id)){
					album.setAlbumName(albumName);
					album.setArtistId(artistId);
					List<String> ids = Arrays.asList(temp.get(4).split(","));
					for(int i = 0; i < ids.size(); i++) {
						album.addSong(ids.get(i));
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Remove song because it no longer exists.
	 * ->Queue and Cached list of songs
	 * @param inputLine
	 */
	public void forgetSong(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		//remove song from cache songs
		synchronized(songs) {
			for(Song s : songs) {
				if(Integer.parseInt(s.getSongId()) == Integer.parseInt(id)) {
					songs.remove(s);
					break;
				}
			}
		}
		
		//remove song from queue if in queue
		synchronized(queueElements) {
		    for(QueueElement q : queueElements) {
			  if(Integer.parseInt(q.getSongId()) == Integer.parseInt(id)) {
				songs.remove(q);
				break;
			  }
			}
		}
		
		//add forget message to list, to remove on ui thread
		forgetSongList.add(id);
	}
	
	/**
	 * Remove album because it no longer exists
	 * -> Cached list of albums
	 * @param inputLine
	 */
	public void forgetAlbum(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		
		synchronized(albums) {
		    for(Album a: albums) {
			  if(Integer.parseInt(a.getAlbumId()) == Integer.parseInt(id)) {
				albums.remove(a);
				break;
			  }
		  }
		}
	}
	
	/**
	 * Remove artist because it no longer exists
	 * @param inputLine
	 */
	public void forgetArtist(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		
		synchronized (artists) {
			for(Artist a: artists) {
				if(Integer.parseInt(a.getArtistId()) == Integer.parseInt(id)) {
					artists.remove(a);
					break;
				}
			}
		}
	}
	
	/**
	 * Add a song to the queue
	 * @param inputLine
	 */
	public void addBottom(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		
		QueueElement element = new QueueElement();
		element.setSongId(id);
		for(Song s: songs) {
			if(id.equalsIgnoreCase(s.getSongId())) {
				element.setSongTitle(s.getSongTitle());
				element.setArtistName(s.getArtistName());
				element.setAlbumName(s.getAlbumName());
				element.setSongDuration(s.getSongDuration());
				break;
			}
		}
		
		synchronized(queueElements) {
			queueElements.add(element);
			Collections.sort(queueElements);
		}
	}
	
	/**
	 * Remove song that has finished playing from queue
	 * @param inputLine
	 */
	public void removeTop(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		
		synchronized (queueElements) {
			for(QueueElement q: queueElements) {
				if(Integer.parseInt(q.getSongId()) == Integer.parseInt(id)){
					queueElements.remove(q);
					break;
				}
			}
			if(queueElements.size()>0){
				Collections.sort(queueElements);
				queueElements.get(0).setScore(Integer.MAX_VALUE);
			}
		}
	}
	
	/**
	 * Update score of song in the queue
	 * @param inputLine
	 */
	public void score(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		String score = temp.get(2);
		
		synchronized (queueElements) {
			for(QueueElement q : queueElements) {
				if(Integer.parseInt(q.getSongId()) == Integer.parseInt(id)) {
					q.setScore(Integer.parseInt(score));
					break;
				}
			}
		    
			//sort elements by votes in decending order
			Collections.sort(queueElements);   
		}
	}
	
	/**
	 * Creates a new song with empty information for the cache
	 */
	public void newSong(String inputLine) {
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);

		Song song = new Song(id);

		//search list for song to enter into correct spot
		synchronized (songs) {
			boolean inserted = false;
			for(int i = 0; i < songs.size(); i++) {
				//if there is a song with that id already, break
				if(Integer.parseInt(song.getSongId()) == Integer.parseInt(songs.get(i).getSongId())) {
					inserted = true;
					break;
				}
			}
			
			if(!inserted)
				songs.add(song);
		}
	}
	
	/**
	 * Updates song fields currently in cache
	 */
	public void songInfo(String inputLine) {
		//split inputLine up
		List<String> temp = Arrays.asList(inputLine.split("\\|"));
		String id = temp.get(1);
		String metaItem = "";
		//search list for song to enter into correct spot
		synchronized (songs) {
			for(Song song: songs) {
				if(song.getSongId().equalsIgnoreCase(id)){
					for(String s : temp) {
						if(s.equalsIgnoreCase("album"))
							metaItem = s;
						else if(s.equalsIgnoreCase("artist"))
							metaItem = s;
						else if(s.equalsIgnoreCase("name"))
							metaItem = s;
						else if(s.equalsIgnoreCase("duration"))
							metaItem = s;
						else if(metaItem.equalsIgnoreCase("artist")) {
							for(Artist search: MessageHandler.getArtists()) {
								if(search.getArtistId().equalsIgnoreCase(s)) {
									song.setArtistName(search.getArtistName());
									break;
								}
							}
						}
						else if(metaItem.equalsIgnoreCase("album")) {
							for(Album search: MessageHandler.getAlbums()) {
								if(search.getAlbumId().equalsIgnoreCase(s)) {
									song.setAlbumName(search.getAlbumName());
									break;
								}
							}
						}
						else if(metaItem.equalsIgnoreCase("name"))
							song.setSongTitle(s);
						else if(metaItem.equalsIgnoreCase("duration"))
							song.setSongDuration(s);
					}
					break;
				}
			}
		}
	}
}
