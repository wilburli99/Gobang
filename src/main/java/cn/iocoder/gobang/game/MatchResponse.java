package cn.iocoder.gobang.game;

import lombok.Data;

@Data
public class MatchResponse {
    private boolean ok;
    private String reason;
    private String message;
}
