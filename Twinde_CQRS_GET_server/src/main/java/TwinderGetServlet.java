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
                if (isMatchExisted(userId)) {
                    String match = getMatches(userId);
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
                if (isStatExisted(userId)) {
                    MatchStats stat = getMatchStats(userId);
                    sendRetrieveOkResponse(response, stat);
                } else {
                    sendUserNotFoundResponse(response);
                }
            }
        }
    }

    private MatchStats getMatchStats(String userId) {
        String findStatsQuery = "SELECT likes, dislikes FROM Stats WHERE user_id = '" + userId + "'";
        Connection connection;
        PreparedStatement pstmt;
        try {
            connection = DatabaseConnectionPool.getConnection();
            connection.setAutoCommit(false);
            ResultSet rs;
            int dislikeNum = 0;
            int likeNum = 0;
            pstmt = connection.prepareStatement(findStatsQuery);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                likeNum = rs.getInt("likes");
                dislikeNum = rs.getInt("dislikes");
            }
            MatchStats matchStats = new MatchStats(likeNum, dislikeNum);
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            pstmt.close();
            return matchStats;
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMatches(String userId) {
        Connection connection;
        PreparedStatement pstmt;
        String findQuery = "SELECT match_list FROM Matches WHERE user_id =" + "'" + userId + "'";
        String matchList = "";
        try {
            connection = DatabaseConnectionPool.getConnection();
            pstmt = connection.prepareStatement(findQuery);
            connection.setAutoCommit(false);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                matchList = rs.getString("match_list");
            }
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            pstmt.close();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return matchList;
    }

    private boolean isStatExisted(String userId) {
        Connection connection;
        PreparedStatement pstmt;
//        String findQuery = "SELECT * FROM SwipeData WHERE swiper_id = '" + userId + "'";
        String findQuery = "SELECT user_id FROM Stats WHERE user_id = '" + userId + "' LIMIT 1";
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

    private boolean isMatchExisted(String userId) {
        Connection connection;
        PreparedStatement pstmt;
        String findQuery = "SELECT user_id FROM Matches WHERE user_id = '" + userId + "' LIMIT 1";
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