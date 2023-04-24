import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class ConsumerLauncher {

    public static final int NUM_THREAD = 20;
    private final static String HOST_NAME = "ec2-52-25-109-172.us-west-2.compute.amazonaws.com";
    private final static String USER_NAME = "rabbit_user";
    private final static String PASSWORD = "GLANT123,./";
    private final static String VIRTUAL_HOST_NAME = "broker";

    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException, SQLException {
        ExecutorService executor = Executors.newFixedThreadPool(30);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        factory.setUsername(USER_NAME);
        factory.setPassword(PASSWORD);
        factory.setVirtualHost(VIRTUAL_HOST_NAME);
        Connection connection = factory.newConnection();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAQPD34WPIA5ECJEFF", "Q3g2CfSinptKqeJsLjCGUHgZz7Eygp6+Vw8yYW+u");
//        BasicAWSCredentials awsCreds = new BasicAWSCredentials("ASIAWIODWU2YNTV63OMB", "WSZCW5e6ArT/wTsFi+0iubtZTLq9Qw/7SOtTFsA2");
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_2)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
        ConsumerThread[] runnables = new ConsumerThread[NUM_THREAD];
        for (int i = 0; i < NUM_THREAD; i++) {
            runnables[i] = new ConsumerThread(connection, executor, client);
        }
        Thread[] threads = new Thread[NUM_THREAD];
        for (int i = 0; i < NUM_THREAD; i++) {
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }
        for (int i = 0; i < NUM_THREAD; i++) {
            threads[i].join();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.shutdown();
            // Code to be executed when the program is about to terminate
            System.out.println("Program is about to terminate...");
        }));
    }
}
