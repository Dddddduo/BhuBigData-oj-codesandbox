package work.bigdata1421.dduojcodesandbox.security;

import java.security.Permission;

/**
 * 默认的安全管理器
 */
public class DefaultSecurityManager extends SecurityManager{

    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何限制");
        System.out.println(perm);
    }

}
