import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderComparison {
    public static void main(String[] args) {
        String folderPath1 = "C:\\Users\\r\\Desktop\\a";
        String folderPath2 = "C:\\Users\\r\\Desktop\\b";

        File folder1 = new File(folderPath1);
        File folder2 = new File(folderPath2);

        try {
            if (areFoldersEqual(folder1, folder2)) {
                System.out.println("Klasörlerin içerikleri aynı.");
            } else {
                System.out.println("Klasörlerin içerikleri farklı.");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Bir hata oluştu: " + e.getMessage());
        }
    }

    public static boolean areFoldersEqual(File folder1, File folder2) throws IOException, NoSuchAlgorithmException {
        if (!folder1.exists() || !folder2.exists() || !folder1.isDirectory() || !folder2.isDirectory()) {
            return false;
        }

        // Her iki klasördeki dosyaların hash'lerini alın
        Set<String> folder1Hashes = getAllFileHashes(folder1);
        Set<String> folder2Hashes = getAllFileHashes(folder2);

        // Hash setlerini karşılaştır
        return folder1Hashes.equals(folder2Hashes);
    }

    private static Set<String> getAllFileHashes(File folder) throws IOException, NoSuchAlgorithmException {
        Set<String> hashes = new HashSet<>();
        List<File> files = getAllFiles(folder);

        for (File file : files) {
            if (file.isFile()) {
                hashes.add(getFileHash(file.toPath()));
            }
        }

        return hashes;
    }

    private static List<File> getAllFiles(File folder) {
        List<File> fileList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    fileList.addAll(getAllFiles(file));
                } else {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    private static String getFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hashBytes = digest.digest(fileBytes);

        // Hash'i hex string'e çevir
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

