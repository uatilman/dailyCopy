package tests;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyFile implements Serializable {
    private Map<String, Object> attributeMap;
    private Path path;
    private  List<MyFile> childList;

    public MyFile(Map<String, Object> attributeMap, Path path) {
        this.attributeMap = attributeMap;
        this.path = path.toAbsolutePath();
    }

    public MyFile(Path path) throws IOException {
        this.attributeMap = Files.readAttributes(path, "*", LinkOption.NOFOLLOW_LINKS);
        this.path = path.toAbsolutePath();
    }

    public MyFile(Path path, Path root) throws IOException {
        this.attributeMap = Files.readAttributes(path, "*", LinkOption.NOFOLLOW_LINKS);
        this.path = root.toAbsolutePath().relativize(path.toAbsolutePath());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\nFile: [").append(path).append("]\n");
        for (Map.Entry<String, Object> kv : attributeMap.entrySet()) {
            builder.append(kv.getKey()).append(" --- ").append(kv.getValue()).append("\n");
        }
        return builder.toString();
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Map<String, Object> getAttributeMap() {
        return attributeMap;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        MyFile myFile = (MyFile) obj;
        return this.getPath().equals(myFile.getPath()) &&
                (this.getAttributeMap().get("isDirectory").equals(true) || myFile.getAttributeMap().get("isDirectory").equals(true) ||
                        (this.getAttributeMap().get("lastModifiedTime").equals(myFile.getAttributeMap().get("lastModifiedTime")) &&
                                this.getAttributeMap().get("size").equals(myFile.getAttributeMap().get("size"))
                        )
                );
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }
//TODO структура данных с вложением в MyFile всех внутренностей
    public static List<MyFile> getFileTreePaths(Path path, boolean isAbsoluteTree) throws Exception {

        List<MyFile> myFiles = new ArrayList<>();
        Files.walkFileTree(path, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.equals(path)) return FileVisitResult.CONTINUE;
                if (isAbsoluteTree) myFiles.add(new MyFile(dir));
                else myFiles.add(new MyFile(dir, path));

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isAbsoluteTree)
                    myFiles.add(new MyFile(file));
                else myFiles.add(new MyFile(file, path));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return myFiles;
    }
}
