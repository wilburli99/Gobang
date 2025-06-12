package cn.iocoder.gobang.game;

import cn.iocoder.gobang.GobangApplication;
import cn.iocoder.gobang.mapper.UserMapper;
import cn.iocoder.gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;
// 表示一个房间
@Data
public class Room {
    private String roomId;
    private User user1;
    private User user2;
    // 先手
    private int whiteUser;
    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;
    // 棋盘
    // 这个二维数组用来表示棋盘
    // 约定:
    // 1) 使用 0 表示当前位置未落子. 初始化好的 int 二维数组, 就相当于是 全 0
    // 2) 使用 1 表示 user1 的落子位置
    // 3) 使用 2 表示 user2 的落子位置
    private int[][] board = new int[MAX_ROW][MAX_COL];

    private ObjectMapper objectMapper = new ObjectMapper();

    private OnlineUserManage onlineUserManage;

    private UserMapper userMapper;

    private RoomManager roomManager;

    // 超时情况下的特殊坐标值
    private static final int TIMEOUT_POSITION = -1;

    public Room(){
        // 使用UUID来生成随机的字符串作为房间ID
        roomId = UUID.randomUUID().toString();
        // 手动注入对象
        onlineUserManage = GobangApplication.context.getBean(OnlineUserManage.class);
        roomManager = GobangApplication.context.getBean(RoomManager.class);
        userMapper = GobangApplication.context.getBean(UserMapper.class);
    }
    // 通过这个方法处理一次落子操作
    // 1.记录当前落子的位置
    // 2.进行胜负判定
    // 3.给客户端返回响应
    public void putChess(String reqJson) throws IOException {
        // 1. 解析请求
        GameRequest request = objectMapper.readValue(reqJson, GameRequest.class);
        GameResponse response = new GameResponse();
        
        // 2. 检查是否是超时消息
        if ("timeout".equals(request.getMessage())) {
            // 处理超时情况
            handleTimeout(request.getUserId());
            return;
        }
        
        // 3. 处理正常落子
        // 当前这个子是玩家1 落的还是玩家2 落的. 根据这个玩家1 和 玩家2 来决定往数组中是写 1 还是 2
        int chess = request.getUserId() == user1.getUserId() ? 1 : 2;
        int row = request.getRow();
        int col = request.getCol();
        if (board[row][col] != 0) {
            // 在客户端已经针对重复落子进行过判定了. 此处为了程序更加稳健, 在服务器再判定一次.
            System.out.println("当前位置 (" + row + ", " + col + ") 已经有子了!");
            return;
        }
        board[row][col] = chess;
        // 4. 打印出当前的棋盘信息, 方便来观察局势. 也方便后面验证胜负关系的判定.
        printBoard();

        // 5. 进行胜负判定
        int winner = checkWinner(row, col, chess);

        // 6. 给房间中的所有客户端都返回响应.
        response.setMessage("putChess");
        response.setUserId(request.getUserId());
        response.setRow(row);
        response.setCol(col);
        response.setWinner(winner);

        // 要想给用户发送 websocket 数据, 就需要获取到这个用户的 WebSocketSession
        WebSocketSession session1 = onlineUserManage.getFromGameRoom(user1.getUserId());
        WebSocketSession session2 = onlineUserManage.getFromGameRoom(user2.getUserId());
        // 万一当前查到的会话为空(玩家已经下线了) 特殊处理一下
        if (session1 == null) {
            // 玩家1 已经下线了. 直接认为玩家2 获胜!
            response.setWinner(user2.getUserId());
            System.out.println("玩家1 掉线!");
        }
        if (session2 == null) {
            // 玩家2 已经下线. 直接认为玩家1 获胜!
            response.setWinner(user1.getUserId());
            System.out.println("玩家2 掉线!");
        }
        // 把响应构造成 JSON 字符串, 通过 session 进行传输.
        String respJson = objectMapper.writeValueAsString(response);
        if (session1 != null) {
            session1.sendMessage(new TextMessage(respJson));
        }
        if (session2 != null) {
            session2.sendMessage(new TextMessage(respJson));
        }

        // 7. 如果当前胜负已分, 此时这个房间就失去存在的意义了. 就可以直接销毁房间. (把房间从房间管理器中给移除)
        if (response.getWinner() != 0) {
            // 胜负已分
            System.out.println("游戏结束! 房间即将销毁! roomId=" + roomId + " 获胜方为: " + response.getWinner());
            // 更新获胜方和失败方的信息.
            int winUserId = response.getWinner();
            int loseUserId = response.getWinner() == user1.getUserId() ? user2.getUserId() : user1.getUserId();
            userMapper.userWin(winUserId);
            userMapper.userLose(loseUserId);
            // 销毁房间
            roomManager.remove(roomId, user1.getUserId(), user2.getUserId());
        }
    }
    
    // 处理超时情况
    private void handleTimeout(int timeoutUserId) throws IOException {
        System.out.println("玩家 " + timeoutUserId + " 超时!");
        
        // 确定获胜方（非超时方获胜）
        int winnerUserId = (timeoutUserId == user1.getUserId()) ? user2.getUserId() : user1.getUserId();
        
        // 构造响应
        GameResponse response = new GameResponse();
        response.setMessage("putChess");
        response.setUserId(timeoutUserId);
        response.setRow(TIMEOUT_POSITION);
        response.setCol(TIMEOUT_POSITION);
        response.setWinner(winnerUserId);
        
        // 发送响应给两个玩家
        WebSocketSession session1 = onlineUserManage.getFromGameRoom(user1.getUserId());
        WebSocketSession session2 = onlineUserManage.getFromGameRoom(user2.getUserId());
        
        String respJson = objectMapper.writeValueAsString(response);
        if (session1 != null) {
            session1.sendMessage(new TextMessage(respJson));
        }
        if (session2 != null) {
            session2.sendMessage(new TextMessage(respJson));
        }
        
        // 更新玩家分数
        userMapper.userWin(winnerUserId);
        userMapper.userLose(timeoutUserId);
        
        // 销毁房间
        roomManager.remove(roomId, user1.getUserId(), user2.getUserId());
        
        System.out.println("超时游戏结束! 获胜方: " + winnerUserId);
    }

    private void printBoard() {
        // 打印出棋盘
        System.out.println("[打印棋盘信息] " + roomId);
        System.out.println("=====================================================================");
        for (int r = 0; r < MAX_ROW; r++) {
            for (int c = 0; c < MAX_COL; c++) {
                // 针对一行之内的若干列, 不要打印换行
                System.out.print(board[r][c] + " ");
            }
            // 每次遍历完一行之后, 再打印换行.
            System.out.println();
        }
        System.out.println("=====================================================================");
    }

    // 使用这个方法来判定当前落子是否分出胜负.
    // 约定如果玩家1 获胜, 就返回玩家1 的 userId
    // 如果玩家2 获胜, 就返回玩家2 的 userId
    // 如果胜负未分, 就返回 0
    private int checkWinner(int row, int col, int chess) {
        // 1. 检查所有的行
        //    先遍历这五种情况
        for (int c = col - 4; c <= col; c++) {
            // 针对其中的一种情况, 来判定这五个子是不是连在一起了~
            // 不光是这五个子得连着, 而且还得和玩家落的子是一样~~ (才算是获胜)
            try {
                if (board[row][c] == chess
                        && board[row][c + 1] == chess
                        && board[row][c + 2] == chess
                        && board[row][c + 3] == chess
                        && board[row][c + 4] == chess) {
                    // 构成了五子连珠! 胜负已分!
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // 如果出现数组下标越界的情况, 就在这里直接忽略这个异常.
                continue;
            }
        }

        // 2. 检查所有列
        for (int r = row - 4; r <= row; r++) {
            try {
                if (board[r][col] == chess
                        && board[r + 1][col] == chess
                        && board[r + 2][col] == chess
                        && board[r + 3][col] == chess
                        && board[r + 4][col] == chess) {
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        // 3. 检查左对角线
        for (int r = row - 4, c = col - 4; r <= row && c <= col; r++, c++) {
            try {
                if (board[r][c] == chess
                        && board[r + 1][c + 1] == chess
                        && board[r + 2][c + 2] == chess
                        && board[r + 3][c + 3] == chess
                        && board[r + 4][c + 4] == chess) {
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        // 4. 检查右对角线
        for (int r = row - 4, c = col + 4; r <= row && c >= col; r++, c--) {
            try {
                if (board[r][c] == chess
                        && board[r + 1][c - 1] == chess
                        && board[r + 2][c - 2] == chess
                        && board[r + 3][c - 3] == chess
                        && board[r + 4][c - 4] == chess) {
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
        }

        // 胜负未分, 就直接返回 0 了.
        return 0;
    }
}
