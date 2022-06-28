package server;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioServer {

    private ServerSocketChannel server;
    private Selector selector;
    private String currentDir = System.getProperty("user.home");

    NioServer() throws IOException {
        server = ServerSocketChannel.open();
        selector = Selector.open();
        server.bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws IOException {
        while ((server.isOpen())) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    try {
                        handleRead(key);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap("Welcome to terminal\r\n->".getBytes(StandardCharsets.UTF_8)));
    }

    private void handleRead(SelectionKey key) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder s = new StringBuilder();
        while ((channel.isOpen())) {
            int read = channel.read(buf);
            if (read < 0) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                s.append((char) buf.get());
            }
            buf.clear();
        }
        s.append("->");

/*    byte[] message = s.toString().getBytes(StandardCharsets.UTF_8);
        for (SelectionKey selectedKey : selector.keys()) {
            if (selectedKey.isValid() && selectedKey.channel() instanceof SocketChannel sc) {
                sc.write(ByteBuffer.wrap(message));
            }
        }*/

        if (s.toString().startsWith("ls")) {
            listFilesFromDirectory(currentDir, channel);

//Команды нормально работают с латиницей, но отказываются распознавать русские буквы в названиях. Перепробовала всё из гугла, меняла настройки Putty, ничего не помогает. Осталось только вместо Putty написать свой клиент, чтобы проверить, насколько корректно это работает

        } else if (s.toString().startsWith("cat ")) {
            String filename = s.substring(3, s.toString().length() - 2).trim();
            Path filepath = Path.of(currentDir).resolve(filename);
            if (Files.exists(filepath)) {
                try {
                    channel.write(ByteBuffer.wrap(Files.readAllBytes(filepath)));
                } catch (IOException e) {
                    channel.write(ByteBuffer.wrap("File reading error\r\n->".getBytes(StandardCharsets.UTF_8)));
                    e.printStackTrace();
                }
            } else {
                channel.write(ByteBuffer.wrap("Wrong file name\r\n->".getBytes(StandardCharsets.UTF_8)));
            }
        } else if (s.toString().startsWith("cd")) {
            byte[] selectedDirPrinted = s.substring(2, s.toString().length() - 2).trim().getBytes(StandardCharsets.UTF_8);
            String selectedDir = new String(selectedDirPrinted, StandardCharsets.UTF_8);
            if(!selectedDir.isEmpty()) {
                try {
                    if (Files.isDirectory(Paths.get(selectedDir))) {
                        listFilesFromDirectory(selectedDir, channel);
                        currentDir = selectedDir;
                    } else {
                        channel.write(ByteBuffer.wrap("Selected item is not directory\r\n->".getBytes(StandardCharsets.UTF_8)));
                    }
                }catch (InvalidPathException e){
                    channel.write(ByteBuffer.wrap("Wrong format of directory name\r\n->".getBytes(StandardCharsets.UTF_8)));
                    e.printStackTrace();
                }
            }else {
                channel.write(ByteBuffer.wrap("No directory name\r\n->".getBytes(StandardCharsets.UTF_8)));
            }
        }
    }


    private void listFilesFromDirectory(String currentDir, SocketChannel channel) throws IOException {
        try {
            List<Path> list = Files.list(Path.of(currentDir)).toList();
            for (Path pathString : list) {
                channel.write(ByteBuffer.wrap((pathString.toString() + "\r\n->").getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            channel.write(ByteBuffer.wrap("Listing error\r\n->".getBytes(StandardCharsets.UTF_8)));
            e.printStackTrace();
        }
    }
}





