package cn.iocoder.gobang.game;

import lombok.Data;

@Data
public class MatchRequest {
    private boolean ok;
    private String reason;
    private String message;
}
