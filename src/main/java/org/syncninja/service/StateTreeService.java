package org.syncninja.service;

import org.syncninja.model.Branch;
import org.syncninja.model.Commit;
import org.syncninja.model.StateTree.StateDirectory;
import org.syncninja.model.StateTree.StateFile;
import org.syncninja.model.StateTree.StateRoot;
import org.syncninja.model.StateTree.StateTree;
import org.syncninja.model.commitTree.CommitDirectory;
import org.syncninja.model.commitTree.CommitFile;
import org.syncninja.model.commitTree.CommitNode;
import org.syncninja.repository.StateTreeRepository;
import org.syncninja.util.ResourceBundleEnum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StateTreeService {
    private final StateTreeRepository stateTreeRepository;

    public StateTreeService() {
        stateTreeRepository = new StateTreeRepository();
    }

    public StateTree getStateNode(String path) throws Exception {
        return stateTreeRepository.findById(path).orElseThrow(() ->
                new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.FILE_NOT_FOUND, new Object[]{path})));
    }

    public void generateStateRootNode(String path, Branch currentBranch) throws Exception {
        StateRoot stateRoot = null;
        if (stateTreeRepository.findById(path).isPresent()) {
            throw new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.DIRECTORY_ALREADY_INITIALIZED, new Object[]{path}));
        } else {
            stateRoot = new StateRoot(path, currentBranch);
            stateTreeRepository.save(stateRoot);
        }
    }

    public StateRoot getStateRoot(String path) throws Exception {
        return (StateRoot) stateTreeRepository.findById(path).orElseThrow(
                () -> new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.DIRECTORY_NOT_INITIALIZED, new Object[]{path})));
    }

    public void updateStateRoot(StateRoot stateRoot, Commit newCommit) {
        stateTreeRepository.updateStateRoot(stateRoot, newCommit);
    }

    public void addChangesToStateTree(CommitNode commitNode, StateTree oldStateNode) throws Exception {
        List<CommitNode> commitNodeList = new ArrayList<>();
        StateTree currentStateTree;

        if(commitNode instanceof CommitDirectory){
            currentStateTree = getStateDirectory(commitNode.getPath());
            stateTreeRepository.save(currentStateTree);
            commitNodeList = ((CommitDirectory) commitNode).getCommitNodeList();
        } else {
            currentStateTree = getStateFile(commitNode.getPath());
            CommitFile commitFile = ((CommitFile)commitNode);
            List<String> stateFileLines = ((StateFile)currentStateTree).getLines();

            for(int index = 0; index < commitFile.getLineNumberList().size(); index++){
                String newLine = commitFile.getNewValuesList().get(index);
                int stateFileLineNumber = commitFile.getLineNumberList().get(index)-1;

                if(stateFileLineNumber < stateFileLines.size()){
                    ((StateFile)currentStateTree).getLines().remove(stateFileLineNumber);
                }
                ((StateFile)currentStateTree).getLines().add(stateFileLineNumber, newLine);
            }
            stateTreeRepository.save(currentStateTree);
        }

        if(oldStateNode != null && oldStateNode.isDirectory()){
            ((StateDirectory)oldStateNode).addFile(currentStateTree);
            stateTreeRepository.save(oldStateNode);
        }

        for (CommitNode childCommitNode : commitNodeList) {
            addChangesToStateTree(childCommitNode, currentStateTree);
        }
    }

    private StateFile getStateFile(String path) throws Exception {
        return (StateFile) stateTreeRepository.findById(path).orElse(
                new StateFile(path)
        );
    }

    private StateDirectory getStateDirectory(String path) throws Exception {
        return (StateDirectory) stateTreeRepository.findById(path).orElse(
                new StateDirectory(path)
        );
    }
}