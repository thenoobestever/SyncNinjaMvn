package org.syncninja.service;

import org.syncninja.dto.StatusFileDTO;
import org.syncninja.model.commitTree.CommitDirectory;
import org.syncninja.model.commitTree.CommitFile;
import org.syncninja.model.commitTree.CommitNode;
import org.syncninja.repository.CommitNodeRepository;
import org.syncninja.util.CompareFileUtil;
import org.syncninja.util.FileTrackingState;
import org.syncninja.util.LinesContainer;
import org.syncninja.util.ResourceBundleEnum;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class CommitTreeService {
    private final StatusService statusService;
    private final CommitNodeRepository commitNodeRepository;
    private final CommitService commitService;

    public CommitTreeService() {
        this.statusService = new StatusService();
        this.commitNodeRepository = new CommitNodeRepository();
        this.commitService = new CommitService();
    }

    public void addFilesFromDirectoryToCommitTree(String directoryPath) throws Exception {
        FileState fileState = statusService.getStatus(directoryPath);
        List<String> untracedFiles =fileState.getUntracked();
        addFilesToCommitTree(untracedFiles,directoryPath);
    }

    public void addDirectoryToCommitTree(String directoryPath) throws IOException {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("The provided path is not a directory: " + directoryPath);
        }
        List<String> filePaths = Files.walk(Path.of(directoryPath))
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .toList();

        addFilesToCommitTree(filePaths, directoryPath);

        File[] subDirectories = directory.listFiles(File::isDirectory);
        if (subDirectories != null) {
            for (File subDirectory : subDirectories) {
                addDirectoryToCommitTree(subDirectory.getAbsolutePath());
            }
        }
    }
    public void addFileToCommitTree(String filePath) {
        if (!Files.isRegularFile(Path.of(filePath))) {
            throw new IllegalArgumentException("The provided path is not a regular file: " + filePath);
        }
        addFilesToCommitTree(Collections.singletonList(filePath), new File(filePath).getParent());
    }
    private void addFilesToCommitTree(List<String> filePaths, String mainDirectoryPath) {
        CommitNode root = new CommitDirectory(mainDirectoryPath);

    public void addFilesFromDirectoryToCommitTree(String directoryPath) throws Exception {
        FileTrackingState fileTrackingState = statusService.getState(directoryPath);
        List<StatusFileDTO> untrackedFiles = fileTrackingState.getUntracked();
        addFilesToCommitTree(untrackedFiles, directoryPath);
    }

//    public void addDirectoryToCommitTree(String directoryPath) throws IOException {
//        File directory = new File(directoryPath);
//        if (!directory.isDirectory()) {
//            throw new IllegalArgumentException("The provided path is not a directory: " + directoryPath);
//        }
//        List<String> filePaths = Files.walk(Path.of(directoryPath))
//                .filter(Files::isRegularFile)
//                .map(Path::toString)
//                .toList();
//
//        addFilesToCommitTree(filePaths, directoryPath);
//
//
//        File[] subDirectories = directory.listFiles(File::isDirectory);
//        if (subDirectories != null) {
//            for (File subDirectory : subDirectories) {
//                addDirectoryToCommitTree(subDirectory.getAbsolutePath());
//            }
//        }
//    }

    public void addFileToCommitTree(String filePath) {
        if (!Files.isRegularFile(Path.of(filePath))) {
            throw new IllegalArgumentException(ResourceMessagingService.getMessage(ResourceBundleEnum.PATH_NOT_FILE, new Object[]{filePath}));
        }

        //addFilesToCommitTree(,new File(filePath).getParent());
    }
    private void addFilesToCommitTree(List<StatusFileDTO> statusFileDTOs, String mainDirectoryPath) throws Exception {
        CommitNode root = new CommitDirectory(mainDirectoryPath);

        for (StatusFileDTO statusFileDTO : statusFileDTOs) {
            String relativePath = statusFileDTO.getPath().substring(mainDirectoryPath.length() + 1);
            String[] pathComponents = relativePath.split("\\\\");
            CommitNode currentNode = root;
            String previousPath = mainDirectoryPath;
            for (String component : pathComponents) {
                previousPath = previousPath + "\\" + component;
                boolean found = false;
                if (currentNode instanceof CommitDirectory && ((CommitDirectory) currentNode).getCommitNodeList() != null) {
                    for (CommitNode child : ((CommitDirectory) currentNode).getCommitNodeList()) {
                        if (child.getPath().equals(previousPath)) {
                            currentNode = child;
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    CommitNode newNode;
                    if (isFile(previousPath)) {
                        LinesContainer linesContainer = CompareFileUtil.compareFiles(previousPath, statusFileDTO);
                        newNode = new CommitFile(previousPath, linesContainer.getLineNumbers(), linesContainer.getNewLines(), linesContainer.getOldLines());
                    } else {
                        newNode = new CommitDirectory(previousPath);
                    }
                    ((CommitDirectory) currentNode).getCommitNodeList().add(newNode);
                    currentNode = newNode;
                }
            }
        }
        commitNodeRepository.save(root);
    }
    private void addFilesToCommitTree(String path) throws Exception {
    }
        private boolean isFile(String path) {
        return new File(path).isFile();
    }
}
}