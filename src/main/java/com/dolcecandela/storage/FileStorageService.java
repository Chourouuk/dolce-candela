package com.dolcecandela.storage;

import com.dolcecandela.common.ApiException;
import com.dolcecandela.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Stockage des images sur disque (volume Docker). Mesures de sécurité :
 *  - type réel détecté par les "magic bytes" (on ignore le nom et le Content-Type envoyés par le client) ;
 *  - seuls PNG, JPEG et WebP sont acceptés, 10 Mo maximum ;
 *  - nom de fichier aléatoire (UUID) : pas de traversée de répertoire possible.
 * Pour passer au cloud plus tard (S3, Azure Blob...), il suffit de remplacer cette classe.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    public static final String PUBLIC_PREFIX = "/uploads/";

    public enum Folder {
        PRODUCTS("products"),
        REFERENCES("references");

        private final String path;

        Folder(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    private final Path root;

    public FileStorageService(AppProperties properties) throws IOException {
        this.root = Path.of(properties.storage().uploadDir()).toAbsolutePath().normalize();
        for (Folder folder : Folder.values()) {
            Files.createDirectories(root.resolve(folder.path()));
        }
    }

    public Path getRoot() {
        return root;
    }

    /** Enregistre l'image et renvoie son URL publique, ex. /uploads/products/3f2a....jpg */
    public String store(MultipartFile file, Folder folder) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Fichier vide.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw ApiException.badRequest("Image trop lourde (10 Mo maximum).");
        }
        String extension;
        try (InputStream in = file.getInputStream()) {
            extension = detectImageExtension(in.readNBytes(12));
        } catch (IOException e) {
            throw ApiException.badRequest("Impossible de lire le fichier.");
        }
        if (extension == null) {
            throw ApiException.badRequest("Format non supporté : PNG, JPG ou WebP uniquement.");
        }
        String filename = UUID.randomUUID() + "." + extension;
        Path target = root.resolve(folder.path()).resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.badRequest("Chemin de fichier invalide.");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Échec d'écriture du fichier {}", target, e);
            throw new IllegalStateException("Échec de l'enregistrement de l'image", e);
        }
        return PUBLIC_PREFIX + folder.path() + "/" + filename;
    }

    /** Supprime un fichier précédemment stocké (ignore les URL externes ou les images de démo). */
    public void deleteQuietly(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        Path target = root.resolve(publicUrl.substring(PUBLIC_PREFIX.length())).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Impossible de supprimer {}", target, e);
        }
    }

    /** Vérifie qu'une URL correspond bien à un fichier uploadé dans le dossier attendu. */
    public boolean isStoredUrl(String publicUrl, Folder folder) {
        if (publicUrl == null) {
            return false;
        }
        String prefix = PUBLIC_PREFIX + folder.path() + "/";
        if (!publicUrl.startsWith(prefix) || publicUrl.contains("..")) {
            return false;
        }
        Path target = root.resolve(publicUrl.substring(PUBLIC_PREFIX.length())).normalize();
        return target.startsWith(root) && Files.isRegularFile(target);
    }

    static String detectImageExtension(byte[] header) {
        if (header.length >= 8 && startsWith(header, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "png";
        }
        if (header.length >= 3 && startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "jpg";
        }
        if (header.length >= 12 && startsWith(header, new byte[]{'R', 'I', 'F', 'F'})
                && Arrays.equals(Arrays.copyOfRange(header, 8, 12), new byte[]{'W', 'E', 'B', 'P'})) {
            return "webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(Arrays.copyOfRange(data, 0, prefix.length), prefix);
    }
}
