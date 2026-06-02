package com.example.nansy_mobile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class StompWebSocketHandler {
    private WebSocket webSocket;
    private boolean isConnected = false;
    private String sessionId;

    private final Map<String, Consumer<String>> subscriptionListeners = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptions = new ConcurrentHashMap<>();

    public void connect(String serverUrl, String username, String jwtToken) {
        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(serverUrl + "?token=" + jwtToken)
                    .addHeader("Authorization", "Bearer " + jwtToken)
                    .addHeader("Sec-WebSocket-Protocol", "v12.stomp")
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    System.out.println("WebSocket соединение установлено");
                    isConnected = true;

                    String connectFrame = String.format(
                            "CONNECT\n" +
                            "accept-version:1.2\n" +
                            "host:localhost\n" +
                            "heart-beat:10000,10000\n" +
                            "\n" +
                            "\u0000"
                    );

                    webSocket.send(connectFrame);
                    webSocket.request();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    System.out.println("📥 RAW: '" + text + "'");
                    buffer.append(text);

                    // Проверяем, есть ли в буфере полный фрейм
                    String current = buffer.toString();
                    if (current.contains("\n\n") || current.contains("\0") || current.contains("ERROR")) {
                        System.out.println("Processing full message");
                        handleStompFrame(current);
                        buffer.setLength(0);
                        webSocket.request();
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    onMessage(webSocket, bytes.utf8());
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    System.out.println("Соединение закрыто: " + reason + " (код: " + code + ")");
                    isConnected = false;
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    System.err.println("WebSocket ошибка: " + t.getMessage());
                    isConnected = false;
                }
            });

        } catch (Exception e) {
            System.err.println("Не удалось подключиться: " + e.getMessage());
        }
    }

    public void subscribe(String destination, java.util.function.Consumer<String> listener) {
        if (!isConnected) {
            System.err.println("Не подключен к серверу");
            return;
        }

        String subscriptionId = "sub-" + System.currentTimeMillis();

        subscriptionListeners.put(subscriptionId, listener);
        subscriptions.put(subscriptionId, destination);

        String subscribeFrame = String.format(
                "SUBSCRIBE\n" +
                "id:%s\n" +
                "destination:%s\n" +
                "ack:client\n" +
                "\n" +
                "\0",
                subscriptionId, destination
        );

        webSocket.send(subscribeFrame);
        webSocket.request();
    }

    public void send(String destination, String message) {
        if (!isConnected) {
            System.err.println("Не подключен к серверу");
            return;
        }

        String sendFrame = String.format(
                "SEND\n" +
                "destination:%s\n" +
                "content-type:text/plain\n" +
                "\n" +
                "%s\n" +
                "\u0000",
                destination, message
        );

        webSocket.send(sendFrame);
        webSocket.request();
    }

    public void disconnect() {
        if (webSocket != null) {
            String disconnectFrame = String.format(
                    "DISCONNECT\n" +
                    "receipt:disconnect-ack\n" +
                    "\n" +
                    "\0"
            );
            webSocket.send(disconnectFrame);
            webSocket.close(1000, "Client disconnect");
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void handleStompFrame(String frame) {
        // Убираем null символ и пробелы в конце
        String cleanFrame = frame.replace("\0", "").trim();

        if (cleanFrame.isEmpty()) {
            System.err.println("получен пустой фрейм");
            webSocket.send("\n");
            return;
        }

        String[] lines = cleanFrame.split("\n");
        if (lines.length == 0) {
            System.err.println("фрейм не содержит строк");
            return;
        }

        String command = lines[0];
        System.out.println("Command: '" + command + "'");

        switch (command) {
            case "CONNECTED":
                System.out.println("✅ CONNECTED!");
                handleConnectedFrame(lines);
                break;
            case "MESSAGE":
                System.out.println("📩 MESSAGE!");
                handleMessageFrame(lines);
                break;
            case "ERROR":
                System.err.println("❌ ERROR!");
                handleErrorFrame(lines);
                break;
            default:
                System.out.println("Неизвестная команда: " + command);
        }

        String messageId = null;
        for (String line : lines) {
            if (line.startsWith("message-id:")) {
                messageId = line.substring("message-id:".length()).trim();
                break;
            }
        }

        // ✅ Отправляем ACK, чтобы сервер знал, что мы получили сообщение
        if (messageId != null) {
            sendAck(messageId);
        }
    }

    private void sendAck(String messageId) {
        String ackFrame = String.format(
                "ACK\n" +
                "id:%s\n" +
                "\n" +
                "\0",
                messageId
        );
        webSocket.send(ackFrame);
        System.out.println("✅ ACK отправлен для message-id: " + messageId);
    }

    private void handleConnectedFrame(String[] lines) {
        for (String line : lines) {
            if (line.startsWith("session:")) {
                sessionId = line.substring("session:".length());
                System.out.println("STOMP подключен, session-id: " + sessionId);
            }
        }

        isConnected = true;
    }

    private void handleMessageFrame(String[] lines) {
        String subscriptionId = null;
        StringBuilder body = new StringBuilder();
        boolean inBody = false;

        for (String line : lines) {
            if (!inBody && line.startsWith("subscription:")) {
                subscriptionId = line.substring("subscription:".length()).trim();
            }

            if (!inBody && line.isEmpty()) {
                inBody = true;
                continue;
            }

            if (inBody) {
                if (body.length() > 0) body.append("\n");
                body.append(line);
            }
        }

        String message = body.toString().replace("\0", "");
        System.out.println("Получено сообщение: " + message);

        if (subscriptionId != null && subscriptionListeners.containsKey(subscriptionId)) {
            Consumer<String> listener = subscriptionListeners.get(subscriptionId);
            if (listener != null) {
                listener.accept(message);
                System.out.println("✅ Listener called for subscription: " + subscriptionId);
            } else {
                System.err.println("Listener is null for: " + subscriptionId);
            }
        } else {
            System.err.println("No listener found for subscription: " + subscriptionId);
            System.err.println("Available subscriptions: " + subscriptionListeners.keySet());
        }
    }

    private void handleErrorFrame(String[] lines) {
        StringBuilder errorMsg = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("message:")) {
                errorMsg.append(line.substring(8));
            } else if (line.isEmpty()) {
                break;
            }
        }
        System.err.println("STOMP ошибка: " + errorMsg);
    }
}