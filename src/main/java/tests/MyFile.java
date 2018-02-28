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
    private Path path;
    private List<MyFile> childList;
    private long time;
    private Path absPath;

    private boolean isDir;

    public MyFile(Path path, Path root) {

        this.absPath = path;
        this.path = root.toAbsolutePath().relativize(path.toAbsolutePath());

        if (Files.isDirectory(path)) {
            this.isDir = true;
            try {
                this.childList = Files.list(path).filter(MyFile::isHidden).map(path1 -> new MyFile(path1, root)).sorted().collect(Collectors.toList());
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

    private static boolean isHidden(Path path) {
        return !path.getFileName().toString().startsWith("~");
    }

    public void sync(MyFile dst) throws IOException {
        List<MyFile> removeList = new ArrayList<>();

        for (int i = 0; i < childList.size(); i++) {
            List<MyFile> dstChildList = dst.childList;
            MyFile currentSrc = childList.get(i);
            int index = currentSrc.containByName(dstChildList); // индекс ткущего элемента в целевом списке
            MyFile currentDst = index >= 0 ? dstChildList.get(index) : null;

            Path srcPath = currentSrc.absPath;
            Path dstPath = currentDst != null ?
                    currentDst.absPath :
                    Paths.get(dst.absPath.toString(), currentSrc.path.toString());
            // TODO: 28.02.2018 отдельный блок вначале синхронизации с проверкой на полное совпадение - логично только для файлов, т.к. сравнение папок по дате и названию ненадежно
            // TODO: 28.02.2018 перенос в корзину вместо удаления - если перенос без архива, операция быстрая т.к. файл меняется не сильно.

            if (currentSrc.isDir) { // если папка
                if (currentDst != null) {// если папка существует
                    currentSrc.sync(currentDst);
                } else { //если имени в целевом списке нет - копируем папку целиком
                    Files.createDirectory(dstPath);
                    FileUtils.copyDirectory(srcPath.toFile(), dstPath.toFile());
                }
                Files.setLastModifiedTime(dstPath, FileTime.fromMillis(currentSrc.time));

            } else { // если файл
                if (!dstChildList.contains(currentSrc)) { // если файлы не одинаковые
                    if (currentDst != null) {// если совпадают имена
                        if (currentSrc.isNewer(currentDst)) { // если исходный файл новее
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
                        FileUtils.copyFile(srcPath.toFile(), dstPath.toFile(), true);
                    }
                }
            }
            removeList.add(currentDst);
        }
        dst.childList.removeAll(removeList);
        if (dst.childList.size() != 0) {
            System.err.println("в резервном листе остались файлы \n" + dst.childList);
            dst.childList.forEach(myFile -> {
                try {
                    FileUtils.forceDelete(myFile.absPath.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


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