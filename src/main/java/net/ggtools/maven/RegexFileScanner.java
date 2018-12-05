package net.ggtools.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans a root directory for files or directory matching a specific regular expression.
 *
 * @author Christophe Labouisse on 27/07/2015.
 */
public class RegexFileScanner {
	
	private static final long CONTENT_REGEX_SIZE_LIMIT = 16L * 1024L * 1024L;

    public static List<Result> scan(Path root, String regexFilter) throws IOException {
    	return scan(root, regexFilter, null);
    }

    public static List<Result> scan(Path root, String regexFilter, String regexContent) throws IOException {
        RegexFileVisitor visitor = new RegexFileVisitor(root, regexFilter, regexContent);
        Files.walkFileTree(root, visitor);
        return new ArrayList<>(visitor.results);
    }

    public static class Result {

        private final Path path;

        private final String[] groups;
        private final String[] contentGroups;

        public Result(Path path, String[] groups, String[] contentGroups) {
            this.path = path;
            this.groups = groups;
            this.contentGroups = contentGroups;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Result{");
            sb.append("path=").append(path);
            sb.append(", groups=").append(groups);
            sb.append(", contentGroups=").append(contentGroups);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Result result = (Result) o;
            return Objects.equals(path, result.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        public Path getPath() {
            return path;
        }

        public String[] getGroups() {
            return groups;
        }
        
        public String[] getContentGroups() {
			return contentGroups;
		}

    }

    /**
     * @author Christophe Labouisse on 26/07/2015.
     */
    static class RegexFileVisitor extends SimpleFileVisitor<Path> {

        private final Path baseDir;

        private final Pattern filter;
        private final Pattern contentFilter;

        private final Collection<Result> results = new LinkedHashSet<>();

        public RegexFileVisitor(Path baseDir, String filteringRegex) {
        	this(baseDir, filteringRegex, null);
        }

        public RegexFileVisitor(Path baseDir, String filteringRegex, String contentRegex) {
            this.baseDir = baseDir;
            filter = Pattern.compile(filteringRegex);
            contentFilter = contentRegex == null ? null : Pattern.compile(contentRegex);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Objects.requireNonNull(dir);
            Objects.requireNonNull(attrs);
            return addResultIfMatch(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            addResultIfMatch(file);
            return super.visitFile(file, attrs);
        }

        boolean addResultIfMatch(Path path) throws IOException {
            Path relativePath = baseDir.relativize(path);
            Matcher matcher = filter.matcher(relativePath.toString());
            if (matcher.find()) {
            	String[] filenameGroups = this.getGroups(matcher);
            	String[] contentGroups = null;
            	
            	if (this.contentFilter != null) {
            		if (!Files.isDirectory(path) && Files.size(path) < CONTENT_REGEX_SIZE_LIMIT) {
	            		String content = new String(Files.readAllBytes(path));
	            		matcher = this.contentFilter.matcher(content);
	            		if (matcher.find())
	            			contentGroups = this.getGroups(matcher);
            		}
            	}
            	
                results.add(new Result(path.toAbsolutePath(), filenameGroups, contentGroups));
                return true;
            }
            return false;
        }
        
        private String[] getGroups(Matcher matcher) {
            String[] groups = new String[matcher.groupCount() + 1];
            for (int i = 0; i <= matcher.groupCount(); i++)
                groups[i] = matcher.group(i);
            return groups;
        }

    }
}
