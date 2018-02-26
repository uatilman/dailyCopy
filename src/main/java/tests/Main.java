package tests;

//import org.apache.commons.codec.digest.DigestUtils;

import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;


public class Main {
    private static final Path SRC = Paths.get("srcFiles").toAbsolutePath();
    private static final Path DST = Paths.get("dstFiles").toAbsolutePath();

    public static void main(String[] args) throws Exception {
       MyFile src = new MyFile(SRC, SRC);
       MyFile dst = new MyFile(DST, SRC);

          src.sync(dst);

//        List<MyFile> myFilesDst = MyFile.getFileTreePaths(DST, false);
//        myFilesSrc.sort(Comparator.comparing(MyFile::getPath));
//        myFilesDst.sort(Comparator.comparing(MyFile::getPath));
//
//
//        System.out.println(myFilesSrc);

    }

    private static void streamTest() {
        List<Path> paths = new ArrayList<>();
        List<Path> paths1 = new ArrayList<>();
        paths.addAll(paths.stream().filter(path -> Files.isDirectory(path)).collect(Collectors.toList()));
        paths.forEach(path -> System.out.println(path.getFileName()));
    }


    private static Map<String, Object> getAttributes(Path path) throws IOException {
        BasicFileAttributes attr;
        attr = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
//        System.out.println("Creation time: " + attr.creationTime());
//        System.out.println("Last access time: " + attr.lastAccessTime());
//        System.out.println("Last modified time: " + attr.lastModifiedTime());
        return Files.readAttributes(path, "*", LinkOption.NOFOLLOW_LINKS);
    }


}
