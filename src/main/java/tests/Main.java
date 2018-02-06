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
        List<MyFile> myFilesSrc = MyFile.getFileTreePaths(SRC, false);
        List<MyFile> myFilesDst = MyFile.getFileTreePaths(DST, false);

        myFilesSrc.sort(Comparator.comparing(MyFile::getPath));
        myFilesDst.sort(Comparator.comparing(MyFile::getPath));


        System.out.println(myFilesSrc);
        for (int i = 0; i < myFilesSrc.size(); i++) {
        }
    }

    private static void streamTest() {
        List<Path> paths = new ArrayList<>();
        List<Path> paths1 = new ArrayList<>();
        paths.addAll(paths.stream().filter(path -> Files.isDirectory(path)).collect(Collectors.toList()));
        paths.forEach(path -> System.out.println(path.getFileName()));
    }

    private static void getNameWithIntParamTest(File file) {
        System.out.println(file.toPath().getName(0));
        System.out.println(file.toPath().getName(1));
        System.out.println(file.toPath().getName(2));
        System.out.println(file.toPath().getName(3));
        System.out.println(file.toPath().getName(4));
    }

    private static Map<String, Object> getAttributes(Path path) throws IOException {
        BasicFileAttributes attr;
        attr = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
//        System.out.println("Creation time: " + attr.creationTime());
//        System.out.println("Last access time: " + attr.lastAccessTime());
//        System.out.println("Last modified time: " + attr.lastModifiedTime());
        return Files.readAttributes(path, "*", LinkOption.NOFOLLOW_LINKS);
    }


    private static void testSHA256Variants() throws NoSuchAlgorithmException {
        String text = "hello";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
//        digest.update();

        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
//        new Base64.Decoder().
        System.out.println(new String(hash));
        String encoded = Base64.getMimeEncoder().encodeToString(hash);
        String encoded1 = Base64.getEncoder().encodeToString(hash);
        System.out.println(new BASE64Encoder().encode(hash));
        System.out.println(encoded);
        System.out.println(encoded1);
        System.out.println(bytesToHex(hash));

//        System.out.println(DigestUtils.sha256Hex(text));
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
