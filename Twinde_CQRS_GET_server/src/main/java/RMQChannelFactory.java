import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple RabbitMQ channel factory based on the APche pooling libraries
 */
public class RMQChannelFactory extends BasePooledObjectFactory<Channel> {

    // Valid RMQ connection
    private final static String QUEUE_NAME_1 = "Twinder_queue_1";
    private final static String QUEUE_NAME_2 = "Twinder_queue_2";
    private final static String EXCHANGE_NAME = "swipe_task";
    private final Connection connection;
    // used to count created channels for debugging
    private int count;

    public RMQChannelFactory(Connection connection) {
        this.connection = connection;
        count = 0;
    }

    @Override
    synchronized public Channel create() throws IOException {
        count ++;
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, false);
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-queue-type", "quorum");
        channel.queueDeclare(QUEUE_NAME_1, true, false, false, arguments);
        channel.queueBind(QUEUE_NAME_1, EXCHANGE_NAME, "");
        return channel;
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        //System.out.println("Wrapping channel");
        return new DefaultPooledObject<>(channel);
    }

    public int getChannelCount() {
        return count;
    }

    // for all other methods, the no-op implementation
    // in BasePooledObjectFactory will suffice
}