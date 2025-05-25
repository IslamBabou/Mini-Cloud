	package Cloud.Model;
	
	import jakarta.persistence.*;
	
	@Entity
	@Table(name = "files")
	public class FileEntity {
	
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	
	    private String username;
	    private String filename;
	
	    @Lob
	    private byte[] content;
	
	    // Getters and setters
	    public Long getId() { return id; }
	    public void setId(Long id) { this.id = id; }
	
	    public String getUsername() { return username; }
	    public void setUsername(String username) { this.username = username; }
	
	    public String getFilename() { return filename; }
	    public void setFilename(String filename) { this.filename = filename; }
	
	    public byte[] getContent() { return content; }
	    public void setContent(byte[] content) { this.content = content; }
	}
