package Cloud.Service;

import Cloud.Model.FileEntity;
import Cloud.Repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    public void saveFile(String username, MultipartFile file) throws Exception {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setUsername(username);
        fileEntity.setFilename(file.getOriginalFilename());
        fileEntity.setContent(file.getBytes());
        fileRepository.save(fileEntity);
    }

    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }

    public FileEntity getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + id));
    }
}
