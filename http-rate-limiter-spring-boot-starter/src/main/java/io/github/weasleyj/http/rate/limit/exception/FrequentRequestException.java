package io.github.weasleyj.http.rate.limit.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Frequent Request Exception
 *
 * @author liuwenjing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class FrequentRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 异常消息
     */
    private String msg = "操作太过频繁，请稍后再试";

    /**
     * 错误码, 默认: 10001
     */
    private int code = 10001;

    public FrequentRequestException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public FrequentRequestException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public FrequentRequestException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }
}
