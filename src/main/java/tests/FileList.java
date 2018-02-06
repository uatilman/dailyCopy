package tests;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

public class FileList {
    private static final Path SRC = Paths.get("srcFiles");
    private static final Path DST = Paths.get("dstFiles");

    public static void main(String[] args) throws Exception {

        List<MyFile> myFilesSrc = MyFile.getFileTreePaths(SRC, false);
        List<MyFile> myFilesDst = MyFile.getFileTreePaths(DST, false);
        System.out.println(myFilesSrc);
        myFilesSrc.sort(Comparator.comparing(MyFile::getPath));
        myFilesDst.sort(Comparator.comparing(MyFile::getPath));

        ListIterator<MyFile> iteratorSrc = myFilesSrc.listIterator();
        ListIterator<MyFile> iteratorDst = myFilesDst.listIterator();

        //TODO если в обратном
        while (iteratorSrc.hasNext() || iteratorDst.hasNext()) {
            if (!iteratorSrc.hasNext()) {
                // если в исходном списке нет файлов, удаляем все файлы, который остались в
                while (iteratorDst.hasNext()) {
                    MyFile currentMyFileDst = iteratorDst.next();
//==================================================   TO METHOD
                    List<MyFile> removeList = removeAll(currentMyFileDst, DST);

                    int previousDstIteratorIndex = iteratorDst.previousIndex();
                    while (!removeList.isEmpty() && iteratorDst.hasNext()) {
                        if (removeList.contains(iteratorDst.next())) iteratorDst.remove();
                    }

                    int i = 0;
                    while (previousDstIteratorIndex != iteratorDst.previousIndex()) {
                        System.out.println("previous " + i);
                        iteratorDst.previous();
                        i++;
                    }
                    iteratorSrc.previous();
//======================
                }
                break;
            }

            if (!iteratorDst.hasNext()) {
                //если в целевом списке нет файлов копируем все
                while (iteratorSrc.hasNext()) {
                    Path currentSrc = iteratorSrc.next().getPath();
                    Files.copy(
                            SRC.resolve(currentSrc),
                            DST.resolve(currentSrc),
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
                break;
            }

            MyFile currentMyFileSrc = iteratorSrc.next();
            MyFile currentMyFileDst = iteratorDst.next();

            Path currentSrc = currentMyFileSrc.getPath();
            Path currentDst = currentMyFileDst.getPath();
//TODO обернуть в equals. Compare MyFiles
            if (currentSrc.compareTo(currentDst) < 0) {
                // в currentDst отсутствует файл - копировать его, добавить в список, - сравнить после добавление где будет итератор
                Files.copy(
                        SRC.resolve(currentSrc),
                        DST.resolve(currentSrc),
                        StandardCopyOption.COPY_ATTRIBUTES);
                iteratorDst.previous();
                iteratorDst.add(currentMyFileSrc);
//                iteratorDst.next();
            } else if (currentSrc.compareTo(currentDst) > 0) {
                // в currentDst есть файл, который удален

                if (!Files.isDirectory(DST.resolve(currentDst))) {
                    Files.delete(DST.resolve(currentDst));
                    iteratorDst.remove();
//                    iteratorDst.previous();
                    iteratorSrc.previous(); // сбрасываем итератор исходного назад
                } else {
//======================================= TO METHOD

                    List<MyFile> removeList = removeAll(currentMyFileDst, DST);

                    int previousDstIteratorIndex = iteratorDst.previousIndex();
                    while (!removeList.isEmpty() && iteratorDst.hasNext()) {
                        if (removeList.contains(iteratorDst.next())) iteratorDst.remove();
                    }


                    int i = 0;
                    while (previousDstIteratorIndex != iteratorDst.previousIndex()) {
                        System.out.println("previous " + i);
                        iteratorDst.previous();
                        i++;
                    }
                    iteratorSrc.previous();
//==========================================
                }
            } else {
                if (Files.isDirectory(currentDst) && Files.isDirectory(currentDst)) {
                    //TODO установить в целевую директорию теже атрибуты
                }
            }
        }
    }

    public static List<MyFile> removeAll(MyFile myFile, Path root) throws Exception {
        List<MyFile> myFiles = MyFile.getFileTreePaths(root.resolve(myFile.getPath()), true);
        List<MyFile> removeList = new ArrayList<>();
        ListIterator<MyFile> iterator = myFiles.listIterator();

        while (iterator.hasNext() && !myFiles.isEmpty()) {
            MyFile temp = iterator.next();
            try {
                Files.delete(temp.getPath());
                temp.setPath(root.toAbsolutePath().relativize((temp.getPath().toAbsolutePath())));
                removeList.add(temp);//задать относительный путь
                iterator.remove();
            } catch (DirectoryNotEmptyException e) {
                continue;
            }
            if (!iterator.hasNext()) iterator = myFiles.listIterator();
        }
        removeList.add(myFile);
        Files.delete(root.resolve(myFile.getPath()));
        return removeList;
    }

    @Deprecated
    public static boolean isEqual(Path currentSrc, Path currentDst) throws IOException {
        FileTime srcFileTime = Files.getLastModifiedTime(SRC.resolve(currentSrc));
        FileTime dstFileTime = Files.getLastModifiedTime(DST.resolve(currentDst));
        return currentSrc.equals(currentDst) && srcFileTime.equals(dstFileTime);
    }

    @Deprecated
    private static void getTree(Path path, Path root, List<Path> list) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path p : stream) {
                list.add(root.relativize(p));
                if (Files.isDirectory(p)) {
                    getTree(p, root, list);
                }
            }
        } catch (IOException x) {
            x.printStackTrace();
        }
    }


    private static void getTree(Path root, List<Path> paths) {


        try {
            Files.walkFileTree(root, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    paths.add(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    paths.add(file);
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        paths.remove(root);
    }


}
// TODO продумать более адекватную структуру
//  https://ru.stackoverflow.com/questions/722720/%D0%90%D0%BB%D0%B3%D0%BE%D1%80%D0%B8%D1%82%D0%BC-%D1%81%D0%B8%D0%BD%D1%85%D1%80%D0%BE%D0%BD%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D0%B8-%D1%81%D0%BF%D0%B8%D1%81%D0%BA%D0%B0-%D1%84%D0%B0%D0%B9%D0%BB%D0%BE%D0%B2-%D0%BD%D0%B0-%D0%B4%D0%B8%D1%81%D0%BA%D0%B5-%D0%B8-%D0%B2-%D0%91%D0%94
