package com.github.xcfyl.drpc.core.server;

import com.github.xcfyl.drpc.core.common.config.ConfigLoader;
import com.github.xcfyl.drpc.core.common.enums.AttributeName;
import com.github.xcfyl.drpc.core.common.factory.RegistryFactory;
import com.github.xcfyl.drpc.core.common.factory.SerializerFactory;
import com.github.xcfyl.drpc.core.common.utils.CommonUtils;
import com.github.xcfyl.drpc.core.exception.RpcCommonException;
import com.github.xcfyl.drpc.core.filter.server.ServerFilterChain;
import com.github.xcfyl.drpc.core.filter.server.ServerLogFilter;
import com.github.xcfyl.drpc.core.protocol.RpcTransferProtocolDecoder;
import com.github.xcfyl.drpc.core.protocol.RpcTransferProtocolEncoder;
import com.github.xcfyl.drpc.core.registry.ProviderData;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * rpc服务端
 *
 * @author 西城风雨楼
 * @date create at 2023/6/22 10:30
 */
@Slf4j
public class RpcServer {
    /**
     * Rpc服务器上下文数据
     */
    private final ServerContext context;
    /**
     * 用于执行服务注册的线程池
     */
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 10,
            1000, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 创建Rpc服务器
     *
     * @param configFileName
     */
    public RpcServer(String configFileName) {
        context = new ServerContext();
        context.setConfigFileName(configFileName);
        ConfigLoader rpcConfigLoader = new ConfigLoader(configFileName);
        context.setServerConfig(rpcConfigLoader.loadRpcServerConfig());
    }

    /**
     * 使用默认配置文件启动创建rpc服务器
     */
    public RpcServer() {
        this("drpc.properties");
    }

    /**
     * 初始化rpc服务端
     *
     * @throws Exception
     */
    public void init() throws Exception {
        ServerConfig config = context.getServerConfig();
        // 设置注册中心对象
        context.setRegistry(RegistryFactory.createRpcRegistry(config.getRegistryType(), config.getRegistryAddr()));
        // 创建序列化器对象
        context.setSerializer(SerializerFactory.createRpcSerializer(config.getSerializeType()));
        // 创建过滤器
        context.setFilterChain(constructServerFilters());
        new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(new RpcTransferProtocolEncoder());
                        channel.pipeline().addLast(new RpcTransferProtocolDecoder());
                        channel.pipeline().addLast(new ServerHandler(context));
                    }
                })
                .bind(config.getPort())
                .sync();
    }

    /**
     * 取消服务注册
     *
     * @param service
     */
    public void unregisterService(Object service) {
        // 将当前服务名称写入
        threadPoolExecutor.submit(() -> {
            // 首先将当前服务写入注册中心
            try {
                ProviderData registryData = getProviderRegistryData(service);
                // 将数据从注册中心移除
                context.getRegistry().unregister(registryData);
                // 删除本地缓存的注册数据
                context.getRegistryDataCache().remove(registryData.getServiceName());
                // 删除本地缓存的服务提供者数据
                context.getServiceProviderCache().remove(registryData.getServiceName());
            } catch (Exception e) {
                log.error("register service failure, service name is #{}, exception is #{}", service, e.getMessage());
            }
        });
    }

    /**
     * 注册服务
     *
     * @param service
     */
    public void registerService(Object service) {
        // 将当前服务名称写入
        threadPoolExecutor.submit(() -> {
            try {
                // 首先将当前服务写入注册中心
                ProviderData registryData = getProviderRegistryData(service);
                context.getRegistry().register(registryData);
                // 将当前服务写入本地缓存中
                context.getRegistryDataCache().put(registryData.getServiceName(), registryData);
                context.getServiceProviderCache().put(registryData.getServiceName(), service);
            } catch (Exception e) {
                log.error("register service failure, service name is #{}, exception is #{}", service, e.getMessage());
            }
        });
    }

    private ServerFilterChain constructServerFilters() {
        ServerFilterChain filterChain = new ServerFilterChain();
        filterChain.addFilter(new ServerLogFilter());
        return filterChain;
    }

    private ProviderData getProviderRegistryData(Object service) throws Exception {
        // 首先将当前服务写入注册中心
        Class<?>[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length != 1) {
            log.error("#{} implement too many interface!", service);
            throw new RpcCommonException("implement too many interface");
        }
        Class<?> clazz = interfaces[0];
        String serviceName = clazz.getName();
        ProviderData registryData = new ProviderData();
        registryData.setIp(CommonUtils.getCurrentMachineIp());
        registryData.setServiceName(serviceName);
        ServerConfig config = context.getServerConfig();
        registryData.setPort(config.getPort());
        registryData.setApplicationName(config.getApplicationName());
        registryData.setAttr(AttributeName.TYPE.getDescription(), "provider");
        registryData.setAttr(AttributeName.CREATE_TIME.getDescription(), System.currentTimeMillis());
        return registryData;
    }
}
