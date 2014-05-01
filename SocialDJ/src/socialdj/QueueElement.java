package socialdj;

public class QueueElement implements Comparable<QueueElement> {
	private String id;
	private String songTitle;
	private String artistName;
	private String albumName;
	private String songDuration;
	private int score;
	
	public QueueElement() {
		this.score = 0;
	}
	
	public QueueElement(String songTitle, String artistName, String albumName, String songDuration) {
		this.score = 0;
		this.songTitle = songTitle;
		this.artistName = artistName;
		this.albumName = albumName;
		this.songDuration = songDuration;
	}
	
	@Override
	public int compareTo(QueueElement compareElement) {
		int compareScore = ((QueueElement)compareElement).getScore();
		return compareScore - this.score;
	}

	
	public void vote(int voteValue){
		score += voteValue;
		score=score<0?0:score;
	}
	
	public void setScore(int score) {
		this.score = score;
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
	
	public int getScore() {
		return score;
	}
	
	public String getSongId() {
		return id;
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
}
