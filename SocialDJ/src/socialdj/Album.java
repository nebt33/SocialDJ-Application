package socialdj;

import java.util.ArrayList;

public class Album implements Comparable<Album> {
	private String id;
	private String albumName;
	private String artistName;
	ArrayList<String> songs;
	
	public Album(String id) {
		this.id = id;
		albumName = "";
		artistName = "";
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
	
	public String getArtistName() {
		return artistName;
	}
	
	public void setArtistName(String artistName) {
		this.artistName = artistName;
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
	
	public ArrayList<String> getSongs() {
		return songs;
	}

	@Override
	public int compareTo(Album album) {
		return this.getAlbumName().compareTo(album.getAlbumName());
	}
}
