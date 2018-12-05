package net.ggtools.maven;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "regex-copy")
public class RegexCopyMojo extends AbstractRegexMojo {

    private static final CopyOption[] COPY_OPTIONS_WITH_OVERWRITE = new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING};

    private static final CopyOption[] COPY_OPTIONS_WITHOUT_OVERWRITE = new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};
    
    public RegexCopyMojo() {
		super("regex-copy");
	}
    
    @Override
    protected void operateOnResult(Path source, Path target) throws IOException {
        try {
            Files.copy(source, target, overwrite ? COPY_OPTIONS_WITH_OVERWRITE : COPY_OPTIONS_WITHOUT_OVERWRITE);
        } catch (FileAlreadyExistsException e) {
            getLog().warn("File " + target + " already exists, skipping");
            getLog().debug(e);
        }
    }
    
}
