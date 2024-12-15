import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class FolderComparison {
    public static void main(String[] args) {
        String folderPath1 = "/home/eates/Masaüstü/a";
        String folderPath2 = "/home/eates/Masaüstü/b";

        File folder1 = new File(folderPath1);
        File folder2 = new File(folderPath2);

        try {
            if (areFoldersEqual(folder1, folder2))
                System.out.println("Klasörlerin içerikleri aynı.");
            else
                System.out.println("Klasörlerin içerikleri farklı.");
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Bir hata oluştu: " + e.getMessage());
        }
    }

    public static boolean areFoldersEqual(File folder1, File folder2) throws IOException, NoSuchAlgorithmException {
        // Her iki klasördeki dosyaların hash'lerini alın
        List<List<String>> folder1Hashes = getAllFileHashes(folder1);
        List<List<String>> folder2Hashes = getAllFileHashes(folder2);

        // Hash listelerini karşılaştır
        return folder1Hashes.get(1).equals(folder2Hashes.get(1));
    }

    private static List<List<String>> getAllFileHashes(File folder) throws IOException, NoSuchAlgorithmException {
        ArrayList<String> paths = new ArrayList<>();
        ArrayList<String> hashes = new ArrayList<>();
        List<File> files = getAllFiles(folder);
        Path baseFolderPath = folder.toPath();

        for (File file : files) {
            Path filePath = file.toPath();
            paths.add(baseFolderPath.relativize(filePath).toString());
            hashes.add(getFileHash(filePath));
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < hashes.size(); i++)
            indices.add(i);

        indices.sort(Comparator.comparing(hashes::get));

        List<String> sortedPaths = new ArrayList<>();
        List<String> sortedHashes = new ArrayList<>();
        for (int index : indices) {
            sortedPaths.add(paths.get(index));
            sortedHashes.add(hashes.get(index));
        }

        return List.of(sortedPaths, sortedHashes);
    }

    private static List<File> getAllFiles(File folder) {
        List<File> fileList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files) {
                if (file.isDirectory())
                    fileList.addAll(getAllFiles(file));
                else
                    fileList.add(file);
            }
        return fileList;
    }

    private static String getFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hashBytes = digest.digest(fileBytes);

        // Hash'i hex string'e çevir
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

