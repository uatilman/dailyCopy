

import org.apache.commons.io.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

public class MyFile implements Serializable, Comparable {
    private Path path;
    private List<MyFile> childList;
    private long time;
    private Path absPath;
    private Path trash;
    private Path root;

    private boolean isDir;


    public MyFile(Path path, Path root, Path trash) {
        this.trash = trash;
        this.root = root;
        this.absPath = path;
        this.path = root.toAbsolutePath().relativize(path.toAbsolutePath());

        if (Files.isDirectory(path)) {
            this.isDir = true;
            try (Stream<Path> str = Files.list(path)) {
                this.childList = str
                        .filter(path2 -> !path2.getFileName().toString().equals(trash.getFileName().toString()))
                        .map(path1 -> new MyFile(path1, root, trash))
                        .sorted()
                        .collect(Collectors.toList());
            } catch (IOException e) {
                Main.LOGGER.log(WARNING, "", e);
                e.printStackTrace();
            }
        } else {
            this.isDir = false;
            this.childList = null;
        }

        try {
            this.time = Files.getLastModifiedTime(path, NOFOLLOW_LINKS).toMillis();
        } catch (IOException e) {
            Main.LOGGER.log(WARNING, "", e);
            e.printStackTrace();
        }
    }

    public void sync(MyFile dst) {
        try {
            List<MyFile> removeList = new ArrayList<>();
            for (int i = 0; i < childList.size(); i++) {
                List<MyFile> dstChildList = dst.childList;
                MyFile currentSrc = childList.get(i);
                int index = currentSrc.containByName(dstChildList); // индекс ткущего элемента в целевом списке
                MyFile currentDst = index >= 0 ? dstChildList.get(index) : null;

                Path srcPath = currentSrc.absPath;
                Path dstPath = currentDst != null ?
                        currentDst.absPath :
                        Paths.get(dst.root.toString(), currentSrc.path.toString());

                if (currentSrc.isDir) { // если папка
                    try {
                        if (currentDst != null) {// если папка существует
                            currentSrc.sync(currentDst);
                        } else { //если имени в целевом списке нет - копируем папку целиком
                            Files.createDirectory(dstPath);
                            Main.LOGGER.log(INFO, dstPath + " - папка не найдена. Папка создана заново и начато копирования");
                            FileUtils.copyDirectory(srcPath.toFile(), dstPath.toFile());
                        }
                        Files.setLastModifiedTime(dstPath, FileTime.fromMillis(currentSrc.time));
                    } catch (NoSuchFileException e) {
                        Main.LOGGER.log(WARNING, "Длинное имя or ...", e);
                    }
                } else { // если файл
                    if (!dstChildList.contains(currentSrc)) { // если файлы не одинаковые
                        if (currentDst != null) {// если совпадают имена
                            if (currentSrc.isNewer(currentDst)) { // если исходный файл новее
                                moveOnce(currentDst);
                                Main.LOGGER.log(INFO, currentDst + " перенесен в корзину");
                                FileUtils.copyFile(srcPath.toFile(), dstPath.toFile());
                                Main.LOGGER.log(INFO, currentDst + " обновлен");
                            } else { //новее файл в резервной копии
                                Main.LOGGER.log(INFO, "В резервном хранилище файл новее. " +
                                        "\n\t Исхожный файл: " + currentSrc + "-  " + FileTime.fromMillis(currentSrc.time) +
                                        "\n\t Резервный файл: " + currentDst + "-  " + FileTime.fromMillis(currentDst.time));
                            }
                        } else { // если нет резервной копии файла
                            FileUtils.copyFile(srcPath.toFile(), dstPath.toFile(), true);
                            Main.LOGGER.log(INFO, srcPath + " не найден и скопирован.");
                        }
                    }
                }
                removeList.add(currentDst);
            }

            dst.childList.removeAll(removeList);
            if (dst.childList.size() != 0) {
                Main.LOGGER.log(INFO, "в резервном листе остались файлы :" + dst.childList + "которые будут перенесены в корзину");
                dst.childList.forEach(this::moveOnce);
            }
        } catch (IOException e) {
            Main.LOGGER.log(WARNING, "", e);
            e.printStackTrace();
        }
    }


    private void moveOnce(MyFile dst) {

        try {
            FileUtils.moveToDirectory(dst.absPath.toFile(), trash.toFile(), true);
            Main.LOGGER.log(INFO, "\t" + dst.absPath + " перенесен в корзину");

            File dirInTrashOldName = Paths.get(trash.toString(), dst.path.getFileName().toString()).toFile();
            StringBuilder newFileName = new StringBuilder();
            newFileName
                    .append(FilenameUtils.getBaseName(dst.path.toString()))
                    .append("_")
                    .append(new SimpleDateFormat("YYYY.MM.dd_H.mm.s").format(Calendar.getInstance().getTime()));
            if (!dst.isDir) {
                newFileName
                        .append(".")
                        .append(FilenameUtils.getExtension(dst.path.toString()));
            }

            File dirInTrashNewName = Paths.get(trash.toString(), newFileName.toString()).toFile();

            boolean renameResult = dirInTrashOldName.renameTo(dirInTrashNewName);
            Main.LOGGER.log(INFO, " rename " + dirInTrashOldName + " to " + dirInTrashNewName + " result: " + renameResult);

        } catch (IOException e) {
            Main.LOGGER.log(WARNING, "", e);
//            e.printStackTrace();
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

    public boolean isNewer(MyFile another) {
        return time > another.time;
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

    public Path getPath() {
        return path;
    }

}