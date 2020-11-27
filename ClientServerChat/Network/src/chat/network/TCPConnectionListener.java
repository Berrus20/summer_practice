package chat.network;

public interface TCPConnectionListener {
    //события
    void onConnectionReady(TCPConnection tcpConnection); //когда запустили соединение
    void onReceiveString(TCPConnection tcpConnection, String value); //когда принимаем строчку
    void onDisconnect(TCPConnection tcpConnection); // когда прервалось соединение
    void onException(TCPConnection tcpConnection, Exception e); //когда происходит исключение
}
