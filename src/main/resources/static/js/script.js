let gameInfo = {
    roomId: null,
    thisUserId: null,
    thatUserId: null,
    isWhite: true,
    // 倒计时相关状态
    countdownTimer: null,
    currentTime: 30,
    isMyTurn: false,
    gameStarted: false,
    // 最新落子标记（使用-1表示无效位置）
    lastMoveRow: -1,
    lastMoveCol: -1
}

// 定义常量，使代码更清晰
const NO_MOVE = -1;        // 表示没有落子
const TIMEOUT_POSITION = -1; // 表示超时情况

//////////////////////////////////////////////////
// 设定界面显示相关操作
//////////////////////////////////////////////////

function setScreenText(me) {
    let screen = document.querySelector('#screen');
    let turnIndicator = document.querySelector('#turn-indicator');
    let countdownLabel = document.querySelector('#countdown-label');
    
    gameInfo.isMyTurn = me;
    
    if (me) {
        screen.innerHTML = "轮到你落子了!";
        screen.style.display = "block";
        turnIndicator.innerHTML = "轮到你了";
        countdownLabel.innerHTML = "你的时间";
    } else {
        screen.innerHTML = "轮到对方落子了!";
        screen.style.display = "block";
        turnIndicator.innerHTML = "对方思考中";
        countdownLabel.innerHTML = "对方时间";
    }
    
    // 无论轮到谁，都启动倒计时显示
    startCountdown();
}

// 添加一个函数来隐藏等待连接的提示
function hideWaitingScreen() {
    let screen = document.querySelector('#screen');
    screen.style.display = "none";
}

//////////////////////////////////////////////////
// 倒计时相关函数
//////////////////////////////////////////////////

function startCountdown() {
    // 清除之前的倒计时
    stopCountdown();
    
    // 重置倒计时时间
    gameInfo.currentTime = 30;
    updateCountdownDisplay();
    
    // 开始新的倒计时
    gameInfo.countdownTimer = setInterval(function() {
        gameInfo.currentTime--;
        updateCountdownDisplay();
        
        if (gameInfo.currentTime <= 0) {
            // 倒计时结束，玩家超时
            handleTimeout();
        }
    }, 1000);
}

function stopCountdown() {
    if (gameInfo.countdownTimer) {
        clearInterval(gameInfo.countdownTimer);
        gameInfo.countdownTimer = null;
    }
}

function updateCountdownDisplay() {
    let countdownDisplay = document.querySelector('#countdown-display');
    countdownDisplay.innerHTML = gameInfo.currentTime;
    
    // 移除之前的样式类
    countdownDisplay.classList.remove('warning', 'timeout', 'my-turn', 'opponent-turn');
    
    // 根据剩余时间添加样式
    if (gameInfo.currentTime <= 0) {
        countdownDisplay.classList.add('timeout');
        countdownDisplay.innerHTML = "超时";
    } else if (gameInfo.currentTime <= 10) {
        countdownDisplay.classList.add('warning');
    } else {
        // 根据当前轮到谁显示不同颜色
        if (gameInfo.isMyTurn) {
            countdownDisplay.classList.add('my-turn');
        } else {
            countdownDisplay.classList.add('opponent-turn');
        }
    }
}

function handleTimeout() {
    stopCountdown();
    
    if (gameInfo.isMyTurn) {
        // 我超时了，发送超时消息给服务器
        let timeoutReq = {
            message: 'timeout',
            userId: gameInfo.thisUserId
        };
        
        websocket.send(JSON.stringify(timeoutReq));
        
        // 显示超时信息
        let screen = document.querySelector('#screen');
        screen.innerHTML = "你超时了，游戏结束！";
        
        let turnIndicator = document.querySelector('#turn-indicator');
        turnIndicator.innerHTML = "超时败北";
    } else {
        // 对方超时了，发送对方的超时消息给服务器
        let timeoutReq = {
            message: 'timeout',
            userId: gameInfo.thatUserId
        };
        
        websocket.send(JSON.stringify(timeoutReq));
        
        // 显示对方超时信息
        let screen = document.querySelector('#screen');
        screen.innerHTML = "对方超时，你获胜！";
        
        let turnIndicator = document.querySelector('#turn-indicator');
        turnIndicator.innerHTML = "对方超时";
    }
    
    // 使用通用函数创建回到大厅按钮
    createBackToHallButton();
}

// 创建回到大厅按钮的通用函数
function createBackToHallButton() {
    // 检查是否已经存在回到大厅按钮，避免重复创建
    let existingBtn = document.querySelector('.return-btn');
    if (!existingBtn) {
        let backBtn = document.createElement('button');
        backBtn.className = 'return-btn';
        backBtn.innerHTML = '回到大厅';
        backBtn.onclick = function() {
            location.replace('/game_hall.html');
        }
        let fatherDiv = document.querySelector('.container>div');
        fatherDiv.appendChild(backBtn);
    }
}

//////////////////////////////////////////////////
// 初始化 websocket
//////////////////////////////////////////////////

// 此处写的路径要写作 /game, 不要写作 /game/
let websocketUrl = "ws://" + location.host + "/game";
let websocket = new WebSocket(websocketUrl);

websocket.onopen = function() {
    console.log("连接游戏房间成功!");
}

websocket.close = function() {
    console.log("和游戏服务器断开连接!");
}

websocket.onerror = function() {
    console.log("和服务器的连接出现异常!");
}

window.onbeforeunload = function() {
    websocket.close();
}

// 处理服务器返回的响应数据
websocket.onmessage = function(event) {
    console.log("[handlerGameReady] " + event.data);
    let resp = JSON.parse(event.data);

    if (!resp.ok) {
        alert("连接游戏失败! reason: " + resp.reason);
        // 如果出现连接失败的情况, 回到游戏大厅
        location.assign("/game_hall.html");
        return;
    }

    if (resp.message == 'gameReady') {
        gameInfo.roomId = resp.roomId;
        gameInfo.thisUserId = resp.thisUserId;
        gameInfo.thatUserId = resp.thatUserId;
        gameInfo.isWhite = (resp.whiteUser == resp.thisUserId);

        let player1UsernameSpan = document.querySelector('#player1-username');
        let player1PieceSpan = document.querySelector('#player1-piece');
        let player2UsernameSpan = document.querySelector('#player2-username');
        let player2PieceSpan = document.querySelector('#player2-piece');
        let player1Label = document.querySelector('#player1-label');
        let player2Label = document.querySelector('#player2-label');

        // 根据谁是白棋，谁是黑棋来显示用户名和棋子颜色
        // resp.thisUsername 和 resp.thatUsername 现在应该包含了真实的用户名
        if (gameInfo.isWhite) {
            player1Label.innerText = "你："; // 我是白棋
            player1UsernameSpan.innerText = resp.thisUsername; // 显示我的用户名
            player1PieceSpan.innerText = "白子";
            player2Label.innerText = "对手："; // 对手是黑棋
            player2UsernameSpan.innerText = resp.thatUsername; // 显示对手用户名
            player2PieceSpan.innerText = "黑子";
        } else {
            player1Label.innerText = "你："; // 我是黑棋
            player1UsernameSpan.innerText = resp.thisUsername; // 显示我的用户名
            player1PieceSpan.innerText = "黑子";
            player2Label.innerText = "对手："; // 对手是白棋
            player2UsernameSpan.innerText = resp.thatUsername; // 显示对手用户名
            player2PieceSpan.innerText = "白子";
        }

        // 初始化棋盘
        initGame();
        // 设置显示区域的内容和倒计时
        gameInfo.gameStarted = true;
        // 重置最新落子位置
        gameInfo.lastMoveRow = NO_MOVE;
        gameInfo.lastMoveCol = NO_MOVE;
        setScreenText(gameInfo.isWhite);
    } else if (resp.message == 'repeatConnection') {
        alert("检测到游戏多开! 请使用其他账号登录!");
        location.assign("/login.html");
    }
}

//////////////////////////////////////////////////
// 初始化一局游戏
//////////////////////////////////////////////////
function initGame() {
    // 是我下还是对方下. 根据服务器分配的先后手情况决定
    let me = gameInfo.isWhite;
    // 游戏是否结束
    let over = false;
    let chessBoard = [];
    //初始化chessBord数组(表示棋盘的数组)
    for (let i = 0; i < 15; i++) {
        chessBoard[i] = [];
        for (let j = 0; j < 15; j++) {
            chessBoard[i][j] = null; // 改为null表示空位，存储对象表示有棋子
        }
    }
    let chess = document.querySelector('#chess');
    let context = chess.getContext('2d');
    context.strokeStyle = "#BFBFBF";
    // 背景图片
    let logo = new Image();
    let logoLoaded = false; // 添加标志位
    logo.src = "image/sky.jpeg";
    logo.onload = function () {
        logoLoaded = true;
        context.drawImage(logo, 0, 0, 450, 450);
        initChessBoard();
    }

    // 绘制棋盘网格
    function initChessBoard() {
        for (let i = 0; i < 15; i++) {
            context.moveTo(15 + i * 30, 15);
            context.lineTo(15 + i * 30, 430);
            context.stroke();
            context.moveTo(15, 15 + i * 30);
            context.lineTo(435, 15 + i * 30);
            context.stroke();
        }
    }

    // 绘制一个棋子, isWhite表示是否为白棋, isLastMove表示是否为最新落子
    function oneStep(i, j, isWhite, isLastMove = false) {
        // 绘制棋子
        context.beginPath();
        context.arc(15 + i * 30, 15 + j * 30, 13, 0, 2 * Math.PI);
        context.closePath();
        var gradient = context.createRadialGradient(15 + i * 30 + 2, 15 + j * 30 - 2, 13, 15 + i * 30 + 2, 15 + j * 30 - 2, 0);
        if (!isWhite) {
            gradient.addColorStop(0, "#0A0A0A");
            gradient.addColorStop(1, "#636766");
        } else {
            gradient.addColorStop(0, "#D1D1D1");
            gradient.addColorStop(1, "#F9F9F9");
        }
        context.fillStyle = gradient;
        context.fill();
        
        // 如果是最新落子，绘制标记
        if (isLastMove) {
            drawLastMoveMarker(i, j);
        }
    }
    
    // 绘制最新落子标记
    function drawLastMoveMarker(col, row) {
        // 绘制外圈（稍大的圆）
        context.beginPath();
        context.arc(15 + col * 30, 15 + row * 30, 6, 0, 2 * Math.PI);
        context.closePath();
        context.fillStyle = "#FFFFFF"; // 白色外圈
        context.fill();
        
        // 绘制内圈（红色标记）
        context.beginPath();
        context.arc(15 + col * 30, 15 + row * 30, 4, 0, 2 * Math.PI);
        context.closePath();
        context.fillStyle = "#FF4444"; // 红色标记
        context.fill();
        
        // 添加边框使标记更清晰
        context.beginPath();
        context.arc(15 + col * 30, 15 + row * 30, 6, 0, 2 * Math.PI);
        context.strokeStyle = "#333333";
        context.lineWidth = 1;
        context.stroke();
        
        // 恢复默认设置
        context.lineWidth = 1;
        context.strokeStyle = "#BFBFBF";
    }

    chess.onclick = function (e) {
        if (over) {
            return;
        }
        if (!me) {
            return;
        }
        let x = e.offsetX;
        let y = e.offsetY;
        // 注意, 横坐标是列, 纵坐标是行
        let col = Math.floor(x / 30);
        let row = Math.floor(y / 30);
        if (chessBoard[row][col] == null) {
            // 落子时停止倒计时
            stopCountdown();
            
            // 发送坐标给服务器, 服务器要返回结果
            send(row, col);

            // 留到浏览器收到落子响应的时候再处理(收到响应再来画棋子)
            // oneStep(col, row, gameInfo.isWhite);
            // chessBoard[row][col] = 1;
        }
    }

    function send(row, col) {
        let req = {
            message: 'putChess',
            userId: gameInfo.thisUserId,
            row: row,
            col: col
        };

        websocket.send(JSON.stringify(req));
    }

    // 之前 websocket.onmessage 主要是用来处理了游戏就绪响应. 在游戏就绪之后, 初始化完毕之后, 也就不再有这个游戏就绪响应了. 
    // 就在这个 initGame 内部, 修改 websocket.onmessage 方法~~, 让这个方法里面针对落子响应进行处理!
    websocket.onmessage = function(event) {
        console.log("[handlerPutChess] " + event.data);

        let resp = JSON.parse(event.data);
        if (resp.message != 'putChess') {
            console.log("响应类型错误!");
            return;
        }

        // 检查是否是超时情况（row和col为TIMEOUT_POSITION表示超时）
        let isTimeout = (resp.row === TIMEOUT_POSITION && resp.col === TIMEOUT_POSITION);
        
        if (!isTimeout) {
            // 正常落子情况
            // 更新最新落子位置
            gameInfo.lastMoveRow = resp.row;
            gameInfo.lastMoveCol = resp.col;
            
            // 先判定当前这个响应是自己落的子, 还是对方落的子.
            if (resp.userId == gameInfo.thisUserId) {
                // 我自己落的子
                // 存储棋子信息到棋盘数组
                chessBoard[resp.row][resp.col] = {
                    userId: resp.userId,
                    isWhite: gameInfo.isWhite
                };
            } else if (resp.userId == gameInfo.thatUserId) {
                // 我的对手落的子
                chessBoard[resp.row][resp.col] = {
                    userId: resp.userId,
                    isWhite: !gameInfo.isWhite
                };
            } else {
                // 响应错误! userId 是有问题的!
                console.log('[handlerPutChess] resp userId 错误!');
                return;
            }

            // 重绘整个棋盘以显示最新标记
            redrawBoard();

            // 交换双方的落子轮次
            me = !me;
            setScreenText(me);
        }

        // 判定游戏是否结束
        let screenDiv = document.querySelector('#screen');
        if (resp.winner != 0) {
            // 游戏结束，停止倒计时
            stopCountdown();
            
            let turnIndicator = document.querySelector('#turn-indicator');
            let countdownLabel = document.querySelector('#countdown-label');
            countdownLabel.innerHTML = "游戏结束";
            
            if (resp.winner == gameInfo.thisUserId) {
                if (isTimeout) {
                    screenDiv.innerHTML = '对方超时，你赢了!';
                    turnIndicator.innerHTML = "对方超时";
                } else {
                    screenDiv.innerHTML = '你赢了!';
                    turnIndicator.innerHTML = "胜利！";
                }
            } else if (resp.winner == gameInfo.thatUserId) {
                if (isTimeout && resp.userId == gameInfo.thisUserId) {
                    screenDiv.innerHTML = '你超时了，游戏结束!';
                    turnIndicator.innerHTML = "超时败北";
                } else {
                    screenDiv.innerHTML = '你输了!';
                    turnIndicator.innerHTML = "败北";
                }
            } else {
                alert("winner 字段错误! " + resp.winner);
            }
            // 使用通用函数创建回到大厅按钮
            createBackToHallButton();
            
            // 设置游戏结束标志
            over = true;
        }
    }

    // 重绘整个棋盘（包括背景、网格、所有棋子和标记）
    function redrawBoard() {
        // 清除画布
        context.clearRect(0, 0, 450, 450);
        
        // 重绘背景
        if (logoLoaded) {
            context.drawImage(logo, 0, 0, 450, 450);
        }
        
        // 重绘网格
        initChessBoard();
        
        // 重绘所有棋子
        for (let row = 0; row < 15; row++) {
            for (let col = 0; col < 15; col++) {
                if (chessBoard[row][col] !== null) {
                    // 获取棋子信息
                    let pieceInfo = chessBoard[row][col];
                    let isWhite = pieceInfo.isWhite;
                    
                    // 检查是否为最新落子
                    let isLastMove = (row === gameInfo.lastMoveRow && col === gameInfo.lastMoveCol);
                    
                    // 绘制棋子
                    oneStep(col, row, isWhite, isLastMove);
                }
            }
        }
    }
}


