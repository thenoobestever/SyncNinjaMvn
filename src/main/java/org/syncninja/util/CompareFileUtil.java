package org.syncninja.util;

import org.syncninja.dto.FileStatusEnum;
import org.syncninja.dto.StatusFileDTO;

import java.util.ArrayList;
import java.util.List;

public class CompareFileUtil {

    public static LinesContainer compareFiles(String filePath, StatusFileDTO statusFileDTO) throws Exception {
        List<String> newFileList = new ArrayList<>();
        if (statusFileDTO.getFileStatus() != FileStatusEnum.IS_DELETED){
            newFileList = Fetcher.readFile(filePath);
        }
        List<String> oldFileList;
        if (statusFileDTO.getStateFile() != null) {
            oldFileList = statusFileDTO.getStateFile().getLines();
        } else {
            oldFileList = new ArrayList<>();
        }
        return compareNewAndOldLists(newFileList, oldFileList);
    }

    public static LinesContainer compareNewAndOldLists(List<String> newFileList, List<String> oldFileList) {
        LinesContainer linesContainer = new LinesContainer(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        if(!newFileList.isEmpty() && oldFileList.isEmpty()) {
            for(int index = 0; index < newFileList.size(); index++){
                linesContainer.getLineNumbers().add(index + 1);
                linesContainer.getNewLines().add(newFileList.get(index));
                linesContainer.getOldLines().add("");
            }
            return linesContainer;
        }
        int maximumLength = Math.max(newFileList.size(), oldFileList.size());

        int lineNumber = 1;
        String newLine, oldLine;

        while (lineNumber <= maximumLength) {
            if (lineNumber > newFileList.size()) {
                newLine = "";
            } else {
                newLine = newFileList.get(lineNumber - 1);
            }

            if (lineNumber > oldFileList.size()) {
                oldLine = "";
            } else {
                oldLine = oldFileList.get(lineNumber - 1);
            }

            if (!newLine.equals(oldLine)) {
                linesContainer.getLineNumbers().add(lineNumber);
                linesContainer.getNewLines().add(newLine);
                linesContainer.getOldLines().add(oldLine);
            }

            lineNumber++;
        }
        return linesContainer;
    }
}
