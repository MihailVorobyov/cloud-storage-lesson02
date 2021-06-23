package com.polozov.cloudstorage.lesson02;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class IOUtils {
    public static void main(String[] args) throws IOException, InterruptedException {
        Path testPath = Path.of(".");
//        System.out.println(testPath);
//        System.out.println(testPath.toAbsolutePath());

        String root = "client";
        Path path = Path.of("");
        Path path1 = Paths.get("client" + File.separator + "1.txt");
        Path path2 = Paths.get("client" + File.separator, "dir1", "dir2", "2.txt");
        Path path3 = Path.of(root, "dir1", "dir2", "3.txt");
        Path path4 = Path.of(root);

//        path.toAbsolutePath().iterator().forEachRemaining(System.out::println);

        WatchService service = FileSystems.getDefault().newWatchService();

        path4.register(service,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
//
//        WatchKey key;
//        String notification = "Event type: %s. File: %s\n";
//        while ((key = service.take()) != null) {
//            for (WatchEvent event : key.pollEvents()) {
//                System.out.printf(notification, event.kind(), event.context());
//            }
//            key.reset();
//        }

//        new Thread(() -> {
//            String notification = "Event type: %s. File: %s\n";
//            while (true) {
//                try {
//                    WatchKey key = service.take();
//                    if (key.isValid()) {
//                        List<WatchEvent<?>> watchEvents = key.pollEvents();
//                        for (WatchEvent<?> event : watchEvents) {
//                            System.out.printf(notification, event.kind(), event.context());
//                        }
//                    }
//
//                    key.reset();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();

        // проверка на существование
        System.out.println("1.txt exists: " + Files.exists(path1));

        // создание файла
        Path path5 = Path.of("client", "dir1", "dir2", "4.txt");
        if (!Files.exists(path5)) {
            Files.createFile(path5);
        }

        // перемещение
//        Path path6 = Files.move(path5, Path.of("client", "dir1", "dir2", "5.txt"));
        Path path6 = Path.of("client", "dir1", "dir2", "5.txt");

        // копирование
        Path path7 = Files.copy(path6, Path.of("client", "dir1", "dir2", "6.txt"), StandardCopyOption.REPLACE_EXISTING);

        // запись в файл
        Files.writeString(path7, "\n\nNew String\n", StandardOpenOption.APPEND);

        // удаление
//        Files.delete(path2);

        // создание директорий и поддиректорий
//        Files.createDirectories(Path.of("client", "dir0", "dir4", "dir5"));

        // обход дерева файлов и директорий
//        Files.walkFileTree(path4, new FileVisitor<Path>() {
//            @Override
//            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                System.out.println("pre - " + dir.getFileName());
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                System.out.println("visit file - " + file.getFileName());
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//                System.out.println("visit failed file - " + file.getFileName());
//                return FileVisitResult.TERMINATE;
//            }
//
//            @Override
//            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//                System.out.println("post - " + dir.getFileName());
//                return FileVisitResult.CONTINUE;
//            }
//        });

        // поиск файла
        Files.walkFileTree(path4, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if ("5.txt".equals(file.getFileName().toString())) {
                    System.out.println(file.getFileName() + " is founded. Path: " + file.toAbsolutePath());
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
