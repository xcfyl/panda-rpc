package com.github.xcfyl.pandarpc.core.client;

import com.github.xcfyl.pandarpc.core.proxy.ProxyFactory;
import com.github.xcfyl.pandarpc.core.router.RpcRouter;

/**
 * @author 西城风雨楼
 * @date create at 2023/6/22 12:05
 */
public class RpcReference {
    private final ProxyFactory proxyFactory;
    private final RpcRouter router;

    public RpcReference(ProxyFactory proxyFactory, RpcRouter router) {
        this.proxyFactory = proxyFactory;
        this.router = router;
    }

    /**
     * 获取某个接口的代理类对象
     *
     * @param clazz 被代理的类
     * @return 返回clazz的代理类对象
     * @param <T> 返回的代理对象类型
     * @throws Throwable 可能抛出的异常
     */
    public <T> T get(Class<T> clazz) throws Throwable {
        return proxyFactory.getProxy(clazz);
    }
}
