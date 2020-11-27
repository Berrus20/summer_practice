package chat.network;

import java.io.*;
import java.net.Socket;

//Класс универсальный, мы можем использовать его и в клиенте, и на сервере

public class TCPConnection {

    private final Socket socket;
    private final Thread rxThread;//поток будет на каждом клиенте, будет слушать входящие соединения, читать поток ввода
    private final TCPConnectionListener eventListener; //слушатель событий
    private final BufferedReader in; //поток ввода
    private final BufferedWriter out; //поток вывода

//конструктор рассчитывает на то, что сокет будет создаваться внутри
    public TCPConnection(TCPConnectionListener eventListener, String ipAddr, int port) throws  IOException {
        //вызываем другой конструктор
        this(eventListener, new Socket(ipAddr, port));
    }
//конструктор на вход примет готовый объект сокета и с ним создаст соединение
    public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.eventListener = eventListener; //запоминаем событие
        this.socket = socket;//спрашиваем сокет и запоминаем его
//получим поток ввода, работающий со строками
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//получим поток вывода, работающий со строками
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//создаём новый поток и слушаем всё входящее
        rxThread = new Thread(new Runnable() {
            //описание класса, реализующего интерфейс Runnable
            @Override
            public void run() {
        //здесь слушаем входящее соединение
                try {
                    eventListener.onConnectionReady(TCPConnection.this/*передаем экземпляр класса TCPConnection*/); //событие: соединение готово
                    //получаем строчки в бесконечном цикле
                    while (!rxThread.isInterrupted()) { //пока поток не прерван
                        //получаем строчку и передаем eventListener
                        eventListener.onReceiveString(TCPConnection.this, in.readLine());
                    }
                } catch (IOException e) {
                    //говорим eventListener, что произошло исключение
                    eventListener.onException(TCPConnection.this, e);
                } finally {
                    //говорим eventListener, что произошло отключение
                    eventListener.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxThread.start(); //запускаем поток
    }

//синхронизируем методы, чтобы к ним можно было обращаться из разных потоков
    public synchronized void sendString(String value) {
        try {
            out.write(value + "\r\n");
            //принудительно отправляем строчку, иначе возможно запись строчки в буфер и не отправка по сети
            out.flush();
        } catch (IOException e) {
            //говорим eventListener, что произошло исключение
            eventListener.onException(TCPConnection.this, e);
            disconnect();
        }
    }

    public synchronized void disconnect() {
        rxThread.interrupt(); //прерывание потока
        try {
            socket.close();
        } catch (IOException e) {
            //говорим eventListener, что произошло исключение
            eventListener.onException(TCPConnection.this, e);
        }
    }
//записываем информацию, что кто-то подключился или отключился
    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
