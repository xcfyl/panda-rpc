package com.github.xcfyl.drpc.core.proxy.jdk;

import com.alibaba.fastjson.JSON;
import com.github.xcfyl.drpc.core.client.ConnectionManager;
import com.github.xcfyl.drpc.core.client.ConnectionWrapper;
import com.github.xcfyl.drpc.core.client.RpcClientContext;
import com.github.xcfyl.drpc.core.client.SubscribedServiceWrapper;
import com.github.xcfyl.drpc.core.protocol.RpcRequest;
import com.github.xcfyl.drpc.core.protocol.RpcResponse;
import com.github.xcfyl.drpc.core.protocol.RpcTransferProtocol;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * rpc客户端发起rpc请求的时候，代理类的执行逻辑
 *
 * @author 西城风雨楼
 * @date create at 2023/6/22 11:17
 */
@Slf4j
public class RpcInvocationHandler<T> implements InvocationHandler {
    private final SubscribedServiceWrapper<T> serviceWrapper;
    private final RpcClientContext rpcClientContext;

    public RpcInvocationHandler(RpcClientContext rpcClientContext, SubscribedServiceWrapper<T> serviceWrapper) {
        this.serviceWrapper = serviceWrapper;
        this.rpcClientContext = rpcClientContext;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        String serviceName = serviceWrapper.getServiceClass().getName();
        String methodName = method.getName();
        RpcRequest request = new RpcRequest(requestId, serviceName, methodName, args);
        ConnectionManager connectionManager = rpcClientContext.getConnectionManager();
        List<ConnectionWrapper> originalConnections = connectionManager.getOriginalConnections(serviceName);
        List<ConnectionWrapper> filteredConnections = new ArrayList<>(originalConnections);
        // 对连接缓存进行过滤
        rpcClientContext.getFilterChain().doFilter(filteredConnections, request);
        // 由路由对象从过滤后的连接缓存中选择一个连接对象
        ConnectionWrapper connectionWrapper = rpcClientContext.getRouter().select(serviceName);
        RpcTransferProtocol protocol = new RpcTransferProtocol(JSON.toJSONString(request).getBytes());
        connectionWrapper.writeAndFlush(protocol);

        // 判断是否是同步方法调用
        if (serviceWrapper.isSync()) {
            long beginTime = System.currentTimeMillis();
            long timeout = rpcClientContext.getClientConfig().getRequestTimeout();
            while (System.currentTimeMillis() - beginTime < timeout) {
                RpcResponse response = rpcClientContext.getResponseCache().get(requestId);
                if (response != null) {
                    return response.getBody();
                }
            }
            throw new TimeoutException("请求超时");
        }
        return null;
    }
}