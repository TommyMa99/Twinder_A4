import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.rabbitmq.client.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class ConsumerThread implements Runnable {
    private Connection connection;
    private Channel channel;
    private final static String QUEUE_NAME_1 = "Twinder_queue_1";
    private final static String EXCHANGE_NAME = "swipe_task";
    private ExecutorService executor;
    public AmazonDynamoDB client;


    public ConsumerThread(Connection connection, ExecutorService executor, AmazonDynamoDB client) throws SQLException {
        this.connection = connection;
        this.executor = executor;
        this.client = client;
        System.out.println("Here");
    }
    @Override
    public void run() {
        int batchSize = 20;
        try {
            channel = this.connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("x-queue-type", "quorum");
            channel.queueDeclare(QUEUE_NAME_1, true, false, false, arguments);
            channel.queueBind(QUEUE_NAME_1, EXCHANGE_NAME, "");
            channel.basicQos(batchSize, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> myMessagges = new ArrayList<>();
        try {
            channel.basicConsume(QUEUE_NAME_1, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String str = StringEscapeUtils.unescapeJava(new String(body));
                    String[] splits = str.split("comment");
                    int l = splits[0].length();
                    String firstPart = splits[0].substring(1, l-2).replace("\"", "");
                    Map<String, String> headerMap = Arrays.stream(firstPart.split(","))
                            .map(s -> s.split(":"))
                            .collect(Collectors.toMap(s -> s[0], s -> s[1]));
                    String swipeDir = headerMap.get("leftorright");
                    String swiper = headerMap.get("swiper");
                    String swipee = headerMap.get("swipee");
                    myMessagges.add(swiper+"#"+swipee+"#"+swipeDir);
                    if (myMessagges.size() >= batchSize) {
                        List<String> copyList = new ArrayList<>();

                        List<WriteRequest> writeRequests = new ArrayList<>();
                        for (String m : myMessagges) {
                            Map<String, AttributeValue> item = new HashMap<>();
                            item.put("swipe_info", new AttributeValue().withS(m));
                            PutRequest putRequest = new PutRequest().withItem(item);
                            WriteRequest writeRequest = new WriteRequest().withPutRequest(putRequest);
                            writeRequests.add(writeRequest);
                        }
                        BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest()
                                .withRequestItems(Map.of("twinder_post", writeRequests));
                        client.batchWriteItem(batchWriteItemRequest);
//                        for (String element : myMessagges) {
//                            String copyElement = new String(element);
//                            copyList.add(copyElement);
//                        }
//                        executor.execute(new DbWorker(copyList, client));
//                        for (String m : myMessagges) {
//                            String tableName = "twinder_post";
//                            Map<String, AttributeValue> item = new HashMap<>();
//                            item.put("swipe_info", new AttributeValue().withS(m));
//
//                            PutItemRequest request = new PutItemRequest()
//                                    .withTableName(tableName)
//                                    .withItem(item);
//
//                            client.putItem(request);
//                        }
                        myMessagges.clear();
                        channel.basicAck(envelope.getDeliveryTag(), true);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
