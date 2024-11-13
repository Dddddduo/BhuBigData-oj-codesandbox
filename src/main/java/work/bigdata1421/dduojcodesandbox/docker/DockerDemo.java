package work.bigdata1421.dduojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

public class
DockerDemo {
    public static void main(String[] args) {
        // 获取默认的 Docker Client
        DockerClient build = DockerClientBuilder.getInstance().build();
        System.out.println(build);
    }
}
