package zw.co.tncybertech.dispute.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;

@Service
public class StorageService {

  private final Path root;

  public StorageService(@Value("${storage.root}") String rootPath) {
    this.root = Path.of(rootPath).toAbsolutePath().normalize();
  }

  public String save(Long disputeId, MultipartFile file) throws IOException {
    Files.createDirectories(root.resolve(String.valueOf(disputeId)));

    String safeName = (file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()).replaceAll("[^a-zA-Z0-9._-]", "_");
    String name = UUID.randomUUID() + "_" + safeName;

    Path dest = root.resolve(String.valueOf(disputeId)).resolve(name);
    Files.copy(file.getInputStream(), dest);

    return dest.toString();
  }

  public Path resolveStoredPath(String storedPath) {
    Path path = Path.of(storedPath).toAbsolutePath().normalize();
    if (!path.startsWith(root)) {
      throw new SecurityException("Forbidden");
    }
    return path;
  }
}
