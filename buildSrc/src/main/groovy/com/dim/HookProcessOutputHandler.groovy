package com.dim;

import com.android.ide.common.blame.ParsingProcessOutputHandler;
import com.android.ide.common.process.BaseProcessOutputHandler;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessOutput
import com.dim.bean.Patch
import com.dim.common.Logger
import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * HookProcessOutputHandler <br/>
 * Created by dim on 2016-07-10.
 */
public class HookProcessOutputHandler extends BaseProcessOutputHandler {

    private ParsingProcessOutputHandler mOutputHandler;
    private Patch patch;
    private File output;

    public HookProcessOutputHandler(ParsingProcessOutputHandler outputHandler, Patch patch, File output) {
        mOutputHandler = outputHandler;
        this.patch = patch;
        this.output = output;
    }

    @Override
    public void handleOutput(ProcessOutput processOutput) throws ProcessException {
        mOutputHandler.handleOutput(processOutput);
        String stdout = processOutput.getStandardOutputAsString();
        if (!stdout.isEmpty()) {
            String info = "";
            String[] split = stdout.split("\n");
            int classIndex = 1;
            for (String item : split) {
                if (item.startsWith("create dex...")) {
                    if (classIndex == 1) {
                        info += "classes.dex\n";
                    } else {
                        info += "classes" + classIndex + ".dex\n";
                    }
                    classIndex++;
                } else {

                    if (!item.startsWith("processing archive")) {
                        info += item.substring(11, item.length() - 3) + "\n";
                    }
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
        String stderr = processOutput.getErrorOutputAsString();
        if (!stderr.isEmpty()) {
            Logger.dim("dex 错误 " + stderr);
        }

    }
}
