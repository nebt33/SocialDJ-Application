package socialdj;

public class Artist implements Comparable<Artist> {
	private String id;
	private String artistName;
	
	public Artist(String id) {
		this.id = id;
	}
	
	public void setArtistId(String id) {
		this.id = id;
	}
	
	public String getArtistId() {
		return id;
	}
	
	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}

	public String getArtistName() {
		return artistName;
	}

	@Override
	public int compareTo(Artist artist) {
		return this.getArtistName().compareTo(artist.getArtistName());
	}
}
