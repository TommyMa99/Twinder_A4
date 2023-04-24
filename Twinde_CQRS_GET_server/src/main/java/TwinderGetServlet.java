import com.google.gson.Gson;
import model.MatchStats;
import model.Matches;
import model.ResponseMsg;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class TwinderGetServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String urlPattern = request.getServletPath();
        if (urlPattern.equals("/matches")) {
            try {
                handleMatchRequest(request, response);
            } catch (SQLException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (urlPattern.equals("/stats")) {
            try {
                handleStatRequest(request, response);
            } catch (SQLException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            sendInvalidInputResponse(response);
        }
    }

    private void handleMatchRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, ClassNotFoundException {
        // Gather Path Info
        String pathInfo = request.getPathInfo();
        StringBuilder builder = new StringBuilder(pathInfo);
        builder.deleteCharAt(0);
        pathInfo = builder.toString();
        String[] parts = pathInfo.split("/");
        if (parts.length != 1){
            sendInvalidInputResponse(response);
        } else {
            String userId = parts[0];
            if (Integer.parseInt(userId) > 1000000 || Integer.parseInt(userId) < 1){
                sendUserNotFoundResponse(response);
            } else {
                if (isSwipeeExisted(userId)) {
                    Matches match = getMatches(userId);
                    sendRetrieveOkResponse(response, match);
                } else {
                    sendUserNotFoundResponse(response);
                }
            }
        }
    }

    private void handleStatRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, ClassNotFoundException {
        // Gather Path Info
        String pathInfo = request.getPathInfo();
        StringBuilder builder = new StringBuilder(pathInfo);
        builder.deleteCharAt(0);
        pathInfo = builder.toString();
        String[] parts = pathInfo.split("/");
        if (parts.length != 1){
            sendInvalidInputResponse(response);
        } else {
            String userId = parts[0];
            if (Integer.parseInt(userId) > 1000000 || Integer.parseInt(userId) < 1){
                sendUserNotFoundResponse(response);
            } else {
                if (isSwipeeExisted(userId)) {
                    MatchStats stat = getMatchStats(userId);
                    sendRetrieveOkResponse(response, stat);
                } else {
                    sendUserNotFoundResponse(response);
                }
            }
        }
    }

    private MatchStats getMatchStats(String userId) {
        String findDislikeQuery = "SELECT Count(*) AS c FROM SwipeData WHERE swipee_id = '" + userId + "'" + "AND direction = 'left'";
        String findLikeQuery = "SELECT Count(*) AS c FROM SwipeData WHERE swipee_id = '" + userId + "'" + "AND direction = 'right'";
        Connection connection;
        PreparedStatement pstmt;
        try {
            connection = DatabaseConnectionPool.getConnection();
            pstmt = connection.prepareStatement(findDislikeQuery);
            connection.setAutoCommit(false);
            ResultSet rs = pstmt.executeQuery();
            String dislikeNum = "";
            String likeNum = "";
            while (rs.next()) {
                dislikeNum = rs.getString("c");
            }
            pstmt = connection.prepareStatement(findLikeQuery);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                likeNum = rs.getString("c");
            }
            MatchStats matchStats = new MatchStats(Integer.parseInt(likeNum), Integer.parseInt(dislikeNum));
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            pstmt.close();
            return matchStats;
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Matches getMatches(String userId) {
        Connection connection;
        PreparedStatement pstmt;
        String findQuery = "SELECT swiper_id FROM SwipeData WHERE swipee_id =" + "'" + userId + "'" + " AND direction = 'right'";
        Matches match = new Matches();
        try {
            connection = DatabaseConnectionPool.getConnection();
            pstmt = connection.prepareStatement(findQuery);
            connection.setAutoCommit(false);
            System.out.println(findQuery);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println(rs);
                String matchId = rs.getString("swiper_id");
                match.addToList(matchId);
                if (match.getMatchList().size() >= 100) {
                    break;
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            pstmt.close();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return match;
    }

    private boolean isSwiperExisted(String userId) {
        Connection connection;
        PreparedStatement pstmt;
        String findQuery = "SELECT * FROM SwipeData WHERE swiper_id = '" + userId + "'";
        try {
            connection = DatabaseConnectionPool.getConnection();
            pstmt = connection.prepareStatement(findQuery);
            ResultSet rs = pstmt.executeQuery();
            boolean existed = rs.next();
            connection.close();
            pstmt.close();
            return existed;
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSwipeeExisted(String userId) {
        Connection connection;
        PreparedStatement pstmt;
        String findQuery = "SELECT * FROM SwipeData WHERE swipee_id = '" + userId + "'";
        try {
            connection = DatabaseConnectionPool.getConnection();
            connection.setAutoCommit(false);
            pstmt = connection.prepareStatement(findQuery);
            ResultSet rs = pstmt.executeQuery();
            Boolean existed = rs.next();
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            pstmt.close();
            return existed;
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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

    private void sendRetrieveOkResponse(HttpServletResponse response, Object obj) throws IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().print(gson.toJson(obj));
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
