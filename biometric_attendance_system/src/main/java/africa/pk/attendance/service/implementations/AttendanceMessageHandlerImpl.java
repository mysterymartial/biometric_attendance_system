package africa.pk.attendance.service.implementations;
import africa.pk.attendance.service.interfaces.AttendanceMessageHandler;
import africa.pk.attendance.service.interfaces.AttendanceMessageService;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AttendanceMessageHandlerImpl implements AttendanceMessageHandler {
    private String broker;
    private String clientId;
    private String username;
    private String password;
    private String topic;
    private int subQos = 1;
    private final List<africa.pk.attendance.dtos.response.MessageToBeReturned> messageToBeReturned = new ArrayList<>();
    private MqttClient client;

    @Lazy
    private AttendanceMessageService attendanceMessageService;

    @PostConstruct
    public void initializeTheClient() {
        Properties envProps = new Properties();
        boolean loadedFromEnvFile = false;

        try (FileInputStream fis = new FileInputStream(".env")) {
            envProps.load(fis);
            loadedFromEnvFile = true;
            System.out.println("Loaded configuration from .env file");
        } catch (Exception e) {
            System.out.println("Could not load .env file, will try environment variables: " + e.getMessage());
        }

        this.broker = getConfigValue(envProps, "MQTT_BROKER_URL", loadedFromEnvFile);
        this.clientId = getConfigValue(envProps, "MQTT_CLIENT_ID", loadedFromEnvFile);
        this.username = getConfigValue(envProps, "MQTT_USERNAME", loadedFromEnvFile);
        this.password = getConfigValue(envProps, "MQTT_PASSWORD", loadedFromEnvFile);
        this.topic = getConfigValue(envProps, "MQTT_TOPIC", loadedFromEnvFile);

        validateConfig();

        try {
            client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setCleanSession(true);
            options.setConnectionTimeout(60);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            options.setHttpsHostnameVerificationEnabled(false);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                    System.out.println("Cause: " + cause.getCause());
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Gson gson = new Gson();
                    africa.pk.attendance.dtos.request.AttendanceMessage incomingMessage = gson.fromJson(new String(message.getPayload()), africa.pk.attendance.dtos.request.AttendanceMessage.class);
                    if (incomingMessage.getTime() != null && incomingMessage.getDate() != null && incomingMessage.getFingerprintId() != null) {
                        africa.pk.attendance.dtos.response.AttendanceProcessingResult result = attendanceMessageService.addMessage(incomingMessage);
                        System.out.println("Received and processed message: " + incomingMessage);
                        if (!result.isSuccess()) {
                            getMessageFromAttendanceHandler(result.getMessage(), result.getTopic());
                        }
                    } else {
                        System.out.println("Invalid message received: " + new String(message.getPayload()));
                        getMessageFromAttendanceHandler("Error: Invalid message format", incomingMessage.getTopic());
                    }
                    publishMessageToAttendanceSystem(incomingMessage.getTopic(), client);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Delivery complete: " + token.isComplete());
                }
            });

            client.connect(options);
            client.subscribe(topic, subQos);
            System.out.println("Connected to MQTT broker and subscribed to topic: " + topic);
        } catch (MqttException e) {
            System.err.println("Failed to connect to MQTT broker: " + e.getMessage());
            System.err.println("Reason code: " + e.getReasonCode());
            System.err.println("Cause: " + e.getCause());
            reconnect();
        }
    }

    private String getConfigValue(Properties props, String key, boolean loadedFromEnvFile) {
        if (loadedFromEnvFile && props.getProperty(key) != null) {
            return props.getProperty(key);
        }


        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        return null;
    }

    private void validateConfig() {
        if (broker == null || broker.isEmpty()) {
            throw new IllegalStateException("MQTT_BROKER_URL not found in configuration.");
        }
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalStateException("MQTT_CLIENT_ID not found in configuration.");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("MQTT_USERNAME not found in configuration.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("MQTT_PASSWORD not found in configuration.");
        }
        if (topic == null || topic.isEmpty()) {
            throw new IllegalStateException("MQTT_TOPIC not found in configuration.");
        }
    }

    private void reconnect() {
        int retryInterval = 5000;
        int maxRetries = 5;
        int retryCount = 0;
        while (!client.isConnected() && retryCount < maxRetries) {
            try {
                System.out.println("Attempting to reconnect to MQTT broker... (Attempt " + (retryCount + 1) + " of " + maxRetries + ")");
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                options.setCleanSession(true);
                options.setConnectionTimeout(60);
                options.setKeepAliveInterval(60);
                options.setAutomaticReconnect(true);
                options.setHttpsHostnameVerificationEnabled(false);
                client.connect(options);
                client.subscribe(topic, subQos);
                System.out.println("Reconnected to MQTT broker and resubscribed to topic: " + topic);
                return;
            } catch (MqttException e) {
                System.err.println("Reconnection failed: " + e.getMessage());
                retryCount++;
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (!client.isConnected()) {
            System.err.println("Failed to reconnect to MQTT broker after " + maxRetries + " attempts.");
        }
    }

    private void publishMessageToAttendanceSystem(String subTopic, MqttClient client) throws MqttException {
        AtomicReference<String> message = new AtomicReference<>();
        messageToBeReturned.forEach((attendanceReturnMessages) -> {
            if (attendanceReturnMessages.getTopicToPublishTo().equals(subTopic)) {
                message.set(attendanceReturnMessages.getMessage());
            }
        });
        if (message.get() != null && client.isConnected()) {
            MqttMessage newMessage = new MqttMessage(message.get().getBytes());
            newMessage.setQos(1);
            client.publish(subTopic, newMessage);
            messageToBeReturned.removeIf(messageToBeReturned -> messageToBeReturned.getTopicToPublishTo().equals(subTopic));
            System.out.println("Published message to topic " + subTopic + ": " + message.get());
        }
    }

    @Override
    public void getMessageFromAttendanceHandler(String message, String topicToSendMessageTo) {
        africa.pk.attendance.dtos.response.MessageToBeReturned messageToBeReturned = new africa.pk.attendance.dtos.response.MessageToBeReturned();
        messageToBeReturned.setMessage(message);
        messageToBeReturned.setTopicToPublishTo(topicToSendMessageTo);
        this.messageToBeReturned.add(messageToBeReturned);
    }

    @Override
    public void publishMessage(String message, String topic) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttMessage.setQos(1);
                client.publish(topic, mqttMessage);
                System.out.println("Published message to topic " + topic + ": " + message);
            } else {
                System.err.println("Cannot publish message: MQTT client is not connected.");
            }
        } catch (MqttException e) {
            System.err.println("Failed to publish message to MQTT: " + e.getMessage());
        }
    }
}
