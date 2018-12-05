package net.ggtools.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractRegexMojo extends AbstractMojo {

	private final String goal;
	
    @Parameter(defaultValue = "${project.basedir}")
    protected File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}")
    protected File destinationDirectory;

    @Parameter(property = "regex-copy.source", required = true)
    protected String source;
    
    @Parameter(property = "regex-copy.sourceContent")
    protected String sourceContent;

    @Parameter(property = "regex-copy.destination", required = true)
    protected String destination;

    @Parameter(property = "regex-copy.overwrite", defaultValue = "false")
    protected boolean overwrite;
    
    public AbstractRegexMojo(String goal) {
    	this.goal = goal;
	}

    public void execute() throws MojoExecutionException {
        getLog().info("Performing regex-copy:" + this.goal);
        getLog().debug("Getting candidate files and directories");
        try {
            List<RegexFileScanner.Result> results = RegexFileScanner.scan(sourceDirectory.toPath(), source, this.sourceContent);
            getLog().info("Got " + results.size() + " files or directories");
            getLog().debug("Results: " + results);
            for (RegexFileScanner.Result result : results) {
                copyResult(result);
            }
        } catch (IOException e) {
            String msg = "Error while retrieving candidate files";
            getLog().error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }

    private Path mapResultToDestination(RegexFileScanner.Result result) {
    	int groupCount = result.getGroups().length;
        String resultDest = destination;
        for (int i = 0; i < groupCount; i++) {
            String group = result.getGroups()[i];
            if (group != null) {
                resultDest = resultDest.replaceAll("\\{" + i + "\\}", group);
            }
        }
        if (result.getContentGroups() != null) {
	        for (int i = 0; i < result.getContentGroups().length; i++) {
	            String group = result.getContentGroups()[i];
	            if (group != null) {
	                resultDest = resultDest.replaceAll("\\{" + (i+groupCount) + "\\}", group);
	            }
	        }
        }
        return destinationDirectory.toPath().resolve(resultDest);
    }

    private void copyResult(RegexFileScanner.Result result) throws IOException {
    	Path destPath = mapResultToDestination(result).toAbsolutePath();
        if (!Files.isRegularFile(result.getPath())) {
            getLog().warn("Only files are accepted at the moment skipping " + result.getPath());
            return;
        }

        if (!Files.exists(destPath.getParent())) {
            Files.createDirectories(destPath.getParent());
        }
        
        this.operateOnResult(result.getPath(), destPath);
    }
    
    protected abstract void operateOnResult(Path source, Path target) throws IOException;
    
}
