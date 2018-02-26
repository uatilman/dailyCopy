package tests;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public class MyFile implements Serializable, Comparable {
    //    private Map<String, Object> attributeMap;
    private Path path;
    private List<MyFile> childList;
    private long time;
    private Path root;
    private boolean isDir;

    public MyFile(Path path, Path root) {
        this.path = root.toAbsolutePath().relativize(path.toAbsolutePath());
        this.root = root;

        if (Files.isDirectory(path)) {
            this.isDir = true;
            try {
                this.childList = Files.list(path).map((Path path1) -> new MyFile(path1, root)).collect(Collectors.toList());
                Collections.sort(childList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.isDir = false;
            this.childList = null;
        }

        try {
            this.time = Files.getLastModifiedTime(path, NOFOLLOW_LINKS).toMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sync(MyFile dst) {
        List<MyFile> removeList = new ArrayList<>();

        for (int i = 0; i < childList.size(); i++) {
            MyFile currentSrc = childList.get(i);
            if (currentSrc.isDir) { // если папка
                if (dst.getChildList().contains(currentSrc)) { //если название и время изменения папки совпадают
                    currentSrc.sync(dst.getChildList().get(dst.getChildList().indexOf(currentSrc))); // далее рекурсию можно будет удалить, когда появится изменение даты папки
                    removeList.add(currentSrc);
                    continue;

                } else { // если полного совпадения нет
                    if (currentSrc.containName(dst.getChildList())){// если совпадают имена todo containName - вовзращать -1 если лож,
                        // сравнение по времени todo а лучше уйти в рекурсию
                    } else { //если имени в целевом списке нет
                        //копирвать папку целиком //todo вопрос, отпал, дочки сами уйдут
                    }




                }

            } else { // если файл

            }

//
//            else if (dst.getChildList().contains(currentSrc)) {
//                removeSrcList.add(currentSrc);
//                removeDstList.add(currentSrc);
//                continue;
//            } else {
//
//            }


        }
    }

    public boolean containName(List<MyFile> another) {
        if (another == null) return false;
        for (MyFile m : another) {
            if (m.getPath().toString().equals(getPath().toString())) return true;
        }
        return false;
    }

    public boolean isDir() {
        return isDir;
    }

    public boolean isNewer(MyFile another) {
        return time > another.time;
    }

    public boolean isOlder(MyFile another) {
        return time < another.time;
    }

    public List<MyFile> getChildList() {
        return childList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyFile myFile = (MyFile) o;

        if (time != myFile.time) return false;
        return path.equals(myFile.path);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return path.toString();
    }

    public void setChildList(List<MyFile> childList) {
        this.childList = childList;
    }

    public long getTime() {
        return time;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }


//
//    //TODO структура данных с вложением в MyFile всех внутренностей
//    public static List<MyFile> getFileTreePaths(Path path, boolean isAbsoluteTree) throws Exception {
//
//        List<MyFile> myFiles = new ArrayList<>();
//        Files.walkFileTree(path, new FileVisitor<Path>() {
//            @Override
//            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                if (dir.equals(path)) return FileVisitResult.CONTINUE;
//                if (isAbsoluteTree) myFiles.add(new MyFile(dir));
//                else myFiles.add(new MyFile(dir, path));
//
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                if (isAbsoluteTree)
//                    myFiles.add(new MyFile(file));
//                else myFiles.add(new MyFile(file, path));
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//                return FileVisitResult.CONTINUE;
//            }
//        });
//        return myFiles;
//    }
}

//    public MyFile(Map<String, Object> attributeMap, Path path) {
//        this.attributeMap = attributeMap;
//        this.path = path.toAbsolutePath();
//    }
//
//    public MyFile(Path path) throws IOException {
//        this.attributeMap = Files.readAttributes(path, "*", LinkOption.NOFOLLOW_LINKS);
//        this.path = path.toAbsolutePath();
//    }
//
//    public MyFile(Path path, Path root) throws IOException {
//        this.attributeMap = Files.readAttributes(path, "*", LinkOption.NOFOLLOW_LINKS);
//        this.path = root.toAbsolutePath().relativize(path.toAbsolutePath());
//    }

//    @Override
//    public String toString() {
//        StringBuilder builder = new StringBuilder();
//        builder.append("\nFile: [").append(path).append("]\n");
//        for (Map.Entry<String, Object> kv : attributeMap.entrySet()) {
//            builder.append(kv.getKey()).append(" --- ").append(kv.getValue()).append("\n");
//        }
//        return builder.toString();
//    }

//    @Override
//    public boolean equals(Object obj) {
//        MyFile myFile = (MyFile) obj;
//        return this.getPath().equals(myFile.getPath()) &&
//                (this.getAttributeMap().get("isDirectory").equals(true) || myFile.getAttributeMap().get("isDirectory").equals(true) ||
//                        (this.getAttributeMap().get("lastModifiedTime").equals(myFile.getAttributeMap().get("lastModifiedTime")) &&
//                                this.getAttributeMap().get("size").equals(myFile.getAttributeMap().get("size"))
//                        )
//                );
//    }
//
//    @Override
//    public int hashCode() {
//        return getPath().hashCode();
//    }