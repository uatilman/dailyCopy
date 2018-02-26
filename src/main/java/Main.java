import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

public class Main {
    private static StringBuilder builder = new StringBuilder();
    private final static Path LOG = Paths.get("copylog.txt");
    private final static Path SETTINGS_FILE = Paths.get("pref.txt");
    private static List<Path> srcDailyList = new ArrayList<>();
    private static List<Path> srcFridayList = new ArrayList<>();
    private static Calendar startSmallCopy;
    private static Calendar startAllCopy;
    private static Calendar justNow;

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            justNow = Calendar.getInstance();
            startSmallCopy = Calendar.getInstance();
            startAllCopy = Calendar.getInstance();
// TODO: 19.02.2018 replace DD - day in year to dd - day in month

            builder.append(new SimpleDateFormat("YYYY.MM.dd_H.mm").format(justNow.getTime()));
            builder.append("\n");
            Files.write(LOG, builder.toString().getBytes(), StandardOpenOption.APPEND);
            builder.setLength(0);
            setSettings();


            if ((justNow.get(Calendar.DAY_OF_WEEK) - 1) != 5) {
                prepareCopy(startSmallCopy, srcDailyList, "daily_copy");
            } else {
                prepareCopy(startAllCopy, srcFridayList, "friday_copy");
            }
        } catch (Exception e) {
            Files.write(LOG, e.getMessage().getBytes(), StandardOpenOption.APPEND);
            e.printStackTrace();
        }
    }

    private static void setSettings() throws IOException {
        List<String> settings = Files.readAllLines(SETTINGS_FILE);
        for (int i = 0; i < settings.size(); i++) {
            switch (settings.get(i)) {
                case "#Every Day":
                    i++;
                    while (settings.get(i).startsWith("\\\\") && i < settings.size()) {
                        srcDailyList.add(Paths.get(settings.get(i++)));
                    }
                    break;
                case "#Every Week":
                    i++;
                    while (settings.get(i).startsWith("\\\\") && i < settings.size()) {
                        srcFridayList.add(Paths.get(settings.get(i++)));
                    }
                    break;
                case "#Daily hour start time:":
                    if (i >= settings.size()) break;
                    try {
                        startSmallCopy.set(Calendar.HOUR_OF_DAY, Integer.parseInt(settings.get(++i)));
                        System.out.println(Integer.parseInt(settings.get(i)));

                    } catch (NumberFormatException e) {
                        //TODO записать в лог
                    }
                    break;
                case "#Daily minutes start time:":
                    if (i >= settings.size()) break;
                    try {
                        startSmallCopy.set(Calendar.MINUTE, Integer.parseInt(settings.get(++i)));
                        System.out.println(Integer.parseInt(settings.get(i)));
                    } catch (NumberFormatException e) {
                        //TODO записать в лог
                    }
                    break;

                case "#Friday hour start time:":
                    if (i >= settings.size()) break;
                    try {
                        startAllCopy.set(Calendar.HOUR_OF_DAY, Integer.parseInt(settings.get(++i)));
                    } catch (NumberFormatException e) {
                        //TODO записать в лог
                    }
                    break;
                case "#Friday minutes start time:":
                    if (i >= settings.size()) break;
                    try {
                        startAllCopy.set(Calendar.MINUTE, Integer.parseInt(settings.get(++i)));
                    } catch (NumberFormatException e) {
                        //TODO записать в лог
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void prepareCopy(Calendar startCopy, List<Path> srcList, String type) throws IOException, InterruptedException {
        Path dst;
        dst = Paths.get("E:\\" + new SimpleDateFormat("YYYY.MM.dd_H.mm").format(startCopy.getTime()) + "_test" + type);

        if (!Files.exists(dst))
            Files.createDirectory(dst);
//region log/sleep
        long dt = startCopy.getTimeInMillis() - justNow.getTimeInMillis();
        builder.append("Before copying is left ").append(dt / 1000).append(", sec \n");
        Files.write(LOG, builder.toString().getBytes(), StandardOpenOption.APPEND);


        builder.setLength(0);
        Thread.sleep(dt);
//endregion
        Files.write(LOG, builder.toString().getBytes(), StandardOpenOption.APPEND);
        builder.setLength(0);

        for (Path path : srcList) {
            Path dstLocal = Paths.get(dst.toString() + "\\" + path.getFileName());
            if (!Files.exists(dstLocal))
                Files.createDirectory(dstLocal);
            copy(path, dstLocal);
            Files.setLastModifiedTime(dstLocal, Files.getLastModifiedTime(path));
        }

        //region log

        builder.append("end copy. Copy time ").append(new SimpleDateFormat("YYYY.MM.ddH.mm").format(Calendar.getInstance().getTime())).append(" sec. \n");
        Files.write(LOG, builder.toString().getBytes(), StandardOpenOption.APPEND);
        builder.setLength(0);
        //endregion
    }

    private static void copy(Path source, Path target) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path file : stream) {
                System.out.println(file.getFileName());
//TODO add and test nio method Files.isDirectory(path)
                if (file.toFile().isFile()) {

                    Files.copy(
                            file,
                            target.resolve(source.relativize(file)),
                            StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    Path newDir = target.resolve(source.relativize(file));
                    try {
                        Files.copy(file, newDir, StandardCopyOption.COPY_ATTRIBUTES);
                    } catch (FileAlreadyExistsException x) {
                        System.err.println(x.getMessage());
                    }

                    copy(file, newDir);

                    FileTime time = Files.getLastModifiedTime(file);
                    Files.setLastModifiedTime(newDir, time);
                }


            }
        } catch (IOException | DirectoryIteratorException x) {
            // IOException не может броситься во время итерации.
            // В этом куске кода оно может броситься только
            // методом newDirectoryStream.
            System.err.println(x);
        }
    }

//region old

//endregion

}
