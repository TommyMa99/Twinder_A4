import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import model.ResponseMsg;
import model.SwipeDetail;

public class TwinderServlet extends HttpServlet {
    private RMQChannelPool pool;
    private Connection connection;
    private final static String HOST_NAME = "ec2-52-25-109-172.us-west-2.compute.amazonaws.com";
    private final static String USER_NAME = "rabbit_user";
    private final static String PASSWORD = "GLANT123,./";
    private final static String VIRTUAL_HOST_NAME = "broker";
    private final static String EXCHANGE_NAME = "swipe_task";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        factory.setUsername(USER_NAME);
        factory.setPassword(PASSWORD);
        factory.setVirtualHost(VIRTUAL_HOST_NAME);
        try {
            connection = factory.newConnection();
            RMQChannelFactory factory_channel = new RMQChannelFactory(connection);
            pool = new RMQChannelPool(50, factory_channel);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        try {
            connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        ResponseMsg message = new ResponseMsg();
        String success_msg = "Healthy";
        message.setMessage(success_msg);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().print(gson.toJson(message));
        response.getOutputStream().flush();
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
//            rabbitConnection.configQueue();
            // channels POOL
             Channel channel = pool.borrowObject();

            // Gather Path Info
            String pathInfo = request.getPathInfo();
            StringBuilder builder = new StringBuilder(pathInfo);
            builder.deleteCharAt(0);
            pathInfo = builder.toString();
            String[] parts = pathInfo.split("/");

            // Gather Body Info
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = request.getReader().readLine()) != null) {
                sb.append(s);
            }
            Gson gson = new Gson();
            SwipeDetail swipeDetail = (SwipeDetail) gson.fromJson(sb.toString(), SwipeDetail.class);

            // Use Path and Body for error check
            if (parts.length != 1){
                sendInvalidInputResponse(response);
            } else if ((!Objects.equals(parts[0], "left")) && (!Objects.equals(parts[0], "right"))) {
                sendInvalidInputResponse(response);
            } else if ((Integer.parseInt(swipeDetail.getSwiper()) > 1000000) ||
                    (Integer.parseInt(swipeDetail.getSwiper()) == Integer.parseInt(swipeDetail.getSwipee())) ||
                    (Integer.parseInt(swipeDetail.getSwiper()) < 1) ||
                    (Integer.parseInt(swipeDetail.getSwipee()) > 1000000) ||
                    (Integer.parseInt(swipeDetail.getSwipee()) < 1)) {
                System.out.println(swipeDetail);
                sendUserNotFoundResponse(response);
            } else {
                sendSuccessResponse(response);
                swipeDetail.setLeftorright(parts[0]);
                byte[] data = new Gson().toJson(swipeDetail).getBytes();
                channel.basicPublish(EXCHANGE_NAME, "", null, data);
                pool.returnObject(channel);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sendInvalidInputResponse(response);
        }
    }

    private void sendInvalidInputResponse(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        ResponseMsg message = new ResponseMsg();
        String invalid_input_msg = "Invalid inputs";
        message.setMessage(invalid_input_msg);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getOutputStream().print(gson.toJson(message));
        response.getOutputStream().flush();
    }

    private void sendSuccessResponse(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        ResponseMsg message = new ResponseMsg();
        String success_msg = "Write successful";
        message.setMessage(success_msg);
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getOutputStream().print(gson.toJson(message));
        response.getOutputStream().flush();
    }

    private void sendUserNotFoundResponse(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        ResponseMsg message = new ResponseMsg();
        String user_not_found_msg = "User not found";
        message.setMessage(user_not_found_msg);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getOutputStream().print(gson.toJson(message));
        response.getOutputStream().flush();
    }
}
