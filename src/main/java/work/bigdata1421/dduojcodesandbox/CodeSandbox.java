package work.bigdata1421.dduojcodesandbox;


//代码沙箱接口的定义


import work.bigdata1421.dduojcodesandbox.model.ExecuteCodeRequest;
import work.bigdata1421.dduojcodesandbox.model.ExecuteCodeResponse;

import java.io.IOException;

public interface CodeSandbox {

    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) throws IOException;
}
