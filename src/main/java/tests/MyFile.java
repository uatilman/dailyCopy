package tests;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
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

    public void sync(MyFile dst) throws IOException {
        List<MyFile> removeList = new ArrayList<>();

        for (int i = 0; i < childList.size(); i++) {
            MyFile currentSrc = childList.get(i);
            List<MyFile> dstChildList = dst.childList;
            if (currentSrc.isDir) { // если папка

                int index = currentSrc.containByName(dstChildList);

//                if (dstChildList.contains(currentSrc)) { //если название и время изменения папки совпадают
//                    removeList.add(currentSrc);
//
//                    currentSrc.sync(dstChildList.get(index));

//                } else { // если полного совпадения нет

                if (index >= 0) {// если совпадают имена  уходим в рекурсию
                    MyFile currentDst = dstChildList.get(index);
                    currentSrc.sync(currentDst);
                    Files.setLastModifiedTime(dst.root.resolve(dst.getPath()), FileTime.fromMillis(currentSrc.time)); //актуально для случая несовпадения по time !dstChildList.contains(currentSrc)
                    removeList.add(currentDst);
                } else { //если имени в целевом списке нет - копируем папку целиком
                    Path srcDir = currentSrc.root.resolve(currentSrc.path);
                    Path newDir = Paths.get(dst.root.resolve(dst.path).toString(), currentSrc.path.toString());
                    Files.createDirectory(newDir);
                    FileUtils.copyDirectory(
                            srcDir.toFile(),
                            newDir.toFile(),
                            true
                    );
                    Files.setLastModifiedTime(newDir, FileTime.fromMillis(currentSrc.time));
                    removeList.add(currentSrc);
                }
//                }

            } else { // если файл
                if (dstChildList.contains(currentSrc)) { // если файлы одинаковые todo вынести для файла и папки в первый блок цикла
                    removeList.add(currentSrc);

                } else { // если полного совпадения нет
                    int index;
                    if ((index = currentSrc.containByName(dstChildList)) >= 0) {// если совпадают имена
                        MyFile currentDst = dstChildList.get(index);
                        if (currentSrc.isNewer(currentDst)) { // если исходный файл новее
                            System.out.println("debug before Delete ");
                            removeList.add(currentDst);//todo из-за этой строчки приходится прописывать в каждом условии

                            Path dstPath = currentDst.root.resolve(currentDst.path);
                            Path srcPath = currentSrc.root.resolve(currentSrc.path);

                            Files.delete(dstPath);
                            FileUtils.copyFile(srcPath.toFile(), dstPath.toFile());
                        } else { //новее файл в резервной копии
                            // этого неможет быть
                            System.err.println(
                                    "В резервном хранилище файл новее. " +
                                            "\n\t Исхожный файл: " + currentSrc + "-  " + FileTime.fromMillis(currentSrc.time) +
                                            "\n\t Резервный файл: " + currentDst + "-  " + FileTime.fromMillis(currentDst.time));
                        }
                    } else { // если нет резервной копии файла
                        Path srcPath = currentSrc.root.resolve(currentSrc.path);
                        Path dstPath = Paths.get(dst.root.resolve(dst.path).toString(), currentSrc.path.toString());

                        FileUtils.copyFile(srcPath.toFile(), dstPath.toFile(), true);
                    }
                }
            }
        }

        dst.childList.removeAll(removeList);
        if (dst.childList.size() != 0) {
            System.err.println("в резервном листе остались файды \n" + dst.childList);
        }

    }

    public int containByName(List<MyFile> another) {
        if (another == null) return -1;
        for (int i = 0; i < another.size(); i++) {
            MyFile m = another.get(i);
            if (m.getPath().toString().equals(getPath().toString())) return i;
        }
        return -1;
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