package socialdj;

public class Song implements Comparable<Song> {
	private String id;
	private String songTitle;
	private String artistName;
	private String albumName;
	private String songDuration;
	
	public Song(String id) {
		this.id = id;
		this.songTitle = "";
		this.artistName = "";
		this.albumName = "";
		this.songDuration = "";
	}
	
	@Override
	public int compareTo(Song song) {
		return this.getSongTitle().compareTo(song.getSongTitle());
	}
	
	public void setSongTitle(String songTitle) {
		this.songTitle = songTitle;
	}
	
	public void setAlbumName(String albumName) {
		this.albumName = albumName;
	}
	
	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}
	
	public void setSongDuration(String songDuration) {
		this.songDuration = songDuration;
	}
	
	public void setSongId(String id) {
		this.id = id;
	}
	
	public String getAlbumName() {
		return albumName;
	}
	
	public String getSongTitle() {
		return songTitle;
	}
	
	public String getArtistName() {
		return artistName;
	}
	
	public String getSongDuration() {
		return songDuration;
	}
	
	public String getSongId() {
		return id;
	}
}
