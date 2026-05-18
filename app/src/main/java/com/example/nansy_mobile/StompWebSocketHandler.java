package com.example.nansy_mobile;

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

    public void connect(String serverUrl, String username, String jwtToken) {
        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(serverUrl + "?token=" + jwtToken)
                    .addHeader("Authorization", "Bearer " + jwtToken)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    System.out.println("WebSocket соединение установлено");
                    isConnected = true;

                    String connectFrame = "CONNECT\n" +
                            "accept-version:1.2\n" +
                            "host:localhost\n" +
                            "heart-beat:10000,10000\n" +
                            "\n" +
                            "\0";

                    webSocket.send(connectFrame);
                    webSocket.request();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    buffer.append(text);

                    if (text.endsWith("\0")) {
                        String fullMessage = buffer.toString();
                        buffer.setLength(0);
                        handleStompFrame(fullMessage);
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

        String subscribeFrame = "SUBSCRIBE\n" +
                "id:" + subscriptionId + "\n" +
                "destination:" + destination + "\n" +
                "ack:auto\n" +
                "\n" +
                "\0";

        webSocket.send(subscribeFrame);
        webSocket.request();
    }

    public void send(String destination, String body) {
        if (!isConnected) {
            System.err.println("Не подключен к серверу");
            return;
        }

        String sendFrame = "SEND\n" +
                "destination:" + destination + "\n" +
                "content-type:application/json\n" +
                "\n" +
                body + "\n" +
                "\0";

        webSocket.send(sendFrame);
        webSocket.request();
    }

    public void disconnect() {
        if (webSocket != null) {
            String disconnectFrame = "DISCONNECT\n" +
                    "\n" +
                    "\0";
            webSocket.send(disconnectFrame);
            webSocket.close(1000, "Client disconnect");
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void handleStompFrame(String frame) {
        if (frame == null || frame.trim().isEmpty()) {
            System.err.println("получен пустой фрейм");
            return;
        }

        String[] lines = frame.split("\n");

        if (lines.length == 0) {
            System.err.println("фрейм не содержит строк");
            return;
        }

        String command = lines[0];

        if (command == null || command.isEmpty()) {
            System.err.println("команда в фрейме пустая");
            return;
        }

        switch (command) {
            case "CONNECTED":
                handleConnectedFrame(lines);
                break;
            case "MESSAGE":
                handleMessageFrame(lines);
                break;
            case "ERROR":
                handleErrorFrame(lines);
                break;
            default:
                System.out.println("Получена неизвестная команда: " + command);
        }
    }

    private void handleConnectedFrame(String[] lines) {
        for (String line : lines) {
            if (line.startsWith("session:")) {
                sessionId = line.substring("session:".length());
                System.out.println("STOMP подключен, session-id: " + sessionId);
                break;
            }
        }
    }

    private void handleMessageFrame(String[] lines) {
        StringBuilder body = new StringBuilder();
        boolean inBody = false;

        for (String line : lines) {
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