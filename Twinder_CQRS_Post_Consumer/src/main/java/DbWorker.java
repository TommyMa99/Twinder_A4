import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbWorker implements Runnable {
    private AmazonDynamoDB client;
    private List<String> myMessagges;

    public DbWorker(List<String> myMessagges, AmazonDynamoDB client) {
        this.myMessagges = myMessagges;
        this.client = client;
    }

    @Override
    public void run() {
        List<WriteRequest> writeRequests = new ArrayList<>();
        for (String m : myMessagges) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("swipe_info", new AttributeValue().withS(m));
            PutRequest putRequest = new PutRequest().withItem(item);
            WriteRequest writeRequest = new WriteRequest().withPutRequest(putRequest);
            writeRequests.add(writeRequest);
        }
        BatchWriteItemResult result = null;
        do {
            BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest()
                    .withRequestItems(Map.of("twinder_post", writeRequests));
            result = client.batchWriteItem(batchWriteItemRequest);
        } while (!result.getUnprocessedItems().isEmpty());
    }
}