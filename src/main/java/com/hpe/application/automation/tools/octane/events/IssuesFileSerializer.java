package com.hpe.application.automation.tools.octane.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.dto.SecurityScans.OctaneIssue;
import hudson.model.Run;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssuesFileSerializer {
    private String targetDir;
    private List<OctaneIssue> octaneIssues;
    public IssuesFileSerializer(String targetDir, List<OctaneIssue> issues){
        this.targetDir = targetDir;
        this.octaneIssues = issues;
    }

    public void doSerialize() {
        try{
            Map dataFormat = new HashMap<>();
            dataFormat.put("data",octaneIssues);
            String vulnerabilitiesScanFilePath = targetDir + File.separator + SSCHandler.SCAN_RESULT_FILE;
            PrintWriter fw = new PrintWriter(vulnerabilitiesScanFilePath, "UTF-8");
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(fw,dataFormat);
            fw.flush();
            fw.close();
        }catch(Exception e){
            System.out.println(e);
        }

    }
}