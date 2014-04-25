package socialdj;

import java.util.ArrayList;

public class Album implements Comparable<Album> {
	private String id;
	private String albumName;
	ArrayList<String> songs;
	
	public Album(String id) {
		this.id = id;
		albumName = "";
		songs = new ArrayList<String>();
	}
	
	public void addSong(String songId) {
		songs.add(songId);
	}
	
	public void removeSong(String songId) {
		for(String s: songs)
			if(songId.equalsIgnoreCase(s)) {
		      songs.remove(songId);
		      break;
			}
	}
	
	public String getAlbumId() {
		return id;
	}
	
	public void setAlbumId(String id) {
		this.id = id;
	}
	
	public void setAlbumName(String name) {
		this.albumName = name;
	}
	
	public String getAlbumName(){
		return albumName;
	}

	@Override
	public int compareTo(Album album) {
		return this.getAlbumName().compareTo(album.getAlbumName());
	}
}
