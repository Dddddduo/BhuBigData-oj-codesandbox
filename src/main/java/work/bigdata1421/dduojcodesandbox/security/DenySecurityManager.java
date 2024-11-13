package work.bigdata1421.dduojcodesandbox.security;

import java.security.Permission;

/**
 * 禁止所有的权限安全管理器
 */
public class DenySecurityManager extends SecurityManager{

    // 抛出异常
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限异常"+perm.getActions());
    }

}
