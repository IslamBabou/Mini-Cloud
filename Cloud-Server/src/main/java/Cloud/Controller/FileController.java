package Cloud.Controller;

import Cloud.Model.FileEntity;
import Cloud.Service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadFile(@RequestParam String username,
                                             @RequestParam MultipartFile file) {
        try {
            fileService.saveFile(username, file);
            return ResponseEntity.ok("File uploaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileEntity>> listFiles() {
        return ResponseEntity.ok(fileService.getAllFiles());
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        FileEntity file = fileService.getFileById(id);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file.getContent());
    }
}
