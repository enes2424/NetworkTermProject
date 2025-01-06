import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.DefaultListModel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class FolderOperation {
	public static ArrayList<byte[]>         bytes;
	public static int				        totalnumOfBytes;
    public static DefaultListModel<String>	listModel1;

    public static List<List<String>> getAllFileInformations(Path baseFolderPath, File folder, ArrayList<String> filters) throws IOException, NoSuchAlgorithmException {
        ArrayList<String>	paths = new ArrayList<>();
        ArrayList<String>	numOfBytes = new ArrayList<>();
        ArrayList<String>	hashes = new ArrayList<>();
        List<File> files;

        totalnumOfBytes = 0;
        bytes = new ArrayList<>();

        if (folder.isDirectory())
            files = getAllFiles(folder, baseFolderPath);
        else
            files = List.of(folder);

        x: for (File file : files) {
            if (filters != null)
                for (String filter : filters)
                    if (isMatchedFileAndMask(file.getName(), filter))
                        continue x;
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

    private static List<File> getAllFiles(File folder, Path baseFolderPath) {
        List<File> fileList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!listModel1.contains(baseFolderPath.relativize(file.toPath()).toString()))
                        fileList.addAll(getAllFiles(file, baseFolderPath));
                } else
                    fileList.add(file);
            }
        return fileList;
    }

    public static List<File>  getAllFilesAndFolders(File folder) {
        List<File> fileList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files) {
                fileList.add(file);
                if (file.isDirectory())
                    fileList.addAll(getAllFilesAndFolders(file));
            }
        return fileList;
    }

    public static List<String>  getAllFolders(DefaultListModel<String>	listModel, Path baseFolderPath, File folder) {
        List<String> folderList = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files)
                if (file.isDirectory() && !listModel.contains(baseFolderPath.relativize(file.toPath()).toString())) {
                    folderList.add(baseFolderPath.relativize(file.toPath()).toString());
                    folderList.addAll(getAllFolders(listModel, baseFolderPath, file));
                }
        return folderList;
    }

    private static String getFileHash(byte[] fileBytes) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static boolean isMatchedFileAndMask(String fileName, String mask) {
        int i1 = 0, i2 = 0, len1 = fileName.length(), len2 = mask.length();
        while (i1 < len1 && i2 < len2)
        {
            if (mask.charAt(i2) == '*')
                break;
            if (mask.charAt(i2) != '?' && fileName.charAt(i1) != mask.charAt(i2)) {
                return false;
            }
            i1++;
            i2++;
        }
        while (i2 < len2 && mask.charAt(i2) == '*')
            i2++;
        if (i1 == len1)
            return i2 == len2;
        if (i2 == len2)
            return mask.charAt(i2 - 1) == '*';
        while (true) {
            if (fileName.charAt(i1) == mask.charAt(i2) &&
                isMatchedFileAndMask(fileName.substring(i1), mask.substring(i2)))
                    return true;
            if (++i1 == len1)
                return false;
        }
    }
}
