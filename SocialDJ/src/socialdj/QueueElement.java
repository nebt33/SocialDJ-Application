package socialdj;

public class QueueElement implements Comparable<QueueElement> {
	private String id;
	private String songTitle;
	private String artistName;
	private String albumName;
	private String songDuration;
	private int score;
	private boolean upvoted = false;
	private boolean downvoted = false;
	
	public QueueElement() {
		this.score = 0;
	}
	
	public QueueElement(QueueElement other) {
		this.score = other.score;
		if(other.songTitle != null)
			this.songTitle = other.songTitle;
		if(other.artistName != null)
			this.artistName = other.artistName;
		if(other.albumName != null)
			this.albumName = other.albumName;
		if(other.songDuration != null)
			this.songDuration = other.songDuration;
		if(other.id != null)
			this.id = other.id;
	}
	
	@Override
	public int compareTo(QueueElement compareElement) {
		int compareScore = ((QueueElement)compareElement).getScore();
		return compareScore - this.score;
	}

	public void upVote() {
		upvoted = true;
		downvoted = false;
	}

	public void downVote() {
		upvoted = false;
		downvoted = true;
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

	public boolean isVotedUp() {
		return upvoted;
	}
	
	public boolean isVotedDown() {
		return downvoted;
	}
	
	
}
