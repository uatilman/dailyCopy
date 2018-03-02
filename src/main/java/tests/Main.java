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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

import static java.util.logging.Level.*;
import static java.util.logging.Level.SEVERE;


public class Main {
    public static final Logger LOGGER = Logger.getLogger(MyFile.class.getName());
    private static final Path SRC = Paths.get("C:\\Users\\usr-mbk00066\\Desktop\\srcFiles").toAbsolutePath();
    private static final Path DST = Paths.get("C:\\Users\\usr-mbk00066\\Desktop\\dstFiles").toAbsolutePath();
    private static final Path TRASH_PATH = Paths.get("C:\\Users\\usr-mbk00066\\Desktop\\dstFiles\\trash").toAbsolutePath();

    public static void main(String[] args) {
        initLogger();

        MyFile src;
        MyFile dst;

        while (true) {
            try {
                Main.LOGGER.log(INFO, "\n===========================\n" + new SimpleDateFormat("YYYY.MM.dd_H.mm.s").format(Calendar.getInstance().getTime()) + "\n");
                src = new MyFile(SRC, SRC, TRASH_PATH);
                dst = new MyFile(DST, DST, TRASH_PATH);
                src.sync(dst);

                Thread.sleep(60000);

            } catch (Exception e) {
                LOGGER.log(WARNING, "", e);
                break;
            }
        }


    }

    private static void initLogger() {
        LOGGER.setLevel(ALL);
        LOGGER.setUseParentHandlers(false);
        try {

            Handler fileHandlerException = new FileHandler("exception.log");
            fileHandlerException.setFormatter(new SimpleFormatter());
            fileHandlerException.setLevel(WARNING);
            LOGGER.addHandler(fileHandlerException);


            Handler fileHandlerInfo = new FileHandler("info.log");
            fileHandlerInfo.setFormatter(new SimpleFormatter());
            fileHandlerInfo.setLevel(CONFIG);
            fileHandlerInfo.setFilter(record -> record.getLevel().equals(INFO));
            LOGGER.addHandler(fileHandlerInfo);


            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(SEVERE);
            LOGGER.addHandler(consoleHandler);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
