package com.dim;

import com.android.ide.common.blame.ParsingProcessOutputHandler;
import com.android.ide.common.process.BaseProcessOutputHandler;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessOutput;

import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * HookProcessOutputHandler <br/>
 * Created by dim on 2016-07-10.
 */
public class HookProcessOutputHandler extends BaseProcessOutputHandler {

    private ParsingProcessOutputHandler mOutputHandler;
    private com.dim.bean.Patch patch;
    private java.io.File output;

    public HookProcessOutputHandler(ParsingProcessOutputHandler outputHandler, com.dim.bean.Patch patch, File output) {
        mOutputHandler = outputHandler;
        this.patch = patch;
        this.output = output;
    }

    @Override
    public void handleOutput(ProcessOutput processOutput) throws ProcessException {
        mOutputHandler.handleOutput(processOutput);
        if (processOutput instanceof BaseProcessOutput) {
            BaseProcessOutput impl = (BaseProcessOutput) processOutput;
            String stdout = impl.getStandardOutputAsString();
            if (!stdout.isEmpty()) {
                String info = "";
                String[] split = stdout.split("\n");
                int classIndex = 1;
                for (String item : split) {
                    if (item.startsWith("processing archive")) {
                        if (classIndex == 1) {
                            info += "classes.dex\n";
                        } else {
                            info += "classes" + classIndex + ".dex\n";
                        }
                        classIndex++;
                    } else {

                        info += item.substring(11, item.length() - 3) + "\n";
                    }
                }

                try {
                    FileUtils.writeStringToFile(patch.getDexInfoFile(), info);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    FileUtils.copyDirectory(output, patch.getClassFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String stderr = impl.getErrorOutputAsString();
            if (!stderr.isEmpty()) {
                System.out.println("-------stderr---: " + stderr);
            }
        } else {
        }
    }
}
