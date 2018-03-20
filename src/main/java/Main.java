import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;
import java.util.logging.Formatter;

import static java.util.logging.Level.*;
import static java.util.logging.Level.SEVERE;

public class Main {
    public static final Logger LOGGER = Logger.getLogger(MyFile.class.getName());
    private static final Path SRC = Paths.get("\\\\SRVFILES\\tppdocs\\АО\\АО").toAbsolutePath();
    private static final Path DST = Paths.get("E:\\AutoSync").toAbsolutePath();
    private static final Path TRASH_PATH = Paths.get("E:\\AutoSync\\trash").toAbsolutePath();
    private static Handler fileHandlerException;
    private static Handler fileHandlerInfo;
    private static Handler consoleHandlerInfo;
    private static long lastModificationTime = -1;
    private static MenuItem labelItem;
    private static boolean isStart = false;
    private static Thread startThread;

    // TODO: 19.03.2018 дельта при сравнении файлов по времени 10 секунд
    // TODO: 20.03.2018 расчет времени выключения
    public static void main(String[] args) {
        initLogger();

        ExecutorService menuExecutorService = Executors.newFixedThreadPool(1);
        SwingUtilities.invokeLater(Main::createAndShowGUI);

        try {
            Process proc =   Runtime.getRuntime().exec("shutdown /s /t 1800");
            System.out.println("before wait");
            proc.waitFor();
            System.out.println("after wait");

            proc.destroy();
            System.out.println("after destroy");

        } catch (IOException  | InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                if (!isStart) {
//                    startThread = new Thread(Main::prepareStartThread);
//                    startThread.prepareStartThread();
//                    startThread.join();
                    prepareStartThread();
                    startThread.start();
                    startThread.join();
                    menuExecutorService.execute(Main::updateLabel);
                }
                Main.LOGGER.log(INFO, "start sleep");

                Thread.sleep(300000);
                Main.LOGGER.log(INFO, "close sleep");

            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

        }
        close();
    }


    private static void prepareStartThread() {
        startThread = new Thread(() -> {
            isStart = true;
            long t;
            long t1;

            MyFile src;
            MyFile dst;
            try {
                LOGGER.log(INFO, "prepareStartThread sync ===========================");

                t = System.currentTimeMillis();
                src = new MyFile(SRC, SRC, TRASH_PATH);

                t1 = System.currentTimeMillis();
                LOGGER.log(INFO, "Time to get src tree, seconds: " + (t1 - t) / 1000);

                dst = new MyFile(DST, DST, TRASH_PATH);
                t = System.currentTimeMillis();
                LOGGER.log(INFO, "Time to get dst tree, seconds: " + (t - t1) / 1000);

                src.sync(dst);
                t1 = System.currentTimeMillis();
                LOGGER.log(INFO, "Time to sync, seconds: " + (t1 - t) / 1000);

            } catch (Exception | Error e) {
                LOGGER.log(WARNING, "", e);
                e.printStackTrace();
            }
            lastModificationTime = System.currentTimeMillis();

            LOGGER.log(INFO, "close sync ===========================");

            isStart = false;
        });


    }

    private static void updateLabel() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!isStart) {
            labelItem.setLabel("Модифицировано " + new SimpleDateFormat("YYYY.MM.dd_H.mm").format(new Date(lastModificationTime)));
        } else {
            labelItem.setLabel("Идет синхронизация. " + "Модифицировано " + new SimpleDateFormat("H.mm").format(new Date(lastModificationTime)));

        }
    }

    private static void initLogger() {
        LOGGER.setLevel(ALL);
        LOGGER.setUseParentHandlers(false);

        try {
            fileHandlerException = new FileHandler(
                    "exception_log_" +
                            new SimpleDateFormat("YYYY.MM.dd_H.mm.s").format(Calendar.getInstance().getTime()) +
                            ".log"
            );
            fileHandlerException.setFormatter(new SimpleFormatter());
            fileHandlerException.setLevel(WARNING);
            fileHandlerException.setEncoding("UTF-8");
            LOGGER.addHandler(fileHandlerException);


            fileHandlerInfo = new FileHandler(
                    "info_log_" +
                            new SimpleDateFormat("YYYY.MM.dd_H.mm.s").format(Calendar.getInstance().getTime()) +
                            ".log"

            );
            fileHandlerInfo.setFormatter(new SimpleFormatter());
            fileHandlerInfo.setLevel(CONFIG);
            fileHandlerInfo.setEncoding("UTF-8");
            fileHandlerInfo.setFilter(record -> record.getLevel().equals(INFO));
            LOGGER.addHandler(fileHandlerInfo);

            consoleHandlerInfo = new ConsoleHandler();
            consoleHandlerInfo.setLevel(CONFIG);
            consoleHandlerInfo.setFilter(record -> record.getLevel().equals(INFO));
            LOGGER.addHandler(consoleHandlerInfo);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI() {
        //Check the SystemTray support
        if (!SystemTray.isSupported()) {
            LOGGER.log(WARNING, "SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(createImage("images/bulb.gif", "tray icon"));
        trayIcon.setImageAutoSize(true);
        final SystemTray tray = SystemTray.getSystemTray();


        MenuItem aboutItem = new MenuItem("About");
//        if (lastModificationTime > 0)
//            labelItem = new MenuItem("Модифицировано " + new SimpleDateFormat("YYYY.MM.dd_H.mm").format(new Date(lastModificationTime)));
//        else
        labelItem = new MenuItem("Первая синхронизация после запуска");

        MenuItem updateItem = new MenuItem("Обновить");
        MenuItem exitItem = new MenuItem("Выйти");

        popup.add(labelItem);
        popup.addSeparator();
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(updateItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            LOGGER.log(WARNING, "TrayIcon could not be added.");
            return;
        }

        trayIcon.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Программа синхронизации запущена"));

        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Программа синхронизирует сетевую папку с жестким диском 1 раз в минуту или по запросу"));

        updateItem.addActionListener(e -> {
            LOGGER.log(INFO, "нажата кнопка обновить");

            if (!isStart) {
//                new Thread(Main::prepareStartThread).start();

                prepareStartThread();
                startThread.start();
            } else {
                JOptionPane.showMessageDialog(null,
                        "Синхронизация уже идет");
            }
        });

        exitItem.addActionListener(e -> {
            close();
            tray.remove(trayIcon);
            System.exit(0);
        });
    }

    private static Image createImage(String path, String description) {
        URL imageURL = TrayIconDemo.class.getResource(path);

        if (imageURL == null) {
            LOGGER.log(WARNING, "Resource not found: " + path);
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

    private static void close() {
        fileHandlerException.close();
        fileHandlerInfo.close();
        consoleHandlerInfo.close();
    }
}
