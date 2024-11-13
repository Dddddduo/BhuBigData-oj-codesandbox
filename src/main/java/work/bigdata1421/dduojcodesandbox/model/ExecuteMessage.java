package work.bigdata1421.dduojcodesandbox.model;

import lombok.Data;

/*
* 进程的执行信息
* */
@Data
public class ExecuteMessage {

    private int exitValue;

    private String message;

    private String errorMessage;

    private Long time;
}
