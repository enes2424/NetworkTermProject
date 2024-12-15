import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class FolderComparison {

    public static List<List<String>> getAllFileHashes(Path baseFolderPath, File folder) throws IOException, NoSuchAlgorithmException {
        ArrayList<String> paths = new ArrayList<>();
        ArrayList<String> hashes = new ArrayList<>();
        List<File> files;
        if (folder.isDirectory())
            files = getAllFiles(folder);
        else
            files = List.of(folder);

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

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

