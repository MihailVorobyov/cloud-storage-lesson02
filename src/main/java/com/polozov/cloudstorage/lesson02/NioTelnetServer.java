package com.polozov.cloudstorage.lesson02;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class NioTelnetServer {
    private static final String[] commands = {
        "\tls         view all files from current directory\n\r",
        "\ttouch      create new file\n\r",
        "\tmkdir      create new directory\n\r",
        "\tcd         (path | ~ | ..) change current directory to path, to root or one level up\n\r",
        "\trm         (filename / dirname) remove file / directory\n\r",
        "\tcopy       (src) (target) copy file or directory from src path to target path\n\r",
        "\tcat        (filename) view text file\n\r",
        "\tchangenick (nickname) change user's nickname\n\r"
    };
    
    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private static int userNumber;
    private Map<SocketAddress, String> clients = new HashMap<>();
    private Map<SocketAddress, String> currentPaths = new HashMap<>();
    private Map<SocketAddress, Path> roots = new HashMap<>();
    
    private final String ABSOLUTE_SERVER_PATH;

    public NioTelnetServer() throws Exception {
        userNumber = 0;
        ABSOLUTE_SERVER_PATH = Paths.get("server").toAbsolutePath().toString();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5679));
        server.configureBlocking(false);
        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "att");
    
        // Проверяем, существует ли пользователь.
        // Сохраняем нового пользователя в Map
        String userName;
        if (clients.get(channel.getRemoteAddress()) == null) {
            userName = "user" + userNumber++;
            clients.put(channel.getRemoteAddress(), userName);
            makeDirectory("mkdir " + userName, "server", channel.getRemoteAddress());
        } else {
            userName = clients.get(channel.getRemoteAddress());
        }
        currentPaths.putIfAbsent(channel.getRemoteAddress(), userName);
        roots.putIfAbsent(channel.getRemoteAddress(), Paths.get(userName));
        channel.write(ByteBuffer.wrap(String.join(" ", "Hello", userName, "\n\r").getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n\r".getBytes(StandardCharsets.UTF_8)));
    }
    
    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        String currentPath = currentPaths.get(client);
        String userName = clients.get(client);
        int readBytes = channel.read(buffer);

        if (readBytes < 0) {
            channel.close();
            return;
        } else  if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();
    
        // TODO: 21.06.2021
        // mkdir (dirname) - создание директории
        // cd (path | ~ | ..) - изменение текущего положения
        // rm (filename / dirname) - удаление файла / директории
        // copy (src) (target) - копирование файлов / директории
        // cat (filename) - вывод содержимого текстового файла
        // changenick (nickname) - изменение имени пользователя

        // 1. Создать переменные комманд - выполнено
        // 2. Создать методы обработки комманд -
        
        // добавить имя клиента

        if (key.isValid()) {
            String command = sb.toString()
                .replace("\r", "")
                .replace("\n", "");
            if ("--help".equals(command)) {
                for (String c : commands) {
                    sendMessage(c, selector, client);
                }
            } else if ("ls".equals(command)) {
                sendMessage(getFilesList(currentPath).concat("\n\r"), selector, client);
            } else if (command.startsWith("touch")) {
                sendMessage(createFile(command, currentPath, client).concat("\n\r"), selector, client);
            } else if (command.startsWith("mkdir")) {
                sendMessage(makeDirectory(command, currentPath, client).concat("\n\r"), selector, client);
            } else if (command.startsWith("cd")) {
                sendMessage(changeDirectory(command, currentPath, client).concat("\n\r"), selector, client);
//                } else if (command.startsWith("rm", currentPath)) {
//
//                } else if (command.startsWith("copy")) {
//
//                } else if (command.startsWith("cat", currentPath)) {
//
//                } else if (command.startsWith("changenick")) {
//
            }
            String startOfLine = userName + ": " + currentPaths.get(client) + "> ";
            sendMessage(startOfLine, selector, client);
        }
    }
    
    // изменение текущей директории
    private String changeDirectory(String command, String currentPath, SocketAddress client) {
        String msg = "";
        // cd (path | ~ | .. ) - изменение текущего положения
        String[] arguments = command.split(" ", 2);
        if (arguments.length < 2) {
            return msg;
        }
        String path = arguments[1].trim();
        if ("".equals(path)) {
            return msg;
        }
        String root = roots.get(client).toString();
        String absoluteRoot = ABSOLUTE_SERVER_PATH + File.separator + root;
        
        if ("~".equals(path)) {
            currentPath = root;
        } else if ("..".equals(path)) {
            if (Paths.get("server", currentPath, "\\..").toAbsolutePath().normalize().startsWith(absoluteRoot)) {
                currentPath = Paths.get(currentPath, "\\..").normalize().toString();
            }
        } else {
            Path normalizedPath = Paths.get("server", root, path).toAbsolutePath().normalize();
            // Если path находится в ветке данного пользователя
            if (normalizedPath.startsWith(Paths.get(absoluteRoot))) {
                // Если path представляет полный путь относительно директории server, существует и является директорией
                if (Files.exists(Paths.get("server", path)) && Files.isDirectory(Paths.get("server", path))) {
                    // Изменяем текущую директорию на path
                    currentPath = path;
                    // Если path представляет путь относительно текущей директории, существует и является директорией
                } else if (Files.exists(Paths.get("server", currentPath, path)) && Files.isDirectory(Paths.get("server", currentPath,
                    path))) {
                    // Изменяем текущую директорию на currentPath/path
                    currentPath = Paths.get(currentPath, path).toString();
                }
            } else {
                msg = "Wrong path!\n\r";
            }
        }
        currentPaths.replace(client, currentPath);
        return msg;
    }
    
    // создание файла
    private String createFile(String command, String currentPath, SocketAddress client) {
        String[] arguments = command.split(" ", 2);
        Path path = Paths.get("server", currentPath, arguments[1]);
        try {
            if (path.normalize().toAbsolutePath().startsWith(ABSOLUTE_SERVER_PATH + File.separator + roots.get(client))) {
//                currentPath = arguments[1];
                if (!Files.exists(path)) {
                    Files.createFile(path);
                    return "File " + arguments[1] + " created\n\r";
                } else  {
                    return "File already exists!\n\r";
                }
            }
//            if (path.normalize().toAbsolutePath().toString().startsWith("server" + File.separator)) {
//                currentPath = arguments[1];
//            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Wrong path!";
        }
        return "";
    }
    
    // создание директории
    private String makeDirectory(String command, String currentPath, SocketAddress client) {
        String[] arguments = command.split(" ", 2);
        Path path = Paths.get("server", currentPath, arguments[1]);
        try {
            if (path.normalize().toAbsolutePath().startsWith(ABSOLUTE_SERVER_PATH + File.separator + roots.get(client))) {
                if (currentPath.equals("server")) {
                    path = Paths.get(currentPath, arguments[1]);
                    if (Files.exists(path)) {
                        return "";
                    }
                }
                if (!Files.exists(path)) {
                    Files.createDirectory(path);
                    return "Directory " + arguments[1] + " created\n\r";
                } else {
                    return "Directory already exists!\n\r";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "\n\r";
        }
//        return "Directory " + path + " created\n\r";
        return "\n\r";
    }
    
    // Получаем список файлов и папк в текущей директории
    private String getFilesList(String currentPath) {
        String[] servers = new File("server" + File.separator + currentPath).list();
        if (!(servers == null) && servers.length > 0) {
            Arrays.sort(servers);
            return String.join(" ", servers).concat("\n\r");
        } else {
            return "\n\r";
        }
    }
    
    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel()).write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new NioTelnetServer();
    }
}
