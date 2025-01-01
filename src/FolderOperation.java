import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class FolderOperation {
	public static ArrayList<byte[]> bytes;
	public static int				totalnumOfBytes;
	
    public static List<List<String>> getAllFileInformations(Path baseFolderPath, File folder) throws IOException, NoSuchAlgorithmException {
        ArrayList<String>	paths = new ArrayList<>();
        ArrayList<String>	numOfBytes = new ArrayList<>();
        ArrayList<String>	hashes = new ArrayList<>();
        totalnumOfBytes = 0;
        bytes = new ArrayList<>();
        List<File> files;
        if (folder.isDirectory())
            files = getAllFiles(folder);
        else
            files = List.of(folder);

        for (File file : files) {
            Path filePath = file.toPath();
            paths.add(baseFolderPath.relativize(filePath).toString());
            byte[] fileBytes = Files.readAllBytes(filePath);
            hashes.add(getFileHash(fileBytes));
            bytes.add(fileBytes);
            numOfBytes.add("" + fileBytes.length);
            totalnumOfBytes += fileBytes.length;
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < hashes.size(); i++)
            indices.add(i);

        indices.sort(Comparator.comparing(hashes::get));

        List<String> sortedPaths = new ArrayList<>();
        List<String> sortedNumOfBytes = new ArrayList<>();
        List<String> sortedHashes = new ArrayList<>();
        ArrayList<byte[]> sortedBytes = new ArrayList<>();
        for (int index : indices) {
            sortedPaths.add(paths.get(index));
            sortedHashes.add(hashes.get(index));
            sortedBytes.add(bytes.get(index));
            sortedNumOfBytes.add(numOfBytes.get(index));
        }
        bytes = sortedBytes;
        return List.of(sortedPaths, sortedNumOfBytes, sortedHashes);
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

    private static String getFileHash(byte[] fileBytes) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}